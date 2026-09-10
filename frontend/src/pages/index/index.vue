<template>
  <view class="container">
    <text class="title">劳动学时管理</text>
    <text class="subtitle">阶段0：项目初始化完成</text>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const title = ref('劳动学时管理');
</script>

<style lang="scss">
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 32rpx;
  background-color: #f8f8f8;
}

.title {
  font-size: 48rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 16rpx;
}

.subtitle {
  font-size: 28rpx;
  color: #999;
}
</style>
