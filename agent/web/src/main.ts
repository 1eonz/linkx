import App from './App.vue';
import { useCreateApp } from './hooks';

// styles
import 'virtual:windi.css';
import 'element-plus/dist/index.css';
import './assets/font/index.css';
import './styles/element-plus.less';

// Register icon sprite
import 'virtual:svg-icons-register';
import './utils/rem';
import 'dayjs/locale/zh-cn';

const bootstrap = () => {
  const app = useCreateApp(App);

  // 挂载
  app.mount('#app');
};

bootstrap();
