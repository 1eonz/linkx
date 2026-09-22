1. 改SDK异步登录

```js
    // 在登录成功后，自动注册websocket的事件通知，故要对login的callbacl进行封装
    var ajaxCfg = {
      "type": "PUT",
      "url": url,
      "async": true,
    }
```

2. 引入worker.js地址

```js
    printConsoleLog('---- create new worker ----');
    const { origin } = location
    const ip = origin.includes('localhost') ? origin : origin + '/cloudcmd';
    window.myWorker = new SharedWorker(`${ip}/libs/huaweiSDK/worker.js`);
```

3. 引入YuvplayerWorker.js地址

```js
    const { origin } = location
    const ip = origin.includes('localhost') ? origin : origin + '/cloudcmd';
    const worker = new Worker( `${ip}/libs/huaweiSDK/workers/YuvplayerWorker.js`);
```
