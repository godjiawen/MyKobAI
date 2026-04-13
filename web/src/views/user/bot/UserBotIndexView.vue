<template>
  <div class="container bot-page">
    <div class="row g-3">
      <div class="col-lg-3 col-md-4">
        <div class="card bot-profile-card">
          <div class="card-body photo-section">
            <div class="avatar-wrap" @click="triggerUpload">
              <img :src="userStore.photo" alt="avatar" class="profile-avatar" />
              <div class="avatar-overlay">
                <span v-if="!uploading">Click to change</span>
                <span v-else>Uploading...</span>
              </div>
            </div>
            <p class="profile-name">{{ userStore.username }}</p>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useUserStore } from "@/store/user";
import { useRouter } from "vue-router";
import Modal from "bootstrap/js/dist/modal";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";
import AppDialog from "@/components/AppDialog.vue";

// Import vue3-ace-editor and its dependencies
import { VAceEditor } from 'vue3-ace-editor';
import 'ace-builds/src-noconflict/mode-java';
import 'ace-builds/src-noconflict/mode-python';
import 'ace-builds/src-noconflict/mode-c_cpp';
import 'ace-builds/src-noconflict/mode-javascript';
import 'ace-builds/src-noconflict/theme-chrome';

const userStore = useUserStore();
const router = useRouter();
const authToken = () => userStore.token;

const bots = ref([]);
const isSubmitting = ref(false); // global loading guard
// --- DOM ref map ---
const botModalRef = ref(null);
const usernameModalRef = ref(null);
const passwordModalRef = ref(null);
const dialogState = reactive({
  visible: false,
  title: "",
  message: "",
  confirmText: "OK",
  cancelText: "Cancel",
  showCancel: false,
});
let dialogResolver = null;

// Bootstrap Modal instance helpers
const openModal = (modalRef) => {
  if (modalRef) Modal.getOrCreateInstance(modalRef).show();
};
const closeModal = (modalRef) => {
  if (modalRef) Modal.getOrCreateInstance(modalRef).hide();
};

const openDialog = ({
  title = "Notice",
  message = "",
  confirmText = "OK",
  cancelText = "Cancel",
  showCancel = false,
} = {}) => new Promise((resolve) => {
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

// --- Bot draft data management ---
const botDraft = reactive({
  id: null,
  title: "",
  description: "",
  content: "",
  language: "java",
  error_message: "",
});

// Compute the Ace Editor language mode identifier
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

// --- Initialization and data fetching ---
const refreshBots = async () => {
  try {
    bots.value = await apiRequest(API_PATHS.botList, {
      token: authToken(),
      query: { _: Date.now() }, // avoid browser/proxy returning cached list
    });
  } catch (error) {
    console.error("Failed to load bots", error);
  }
};

onMounted(() => {
  refreshBots();
});

// --- Bot business logic ---
const openBotModal = (bot = null) => {
  if (bot) {
    // Edit mode: copy current bot data into draft
    Object.assign(botDraft, { ...bot, error_message: "" });
  } else {
    // New bot mode: reset draft and fill template code
    Object.assign(botDraft, {
      id: null, title: "", description: "", language: "java", error_message: "",
      content: codePlaceholder("java")
    });
  }
  openModal(botModalRef.value);
};

const saveBot = async () => {
  botDraft.error_message = "";

  // 1. Front-end validation
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

    // 2. UI update: for edit mode do an optimistic local update to avoid a full list re-fetch.
    //    For create mode, show a temporary entry immediately then replace with server data.
    if (isEdit) {
      const index = bots.value.findIndex((b) => b.id === botDraft.id);
      if (index !== -1) {
        bots.value[index] = { ...bots.value[index], ...payload };
      }
    } else {
      // Show optimistic entry immediately, then fetch real list (with real id/createtime from server)
      const now = new Date().toISOString().slice(0, 19).replace("T", " ");
      bots.value = [{
        id: `tmp-${Date.now()}`,
        ...payload,
        createtime: now,
      }, ...bots.value];
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
  const confirmed = await openDialog({
    title: "Delete Bot",
    message: `Are you sure you want to delete bot "${bot.title}"?`,
    confirmText: "Delete",
    cancelText: "Cancel",
    showCancel: true,
  });
  if (!confirmed) return;

  try {
    const resp = await apiRequest(API_PATHS.botRemove, {
      method: "POST", token: authToken(), data: { bot_id: bot.id },
    });

    if (resp.error_message === "success") {
      bots.value = bots.value.filter((b) => b.id !== bot.id);
    } else {
      await openDialog({
        title: "Delete Failed",
        message: resp.error_message || "Delete failed",
      });
    }
  } catch (error) {
    console.error(error);
    await openDialog({
      title: "Delete Failed",
      message: "Delete request failed. Please try again.",
    });
  }
};

// --- User profile business logic ---
const triggerUpload = () => {
  if (fileInput.value) fileInput.value.click();
};

const uploadAvatar = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const validTypes = ["image/jpeg", "image/png", "image/jpg"];
  if (!validTypes.includes(file.type)) {
    await openDialog({
      title: "Invalid File Type",
      message: "Only JPG/PNG files are allowed.",
    });
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    await openDialog({
      title: "File Too Large",
      message: "Image size must be less than 5MB.",
    });
    return;
  }

  const formData = new FormData();
  formData.append("file", file);
  uploading.value = true;

  try {
    const resp = await apiRequest(API_PATHS.uploadAvatar, {
      method: "POST", token: authToken(), data: formData,
    });
    if (resp.error_message === "success") {
      userStore.updatePhoto(resp.photo);
    } else {
      await openDialog({
        title: "Upload Failed",
        message: resp.error_message || "Upload failed",
      });
    }
  } catch (error) {
    await openDialog({
      title: "Upload Failed",
      message: "Avatar upload failed. Please try again.",
    });
  } finally {
    uploading.value = false;
    if (fileInput.value) fileInput.value.value = "";
  }
};

const updateUsername = async () => {
  accountDraft.username_error = "";
  const newUsername = accountDraft.new_username.trim();
  if (!newUsername) return accountDraft.username_error = "Username cannot be empty";

  isSubmitting.value = true;
  try {
    const resp = await apiRequest(API_PATHS.updateUsername, {
      method: "POST", token: authToken(), data: { new_username: newUsername },
    });
    if (resp.error_message === "success") {
      userStore.updateUsername(newUsername);
      accountDraft.new_username = "";
      closeModal(usernameModalRef.value);
      await openDialog({
        title: "Username Updated",
        message: "Username updated successfully.",
      });
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
      await openDialog({
        title: "Password Updated",
        message: "Password updated successfully! Please login again.",
      });
      await userStore.logout();
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

// Preset code template configuration
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

/* Language badges */
.lang-badge { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 0.72rem; font-weight: 700; letter-spacing: 0.4px; }
.lang-java       { background: rgba(237, 129, 50, 0.15); color: #c25a00; }
.lang-python     { background: rgba(55, 118, 171, 0.15); color: #1e6fa6; }
.lang-cpp        { background: rgba(100, 54, 180, 0.15); color: #5c2eab; }
.lang-javascript { background: rgba(202, 163,  10, 0.15); color: #8a6800; }
</style>
