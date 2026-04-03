<template>
  <div class="container bot-page">
    <div class="row g-3">
      <div class="col-lg-3 col-md-4">
        <div class="card bot-profile-card">
          <div class="card-body photo-section">
            <div class="avatar-wrap" @click="triggerUpload">
              <img :src="store.state.user.photo" alt="avatar" class="profile-avatar" />
              <div class="avatar-overlay">
                <span v-if="!uploading">Click to change</span>
                <span v-else>Uploading...</span>
              </div>
            </div>
            <p class="profile-name">{{ store.state.user.username }}</p>
            <div class="profile-actions mt-3">
              <button class="btn btn-sm btn-outline-primary mb-2 w-100" @click="openModal(usernameModalRef)">Modify Username</button>
              <button class="btn btn-sm btn-outline-warning w-100" @click="openModal(passwordModalRef)">Modify Password</button>
            </div>
            <input type="file" ref="fileInput" @change="uploadAvatar" accept="image/png, image/jpeg, image/jpg" style="display: none;" />
          </div>
        </div>
      </div>

      <div class="col-lg-9 col-md-8">
        <div class="card bot-main-card">
          <div class="card-header bot-main-header">
            <span class="title">My Bots</span>
            <button type="button" class="btn btn-primary" @click="openBotModal(null)">
              Create Bot
            </button>
          </div>
          <div class="card-body">
            <table class="table table-striped table-hover">
              <thead>
              <tr>
                <th>Title</th>
                <th>Language</th>
                <th>Created At</th>
                <th>Actions</th>
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
                  <button type="button" class="btn btn-secondary me-2" @click="openBotModal(bot)">Edit</button>
                  <button type="button" class="btn btn-danger" @click="removeBot(bot)">Delete</button>
                </td>
              </tr>
              <tr v-if="bots.length === 0">
                <td colspan="4" class="text-center text-muted py-4">You have no bots yet. Create one!</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="modal fade" ref="botModalRef" tabindex="-1">
      <div class="modal-dialog modal-xl">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ botDraft.id ? 'Edit Bot' : 'Create Bot' }}</h5>
            <button type="button" class="btn-close" @click="closeModal(botModalRef)"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label">Title <span class="text-danger">*</span></label>
              <input v-model="botDraft.title" type="text" class="form-control" placeholder="Bot title" />
            </div>
            <div class="mb-3">
              <label class="form-label">Language <span class="text-danger">*</span></label>
              <select v-model="botDraft.language" class="form-select">
                <option value="java">Java</option>
                <option value="python">Python</option>
                <option value="cpp">C++</option>
                <option value="javascript">JavaScript</option>
              </select>
            </div>
            <div class="mb-3">
              <label class="form-label">Description</label>
              <textarea v-model="botDraft.description" class="form-control" rows="3" placeholder="Bot description"></textarea>
            </div>
            <div class="mb-3">
              <label class="form-label">Code <span class="text-danger">*</span></label>
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
              {{ isSubmitting ? 'Saving...' : 'Save' }}
            </button>
            <button type="button" class="btn btn-secondary" @click="closeModal(botModalRef)">Cancel</button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal fade" ref="usernameModalRef" tabindex="-1">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Modify Username</h5>
            <button type="button" class="btn-close" @click="closeModal(usernameModalRef)"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label">New Username</label>
              <input v-model="accountDraft.new_username" type="text" class="form-control" placeholder="6-12 characters, letters/numbers/underscores" />
            </div>
            <div class="error-message">{{ accountDraft.username_error }}</div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-primary" @click="updateUsername" :disabled="isSubmitting">Save</button>
            <button type="button" class="btn btn-secondary" @click="closeModal(usernameModalRef)">Cancel</button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal fade" ref="passwordModalRef" tabindex="-1">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Modify Password</h5>
            <button type="button" class="btn-close" @click="closeModal(passwordModalRef)"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label">Current Password</label>
              <input v-model="accountDraft.old_password" type="password" class="form-control" placeholder="Enter current password" />
            </div>
            <div class="mb-3">
              <label class="form-label">New Password</label>
              <input v-model="accountDraft.new_password" type="password" class="form-control" placeholder="Enter new password" />
              <div class="form-text">8-16 chars, include at least 3: Uppercase, Lowercase, Number, Symbol.</div>
            </div>
            <div class="mb-3">
              <label class="form-label">Confirm New Password</label>
              <input v-model="accountDraft.confirmed_password" type="password" class="form-control" placeholder="Confirm new password" />
            </div>
            <div class="error-message">{{ accountDraft.password_error }}</div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-primary" @click="updatePassword" :disabled="isSubmitting">Save</button>
            <button type="button" class="btn btn-secondary" @click="closeModal(passwordModalRef)">Cancel</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
import Modal from "bootstrap/js/dist/modal";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

// 引入 vue3-ace-editor 及其依赖
import { VAceEditor } from 'vue3-ace-editor';
import 'ace-builds/src-noconflict/mode-java';
import 'ace-builds/src-noconflict/mode-python';
import 'ace-builds/src-noconflict/mode-c_cpp';
import 'ace-builds/src-noconflict/mode-javascript';
import 'ace-builds/src-noconflict/theme-chrome';

const store = useStore();
const router = useRouter();
const authToken = () => store.state.user.token;

const bots = ref([]);
const isSubmitting = ref(false); // 全局的加载状态保护

// --- DOM 引用映射 ---
const botModalRef = ref(null);
const usernameModalRef = ref(null);
const passwordModalRef = ref(null);

// 安全地获取和控制 Bootstrap Modal 实例
const openModal = (modalRef) => {
  if (modalRef) Modal.getOrCreateInstance(modalRef).show();
};
const closeModal = (modalRef) => {
  if (modalRef) Modal.getOrCreateInstance(modalRef).hide();
};

// --- Bot 草稿数据管理 ---
const botDraft = reactive({
  id: null,
  title: "",
  description: "",
  content: "",
  language: "java",
  error_message: "",
});

// 计算 Ace Editor 需要的语言标识映射
const aceLang = computed(() => {
  const map = { cpp: 'c_cpp', java: 'java', python: 'python', javascript: 'javascript' };
  return map[botDraft.language] || 'java';
});

const accountDraft = reactive({
  new_username: "", username_error: "",
  old_password: "", new_password: "", confirmed_password: "", password_error: "",
});

const fileInput = ref(null);
const uploading = ref(false);

// --- 初始化与获取数据 ---
const refreshBots = async () => {
  try {
    bots.value = await apiRequest(API_PATHS.botList, { token: authToken() });
  } catch (error) {
    console.error("Failed to load bots", error);
  }
};

onMounted(() => {
  refreshBots();
});

// --- Bot 业务逻辑 ---
const openBotModal = (bot = null) => {
  if (bot) {
    // 编辑模式：深拷贝当前 bot 数据给草稿
    Object.assign(botDraft, { ...bot, error_message: "" });
  } else {
    // 新建模式：重置草稿并填充模板代码
    Object.assign(botDraft, {
      id: null, title: "", description: "", language: "java", error_message: "",
      content: codePlaceholder("java")
    });
  }
  openModal(botModalRef.value);
};

const saveBot = async () => {
  botDraft.error_message = "";

  // 1. 前端基础校验
  if (!botDraft.title.trim()) return botDraft.error_message = "Title cannot be empty.";
  if (!botDraft.content.trim()) return botDraft.error_message = "Code cannot be empty.";

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
      method: "POST", token: authToken(), data: payload
    });

    if (resp.error_message !== "success") {
      botDraft.error_message = resp.error_message || "Operation failed";
      return;
    }

    // 2. 优化：如果为编辑模式，执行本地（乐观）UI更新，省去重新拉取全量接口的时间
    if (isEdit) {
      const index = bots.value.findIndex(b => b.id === botDraft.id);
      if (index !== -1) {
        bots.value[index] = { ...bots.value[index], ...payload };
      }
    } else {
      // 新增模式：因为需要后端的 bot id 和创建时间，所以执行列表刷新
      await refreshBots();
    }

    closeModal(botModalRef.value);
  } catch (error) {
    botDraft.error_message = error.message || "Network error";
  } finally {
    isSubmitting.value = false;
  }
};

const removeBot = async (bot) => {
  if (!confirm(`Are you sure you want to delete bot "${bot.title}"?`)) return;

  try {
    const resp = await apiRequest(API_PATHS.botRemove, {
      method: "POST", token: authToken(), data: { bot_id: bot.id },
    });

    if (resp.error_message === "success") {
      // 优化：本地剔除数据，瞬间响应 UI，不重新请求列表
      bots.value = bots.value.filter(b => b.id !== bot.id);
    } else {
      alert(resp.error_message || "Delete failed");
    }
  } catch (error) {
    console.error(error);
  }
};

// --- 用户资料业务逻辑 ---
const triggerUpload = () => {
  if (fileInput.value) fileInput.value.click();
};

const uploadAvatar = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const validTypes = ["image/jpeg", "image/png", "image/jpg"];
  if (!validTypes.includes(file.type)) return alert("只能上传 JPG/PNG 格式的图片");
  if (file.size > 5 * 1024 * 1024) return alert("图片大小不能超过 5MB");

  const formData = new FormData();
  formData.append("file", file);
  uploading.value = true;

  try {
    const resp = await apiRequest(API_PATHS.uploadAvatar, {
      method: "POST", token: authToken(), data: formData,
    });
    if (resp.error_message === "success") {
      store.commit("updatePhoto", resp.photo);
    } else {
      alert(resp.error_message || "上传失败");
    }
  } catch (error) {
    alert("头像上传失败");
  } finally {
    uploading.value = false;
    if (fileInput.value) fileInput.value.value = "";
  }
};

const updateUsername = async () => {
  accountDraft.username_error = "";
  if (!accountDraft.new_username.trim()) return accountDraft.username_error = "Username cannot be empty";

  isSubmitting.value = true;
  try {
    const resp = await apiRequest(API_PATHS.updateUsername, {
      method: "POST", token: authToken(), data: { new_username: accountDraft.new_username },
    });
    if (resp.error_message === "success") {
      store.commit("updateUsername", accountDraft.new_username);
      accountDraft.new_username = "";
      closeModal(usernameModalRef.value);
    } else {
      accountDraft.username_error = resp.error_message;
    }
  } catch (error) {
    accountDraft.username_error = error.message;
  } finally {
    isSubmitting.value = false;
  }
};

const updatePassword = async () => {
  accountDraft.password_error = "";
  if (accountDraft.new_password !== accountDraft.confirmed_password) {
    return accountDraft.password_error = "Passwords do not match.";
  }

  isSubmitting.value = true;
  try {
    const resp = await apiRequest(API_PATHS.updatePassword, {
      method: "POST", token: authToken(), data: {
        old_password: accountDraft.old_password,
        new_password: accountDraft.new_password,
        confirmed_password: accountDraft.confirmed_password,
      },
    });
    if (resp.error_message === "success") {
      closeModal(passwordModalRef.value);
      alert("Password updated successfully! Please login again.");
      store.dispatch("logout");
      await router.push({ name: "user_account_login" });
    } else {
      accountDraft.password_error = resp.error_message;
    }
  } catch (error) {
    accountDraft.password_error = error.message;
  } finally {
    isSubmitting.value = false;
  }
};

// 预设代码模板配置
const codePlaceholder = (lang) => {
  const map = {
    java: `import java.util.Scanner;\nimport java.io.File;\n\npublic class Bot implements java.util.function.Supplier<Integer> {\n    @Override\n    public Integer get() {\n        // Return direction: 0=Up, 1=Right, 2=Down, 3=Left\n        return 0;\n    }\n}`,
    python: `# Return direction: 0=Up, 1=Right, 2=Down, 3=Left\n\ndirection = 0\nprint(direction)`,
    cpp: `#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    // Return direction: 0=Up, 1=Right, 2=Down, 3=Left\n    cout << 0 << endl;\n    return 0;\n}`,
    javascript: `"use strict";\n// Return direction: 0=Up, 1=Right, 2=Down, 3=Left\nconsole.log(0);`,
  };
  return map[lang] || map.java;
};
</script>

<style scoped>
.bot-page {
  margin-top: 20px;
}
.bot-profile-card, .bot-main-card {
  background: var(--kob-panel);
  border: 1px solid var(--kob-panel-border);
  box-shadow: 0 10px 30px rgba(0, 50, 100, 0.08);
  backdrop-filter: blur(12px);
}
.photo-section { text-align: center; }
.avatar-wrap {
  position: relative; display: inline-block; width: 100%; border-radius: 14px; overflow: hidden; cursor: pointer;
}
.profile-avatar {
  width: 100%; display: block; border-radius: 14px; border: 2px solid rgba(90, 180, 255, 0.3); transition: filter 0.3s;
}
.avatar-overlay {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0, 0, 0, 0.5); color: white; display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.3s; font-size: 0.9rem; font-weight: 500;
}
.avatar-wrap:hover .profile-avatar { filter: brightness(0.8); }
.avatar-wrap:hover .avatar-overlay { opacity: 1; }
.profile-name { margin: 14px 0 0; font-weight: 600; text-align: center; color: var(--kob-text); }
.bot-main-header { display: flex; justify-content: space-between; align-items: center; }
.bot-main-header .title { font-family: "Space Grotesk", sans-serif; font-size: 1.2rem; font-weight: 700; color: var(--kob-text); }
.modal-content { background: #ffffff; border: 1px solid rgba(90, 180, 255, 0.3); color: var(--kob-text); }
.modal-header .btn-close { filter: none; }
.error-message { color: #e74c3c; min-height: 20px;}

/* 语言徽章 */
.lang-badge { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 0.72rem; font-weight: 700; letter-spacing: 0.4px; }
.lang-java       { background: rgba(237, 129, 50, 0.15); color: #c25a00; }
.lang-python     { background: rgba(55, 118, 171, 0.15); color: #1e6fa6; }
.lang-cpp        { background: rgba(100, 54, 180, 0.15); color: #5c2eab; }
.lang-javascript { background: rgba(202, 163,  10, 0.15); color: #8a6800; }
</style>