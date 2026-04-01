<template>
  <div
    ref="chatRoot"
    class="chat-box"
    @focusin="handleFocusIn"
    @focusout="handleFocusOut"
  >
    <!-- Header -->
    <div class="chat-header">
      <span class="chat-title">💬 Chat</span>
      <span class="chat-dot" :class="{ online: isConnected }" :title="isConnected ? 'Connected' : 'Connecting…'"></span>
    </div>

    <!-- Message list -->
    <div class="chat-messages" ref="messagesEl">
      <div v-if="messages.length === 0" class="chat-empty">
        Game chat is ready. Say hi!
      </div>
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="chat-msg"
        :class="{ 'is-self': msg.userId === currentUserId }"
      >
        <span class="msg-name">{{ msg.userId === currentUserId ? 'You' : msg.username }}</span>
        <div class="msg-bubble">{{ msg.content }}</div>
      </div>
    </div>

    <!-- Input -->
    <div class="chat-input-row">
      <input
        v-model="inputText"
        class="chat-input"
        placeholder="Type a message…"
        maxlength="200"
        @keyup.enter="sendMessage"
        :disabled="!isConnected"
      />
      <button
        class="chat-send-btn"
        @click="sendMessage"
        :disabled="!isConnected || !inputText.trim()"
        title="Send (Enter)"
      >
        ↑
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted, nextTick } from "vue";
import { useStore } from "vuex";
import { buildChatWebSocketUrl } from "@/config/env";

const props = defineProps({
  roomId: { type: String, default: "" },
});
const emit = defineEmits(["activity-change"]);

const store = useStore();
const messages = ref([]);
const inputText = ref("");
const messagesEl = ref(null);
const chatRoot = ref(null);
const isConnected = ref(false);
let socket = null;
let blurTimer = null;

const currentUserId = computed(() => Number.parseInt(store.state.user.id, 10));

const scrollToBottom = async () => {
  await nextTick();
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight;
  }
};

const emitActivity = (active) => {
  emit("activity-change", active);
};

const handleFocusIn = () => {
  if (blurTimer) {
    clearTimeout(blurTimer);
    blurTimer = null;
  }
  emitActivity(true);
};

const handleFocusOut = () => {
  if (blurTimer) clearTimeout(blurTimer);
  blurTimer = setTimeout(() => {
    const root = chatRoot.value;
    const activeElement = document.activeElement;
    if (root && activeElement && root.contains(activeElement)) return;
    emitActivity(false);
  }, 0);
};

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
  messages.value = [];
  emitActivity(false);
};

const connectChat = (roomId) => {
  disconnectChat();
  if (!roomId) return;

  const token = store.state.user.token;
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
    } catch (e) {
      console.error("[ChatBox] parse error", e);
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

const sendMessage = () => {
  const content = inputText.value.trim();
  if (!content || !socket || socket.readyState !== WebSocket.OPEN) return;

  socket.send(
    JSON.stringify({
      content,
      username: store.state.user.username,
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

onUnmounted(disconnectChat);
onUnmounted(() => {
  if (blurTimer) clearTimeout(blurTimer);
  emitActivity(false);
});
</script>

<style scoped>
.chat-box {
  display: flex;
  flex-direction: column;
  width: 270px;
  flex-shrink: 0;
  border-radius: 18px;
  border: 1px solid var(--kob-panel-border);
  background: var(--kob-panel);
  box-shadow: 0 10px 28px rgba(0, 50, 100, 0.08);
  backdrop-filter: blur(12px);
  overflow: hidden;
}

/* ---- header ---- */
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

.chat-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #bbb;
  transition: background 0.3s;
}

.chat-dot.online {
  background: #27ae60;
  box-shadow: 0 0 6px rgba(39, 174, 96, 0.5);
}

/* ---- messages ---- */
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

/* ---- input ---- */
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
  transition: border-color 0.18s, box-shadow 0.18s;
}

.chat-input:focus {
  border-color: var(--kob-accent);
  box-shadow: 0 0 0 3px rgba(61, 174, 255, 0.15);
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
  font-size: 1rem;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
  cursor: pointer;
  transition: transform 150ms ease, opacity 150ms ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.chat-send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>

