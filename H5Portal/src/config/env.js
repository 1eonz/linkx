// 开发环境
const development = {
  // baseUrl: "222.209.208.218:8017",
  baseUrl: "172.16.23.30:30884",
  appid: "wx11111111111111",
};

// 测试环境
const test = {
  baseUrl: "10.28.64.58:30280",
  appid: "wx22222222222222",
};

// 生产环境
const production = {
  baseUrl: "10.28.64.58:30280",
  appid: "wx333333333333333",
};

// 注意:这里的属性名要和上面package.json中定义的扩展节点编译名称相同
const config = {
  development,
  test,
  production,
};

export default config;
