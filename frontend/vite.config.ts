import { defineConfig } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';

// uni-app 前端 Vite 配置
export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5173,
    host: '0.0.0.0',
  },
});
