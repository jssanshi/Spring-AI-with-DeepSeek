"use client";

import {
  AppendMessage,
  AssistantRuntimeProvider,
  ExternalStoreThreadData, ExternalStoreThreadListAdapter, ThreadMessageLike, useExternalStoreRuntime
} from "@assistant-ui/react";
import { Thread } from "@/components/assistant-ui/thread";
import { ThreadList } from "@/components/assistant-ui/thread-list";
import { useEffect, useState } from "react";
import { v4 as uuidv4 } from "uuid";
import { Sun, Moon, AlignJustify } from "lucide-react";
import { Button } from "@/components/ui/button";
import ThemeAwareLogo from "@/components/mem0/theme-aware-logo";
import Link from "next/link";
import GithubButton from "@/components/mem0/github-button";
import {cancelAssistantStream, fetchMessages, runAssistantStream} from "@/app/ExternalRuntime";
import {useThreadContext} from "@/components/assistant-ui/thread-context";

export const useUserId = () => {
  const [userId, setUserId] = useState<string>("");

  useEffect(() => {
    let id = localStorage.getItem("userId");
    if (!id) {
      //id = uuidv4();
      id = "user1";
      localStorage.setItem("userId", id);
    }
    setUserId(id);
  }, []);

  const resetUserId = () => {
    const newId = uuidv4();
    localStorage.setItem("userId", newId);
    setUserId(newId);
    // Clear all threads from localStorage
    const keys = Object.keys(localStorage);
    keys.forEach(key => {
      if (key.startsWith('thread:')) {
        localStorage.removeItem(key);
      }
    });
    // Force reload to clear all states
    window.location.reload();
  };

  return { userId, resetUserId };
};

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;

export const Assistant = () => {
  const { userId, resetUserId } = useUserId();
/*  const runtime = useChatRuntime({
    transport: new AssistantChatTransport({
      api: "http://localhost:8080/ai/chat/stream",
      body: { userId }
    }),
  });

  const runtime_local = useLocalRuntime(LocalAdapter);
  */

  const [isDarkMode, setIsDarkMode] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const toggleDarkMode = () => {
    setIsDarkMode(!isDarkMode);
    if (!isDarkMode) {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
  };

  const [messages, setMessages] = useState<readonly ThreadMessageLike[]>([]);
  const [isRunning, setIsRunning] = useState(false);

  const onNew = async (message: AppendMessage) => {
    if (message.content.length !== 1 || message.content[0]?.type !== "text") {
      throw new Error("Only text content is supported");
    }

    const userMessage: ThreadMessageLike = {
      role: "user",
      content: [{ type: "text", text: message.content[0].text }],
      id: crypto.randomUUID(),
    };
    const msgs = threads.get(currentThreadId) ?? [];
    const newMessages = [...msgs, userMessage];

    await runAssistantStream(message.content[0].text, currentThreadId, newMessages, setThreads, setMessages, setIsRunning, setThreadList, setCurrentThreadId, userId);
  };

  const onReload = async (parentId: string | null) => {
    if (!parentId) return;

    const idx = messages.findIndex((m) => m.id === parentId);
    if (idx === -1) return;

    const parentMessage = messages[idx];
    if (parentMessage.role !== "user") return;
    const baseMessages = messages.slice(0, idx + 1);

    await runAssistantStream(parentMessage.content[0].text, currentThreadId, baseMessages, setThreads, setMessages, setIsRunning, setThreadList, setCurrentThreadId, userId);
  };

  const onEdit = async (message: AppendMessage) => {
    if (message.content.length !== 1 || message.content[0]?.type !== "text") {
      throw new Error("Only text content is supported");
    }

    const newText = message.content[0].text;

    // 找到被编辑的消息（假设 message.id 就是原消息的 id）
    const idx = messages.findIndex((m) => m.id === message.sourceId);
    if (idx === -1) {
      console.warn("Edited message not found:", message.sourceId);
      return;
    }

    const oldMessage = messages[idx];
    if (oldMessage.role !== "user") {
      console.warn("Only user messages can be edited");
      return;
    }

    // 替换 user 消息内容
    const updatedUserMessage: ThreadMessageLike = {
      ...oldMessage,
      content: [{ type: "text", text: newText }],
    };
    const baseMessages = [...messages.slice(0, idx), updatedUserMessage];

    await runAssistantStream(message.content[0].text, currentThreadId, baseMessages, setThreads, setMessages, setIsRunning, setThreadList, setCurrentThreadId, userId);
  };

  const onCancel = async () => {
    await cancelAssistantStream(setIsRunning);
  };

  const convertMessage = (message: ThreadMessageLike) => {
    return message;
  };

  const { currentThreadId, setCurrentThreadId, threads, setThreads } =
      useThreadContext();

  const [threadList, setThreadList] = useState<
      (ExternalStoreThreadData<"regular"> | ExternalStoreThreadData<"archived">)[]
      >([]);

  //在组件挂载时从后端获取线程列表
  useEffect(() => {
    if (!userId) return;

    const fetchThreads = async () => {
      try {
        const response = await fetch(`${apiBaseUrl}/chat/threads?userName=${userId}`);
        if (!response.ok) {
          throw new Error(`加载对话列表失败: ${response.status}`);
        }
        const data = await response.json();
        setThreadList(data);
      } catch (error) {
        console.error("获取对话列表失败:", error);
      }
    };

    fetchThreads();
  }, [userId]); //仅在组件首次加载时调用一次

  const threadListAdapter: ExternalStoreThreadListAdapter = {
    threadId: currentThreadId,
    threads: threadList.filter(
        (t): t is ExternalStoreThreadData<"regular"> => t.status === "regular"
    ),
    archivedThreads: threadList.filter(
        (t): t is ExternalStoreThreadData<"archived"> => t.status === "archived"
    ),
    onArchive: (id) => {
      setThreadList((prev) =>
          prev.map((t) =>
              t.id === id ? { ...t, status: "archived" } : t,
          ),
      );
    },
    onSwitchToNewThread: async () => {
/*      const newId = `thread-${Date.now()}`;
      console.log("New thread created:", newId)
      setThreadList((prev) => [
        ...prev,
        {
          id: newId,
          status: "regular",
          title: "New Chat999",
        }
      ]);

      setThreads((prev) => new Map(prev).set(newId, []));
      setCurrentThreadId(newId);*/
      setCurrentThreadId("default")
      setMessages([])
    },
    onSwitchToThread: async (threadId) => {
      setCurrentThreadId(threadId);

      let msgs = threads.get(threadId);
      if (!msgs) {
        msgs = await fetchMessages(threadId, setThreads);
      }

      // 更新 ExternalStore 的消息状态
      setMessages(msgs);
    },
  };

  const runtime_ex_store = useExternalStoreRuntime<ThreadMessageLike>({
    messages,
    setMessages,
    onNew,
    convertMessage,
    onEdit,
    onReload,
    onCancel,
    isRunning,
    adapters: {
      threadList: threadListAdapter,
    },
  });

  return (
    <AssistantRuntimeProvider runtime={runtime_ex_store}>
      <div className={`bg-[#f8fafc] dark:bg-zinc-900 text-[#1e293b] ${isDarkMode ? "dark" : ""}`}>
        <header className="h-16 border-b border-[#e2e8f0] flex items-center justify-between px-4 sm:px-6 bg-white dark:bg-zinc-900 dark:border-zinc-800 dark:text-white">
          <div className="flex items-center">
          <Link href="/" className="flex items-center">
            <ThemeAwareLogo width={120} height={40} isDarkMode={isDarkMode} />
          </Link>
          </div>

          <Button
              variant="ghost"
              size="sm"
              onClick={() => setSidebarOpen(true)}
              className="text-[#475569] dark:text-zinc-300 md:hidden"
            >
              <AlignJustify size={24} className="md:hidden" />
          </Button>


          <div className="md:flex items-center hidden">
            <button
              className="p-2 rounded-full hover:bg-[#eef2ff] dark:hover:bg-zinc-800 text-[#475569] dark:text-zinc-300"
              onClick={toggleDarkMode}
              aria-label="Toggle theme"
            >
              {isDarkMode ? <Sun className="w-6 h-6" /> : <Moon className="w-6 h-6" />}
            </button>
            <GithubButton url="https://github.com/mem0ai/mem0/tree/main/examples" />
          </div>
        </header>
        <div className="grid grid-cols-1 md:grid-cols-[260px_1fr] gap-x-0 h-[calc(100dvh-4rem)]">
          <ThreadList onResetUserId={resetUserId} isDarkMode={isDarkMode} />
          <Thread sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} onResetUserId={resetUserId} isDarkMode={isDarkMode} toggleDarkMode={toggleDarkMode} />
        </div>
      </div>
    </AssistantRuntimeProvider>
  );
};
