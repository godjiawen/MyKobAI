<!-- 界面组件。 -->
<template>
  <ContentField>
    <section class="auth-wrap">
      <div class="auth-head">
        <h2>创建账号</h2>
        <p>完成注册后即可开始冲榜。</p>
      </div>
      <div class="row justify-content-md-center">
        <div class="col-lg-5 col-md-7 col-12">
          <form class="auth-form" @submit.prevent="register">
            <div class="mb-3">
              <label for="username" class="form-label">用户名</label>
              <input id="username" v-model="username" type="text" class="form-control" placeholder="请输入用户名" />
              <div class="rule-tip">6-12位，仅字母/数字/下划线</div>
            </div>
            <div class="mb-3">
              <label for="password" class="form-label">密码</label>
              <input id="password" v-model="password" type="password" class="form-control" placeholder="请输入密码" />
              <div class="rule-tip">8-16位，且至少包含两类字符：大写字母/小写字母/数字/特殊字符</div>
            </div>
            <div class="mb-3">
              <label for="confirmedPassword" class="form-label">确认密码</label>
              <input id="confirmedPassword" v-model="confirmedPassword" type="password" class="form-control" placeholder="请再次输入密码" />
            </div>
            <div class="error-message">{{ errorMessage }}</div>
            <button type="submit" class="btn btn-primary auth-btn">注册</button>
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

  let score = 0;
  if (/[A-Z]/.test(passwordValue)) score += 1;
  if (/[a-z]/.test(passwordValue)) score += 1;
  if (/\d/.test(passwordValue)) score += 1;
  if (/[^A-Za-z0-9]/.test(passwordValue)) score += 1;
  if (score < 2) {
    return "密码至少包含大写字母、小写字母、数字、特殊字符中的两项";
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
      errorMessage.value = resp.error_message || "注册失败";
      return;
    }

    await router.push({ name: "user_account_login" });
  } catch (error) {
    errorMessage.value = error.message || "注册失败";
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
  color: var(--kob-text);
}

.auth-head p {
  margin: 8px 0 0;
  color: var(--kob-muted);
}

.auth-form {
  padding: 20px;
  border: 1px solid rgba(90, 180, 255, 0.3);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.6);
}

.auth-btn {
  width: 100%;
  margin-top: 4px;
  border-radius: 12px;
  padding: 10px 0;
  font-weight: 600;
  color: #ffffff;
}

div.error-message {
  min-height: 22px;
  color: #e74c3c;
}

.rule-tip {
  margin-top: 6px;
  color: var(--kob-muted);
  font-size: 12px;
}
</style>
