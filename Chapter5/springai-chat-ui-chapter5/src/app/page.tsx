import { Assistant } from "@/app/assistant"
import {ThreadProvider} from "@/components/assistant-ui/thread-context";

export default function Page() {
  return (
      <ThreadProvider>
        <Assistant />
      </ThreadProvider>
  );
}