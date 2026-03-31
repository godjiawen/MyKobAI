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
              <button class="btn btn-sm btn-outline-primary mb-2 w-100" data-bs-toggle="modal" data-bs-target="#update-username-modal">Modify Username</button>
              <button class="btn btn-sm btn-outline-warning w-100" data-bs-toggle="modal" data-bs-target="#update-password-modal">Modify Password</button>
            </div>
            <input type="file" ref="fileInput" @change="uploadAvatar" accept="image/png, image/jpeg, image/jpg" style="display: none;" />
          </div>
        </div>
      </div>

      <!-- Update Username Modal -->
      <div class="modal fade" id="update-username-modal" tabindex="-1">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">Modify Username</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
              <div class="mb-3">
                <label for="new-username" class="form-label">New Username</label>
                <input v-model="accountDraft.new_username" type="text" class="form-control" id="new-username" placeholder="6-12 characters, letters/numbers/underscores" />
              </div>
              <div class="error-message" v-if="accountDraft.username_error">{{ accountDraft.username_error }}</div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-primary" @click="updateUsername">Save</button>
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Update Password Modal -->
      <div class="modal fade" id="update-password-modal" tabindex="-1">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">Modify Password</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
              <div class="mb-3">
                <label for="old-password" class="form-label">Current Password</label>
                <input v-model="accountDraft.old_password" type="password" class="form-control" id="old-password" placeholder="Enter current password" />
              </div>
              <div class="mb-3">
                <label for="new-password" class="form-label">New Password</label>
                <input v-model="accountDraft.new_password" type="password" class="form-control" id="new-password" placeholder="Enter new password" />
                <div class="form-text">8-16 chars, include at least 3: Uppercase, Lowercase, Number, Symbol.</div>
              </div>
              <div class="mb-3">
                <label for="confirmed-password" class="form-label">Confirm New Password</label>
                <input v-model="accountDraft.confirmed_password" type="password" class="form-control" id="confirmed-password" placeholder="Confirm new password" />
              </div>
              <div class="error-message" v-if="accountDraft.password_error">{{ accountDraft.password_error }}</div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-primary" @click="updatePassword">Save</button>
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-9 col-md-8">
        <div class="card bot-main-card">
          <div class="card-header bot-main-header">
            <span class="title">My Bots</span>
            <button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#add-bot-btn">
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
                    <button
                      type="button"
                      class="btn btn-secondary me-2"
                      data-bs-toggle="modal"
                      :data-bs-target="`#update-bot-modal${bot.id}`"
                    >
                      Edit
                    </button>
                    <button type="button" class="btn btn-danger" @click="removeBot(bot)">Delete</button>

                    <div class="modal fade" :id="`update-bot-modal${bot.id}`" tabindex="-1">
                      <div class="modal-dialog modal-xl">
                        <div class="modal-content">
                          <div class="modal-header">
                            <h5 class="modal-title">Edit Bot</h5>
                            <button
                              type="button"
                              class="btn-close"
                              data-bs-dismiss="modal"
                              aria-label="Close"
                              @click="refreshBots"
                            ></button>
                          </div>
                          <div class="modal-body">
                            <div class="mb-3">
                              <label class="form-label">Title</label>
                              <input v-model="bot.title" type="text" class="form-control" placeholder="Bot title" />
                            </div>
                            <div class="mb-3">
                              <label class="form-label">Language</label>
                              <select v-model="bot.language" class="form-select">
                                <option value="java">Java</option>
                                <option value="python">Python</option>
                                <option value="cpp">C++</option>
                                <option value="javascript">JavaScript</option>
                              </select>
                            </div>
                            <div class="mb-3">
                              <label class="form-label">Description</label>
                              <textarea
                                v-model="bot.description"
                                class="form-control"
                                rows="3"
                                placeholder="Bot description"
                              ></textarea>
                            </div>
                            <div class="mb-3">
                              <label class="form-label">Code</label>
                              <textarea
                                v-model="bot.content"
                                class="form-control bot-code-input"
                                rows="12"
                                :placeholder="codePlaceholder(bot.language)"
                              ></textarea>
                            </div>
                          </div>
                          <div class="modal-footer">
                            <div class="error-message">{{ bot.error_message }}</div>
                            <button type="button" class="btn btn-primary" @click="updateBot(bot)">Save</button>
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" @click="refreshBots">Cancel</button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="modal fade" id="add-bot-btn" tabindex="-1">
      <div class="modal-dialog modal-xl">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Create Bot</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label">Title</label>
              <input v-model="botDraft.title" type="text" class="form-control" placeholder="Bot title" />
            </div>
            <div class="mb-3">
              <label class="form-label">Language</label>
              <select v-model="botDraft.language" class="form-select">
                <option value="java">Java</option>
                <option value="python">Python</option>
                <option value="cpp">C++</option>
                <option value="javascript">JavaScript</option>
              </select>
            </div>
            <div class="mb-3">
              <label class="form-label">Description</label>
              <textarea
                v-model="botDraft.description"
                class="form-control"
                rows="3"
                placeholder="Bot description"
              ></textarea>
            </div>
            <div class="mb-3">
              <label class="form-label">Code</label>
              <textarea
                v-model="botDraft.content"
                class="form-control bot-code-input"
                rows="12"
                :placeholder="codePlaceholder(botDraft.language)"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <div class="error-message">{{ botDraft.error_message }}</div>
            <button type="button" class="btn btn-primary" @click="addBot">Create</button>
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import Modal from "bootstrap/js/dist/modal";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

const store = useStore();
const router = useRouter();
const bots = ref([]);

const botDraft = reactive({
  title: "",
  description: "",
  content: "",
  language: "java",
  error_message: "",
});

const accountDraft = reactive({
  new_username: "",
  username_error: "",
  old_password: "",
  new_password: "",
  confirmed_password: "",
  password_error: "",
});

const fileInput = ref(null);
const uploading = ref(false);

const authToken = () => store.state.user.token;

const triggerUpload = () => {
  if (fileInput.value) {
    fileInput.value.click();
  }
};

const uploadAvatar = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const validTypes = ["image/jpeg", "image/png", "image/jpg"];
  if (!validTypes.includes(file.type)) {
    alert("只能上传 JPG/PNG 格式的图片");
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    alert("图片大小不能超过 5MB");
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
      store.commit("updatePhoto", resp.photo);
      alert("头像上传成功！");
    } else {
      alert(resp.error_message || "上传失败");
    }
  } catch (error) {
    console.error(error);
    alert("头像上传失败");
  } finally {
    uploading.value = false;
    if (fileInput.value) fileInput.value.value = ""; // reset input
  }
};

const updateUsername = async () => {
  accountDraft.username_error = "";
  try {
    const resp = await apiRequest(API_PATHS.updateUsername, {
      method: "POST",
      token: authToken(),
      data: {
        new_username: accountDraft.new_username,
      },
    });

    if (resp.error_message === "success") {
      store.commit("updateUsername", accountDraft.new_username);
      const modalElement = document.getElementById("update-username-modal");
      if (modalElement) {
        Modal.getInstance(modalElement)?.hide();
      }
      accountDraft.new_username = "";
      alert("Username updated successfully!");
    } else {
      accountDraft.username_error = resp.error_message;
    }
  } catch (error) {
    accountDraft.username_error = error.message || "Failed to update username";
  }
};

const updatePassword = async () => {
  accountDraft.password_error = "";
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
      const modalElement = document.getElementById("update-password-modal");
      if (modalElement) {
        Modal.getInstance(modalElement)?.hide();
      }
      accountDraft.old_password = "";
      accountDraft.new_password = "";
      accountDraft.confirmed_password = "";
      alert("Password updated successfully! Please login again.");
      store.dispatch("logout");
      router.push({ name: "user_account_login" });
    } else {
      accountDraft.password_error = resp.error_message;
    }
  } catch (error) {
    accountDraft.password_error = error.message || "Failed to update password";
  }
};

const refreshBots = async () => {
  try {
    bots.value = await apiRequest(API_PATHS.botList, { token: authToken() });
  } catch (error) {
    console.error(error);
  }
};

const addBot = async () => {
  botDraft.error_message = "";

  try {
    const resp = await apiRequest(API_PATHS.botAdd, {
      method: "POST",
      token: authToken(),
      data: {
        title: botDraft.title,
        description: botDraft.description,
        content: botDraft.content,
        language: botDraft.language,
      },
    });

    if (resp.error_message !== "success") {
      botDraft.error_message = resp.error_message || "Create failed";
      return;
    }

    botDraft.title = "";
    botDraft.description = "";
    botDraft.content = "";
    botDraft.language = "java";

    const modalElement = document.getElementById("add-bot-btn");
    if (modalElement) {
      Modal.getOrCreateInstance(modalElement).hide();
    }

    await refreshBots();
  } catch (error) {
    botDraft.error_message = error.message || "Create failed";
  }
};

const updateBot = async (bot) => {
  bot.error_message = "";

  try {
    const resp = await apiRequest(API_PATHS.botUpdate, {
      method: "POST",
      token: authToken(),
      data: {
        bot_id: bot.id,
        title: bot.title,
        description: bot.description,
        content: bot.content,
        language: bot.language || "java",
      },
    });

    if (resp.error_message !== "success") {
      bot.error_message = resp.error_message || "Update failed";
      return;
    }

    const modalElement = document.getElementById(`update-bot-modal${bot.id}`);
    if (modalElement) {
      Modal.getOrCreateInstance(modalElement).hide();
    }

    await refreshBots();
  } catch (error) {
    bot.error_message = error.message || "Update failed";
  }
};

const removeBot = async (bot) => {
  try {
    const resp = await apiRequest(API_PATHS.botRemove, {
      method: "POST",
      token: authToken(),
      data: { bot_id: bot.id },
    });

    if (resp.error_message === "success") {
      await refreshBots();
    }
  } catch (error) {
    console.error(error);
  }
};

const codePlaceholder = (lang) => {
  const map = {
    java:       "public class Bot implements java.util.function.Supplier<Integer> {\n    @Override\n    public Integer get() {\n        // read input.txt, return 0/1/2/3\n        return 0;\n    }\n}",
    python:     "with open('input.txt') as f:\n    data = f.read().strip()\n# parse data, compute direction (0-3)\ndirection = 0\nprint(direction)",
    cpp:        "#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    ifstream fin(\"input.txt\");\n    string s; fin >> s; fin.close();\n    // compute direction (0-3)\n    cout << 0 << endl;\n}",
    javascript: "const fs = require('fs');\nconst input = fs.readFileSync('input.txt','utf8').trim();\n// compute direction (0-3)\nconsole.log(0);",
  };
  return map[lang] || map.java;
};

onMounted(refreshBots);
</script>

<style scoped>
.bot-page {
  margin-top: 20px;
}

.bot-profile-card,
.bot-main-card {
  background: var(--kob-panel);
  border: 1px solid var(--kob-panel-border);
  box-shadow: 0 10px 30px rgba(0, 50, 100, 0.08); /* light shadow */
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
  color: white;
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
  background: #ffffff;
  border: 1px solid rgba(90, 180, 255, 0.3);
  color: var(--kob-text);
}

.modal-header .btn-close {
  filter: none;
}

.error-message {
  color: #e74c3c;
  margin-right: auto;
}

.bot-code-input {
  min-height: 260px;
  font-family: "JetBrains Mono", "Consolas", monospace;
  resize: vertical;
}

/* 语言徽章 */
.lang-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.4px;
}
.lang-java       { background: rgba(237, 129, 50, 0.15); color: #c25a00; }
.lang-python     { background: rgba(55, 118, 171, 0.15); color: #1e6fa6; }
.lang-cpp        { background: rgba(100, 54, 180, 0.15); color: #5c2eab; }
.lang-javascript { background: rgba(202, 163,  10, 0.15); color: #8a6800; }
</style>
