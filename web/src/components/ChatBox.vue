<template>
  <div ref="chatRoot" class="chat-box" @focusin="handleFocusIn" @focusout="handleFocusOut">
    <div class="chat-header">
      <span class="chat-title">对局聊天</span>
      <div class="chat-indicators">
        <span v-if="isInputFocused" class="chat-focus">输入中</span>
        <span
          class="chat-dot"
          :class="{ online: isConnected }"
          :title="isConnected ? '已连接' : '连接中'"
        ></span>
      </div>
    </div>

    <div ref="messagesEl" class="chat-messages">
      <div v-if="messages.length === 0" class="chat-empty">聊天已就绪，发条消息吧。</div>
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="chat-msg"
        :class="{ 'is-self': msg.userId === currentUserId }"
      >
        <span class="msg-name">{{ msg.userId === currentUserId ? '我' : msg.username }}</span>
        <div class="msg-bubble">{{ msg.content }}</div>
      </div>
    </div>

    <div class="chat-input-row">
      <input
        v-model="inputText"
        class="chat-input"
        placeholder="输入消息..."
        maxlength="200"
        :disabled="!isConnected"
        @keyup.enter="sendMessage"
      />
      <button
        class="chat-send-btn"
        :disabled="!isConnected || !inputText.trim()"
        title="发送（Enter）"
        @click="sendMessage"
      >
        发
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onUnmounted, ref, watch } from "vue";
import { useUserStore } from "@/store/user";
import { buildChatWebSocketUrl } from "@/config/env";

const props = defineProps({
  roomId: { type: String, default: "" },
});

const userStore = useUserStore();
const messages = ref([]);
const inputText = ref("");
const messagesEl = ref(null);
const chatRoot = ref(null);
const isConnected = ref(false);
const isInputFocused = ref(false);

let socket = null;
let blurTimer = null;

const currentUserId = computed(() => Number.parseInt(userStore.id, 10));

/**
 * 处理 scrollToBottom 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of scrollToBottom with async flow control, including state updates, interaction orchestration, and error branches.
 *
 */
const scrollToBottom = async () => {
  await nextTick();
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight;
  }
};

/**
 * 处理 handleFocusIn 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of handleFocusIn, including state updates, interaction orchestration, and error branches.
 */
const handleFocusIn = () => {
  if (blurTimer) {
    clearTimeout(blurTimer);
    blurTimer = null;
  }
  isInputFocused.value = true;
};

/**
 * 处理 handleFocusOut 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of handleFocusOut, including state updates, interaction orchestration, and error branches.
 */
const handleFocusOut = () => {
  if (blurTimer) clearTimeout(blurTimer);
  blurTimer = setTimeout(() => {
    const root = chatRoot.value;
    const activeElement = document.activeElement;
    isInputFocused.value = !!(root && activeElement && root.contains(activeElement));
  }, 0);
};

/**
 * 处理 disconnectChat 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of disconnectChat, including state updates, interaction orchestration, and error branches.
 */
const disconnectChat = () => {
  if (socket) {
    socket.onopen = null;
    socket.onmessage = null;
    socket.onclose = null;
    socket.onerror = null;
    if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
      socket.close();
    }
    socket = null;
  }
  isConnected.value = false;
  isInputFocused.value = false;
  messages.value = [];
};

/**
 * 处理 connectChat 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of connectChat, including state updates, interaction orchestration, and error branches.
 */
const connectChat = (roomId) => {
  disconnectChat();
  if (!roomId) return;

  const token = userStore.token;
  if (!token) return;

  socket = new WebSocket(buildChatWebSocketUrl(token, roomId));

  socket.onopen = () => {
    isConnected.value = true;
  };

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      messages.value.push(data);
      scrollToBottom();
    } catch (error) {
      console.error("[ChatBox] parse error", error);
    }
  };

  socket.onclose = () => {
    isConnected.value = false;
    socket = null;
  };

  socket.onerror = () => {
    isConnected.value = false;
  };
};

/**
 * 处理 sendMessage 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of sendMessage, including state updates, interaction orchestration, and error branches.
 */
const sendMessage = () => {
  const content = inputText.value.trim();
  if (!content || !socket || socket.readyState !== WebSocket.OPEN) return;

  socket.send(
    JSON.stringify({
      content,
      username: userStore.username,
    })
  );
  inputText.value = "";
};

watch(
  () => props.roomId,
  (newRoomId) => {
    if (newRoomId) {
      connectChat(newRoomId);
    } else {
      disconnectChat();
    }
  },
  { immediate: true }
);

onUnmounted(() => {
  disconnectChat();
  if (blurTimer) clearTimeout(blurTimer);
});
</script>

<style scoped>
.chat-box {
  display: flex;
  flex-direction: column;
  width: min(320px, 30vw);
  min-width: 250px;
  flex-shrink: 0;
  border-radius: var(--kob-radius-md);
  border: 1px solid rgba(90, 180, 255, 0.24);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(244, 250, 255, 0.84)),
    radial-gradient(circle at top right, rgba(61, 174, 255, 0.08), transparent 40%);
  box-shadow: var(--kob-shadow-sm);
  backdrop-filter: blur(12px);
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 11px 16px;
  border-bottom: 1px solid rgba(90, 180, 255, 0.2);
  background: rgba(61, 174, 255, 0.07);
  flex-shrink: 0;
}

.chat-title {
  font-family: "Space Grotesk", sans-serif;
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--kob-text);
}

.chat-indicators {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-focus {
  border-radius: 999px;
  padding: 2px 8px;
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-accent-strong);
  font-size: 0.72rem;
  font-weight: 600;
  line-height: 1;
}

.chat-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #bbb;
  transition: background var(--motion-fast) var(--ease-out);
}

.chat-dot.online {
  background: #27ae60;
  box-shadow: 0 0 6px rgba(39, 174, 96, 0.5);
}

.chat-messages {
  flex: 1 1 0;
  overflow-y: auto;
  padding: 12px 12px 6px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-messages::-webkit-scrollbar {
  width: 4px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(90, 180, 255, 0.3);
  border-radius: 4px;
}

.chat-empty {
  text-align: center;
  color: var(--kob-muted);
  font-size: 0.8rem;
  margin-top: 24px;
}

.chat-msg {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 88%;
}

.chat-msg.is-self {
  align-self: flex-end;
  align-items: flex-end;
}

.msg-name {
  font-size: 0.7rem;
  color: var(--kob-muted);
  margin-bottom: 3px;
  padding: 0 4px;
}

.msg-bubble {
  background: rgba(90, 180, 255, 0.13);
  border-radius: 14px 14px 14px 4px;
  padding: 7px 12px;
  font-size: 0.875rem;
  color: var(--kob-text);
  line-height: 1.4;
  word-break: break-word;
}

.chat-msg.is-self .msg-bubble {
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
  color: #fff;
  border-radius: 14px 14px 4px 14px;
}

.chat-input-row {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid rgba(90, 180, 255, 0.2);
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  border: 1px solid rgba(90, 180, 255, 0.35);
  border-radius: 999px;
  padding: 7px 14px;
  font-size: 0.85rem;
  color: var(--kob-text);
  background: #fff;
  outline: none;
  transition:
    border-color var(--motion-fast) var(--ease-out),
    box-shadow var(--motion-fast) var(--ease-out);
}

.chat-input:focus {
  border-color: var(--kob-accent);
  box-shadow: 0 0 0 3px rgba(61, 174, 255, 0.15);
}

.chat-input:focus-visible {
  outline: none;
}

.chat-input:disabled {
  opacity: 0.5;
}

.chat-send-btn {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border: none;
  border-radius: 50%;
  font-size: 0.85rem;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
  cursor: pointer;
  transition:
    transform var(--motion-fast) var(--ease-out),
    opacity var(--motion-fast) var(--ease-out),
    box-shadow var(--motion-fast) var(--ease-out);
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.chat-send-btn:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 3px rgba(255, 255, 255, 0.9),
    0 0 0 6px rgba(61, 174, 255, 0.28);
}

.chat-send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@media (max-width: 991px) {
  .chat-box {
    width: 100%;
    min-width: 0;
    border-radius: 16px;
  }

  .chat-messages {
    padding: 10px 10px 4px;
  }
}
</style>
