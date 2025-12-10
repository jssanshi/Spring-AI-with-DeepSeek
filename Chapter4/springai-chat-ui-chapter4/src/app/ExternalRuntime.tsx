import {ExternalStoreThreadData, ThreadMessageLike} from "@assistant-ui/react";
import {useUserId} from "@/app/assistant";

// 一个全局 AbortController，用于取消当前请求
let controller: AbortController | null = null;

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;

export const runAssistantStream = async (
    input: string,
    threadId: string, // ✅ 当前线程
    baseMessages: ThreadMessageLike[],
    setThreads: React.Dispatch<React.SetStateAction<Map<string, ThreadMessageLike[]>>>,
    setMessages: React.Dispatch<React.SetStateAction<readonly ThreadMessageLike[]>>,
    setIsRunning: React.Dispatch<React.SetStateAction<boolean>>,
    setThreadList: (value: (prev) => (ExternalStoreThreadData<"regular"> | ExternalStoreThreadData<"archived"> | { id: string; title: string; status: string })[]) => void,
    setCurrentThreadId: (id: string) => void,
    userId: string
) => {
    // 如果是 default thread，就创建新 thread
    if (threadId === "default") {
        const newId = `thread-${Date.now()}`;
        const newTitle = input.slice(0, 20);
        const newThread = {
            id: newId,
            status: "regular",
            title: newTitle,
        };
        setThreadList((prev) => [...prev, newThread]);
        setCurrentThreadId(newId);
        threadId = newId;

        try {
            const response = await fetch(`${apiBaseUrl}/chat/threads?userName=${userId}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(newThread),
            });

            if (!response.ok) {
                throw new Error(`保存失败: ${response.status}`);
            }

            const result = await response.json();
            console.log("保存成功:", result);
        } catch (error) {
            console.error("保存线程失败:", error);
        }
    }

    if (controller) controller.abort();
    controller = new AbortController();
    setIsRunning(true);

    // 初始化 assistant 消息
    const assistantId = crypto.randomUUID();
    let partialAssistantMessage: ThreadMessageLike = {
        role: "assistant",
        id: assistantId,
        content: [{ type: "text", text: "" }],
        metadata: { unstable_annotations: [] },
    };

    // 放入当前消息状态 + threads
    const newMessages = [...baseMessages, partialAssistantMessage];
    setMessages(newMessages);

    // 同步到线程 Map
    setThreads((prev: Map<string, ThreadMessageLike[]>) => {
        const newMap = new Map<string, ThreadMessageLike[]>(prev);
        newMap.set(threadId, newMessages);
        return newMap;
    });

    try {
        const resp = await fetch(`${apiBaseUrl}/ai/chat/stream?userName=${userId}`, {
            method: "POST",
            body: JSON.stringify({ baseMessages }),
            headers: { "Content-Type": "application/json" },
            signal: controller.signal,
        });

        if (!resp.body) throw new Error("No response body");

        const reader = resp.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (true) {
            const { value, done } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const parts = buffer.split("\n\n");
            buffer = parts.pop() ?? "";

            for (const part of parts) {
                if (!part.startsWith("data:")) continue;
                const jsonStr = part.replace(/^data:\s*/, "").trim();

                if (jsonStr === "[DONE]") {
                    await saveChatMessages(threadId, baseMessages, partialAssistantMessage, userId);
                    return;
                }

                try {
                    const event = JSON.parse(jsonStr);

                    if (event.type === "text-delta") {
                        const delta = event.delta ?? "";
                        partialAssistantMessage = {
                            ...partialAssistantMessage,
                            content: [
                                {
                                    type: "text",
                                    text:
                                        partialAssistantMessage.content[0].text +
                                        delta,
                                },
                            ],
                        };
                    }

                    if (event.type === "mem") {
                        const memObj = event.delta;
                        partialAssistantMessage = {
                            ...partialAssistantMessage,
                            metadata: {
                                ...partialAssistantMessage.metadata,
                                unstable_annotations: [
                                    ...(partialAssistantMessage.metadata
                                        ?.unstable_annotations ?? []),
                                    memObj,
                                ],
                            },
                        };
                    }

                    // 更新 UI 消息状态
                    setMessages((current) =>
                        current.map((m) =>
                            m.id === assistantId ? partialAssistantMessage : m
                        )
                    );

                    // 同步更新到 threads
                    setThreads((prev: Map<string, ThreadMessageLike[]>) => {
                        const newMap = new Map(prev);
                        const currentMsgs = newMap.get(threadId) ?? [];
                        const updatedMsgs = currentMsgs.map((m) =>
                            m.id === assistantId ? partialAssistantMessage : m
                        );
                        newMap.set(threadId, updatedMsgs);
                        return newMap;
                    });
                } catch (err) {
                    console.error("Failed to parse SSE chunk:", err, jsonStr);
                }
            }
        }
    } catch (err) {
        if ((err as any).name === "AbortError") {
            console.log("Generation cancelled");
        } else {
            console.error("Stream error:", err);
        }
    } finally {
        controller = null;
        setIsRunning(false);
    }
};

/**
 * 取消当前 SSE 请求
 */
export const cancelAssistantStream = async (
    setIsRunning: React.Dispatch<React.SetStateAction<boolean>>
) => {
    if (controller) {
        controller.abort();
        controller = null;
        setIsRunning(false);
    }
};

export async function fetchMessages(
    threadId: string,
    setThreads: React.Dispatch<
        React.SetStateAction<Map<string, ThreadMessageLike[]>>
        >
) {
    try {
        const res = await fetch(`${apiBaseUrl}/chat/threads/${threadId}/messages`);
        if (!res.ok) throw new Error("获取消息失败");

        const data: ThreadMessageLike[] = await res.json();

        setThreads((prev: Map<string, ThreadMessageLike[]>) => {
            const newMap = new Map<string, ThreadMessageLike[]>(prev);
            newMap.set(threadId, data);
            return newMap;
        });

        return data;
    } catch (err) {
        console.error(err);
        return [];
    }
}

/**
 * 保存当前 partialAssistantMessage 及其前一条消息，并建立 parent_id 关系
 * @param threadId 线程 ID
 * @param baseMessages 当前已有的消息列表（不包含 partialAssistantMessage）
 * @param partialAssistantMessage 当前生成的助手消息
 */
async function saveChatMessages(threadId, baseMessages, partialAssistantMessage, userId) {
    const currentAssistantMsg = { ...partialAssistantMessage };
    const prevMessage = baseMessages.length > 0 ? { ...baseMessages[baseMessages.length - 1] } : null;
    const prevPrevMessage = baseMessages.length > 1 ? baseMessages[baseMessages.length - 2] : null;

    // 设置 parent_id 链接关系
    if (prevMessage) {
        currentAssistantMsg.parent_id = prevMessage.id;
        prevMessage.parent_id = prevPrevMessage ? prevPrevMessage.id : null;
    } else {
        currentAssistantMsg.parent_id = null;
    }

    // 并行保存到后端
    const saveRequests = [];

    if (prevMessage) {
        saveRequests.push(
            fetch(`${apiBaseUrl}/chat/threads/${threadId}/messages?userName=${userId}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(toBackendMessageFormat(prevMessage)),
            })
        );
    }

    saveRequests.push(
        fetch(`${apiBaseUrl}/chat/threads/${threadId}/messages?userName=${userId}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(toBackendMessageFormat(currentAssistantMsg)),
        })
    );

    try {
        await Promise.all(saveRequests);
        console.log("消息保存成功", { prevMessage, currentAssistantMsg });
    } catch (err) {
        console.error("保存消息失败:", err);
    }
}

function toBackendMessageFormat(msg) {
    return {
        messageId: msg.id,
        role: msg.role,
        content: Array.isArray(msg.content)
            ? msg.content.map(c => c.text).join("\n")
            : msg.content,
        type: Array.isArray(msg.content)
            ? msg.content.map(c => c.type).join("\n")
            : msg.content,
        parentId: msg.parent_id ?? null,
    };
}