<template>
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
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import Modal from "bootstrap/js/dist/modal";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";
import { useUserStore } from "@/store/user";
import AppDialog from "@/components/AppDialog.vue";

const userStore = useUserStore();
const router = useRouter();
const authToken = () => userStore.token;

const isSubmitting = ref(false);
const uploading = ref(false);
const fileInput = ref(null);
const usernameModalRef = ref(null);
const passwordModalRef = ref(null);

const accountDraft = reactive({
  new_username: "",
  username_error: "",
  old_password: "",
  new_password: "",
  confirmed_password: "",
  password_error: "",
});

const dialogState = reactive({
  visible: false,
  title: "",
  message: "",
  confirmText: "OK",
  cancelText: "Cancel",
  showCancel: false,
});
let dialogResolver = null;

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
      method: "POST",
      token: authToken(),
      data: formData,
    });
    if (resp.error_message === "success") {
      userStore.updatePhoto(resp.photo);
    } else {
      await openDialog({
        title: "Upload Failed",
        message: resp.error_message || "Upload failed",
      });
    }
  } catch {
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
  if (!newUsername) {
    accountDraft.username_error = "Username cannot be empty";
    return;
  }

  isSubmitting.value = true;
  try {
    const resp = await apiRequest(API_PATHS.updateUsername, {
      method: "POST",
      token: authToken(),
      data: { new_username: newUsername },
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
  if (!accountDraft.new_password) {
    accountDraft.password_error = "New password cannot be empty.";
    return;
  }
  if (accountDraft.new_password !== accountDraft.confirmed_password) {
    accountDraft.password_error = "Passwords do not match.";
    return;
  }

  isSubmitting.value = true;
  try {
    const resp = await apiRequest(API_PATHS.updatePassword, {
      method: "POST",
      token: authToken(),
      data: {
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
</script>

<style scoped>
.bot-profile-card {
  background: var(--kob-panel);
  border: 1px solid var(--kob-panel-border);
  box-shadow: 0 10px 30px rgba(0, 50, 100, 0.08);
  backdrop-filter: blur(12px);
}

.photo-section {
  text-align: center;
}

.avatar-wrap {
  position: relative;
  display: inline-block;
  width: 100%;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
}

.profile-avatar {
  width: 100%;
  display: block;
  border-radius: 14px;
  border: 2px solid rgba(90, 180, 255, 0.3);
  transition: filter 0.3s;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 0.9rem;
  font-weight: 500;
}

.avatar-wrap:hover .profile-avatar {
  filter: brightness(0.8);
}

.avatar-wrap:hover .avatar-overlay {
  opacity: 1;
}

.profile-name {
  margin: 14px 0 0;
  font-weight: 600;
  text-align: center;
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
</style>
