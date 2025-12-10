import {ChatModelRunOptions, MessageStatus, TextMessagePart} from "@assistant-ui/react";

export const LocalAdapter: {
    run({
            messages,
            abortSignal,
            runConfig,
            context
        }: ChatModelRunOptions): Promise<{ metadata: {}; content: TextMessagePart[]; status: MessageStatus }>
} = {
    async run({ messages, abortSignal, runConfig, context }: ChatModelRunOptions) {
        const result = await fetch("http://localhost:8080/ai/generate", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
            },
            //body: JSON.stringify({ messages }),
            signal: abortSignal,
        });

        const data = await result;

        console.log("result", result)

        // 返回符合 ChatModelRunResult 的类型
        return {
            content: [
                {
                    type: "text",
                    text: "1233", // 确保 text 是 string 类型
                } as TextMessagePart, // 需要断言为正确的类型
            ],
            // 可以选择性添加其他字段
            status: "completed" as MessageStatus, // 根据实际情况设置状态
            metadata: {
                // 可选的元数据
                unstable_annotations:[{"type":"mem0-get","memories":[{"id":"236829a1-1921-45bd-97c3-e90b196072a5","memory":"Likes to eat","user_id":"8af28916-1848-4588-a45e-c814597f9d85","metadata":null,"categories":["user_preferences"],"created_at":"2025-09-24T19:46:35.034350-07:00","updated_at":"2025-09-26T05:25:21.513046-07:00","expiration_date":null,"structured_attributes":null,"score":0.6757646010190476},{"id":"8e85f1c4-89fa-42af-bc5a-61ec4ed6c752","memory":"Likes to travel","user_id":"8af28916-1848-4588-a45e-c814597f9d85","metadata":null,"categories":["user_preferences"],"created_at":"2025-09-25T02:03:07.537516-07:00","updated_at":"2025-09-25T02:03:07.622262-07:00","expiration_date":null,"structured_attributes":null,"score":0.16110894957658525}]}]
            }
        };
    },
};