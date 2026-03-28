<template>
  <ContentField>
    <section class="auth-wrap">
      <div class="auth-head">
        <h2>Create Account</h2>
        <p>Register and start climbing the ranklist.</p>
      </div>
      <div class="row justify-content-md-center">
        <div class="col-lg-5 col-md-7 col-12">
          <form class="auth-form" @submit.prevent="register">
            <div class="mb-3">
              <label for="username" class="form-label">Username</label>
              <input id="username" v-model="username" type="text" class="form-control" placeholder="Choose username" />
              <div class="rule-tip">6-12位，仅字母/数字/下划线</div>
            </div>
            <div class="mb-3">
              <label for="password" class="form-label">Password</label>
              <input id="password" v-model="password" type="password" class="form-control" placeholder="Set password" />
              <div class="rule-tip">8-16位，且至少包含以下四类中的三类：大写字母/小写字母/数字/特殊字符</div>
            </div>
            <div class="mb-3">
              <label for="confirmedPassword" class="form-label">Confirm Password</label>
              <input id="confirmedPassword" v-model="confirmedPassword" type="password" class="form-control" placeholder="Repeat password" />
            </div>
            <div class="error-message">{{ errorMessage }}</div>
            <button type="submit" class="btn btn-primary auth-btn">Create</button>
          </form>
        </div>
      </div>
    </section>
  </ContentField>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import ContentField from "@/components/ContentField.vue";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

const router = useRouter();

const username = ref("");
const password = ref("");
const confirmedPassword = ref("");
const errorMessage = ref("");

const validateRegisterForm = () => {
  const usernameValue = username.value.trim();
  const passwordValue = password.value;
  const confirmedValue = confirmedPassword.value;

  if (!/^[A-Za-z0-9_]{6,12}$/.test(usernameValue)) {
    return "用户名需为6-12位，只能包含字母、数字和下划线";
  }

  if (passwordValue.length < 8 || passwordValue.length > 16) {
    return "密码长度需为8-16位";
  }

  if (/\s/.test(passwordValue)) {
    return "密码不能包含空白字符";
  }

  let score = 0;
  if (/[A-Z]/.test(passwordValue)) score += 1;
  if (/[a-z]/.test(passwordValue)) score += 1;
  if (/\d/.test(passwordValue)) score += 1;
  if (/[^A-Za-z0-9]/.test(passwordValue)) score += 1;
  if (score < 3) {
    return "密码至少包含大写字母、小写字母、数字、特殊字符中的三项";
  }

  if (passwordValue !== confirmedValue) {
    return "两次输入的密码不一致";
  }

  return "";
};

const register = async () => {
  errorMessage.value = "";

  const validationError = validateRegisterForm();
  if (validationError) {
    errorMessage.value = validationError;
    return;
  }

  try {
    const resp = await apiRequest(API_PATHS.register, {
      method: "POST",
      data: {
        username: username.value,
        password: password.value,
        confirmedPassword: confirmedPassword.value,
      },
    });

    if (resp.error_message !== "success") {
      errorMessage.value = resp.error_message || "Register failed";
      return;
    }

    await router.push({ name: "user_account_login" });
  } catch (error) {
    errorMessage.value = error.message || "Register failed";
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

.rule-tip {
  margin-top: 6px;
  color: #a9bfd3;
  font-size: 12px;
}
</style>
