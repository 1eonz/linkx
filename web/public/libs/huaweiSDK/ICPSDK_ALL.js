function _registerEvent(target, eventType, cb) {
	const eventListener = target.addEventListener || target.attachEvent;
	eventListener.call(target, eventType, cb);

	return {
		remove: function () {
			const eventRemover = target.removeEventListener || target.detachEvent;
			eventRemover.call(target, eventType, cb);
		},
	};
}

function openUriWithTimeoutHack(uri, failCb, successCb, timeoutDuration) {
	let timeout = setTimeout(function () {
		failCb();
		handler.remove();
	}, timeoutDuration);

	let target = window;
	while (target !== target.parent) {
		target = target.parent;
	}

	let handler = _registerEvent(target, 'blur', onBlur);

	function onBlur() {
		clearTimeout(timeout);
		handler.remove();
		successCb();
	}

var iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    document.body.appendChild(iframe);
    iframe.src = uri;

}

window.onbeforeunload = function () {
    sendMsg2ShareWorker(MessageType.LIFECYCLE, ReqEnum.LIFECYCLE.DESTRUCTPAGE, tabPageId);
}

var jsonObject = undefined;
var tabPageId = undefined;
// funcId有两种函数类型：1.数字：JSSDK接口请求的回调函数；2.字符串eventName + envetType:事件的回调函数
var funcId = 1;
// 缓存事件回调函数和接口回调函数
var funcMap = {};

// 消息类型
var MessageType = {
    LIFECYCLE: "lifecycle",
    LOG: "log",
    REQUEST: "request",
    RESPONSE: "response",
    EVENT: "event",
}

// 请求类型
var ReqEnum = {
    LIFECYCLE: {
        REGISTERPAGE: 1, // 注册page
        INITMSP: 2, // 初始化MSP
        DESTRUCTPAGE: 3 // 销毁page
    },
    AUTH: {
        UNIFIEDLOGIN: 101, // 统一登录
        UNIFIEDLOGOUT: 102, // 统一登出
        UNIFIPASSWORDCHANGE: 103, // 统一修改密码
        GETLOGINSTATUS: 104 // 获取用户登录状态
    },
    VOICE: {
        DIAL: 201, // 语音点呼
        ANSWER: 202, // 语音接听
        REJECT: 203, // 语音拒接
        RELEASE: 204, // 语音挂断
        INTERCEPT: 205, // 语音抢话
        HOLD: 206, // 保持
        UNHOLD: 207, // 取保持
        TRANSFER: 208, // 点呼人工转接
        CANCELTRANSFER: 209, // 点呼人工转接取消
        BREAKOFF: 210, // 点呼强拆
        HALFDIAL: 211, // 半双工点呼发起/抢话
        RELEASEHALFDIAL: 212, // 半双工点呼释放
        CLOSEHALFDIAL: 213, // 半双工点呼挂断
        DISCREETLISTEN: 214, // 开启/关闭缜密侦听
        AMBIENCELISTEN: 215, // 开启环境侦听
        SUBSCRIBEUSERSTATUS: 216, // 订阅用户
        UNSUBSCRIBEUSERSTATUS: 217, // 取消用户订阅
        DIALOUT: 218, // 发起PSTN/PLMN 电话呼叫
        RELEASEDIALOUT: 219, // 挂断PSTN/PLMN 电话呼叫
        VOICEBROADCAST: 220 // 发起PSTN/PLMN电话呼叫广播
    },
    VIDEO: {
        MONITORVIDEO: 301, // 视频监控
        DIALVIDEO: 302, // 视频点呼
        ANSWER: 303, // 视频接听
        REJECT: 304, // 视频拒接
        RELEASE: 305, // 视频挂断
        DISPATCHVIDEO: 306, // 视频分发
        CANCELVIDEODISPATCH: 307, // 视频分发取消
        STARTVIDEOUPLOADWALL: 308, // 视频上墙
        STOPVIDEOUPLOADWALL: 309, // 视频下墙
        PTZCTRLCAMERA: 310, // PTZ操作
        SETWSSFLOWWINFMTTYPE: 311 // 改变分辨率
    },
    GROUP: {
        ADDDYNAMICGROUP: 401, // 动态群组创建
        DELETEDYNAMICGROUP: 402, // 动态群组删除
        MODIFYDYNAMICGROUP: 403, // 动态群组修改

        ADDPATCHGROUP: 405, // 派接组创建
        DELETEPATCHGROUP: 406, // 派接组删除
        ADDPATCHGROUPMEMBER: 407, // 添加派接组成员
        DELETEPATCHGROUPMEMBER: 408, // 删除派接组成员

        SUBSCRIBETALKINGGROUP: 411, // 订阅群组
        UNSUBSCRIBETALKINGGROUP: 412, // 去订阅群组
        JOINTALKINGGROUP: 413, // 加入组呼
        SUBJOINTALKINGGROUP: 414, // 订阅并自动加入组呼
        PTTTALKINGGROUP: 415, // 组呼发起或抢权
        PTTRELEASETALKINGGROUP: 416, // 组呼放权
        LEAVETALKINGGROUP: 417, // 退出组呼
        PTTTALKINGGROUPEMERGENCYCALL: 418, // 紧急组呼
        BREAKOFFTALKINGGROUP: 419, // 组呼强拆
        ADDTALKINGGROUPTEMPUSER: 420, // 组呼添加临时用户
        BREAKINTALKINGGROUP: 421, // 组呼强插
        GETPDTGROUPINFO: 422, // 查询PDT用户当前组
        SETLISTENGROUP: 423 // 设置监听组
    },
    CONF: {
        CREATECONF: 501, // 创建音视频会议
        JOINCONF: 502, // 主动加入音视频会议
        ACCEPTAUDIOCONF: 503, // 音频被动入会
        ACCEPTVIDEOCONF: 504, // 视频被动入会
        REJECTAUDIOCONF: 505, // 音频拒接入会
        REJECTVIDEOCONF: 506, // 视频拒接入会
        EXITAUDIOCONF: 507, // 离开音频会议
        EXITVIDEOCONF: 508, // 离开视频会议
        ADDCONFMEMBERS: 509, // 增加与会成员
        HANGUPCONFMEMBER: 510, // 挂断成员
        CALLCONFMEMBER: 511, // 重呼成员
        ENDCONF: 512, // 结束会议
        MUTECONFMEMBER: 513, // 静音会议成员
        MUTECONF: 514, // 静音会议
        HOLDCONF: 515, // 保持会议
        UNHOLDCONF: 516, // 取消保持会议
        WATCHCONFMEMBER: 517, // 选看与会者
        WATCHMIXPICTURE: 518, // 选看多画面
        BROADCASTCONFMEMBER: 519, // 广播与会者
        BROADCASTMIXPICTURE: 520, // 广播多画面
        CANCELBROADCASTCONFMEMBER: 521, // 取消广播与会者
        CANCELBROADCASTMIXPICTURE: 522, // 取消广播多画面
        STARTCONFUPLOADWALL: 523, // 开始会议上墙
        STOPCONFUPLOADWALL: 524, // 停止会议上墙
        QUERYCONFLISTBYATTENDEE: 525, // 查看调度员所在的会议信息
        QUERYRECORDURLBYCONFID: 526, // 会议录制信息查询
        QUERYVWALLDISPLAYMATRIXINFO: 527, // 查询大屏上墙显示模式
        QUERYCONFWALLINFO: 528, // 查询大屏上墙图层
        SETCHAIRMANNAME: 529, // 设置主席名字
        SETSPOKENMEMBER: 530, // 指定发言方
        DELCONFMEMBER: 531, // 删除成员
        PROXYFLOORGROUP: 532, // 群组用户话权代理
        PROXYFLOORGATEWAY: 533, // 网关用户话权代理
        QUERYCONTINUOUSPRESENCEINFO: 534, // 查询多画面信息
        ACCEPTAPPLYFLOOR: 535, // 接受举手申请
        REJECTAPPLYFLOOR: 536, // 拒绝举手申请
        APPLYFLOOR: 537, // 举手申请
        SETCHAIRMAN: 538, // 切换会议主席
        PROLONGCONFTIME: 539, // 延长会议时间
        QUERYVDCADDRESSBOOK: 540, // 查询VDC通讯录
        QUERYCONFERENCELIST: 541, // 查询会议列表
        QUERYCONFERENCEINFO: 542, // 查询指定会议ID信息
        QUERYCONFEREEADDRESSBOOK: 543, // 查询与会人通讯录
        LOCKVIDEOSOURCE: 544, // 锁定视频源
        LOCKCONF: 545, // 锁定会议
        QUERYCONFERENCEBYID: 546, // 根据外部会议ID查询内部会议ID
        MODCONF: 547, // 修改会议
        DELCONF: 548, // 取消预约会议
        QUERYCASCONFINFOS: 549, //查询级联会议结构信息
        QUERYORGTREE: 550, //查询组织树
        QUERYAREAORGINFO: 551 , //查询SMC信息
        QUERYCONFURL: 552, // 查询会议入会链接
        SETTEXTTIPS: 553, //设置会议字幕横幅
    },
    SMS: {
        SENDDISPSMS: 601, // 短信发送
        SENDDISPMMS: 602 // 彩信发送
    },
    GIS: {
        SUBSCRIBEGIS: 701, // GIS订阅
        UNSUBSCRIBEGIS: 702, // GIS取消订阅
        QUERYGISSUBLIST: 703 // GIS订阅终端列表查询
    },
    QUERY: {
        QUERYTALKINGGROUP: 801, // 查询群组（过时）
        QUERYTALKINGGROUPV1: 802, // 查询群组
        QUERYTALKINGGROUPMEMBERS: 803, // 查询群组成员（过时）
        QUERYTALKINGGROUPMEMBERSV1: 804, // 查询群组成员
        QUERYDYNAMICGROUPMEMBERS: 805, // 查询动态组成员
        QUERYSTATICGROUP: 806, // 查询静态组（过时）
        QUERYSTATICGROUPV1: 807, // 查询静态组
        QUERYUSERLIST: 808, // 查询用户（过时）
        QUERYUSERLISTV1: 809, // 查询用户
        QUERYCAMERAS: 810, // 查询摄像头（过时）
        QUERYSPECIFIEDLEVELCAMERA: 811, // 查询指定层级的摄像头
        QUERYDECODER: 812, // 查询解码器
        QUERYDEPARTMENTLIST: 813, // 查询部门
        QUERYCAMERALEVEL: 814, // 查询摄像头层级
        QUERYCAMERALEVELPERMISSION: 815, // 查询摄像头层级权限
        QUERYTALKINGGROUPINFO: 816, // 查询群组属性
        QUERYPATCHGROUPINFO: 817, // 查询派接组属性
        QUERYTALKINGGROUPPATCHEDINFO: 818, // 查询群组被派接属性
        QUERYUSERINFO: 819, // 查询用户属性
        QUERYCAMERAINFO: 820, // 查询摄像头属性
        QUERYCAMERAPERMISSIONINFO: 821, // 查询摄像头权限开关
        QUERYCALLINFO: 822, // 查询视频呼叫信息
        QUERYDCINFO: 823, // 查询调度员属性
        QUERYUEINFO: 824, // 查询终端属性
        QUERYUEGISINFO: 825, // 查询终端GIS配置
        QUERYRECORD: 826, // 查询录音录像文件信息
        QUERYCAMERAGPSINFO: 827, // 获取所有摄像头GPS信息
        QUERYMRS: 828, // 查询录音录像服务器
        QUERYDEVICEIDRELATIONSHIP: 829, // 查询GBID和ISDN对应关系
        QUERYDYNAMICGROUP: 830, // 查询动态组
        QUERYPATCHGROUP: 831, // 查询派接组
        QUERYPATCHGROUPMEMBERS: 832, // 查询派接组成员
        QUERYCAMERASV1: 833, // 查询摄像头
        QUERYGISTRACK: 834, // 查询摄像头
    },
    DEVICE: {
        GETSOUNDDEVICE: 901, // 查询扬声器和麦克风列表
        GETVOLUME: 902, // 查询扬声器音量
        SETVOLUME: 903, // 设置扬声器音量
        MUTESPEAKER: 904, // 静音扬声器
        UNMUTESPEAKER: 905, // 取消静音扬声器
        MUTEMIC: 906, // 静音麦克风
        UNMUTEMIC: 907, // 取消静音麦克风
        QUERYSOUNDDEVICE: 908, // 查询声卡设备
        ASSIGNSOUNDDEVICE: 909, // 切换声卡设备
        SENDDTMF: 910, // 二次拨号
        QUERYCAMERAABILITY: 911, // 查询本地摄像头能力
        STOPPLAYTONE: 912, // 停止播放放音
        GETVERSION: 913, // 版本号查询
        INITMRS: 914, // 初始化录音录像服务器
        FORCEINITMSP: 915, // 强制初始化MSP
        HIDEVIDEOWINDOW: 916, // 窗口隐藏
        WINDOWONTOP: 917, // 窗口置顶
        WINDOWDRAGENABLE: 918, // 窗口可拖动
        SHOWWINDOWBORDER: 919, // 窗口显示标题栏
        SETWINDOWINFO: 920, // 修改窗口位置和大小
        GETRESOLUTION: 921, // 获取屏幕分辨率
        SNAPSHOT: 922, // 截图
        STARTLOCALRECORD: 923, // 开始本地录音录像
        STOPLOCALRECORD: 924, // 关闭本地录音录像
        PLAYAUDIOSHORTMSG: 925, // 语音短消息生成amr文件
        CREATEWINDOW: 926, // 创建用户视频窗口
        DESTROYWINDOW: 927, // 释放用户视频窗口
        RINGTYPE: 928, // 振铃开关
        SETLINKEDPHONE: 929, // 绑定硬话机
        SETRINGVOLUME: 930, // 设置振铃音量
        MUTEGROUP: 931, // 静音群组
        UNMUTEGROUP: 932, // 取消静音群组
        SETGROUPVOLUME: 933, // 设置群组扬声器音量
        GETMSPVERSION: 934, // 获取MSP版本号
        CHECKMSPISINSTALL: 935, // 检查MSP是否已安装
        SETMEDIAMODE:936, // 设置媒体透传开关
        UPGRADEMSPVERSION:937, // MSP版本升级或回退
        SETLOCALRECORDPARAM:938, // 设置本地录音录像位置
        STARTWINDOWSHARE:939, // 开始共享桌面
        STOPWINDOWSHARE:940, // 停止共享桌面
    },
    REGISTER: {
        EVENTREGISTER: 1001
    },
    WEBSOCKET: {
        CHANGEMDCIP: 1101,
        GETLOCALIP: 1102
    },
    UTIL: {
        CLOSEWSSFLOWWINDOW: 1201
    },
}

function printConsoleLog(param) {
    var date = new Date();
    var year = date.getFullYear();
    var month = date.getMonth() + 1;
    month = month < 10 ? "0" + month : month;
    var day = date.getDate();
    day = day < 10 ? "0" + day : day;
    var hour = date.getHours();
    hour = hour < 10 ? "0" + hour : hour;
    var minute = date.getMinutes();
    minute = minute < 10 ? "0" + minute : minute;
    var second = date.getSeconds();
    second = second < 10 ? "0" + second : second;
    var msecond = date.getMilliseconds();
    msecond = msecond < 10 ? "0" + msecond : msecond;
    var current = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + "." + msecond;
    console.log('[' + current + '] ' + param);
}

function printLog(msg, isEvent) {
    if (typeof uiManager === "undefined") {
        return;
    }
    if (uiManager != undefined && uiManager.isSilence) {
        if (msg["eventName"] == "OnRecvGISNotify" ||
            msg["eventName"] == "OnUserStatusNotify") {
            return;
        }
    };

    var logMsg = 'Event';
    if (isEvent == null) {
        uiManager.insertResponseEditor(msg);
        return;
    }

    var date = getCurrentDate();// new Date().format("yyyy-MM-dd hh:mm:ss");
    $('#log-area').val($('#log-area').val() + '[' + date + '] ' + logMsg + ': ' + JSON.stringify(msg) + '\n');
    $('#log-area').scrollTop($('#log-area')[0].scrollHeight);
}

function getCurrentDate() {
    var date = new Date();
    var year = date.getFullYear();
    var month = date.getMonth() + 1;
    month = month < 10 ? "0" + month : month;
    var day = date.getDate();
    day = day < 10 ? "0" + day : day;
    var hour = date.getHours();
    hour = hour < 10 ? "0" + hour : hour;
    var minute = date.getMinutes();
    minute = minute < 10 ? "0" + minute : minute;
    var second = date.getSeconds();
    second = second < 10 ? "0" + second : second;
    var msecond = date.getMilliseconds();
    msecond = msecond < 10 ? "0" + msecond : msecond;
    var current = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + "." + msecond;
    return current;
}

function eventHandle(data) {
    console.log("eventHandle:", data)
    if (data['eventName'] != undefined) {
        var cmd = data['eventName']
    }
    if (data['rsp'] != undefined) {
        var rsp = data['rsp']
    }
    if (data['value'] != undefined) {
        var value = data['value']
    }

    if ("OnCallConnect" == cmd || "OnConfConnect" == cmd) {
        if ("3003" == rsp || "3006" == rsp) {
            if (undefined != value.wssUrl && undefined != value.cid) {
                console.log('video OnCallConnect, create player !!!');
                var div1 = document.getElementById('yuv-player-001');
                var div2 = document.getElementById('yuv-player-002');
                var strWssUrl = value.wssUrl;
                var strCid = value.cid;
                var iPos = strWssUrl.lastIndexOf(':')
                iPort = parseInt(strWssUrl.substr(iPos+1, strWssUrl.length - iPos - 1));
                iWidth = value["width"];
                iHeight = value["height"];
                //iWidth = "480";
                //iHeight = "640";
                /////////////////////////////////// 调用——start
                if (!window.yuvIndex) {
                    window.yuvIndex = 1;
                } else {
                    window.yuvIndex++;
                    window.yuvIndex %= 17;
                    window.yuvIndex = window.yuvIndex == 0 ? 1 : window.yuvIndex;
                }
                const notifyMSP = function () {
                    console.info("调用改变分辨率接口");
                };
                const box = document.getElementById(`yuv-player-00${window.yuvIndex}`);
                console.log('wssflow create player, iPort=%d, iWidth=%d, iHeight=%d, strWssUrl=%s, window.yuvIndex=%d', iPort, iWidth, iHeight, strWssUrl, window.yuvIndex);
                console.info('box---------', box)
                var baseWidth = 16;
                var baseHeight = 9;
                var baseWidthHeight = parseInt(iWidth * 9 / 16);
                var baseHeightWidth = parseInt(iHeight * 16 / 9);
                if (baseWidthHeight <= iHeight) {
                    baseWidth = iWidth;
                    baseHeight = baseWidthHeight;
                } else if (baseHeightWidth <= iWidth) {
                    baseWidth = baseHeightWidth;
                    baseHeight = iHeight;
                }
                box.style.width = baseWidth + 'px';
                box.style.height = baseHeight + 'px';
                new CREATE_PLAYER_DEMO({
                    wsUrl: strWssUrl,
                    port: iPort,
                    box,
                    sharpType: {
                        width: iWidth,
                        height: iHeight
                    },
                    cid: strCid,
                    tabPageId: tabPageId,
                    changeCB: notifyMSP,
                });
                ///////////////////////////////////——end
                return;
            }
        }
    }

    switch (cmd) {
        case "OnModifyPasswordNotify":
            if (rsp == "0") {
                $("#logined-user").text(Config.i18n.common.label.usernotlogined);
            }
            break;
        default:
            break;
    }
}

function sendMsg2ShareWorker(messageType, operateId, param){
    var toWorkJsonMsg = {};
    toWorkJsonMsg['type'] = messageType;
    toWorkJsonMsg['operateId'] = operateId;
    toWorkJsonMsg['tabPageId'] = tabPageId;
    toWorkJsonMsg['value'] = param;
    if (!param['funcId'] && param["callback"]) {
        funcMap[funcId] = param["callback"];
        param['funcId'] = funcId;
        funcId++;
    }
    if(window.myWorker) {
        var jsonMsg = JSON.stringify(toWorkJsonMsg, function(key, val) {
                        return val;
                    });
        window.myWorker.port.postMessage([jsonMsg]);
    }
};

// 检查浏览器是否支持worker
function IsSupportWorker() {
    if (typeof(Worker) === 'undefined')	// 使用Worker前检查一下浏览器是否支持
        $("#showResult").val(' Sorry! No Web Worker support.. ');
    else {
        $("#showResult").val(' Success! The Web Worker support.. ');
        if(window.w){
            printLog("window.w is exist!");
        }else{
            printLog("window.w is not exist!");
        }
    }
};

string2Json = function(strJson) {
	try {
		var a = JSON.parse(strJson, function(k, v) {
			if (null != v && v.indexOf && v.indexOf('function') > -1) {
				return eval("(function(){return " + v + " })()");
			} else if (null != v && v.indexOf && v.indexOf('=>') > -1) { // 支持箭头函数
				return eval("(function(){return " + v + " })()");
			}
			return v;
		});
		return a;
	} catch (e) {
		//alert(e); // error in the above string (in this case, yes)!
		console.log('parse json error: ' + strJson);
		return null;
	}
};

json2String = function(jsonObject) {
	var jsonStrMsg = JSON.stringify(jsonObject, function(key, val) {
		if (typeof val === 'function') {
			return val + '';
		}
		return val;
	});
	return jsonStrMsg;
};

function isJSON(strJson) {
    try {
        var obj = JSON.parse(strJson);
        return true;
    } catch(e) {
        return false;
    }
}

function isNumber(param) {
    if (undefined == param || "" == param) {
		return false;
	}
	var regex = /^[1-9]+[0-9]*$/;
	if (regex.test(param) || /^0$/.test(param)) {
		return true;
	}
	return false;
}

// 与workerJs第二次握手后，status: 1，则isInit: true
var isInit = false;
var sdkVersion = "022.650.160";

(function(root, factory) {
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define([], factory);
    } else if (typeof exports === 'object') {
        // Node. Does not work with strict CommonJS, but
        // only CommonJS-like environments that support module.exports,
        // like Node.
        module.exports = factory();
    } else if (this == undefined) {
        window.ICPSDK = factory();
    } else {
        // Browser globals (root is window)
        root.ICPSDK = factory();
    }
}(this, function() {
    function ICPSDK(config) {
        this.dispatch = {};
        this.config = config;
        this.dispatch.auth = new ICP_Dispatch_Auth();
        this.dispatch.event = new ICP_Dispatch_Event();
        this.dispatch.gis = new ICP_Dispatch_GIS();
        this.dispatch.sms = new ICP_Dispatch_Sms();
        this.dispatch.voice = new ICP_Dispatch_Voice();
        this.dispatch.video = new ICP_Dispatch_Video();
        this.dispatch.group = new ICP_Dispatch_Group();
        this.dispatch.webSocket = new ICP_Dispatch_WebSocket();
        this.dispatch.device = new ICP_Dispatch_Device();
        this.dispatch.query = new ICP_Dispatch_Query();
        this.dispatch.conf = new ICP_Dispatch_Conf();
        this.dispatch.util = new ICP_Dispatch_Util();
        cloudICP = this;
        cloudICP.config["mspVersion"] = sdkVersion;

        if (!!window.SharedWorker && !window.myWorker) {
            printConsoleLog('---- create new worker ----');
            const { origin } = location
            const ip = origin.includes('localhost') ? origin : origin + '/cloudcmd';
            window.myWorker = new SharedWorker(`${ip}/libs/huaweiSDK/worker.js`);

            window.myWorker.port.onmessage = function (e) {
                var jsonMsg = e.data;
                jsonObject = string2Json(jsonMsg)

                if (jsonObject.value != undefined && jsonObject.value != "" && typeof jsonObject.value == 'string') {
                    if (isJSON(jsonObject.value)) {
                        jsonObject.value = string2Json(jsonObject.value);
                    } else if (jsonObject.type == "log") {
                        console.log(jsonObject.value)
                    }
                }

                if (tabPageId == undefined) {
                    console.log("---- initializing the MSP ----")
                    if (jsonObject['tabPageId'] != undefined && jsonObject['tabPageId'] != "") {
                        tabPageId = jsonObject['tabPageId']
                    } else if(jsonObject.value != "" && jsonObject.value != undefined) {
                        if (jsonObject['value']['tabPageId'] != undefined && jsonObject['value']['tabPageId'] != "") {
                            tabPageId = jsonObject['value']['tabPageId']
                        }
                    }
                    if (!config["serverAddress"]) {
                        this.util.log({ "logLevel": "error", "logMsg": "serverAddress is invalid" });
                        throw new Error("serverAddress is invalid");
                        return;
                    }

                    if (!config["serverDomain"]) {
                        config["serverDomain"] = "sdkserver.cloudicp.huawei.com";
                    }

                    if (undefined == config["sdkStatusNotify"] || typeof config["sdkStatusNotify"] != "function") {
                        this.util.log({ "logLevel": "error", "logMsg": "sdkStatusNotify is invalid" });
                        throw new Error("sdkStatusNotify is invalid");
                        return;
                    }

                    if (!config["localMediaServiceWSPort"] || !this.util.checkPort(config["localMediaServiceWSPort"])) {
                        config["localMediaServiceWSPort"] = "17802";
                    }

                    if (!config["localMediaDataServiceWSPort"] || !this.util.checkPort(config["localMediaDataServiceWSPort"])) {
                        config["localMediaDataServiceWSPort"] = "17803";
                    }

                    if (!config["localDeamonServiceWSPort"] || !this.util.checkPort(config["localDeamonServiceWSPort"])) {
                        config["localDeamonServiceWSPort"] = "17801";
                    }

                    if (undefined == config["videoConfLocalWindow"]) {
                        config["videoConfLocalWindow"] = true;
                    } else {
                        config["videoConfLocalWindow"] = config["videoConfLocalWindow"];
                    }

                    if (undefined == config["demo_wssflow_enable"]) {
                        config["demo_wssflow_enable"] = false;
                    } else {
                        config["demo_wssflow_enable"] = config["demo_wssflow_enable"];
                    }

                    if (!config["serverWSPort"]) {
                        config["serverWSPort"] = "8002";
                    }

                    if (!config["serverHttpPort"]) {
                        config["serverHttpPort"] = "8002";
                    }

                    if (!config["debugMode"] || (config["debugMode"] != "true" && config["debugMode"] != "false")) {
                        config["debugMode"] = "false";
                    }

                    if (!config["mode"]) {
                        config["mode"] = "window";
                    }

                    if (!config["ringFlag"]) {
                        config["ringFlag"] = "0";
                    }

                    if (!config["ssl_enable"]) {
                        config["ssl_enable"] = true;
                    }

                    if (config["sdkStatusNotify"]) {
                        funcMap["sdkStatusNotify"] = config["sdkStatusNotify"];
                        delete config["sdkStatusNotify"];
                        config["funcId"] = "sdkStatusNotify";
                    }

                    config["sdkServerHttpTimeout"] = "60000";
                    sendMsg2ShareWorker(MessageType.LIFECYCLE, ReqEnum.LIFECYCLE.INITMSP, config);

                    return;
                }

                if (config["isDemo"] && !config["isRegister"]) {
                    console.log("demo page start register event")
                    registerEvent()
                    config["isRegister"] = true;
                }

                do {
                    if (typeof jsonObject.value.funcId == 'undefined') {
                        break;
                    }

                    var tempFuncId = jsonObject.value.funcId

                    if (jsonObject.value.logType != undefined) {
                        delete jsonObject.value.logType
                    }

                    if ("sdkStatusNotify" == jsonObject.value.funcId) {
                        console.log("---- invoke the sdkStatusNotify function ----")
                        delete jsonObject.value.funcId;
                        if (jsonObject["value"]["status"] == "1") {
                            isInit = true;
                        }

                        funcMap["sdkStatusNotify"](jsonObject.value);
                        break;
                    }

                    console.log("---- invoke the callback function ----")
                    delete jsonObject.value.funcId;
                    if (typeof funcMap[tempFuncId] != 'undefined') {
                        // 根据funcId调用缓存中的回调函数
                        funcMap[tempFuncId](jsonObject.value);

                        if (isNumber(tempFuncId)) { // 如果tempFuncId是数字，调用完成后需要在缓存Map中删除这个接口回调函数
                            delete funcMap[tempFuncId];
                        }
                    }
                } while (false)

                // isdn存在则为登陆或登出接口
                if (jsonObject["isdn"] != undefined && typeof jsonObject["isdn"] != "undefined") {
                    console.log("---- update the user isdn to", jsonObject["isdn"], " ----");

                    if (config["isDemo"]) {
                        if (jsonObject["isdn"] != "" && jsonObject["value"]["rsp"] == "0") { // 登录接口
                            $("#logined-user").text(Config.i18n.common.label.user + jsonObject["isdn"]);
                        } else if (jsonObject["isdn"] == "" && jsonObject["value"]["rsp"] == "0") { // 登出接口
                            $("#logined-user").text(Config.i18n.common.label.usernotlogined);
                        }
                    }
                }

                if (jsonObject.value.eventName != undefined && jsonObject.value.eventName != "" && config["isOpenEventHandle"]) {
                    console.log("handle event")
                    eventHandle(jsonObject.value)
                }

                if (jsonObject["value"] != undefined && jsonObject["value"]["desc"] == 'modify mdcip success !') {
                    console.log("entry print mdc mod");
                    if (jsonObject["value"]["mdcip"]) {
                        config["mdcip"] = jsonObject["value"]["mdcip"];
                        delete jsonObject["value"]["mdcip"];
                    }
                    jsonObject.event = json2String(jsonObject.value).replace(/[\'\"\\\/\b\f\n\r\t]/g, '');
                    printLog(jsonObject.value, true);
                }

            }
            sendMsg2ShareWorker(MessageType.LIFECYCLE, ReqEnum.LIFECYCLE.REGISTERPAGE, "");
        } else {
            IsSupportWorker();
            printLog("++++ 浏览器不支持SharedWorker ++++");
        }
    }
    return ICPSDK;
}));

function ICP_Dispatch_Auth() {
};

function ICP_Dispatch_Event() {
};

function ICP_Dispatch_GIS() {
};

function ICP_Dispatch_Sms() {
};

function ICP_Dispatch_Voice() {
};

function ICP_Dispatch_Video() {
};

function ICP_Dispatch_Group() {
};

function ICP_Dispatch_WebSocket() {
};

function ICP_Dispatch_Device() {
};

function ICP_Dispatch_Query() {
};

function ICP_Dispatch_Conf() {
};

function ICP_Dispatch_Util() {
};

/**
 * login module
 * @param {Object} param {
 *  param: ,
 *  interface:
 * }
 */
ICP_Dispatch_Auth.prototype.unifiedLogin = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.AUTH.UNIFIEDLOGIN, param);
}

ICP_Dispatch_Auth.prototype.unifiedLogout = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.AUTH.UNIFIEDLOGOUT, param);
}

ICP_Dispatch_Auth.prototype.unifiPasswordChange = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.AUTH.UNIFIPASSWORDCHANGE, param);
}

ICP_Dispatch_Auth.prototype.GetLoginStatus = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.AUTH.GETLOGINSTATUS, param);
}

/**
 * voice module
 * @param {Object} param {
 *  param: ,
 *  interface:
 * }
 */
ICP_Dispatch_Voice.prototype.dial = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.DIAL, param);
}

ICP_Dispatch_Voice.prototype.answer = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.ANSWER, param);
}

ICP_Dispatch_Voice.prototype.reject = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.REJECT, param);
}

ICP_Dispatch_Voice.prototype.release = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.RELEASE, param);
}

ICP_Dispatch_Voice.prototype.intercept = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.INTERCEPT, param);
}

ICP_Dispatch_Voice.prototype.hold = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.HOLD, param);
}

ICP_Dispatch_Voice.prototype.unhold = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.UNHOLD, param);
}

ICP_Dispatch_Voice.prototype.transfer = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.TRANSFER, param);
}

ICP_Dispatch_Voice.prototype.cancelTransfer = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.CANCELTRANSFER, param);
}

ICP_Dispatch_Voice.prototype.breakOff = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.BREAKOFF, param);
}

ICP_Dispatch_Voice.prototype.halfDial = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.HALFDIAL, param);
}

ICP_Dispatch_Voice.prototype.releaseHalfDial = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.RELEASEHALFDIAL, param);
}

ICP_Dispatch_Voice.prototype.closeHalfDial = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.CLOSEHALFDIAL, param);
}

ICP_Dispatch_Voice.prototype.discreetListen = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.DISCREETLISTEN, param);
}

ICP_Dispatch_Voice.prototype.ambienceListen = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.AMBIENCELISTEN, param);
}

ICP_Dispatch_Voice.prototype.subscribeUserStatus = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.SUBSCRIBEUSERSTATUS, param);
}

ICP_Dispatch_Voice.prototype.unsubscribeUserStatus = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.UNSUBSCRIBEUSERSTATUS, param);
}

ICP_Dispatch_Voice.prototype.dialout = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.DIALOUT, param);
}

ICP_Dispatch_Voice.prototype.releaseDialout = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.RELEASEDIALOUT, param);
}

ICP_Dispatch_Voice.prototype.voiceBroadcast = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VOICE.VOICEBROADCAST, param);
}

/**
 * video module
 * @param {Object} param {
 *  param: ,
 *  interface:
 * }
 */
ICP_Dispatch_Video.prototype.monitorVideo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.MONITORVIDEO, param);
}

ICP_Dispatch_Video.prototype.dialVideo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.DIALVIDEO, param);
}

ICP_Dispatch_Video.prototype.answer = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.ANSWER, param);
}

ICP_Dispatch_Video.prototype.reject = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.REJECT, param);
}

ICP_Dispatch_Video.prototype.release = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.RELEASE, param);
}

ICP_Dispatch_Video.prototype.dispatchVideo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.DISPATCHVIDEO, param);
}

ICP_Dispatch_Video.prototype.cancelVideoDispatch = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.CANCELVIDEODISPATCH, param);
}

ICP_Dispatch_Video.prototype.startVideoUploadWall = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.STARTVIDEOUPLOADWALL, param);
}

ICP_Dispatch_Video.prototype.stopVideoUploadWall = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.STOPVIDEOUPLOADWALL, param);
}

ICP_Dispatch_Video.prototype.ptzctrlCamera = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.PTZCTRLCAMERA, param);
}

ICP_Dispatch_Video.prototype.setWssflowWinFmttype = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.VIDEO.SETWSSFLOWWINFMTTYPE, param);
}

/**
 * group module
 * @param {Object} param {
 *  param: ,
 *  interface:
 * }
 */
ICP_Dispatch_Group.prototype.addDynamicGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.ADDDYNAMICGROUP, param);
}

ICP_Dispatch_Group.prototype.deleteDynamicGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.DELETEDYNAMICGROUP, param);
}

ICP_Dispatch_Group.prototype.modifyDynamicGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.MODIFYDYNAMICGROUP, param);
}

ICP_Dispatch_Group.prototype.addPatchGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.ADDPATCHGROUP, param);
}

ICP_Dispatch_Group.prototype.deletePatchGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.DELETEPATCHGROUP, param);
}

ICP_Dispatch_Group.prototype.addPatchGroupMember = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.ADDPATCHGROUPMEMBER, param);
}

ICP_Dispatch_Group.prototype.deletePatchGroupMember = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.DELETEPATCHGROUPMEMBER, param);
}

ICP_Dispatch_Group.prototype.subscribeTalkingGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.SUBSCRIBETALKINGGROUP, param);
}

ICP_Dispatch_Group.prototype.unsubscribeTalkingGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.UNSUBSCRIBETALKINGGROUP, param);
}

ICP_Dispatch_Group.prototype.joinTalkingGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.JOINTALKINGGROUP, param);
}

ICP_Dispatch_Group.prototype.subjoinTalkingGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.SUBJOINTALKINGGROUP, param);
}

ICP_Dispatch_Group.prototype.pttTalkingGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.PTTTALKINGGROUP, param);
}

ICP_Dispatch_Group.prototype.pttreleaseTalkingGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.PTTRELEASETALKINGGROUP, param);
}

ICP_Dispatch_Group.prototype.leaveTalkingGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.LEAVETALKINGGROUP, param);
}

ICP_Dispatch_Group.prototype.pttTalkingGroupEmergencyCall = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.PTTTALKINGGROUPEMERGENCYCALL, param);
}

ICP_Dispatch_Group.prototype.breakoffTalkingGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.BREAKOFFTALKINGGROUP, param);
}

ICP_Dispatch_Group.prototype.addTalkingGroupTempUser = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.ADDTALKINGGROUPTEMPUSER, param);
}

ICP_Dispatch_Group.prototype.breakinTalkingGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.BREAKINTALKINGGROUP, param);
}

ICP_Dispatch_Group.prototype.getPDTGroupInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.GETPDTGROUPINFO, param);
}

ICP_Dispatch_Group.prototype.setListenGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GROUP.SETLISTENGROUP, param);
}

/**
 * WebSocket module
 * @param {Object} param {
 *  param: ,
 *  interface:
 * }
 */

ICP_Dispatch_WebSocket.prototype.getLocalIp = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.WEBSOCKET.GETLOCALIP, param);
}

ICP_Dispatch_WebSocket.prototype.ModifyMdcIp = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.WEBSOCKET.CHANGEMDCIP, param);
}

ICP_Dispatch_WebSocket.prototype.getLocalIpCallBack = function (data) {
    if (data["result"] == 0) {
		var param = {
			"status": "0",
			"desc": "get localip and cameraInfo success."
		}
		var date = getCurrentDate();
		$('#log-area').val($('#log-area').val() + '[' + date + '] SDK Status Nofity : ' + JSON.stringify(param) + '\n');
		$('#log-area').scrollTop($('#log-area')[0].scrollHeight);
	}
    config["mdcip"] = data["mdcip"]
}


/**
 * conf module
 * @param {Object} param {
 *  param: ,
 *  interface:
 * }
 */
ICP_Dispatch_Conf.prototype.createConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.CREATECONF, param);
}

ICP_Dispatch_Conf.prototype.joinConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.JOINCONF, param);
}

ICP_Dispatch_Conf.prototype.acceptAudioConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.ACCEPTAUDIOCONF, param);
}

ICP_Dispatch_Conf.prototype.acceptVideoConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.ACCEPTVIDEOCONF, param);
}

ICP_Dispatch_Conf.prototype.rejectAudioConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.REJECTAUDIOCONF, param);
}

ICP_Dispatch_Conf.prototype.rejectVideoConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.REJECTVIDEOCONF, param);
}

ICP_Dispatch_Conf.prototype.exitAudioConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.EXITAUDIOCONF, param);
}

ICP_Dispatch_Conf.prototype.exitVideoConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.EXITVIDEOCONF, param);
}

ICP_Dispatch_Conf.prototype.addConfMembers = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.ADDCONFMEMBERS, param);
}

ICP_Dispatch_Conf.prototype.hangupConfMember = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.HANGUPCONFMEMBER, param);
}

ICP_Dispatch_Conf.prototype.callConfMember = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.CALLCONFMEMBER, param);
}

ICP_Dispatch_Conf.prototype.endConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.ENDCONF, param);
}

ICP_Dispatch_Conf.prototype.muteConfMember = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.MUTECONFMEMBER, param);
}

ICP_Dispatch_Conf.prototype.muteConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.MUTECONF, param);
}

ICP_Dispatch_Conf.prototype.holdConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.HOLDCONF, param);
}

ICP_Dispatch_Conf.prototype.unholdConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.UNHOLDCONF, param);
}

ICP_Dispatch_Conf.prototype.watchConfMember = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.WATCHCONFMEMBER, param);
}

ICP_Dispatch_Conf.prototype.watchMixPicture = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.WATCHMIXPICTURE, param);
}

ICP_Dispatch_Conf.prototype.broadcastConfMember = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.BROADCASTCONFMEMBER, param);
}

ICP_Dispatch_Conf.prototype.broadcastMixPicture = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.BROADCASTMIXPICTURE, param);
}

ICP_Dispatch_Conf.prototype.cancelBroadcastConfMember = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.CANCELBROADCASTCONFMEMBER, param);
}

ICP_Dispatch_Conf.prototype.cancelBroadcastMixPicture = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.CANCELBROADCASTMIXPICTURE, param);
}

ICP_Dispatch_Conf.prototype.startConfUploadWall = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.STARTCONFUPLOADWALL, param);
}

ICP_Dispatch_Conf.prototype.stopConfUploadWall = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.STOPCONFUPLOADWALL, param);
}

ICP_Dispatch_Conf.prototype.queryConferenceList = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYCONFERENCELIST, param);
}

ICP_Dispatch_Conf.prototype.queryConferenceInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYCONFERENCEINFO, param);
}

ICP_Dispatch_Conf.prototype.queryConfListByAttendee = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYCONFLISTBYATTENDEE, param);
}

ICP_Dispatch_Conf.prototype.queryRecordURLByConfID = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYRECORDURLBYCONFID, param);
}

ICP_Dispatch_Conf.prototype.queryVWallDisplayMatrixInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYVWALLDISPLAYMATRIXINFO, param);
}

ICP_Dispatch_Conf.prototype.queryConfWallInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYCONFWALLINFO, param);
}

ICP_Dispatch_Conf.prototype.setChairmanName = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.SETCHAIRMANNAME, param);
}

ICP_Dispatch_Conf.prototype.setSpokenMember = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.SETSPOKENMEMBER, param);
}

ICP_Dispatch_Conf.prototype.delConfMember = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.DELCONFMEMBER, param);
}

ICP_Dispatch_Conf.prototype.proxyFloorGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.PROXYFLOORGROUP, param);
}

ICP_Dispatch_Conf.prototype.proxyFloorGateway = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.PROXYFLOORGATEWAY, param);
}

ICP_Dispatch_Conf.prototype.queryContinuousPresenceInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYCONTINUOUSPRESENCEINFO, param);
}

ICP_Dispatch_Conf.prototype.acceptApplyFloor = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.ACCEPTAPPLYFLOOR, param);
}

ICP_Dispatch_Conf.prototype.rejectApplyFloor = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.REJECTAPPLYFLOOR, param);
}

ICP_Dispatch_Conf.prototype.applyFloor = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.APPLYFLOOR, param);
}

ICP_Dispatch_Conf.prototype.setChairman = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.SETCHAIRMAN, param);
}

ICP_Dispatch_Conf.prototype.prolongConfTime = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.PROLONGCONFTIME, param);
}

ICP_Dispatch_Conf.prototype.queryVDCAddressBook = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYVDCADDRESSBOOK, param);
}

ICP_Dispatch_Conf.prototype.queryConfereeAddressBook = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYCONFEREEADDRESSBOOK, param);
}

ICP_Dispatch_Conf.prototype.lockVideoSource = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.LOCKVIDEOSOURCE, param);
}

ICP_Dispatch_Conf.prototype.lockConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.LOCKCONF, param);
}

ICP_Dispatch_Conf.prototype.queryConferenceById = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYCONFERENCEBYID, param);
}

ICP_Dispatch_Conf.prototype.modConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.MODCONF, param);
}

ICP_Dispatch_Conf.prototype.delConf = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.DELCONF, param);
}

ICP_Dispatch_Conf.prototype.queryCasConfInfos = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYCASCONFINFOS, param);
}

ICP_Dispatch_Conf.prototype.queryOrgTree = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYORGTREE, param);
}

ICP_Dispatch_Conf.prototype.queryAreaOrgInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYAREAORGINFO, param);
}

ICP_Dispatch_Conf.prototype.queryConfURL = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.QUERYCONFURL, param);
}

ICP_Dispatch_Conf.prototype.setTextTips = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.CONF.SETTEXTTIPS, param);
}
/**
 * sms module
 * @param {Object} param {
 *  param: ,
 *  interface:
 * }
 */
ICP_Dispatch_Sms.prototype.sendDispSMS = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.SMS.SENDDISPSMS, param);
}

ICP_Dispatch_Sms.prototype.sendDispMMS = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.SMS.SENDDISPMMS, param);
}

/**
 * gis module
 * @param {Object} param {
 *  passInPrams: ,
 *  interface:
 * }
 */
ICP_Dispatch_GIS.prototype.subscribeGIS = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GIS.SUBSCRIBEGIS, param);
}

ICP_Dispatch_GIS.prototype.unsubscribeGIS = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GIS.UNSUBSCRIBEGIS, param);
}

ICP_Dispatch_GIS.prototype.queryGISSubList = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.GIS.QUERYGISSUBLIST, param);
}

/**
 * query module
 * @param {Object} param {
 *  passInPrams: ,
 *  interface:
 * }
 */
ICP_Dispatch_Query.prototype.queryTalkingGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYTALKINGGROUP, param);
}

ICP_Dispatch_Query.prototype.queryTalkingGroupV1 = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYTALKINGGROUPV1, param);
}

ICP_Dispatch_Query.prototype.queryTalkingGroupMembers = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYTALKINGGROUPMEMBERS, param);
}

ICP_Dispatch_Query.prototype.queryTalkingGroupMembersV1 = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYTALKINGGROUPMEMBERSV1, param);
}

ICP_Dispatch_Query.prototype.queryDynamicGroupMembers = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYDYNAMICGROUPMEMBERS, param);
}

ICP_Dispatch_Query.prototype.queryStaticGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYSTATICGROUP, param);
}

ICP_Dispatch_Query.prototype.queryStaticGroupV1 = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYSTATICGROUPV1, param);
}

ICP_Dispatch_Query.prototype.queryUserList = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYUSERLIST, param);
}

ICP_Dispatch_Query.prototype.queryUserListV1 = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYUSERLISTV1, param);
}

ICP_Dispatch_Query.prototype.queryCameras = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYCAMERAS, param);
}

ICP_Dispatch_Query.prototype.queryCamerasV1 = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYCAMERASV1, param);
}

ICP_Dispatch_Query.prototype.querySpecifiedLevelCamera = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYSPECIFIEDLEVELCAMERA, param);
}

ICP_Dispatch_Query.prototype.queryDecoder = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYDECODER, param);
}

ICP_Dispatch_Query.prototype.queryDepartmentList = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYDEPARTMENTLIST, param);
}

ICP_Dispatch_Query.prototype.queryCameraLevel = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYCAMERALEVEL, param);
}

ICP_Dispatch_Query.prototype.queryCameraLevelPermission = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYCAMERALEVELPERMISSION, param);
}

ICP_Dispatch_Query.prototype.queryTalkingGroupInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYTALKINGGROUPINFO, param);
}

ICP_Dispatch_Query.prototype.queryPatchGroupInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYPATCHGROUPINFO, param);
}

ICP_Dispatch_Query.prototype.queryTalkingGroupPatchedInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYTALKINGGROUPPATCHEDINFO, param);
}

ICP_Dispatch_Query.prototype.queryUserInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYUSERINFO, param);
}

ICP_Dispatch_Query.prototype.queryCameraInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYCAMERAINFO, param);
}

ICP_Dispatch_Query.prototype.queryCameraPermissionInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYCAMERAPERMISSIONINFO, param);
}

ICP_Dispatch_Query.prototype.queryCallInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYCALLINFO, param);
}

ICP_Dispatch_Query.prototype.queryDCInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYDCINFO, param);
}

ICP_Dispatch_Query.prototype.queryUEInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYUEINFO, param);
}

ICP_Dispatch_Query.prototype.queryUEGisInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYUEGISINFO, param);
}

ICP_Dispatch_Query.prototype.queryGISTrack = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYGISTRACK, param);
}

ICP_Dispatch_Query.prototype.queryRecord = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYRECORD, param);
}

ICP_Dispatch_Query.prototype.queryCameraGPSInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYCAMERAGPSINFO, param);
}

ICP_Dispatch_Query.prototype.queryMRS = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYMRS, param);
}

ICP_Dispatch_Query.prototype.queryDeviceIDRelationship = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYDEVICEIDRELATIONSHIP, param);
}

ICP_Dispatch_Query.prototype.queryDynamicGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYDYNAMICGROUP, param);
}

ICP_Dispatch_Query.prototype.queryPatchGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYPATCHGROUP, param);
}

ICP_Dispatch_Query.prototype.queryPatchGroupMembers = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.QUERY.QUERYPATCHGROUPMEMBERS, param);
}

/**
 * device module
 * @param {Object} param {
 *  passInPrams: ,
 *  interface:
 * }
 */
ICP_Dispatch_Device.prototype.getSoundDevice = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.GETSOUNDDEVICE, param);
}

ICP_Dispatch_Device.prototype.getVolume = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.GETVOLUME, param);
}

ICP_Dispatch_Device.prototype.setVolume = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.SETVOLUME, param);
}

ICP_Dispatch_Device.prototype.muteSpeaker = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.MUTESPEAKER, param);
}

ICP_Dispatch_Device.prototype.unmuteSpeaker = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.UNMUTESPEAKER, param);
}

ICP_Dispatch_Device.prototype.muteGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.MUTEGROUP, param);
}

ICP_Dispatch_Device.prototype.unmuteGroup = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.UNMUTEGROUP, param);
}

ICP_Dispatch_Device.prototype.setGroupVolume = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.SETGROUPVOLUME, param)
}

ICP_Dispatch_Device.prototype.muteMic = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.MUTEMIC, param);
}

ICP_Dispatch_Device.prototype.unmuteMic = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.UNMUTEMIC, param);
}

ICP_Dispatch_Device.prototype.querySoundDevice = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.QUERYSOUNDDEVICE, param);
}

ICP_Dispatch_Device.prototype.assignSoundDevice = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.ASSIGNSOUNDDEVICE, param);
}

ICP_Dispatch_Device.prototype.sendDTMF = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.SENDDTMF, param);
}

ICP_Dispatch_Device.prototype.queryCameraAbility = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.QUERYCAMERAABILITY, param);
}

ICP_Dispatch_Device.prototype.stopPlayTone = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.STOPPLAYTONE, param);
}

ICP_Dispatch_Device.prototype.getVersion = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.GETVERSION, param);
}

ICP_Dispatch_Device.prototype.initMRS = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.INITMRS, param);
}

ICP_Dispatch_Device.prototype.forceInitMSP = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.FORCEINITMSP, param);
}

ICP_Dispatch_Device.prototype.hideVideoWindow = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.HIDEVIDEOWINDOW, param);
}

ICP_Dispatch_Device.prototype.windowOnTop = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.WINDOWONTOP, param);
}

ICP_Dispatch_Device.prototype.windowDragEnable = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.WINDOWDRAGENABLE, param);
}

ICP_Dispatch_Device.prototype.showWindowBorder = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.SHOWWINDOWBORDER, param);
}

ICP_Dispatch_Device.prototype.setWindowInfo = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.SETWINDOWINFO, param);
}

ICP_Dispatch_Device.prototype.getResolution = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.GETRESOLUTION, param);
}

ICP_Dispatch_Device.prototype.snapshot = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.SNAPSHOT, param);
}

ICP_Dispatch_Device.prototype.setLocalRecordParam = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.SETLOCALRECORDPARAM, param);
}

ICP_Dispatch_Device.prototype.startWindowShare = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.STARTWINDOWSHARE, param);
}

ICP_Dispatch_Device.prototype.stopWindowShare = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.STOPWINDOWSHARE, param);
}

ICP_Dispatch_Device.prototype.startLocalRecord = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.STARTLOCALRECORD, param);
}

ICP_Dispatch_Device.prototype.stopLocalRecord = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.STOPLOCALRECORD, param);
}

ICP_Dispatch_Device.prototype.playAudioShortMsg = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.PLAYAUDIOSHORTMSG, param);
}

ICP_Dispatch_Device.prototype.createWindow = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.CREATEWINDOW, param);
}

ICP_Dispatch_Device.prototype.destroyWindow = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.DESTROYWINDOW, param);
}

ICP_Dispatch_Device.prototype.ringType = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.RINGTYPE, param);
}

ICP_Dispatch_Device.prototype.setLinkedPhone = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.SETLINKEDPHONE, param);
}

ICP_Dispatch_Device.prototype.setRingVolume = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.SETRINGVOLUME, param);
}

ICP_Dispatch_Device.prototype.getMspVersion = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.GETMSPVERSION, param);
}

ICP_Dispatch_Device.prototype.checkMSPIsInstall = function (param) {
	if (undefined == param || undefined == param.mdcip || !cloudICP.dispatch.util.checkIp(param.mdcip)) {
        console.warn("[checkMSPIsInstall] param is invalid, param:" + JSON.stringify(param));
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
        console.warn("[checkMSPIsInstall] callback is not a function, param:" + JSON.stringify(param));
		return;
	}

	var strUrl = "http://" + param.mdcip + ":8009/mspversion/" + cloudICP.config.mspVersion + "/eSDK_ICP_MSP.zip";
	var strVersion = "MSP_V" + cloudICP.config.mspVersion + ".zip";
    cloudICP.config.hasExeCheckMsp = true;
    cloudICP.config.setMDCIP = param.mdcip;
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.CHECKMSPISINSTALL, param);
	setTimeout(() => {
		openUriWithTimeoutHack(//检测视频插件MSP是否安装、运行
			'myapp://',  //myapp 是已注册的自定义协议
			() => {
				var checkMspBtnObj = document.getElementById("check-msp-btn-msg");
				var messageElement = document.getElementById('open-msp-result-message');
				if (checkMspBtnObj) {
					$("#check-msp-btn-msg").text("MSP插件未安装");
				}
				if (messageElement) {
					messageElement.innerHTML = '<b id="checkmsp-message" style="text-align:left;color:#0000FF;font-size:18px">检测结果：</b>'
					+ '<b style="text-align:left;font-size:18px"> MSP 插件未安装。请下载、安装MSP插件: </b>' + '<a style="color:red;font-size:16px" href="'
					+ strUrl + '">' + strVersion + '</a>';
				}
				param.callback({
					"isInstall" : false,
					"downloadUrl" : strUrl
				});
			},
			() => {
				var checkMspBtnObj = document.getElementById("check-msp-btn-msg");
				var messageElement = document.getElementById('open-msp-result-message');
				if (checkMspBtnObj) {
					$("#check-msp-btn-msg").text("MSP插件已安装");
				}
				if (messageElement) {
					messageElement.innerHTML = '<b id="checkmsp-message" style="text-align:left;color:#0000FF;font-size:18px">检测结果：</b>' + '<font>MSP 插件已安装。</font>';
				}
				param.callback({
					"isInstall" : true
				});
			},
			2000
		);
	}, 100);
}

ICP_Dispatch_Device.prototype.setMediaMode = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.SETMEDIAMODE, param);
}

ICP_Dispatch_Device.prototype.UpgradeMspVersion = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.DEVICE.UPGRADEMSPVERSION, param);
}

ICP_Dispatch_Util.prototype.closeWssflowWindow = function (param) {
    sendMsg2ShareWorker(MessageType.REQUEST, ReqEnum.UTIL.CLOSEWSSFLOWWINDOW, param);
}

ICP_Dispatch_Util.prototype.beforeUnloadSendMsg2ShareWorker = function (param) {
    sendMsg2ShareWorker(MessageType.LIFECYCLE, ReqEnum.LIFECYCLE.DESTRUCTPAGE, tabPageId);
}

ICP_Dispatch_Util.prototype.checkIp = function(ip) {
	if (undefined == ip) {
		return false;
	}
	var regex =
		/^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/;
	if (regex.test(ip)) {
		return true;
	}

	if (/^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]$/.test(ip)) {
		return true;
	}

	return false;
}

/**
 * Event module
 * @param {Object} param {
 *  passInPrams: ,
 *  interface:
 * }
 */
ICP_Dispatch_Event.prototype.register = function (param) { // 注册事件
    // 事件的回调函数funcId为字符串，值为"eventType" + "eventName"
    param["funcId"] = param["eventType"] + param["eventName"];
    // 如果workerJs已经初始化，不需要再向workJs注册，直接缓存，然后return
    funcMap[param["funcId"]] = param["callback"];
    if (isInit == true) {
        return;
    }
    sendMsg2ShareWorker(MessageType.EVENT, ReqEnum.REGISTER.EVENTREGISTER, param);
}
