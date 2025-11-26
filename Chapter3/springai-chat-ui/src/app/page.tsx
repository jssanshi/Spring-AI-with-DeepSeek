"use client";

import { Thread } from "@/components/assistant-ui/thread";
import { AssistantRuntimeProvider } from "@assistant-ui/react";
import { useChatRuntime } from "@assistant-ui/react-ai-sdk";
import { AssistantChatTransport } from "@assistant-ui/react-ai-sdk";

export default function Home() {
    const runtime = useChatRuntime({
        transport: new AssistantChatTransport({
            api: "http://localhost:8080/ai/chat/stream"
        }),
    });
    return (
        <AssistantRuntimeProvider runtime={runtime}>
            <div className="h-full">
                <Thread />
            </div>
        </AssistantRuntimeProvider>
    );
}