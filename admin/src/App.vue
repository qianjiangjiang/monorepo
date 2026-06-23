<script setup lang="ts">
import { reactive } from 'vue'
import { Lock, Right, User } from '@element-plus/icons-vue'

const form = reactive({
  username: '',
  password: '',
})

function submitLogin() {
  console.info('admin login placeholder', form.username)
}
</script>

<template>
  <main class="admin-shell">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="brand">
        <span class="brand-mark">D</span>
        <div>
          <h1 id="login-title">解梦管理端</h1>
          <p>AI 渠道与内容运营后台</p>
        </div>
      </div>

      <el-form class="login-form" :model="form" label-position="top" @submit.prevent="submitLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" size="large" placeholder="admin" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            placeholder="password"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-button class="login-button" size="large" type="primary" native-type="submit">
          登录
          <el-icon class="button-icon"><Right /></el-icon>
        </el-button>
      </el-form>
    </section>
  </main>
</template>
