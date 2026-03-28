<template>
  <div class="container bot-page">
    <div class="row g-3">
      <div class="col-lg-3 col-md-4">
        <div class="card bot-profile-card">
          <div class="card-body">
            <img :src="store.state.user.photo" alt="avatar" class="profile-avatar" />
            <p class="profile-name">{{ store.state.user.username }}</p>
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
                  <th>Created At</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="bot in bots" :key="bot.id">
                  <td class="align-middle">{{ bot.title }}</td>
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
                                placeholder="Bot code"
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
                placeholder="Bot code"
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
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

const store = useStore();
const bots = ref([]);

const botDraft = reactive({
  title: "",
  description: "",
  content: "",
  error_message: "",
});

const authToken = () => store.state.user.token;

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
      },
    });

    if (resp.error_message !== "success") {
      botDraft.error_message = resp.error_message || "Create failed";
      return;
    }

    botDraft.title = "";
    botDraft.description = "";
    botDraft.content = "";

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

onMounted(refreshBots);
</script>

<style scoped>
.bot-page {
  margin-top: 20px;
}

.bot-profile-card,
.bot-main-card {
  background: rgba(8, 24, 38, 0.74);
  border: 1px solid rgba(145, 210, 255, 0.2);
}

.profile-avatar {
  width: 100%;
  border-radius: 14px;
  border: 1px solid rgba(145, 210, 255, 0.28);
}

.profile-name {
  margin: 14px 0 0;
  font-weight: 600;
  text-align: center;
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
}

.modal-content {
  background: #0f2133;
  border: 1px solid rgba(145, 210, 255, 0.24);
  color: #e8f4ff;
}

.error-message {
  color: #ff8f8f;
  margin-right: auto;
}

.bot-code-input {
  min-height: 260px;
  font-family: "JetBrains Mono", "Consolas", monospace;
  resize: vertical;
}
</style>
