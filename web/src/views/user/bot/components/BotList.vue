<template>
  <div class="card bot-main-card">
    <div class="card-header bot-main-header">
      <span class="title">我的 Bot</span>
      <button type="button" class="btn btn-primary" @click="openBotModal(null)">新建 Bot</button>
    </div>
    <div class="card-body">
      <table class="table table-striped table-hover">
        <thead>
          <tr>
            <th>名称</th>
            <th>语言</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="bot in bots" :key="bot.id">
            <td class="align-middle">{{ bot.title }}</td>
            <td class="align-middle">
              <span :class="['lang-badge', 'lang-' + (bot.language || 'java')]">
                {{ (bot.language || 'java').toUpperCase() }}
              </span>
            </td>
            <td class="align-middle">{{ bot.createtime }}</td>
            <td>
              <button type="button" class="btn btn-secondary me-2" @click="openBotModal(bot)">编辑</button>
              <button type="button" class="btn btn-danger" @click="removeBot(bot)">删除</button>
            </td>
          </tr>
          <tr v-if="bots.length === 0">
            <td colspan="4" class="text-center text-muted py-4">当前还没有 Bot，先创建一个吧。</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <div class="modal fade" ref="botModalRef" tabindex="-1">
    <div class="modal-dialog modal-xl">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">{{ botDraft.id ? "编辑 Bot" : "新建 Bot" }}</h5>
          <button type="button" class="btn-close" @click="closeModal(botModalRef)"></button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">名称 <span class="text-danger">*</span></label>
            <input v-model="botDraft.title" type="text" class="form-control" placeholder="请输入 Bot 名称" />
          </div>
          <div class="mb-3">
            <label class="form-label">语言 <span class="text-danger">*</span></label>
            <select v-model="botDraft.language" class="form-select" @change="onLanguageChange">
              <option value="java">Java</option>
              <option value="python">Python</option>
              <option value="cpp">C++</option>
              <option value="javascript">JavaScript</option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-label">描述</label>
            <textarea v-model="botDraft.description" class="form-control" rows="3" placeholder="简要描述 Bot 思路"></textarea>
          </div>
          <div class="mb-3">
            <label class="form-label">代码 <span class="text-danger">*</span></label>
            <VAceEditor
              v-model:value="botDraft.content"
              :lang="aceLang"
              theme="chrome"
              style="height: 400px; border: 1px solid #ddd; border-radius: 4px;"
              :options="{ fontSize: 14, showPrintMargin: false }"
            />
          </div>
        </div>
        <div class="modal-footer">
          <div class="error-message me-auto">{{ botDraft.error_message }}</div>
          <button type="button" class="btn btn-primary" @click="saveBot" :disabled="isSubmitting">
            {{ isSubmitting ? "保存中..." : "保存" }}
          </button>
          <button type="button" class="btn btn-secondary" @click="closeModal(botModalRef)">取消</button>
        </div>
      </div>
    </div>
  </div>

  <AppDialog
    v-model="dialogState.visible"
    :title="dialogState.title"
    :message="dialogState.message"
    :confirm-text="dialogState.confirmText"
    :cancel-text="dialogState.cancelText"
    :show-cancel="dialogState.showCancel"
    @confirm="handleDialogConfirm"
    @cancel="handleDialogCancel"
  />
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import Modal from "bootstrap/js/dist/modal";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";
import { useUserStore } from "@/store/user";
import AppDialog from "@/components/AppDialog.vue";
import { VAceEditor } from "vue3-ace-editor";
import "ace-builds/src-noconflict/mode-java";
import "ace-builds/src-noconflict/mode-python";
import "ace-builds/src-noconflict/mode-c_cpp";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-noconflict/theme-chrome";

const userStore = useUserStore();
const authToken = () => userStore.token;

const bots = ref([]);
const isSubmitting = ref(false);
const botModalRef = ref(null);

const dialogState = reactive({
  visible: false,
  title: "",
  message: "",
  confirmText: "确定",
  cancelText: "取消",
  showCancel: false,
});
let dialogResolver = null;

const botDraft = reactive({
  id: null,
  title: "",
  description: "",
  content: "",
  language: "java",
  error_message: "",
});

const aceLang = computed(() => {
  const map = { cpp: "c_cpp", java: "java", python: "python", javascript: "javascript" };
  return map[botDraft.language] || "java";
});

const openModal = (modalRef) => {
  if (modalRef) Modal.getOrCreateInstance(modalRef).show();
};

const closeModal = (modalRef) => {
  if (modalRef) Modal.getOrCreateInstance(modalRef).hide();
};

const openDialog = ({
  title = "提示",
  message = "",
  confirmText = "确定",
  cancelText = "取消",
  showCancel = false,
} = {}) => new Promise((resolve) => {
  if (dialogResolver) {
    const previousResolve = dialogResolver;
    dialogResolver = null;
    previousResolve(false);
  }
  dialogState.title = title;
  dialogState.message = message;
  dialogState.confirmText = confirmText;
  dialogState.cancelText = cancelText;
  dialogState.showCancel = showCancel;
  dialogState.visible = true;
  dialogResolver = resolve;
});

const settleDialog = (result) => {
  dialogState.visible = false;
  const resolve = dialogResolver;
  dialogResolver = null;
  if (resolve) resolve(result);
};

const handleDialogConfirm = () => settleDialog(true);
const handleDialogCancel = () => settleDialog(false);

const refreshBots = async () => {
  try {
    bots.value = await apiRequest(API_PATHS.botList, {
      token: authToken(),
      query: { _: Date.now() },
    });
  } catch (error) {
    console.error("Failed to load bots", error);
    throw error;
  }
};

onMounted(() => {
  refreshBots().catch(() => {});
});

const openBotModal = (bot = null) => {
  if (bot) {
    Object.assign(botDraft, { ...bot, error_message: "" });
  } else {
    Object.assign(botDraft, {
      id: null,
      title: "",
      description: "",
      language: "java",
      error_message: "",
      content: codePlaceholder("java"),
    });
  }
  openModal(botModalRef.value);
};

const saveBot = async () => {
  botDraft.error_message = "";
  if (!botDraft.title.trim()) {
    botDraft.error_message = "Bot 名称不能为空。";
    return;
  }
  if (!botDraft.content.trim()) {
    botDraft.error_message = "Bot代码不能为空。";
    return;
  }

  isSubmitting.value = true;
  const isEdit = !!botDraft.id;
  const targetApi = isEdit ? API_PATHS.botUpdate : API_PATHS.botAdd;
  const payload = {
    title: botDraft.title,
    description: botDraft.description,
    content: botDraft.content,
    language: botDraft.language || "java",
  };
  if (isEdit) payload.bot_id = botDraft.id;

  try {
    const resp = await apiRequest(targetApi, {
      method: "POST",
      token: authToken(),
      data: payload,
    });

    if (resp.error_message !== "success") {
      botDraft.error_message = resp.error_message || "保存失败";
      return;
    }

    if (isEdit) {
      const index = bots.value.findIndex((b) => b.id === botDraft.id);
      if (index !== -1) {
        bots.value[index] = { ...bots.value[index], ...payload };
      } else {
        await refreshBots();
      }
    } else {
      const now = new Date().toISOString().slice(0, 19).replace("T", " ");
      const tempId = `tmp-${Date.now()}`;
      bots.value = [{ id: tempId, ...payload, createtime: now }, ...bots.value];
      try {
        await refreshBots();
      } catch {
        bots.value = bots.value.filter((b) => !String(b.id).startsWith("tmp-"));
        botDraft.error_message = "列表刷新失败，请稍后重试。";
        return;
      }
    }

    closeModal(botModalRef.value);
  } catch (error) {
    botDraft.error_message = error.message || "网络异常";
  } finally {
    isSubmitting.value = false;
  }
};

const removeBot = async (bot) => {
  const confirmed = await openDialog({
    title: "删除 Bot",
    message: `确定删除 Bot「${bot.title}」吗？`,
    confirmText: "删除",
    cancelText: "取消",
    showCancel: true,
  });
  if (!confirmed) return;

  try {
    const resp = await apiRequest(API_PATHS.botRemove, {
      method: "POST",
      token: authToken(),
      data: { bot_id: bot.id },
    });
    if (resp.error_message === "success") {
      bots.value = bots.value.filter((b) => b.id !== bot.id);
    } else {
      await openDialog({
        title: "删除失败",
        message: resp.error_message || "删除失败",
      });
    }
  } catch {
    await openDialog({
      title: "删除失败",
      message: "删除请求失败，请稍后重试。",
    });
  }
};

const onLanguageChange = () => {
  if (botDraft.id) return;
  const placeholders = [
    codePlaceholder("java"),
    codePlaceholder("python"),
    codePlaceholder("cpp"),
    codePlaceholder("javascript"),
  ];
  if (!botDraft.content || placeholders.includes(botDraft.content)) {
    botDraft.content = codePlaceholder(botDraft.language || "java");
  }
};

const codePlaceholder = (lang) => {
  const map = {
    java: `import java.util.Scanner;\nimport java.io.File;\n\npublic class Bot implements java.util.function.Supplier<Integer> {\n    @Override\n    public Integer get() {\n        // 返回方向：0=上，1=右，2=下，3=左\n        return 0;\n    }\n}`,
    python: `# 返回方向：0=上，1=右，2=下，3=左\n\ndirection = 0\nprint(direction)`,
    cpp: `#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    // 返回方向：0=上，1=右，2=下，3=左\n    cout << 0 << endl;\n    return 0;\n}`,
    javascript: `"use strict";\n// 返回方向：0=上，1=右，2=下，3=左\nconsole.log(0);`,
  };
  return map[lang] || map.java;
};
</script>

<style scoped>
.bot-main-card {
  background: var(--kob-panel);
  border: 1px solid var(--kob-panel-border);
  box-shadow: 0 10px 30px rgba(0, 50, 100, 0.08);
  backdrop-filter: blur(12px);
}

.bot-main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bot-main-header .title {
  font-family: "Space Grotesk", sans-serif;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--kob-text);
}

.modal-content {
  background: #fff;
  border: 1px solid rgba(90, 180, 255, 0.3);
  color: var(--kob-text);
}

.modal-header .btn-close {
  filter: none;
}

.error-message {
  color: #e74c3c;
  min-height: 20px;
}

.lang-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.lang-java {
  background: rgba(237, 129, 50, 0.15);
  color: #c25a00;
}

.lang-python {
  background: rgba(55, 118, 171, 0.15);
  color: #1e6fa6;
}

.lang-cpp {
  background: rgba(100, 54, 180, 0.15);
  color: #5c2eab;
}

.lang-javascript {
  background: rgba(202, 163, 10, 0.15);
  color: #8a6800;
}
</style>
