<template>
  <div v-if="modelValue" class="chat-drawer-backdrop" @click.self="$emit('update:modelValue', false)">
    <section class="chat-drawer" role="dialog" aria-modal="true" aria-labelledby="friend-chat-title">
      <aside class="chat-drawer__rail">
        <header class="chat-drawer__rail-head">
          <div>
            <p class="chat-drawer__kicker">Private Chat</p>
            <h3>好友私聊</h3>
          </div>
          <button type="button" class="chat-drawer__close mobile-only" aria-label="关闭私聊面板" @click="$emit('update:modelValue', false)">
            ×
          </button>
        </header>

        <div v-if="loadingConversations" class="chat-drawer__conversation-list chat-drawer__conversation-list--loading" aria-hidden="true">
          <span v-for="index in 5" :key="`chat-conv-loading-${index}`" class="chat-conv-loading"></span>
        </div>

        <div v-else class="chat-drawer__conversation-list">
          <button
            v-for="conversation in conversations"
            :key="conversation.friendId"
            type="button"
            class="chat-conversation"
            :class="{ active: activeFriend?.id === conversation.friendId }"
            @click="$emit('select-friend', conversation.friendId)"
          >
            <img :src="conversation.photo" :alt="conversation.name" class="chat-conversation__avatar" />
            <div class="chat-conversation__copy">
              <div class="chat-conversation__row">
                <strong>{{ conversation.name }}</strong>
                <span>{{ conversation.lastMessageAt }}</span>
              </div>
              <p>{{ conversation.lastMessage || "还没有消息，先打个招呼吧。" }}</p>
              <div class="chat-conversation__meta">
                <span>{{ conversation.statusLabel }}</span>
                <span v-if="conversation.unreadCount > 0" class="chat-conversation__badge">
                  {{ conversation.unreadCount }}
                </span>
              </div>
            </div>
          </button>

          <div v-if="!conversations.length" class="chat-conversation__empty">
            <strong>还没有会话</strong>
            <p>从好友详情里打开私聊后，这里会显示会话列表。</p>
          </div>
        </div>
      </aside>

      <div class="chat-drawer__thread">
        <header class="chat-thread__head">
          <div v-if="activeFriend" class="chat-thread__friend">
            <img :src="activeFriend.photo" :alt="activeFriend.name" class="chat-thread__avatar" />
            <div>
              <p class="chat-drawer__kicker">Conversation</p>
              <h3 id="friend-chat-title">{{ activeFriend.name }}</h3>
              <p>{{ activeConversation?.statusLabel || activeFriend.statusLabel }} · 积分 {{ activeFriend.rating }}</p>
            </div>
          </div>
          <button type="button" class="chat-drawer__close" aria-label="关闭私聊面板" @click="$emit('update:modelValue', false)">
            ×
          </button>
        </header>

        <div v-if="loadingMessages" class="chat-thread__body chat-thread__body--loading" aria-hidden="true">
          <span v-for="index in 6" :key="`chat-msg-loading-${index}`" class="chat-message-loading" :class="{ 'is-self': index % 2 === 0 }"></span>
        </div>

        <div v-else ref="messagesEl" class="chat-thread__body">
          <div v-if="!messages.length" class="chat-thread__empty">
            <strong>还没有聊天记录</strong>
            <p>发送第一条消息吧。</p>
          </div>

          <article
            v-for="message in messages"
            :key="message.id"
            class="chat-message"
            :class="{ 'is-self': message.senderId === currentUserId }"
          >
            <img
              :src="message.senderId === currentUserId ? currentUserPhoto : message.senderPhoto"
              :alt="message.senderName"
              class="chat-message__avatar"
            />
            <div class="chat-message__bubble-wrap">
              <span class="chat-message__name">
                {{ message.senderId === currentUserId ? "我" : message.senderName }}
              </span>
              <div class="chat-message__bubble">{{ message.content }}</div>
              <span class="chat-message__time">{{ message.createdAt }}</span>
            </div>
          </article>
        </div>

        <footer class="chat-thread__composer">
          <label class="chat-thread__field">
            <span>消息内容</span>
            <textarea
              :value="draft"
              class="chat-thread__input"
              rows="3"
              maxlength="500"
              placeholder="输入消息..."
              @input="$emit('update:draft', $event.target.value)"
              @keydown.enter.exact.prevent="$emit('send')"
            ></textarea>
          </label>
          <div class="chat-thread__actions">
            <small>Enter 发送，Shift + Enter 换行</small>
            <button type="button" class="btn btn-primary chat-thread__send" :disabled="sending || !draft.trim()" @click="$emit('send')">
              {{ sending ? "发送中..." : "发送" }}
            </button>
          </div>
        </footer>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from "vue";
import { useUserStore } from "@/store/user";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  conversations: {
    type: Array,
    default: () => [],
  },
  activeFriend: {
    type: Object,
    default: null,
  },
  activeConversation: {
    type: Object,
    default: null,
  },
  messages: {
    type: Array,
    default: () => [],
  },
  draft: {
    type: String,
    default: "",
  },
  loadingConversations: {
    type: Boolean,
    default: false,
  },
  loadingMessages: {
    type: Boolean,
    default: false,
  },
  sending: {
    type: Boolean,
    default: false,
  },
});

defineEmits(["update:modelValue", "select-friend", "update:draft", "send"]);

const userStore = useUserStore();
const messagesEl = ref(null);
const currentUserId = computed(() => Number.parseInt(userStore.id, 10));
const currentUserPhoto = computed(() => userStore.photo);

const scrollToBottom = async () => {
  await nextTick();
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight;
  }
};

watch(
  () => [props.messages.length, props.activeFriend?.id],
  () => {
    scrollToBottom();
  },
  { flush: "post" }
);
</script>

<style scoped>
.chat-drawer-backdrop {
  position: fixed;
  inset: 0;
  z-index: 3350;
  padding: 18px;
  background: rgba(6, 20, 32, 0.38);
  backdrop-filter: blur(10px);
}

.chat-drawer {
  width: min(1280px, calc(100vw - 36px));
  height: min(88vh, 920px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  border-radius: 34px;
  overflow: hidden;
  border: 1px solid rgba(90, 180, 255, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(242, 249, 255, 0.92)),
    radial-gradient(circle at top right, rgba(255, 208, 93, 0.14), transparent 28%);
  box-shadow: 0 32px 84px rgba(0, 36, 70, 0.22);
}

.chat-drawer__rail {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-right: 1px solid rgba(90, 180, 255, 0.12);
  background:
    linear-gradient(180deg, rgba(247, 251, 255, 0.92), rgba(238, 246, 255, 0.86));
}

.chat-drawer__rail-head,
.chat-thread__head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  padding: 22px;
  border-bottom: 1px solid rgba(90, 180, 255, 0.12);
}

.chat-drawer__kicker {
  margin: 0 0 8px;
  color: var(--kob-accent-strong);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.chat-drawer__rail-head h3,
.chat-thread__head h3 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  color: var(--kob-text);
}

.chat-drawer__close {
  width: 42px;
  height: 42px;
  border: 1px solid rgba(90, 180, 255, 0.16);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.78);
  color: var(--kob-text);
  font-size: 1.2rem;
  line-height: 1;
}

.mobile-only {
  display: none;
}

.chat-drawer__conversation-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-conv-loading,
.chat-message-loading {
  position: relative;
  overflow: hidden;
  border-radius: 22px;
  background: rgba(220, 232, 245, 0.88);
}

.chat-conv-loading::after,
.chat-message-loading::after {
  content: "";
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.78), transparent);
  animation: chat-shimmer 1.4s ease-in-out infinite;
}

.chat-conv-loading {
  min-height: 88px;
}

.chat-conversation {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 14px;
  border-radius: 22px;
  border: 1px solid rgba(90, 180, 255, 0.14);
  background: rgba(255, 255, 255, 0.8);
  text-align: left;
  transition:
    transform var(--motion-fast) var(--ease-out),
    border-color var(--motion-fast) var(--ease-out),
    box-shadow var(--motion-fast) var(--ease-out);
}

.chat-conversation:hover,
.chat-conversation.active {
  transform: translateY(-1px);
  border-color: rgba(61, 174, 255, 0.28);
  box-shadow: 0 14px 30px rgba(0, 50, 100, 0.08);
}

.chat-conversation__avatar,
.chat-thread__avatar,
.chat-message__avatar {
  object-fit: cover;
  border: 1px solid rgba(90, 180, 255, 0.16);
}

.chat-conversation__avatar {
  width: 58px;
  height: 58px;
  border-radius: 18px;
}

.chat-conversation__copy {
  min-width: 0;
}

.chat-conversation__row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.chat-conversation__row strong {
  min-width: 0;
  color: var(--kob-text);
  font-size: 0.95rem;
}

.chat-conversation__row span,
.chat-thread__friend p,
.chat-message__time {
  color: var(--kob-muted);
  font-size: 0.74rem;
}

.chat-conversation p {
  margin: 8px 0 0;
  color: var(--kob-muted);
  line-height: 1.55;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-conversation__meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  margin-top: 10px;
}

.chat-conversation__meta span {
  font-size: 0.74rem;
  color: var(--kob-text);
}

.chat-conversation__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  min-height: 24px;
  padding: 0 7px;
  border-radius: 999px;
  background: linear-gradient(135deg, #f97316, #ef4444);
  color: #fff !important;
  font-weight: 700;
}

.chat-conversation__empty,
.chat-thread__empty {
  padding: 18px;
  border-radius: 22px;
  border: 1px dashed rgba(90, 180, 255, 0.22);
  background: rgba(249, 252, 255, 0.82);
}

.chat-conversation__empty strong,
.chat-thread__empty strong {
  display: block;
  color: var(--kob-text);
}

.chat-conversation__empty p,
.chat-thread__empty p {
  margin: 8px 0 0;
  color: var(--kob-muted);
  line-height: 1.65;
}

.chat-drawer__thread {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-thread__friend {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.chat-thread__avatar {
  width: 68px;
  height: 68px;
  border-radius: 22px;
}

.chat-thread__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.46), rgba(248, 252, 255, 0.82)),
    radial-gradient(circle at top left, rgba(61, 174, 255, 0.08), transparent 30%);
}

.chat-thread__body--loading {
  gap: 12px;
}

.chat-message-loading {
  width: min(62%, 420px);
  min-height: 72px;
}

.chat-message-loading.is-self {
  align-self: flex-end;
}

.chat-message {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  max-width: min(78%, 520px);
}

.chat-message.is-self {
  align-self: flex-end;
  grid-template-columns: minmax(0, 1fr) auto;
}

.chat-message__avatar {
  width: 42px;
  height: 42px;
  border-radius: 14px;
}

.chat-message.is-self .chat-message__avatar {
  order: 2;
}

.chat-message__bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chat-message__name {
  color: var(--kob-muted);
  font-size: 0.76rem;
  font-weight: 700;
}

.chat-message.is-self .chat-message__bubble-wrap {
  align-items: flex-end;
}

.chat-message__bubble {
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--kob-text);
  line-height: 1.68;
  box-shadow: 0 10px 22px rgba(0, 50, 100, 0.06);
}

.chat-message.is-self .chat-message__bubble {
  background: linear-gradient(135deg, rgba(61, 174, 255, 0.16), rgba(120, 216, 255, 0.12));
}

.chat-thread__composer {
  padding: 20px 22px 22px;
  border-top: 1px solid rgba(90, 180, 255, 0.12);
  background: rgba(255, 255, 255, 0.78);
}

.chat-thread__field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-thread__field span {
  color: var(--kob-muted);
  font-size: 0.78rem;
  font-weight: 700;
}

.chat-thread__input {
  min-height: 116px;
  resize: none;
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid rgba(90, 180, 255, 0.18);
  background: rgba(255, 255, 255, 0.92);
  color: var(--kob-text);
  line-height: 1.65;
}

.chat-thread__actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 14px;
}

.chat-thread__actions small {
  color: var(--kob-muted);
}

.chat-thread__send {
  min-width: 140px;
  min-height: 46px;
  border-radius: 999px;
}

@keyframes chat-shimmer {
  to {
    transform: translateX(100%);
  }
}

@media (max-width: 991px) {
  .chat-drawer {
    width: calc(100vw - 24px);
    height: calc(100vh - 24px);
    grid-template-columns: 1fr;
  }

  .chat-drawer__rail {
    border-right: 0;
    border-bottom: 1px solid rgba(90, 180, 255, 0.12);
  }

  .chat-drawer__conversation-list {
    max-height: 220px;
  }

  .mobile-only {
    display: inline-flex;
  }
}

@media (max-width: 767px) {
  .chat-drawer-backdrop {
    padding: 12px;
  }

  .chat-drawer {
    width: 100%;
    height: calc(100vh - 24px);
  }

  .chat-thread__head,
  .chat-drawer__rail-head,
  .chat-thread__body,
  .chat-thread__composer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .chat-message {
    max-width: 100%;
  }

  .chat-thread__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .chat-thread__send {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .chat-conversation,
  .chat-conv-loading::after,
  .chat-message-loading::after {
    transition: none !important;
    animation: none !important;
  }
}
</style>
