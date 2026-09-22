const WS_CLOSE_REASON = "主动关闭";
const WS_CLOSE_CODE = 4444;

function MspWebSocket(url) {
    this.url = url;
    this.renderStatus = 1; // ws实例状态：0-不断开链接，同时不做渲染；1 - 正常链接正常渲染（断开链接请直接销毁ws）
    this.reconnectTimer = 1;
    this.reconnectTimerMax = 3;
    this.reconnectTimerDelay = 3000;
    this.ws = null;
}

MspWebSocket.prototype._wsInit = function () {
    console.log('init websocket');
    const socket = new WebSocket(this.url);
    socket.binaryType ="arraybuffer";
    socket.onopen = () => {
        console.info("🙂---ws链接---open");
    };

    socket.onclose = (event) => {
        const code = event.code,
            reason = event.reason;
        console.info("😑---ws链接---close");
        if (code !== WS_CLOSE_CODE && reason !== WS_CLOSE_REASON){
            this.reconnectWS();
        }
    };

    socket.onmessage = (event) => {
        if (this.renderStatus === 0) {
            return;
        }
        const yuvData = new Uint8Array(event.data);
        postMessage(yuvData);
    };

    socket.onerror = (e) => {
        console.info("😭---ws链接---error", e);
        this.reconnectWS();
    };
    this.ws = socket;
};

MspWebSocket.prototype.reconnectWS = function () {
    if (this.reconnectTimer > this.reconnectTimerMax) {
        return console.error("【ERROR】---WebSocket链接错误", this.url);
    }
    setTimeout( () => {
        this.reconnectTimer++;
        this.ws = new WebSocket(this.url);

        this._wsInit();
    }, this.reconnectTimerDelay);
};

MspWebSocket.prototype.closeWS = function () {
    let code = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : WS_CLOSE_CODE;
    let reason = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : WS_CLOSE_REASON;
    this.ws.close(code, reason);
    this.ws = null;
};

MspWebSocket.prototype.setWsStatus = function (status) {
    let allowValues = [0, 1];
    let ret = allowValues.includes(status) && this.renderStatus !== status;
    if (ret) {
        this.renderStatus = status;
    }
};

let socket;
onmessage = function(msg) {
    switch (msg.data.cmd) {
        case "_wsInit":
            socket = new MspWebSocket(msg.data.data);
            socket._wsInit();
            break;
        case "reconnectWS":
            socket.reconnectWS();
            break;
        case "closeWS":
            socket.closeWS();
            break;
        case "setWsStatus":
            socket.setWsStatus(msg.data.data);
            break;
        default:
            break;
    }
}