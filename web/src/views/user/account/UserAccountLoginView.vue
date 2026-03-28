<template>
  <ContentField v-if="!store.state.user.pulling_info">
    <section class="auth-wrap">
      <div class="auth-head">
        <h2>Welcome Back</h2>
        <p>Sign in and jump into your next match.</p>
      </div>
      <div class="row justify-content-md-center">
        <div class="col-lg-5 col-md-7 col-12">
          <form class="auth-form" @submit.prevent="login">
            <div class="mb-3">
              <label for="username" class="form-label">Username</label>
              <input id="username" v-model="username" type="text" class="form-control" placeholder="Enter username" />
            </div>
            <div class="mb-3">
              <label for="password" class="form-label">Password</label>
              <input id="password" v-model="password" type="password" class="form-control" placeholder="Enter password" />
            </div>
            <div class="error-message">{{ errorMessage }}</div>
            <button type="submit" class="btn btn-primary auth-btn">Log In</button>
          </form>
        </div>
      </div>
    </section>
  </ContentField>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useStore } from "vuex";
import ContentField from "@/components/ContentField.vue";

const store = useStore();
const router = useRouter();

const username = ref("");
const password = ref("");
const errorMessage = ref("");

const login = async () => {
  errorMessage.value = "";
  try {
    await store.dispatch("login", {
      username: username.value,
      password: password.value,
    });
    await store.dispatch("getinfo");
    await router.push({ name: "home" });
  } catch (error) {
    errorMessage.value = error.message || "Invalid username or password";
  }
};
</script>

<style scoped>
.auth-wrap {
  padding: 6px 4px;
}

.auth-head {
  text-align: center;
  margin-bottom: 20px;
}

.auth-head h2 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-weight: 700;
}

.auth-head p {
  margin: 8px 0 0;
  color: #a9bfd3;
}

.auth-form {
  padding: 20px;
  border: 1px solid rgba(145, 210, 255, 0.2);
  border-radius: 16px;
  background: rgba(8, 30, 44, 0.34);
}

.auth-btn {
  width: 100%;
  margin-top: 4px;
  border-radius: 12px;
  padding: 10px 0;
  font-weight: 600;
}

div.error-message {
  min-height: 22px;
  color: #ff8f8f;
}
</style>
