function ICPSDK_Dispatch_Auth() {
	this.USER_SEND_HEART_INTERVAL = 30000; //心跳间隔时间30s
	this.VDM_SESSION_TOKEN_INTERVAL = 600000; //心跳间隔时间60s
	this.MAX_PASSWORD_LENGTH = 18; //最大密码长度
	this.errorHeartNumber = 0; //异常心跳次数
	this.USER_SEND_HEART_MAX_TRY = 3; //最大尝试次数
	this.sendHeartbeatTimeOutId = "";
	this.vdmtokenRefresh = "";

	this.sdkserverVersion = "";
};

/**
 * unifiedLogin() unified Login
 * @param {Object} param {
 *  user: ,
 *  password: ,
 *  force: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Auth.prototype.unifiedLogin = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unifiedLogin: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unifiedLogin: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (cloudICP.dispatchSdkStatus == "-1") {
		result["rsp"] = "-3";
		result["desc"] = "unifiedLogin: sdk is doing init. please wait";
		param["callback"](result);
		return;
	}

	if (undefined == cloudICP.config["localIP"] || "" == cloudICP.config["localIP"]) {
		result["rsp"] = "-4";
		result["desc"] = "unifiedLogin: sdk init failed. so cannot get localIP.";
		param["callback"](result);
		return;
	}

	if (undefined == cloudICP.config["cameraInfo"]) {
		result["rsp"] = "-5";
		result["desc"] = "unifiedLogin: sdk init failed. so cannot get cameraInfo.";
		param["callback"](result);
		return;
	}

	var loginParam = {};
	var result = {
		"rsp": "-2",
		"desc": ""
	};

	if (!cloudICP.util.checkUserName(param["user"])) {
		result["desc"] = "unifiedLogin: user is invalid";
		param["callback"](result);
		return;
	}

	if (!(param["password"] && param["password"].length >= 1 && param["password"].length <= this
			.MAX_PASSWORD_LENGTH)) {
		result["desc"] = "unifiedLogin: password is invalid";
		param["callback"](result);
		return;
	}

	loginParam["password"] = param["password"];
	loginParam["localip"] = cloudICP.config["localIP"];
	if (undefined != param["logintype"]) {
		loginParam["logintype"] = param["logintype"];
	}

	if (param["force"] && param["force"] == "true") {
		loginParam["force"] = param["force"];
	}

	loginParam["ringFlag"] = cloudICP.config["ringFlag"];

	if (undefined != cloudICP.config["udcIP"] && undefined != cloudICP.config["udcPort"]) {
		loginParam["serverip"] = cloudICP.config["udcIP"];
		loginParam["serverport"] = cloudICP.config["udcPort"];
	} else {
		loginParam["serverip"] = "";
		loginParam["serverport"] = "";
	}

	param["user"] = encodeURIComponent(param["user"]);
	loginParam["reqId"] = tabPageId;
	loginParam["loginUser"] = param["user"];
	var url = cloudICP.getSdkServerUrl() + "/v1/register/" + param["user"] + "/unifilogin";

	// 在登录成功后，自动注册websocket的事件通知，故要对login的callbacl进行封装
	var ajaxCfg = {
		"type": "PUT",
		"url": url,
		"async": true,
		"data": loginParam,
		"callback": function(data) {
			if (data["rsp"] == "4011") {
				logoutCleanUp();
				data["rsp"] = "0";
				// login success, 保存session并删除，防止暴露给用户
				cloudICP.userInfo["session"] = data["session"];
				if (undefined != data["session"]) {
					delete data.session;
				}
				var urlArray = url.split("/");
				// cloudICP.userInfo["isdn"] = param["isdn"];
				cloudICP.userInfo["isdn"] = data["isdn"]
				cloudICP.userInfo["loginUserName"] = param["user"];
				// delete data["isdn"]
				cloudICP.userInfo["isUnified"] = true;

				cloudICP.userInfo["serverVersion"] = data["version"]

				//登录成功, 启动心跳
				clearTimeout(cloudICP.dispatch.auth.sendHeartbeatTimeOutId);
				cloudICP.dispatch.auth.errorHeartNumber = 0;
				cloudICP.dispatch.auth.sendHeartbeatTimeOutId = setTimeout(function() {
					sendHeartbeat();
				}, cloudICP.dispatch.auth.USER_SEND_HEART_INTERVAL);

				//连接websocket
				cloudICP.dispatch.webSocket.connectToSDKServer();

				clearInterval(cloudICP.dispatch.auth.vdmtokenRefresh);

				cloudICP.dispatch.auth.vdmtokenRefresh = setInterval(function() {
					var murl = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo[
						"isdn"] + "/vmdsession";
					var majaxCfg = {
						"type": "GET",
						"url": murl,
						"data": null,
						"callback": function(mdata) {}
					}
					cloudICP.util.ajax(majaxCfg);
				}, cloudICP.dispatch.auth.VDM_SESSION_TOKEN_INTERVAL);
			} else if (data["rsp"] == "104") {
				cloudICP.userInfo["session"] = data["session"];
			}
			var checkSDKServerWebSocketIsConnectedFunc = function(callback, data) {
				var count = 4;
				var loginWaitTime = setInterval(function() {
					if (cloudICP.loginFlag) {
						cloudICP.util.log(tabPageId, { "logLevel": "info", "logMsg": 'SDKServer WebSocket is Connected return.' });
						clearInterval(loginWaitTime);
						//清理无效视频相关业务的缓存
						var iResult;
						Object.keys(cloudICP.util.callStatusMgr.windowInfos).forEach(function(
							keyWindowInfos) {
							iResult = false;
							if (undefined == cloudICP.util.callStatusMgr.windowInfos[
									keyWindowInfos]["cid"]) { //没有cid
								delete cloudICP.util.callStatusMgr.windowInfos[
									keyWindowInfos];
							} else {
								if ("" == JSON.stringify(cloudICP.util.callStatusMgr
										.videoMediaInfoBuf) || "{}" == JSON.stringify(
										cloudICP.util.callStatusMgr.videoMediaInfoBuf
									)) {
									iResult = true;
								} else {
									Object.keys(cloudICP.util.callStatusMgr
										.videoMediaInfoBuf).forEach(function(key) {
										if (key == cloudICP.util.callStatusMgr
											.windowInfos[keyWindowInfos]["cid"]
										) {
											iResult = true;
											return;
										}
									});
								}
							}
							if (true == iResult) { //业务还没有成功
								delete cloudICP.util.callStatusMgr.windowInfos[
									keyWindowInfos];
							}
						});
						callback(data);
						return;
					} else {
						count--;
						cloudICP.util.log(tabPageId, { "logLevel": "info", "logMsg": "SDKServer WebSocket is not Connected, count=" + count.toString() });
						if (count <= 0) {
							clearInterval(loginWaitTime);
							callback(data);
							return;
						}
					}
				}, 1000);
			};
			checkSDKServerWebSocketIsConnectedFunc(param["callback"], data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * unifiedLogout() unified logout
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Auth.prototype.unifiedLogout = function(param) {
	cloudICP.util.LogInputParam(param);
	cloudICP.util.callStatusMgr.cleanWssflowDemoMap();
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unifiedLogout: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unifiedLogout: callback is not a function"
		});
		return;
	}

	var loginParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "unifiedLogout: User not logged.";
		param["callback"](result);
		return;
	}

	if (cloudICP.userInfo["isUnified"] && !cloudICP.userInfo["isUnified"]) {
		result["desc"] = "logoutFromMDC: User not logged by unifiedLogin.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/register/" + cloudICP.userInfo["isdn"] + "/unifilogout";
	var logoutParam = {};
	logoutParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "DELETE",
		"url": url,
		"async": false,
		"data": logoutParam,
		"callback": function(data) {
			//如果登出成功，清理用户信息
			delete data["user"]
			delete data["ipccserveripport"]
			if (data["rsp"] == "4012" || data["rsp"] == "4017") {
				data["rsp"] = "0";
				logoutCleanUp();
			}

			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * unifiPasswordChange() change password of user
 * @param {Object} param {
 *  user: ,
 *  newPassword: ,
 *  oldPassword: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Auth.prototype.unifiPasswordChange = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unifiPasswordChange: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unifiPasswordChange: callback is not a function"
		});
		return;
	}

	var unifiPasswordChangeParam = {};
	var result = {
		"rsp": "-2",
		"desc": ""
	};

	if (!cloudICP.util.checkUserName(param["user"])) {
		result["desc"] = "unifiPasswordChange: user is invalid.";
		param["callback"](result);
		return;
	}

	if (!(param["newPassword"] && param["newPassword"].length >= 1 && param["newPassword"].length <= this
			.MAX_PASSWORD_LENGTH)) {
		result["desc"] = "unifiPasswordChange: newPassword is invalid";
		param["callback"](result);
		return;
	}

	if (!(param["oldPassword"] && param["oldPassword"].length >= 1 && param["oldPassword"].length <= this
			.MAX_PASSWORD_LENGTH)) {
		result["desc"] = "unifiPasswordChange: oldPassword is invalid";
		param["callback"](result);
		return;
	}

	unifiPasswordChangeParam["newPassword"] = param["newPassword"];
	unifiPasswordChangeParam["oldPassword"] = param["oldPassword"];
	unifiPasswordChangeParam["reqId"] = tabPageId;

	var url = cloudICP.getSdkServerUrl() + "/v1/register/" + param["user"] + "/unifipassword";

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": unifiPasswordChangeParam,
		"callback": function(data) {
			delete data["externs"];
			delete data["isdn"];
			delete data["method"];
			delete data["user"];
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}


ICPSDK_Dispatch_Auth.prototype.GetLoginStatus = function (param) {
	cloudICP.util.LogInputParam(param);
	var result = {
		"rsp": "0",
		"loginStatus":"1",
		"session":""
	};
	if (param["value"] != undefined) {
		param = param.value;
	}
	result["loginStatus"] = Boolean(cloudICP.userInfo["isUnified"])?"0":"1";
	if (undefined != cloudICP.userInfo["session"]){
		result["session"] = cloudICP.userInfo["session"];
	}
	param["callback"](result);
}

/**
 * 发送心跳
 */
function sendHeartbeat() {
	if (!cloudICP.userInfo["isdn"]) {
		cloudICP.util.log(0, {
			"logLevel": "error",
			"logMsg": "sendHeartbeat: User not logged."
		});
		return;
	}
	var url = cloudICP.getSdkServerUrl() + "/v1/heartbeat/" + cloudICP.userInfo["isdn"];
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"callback": function(data) {
			if (data["rsp"] == "0") {
				cloudICP.dispatch.auth.errorHeartNumber = 0;
			} else if (data["rsp"] == "10000") {
				//鉴权失败
				cloudICP.userInfo["isdn"] = "";
				cloudICP.userInfo["session"] = "";
				cloudICP.util.log(0, {
					"logLevel": "error",
					"logMsg": "sendHeartbeat: send failed. the data is " + JSON.stringify(data)
				});

				logoutCleanUp();

				var event = {};
				event["eventName"] = "OnDisConnection";
				event["moduleType"] = "5";
				cloudICP.dispatch.event.EVENT_LIST["ModuleNotify"]["OnDisConnection"].event = event;
				sendEvent2Page(0, cloudICP.dispatch.event.EVENT_LIST["ModuleNotify"]["OnDisConnection"]);
				return;
			} else {
				cloudICP.dispatch.auth.errorHeartNumber++;
				cloudICP.util.log(0, {
					"logLevel": "error",
					"logMsg": "sendHeartbeat: send failed. the data is " + JSON.stringify(data)
				});
			}
			if (cloudICP.dispatch.auth.errorHeartNumber > cloudICP.dispatch.auth.USER_SEND_HEART_MAX_TRY) {
				logoutCleanUp();

				var event = {};
				event["eventName"] = "OnDisConnection";
				event["moduleType"] = "5";
				cloudICP.dispatch.event.EVENT_LIST["ModuleNotify"]["OnDisConnection"].event = event;
				sendEvent2Page(0, cloudICP.dispatch.event.EVENT_LIST["ModuleNotify"]["OnDisConnection"]);
				return;
			}
			cloudICP.dispatch.auth.sendHeartbeatTimeOutId = setTimeout(function() {
				sendHeartbeat();
			}, cloudICP.dispatch.auth.USER_SEND_HEART_INTERVAL);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};


function logoutCleanUp() {
	delete cloudICP.userInfo["isdn"];
	cloudICP.userInfo["session"] = "";
	delete cloudICP.userInfo["isUnified"];
	clearTimeout(cloudICP.dispatch.auth.sendHeartbeatTimeOutId);
	clearInterval(cloudICP.dispatch.auth.vdmtokenRefresh);
	var callStatusMgr = cloudICP.util.callStatusMgr;

	if (cloudICP.dispatch.webSocket.isMSPConflicted != true) {
		// 停止振铃音
		var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}}".format(
			parseInt(callStatusMgr.callingCid)
		);

		cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

		// 删除所有振铃
		var param = "{\"description\":\"msp_clean_tone\", \"cmd\": 131076}";

		cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

		// 删除所有通道
		var param = "{\"description\":\"msp_clean_channel\", \"cmd\": 65550}";

		cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
	} else {
		cloudICP.dispatch.webSocket.isMSPConflicted = false;
	}

	callStatusMgr.cleanUp();
	cloudICP.dispatch.conf.chairName = "chairman";
	cloudICP.dispatch.gis.subList = [];
	cloudICP.dispatch.device.isDTMFDone = true;

	cloudICP.dispatch.group.current_groupcall_num = 0;
	cloudICP.dispatch.group.isCurrentPtt = {
		"grpid": "-1",
		"isPtt": false
	};

	cloudICP.dispatch.video.currentVideoAbility = 0;
};
function ICPSDK_Dispatch_BroadCastInfo() {

};

/**
 * startBroadCast
 * @param {
 *  "text" :
 *  "toNumber" :
 *  callback :
 * }
 */
 ICPSDK_Dispatch_BroadCastInfo.prototype.startBroadCast = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "startBroadCast: param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "startBroadCast: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "startBroadCast: The user does not logined";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "startBroadCast",
        "to" : "1",
        "param": {
            "text" : param["text"],
            "toNumber" : param["toNumber"]
        }
    };

    var url = cloudICP.getSdkServerUrl() + "/v1/broadcastInfo/" + cloudICP.userInfo["isdn"] + "/start";
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    }
    cloudICP.util.ajax(ajaxCfg);

}

/**
 * stopBroadCast
 * @param {
 *  "callGroup" :
 *  callback :
 * }
 */
 ICPSDK_Dispatch_BroadCastInfo.prototype.stopBroadCast = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "stopBroadCast: param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "stopBroadCast: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "stopBroadCast: The user does not logined";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "stopBroadCast",
        "to" : "1",
        "param": {
            "callGroup" : param["callGroup"]
        }
    };

    var url = cloudICP.getSdkServerUrl() + "/v1/broadcastInfo/" + cloudICP.userInfo["isdn"] + "/stop";
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": param["callback"]
    }
    cloudICP.util.ajax(ajaxCfg);
}


/**
 * getBroadCastList
 * @param {
 *  callback :
 * }
 */
 ICPSDK_Dispatch_BroadCastInfo.prototype.getBroadCastList = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "getBroadCastList: param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "getBroadCastList: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "getBroadCastList: The user does not logined";
        param["callback"](result);
        return;
    }

    var url = cloudICP.getSdkServerUrl() + "/v1/broadcastInfo/" + cloudICP.userInfo["isdn"] + "/getInfo";
    var ajaxCfg = {
        "type": "GET",
        "url": url,
        "callback": param["callback"]
    }
    cloudICP.util.ajax(ajaxCfg);
};function ICPSDK_Dispatch_Conf() {
    this.MAX_CONF_ID = "9223372036854775807";

    this.chairName = "chairman";

    this.MAX_CONF_NUM = 1;

    this.MAX_MEMBER_NUM = 244;
};

/**
 * createConf
 * @param {
    *  isVideo :
    *  memberInfos : [
    *   {
    *       "number":
    *       "name":
    *       "isCamera":
    *       "isWatchOnly"
    *       "h265"
    *   }
    *  ]
    *  callback:
    * }
*/
ICPSDK_Dispatch_Conf.prototype.createConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "createConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "createConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };
	if (!param["fmt"])
	{
		cloudICP.util.callStatusMgr.fmt = "1080P";
	}
	else
	{
		cloudICP.util.callStatusMgr.fmt = param["fmt"];
	}
    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "createConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (cloudICP.util.callStatusMgr.currentCallState == cloudICP.util.callStatusMgr.RINGING && undefined == param["time"] && undefined == param["timeZone"]) {
        result["rsp"] = "-4";
        result["desc"] = "createConf: There is a call is ringing."
        param["callback"](result);
        return;
    }

	var strWindowinfo = {};
	if (param["windowInfo"] == undefined || undefined == param["windowInfo"]["mode"] || !cloudICP.util.checkmode(
			param["windowInfo"]["mode"])) {
        cloudICP.util.log(tabPageId, {
			"logLevel": "warn",
			"logMsg": "createConf: The mode is invalid, use defult value " + cloudICP.config["mode"]
		});
		strWindowinfo["mode"] = cloudICP.config["mode"];
	}
	if (param["windowInfo"] != undefined) {
        strWindowinfo["mode"] = param["windowInfo"]["mode"];
        strWindowinfo["showToolbar"] = param["windowInfo"]["showToolbar"];
        strWindowinfo["buttonIDs"] = param["windowInfo"]["buttonIDs"];
        strWindowinfo["posX"] = param["windowInfo"]["posX"];
        strWindowinfo["posY"] = param["windowInfo"]["posY"];
        strWindowinfo["width"] = param["windowInfo"]["width"];
        strWindowinfo["height"] = param["windowInfo"]["height"];
        strWindowinfo["videoResizeMode"] = param["windowInfo"]["videoResizeMode"];
		if ("wssflow" == strWindowinfo["mode"]) { //推流模式
			if (undefined !=cloudICP.util.callStatusMgr.wssflowModeCids &&
		                Object.keys(cloudICP.util.callStatusMgr.wssflowModeCids).length >= 16) {

		                result["desc"] = "createConf: the number of window in wssflow mode is greater than 16.";
		                param["callback"](result);
		                return;
            		}
			if (undefined == param["windowInfo"]["width"] || !cloudICP.util.isNumber(param["windowInfo"][
					"width"
				]) || param["windowInfo"]["width"] > 2147483647 || param["windowInfo"]["width"] < 0) {
				result["desc"] = "createConf: The width is invalid.";
				param["callback"](result);
				return;
			}
			if (undefined == param["windowInfo"]["height"] || !cloudICP.util.isNumber(param["windowInfo"][
					"height"
				]) || param["windowInfo"]["height"] > 2147483647 || param["windowInfo"]["height"] < 0) {
				result["desc"] = "createConf: The height is invalid.";
				param["callback"](result);
				return;
			}
		} else if ("window" == strWindowinfo["mode"]) { //弹窗模式
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["posX"])) {
				result["desc"] += "createConf: The posX is invalid, use defult value 400; ";
				strWindowinfo["posX"] = 400;
			}
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["posY"])) {
				result["desc"] += "createConf: The posY is invalid, use defult value 400; ";
				strWindowinfo["posY"] = 400;
			}
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["width"])) {
				result["desc"] += "createConf: The width is invalid, use defult value 640; ";
				strWindowinfo["width"] = 640;
			}
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["height"])) {
				result["desc"] += "createConf: The height is invalid, use defult value 480; ";
				strWindowinfo["height"] = 480;
			}
			if (undefined == param["windowInfo"]["showToolbar"] || !cloudICP.util.checkshowToolbar(param[
					"windowInfo"]["showToolbar"])) {
				result["desc"] += "createConf: The showToolbar is invalid, use defult value 1; ";
				strWindowinfo["showToolbar"] = 1;
			}
			if (undefined == param["windowInfo"]["buttonIDs"] || !cloudICP.util.checkbuttonIDs(param["windowInfo"][
					"buttonIDs"
				])) {
				result["desc"] += "createConf: The buttonIDs is invalid, use defult value 0; ";
				strWindowinfo["buttonIDs"] = 0;
			}
		} else {
			result["desc"] = "createConf: The mode is invalid.";
			param["callback"](result);
			return;
		}
	} else {
		strWindowinfo["showToolbar"] = 1;
		strWindowinfo["buttonIDs"] = 0;
		strWindowinfo["width"] = 800;
		strWindowinfo["height"] = 450;
	}
	if (undefined == param["time"] && undefined == param["timeZone"]) {
		cloudICP.util.callStatusMgr.windowInfos[2147483647] = strWindowinfo;
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;

    if (callStatusMgr.currentGoingConf != 0 && undefined == param["time"] && undefined == param["timeZone"]) {
        result["rsp"] = "-5";
        result["desc"] = "createConf: The number of conf is limited to " + this.MAX_CONF_NUM;
        param["callback"](result);
        return;
    }

    if (param["isVideo"] != "false" && undefined == param["time"] && undefined == param["timeZone"]) {
        if (callStatusMgr.numberOfCall[callStatusMgr.VIDEO] + callStatusMgr.numberOfCall[callStatusMgr.VIDEOCONF] >= cloudICP.dispatch.video.MAX_VIDEO_DIAL_NUM ||
            cloudICP.dispatch.video.currentVideoAbility >= cloudICP.dispatch.video.MAX_VIDEO_ABILITY) {
            result["rsp"] = "-7";
            result["desc"] = "createConf: The ability of video conf is limited";
            param["callback"](result);
            return;
        }
    }


    if (callStatusMgr.isAnyCallExisted() && undefined == param["time"] && undefined == param["timeZone"]) {
        result["rsp"] = "-6";
        result["desc"] = "createConf: This call is conflicted with other calls";
        param["callback"](result);
        return;
    }

    var tmpMemberInfos = param["memberInfos"].slice();

    var tmpArr = []
    var isHaveChairMan = false;

    for (var index = 0; index < tmpMemberInfos.length; index++) {
        if (tmpMemberInfos[index]["number"] == cloudICP.userInfo["isdn"]) {
            isHaveChairMan = true;
        } else {
            if (tmpMemberInfos[index]["isWatchOnly"] == "true") {
                result["rsp"] = "-2";
                result["desc"] = "createConf: memberInfos is invalid";
                param["callback"](result);
                return;
            }
        }
    }

    if (!isHaveChairMan) {
        tmpMemberInfos.push({
            "number": cloudICP.userInfo["isdn"],
            "name": cloudICP.dispatch.conf.chairName,
            "isCamera": "false",
            "isWatchOnly": "false",
            "h265": "false"
        });
    }

    for (var index = 0; index < tmpMemberInfos.length; index++) {
        tmpArr.push(tmpMemberInfos[index]["number"]);
    }

    var retArr = cloudICP.util.findDuplicates(tmpArr);

    if (retArr.length != 0) {
        result["rsp"] = "-2";
        result["desc"] = "createConf: memberInfos is invalid";
        param["callback"](result);
        return;
    }

    if (tmpMemberInfos.length > cloudICP.dispatch.conf.MAX_MEMBER_NUM) {
        result["rsp"] = "-8";
        result["desc"] = "createConf: memberInfos is out of size";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "create",
        "param": {
            "confInfo": {
                "isVideo": param["isVideo"],
                "isRecorded": "false",
                "confrate": param["confRate"],
            },
            "memberInfos": tmpMemberInfos
        }
    };
    if (undefined != param["time"]) {
        requestParam["param"]["confInfo"]["time"] = param["time"];
    }
    if (undefined != param["timeZone"]) {
        requestParam["param"]["confInfo"]["timeZone"] = param["timeZone"];
    }
    if (undefined != param["timeZoneName"]) {
        requestParam["param"]["confInfo"]["timeZoneName"] = param["timeZoneName"];
    }
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * joinConf
 * @param {
 *  confId : ,
 *  fmt : ,
 *  unifiedAccessCode: ,
 *  passcode: ,
 *  isVideo:
 *  callback :
 * }
 */
ICPSDK_Dispatch_Conf.prototype.joinConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "joinConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "joinConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "joinConf: The user does not logined";
        param["callback"](result);
        return;
    }

    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "joinConf: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.isNumber(param["unifiedAccessCode"])) {
        result["desc"] = "joinConf: unifiedAccessCode is invalid";
        param["callback"](result);
        return;
    }

    if (param["passcode"] != "" && !cloudICP.util.isNumber(param["passcode"])) {
        result["desc"] = "joinConf: passcode is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.isBool(param["isVideo"])) {
        result["desc"] = "joinConf: isVideo is invalid";
        param["callback"](result);
        return;
    }

	if (!param["fmt"]) {
        param["fmt"] = "1080P";
    }

    var fmts = ["4K","2K","1080P", "720P", "D1", "CIF", "QCIF", "GCIF"];
    if (param["fmt"] && !cloudICP.util.includes(fmts, param["fmt"])) {
        result["desc"] = "joinConf: The fmt is invalid";
        param["callback"](result);
        return;
    }

	var strWindowinfo = {};
	if (param["windowInfo"] == undefined || undefined == param["windowInfo"]["mode"] || !cloudICP.util.checkmode(
			param["windowInfo"]["mode"])) {
        cloudICP.util.log(tabPageId, {
			"logLevel": "warn",
			"logMsg": "joinConf: The mode is invalid, use defult value " + cloudICP.config["mode"]
		});
		strWindowinfo["mode"] = cloudICP.config["mode"];
	}
	if (param["windowInfo"] != undefined) {
        strWindowinfo["mode"] = param["windowInfo"]["mode"];
        strWindowinfo["showToolbar"] = param["windowInfo"]["showToolbar"];
        strWindowinfo["buttonIDs"] = param["windowInfo"]["buttonIDs"];
        strWindowinfo["posX"] = param["windowInfo"]["posX"];
        strWindowinfo["posY"] = param["windowInfo"]["posY"];
        strWindowinfo["width"] = param["windowInfo"]["width"];
        strWindowinfo["height"] = param["windowInfo"]["height"];
        strWindowinfo["videoResizeMode"] = param["windowInfo"]["videoResizeMode"];
		if ("wssflow" == strWindowinfo["mode"]) { //推流模式
			if (undefined == param["windowInfo"]["width"] || !cloudICP.util.isNumber(param["windowInfo"][
					"width"
				]) || param["windowInfo"]["width"] > 2147483647 || param["windowInfo"]["width"] < 0) {
				result["desc"] = "joinConf: The width is invalid.";
				param["callback"](result);
				return;
			}
			if (undefined == param["windowInfo"]["height"] || !cloudICP.util.isNumber(param["windowInfo"][
					"height"
				]) || param["windowInfo"]["height"] > 2147483647 || param["windowInfo"]["height"] < 0) {
				result["desc"] = "joinConf: The height is invalid.";
				param["callback"](result);
				return;
			}
		} else if ("window" == strWindowinfo["mode"]) { //弹窗模式
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["posX"])) {
				result["desc"] += "joinConf: The posX is invalid, use defult value 400; ";
				strWindowinfo["posX"] = 400;
			}
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["posY"])) {
				result["desc"] += "joinConf: The posY is invalid, use defult value 400; ";
				strWindowinfo["posY"] = 400;
			}
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["width"])) {
				result["desc"] += "joinConf: The width is invalid, use defult value 640; ";
				strWindowinfo["width"] = 640;
			}
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["height"])) {
				result["desc"] += "joinConf: The height is invalid, use defult value 480; ";
				strWindowinfo["height"] = 480;
			}
			if (undefined == param["windowInfo"]["showToolbar"] || !cloudICP.util.checkshowToolbar(param[
					"windowInfo"]["showToolbar"])) {
				result["desc"] += "joinConf: The showToolbar is invalid, use defult value 1; ";
				strWindowinfo["showToolbar"] = 1;
			}
			if (undefined == param["windowInfo"]["buttonIDs"] || !cloudICP.util.checkbuttonIDs(param["windowInfo"][
					"buttonIDs"
				])) {
				result["desc"] += "joinConf: The buttonIDs is invalid, use defult value 0; ";
				strWindowinfo["buttonIDs"] = 0;
			}
		} else {
			result["desc"] = "joinConf: The mode is invalid.";
			param["callback"](result);
			return;
		}
	}

    if (cloudICP.util.callStatusMgr.currentCallState == cloudICP.util.callStatusMgr.RINGING) {
        result["rsp"] = "-4";
        result["desc"] = "joinConf: There is a call is ringing."
        param["callback"](result);
        return;
    }

    var callStatusMgr = cloudICP.util.callStatusMgr;

    callStatusMgr.windowInfos[param["unifiedAccessCode"]] = strWindowinfo;
    if (callStatusMgr.currentGoingConf != 0) {
        result["rsp"] = "-5";
        result["desc"] = "joinConf: The number of conf is limited to " + this.MAX_CONF_NUM;
        param["callback"](result);
        return;
    }

    if (param["isVideo"] != "false") {
        if (callStatusMgr.numberOfCall[callStatusMgr.VIDEO] + callStatusMgr.numberOfCall[callStatusMgr.VIDEOCONF] >= cloudICP.dispatch.video.MAX_VIDEO_DIAL_NUM ||
            cloudICP.dispatch.video.currentVideoAbility >= cloudICP.dispatch.video.MAX_VIDEO_ABILITY) {
            result["rsp"] = "-7";
            result["desc"] = "joinConf: The ability of video conf is limited";
            param["callback"](result);
            return;
        }
    }


    if (callStatusMgr.isAnyCallExisted()) {
        result["rsp"] = "-6";
        result["desc"] = "joinConf: This call is conflicted with other calls";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "join",
        "param": {
            "confId": param["confId"],
            "fmt": param["fmt"],
            "video_format": cloudICP.config.cameraInfo.toString(),
            "audio_format": "1",
            "unifiedAccessCode": param["unifiedAccessCode"],
            "passcode": param["passcode"],
            "isVideo": param["isVideo"]
        }
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * acceptAudioConf
 * @param {
    *  cid :
    *  callback :
    * }
*/
ICPSDK_Dispatch_Conf.prototype.acceptAudioConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "acceptAudioConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "acceptAudioConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "acceptAudioConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkCid(param["cid"])) {
        result["desc"] = "acceptAudioConf: cid is invalid";
        param["callback"](result);
        return;
    }

    var calls =  cloudICP.util.callStatusMgr.allCalls;
    if (calls[param["cid"]] && calls[param["cid"]]["callState"] >=  cloudICP.util.callStatusMgr.CONNECTING) {
        result["rsp"] = "-40002";
        result["desc"] = "";
        param["callback"](result);
        return;
    }

    var callStatusMgr =  cloudICP.util.callStatusMgr;

    if (callStatusMgr.currentGoingConf != 0) {
        result["rsp"] = "-5";
        result["desc"] = "acceptAudioConf: The number of conf is limited to " + this.MAX_CONF_NUM;
        param["callback"](result);
        return;
    }
    if (callStatusMgr.connectingCallMgr[callStatusMgr.VIDEO].length != 0 ||
        callStatusMgr.connectingCallMgr[callStatusMgr.DISCREET].length != 0 ||
        callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE].length != 0 ||
        callStatusMgr.connectingCallMgr[callStatusMgr.VIDEOCONF].length != 0 ) {
        result["rsp"] = "-4";
        result["desc"] = "acceptAudioConf: The call is conflicted with other calls"
        param["callback"](result);
        return;
    };

    var requestParam = {
        "opt": "audioAccept",
        "cid": param["cid"],
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * acceptVideoConf
 * @param {
    *  cid :
    *  windowInfo: {
    *      width:
    *      height:
    *      posX:
    *      posY:
    *  }
    *  callback :
    * }
*/
ICPSDK_Dispatch_Conf.prototype.acceptVideoConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "acceptVideoConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "acceptVideoConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "acceptVideoConf: The user does not logined";
        param["callback"](result);
        return;
    }

    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkCid(param["cid"])) {
        result["desc"] = "acceptVideoConf: cid is invalid";
        param["callback"](result);
        return;
    }

    var strWindowinfo = {};
	if (param["windowInfo"] == undefined || undefined == param["windowInfo"]["mode"] || !cloudICP.util.checkmode(
			param["windowInfo"]["mode"])) {
        cloudICP.util.log(tabPageId, {
			"logLevel": "warn",
			"logMsg": "acceptVideoConf: The mode is invalid, use defult value " + cloudICP.config["mode"]
		});
		strWindowinfo["mode"] = cloudICP.config["mode"];
	}
	if (param["windowInfo"] != undefined) {
        strWindowinfo["mode"] = param["windowInfo"]["mode"];
        strWindowinfo["showToolbar"] = param["windowInfo"]["showToolbar"];
        strWindowinfo["buttonIDs"] = param["windowInfo"]["buttonIDs"];
        strWindowinfo["posX"] = param["windowInfo"]["posX"];
        strWindowinfo["posY"] = param["windowInfo"]["posY"];
        strWindowinfo["width"] = param["windowInfo"]["width"];
        strWindowinfo["height"] = param["windowInfo"]["height"];
        strWindowinfo["videoResizeMode"] = param["windowInfo"]["videoResizeMode"];
		if ("wssflow" == strWindowinfo["mode"]) { //推流模式
			if (undefined == param["windowInfo"]["width"] || !cloudICP.util.isNumber(param["windowInfo"][
					"width"
				]) || param["windowInfo"]["width"] > 2147483647 || param["windowInfo"]["width"] < 0) {
				result["desc"] = "acceptVideoConf: The width is invalid.";
				param["callback"](result);
				return;
			}

            if (undefined == param["windowInfo"]["height"] || !cloudICP.util.isNumber(param["windowInfo"]["height"]) || param["windowInfo"]["height"] > 2147483647 || param["windowInfo"]["height"] < 0) {
                result["desc"] = "acceptVideoConf: The height is invalid.";
                param["callback"](result);
                return;
            }
        } else if ("window" == strWindowinfo["mode"]) {//弹窗模式
            if (!cloudICP.util.checkWindowParam(param["windowInfo"]["posX"])) {
                result["desc"] += "acceptVideoConf: The posX is invalid, use defult value 400; ";
                strWindowinfo["posX"] = 400;
            }
            if (!cloudICP.util.checkWindowParam(param["windowInfo"]["posY"])) {
                result["desc"] += "acceptVideoConf: The posY is invalid, use defult value 400; ";
                strWindowinfo["posY"] = 400;
            }
            if (!cloudICP.util.checkWindowParam(param["windowInfo"]["width"])) {
                result["desc"] += "acceptVideoConf: The width is invalid, use defult value 640; ";
                strWindowinfo["width"] = 640;
            }
            if (!cloudICP.util.checkWindowParam(param["windowInfo"]["height"])) {
                result["desc"] += "acceptVideoConf: The height is invalid, use defult value 480; ";
                strWindowinfo["height"] = 480;
            }
            if (undefined == param["windowInfo"]["showToolbar"] || !cloudICP.util.checkshowToolbar(param["windowInfo"]["showToolbar"])) {
                result["desc"] += "acceptVideoConf: The showToolbar is invalid, use defult value 1; ";
                strWindowinfo["showToolbar"] = 1;
            }
            if (undefined == param["windowInfo"]["buttonIDs"] || !cloudICP.util.checkbuttonIDs(param["windowInfo"]["buttonIDs"])) {
                result["desc"] += "acceptVideoConf: The buttonIDs is invalid, use defult value 0; ";
                strWindowinfo["buttonIDs"] = 0;
            }
        } else {
            result["desc"] = "acceptVideoConf: The mode is invalid.";
            param["callback"](result);
            return;
        }
        strWindowinfo["iCid"] = param["cid"];
    } else {
        strWindowinfo["showToolbar"] = 1;
        strWindowinfo["buttonIDs"] = 0;
        strWindowinfo["iCid"] = param["cid"];
        strWindowinfo["width"] = 1280;
        strWindowinfo["height"] = 720;
    }

    var calls =  cloudICP.util.callStatusMgr.allCalls;
    if (calls[param["cid"]] && calls[param["cid"]]["callState"] >=  cloudICP.util.callStatusMgr.CONNECTING) {
        result["rsp"] = "-40002";
        result["desc"] = "";
        param["callback"](result);
        return;
    }

    var callStatusMgr = cloudICP.util.callStatusMgr;
    callStatusMgr.windowInfos[param["cid"]] = strWindowinfo;
    if (callStatusMgr.currentGoingConf != 0) {
        result["rsp"] = "-5";
        result["desc"] = "acceptVideoConf: The number of conf is limited to " + this.MAX_CONF_NUM;
        param["callback"](result);
        return;
    }
    if (callStatusMgr.connectingCallMgr[callStatusMgr.VIDEO].length != 0 ||
        callStatusMgr.connectingCallMgr[callStatusMgr.DISCREET].length != 0 ||
        callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE].length != 0 ||
        callStatusMgr.connectingCallMgr[callStatusMgr.VIDEOCONF].length != 0 ) {
        result["rsp"] = "-4";
        result["desc"] = "acceptVideoConf: The call is conflicted with other calls"
        param["callback"](result);
        return;
    };

    var requestParam = {
        "opt": "videoAccept",
        "cid": param["cid"],
        "param": {
            "video_format": cloudICP.config.cameraInfo.toString(),
            "audio_format": "1"
        }
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * rejectAudioConf
 * @param {
    *  cid :
    *  callback :
    * }
*/
ICPSDK_Dispatch_Conf.prototype.rejectAudioConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "rejectAudioConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "rejectAudioConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "rejectAudioConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkCid(param["cid"])) {
        result["desc"] = "rejectAudioConf: cid is invalid";
        param["callback"](result);
        return;
    }

    var calls =  cloudICP.util.callStatusMgr.allCalls;
    if (calls[param["cid"]] && calls[param["cid"]]["callState"] >= cloudICP.util.callStatusMgr.CONNECTING) {
        var str;
        if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.CONNECTING) {
            str = "rejectAudioConf: The cid is answered";
        } else if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.HOLD) {
            str = "rejectAudioConf: The cid is holded";
        } else if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.RELEASE) {
            str = "rejectAudioConf: The cid is ended";
        } else {
            str = "rejectAudioConf: The cid is invalid";
        }
        result["rsp"] = "-4";
        result["desc"] = str;
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "audioReject",
        "cid": param["cid"],
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "0") {
				// 停止振铃音
				var mediaParam =
					"{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}}".format(
						parseInt(param["cid"])
					);

                cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam, true);

                var callStatusMgr = cloudICP.util.callStatusMgr;
                callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.RELEASE,  {"value": {"cid": param["cid"]}});
            }

            param["callback"](data);
        }
    }
    cloudICP.util.ajax(ajaxCfg);
};

/**
 * rejectVideoConf
 * @param {
    *  cid :
    *  callback :
    * }
*/
ICPSDK_Dispatch_Conf.prototype.rejectVideoConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "rejectVideoConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "rejectVideoConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "rejectVideoConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkCid(param["cid"])) {
        result["desc"] = "rejectVideoConf: cid is invalid";
        param["callback"](result);
        return;
    }

    var calls =  cloudICP.util.callStatusMgr.allCalls;
    if (calls[param["cid"]] && calls[param["cid"]]["callState"] >= cloudICP.util.callStatusMgr.CONNECTING) {
        var str;
        if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.CONNECTING) {
            str = "rejectVideoConf: The cid is answered";
        } else if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.HOLD) {
            str = "rejectVideoConf: The cid is holded";
        } else if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.RELEASE) {
            str = "rejectVideoConf: The cid is ended";
        } else {
            str = "rejectVideoConf: The cid is invalid";
        }
        result["rsp"] = "-4";
        result["desc"] = str;
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "videoReject",
        "cid": param["cid"],
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "0") {
				// 停止振铃音
				var mediaParam =
					"{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}}".format(
						parseInt(param["cid"])
					);

                cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam, true);

                var callStatusMgr = cloudICP.util.callStatusMgr;
                callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.RELEASE,  {"value": {"cid": param["cid"]}});
            }

            param["callback"](data);
        }
    }
    cloudICP.util.ajax(ajaxCfg);
};

/**
 * addConfMembers @param {
 *  confId :
 *  memberInfos : [
 *   {
 *       "number":
 *       "name":
 *       "isCamera":
 *       "isWatchOnly":
 *       "h265":
 *   }
 *  ]
 *  callback :
 * }
 */
ICPSDK_Dispatch_Conf.prototype.addConfMembers = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "addConfMembers: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "addConfMembers: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "addConfMembers: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "addConfMembers: confId is invalid";
        param["callback"](result);
        return;
    }

    // TODO: 会议人数规格需要确认。
    if (!(param["memberInfos"] && Array.isArray(param["memberInfos"]) && param["memberInfos"].length > 0)) {
        result["desc"] = "addConfMembers: memberInfos is invalid";
        param["callback"](result);
        return;
    }

    var length = param["memberInfos"].length;
    for (var i = 0; i < length; i++) {
        var user = param["memberInfos"][i];
        if (!cloudICP.util.checkIsdn(user["number"])) {
            result["desc"] = "addConfMembers: number is invalid";
            param["callback"](result);
            return;
        }

        if (!user["name"]) {
            result["desc"] = "addConfMembers: name is invalid";
            param["callback"](result);
            return;
        }

        if (!cloudICP.util.isBool(user["isCamera"])) {
            result["desc"] = "addConfMembers: isCamera is invalid";
            param["callback"](result);
            return;
        }

        if (!cloudICP.util.isBool(user["isWatchOnly"])) {
            result["desc"] = "addConfMembers: isWatchOnly is invalid";
            param["callback"](result);
            return;
        }

        if (!cloudICP.util.isBool(user["h265"])) {
            result["desc"] = "addConfMembers: h265 is invalid";
            param["callback"](result);
            return;
        }
    }

    var tmpArr = []
    var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];

    // 检查当前会议状态
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-5";
            result["desc"] = "addConfMembers: The permission of chairman is required.";
            param["callback"](result);
            return;
        }

        if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
            result["rsp"] = "-6";
            result["desc"] = "addConfMembers: The chairman is leaved.";
            param["callback"](result);
            return;
        }

        if (confInfo["confMembersStatus"] && param["memberInfos"].length + confInfo["confMembersStatus"].length > cloudICP.dispatch.conf.MAX_MEMBER_NUM) {
            result["rsp"] = "-9";
            result["desc"] = "addConfMembers: memberInfos is out of size";
            param["callback"](result);
            return;
        }
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "addConfMembers: The member is leaved.";
        param["callback"](result);
        return;
    }

    for (var index = 0; index < param["memberInfos"].length; index++) {
        if (param["memberInfos"][index]["number"] == cloudICP.userInfo["isdn"]) {
            result["rsp"] = "-2";
            result["desc"] = "addConfMembers: memberInfos is invalid";
            param["callback"](result);
            return;
        } else {
            if (param["memberInfos"][index]["isWatchOnly"] == "true") {
                result["rsp"] = "-2";
                result["desc"] = "addConfMembers: memberInfos is invalid";
                param["callback"](result);
                return;
            }
        }
        tmpArr.push(param["memberInfos"][index]["number"]);


        var userStatus = cloudICP.util.callStatusMgr.userStatusMgr[param["memberInfos"][index]["number"]];
        if (userStatus != undefined && userStatus == "4021") {
            result["rsp"] = "-8";
            result["desc"] = "addConfMembers: The member is rigning.";
            param["callback"](result);
            return;
        }
    }
    var retArr = cloudICP.util.findDuplicates(tmpArr);

    if (retArr.length != 0) {
        result["rsp"] = "-2";
        result["desc"] = "addConfMembers: memberInfos is invalid";
        param["callback"](result);
        return;
    }

    if (param["memberInfos"].length> cloudICP.dispatch.conf.MAX_MEMBER_NUM) {
        result["rsp"] = "-9";
        result["desc"] = "addConfMembers: memberInfos is out of size";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "addMembers",
        "param": {
            "confId": param["confId"],
            "memberInfos": param["memberInfos"]
        }
    };

    if (param["casConfParam"] != undefined) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * hangupConfMember @param {
*  confId :
*  memberInfo : {
*       "number":
*   }
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.hangupConfMember = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "hangupConfMember: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "hangupConfMember: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "hangupConfMember: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "hangupConfMember: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["memberInfo"]["number"])) {
        result["desc"] = "hangupConfMember: number is invalid";
        param["callback"](result);
        return;
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-5";
            result["desc"] = "hangupConfMember: The permission of chairman is required.";
            param["callback"](result);
            return;
        }

        if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
            result["rsp"] = "-6";
            result["desc"] = "hangupConfMember: The chairman is leaved.";
            param["callback"](result);
            return;
        }

        for (var index = 0; index < confInfo["value"]["confMembersStatus"].length; index++) {
            var confMember = confInfo["value"]["confMembersStatus"][index];

            if (confMember["number"] == param["memberInfo"]["number"] && (confMember["participantStatus"] == "Disconnected" ||
            confMember["participantStatus"] == "Connecting")) {
                result["rsp"] = "-7";
                result["desc"] = "hangupConfMember: The member is disconnected or connecting.";
                param["callback"](result);
                return;
            }
        }
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "hangupConfMember: The member is leaved.";
        param["callback"](result);
        return;
    }

    if (param["memberInfo"]["number"] == cloudICP.userInfo["isdn"]) {
        result["rsp"] = "-2";
        result["desc"] = "hangupConfMember: number is invalid";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "hangupMember",
        "param": {
            "confId": param["confId"],
            "memberInfo": param["memberInfo"]
        }
    };

    if (param["casConfParam"] != undefined) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * callConfMember @param {
*  confId :
*  memberInfo :
*   {
*       "number":
*   }
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.callConfMember = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "callConfMember: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "callConfMember: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "callConfMember: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "callConfMember: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["memberInfo"]["number"])) {
        result["desc"] = "callConfMember: number is invalid";
        param["callback"](result);
        return;
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-5";
            result["desc"] = "callConfMember: The permission of chairman is required.";
            param["callback"](result);
            return;
        }

        if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
            result["rsp"] = "-8";
            result["desc"] = "callConfMember: The chairman is leaved.";
            param["callback"](result);
            return;
        }

        for (var index = 0; index < confInfo["value"]["confMembersStatus"].length; index++) {
            var confMember = confInfo["value"]["confMembersStatus"][index];
            if (confMember["number"] == param["memberInfo"]["number"] && (confMember["participantStatus"] == "Connected"
            || confMember["participantStatus"] == "Connecting")) {
                result["rsp"] = "-7";
                result["desc"] = "callConfMember: The member is connected or connecting.";
                param["callback"](result);
                return;
            }
        }
    }

    var userStatus = cloudICP.util.callStatusMgr.userStatusMgr[param["memberInfo"]["number"]];
    if (userStatus != undefined && userStatus == "4021") {
        result["rsp"] = "-6";
        result["desc"] = "callConfMember: The member is already conference member or is ringing conference member.";
        param["callback"](result);
        return;
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-8";
        result["desc"] = "callConfMember: The member is leaved.";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "recallMember",
        "param": {
            "confId": param["confId"],
            "memberInfo": param["memberInfo"]
        }
    };

    if (param["casConfParam"] != undefined) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
* endConf @param {
*  confId :
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.endConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "endConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "endConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "endConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "endConf: confId is invalid";
        param["callback"](result);
        return;
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"] + "/close/" + param["confId"];
	var requestParam = {};
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "DELETE",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "0") {
				var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
				if (confInfo) {
					confInfo["isEnding"] = true;
				}
			}
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * exitAudioConf
 * @param {
    *  cid :
    *  callback :
    * }
*/
ICPSDK_Dispatch_Conf.prototype.exitAudioConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "exitAudioConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "exitAudioConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "exitAudioConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkCid(param["cid"])) {
        result["desc"] = "exitAudioConf: cid is invalid";
        param["callback"](result);
        return;
    }

    var call = cloudICP.util.callStatusMgr.allCalls[param["cid"]];
    if (call && call["caller"] != cloudICP.userInfo["isdn"] && call.callState == cloudICP.util.callStatusMgr.RINGING) {
        result["rsp"] = "-4";
        result["desc"] = "exitAudioConf: The callee can't release a ringing call.";
        param["callback"](result);
        return;
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[cloudICP.util.callStatusMgr.currentGoingConf];
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] == confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-5";
            result["desc"] = "exitAudioConf: The chairman can't leave conf.";
            param["callback"](result);
            return;
        }
    }

    var requestParam = {
        "opt": "audioLeave",
        "cid": param["cid"],
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "1") {
				data["rsp"] = "0";
			}
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * exitVideoConf
 * @param {
    *  cid :
    *  callback :
    * }
*/
ICPSDK_Dispatch_Conf.prototype.exitVideoConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "exitVideoConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "exitVideoConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "exitVideoConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkCid(param["cid"])) {
        result["desc"] = "exitVideoConf: cid is invalid";
        param["callback"](result);
        return;
    }

    var call = cloudICP.util.callStatusMgr.allCalls[param["cid"]];
    if (call && call["caller"] != cloudICP.userInfo["isdn"] && call.callState == cloudICP.util.callStatusMgr.RINGING) {
        result["rsp"] = "-4";
        result["desc"] = "exitVideoConf: The callee can't release a ringing call.";
        param["callback"](result);
        return;
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[cloudICP.util.callStatusMgr.currentGoingConf];
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] == confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-5";
            result["desc"] = "exitVideoConf: The chairman can't leave conf.";
            param["callback"](result);
            return;
        }
    }

    var requestParam = {
        "opt": "videoLeave",
        "cid": param["cid"],
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "1") {
				data["rsp"] = "0";
			}
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
* muteConfMember @param {
*  confId :
*  isMute :
*  number :
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.muteConfMember = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "muteConfMember: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "muteConfMember: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "muteConfMember: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "muteConfMember: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.isBool(param["isMute"])) {
        result["desc"] = "muteConfMember: isMute is invalid";
        param["callback"](result);
        return;
    }

    // 判断 param["isQuiet"] 是否为空,为空通过，不为空检查其值
    if (param["isQuiet"] === undefined || param["isQuiet"] === null) {
    } else if (!cloudICP.util.isBool(param["isQuiet"])) {
        // 如果 param["isQuiet"] 有值，则判断是否为布尔值
        result["desc"] = "muteConfMember: isQuiet is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["number"])) {
        result["desc"] = "muteConfMember: number is invalid";
        param["callback"](result);
        return;
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
    if (confInfo != undefined) {
        if ((cloudICP.userInfo["isdn"] != param["number"]) && (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"])) {
            result["rsp"] = "-5";
            result["desc"] = "muteConfMember: The permission of chairman is required.";
            param["callback"](result);
            return;
        }

        if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
            result["rsp"] = "-6";
            result["desc"] = "muteConfMember: The current member is leaved.";
            param["callback"](result);
            return;
        }
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "muteConfMember: The member is leaved.";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "muteMember",
        "param": {
            "confId": param["confId"],
            "isMute": param["isMute"],
            "isQuiet":param["isQuiet"],
            "number": param["number"]
        }
    };

    if (param["casConfParam"] != undefined) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
* muteConf @param {
*  confId :
*  isMute :
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.muteConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "muteConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "muteConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "muteConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "muteConf: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.isBool(param["isMute"])) {
        result["desc"] = "muteConf: isMute is invalid";
        param["callback"](result);
        return;
    }

    // 判断 param["isQuiet"] 是否为空,为空通过，不为空检查其值
    if (param["isQuiet"] === undefined || param["isQuiet"] === null) {
    } else if (!cloudICP.util.isBool(param["isQuiet"])) {
        // 如果 param["isQuiet"] 有值，则判断是否为布尔值
        result["desc"] = "muteConf: isQuiet is invalid";
        param["callback"](result);
        return;
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-5";
            result["desc"] = "muteConf: The permission of chairman is required.";
            param["callback"](result);
            return;
        }

        if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
            result["rsp"] = "-6";
            result["desc"] = "muteConf: The chairman is leaved.";
            param["callback"](result);
            return;
        }
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "muteConf: The member is leaved.";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "muteConf",
        "param": {
            "confId": param["confId"],
            "isMute": param["isMute"],
            "isQuiet":param["isQuiet"]
        }
    };
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
* watchConfMember @param {
*  confId :
*  number :
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.watchConfMember = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "watchConfMember: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "watchConfMember: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "watchConfMember: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "watchConfMember: confId is invalid";
        param["callback"](result);
        return;
    }
    if (!cloudICP.util.checkIsdn(param["number"])) {
        result["desc"] = "watchConfMember: number is invalid";
        param["callback"](result);
        return;
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "watchConfMember: The member is leaved.";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "watchMember",
        "param": {
            "confId": param["confId"],
            "number": param["number"]
        }
    };
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * broadcastMixPicture @param {
*  confId :
*  mixPictureType :
*  memberInfos : [
*   {
*       "number":
*   }
*  ]
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.broadcastMixPicture = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "broadcastMixPicture: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "broadcastMixPicture: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "broadcastMixPicture: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "broadcastMixPicture: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!param["mixPictureType"]) {
        result["desc"] = "broadcastMixPicture: The mixPictureType is invalid";
        param["callback"](result);
        return;
    }

    // TODO: 会议人数规格需要确认。
    if (!(param["memberInfos"] && Array.isArray(param["memberInfos"]) && param["memberInfos"].length > 0)) {
        result["desc"] = "broadcastMixPicture: memberInfos is invalid";
        param["callback"](result);
        return;
    }

    var length = param["memberInfos"].length;
    for (var i = 0; i < length; i++) {
        var user = param["memberInfos"][i];
        if (!cloudICP.util.checkIsdn(user["number"])) {
            result["desc"] = "broadcastMixPicture: number is invalid";
            param["callback"](result);
            return;
        }
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-5";
            result["desc"] = "broadcastMixPicture: The permission of chairman is required.";
            param["callback"](result);
            return;
        }
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "broadcastMixPicture: The member is leaved.";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "broadcastMix",
        "param": {
            "confId": param["confId"],
            "mixPictureType": param["mixPictureType"],
            "memberInfos": param["memberInfos"]
        }
    };
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * watchMixPicture @param {
*  confId :
*  mixPictureType :
*  memberInfos : [
*   {
*       "number":
*   }
*  ]
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.watchMixPicture = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "watchMixPicture: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "watchMixPicture: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "watchMixPicture: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "watchMixPicture: confId is invalid";
        param["callback"](result);
        return;
    }


    if (!param["mixPictureType"]) {
        result["desc"] = "watchMixPicture: The mixPictureType is invalid";
        param["callback"](result);
        return;
    }

    // TODO: 会议人数规格需要确认。
    if (!(param["memberInfos"] && Array.isArray(param["memberInfos"]) && param["memberInfos"].length > 0)) {
        result["desc"] = "watchMixPicture: memberInfos is invalid";
        param["callback"](result);
        return;
    }

    var length = param["memberInfos"].length;
    for (var i = 0; i < length; i++) {
        var user = param["memberInfos"][i];
        if (!cloudICP.util.checkIsdn(user["number"])) {
            result["desc"] = "watchMixPicture: number is invalid";
            param["callback"](result);
            return;
        }
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "watchMixPicture: The member is leaved.";
        param["callback"](result);
        return;
    }

    //  2.0会议系统进入，限制非主席调用选看多画面；3.0会议系统则放开，非主席也能调用选看多画面
    if (cloudICP.util.isNumber(param["confId"])) {
        var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
        if (confInfo != undefined) {
            if (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"]) {
                result["rsp"] = "-5";
                result["desc"] = "watchMixPicture: The permission of chairman is required.";
                param["callback"](result);
                return;
            }
        }
    }

    var requestParam = {
        "opt": "watchMix",
        "param": {
            "confId": param["confId"],
            "mixPictureType": param["mixPictureType"],
            "memberInfos": param["memberInfos"]
        }
    };
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
* broadcastConfMember @param {
*  confId :
*  number :
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.broadcastConfMember = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "broadcastConfMember: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "broadcastConfMember: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "broadcastConfMember: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "broadcastConfMember: confId is invalid";
        param["callback"](result);
        return;
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-5";
            result["desc"] = "broadcastConfMember: The permission of chairman is required.";
            param["callback"](result);
            return;
        }
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "broadcastConfMember: The member is leaved.";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "broadcastMember",
        "param": {
            "confId": param["confId"],
            "number": param["number"]
        }
    };
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }
	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
* cancelBroadcastConfMember @param {
*  confId :
*  number :
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.cancelBroadcastConfMember = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "cancelBroadcastConfMember: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "cancelBroadcastConfMember: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "cancelBroadcastConfMember: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "cancelBroadcastConfMember: confId is invalid";
        param["callback"](result);
        return;
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-5";
            result["desc"] = "cancelBroadcastConfMember: The permission of chairman is required.";
            param["callback"](result);
            return;
        }

        if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
            result["rsp"] = "-6";
            result["desc"] = "cancelBroadcastConfMember: The chairman is leaved.";
            param["callback"](result);
            return;
        }

        // 限制只能取消广播正在广播中的成员
        if (confInfo["value"]["confStatus"]["broadcast"] != param["number"]) {
            result["rsp"] = "-7";
            result["desc"] = "cancelBroadcastConfMember: The member is not broadcasting.";
            param["callback"](result);
            return;
        }
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "cancelBroadcastConfMember: The member is leaved.";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "cancelBroadcastMember",
        "param": {
            "confId": param["confId"],
            "number": param["number"]
        }
    };
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
* cancelBroadcastMixPicture @param {
*  confId :
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.cancelBroadcastMixPicture = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "cancelBroadcastMixPicture: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "cancelBroadcastMixPicture: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "cancelBroadcastMixPicture: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "cancelBroadcastMixPicture: confId is invalid";
        param["callback"](result);
        return;
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-5";
            result["desc"] = "cancelBroadcastMixPicture: The permission of chairman is required.";
            param["callback"](result);
            return;
        }

        if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
            result["rsp"] = "-6";
            result["desc"] = "cancelBroadcastMixPicture: The chairman is leaved.";
            param["callback"](result);
            return;
        }
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "cancelBroadcastMixPicture: The member is leaved.";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "cancelBroadcastMix",
        "param": {
            "confId": param["confId"]
        }
    };
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * startConfUploadWall
 * @param {
    *  confId :
    *  number :
    *  layerInfo :
    *   {
    *       "xPos":
    *       "yPos":
    *       "width":
    *       "height":
    *       "decoder":
    *       "termType":
    *       "termName":
    *   }
    *
    *  callback:
    * }
*/
ICPSDK_Dispatch_Conf.prototype.startConfUploadWall = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "startConfUploadWall: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "startConfUploadWall: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "startConfUploadWall: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "startConfUploadWall: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["number"])) {
        result["desc"] = "startConfUploadWall: number is invalid";
        param["callback"](result);
        return;
    }

    if (!param["layerInfo"]) {
        result["desc"] = "startConfUploadWall: layerInfo is invalid";
        param["callback"](result);
        return;
    }

    var layerInfo = param["layerInfo"];

    if (!layerInfo["xPos"] || !cloudICP.util.isNumber(layerInfo["xPos"])) {
        result["desc"] = "startConfUploadWall: xPos is invalid";
        param["callback"](result);
        return;
    }

    if (!layerInfo["yPos"] || !cloudICP.util.isNumber(layerInfo["yPos"])) {
        result["desc"] = "startConfUploadWall: yPos is invalid";
        param["callback"](result);
        return;
    }

    if (!layerInfo["width"] || !cloudICP.util.isNumber(layerInfo["width"])) {
        result["desc"] = "startConfUploadWall: width is invalid";
        param["callback"](result);
        return;
    }

    if (!layerInfo["height"] || !cloudICP.util.isNumber(layerInfo["height"])) {
        result["desc"] = "startConfUploadWall: height is invalid";
        param["callback"](result);
        return;
    }

    if (undefined == layerInfo["decoder"] || !cloudICP.util.isStrInArray(layerInfo["decoder"], ["ivs", "elte", "te", "dec", "custom"])) {
        result["desc"] = "startConfUploadWall: decoder is invalid";
        param["callback"](result);
        return;
    }

    if (!layerInfo["termType"] || !cloudICP.util.isStrInArray(layerInfo["termType"], ["ivs_camera", "elte_phone", "voip", "pc", "dec_site"])) {
        result["desc"] = "startConfUploadWall: termType is invalid";
        param["callback"](result);
        return;
    }

    if (!layerInfo["termName"] || !cloudICP.util.checkIsdn(layerInfo["termName"])) {
        result["desc"] = "startConfUploadWall: termName is invalid";
        param["callback"](result);
        return;
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "startConfUploadWall: The current member is leaved.";
        param["callback"](result);
        return;
    }

    var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];
    if (confInfo != undefined) {
        if (cloudICP.userInfo["isdn"] != confInfo["value"]["confStatus"]["chair"]) {
            result["rsp"] = "-7";
            result["desc"] = "startConfUploadWall: The permission of chairman is required.";
            param["callback"](result);
            return;
        }
    }

    var murl = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/vmdsession";
    var majaxCfg = {
        "type": "GET",
        "url": murl,
        "data": null,
        "callback": function(mdata) {
            if (mdata["rsp"] == "0") {
                var requestParam = {
                    "opt": "wallStart",
                    "param": {
                        "confId": param["confId"],
                        "number": param["number"],
                        "layerInfo": param["layerInfo"]
                    }
                };

                var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
                var ajaxCfg = {
                    "type": "POST",
                    "url": url,
                    "data": requestParam,
                    "callback": function(data) {
                        param["callback"](data);
                    }
                }
                cloudICP.util.ajax(ajaxCfg);
            } else {
                param["callback"]({"rsp": "-1", "desc": ""});
            }
        }
    }
    cloudICP.util.ajax(majaxCfg);
};

/**
 * stopConfUploadWall
 * @param {
    *  layerID:
    *  termName:
    *  owner:
    *  callback:
    * }
*/
ICPSDK_Dispatch_Conf.prototype.stopConfUploadWall = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "stopConfUploadWall: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "stopConfUploadWall: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "stopConfUploadWall: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!param["layerID"]) {
        result["desc"] = "stopConfUploadWall: layerID is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["owner"])) {
        result["desc"] = "stopConfUploadWall: owner is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["termName"])) {
        result["desc"] = "stopConfUploadWall: termName is invalid";
        param["callback"](result);
        return;
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "stopConfUploadWall: The member is leaved.";
        param["callback"](result);
        return;
    }

    var murl = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/vmdsession";
    var majaxCfg = {
        "type": "GET",
        "url": murl,
        "data": null,
        "callback": function(mdata) {
            if (mdata["rsp"] == "0") {
                var requestParam = {
                    "opt": "wallStop",
                    "param": {
                        "layerID": param["layerID"],
                        "owner": param["owner"],
                        "termName": param["termName"]
                    }
                };

                var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
                var ajaxCfg = {
                    "type": "POST",
                    "url": url,
                    "data": requestParam,
                    "callback": function(data) {
                        param["callback"](data);
                    }
                }
                cloudICP.util.ajax(ajaxCfg);
            } else {
                param["callback"]({"rsp": "-1", "desc": ""});
            }
        }
    }
    cloudICP.util.ajax(majaxCfg);
};

/**
 * holdConf
 * @param {
    *  cid:
    *  callback:
    * }
*/
ICPSDK_Dispatch_Conf.prototype.holdConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "holdConf: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "holdConf: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "holdConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";
    if (!cloudICP.util.checkCid(param["cid"])) {
        result["desc"] = "holdConf: The cid is invalid";
        param["callback"](result);
        return;
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "holdConf: The current member is leaved.";
        param["callback"](result);
        return;
    }

	var requestParam = {
		"opt": "hold",
		"param": {
			type: "1",
			cid: param["cid"],
			reqId: tabPageId
		}
	};

    var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function(data) {
            if (data["rsp"] == "0") {
                cloudICP.util.callStatusMgr.holdMgr.push(param["cid"]);
            }
            param["callback"](data);
        }
    }
    cloudICP.util.ajax(ajaxCfg);
};

/**
 * unholdConf
 * @param {
    *  cid:
    *  callback:
    * }
*/
ICPSDK_Dispatch_Conf.prototype.unholdConf = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unholdConf: param is invalid"
		});
		return;
	}

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "unholdConf: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "unholdConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";
    if (!cloudICP.util.checkCid(param["cid"])) {
        result["desc"] = "unholdConf: The cid is invalid";
        param["callback"](result);
        return;
    }

    if (cloudICP.util.callStatusMgr.currentGoingConf == 0) {
        result["rsp"] = "-6";
        result["desc"] = "unholdConf: The current member is leaved.";
        param["callback"](result);
        return;
    }

    var callStatusMgr = cloudICP.util.callStatusMgr;
    if (callStatusMgr.numberOfCall[callStatusMgr.VIDEO] + callStatusMgr.numberOfCall[callStatusMgr.VIDEOCONF] > 0) {
        result["rsp"] = "-7";
        result["desc"] = "unholdConf: There is a video existed";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "unhold",
        "param": {
            type: "1",
            cid: param["cid"]
        }
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "0") {
				cloudICP.util.callStatusMgr.holdMgr.push(param["cid"]);
			}
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryRecordURLByConfID
 * @param {
    *  confId:
    *  callback:
    * }
*/
ICPSDK_Dispatch_Conf.prototype.queryRecordURLByConfID = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryRecordURLByConfID: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryRecordURLByConfID: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryRecordURLByConfID: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";
    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "queryRecordURLByConfID: The confId is invalid";
        param["callback"](result);
        return;
    }


    var requestParam = {
        "opt": "queryRecord",
        "param": {
            confId: param["confId"],
        }
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryConfListByAttendee
 * @param {
    *  number:
    *  callback:
    * }
*/
ICPSDK_Dispatch_Conf.prototype.queryConfListByAttendee = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryConfListByAttendee: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryConfListByAttendee: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryConfListByAttendee: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    //result["rsp"] = "-2";
    // if (!cloudICP.util.checkIsdn(param["number"])) {
    //     result["desc"] = "queryConfListByAttendee: The number is invalid";
    //     param["callback"](result);
    //     return;
    // }

    var requestParam = {
        "opt": "queryByAttendee",
        "param": {
            number: cloudICP.userInfo["isdn"]
        }
    };

    if (undefined != param["confID"]) {
        requestParam["param"]["confID"] = param["confID"];
    }

    if (undefined != param["queryType"]) {
        requestParam["param"]["queryType"] = param["queryType"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryVWallDisplayMatrixInfo
 * @param {
    *  callback:
    * }
*/
ICPSDK_Dispatch_Conf.prototype.queryVWallDisplayMatrixInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryVWallDisplayMatrixInfo: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryVWallDisplayMatrixInfo: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryVWallDisplayMatrixInfo: The user does not logined";
        param["callback"](result);
        return;
    }

	var murl = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/vmdsession" + "?reqId=" + tabPageId;
	var majaxCfg = {
		"type": "GET",
		"url": murl,
		"data": null,
		"callback": function(mdata) {
			if (mdata["rsp"] == "0") {
				var requestParam = {
					"opt": "queryVwallDisplay",
				};

				var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
				requestParam["reqId"] = tabPageId;
				var ajaxCfg = {
					"type": "POST",
					"url": url,
					"data": requestParam,
					"callback": function(data) {
						param["callback"](data);
					}
				}
				cloudICP.util.ajax(ajaxCfg);
			} else {
				param["callback"]({
					"rsp": "-1",
					"desc": ""
				});
			}
		}
	}
	cloudICP.util.ajax(majaxCfg);
};

/**
 * queryConfWallInfo
 * @param {
    *  callback:
    * }
*/
ICPSDK_Dispatch_Conf.prototype.queryConfWallInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryConfWallInfo: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryConfWallInfo: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryConfWallInfo: The user does not logined";
        param["callback"](result);
        return;
    }

	var murl = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/vmdsession" + "?reqId=" + tabPageId;
	var majaxCfg = {
		"type": "GET",
		"url": murl,
		"data": null,
		"callback": function(mdata) {
			if (mdata["rsp"] == "0") {
				var requestParam = {
					"opt": "queryVwallLayer",
				};

				var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
				requestParam["reqId"] = tabPageId;
				var ajaxCfg = {
					"type": "POST",
					"url": url,
					"data": requestParam,
					"callback": function(data) {
						param["callback"](data);
					}
				}
				cloudICP.util.ajax(ajaxCfg);
			} else {
				param["callback"]({
					"rsp": "-1",
					"desc": ""
				});
			}
		}
	}
	cloudICP.util.ajax(majaxCfg);
};

/**
 * setChairmanName
 * @param {
    *  name: ,
    *  callback: ,
    * }
*/
ICPSDK_Dispatch_Conf.prototype.setChairmanName = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setChairmanName: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setChairmanName: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "setChairmanName: The user does not logined";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";
    if (!param["name"] || typeof(param["name"]) != "string" || param["name"].length > 32) {
        result["desc"] = "setChairmanName: The name is invalid";
        param["callback"](result);
        return;
    }

    cloudICP.dispatch.conf.chairName = param["name"];

    param["callback"]({"rsp": "0", "desc": ""});
};

/**
 * setSpokenMember
 * @param {
    *  confId: ,
    *  member: ,
    * }
*/
ICPSDK_Dispatch_Conf.prototype.setSpokenMember = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setSpokenMember: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setSpokenMember: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "setSpokenMember: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "setSpokenMember: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["member"])) {
        result["desc"] = "setSpokenMember: member is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.isBool(param["isSpoken"])) {
        result["desc"] = "setSpokenMember: isSpoken is invalid";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "SetSpokenMember",
        "param": {
            "confId": param["confId"],
            "member": param["member"],
            "isSpoken": param["isSpoken"]
        }
    };
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * delConfMember @param {
*  confId :
*  memberInfo : {
*       "number":
*   }
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.delConfMember = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "delConfMember: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "delConfMember: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "delConfMember: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "delConfMember: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["memberInfo"]["number"])) {
        result["desc"] = "delConfMember: number is invalid";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "delMember",
        "param": {
            "confId": param["confId"],
            "memberInfo": param["memberInfo"]
        }
    };

    if (param["casConfParam"] != undefined) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};


/**
 * proxyFloorGroup
 * @param {
 *  confId: ,
 *  member: ,
 * }
 */
ICPSDK_Dispatch_Conf.prototype.proxyFloorGroup = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "proxyFloorGroup: param is invalid" });
        return;
    }

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "proxyFloorGroup: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    param["type"] = "0";

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "proxyFloorGroup: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "proxyFloorGroup: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["member"])) {
        result["desc"] = "proxyFloorGroup: member is invalid";
        param["callback"](result);
        return;
    }

    if (!param["operation"] || typeof param["operation"] != "string") {
        result["desc"] = "proxyFloorGroup: operation is invalid";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "proxyFloor",
        "param": param
    };

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * proxyFloorGateway
 * @param {
 *  confId: ,
 *  member: ,
 * }
*/
ICPSDK_Dispatch_Conf.prototype.proxyFloorGateway = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}

    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "proxyFloorGateway: param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "proxyFloorGateway: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    param["type"] = "1";

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "proxyFloorGateway: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "proxyFloorGateway: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["member"])) {
        result["desc"] = "proxyFloorGateway: member is invalid";
        param["callback"](result);
        return;
    }

    if (!param["operation"] || typeof param["operation"] != "string") {
        result["desc"] = "proxyFloorGateway: operation is invalid";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "proxyFloor",
        "param": param
    };

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryContinuousPresenceInfo
 * @param {
    *  confId: ,
    *  member: ,
    * }
*/
ICPSDK_Dispatch_Conf.prototype.queryContinuousPresenceInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryContinuousPresenceInfo: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryContinuousPresenceInfo: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryContinuousPresenceInfo: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "queryContinuousPresenceInfo: confId is invalid";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "queryContinuousPresenceInfo",
        "param": param
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};

/**
 * AcceptApplyFloor
 * @param {
    *  confId: ,
    *  applier: ,
    * }
*/
ICPSDK_Dispatch_Conf.prototype.acceptApplyFloor = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "AcceptApplyFloor: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "AcceptApplyFloor: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "AcceptApplyFloor: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "AcceptApplyFloor: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["applier"])) {
        result["desc"] = "AcceptApplyFloor: applier is invalid";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "acceptApplyFloor",
        "param": param
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};

/**
 * RejectApplyFloor
 * @param {
    *  confId: ,
    *  applier: ,
    * }
*/
ICPSDK_Dispatch_Conf.prototype.rejectApplyFloor = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "rejectApplyFloor: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "rejectApplyFloor: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "rejectApplyFloor: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "rejectApplyFloor: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["applier"])) {
        result["desc"] = "rejectApplyFloor: applier is invalid";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "rejectApplyFloor",
        "param": param
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};

/**
 * applyFloor
 * @param {
    *  confId: ,
    *  applier: ,
    * }
*/
ICPSDK_Dispatch_Conf.prototype.applyFloor = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "applyFloor: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "applyFloor: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "applyFloor: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "applyFloor: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["applier"])) {
        result["desc"] = "applyFloor: applier is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["floorType"])) {
        result["desc"] = "applyFloor: floorType is invalid";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "applyFloor",
        "param": param
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};

/**
 * setChairman 切换会议主席
 * @param {
 *  confId: ,
 *  applier: ,
 * }
 */
ICPSDK_Dispatch_Conf.prototype.setChairman = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setChairman: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setChairman: callback is not a function"
		});
		return;
	}

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "setChairman: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "applyFloor: confId is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkIsdn(param["applier"])) {
        result["desc"] = "applyFloor: applier is invalid";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "setChairman",
        "param": param
    };

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * prolongConfTime 延长会议时间
 * @param {
    *  confId: ,
    *  applier: ,
    * }
*/
ICPSDK_Dispatch_Conf.prototype.prolongConfTime = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "prolongConfTime: param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "prolongConfTime: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "prolongConfTime: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "applyFloor: confId is invalid";
        param["callback"](result);
        return;
    }
    var body = {};
    Object.keys(param).forEach(function (key) {
        if ("callback" != key) {
            body[key] = param[key];
        }
    });
    var requestParam = {
        "opt": "prolongConf",
        "param": body
    };

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/prolong/" + cloudICP.userInfo["isdn"];
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function (data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};
/**
 * queryVDCAddressBook 查询VDC通讯录
 * @param {
 *  currentPage: ,
 * }
*/
ICPSDK_Dispatch_Conf.prototype.queryVDCAddressBook = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "queryVDCAddressBook: param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "queryVDCAddressBook: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryVDCAddressBook: The user does not logined";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "vdcaddressbook",
        "param": param
    };

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"]+ "/vdcaddressbook";
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function (data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryConfereeAddressBook 查询与会通讯录
 * @param {
 *  querytype: ,
 * }
*/
ICPSDK_Dispatch_Conf.prototype.queryConfereeAddressBook = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "queryConfereeAddressBook: param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "queryConfereeAddressBook: callback is not a function" });
        return;
    }
    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryConfereeAddressBook: The user does not logined";
        param["callback"](result);
        return;
    }

    if (undefined == param["querytype"]) {
        param["querytype"] = "users";
    }

    var requestParam = {
        "opt": "queryaddressbook",
        "param": param
    };

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function (data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryConferenceList() query Conference list
 * @param {Object} param {
 *  "callback": ,
 * }
 */
 ICPSDK_Dispatch_Conf.prototype.queryConferenceList = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "queryConferenceList": "param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "queryConferenceList": "callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryConferenceList: User not logged.";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];

    var requestParam = {
        "opt": "queryConferenceList",
        "param": {}
    };
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "async": true,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryConferenceInfo() query Conference list
 * @param {Object} param {
 *  "confId": ,
 *  "callback": ,
 * }
 */
 ICPSDK_Dispatch_Conf.prototype.queryConferenceInfo = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "queryConferenceInfo": "param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "queryConferenceInfo": "callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryConferenceInfo: User not logged.";
        param["callback"](result);
        return;
    }

    if ("" == param["confId"] || undefined == param["confId"]) {
        result["desc"] = "queryConferenceInfo: confId is null.";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    var body = {};
    Object.keys(param).forEach(function (key) {
        if ("callback" != key) {
            body[key] = param[key];
        }
    });
    var requestParam = {
        "opt": "queryConferenceInfo",
        "param": body
    };
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "async": true,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * lockVideoSource 锁定视频源
 * @param {
 *  confId: ,
 *  memberInfos: {
 *      number: ,
 *      isLock: ,
 *  }
 * }
*/
ICPSDK_Dispatch_Conf.prototype.lockVideoSource = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "lockVideoSource: param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "lockVideoSource: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "lockVideoSource: The user does not logined";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "lockVideoSource",
        "param": param
    };

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function (data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * lockConf 锁定会议
 * @param {
 *  confId: ,
 *  isLock: ,
 *  }
 * }
*/
ICPSDK_Dispatch_Conf.prototype.lockConf = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "lockConf: param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "lockConf: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "lockConf: The user does not logined";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "lockConf",
        "param": param
    };

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function (data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryConferenceById 根据外部会议ID查询内部会议ID
 * @param {
 *  confCode: ,
 *  requireConfCtrl: ,
 *  }
 * }
*/
ICPSDK_Dispatch_Conf.prototype.queryConferenceById = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "queryConferenceById: param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "queryConferenceById: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryConferenceById: The user does not logined";
        param["callback"](result);
        return;
    }

    var requestParam = {
        "opt": "queryConferenceById",
        "param": param
    };

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function (data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * modConf 修改会议
 * @param {
 *  confID: ,
 *  time: 预约会议时间,
 *  timeZone: 时区ID,
 *  timeZoneName: 时区名
 *  }
 * @returns
 */
 ICPSDK_Dispatch_Conf.prototype.modConf = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
    if (param["value"] != undefined) {
        param = param.value;
    }
    if (undefined == param) {
        cloudICP.util.log(tabPageId, {
            "logLevel": "error",
            "logMsg": "modConf: param is invalid"
        });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, {
            "logLevel": "error",
            "logMsg": "modConf: callback is not a function"
        });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "modConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confInfo"]["confID"])) {
        result["desc"] = "modConf: confId is invalid";
        param["callback"](result);
        return;
    }

    if (undefined == param["confInfo"]["time"] || undefined == param["confInfo"]["timeZone"]
        || undefined == param["confInfo"]["timeZoneName"]) {
        result["desc"] = "modConf: time/timeZone/timeZoneName is invalid";
        param["callback"](result);
        return;
    }

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    var tmpMemberInfos = param["memberInfos"].slice();
    var requestParam = {
        "opt": "modify",
        "param": {
            "confInfo": {
                "isVideo": param["confInfo"]["isVideo"],
                "isRecorded": "false",
                "confrate": param["confInfo"]["confRate"],
                "confID" : param["confInfo"]["confID"]
            },
            "memberInfos": tmpMemberInfos
        }
    };
    if (undefined != param["confInfo"]["time"]) {
        requestParam["param"]["confInfo"]["time"] = param["confInfo"]["time"];
    }
    if (undefined != param["confInfo"]["timeZone"]) {
        requestParam["param"]["confInfo"]["timeZone"] = param["confInfo"]["timeZone"];
    }
    if (undefined != param["confInfo"]["timeZoneName"]) {
        requestParam["param"]["confInfo"]["timeZoneName"] = param["confInfo"]["timeZoneName"];
    }
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    };
    cloudICP.util.ajax(ajaxCfg);
};

/**
* delConf
* @param {
*  confId :
*  callback :
* }
*/
ICPSDK_Dispatch_Conf.prototype.delConf = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
    if (param["value"] != undefined) {
        param = param.value;
    }
    if (undefined == param) {
        cloudICP.util.log(tabPageId, {
            "logLevel": "error",
            "logMsg": "delConf: param is invalid"
        });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, {
            "logLevel": "error",
            "logMsg": "delConf: callback is not a function"
        });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "delConf: The user does not logined";
        param["callback"](result);
        return;
    }
    //check param valid
    result["rsp"] = "-2";

    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "delConf: confId is invalid";
        param["callback"](result);
        return;
    }

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    var requestParam = {
        "opt": "delete",
        "param": param
    };
    requestParam["param"]["reqId"] = tabPageId;
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function(data) {
            if (data["rsp"] == "0") {
                var confInfo = cloudICP.util.callStatusMgr.confMgr[param["confId"]];  // TODO
                if (confInfo) {
                    confInfo["isEnding"] = true;
                }
            }
            param["callback"](data);
        }
    }
    cloudICP.util.ajax(ajaxCfg);
};

/**
 * query cas conferences information 查询级联会议信息
 * @param {*} param
 * @returns
 */
 ICPSDK_Dispatch_Conf.prototype.queryCasConfInfos = function(param) {
    if (param["value"] != undefined) {
        param = param.value;
    }
    if (undefined == param) {
        cloudICP.util.log({ "logLevel": "error", "queryCasConfInfos": "param is invalid" });
        return;
    }
    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log({ "logLevel": "error", "queryCasConfInfos": "callback is not a function" });
        return;
    }
    var result = {
        "rsp": "-3",
        "desc": ""
    };
    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryCasConfInfos: User not logged.";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";
    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    var requestParam = {
        "opt": "queryCasConfInfos",
        "param":{
            "confId": param["confId"]
        }
    };

    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "async": true,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * Querying the link for joining a conference 查询会议的入会链接
 * @param {*} param
 * @returns
 */
ICPSDK_Dispatch_Conf.prototype.queryConfURL = function(param) {
    if (param["value"] != undefined) {
        param = param.value;
    }
    if (undefined == param) {
        cloudICP.util.log({ "logLevel": "error", "queryConfURL": "param is invalid" });
        return;
    }
    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log({ "logLevel": "error", "queryConfURL": "callback is not a function" });
        return;
    }
    var result = {
        "rsp": "-3",
        "desc": ""
    };
    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryConfURL: User not logged.";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";
    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "joinConf: confId is invalid";
        param["callback"](result);
        return;
    }

    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    var requestParam = {
        "opt": "queryConfURL",
        "param":{
            "confId": param["confId"]
        }
    };

    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "async": true,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryOrgTree 查询组织树
 * @param {*} param
 * @returns
 */
 ICPSDK_Dispatch_Conf.prototype.queryOrgTree = function(param) {
    if (param["value"] != undefined) {
        param = param.value;
    }
    if (undefined == param) {
        cloudICP.util.log({ "logLevel": "error", "queryOrgTree": "param is invalid" });
        return;
    }
    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log({ "logLevel": "error", "queryOrgTree": "callback is not a function" });
        return;
    }
    var result = {
        "rsp": "-3",
        "desc": ""
    };
    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryOrgTree: User not logged.";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";
    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    var requestParam = {
        "opt": "queryOrgTree",
        "param":{
            "placeholder": "no param needed"
        }
    };

    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "async": true,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryAreaOrgInfo 查询SMC信息
 * @param {*} param
 * @returns
 */
 ICPSDK_Dispatch_Conf.prototype.queryAreaOrgInfo = function(param) {
    if (param["value"] != undefined) {
        param = param.value;
    }
    if (undefined == param) {
        cloudICP.util.log({ "logLevel": "error", "queryAreaOrgInfo": "param is invalid" });
        return;
    }
    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log({ "logLevel": "error", "queryAreaOrgInfo": "callback is not a function" });
        return;
    }
    var result = {
        "rsp": "-3",
        "desc": ""
    };
    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryAreaOrgInfo: User not logged.";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";
    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    var requestParam = {
        "opt": "queryAreaOrgInfo",
        "param": param
    };

    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "async": true,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};


function ICPSDK_Dispatch_Device() {
    this.isDTMFDone = true;
};

/*
 *  设置会议字幕、横幅或短消息
 * {
 *      "opt": "setTextTips",
        "param": {
            "confId",
            "content",
            "type",
            "opType",
            "disPosition",
            "displayType"
        }
 * }
*/
ICPSDK_Dispatch_Conf.prototype.setTextTips = function(param) {
    cloudICP.util.LogInputParam(param);  // 记录输入参数

    var tabPageId = param.tabPageId;

    // 处理传入的参数，若 `param["value"]` 存在则赋值给 `param`
    if (param["value"] != undefined) {
        param = param.value;
    }

    if (undefined == param) {
        cloudICP.util.log(tabPageId, {
            "logLevel": "error",
            "logMsg": "setTextTips: param is invalid"
        });
        return;
    }

    // 验证 callback 是否为函数
    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, {
            "logLevel": "error",
            "logMsg": "setTextTips: callback is not a function"
        });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    // 检查用户是否已登录
    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "setTextTips: The user is not logged in";
        param["callback"](result);
        return;
    }

    // 验证 confId 是否有效
    result["rsp"] = "-2";
    if (!cloudICP.util.checkConfId(param["confId"])) {
        result["desc"] = "setTextTips: confId is invalid";
        param["callback"](result);
        return;
    }

    // 验证 content 字段的长度是否合规
    if (!param["content"] || param["content"].length > 2048) {
        result["desc"] = "setTextTips: content is invalid or too long";
        param["callback"](result);
        return;
    }

    // 构建请求参数
    var requestParam = {
        "opt": "setTextTips",
        "param": {
            "confId": param["confId"],
            "content": param["content"],  // 设置的字幕或横幅内容
            "type": param["type"] || "0",  // 类型：字幕（0），横幅（1），短消息（2），默认0
            "opType": param["opType"] || "SET",   // 操作类型，枚举 SAVE,SET,CANCEL
            "disPosition": param["disPosition"] || "1",  // 位置：顶部（1），中部（2），底部（3），默认1
            "displayType": param["displayType"] || "1"  // 显示方式，默认从下向上
        }
    };

    // 若有其他参数则附加
    if (undefined != param["casConfParam"]) {
        requestParam["param"]["casConfParam"] = param["casConfParam"];
    }

    // 构建请求的 URL
    var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
    requestParam["param"]["reqId"] = tabPageId;  // 添加请求ID

    // 配置 AJAX 请求
    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    };

    // 发送请求
    cloudICP.util.ajax(ajaxCfg);
};
function ICPSDK_Dispatch_Device() {
	this.isDTMFDone = true;
	this.recordsArr = [];
};


/**
 * muteSpeaker
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.muteSpeaker = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "muteSpeaker: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "muteSpeaker: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "muteSpeaker: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (param["cid"] != "-1") {
		if (!cloudICP.util.checkCid(param["cid"])) {
			result["desc"] = "muteSpeaker: The cid is invalid";
			param["callback"](result);
			return;
		};
	}

	var meidaSDKparam =
		"{\"description\":\"msp_quite_audio_channel\", \"cmd\": 65543, \"param\":{\"resID\": {0}, \"quiet\":true}, \"reqId\":\"{1}\"}"
		.format(
			parseInt(param["cid"]), tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});

}


/**
 * unmuteSpeaker
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.unmuteSpeaker = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unmuteSpeaker: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unmuteSpeaker: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "unmuteSpeaker: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (param["cid"] != "-1") {
		if (!cloudICP.util.checkCid(param["cid"])) {
			result["desc"] = "unmuteSpeaker: The cid is invalid";
			param["callback"](result);
			return;
		};
	}

	var meidaSDKparam =
		"{\"description\":\"msp_quite_audio_channel\", \"cmd\": 65543, \"param\":{\"resID\": {0}, \"quiet\":false}, \"reqId\":\"{1}\"}"
		.format(
			parseInt(param["cid"]), tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});

}

/**
 * muteGroup
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
 ICPSDK_Dispatch_Device.prototype.muteGroup = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "muteGroup: param is invalid" });
        return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "muteGroup: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "muteGroup: The user does not logined";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";
    if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group.MIN_DYNAMICGROUP_ID &&
        parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
        result["rsp"] = "-2";
        result["desc"] = "muteGroup: grpid is invalid.";
        param["callback"](result);
        return;
    }

    if(cloudICP.util.includes(cloudICP.grpList, param["grpid"])){
        result["rsp"] = "-4";
        result["desc"] = "muteGroup: This group has been muted.";
        param["callback"](result);
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "muteGroup: This group has been muted" });
        return;
    }

    if(undefined != cloudICP.grpCidList[param["grpid"]]){
        if(cloudICP.grpCidList[param["grpid"]] != "-1"){
            var meidaSDKparam = "{\"description\":\"msp_quite_audio_channel\", \"cmd\": 65543, \"param\":{\"resID\": {0}, \"peerID\": {0}, \"quiet\":true}}".format(
                parseInt(cloudICP.grpCidList[param["grpid"]]), parseInt(param["grpid"])
            );

            cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
        }
    }

    cloudICP.grpList.push(param["grpid"]);

    param["callback"]({"rsp": "0", "desc": ""});

}


/**
 * unmuteGroup
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.unmuteGroup = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "unmuteGroup: param is invalid" });
        return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "unmuteGroup: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "unmuteGroup: The user does not logined";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";
    if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group.MIN_DYNAMICGROUP_ID &&
        parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
        result["rsp"] = "-2";
        result["desc"] = "unmuteGroup: grpid is invalid.";
        param["callback"](result);
        return;
    }

    if(!cloudICP.util.includes(cloudICP.grpList, param["grpid"])){
        result["rsp"] = "-4";
        result["desc"] = "unmuteGroup: The Group does not muted.";
        param["callback"](result);
        cloudICP.util.log(tabPageId, { "logLevel": "info", "logMsg": "unmuteGroup: The Group does not muted" });
        return;
    }

    if(undefined != cloudICP.grpCidList[param["grpid"]]){
        if(cloudICP.grpCidList[param["grpid"]] != "-1"){
            var meidaSDKparam = "{\"description\":\"msp_quite_audio_channel\", \"cmd\": 65543, \"param\":{\"resID\": {0}, \"peerID\": {0}, \"quiet\":false}}".format(
                parseInt(cloudICP.grpCidList[param["grpid"]]), parseInt(param["grpid"])
            );

            cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
        }
    }

    for (var index = 0; index < cloudICP.grpList.length; index++) {

        if (cloudICP.grpList[index] == param["grpid"]) {
            cloudICP.grpList.splice(index);
            index--;
        }
    }

    param["callback"]({"rsp": "0", "desc": ""});

}


/**
 * setGroupVolume
 * @param {Object} param {
 *  cid: ,
 *  volume: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.setGroupVolume = function (param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "setGroupVolume: param is invalid" });
        return;
    }

	if (param["value"] != undefined) {
		param = param.value;
	}

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "setGroupVolume: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "setGroupVolume: The user does not logined";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";
    if (!param["grpid"]) {
        result["desc"] = "setGroupVolume: The grpid is invalid";
        param["callback"](result);
        return;
    }

    if (param["grpid"] != "-1") {
        if (!cloudICP.util.checkGroupID(param["grpid"])) {
            result["desc"] = "setGroupVolume: The grpid is invalid";
            param["callback"](result);
            return;
        };
    }

    if (!param["volume"]) {
        result["desc"] = "setGroupVolume: The volume is invalid";
        param["callback"](result);
        return;
    }

    if (!cloudICP.util.checkRate(param["volume"])) {
        result["desc"] = "setGroupVolume: The volume is not number";
        param["callback"](result);
        return;
    }

    if (parseInt(param["volume"]).toString() != param['volume']) {
        result["desc"] = "setGroupVolume: The volume is not Integer";
        param["callback"](result);
        return;
    }


    var meidaSDKparam = "{\"description\":\"msp_set_audio_Volume\", \"cmd\": 65545, \"param\":{\"resID\": -1, \"peerID\": {0}, \"newVolume\": {1} }}".format(
        parseInt(param["grpid"]), parseFloat(param["volume"])
    );

    cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

    param["callback"]({ "rsp": "0", "desc": "" });
};


/**
 * muteMic
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.muteMic = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "muteMic: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "muteMic: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "muteMic: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (param["cid"] != "-1") {
		if (!cloudICP.util.checkCid(param["cid"])) {
			result["desc"] = "muteMic: The cid is invalid";
			param["callback"](result);
			return;
		};

	}

	var meidaSDKparam =
		"{\"description\":\"msp_mute_audio_channel\", \"cmd\": 65542, \"param\":{\"resID\": {0}, \"mute\":true}, \"reqId\":\"{1}\"}"
		.format(
			parseInt(param["cid"]), tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});

}

/**
 * unmuteMic
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.unmuteMic = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unmuteMic: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unmuteMic: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "unmuteMic: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (param["cid"] != "-1") {
		if (!cloudICP.util.checkCid(param["cid"])) {
			result["desc"] = "unmuteMic: The cid is invalid";
			param["callback"](result);
			return;
		};
	}

	var meidaSDKparam =
		"{\"description\":\"msp_mute_audio_channel\", \"cmd\": 65542, \"param\":{\"resID\": {0}, \"mute\":false}, \"reqId\":\"{1}\"}"
		.format(
			parseInt(param["cid"]), tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});

}

/**
 * 二次拨号
 */
ICPSDK_Dispatch_Device.prototype.sendDTMF = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "sendDTMF: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "sendDTMF: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "sendDTMF: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "sendDTMF: The cid is invalid";
		param["callback"](result);
		return;
	};

	if (!param["function"]) {
		result["desc"] = "sendDTMF: The function is not exist";
		param["callback"](result);
		return;
	}

	if (!cloudICP.dispatch.device.isDTMFDone) {
		result["rsp"] = "-4";
		result["desc"] = "sendDTMF: last operation is not finished";
		param["callback"](result);
		return;
	} else {
		cloudICP.dispatch.device.isDTMFDone = false;
	}

	var meidaSDKparam =
		"{\"description\":\"msp_dial_again\", \"cmd\": 65548, \"param\":{\"resID\": {0}, \"functionCode\":\"{1}\"}, \"reqId\":\"{2}\"}"
		.format(
			parseInt(param["cid"]), param["function"], tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});

}


/**
 * getSoundDevice
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.getSoundDevice = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getSoundDevice: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getSoundDevice: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "getSoundDevice: The user does not logined";
		param["callback"](result);
		return;
	}

	var meidaSDKparam = "{\"description\":\"msp_get_audio_dev\", \"cmd\": 131077, \"reqId\": \"{0}\"}"
	.format(
		tabPageId
	);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};

/**
 * getVolume
 * @param {Object} param {
 *  cid: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.getVolume = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getVolume: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getVolume: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "getVolume: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (param["cid"] != "-1") {
		if (!cloudICP.util.checkCid(param["cid"])) {
			result["desc"] = "getVolume: The cid is invalid";
			param["callback"](result);
			return;
		};
	}

	var meidaSDKparam = "{\"description\":\"msp_get_audio_volume\", \"cmd\": 65544, \"param\":{\"resID\": {0} }, \"reqId\":\"{1}\"}"
		.format(
			parseInt(param["cid"]), tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};

/**
 * setVolume
 * @param {Object} param {
 *  cid: ,
 *  volume: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.setVolume = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setVolume: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setVolume: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "setVolume: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (param["cid"] != "-1") {
		if (!cloudICP.util.checkCid(param["cid"])) {
			result["desc"] = "setVolume: The cid is invalid";
			param["callback"](result);
			return;
		};
	}

	if (!param["volume"]) {
		result["desc"] = "setVolume: The volume is invalid";
		param["callback"](result);
		return;
	}

	if (!cloudICP.util.checkRate(param["volume"])) {
		result["desc"] = "setVolume: The volume is not number";
		param["callback"](result);
		return;
	}

	if (parseInt(param["volume"]).toString() != param['volume']) {
		result["desc"] = "setVolume: The volume is not Integer";
		param["callback"](result);
		return;
	}


	var meidaSDKparam =
		"{\"description\":\"msp_set_audio_Volume\", \"cmd\": 65545, \"param\":{\"resID\": {0}, \"newVolume\": {1} }, \"reqId\":\"{2}\"}"
		.format(
			parseInt(param["cid"]), parseFloat(param["volume"]), tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};

/**
 * setRingVolume
 * @param {Object} param {
 *  cid: ,
 *  volume: ,
 *  callback: ,
 * }
 */
 ICPSDK_Dispatch_Device.prototype.setRingVolume = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "setRingVolume: param is invalid" });
        return;
    }

	if (param["value"] != undefined) {
		param = param.value;
	}

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "logMsg": "setRingVolume: callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "setRingVolume: The user does not logined";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";

    if (!param["volume"]) {
        result["desc"] = "setRingVolume: The volume is invalid";
        param["callback"](result);
        return;
    }

    if(!cloudICP.util.checkRate(param["volume"])){
        result["desc"] = "setRingVolume: The volume is not number";
        param["callback"](result);
        return;
    }

    if(parseInt(param["volume"]).toString() != param['volume']){
        result["desc"] = "setRingVolume: The volume is not Integer";
        param["callback"](result);
        return;
    }

    var meidaSDKparam = "{\"description\":\"msp_set_ring_Volume\", \"cmd\": 131082, \"param\":{\"newVolume\": {0} }}".format(
        parseFloat(param["volume"])
    );

    cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

    param["callback"]({"rsp": "0", "desc": ""});
};

/**
 * assignSoundDevice
 * @param {Object} param {
 *  cid: ,
 *  number:,
 *  outputDev: ,
 *  inputDev: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.assignSoundDevice = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "assignSoundDevice: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "assignSoundDevice: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "assignSoundDevice: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (param["cid"] != "-1") {
		if (!cloudICP.util.checkCid(param["cid"])) {
			result["desc"] = "assignSoundDevice: The cid is invalid";
			param["callback"](result);
			return;
		};

		// var call =  cloudICP.util.callStatusMgr.allCalls[param["cid"]];
		// if (!call || call.callState != cloudICP.util.callStatusMgr.CONNECTING) {
		//     result["desc"] = "assignSoundDevice: The cid is invalid";
		//     param["callback"](result);
		//     return;
		// }
	}

	if (param["number"] != "conference") {
		if (!cloudICP.util.checkIsdn(param["number"])) {
			result["desc"] = "assignSoundDevice: The number is invalid";
			param["callback"](result);
			return;
		}
	}

	if (!param["outputDev"] && !param["inputDev"]) {
		result["desc"] = "assignSoundDevice: The inputDev or outputDev is invalid";
		param["callback"](result);
		return;
	} else if (!param["outputDev"]) {
		param["outputDev"] = "";
	} else if (!param["inputDev"]) {
		param["inputDev"] = "";
	}

	var meidaSDKparam =
		"{\"description\":\"msp_assign_sound_device\", \"cmd\": 65546, \"param\":{\"resID\": {0}, \"number\": \"{1}\", \"mediaInfo\":{\"outputDev\":\"{2}\", \"inputDev\": \"{3}\" }}, \"reqId\":\"{4}\"}"
		.format(
			parseInt(param["cid"]), param["number"], param["outputDev"], param["inputDev"], tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};

/**
 * querySoundDevice
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.querySoundDevice = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "querySoundDevice: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "querySoundDevice: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "querySoundDevice: The user does not logined";
		param["callback"](result);
		return;
	}

	var meidaSDKparam = "{\"description\":\"msp_query_sound_device\", \"cmd\": 65547 , \"reqId\": \"{0}\"}"
	.format(
		tabPageId
	);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};

/**
 * queryCameraAbility
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.queryCameraAbility = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryCameraAbility: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryCameraAbility: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryCameraAbility: The user does not logined";
		param["callback"](result);
		return;
	}

	if (undefined == cloudICP.config["cameraInfo"]) {
		result["rsp"] = "-4";
		result["desc"] = "queryCameraAbility: The camera info is lost.";
		param["callback"](result);
		return;
	}

	param["callback"]({
		"rsp": "0",
		"desc": "",
		"camera_ability": cloudICP.config["cameraInfo"]
	});
};

/**
 * stopPlayTone
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.stopPlayTone = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "stopPlayTone: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "stopPlayTone: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "stopPlayTone: The user does not logined";
		param["callback"](result);
		return;
	}

	// 停止振铃
	var reqParam = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\":\"{1}\"}".format(
		parseInt("0"), tabPageId
	);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(reqParam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};

/**
 * getVersion
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.getVersion = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getVersion: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getVersion: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "getVersion: The user does not logined";
		param["callback"](result);
		return;
	}

	param["callback"]({
		"rsp": "0",
		"desc": "",
		"versions": {
			"mspVersion": cloudICP.userInfo["mspVersion"],
			"sdkVersion": cloudICP.userInfo["sdkVersion"],
			"serverVersion": cloudICP.userInfo["serverVersion"]
		}
	});
};

/**
 * getMspVersion
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.getMspVersion = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getMspVersion: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getMspVersion: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	param["callback"]({
		"rsp": "0",
		"desc": "",
		"versions": {
			"mspVersion": cloudICP.userInfo["mspVersion"]
		}
	});
};

/**
 * initMRS
 * @param {Object} param {
 *  mrsIp: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.initMRS = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "initMRS: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "initMRS: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "initMRS: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";

	if (!cloudICP.util.checkIp(param["mrsIp"])) {
		result["desc"] = "initMRS: The mrsIp is invalid";
		param["callback"](result);
		return;
	}

	// 初始化录音录像查询
	var ajaxReq = {
		"type": "PUT",
		"url": cloudICP.getSdkServerUrl() + "/v1/register/" + cloudICP.userInfo["isdn"] + "/initmrs",
		"async": false,
		"data": {
			"mrsServerIP": param["mrsIp"],
		},
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxReq);
};

/**
 * forceInit
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.forceInitMSP = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "forceInitMSP: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "forceInitMSP: callback is not a function"
		});
		return;
	}

	cloudICP.dispatch.webSocket._medisServerWs.send('ForceLogin');

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};


/**
 * hideVideoWindow
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.hideVideoWindow = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "hideVideoWindow: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "hideVideoWindow: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (param["cid"] != "-1") {
		if (!cloudICP.util.checkCid(param["cid"])) {
			result["desc"] = "hideVideoWindow: The cid is invalid";
			param["callback"](result);
			return;
		};
	}


	var meidaSDKparam = "{\"description\":\"hide_video_window\", \"cmd\": 196616, \"param\":{\"cid\": {0}}, \"reqId\":\"{1}\"}".format(
		parseInt(param["cid"]), tabPageId
	);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};


/**
 * windowOnTop
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.windowOnTop = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "windowOnTop: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "windowOnTop: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (param["cid"] != "-1") {
		if (!cloudICP.util.checkCid(param["cid"])) {
			result["desc"] = "windowOnTop: The cid is invalid";
			param["callback"](result);
			return;
		};
	}


	var meidaSDKparam =
		"{\"description\":\"msp_window_top\", \"cmd\": 196614, \"param\":{\"cid\": {0}, \"isOnTop\":  {1}}, \"reqId\":\"{1}\"}".format(
			parseInt(param["cid"]), param["isOnTop"], tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};


/**
 * windowDragEnable
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.windowDragEnable = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "windowDragEnable: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "windowDragEnable: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (param["cid"] != "-1") {
		if (!cloudICP.util.checkCid(param["cid"])) {
			result["desc"] = "windowDragEnable: The cid is invalid";
			param["callback"](result);
			return;
		};
	}

	var meidaSDKparam =
		"{\"description\":\"msp_window_set_enable_drag\", \"cmd\": 196615, \"param\":{\"cid\": {0}, \"isDrag\":  {1}}, \"reqId\":\"{2}\"}"
		.format(
			parseInt(param["cid"]), param["isDrag"], tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};


/**
 * showWindowBorder
 * @param {Object} param {
 *  resID: ,
 *  border: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.showWindowBorder = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getResolution: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getResolution: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "initMRS: The user does not logined";
		param["callback"](result);
		return;
	}

	if (!param["resID"] && !param["cid"]) {
		result["desc"] = "monitorVideo: The resID is invalid";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";

	var meidaSDKparam = {
		"description": "show_window_border",
		"cmd": 196617,
		"param": param
	}

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};


/**
 * setWindowInfo
 * @param {Object} param {
 *  cid:,
 *  windowInfo: {
 *      width:
 *      height:
 *      posX:
 *      posY:
 *  },
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.setWindowInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setWindowInfo: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setWindowInfo: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "initMRS: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";

	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "setWindowInfo: The cid is invalid";
		param["callback"](result);
		return;
	}

	var call = cloudICP.util.callStatusMgr.allCalls[param["cid"]];
	if (call && call.callState != cloudICP.util.callStatusMgr.CONNECTING) {
		result["desc"] = "setWindowInfo: The cid is invalid";
		param["callback"](result);
		return;
	}

	var videoInfo = cloudICP.util.callStatusMgr.videoMediaInfoBuf[param["cid"]];

	if (videoInfo == undefined) {
		result["desc"] = "setWindowInfo: The cid is invalid";
		param["callback"](result);
		return;
	}

	if (param["windowInfo"] != undefined) {
		if (!cloudICP.util.isNumber(param["windowInfo"]["width"]) || param["windowInfo"]["width"] > 2147483647) {
			result["desc"] = "setWindowInfo: The width is invalid";
			param["callback"](result);
			return;
		}

		if (!cloudICP.util.isNumber(param["windowInfo"]["height"]) || param["windowInfo"]["width"] > 2147483647) {
			result["desc"] = "setWindowInfo: The height is invalid";
			param["callback"](result);
			return;
		}

		if (!cloudICP.util.isNumber(param["windowInfo"]["posX"]) || param["windowInfo"]["width"] > 2147483647) {
			result["desc"] = "setWindowInfo: The posX is invalid";
			param["callback"](result);
			return;
		}

		if (!cloudICP.util.isNumber(param["windowInfo"]["posY"]) || param["windowInfo"]["width"] > 2147483647) {
			result["desc"] = "setWindowInfo: The posY is invalid";
			param["callback"](result);
			return;
		}

		var callStatusMgr = cloudICP.util.callStatusMgr;
		if (undefined != callStatusMgr.windowInfos[param["cid"]]) {
			callStatusMgr.windowInfos[param["cid"]]["width"] = param["windowInfo"]["width"];
			callStatusMgr.windowInfos[param["cid"]]["height"] = param["windowInfo"]["height"];
			callStatusMgr.windowInfos[param["cid"]]["posX"] = param["windowInfo"]["posX"];
			callStatusMgr.windowInfos[param["cid"]]["posY"] = param["windowInfo"]["posY"];
		}
		var windowInfo = param["windowInfo"];
	}

	var meidaSDKparam =
		"{\"description\":\"modify_window\", \"cmd\": 196611, \"param\":{\"cid\": {0}, \"width\": {1}, \"height\": {2}, \"x\": {3}, \"y\": {4}}, \"reqId\":\"{5}\"}"
		.format(
			parseInt(param["cid"]), parseInt(windowInfo["width"]), parseInt(windowInfo["height"]), parseInt(
				windowInfo["posX"]), parseInt(windowInfo["posY"]), tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};

/**
 * getResolution
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.getResolution = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getResolution: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "getResolution: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "initMRS: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";

	var meidaSDKparam = "{\"description\":\"get_screen_resolution\", \"cmd\": 196612, \"param\":{}, \"reqId\": \"{0}\"}"
	.format(
		tabPageId
	);

	cloudICP.dispatch.webSocket.sendDataToDeamonSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};

/**
 * 截图
 */
ICPSDK_Dispatch_Device.prototype.snapshot = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "snapshot: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "snapshot: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "snapshot: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "snapshot: The cid is invalid";
		param["callback"](result);
		return;
	};

	if (!param["SnapshotPath"]) {
		result["desc"] = "snapshot: The SnapshotPath is not exist";
		param["callback"](result);
		return;
	}

	if (!param["SnapshotFormat"]) {
		param["SnapshotFormat"] = "bmp"
	}

	// 截图
	var meidaSDKparam =
		"{\"description\":\"msp_snapshot\", \"cmd\": 65552, \"param\":{\"cid\": {0}, \"SnapshotPath\": \"{1}\", \"SnapshotFormat\":\"{2}\"}, \"reqId\":\"{3}\"}"
		.format(
			parseInt(param["cid"]), param["SnapshotPath"], param["SnapshotFormat"], tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});

}

/**
 * 设置本地录音录像存储路径
 */
 ICPSDK_Dispatch_Device.prototype.setLocalRecordParam = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId;
	if (undefined == param.tabPageId){
		tabPageId = 0;
	} else {
		tabPageId = param.tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setLocalRecordParam: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setLocalRecordParam: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "setLocalRecordParam: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	var localRecordPath = "";
	var localRecordMinDiskCap;
	if (undefined != param["localRecordPath"]) {
		localRecordPath = param["localRecordPath"];
	} else {
		localRecordPath = cloudICP.config["localRecordPath"];
	}

	if (undefined != param["localRecordMinDiskCap"]) {
		localRecordMinDiskCap = param["localRecordMinDiskCap"];
	} else {
		localRecordMinDiskCap = cloudICP.config["localRecordMinDiskCap"];
	}
	localRecordPath = localRecordPath.replaceAll('\\','/');
	// 设置本地录音录像存储路径
	var meidaSDKparam =
		"{\"description\":\"set_LocalRecordParam\", \"cmd\": 65558, \"param\":{\"localRecordPath\":\"{0}\",\"localRecordMinDiskCap\":{1}, \"userId\":\"{2}\"},  \"reqId\":\"{3}\"}"
		.format( localRecordPath, localRecordMinDiskCap, cloudICP.userInfo["isdn"], tabPageId);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	function handleSetLocalRecordCallbackMsg(data) {
		var backMsg = {};
		backMsg["rsp"] = data["result"];
		backMsg["desc"] = "";
		if (5 == data["result"]) {
			backMsg["desc"] = "setRecordMinDiskCap failed: Actual free disk space is insufficient.";
		}
		else if(0 == data["result"]) {
			backMsg["desc"] = "";
		}
		param["callback"](backMsg);
	}
	cloudICP.dispatch.webSocket._mediaSdkRspFuncs["65558"] = handleSetLocalRecordCallbackMsg;

}

/**
 * 开始录音录像
 */
ICPSDK_Dispatch_Device.prototype.startLocalRecord = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId;
	if (undefined == param.tabPageId){
		tabPageId = 0;
	} else {
		tabPageId = param.tabPageId;
	}

	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "startLocalRecord: param is invalid"
		});
		return;
	}
	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "startLocalRecord: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "startLocalRecord: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "startLocalRecord: The cid is invalid";
		param["callback"](result);
		return;
	};

	if (-1 < cloudICP.dispatch.device.recordsArr.indexOf(param["cid"])) {
		result["rsp"] = "3";
		result["desc"] = "startLocalRecord: The current recording task is in progress.";
		param["callback"](result);
		return;
	}

	if (!param["Type"]) {
		result["desc"] = "startLocalRecord: The Type is not exist";
		param["callback"](result);
		return;
	}

	// 开始录像
	var meidaSDKparam =
		"{\"description\":\"start_LocalRecord\", \"cmd\": 65553, \"param\":{\"cid\": {0}, \"Type\": {1}, \"autoDelete\": {2}, \"userId\":\"{3}\"}, \"reqId\":\"{3}\"}"
		.format(
			parseInt(param["cid"]), param["Type"], param["autoDelete"], cloudICP.userInfo["isdn"],tabPageId
		);

	function handleStartLocalRecordCallbackMsg(data) {
		var backMsg = {};
		var iNum = cloudICP.dispatch.device.recordsArr.indexOf(param["cid"]);
		backMsg["rsp"] = data["result"];
		backMsg["desc"] = "";
		if (3 == data["result"]) {
			backMsg["desc"] = "The business conversation does not exist.";
			cloudICP.dispatch.device.recordsArr.splice(iNum,1);
		}else if(8 == data["result"]){
			backMsg["desc"] = "Insufficient partition space of the local hard disk.";
			cloudICP.dispatch.device.recordsArr.splice(iNum,1);
		}
		param["callback"](backMsg);
	}
	cloudICP.dispatch.webSocket._mediaSdkRspFuncs["65553"] = handleStartLocalRecordCallbackMsg;
	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
	cloudICP.dispatch.device.recordsArr.push(param["cid"]);
}

/**
 * 关闭录音录像
 */
ICPSDK_Dispatch_Device.prototype.stopLocalRecord = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId;
	if (undefined == param.tabPageId){
		tabPageId = 0;
	} else {
		tabPageId = param.tabPageId;
	}

	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "stopLocalRecord: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "stopLocalRecord: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "stopLocalRecord: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "stopLocalRecord: The cid is invalid";
		param["callback"](result);
		return;
	};

	var iNum = cloudICP.dispatch.device.recordsArr.indexOf(param["cid"]);
	if (-1 == iNum) {
		result["rsp"] = "3";
		result["desc"] = "stopLocalRecord: Not recording task of this cid in progress.";
		param["callback"](result);
		return;
	}

	var meidaSDKparam =
		"{\"description\":\"stop_LocalRecord\", \"cmd\": 65554, \"param\":{\"cid\": {0}}, \"reqId\":\"{1}\"}".format(
			parseInt(param["cid"]), tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
	cloudICP.dispatch.device.recordsArr.splice(iNum,1);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});

}


/**
 * playAudioShortMsg
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.playAudioShortMsg = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "playAudioShortMsg: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "playAudioShortMsg: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	result["rsp"] = "-2";
	if (!param["audioMsg"]) {
		result["desc"] = "The audioMsg is invalid";
		param["callback"](result);
		return;
	}

	var meidaSDKparam =
		"{\"description\":\"msp_play_audio_shortMsg\", \"cmd\": 131081, \"param\":{\"audioMsg\": \"{0}\"}, \"reqId\":\"{1}\"}".format(
			param["audioMsg"], tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};


/**
 * createWindow 创建用户视频窗口
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.createWindow = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param || undefined == param["windowInfo"]) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "createWindow: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "createWindow: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] += "The user does not logined";
		param["callback"](result);
		return;
	}

	if (undefined == param["windowInfo"]["width"] || !cloudICP.util.isNumber(param["windowInfo"]["width"]) || param[
			"windowInfo"]["width"] > 2147483647) {
		result["desc"] += "createWindow: The width is invalid, use defult value 400; ";
		param["windowInfo"]["width"] = 400;
	}
	if (undefined == param["windowInfo"]["height"] || !cloudICP.util.isNumber(param["windowInfo"]["height"]) ||
		param["windowInfo"]["height"] > 2147483647) {
		result["desc"] += "createWindow: The height is invalid, use defult value 225; ";
		param["windowInfo"]["height"] = 225;
	}
	if (undefined == param["windowInfo"]["posX"] || !cloudICP.util.isNumber(param["windowInfo"]["posX"]) || param[
			"windowInfo"]["posX"] > 2147483647) {
		result["desc"] += "createWindow: The posX is invalid, use defult value 400; ";
		param["windowInfo"]["posX"] = 400;
	}
	if (undefined == param["windowInfo"]["posY"] || !cloudICP.util.isNumber(param["windowInfo"]["posY"]) || param[
			"windowInfo"]["posY"] > 2147483647) {
		result["desc"] += "createWindow: The posY is invalid, use defult value 400; ";
		param["windowInfo"]["posY"] = 400;
	}
	if (undefined == param["windowInfo"]["showToolbar"] || !cloudICP.util.isNumber(param["windowInfo"][
			"showToolbar"
		]) || param["windowInfo"]["showToolbar"] > 255) {
		result["desc"] += "createWindow: The showToolbar is invalid, use defult value 1; ";
		param["windowInfo"]["showToolbar"] = 1;
	}
	if (undefined == param["windowInfo"]["buttonIDs"] || !cloudICP.util.isNumber(param["windowInfo"][
			"buttonIDs"
		]) || param["windowInfo"]["buttonIDs"] > 255) {
		result["desc"] += "createWindow: The buttonIDs is invalid, use defult value 0; ";
		param["windowInfo"]["buttonIDs"] = 0;
	}
	if (undefined == param["windowInfo"]["hide"]) {
		result["desc"] += "createWindow: The hide is invalid, use defult value false; ";
		param["windowInfo"]["hide"] = false;
	}
	if (undefined == param["windowInfo"]["enableDrag"]) {
		result["desc"] += "createWindow: The enableDrag is invalid, use defult value true; ";
		param["windowInfo"]["enableDrag"] = true;
	}

	result["rsp"] = "-2";
	var userWindowId = cloudICP.util.getUserWindowId();
	var userWindowName = "";
	if (!param["windowInfo"]["windowName"]) {
		userWindowName = userWindowId.toString();
	}
	var iResult = cloudICP.util.checkUserWindowId(userWindowId, "create");
	if (0 == iResult) {
		result["desc"] = "createWindow: The userWindowId is invalid";
		param["callback"](result);
		return;
	} else if (2 == iResult) {
		result["desc"] = "createWindow: The userWindowId is used, created user window.";
		param["callback"](result);
		return;
	}
	var videoResizeMode = cloudICP.config["videoResizeMode"];
	if (undefined != param["windowInfo"]["videoResizeMode"]) {
		videoResizeMode = param["windowInfo"]["videoResizeMode"];
	}
	var meidaSDKparam =
		"{\"description\":\"msp_create_video_window\", \"cmd\": 196610, \"param\":{\"userWindowId\":{0}, \"windowName\": \"{1}\", \"mode\": \"{2}\", \"width\": {3}, \"height\": {4}, \"x\": {5}, \"y\":{6}, \"showToolbar\": {7}, \"buttonIDs\": {8}, \"hide\":{9}, \"enableDrag\":{10} }, \"reqId\": \"{11}\", \"videoResizeMode\": {12} }"
		.format(
			userWindowId, userWindowName, "window", param["windowInfo"]["width"], param["windowInfo"]["height"],
			param["windowInfo"]["posX"], param["windowInfo"]["posY"],
			param["windowInfo"]["showToolbar"], param["windowInfo"]["buttonIDs"], param["windowInfo"]["hide"],
			param["windowInfo"]["enableDrag"], tabPageId, videoResizeMode
		)
	result["rsp"] = 0;
	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
	param["callback"](result);
};


/**
 * destroyWindow 释放用户创建的视频窗口
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.destroyWindow = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param && undefined == param["userWindowId"]) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "destroyWindow: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "destroyWindow: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	var iResult = cloudICP.util.checkUserWindowId(param["userWindowId"], "destroy");
	if (0 == iResult) {
		result["desc"] = "destroyWindow: The userWindowId is invalid";
		param["callback"](result);
		return;
	}
	var resID = undefined;
	Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function(key) {
		if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["userWindowId"] &&
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["userWindowId"] == param[
				"userWindowId"]) {
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["isNeedDestroyWindow"] = true;
			resID = key;
		}
	});
	if (resID == undefined) { //用户窗口还没有建立媒体面
		if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[param["userWindowId"].toString()]) {
			if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[param["userWindowId"].toString()][
					"value"
				]["cid"]) {
				var cid = cloudICP.util.callStatusMgr.videoMediaInfoBuf[param["userWindowId"].toString()]["value"][
					"cid"
				];
				if (undefined != cloudICP.util.callStatusMgr.windowInfos[cid.toString()]) {
					delete cloudICP.util.callStatusMgr.windowInfos[cid.toString()];
				}
			}
			if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[param["userWindowId"].toString()][
					"value"
				]["to"]) {
				var to = cloudICP.util.callStatusMgr.videoMediaInfoBuf[param["userWindowId"].toString()]["value"][
					"to"
				];
				if (undefined != cloudICP.util.callStatusMgr.windowInfos[to.toString()]) {
					delete cloudICP.util.callStatusMgr.windowInfos[to.toString()];
				}
			}
		}
		var meidaSDKparam =
			"{\"description\":\"msp_delete_close_userWindow\", \"cmd\": 196613, \"param\":{\"userWindowId\":{0}}, \"reqId\": \"{1}\"}"
			.format(
				parseInt(param["userWindowId"]), tabPageId
			);
		cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
		param["callback"]({
			"rsp": "0",
			"desc": ""
		});
		return;
	};
	//关闭窗口上的业务
	var callStatusMgr = cloudICP.util.callStatusMgr;
	var call = callStatusMgr.allCalls[resID];
	if (call) {
		var param = {
			cid: resID,
			callback: function(data) {
				setTimeout(function() {
					cloudICP.util.log(tabPageId, { "logLevel": "info", "logMsg": "close [" + resID + "] window rsp: " + JSON.stringify(data) });
				}, 0);
			}
		}
		if (call.callType == callStatusMgr.VIDEO || call.callType == callStatusMgr.MONITOR || call.callType ==
			callStatusMgr.VIDEODISPATCH) {
			cloudICP.dispatch.video.release(param);
			return;
		}
		if (call.callType == callStatusMgr.VIDEOCONF) {
			var confInfo = cloudICP.util.callStatusMgr.confMgr[cloudICP.util.callStatusMgr.currentGoingConf];
			if (confInfo != undefined) {
				if (cloudICP.userInfo["isdn"] == confInfo["value"]["confStatus"]["chair"]) {
					param.confId = cloudICP.util.callStatusMgr.currentGoingConf;
					cloudICP.dispatch.conf.endConf(param);
					return;
				}
			}
			cloudICP.dispatch.conf.exitVideoConf(param);
		}
	}
	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
};

/**
 * ringType
 * @param {
 *  ringType: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.ringType = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "ringType: param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "ringType: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "ringType: User not logged.";
		param["callback"](result);
		return;
	};

	var result = {
		"rsp": "-2",
		"desc": ""
	};

	if (!param["ringType"] || typeof(param["ringType"]) != "string" || (param["ringType"] != "0" && param[
			"ringType"] != "1")) {
		result["desc"] = "ringType: The ringType is invalid";
		param["callback"](result);
		return;
	}

	cloudICP.util.ringType = param["ringType"];

	param["callback"]({
		"rsp": "0",
		"desc": ""
	});
}


/**
 * setLinkedPhone() set linked phone isnd
 * @param {Object} param {
 *  "phoneIsdn": ,
 *  "linkMode": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.setLinkedPhone = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"setLinkedPhone": "param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"setLinkedPhone": "callback is not a function"
		});
		return;
	}


	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "setLinkedPhone: User not logged.";
		param["callback"](result);
		return;
	};

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["phoneIsdn"])) {
		result["desc"] = "setLinkedPhone: The phoneIsdn is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"] + "/linkphone";

    var resParam = {
        "opt": "set",
        "param": {
            "phoneISDN": param["phoneIsdn"],
            "linkedMode": param["linkMode"]
        }
    };

	resParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};

/**
 * setMediaMode() set isMediaDataPassthrough
 * @param {Object} param {
 *  "cid": ,
 *  "type": ,
 *  "switch": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Device.prototype.setMediaMode = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "callback is not a function"
		});
		return;
	}
	var result = {
		"rsp": "-2",
		"desc": ""
	};

	if (!cloudICP.util.callStatusMgr.checkIsExist(param)) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setMediaMode :A resource already exists."
		});
		result["desc"] = "A resource already exists.";
		param["callback"](result);
		return;
	}

    var meidaSDKparam =
		"{\"description\":\"setMediaMode\", \"cmd\": 65561, \"param\":{\"cid\":{0}, \"type\":{1}, \"switch\":{2}}, \"reqId\":\"{3}\"}".format(
		    parseInt(param["cid"]), parseInt(param["type"]), parseInt(param["switch"]), tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
    param["callback"]({
		"rsp": "0",
		"desc": ""
	});
 };

/**
 * getAllWindowName() get all visible window name
 * @param {Object} param {
 *      no param needed
 * }
 */
 ICPSDK_Dispatch_Device.prototype.getAllWindowName = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}
    var meidaSDKparam =
		"{\"description\":\"get_all_window_name\", \"cmd\": 196619, \"param\":{}, \"reqId\":\"{0}\"}".format(
		    tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
    param["callback"]({
		"rsp": "0",
		"desc": ""
	});
 };

 /**
 * startWindowShare() will start share the window
 * @param {Object} param {
 *      cid:
 *      windowName:
 * }
 */
  ICPSDK_Dispatch_Device.prototype.startWindowShare = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}
    var meidaSDKparam =
		"{\"description\":\"start_window_share\", \"cmd\": 65559, \"param\":{\"cid\":{0}, \"windowName\":\"{1}\"}, \"reqId\":\"{2}\"}".format(
		    parseInt(param["cid"]), param["windowName"], tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
    param["callback"]({
		"rsp": "0",
		"desc": ""
	});
 };

  /**
 * stopWindowShare() will stop share the window
 * @param {Object} param {
 *      cid:
 * }
 */
   ICPSDK_Dispatch_Device.prototype.stopWindowShare = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}
    var meidaSDKparam =
		"{\"description\":\"stop_window_share\", \"cmd\": 65560, \"param\":{\"cid\":{0}}, \"reqId\":\"{1}\"}".format(
		    parseInt(param["cid"]), tabPageId
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
    param["callback"]({
		"rsp": "0",
		"desc": ""
	});
 };

/**
 * checkMSPIsInstall() check msp is install
 * @param {Object} param {
 *  "mdcip": ,
 *  "callback": ,
 * }
 */
 ICPSDK_Dispatch_Device.prototype.checkMSPIsInstall = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (undefined == param || undefined == param.mdcip || !cloudICP.util.checkIp(param.mdcip)) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"checkMSPIsInstall": "param is invalid"
		});
		return;
	}

	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"checkMSPIsInstall": "callback is not a function"
		});
		return;
	}

	var strUrl = "http://" + param.mdcip + ":8009/mspversion/" + cloudICP.config.mspVersion + "/eSDK_ICP_MSP.zip";
	var strVersion = "MSP_V" + cloudICP.config.mspVersion + ".zip";
	cloudICP.config.hasExeCheckMsp = true;
	cloudICP.config.setMDCIP = param.mdcip;
	try {
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
	} catch(e) {}
};


  /**
 * UpgradeMspVersion will Upgrading the MSP Version
 * @param {Object} param {
 *      downloadUrl:
 * }
 */
ICPSDK_Dispatch_Device.prototype.UpgradeMspVersion = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "UpgradeMspVersion: param is invalid"
		});
		return;
	}

	if (undefined == param["downloadUrl"] || "" == param["downloadUrl"]) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "UpgradeMspVersion: downloadUrl is invalid."
		});
		result["desc"] = "UpgradeMspVersion: downloadUrl is invalid.";
		param["callback"](result);
		return;
	}

	if (undefined == param["downloadDir"] || "" == param["downloadDir"]) {
		param["downloadDir"] = "C:\\\\dc_temp"
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "UpgradeMspVersion: callback is not a function."
		});
		return;
	}
    var meidaSDKparam =
		"{\"description\":\"upgrade_msp_version\", \"cmd\": {0}, \"param\":{\"downloadUrl\":\"{1}\", \"reqId\":\"{2}\", \"downloadDir\":\"{3}\", \"local_ip\":\"{4}\"}}".format(
		    0x2000C, param["downloadUrl"], tabPageId, param["downloadDir"], cloudICP.config["localIP"]
		);

	cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
    param["callback"]({
		"rsp": "0",
		"desc": ""
	});
 };

function ICPSDK_Dispatch_Event() {
	this.EVENT_LIST = {
		"VoiceNotify": {
			"OnUserStatusNotify": this.printEvent,
			"OnDialOutProceeding": this.printEvent,
			"OnDialOutRinging": this.printEvent,
			"OnCallConnect": this.printEvent,
			"OnDialOutFailure": this.printEvent,
			"OnCallRelease": this.printEvent,
			"OnDialInRinging": this.printEvent,
			"OnConnectProceeding": this.printEvent,
			"OnHalfDialConnectProceeding": this.printEvent,
			"OnStartRecvHalfDial": this.printEvent,
			"OnStopRecvHalfDial": this.printEvent,
			"OnTransferReject": this.printEvent,
			"OnTransferAccept": this.printEvent,
			"OnTransferSuccess": this.printEvent,
			"OnTransferFailure": this.printEvent,
			"OnTransferNotify": this.printEvent,
			"OnTalkingEmergencyCallStart": this.printEvent,
			"OnCancelTransferFailure": this.printEvent,
			"OnCancelTransferSuccess": this.printEvent,
			"OnHalfDuplexPTTAccept": this.printEvent,
			"OnHalfDialFailure": this.printEvent,
			"OnBreakOffFailure": this.printEvent,
			"OnBreakOffSuccess": this.printEvent,
			"OnHalfDialSuccess": this.printEvent,
			"OnStartDiscreetlistenSuccess": this.printEvent,
			"OnStartDiscreetlistenFailure": this.printEvent,
			"OnStopDiscreetlistenSuccess": this.printEvent,
			"OnStopDiscreetlistenFailure": this.printEvent,
			"OnDiscreetlistenStart": this.printEvent,
			"OnDiscreetlistenStop": this.printEvent,
			"OnHoldSuccess": this.printEvent,
			"OnHoldFailure": this.printEvent,
			"OnUnholdSuccess": this.printEvent,
			"OnUnholdFailure": this.printEvent,
			"OnInterceptSuccess": this.printEvent,
			"OnInterceptFailure": this.printEvent,
			"OnCallException": this.printEvent,
			"OnReceiveDTMF": this.printEvent,
			"MRSInfoNotify": this.printEvent,
			"OnLinkedPhoneNotify": this.printEvent,
			"PSTNBroadcastOnceSendOverNotify": this.printEvent,
			"OnTalkingPrempted": this.printEvent,
		},
		"VideoNotify": {
			"OnVideoDispatchRequest": this.printEvent,
			"OnVideoDispatchFailure": this.printEvent,
			"OnVideoDispatchSuccess": this.printEvent,
			"OnVideoDispatchStatusNotify": this.printEvent,
			"OnCancelVideoDispatchSuccess": this.printEvent,
			"OnCancelVideoDispatchFailure": this.printEvent,
			"OnStartVideoUploadWallSuccess": this.printEvent,
			"OnStartVideoUploadWallFailure": this.printEvent,
			"OnStopVideoUploadWallSuccess": this.printEvent,
			"OnStopVideoUploadWallFailure": this.printEvent,
		},
		"GroupCallNotify": {
			"OnGroupCallStatusNotify": this.printEvent,
			"OnTalkingGroupCallFailure": this.printEvent,
			"OnTalkingGroupCallPTTStart": this.printEvent,
			"OnTalkingGroupCallPTTNotify": this.printEvent,
			"OnTalkingGroupCallPTTSuccess": this.printEvent,
			"OnTalkingGroupCallPTTIdle": this.printEvent,
			"OnTalkingGroupCallPTTRecv": this.printEvent,
			"OnTalkingGroupCallStart": this.printEvent,
			"OnTalkingGroupCallStop": this.printEvent,
			"OnTalkingGroupCallRelease": this.printEvent,
			"OnTalkingGroupCallPTTFailure": this.printEvent,
			"OnTalkingGroupCallJoinNotify": this.printEvent,
			"OnAddTalkingGroupTempUserSuccess": this.printEvent,
			"OnAddTalkingGroupTempUserFailure": this.printEvent,
			"OnTalkingGourpEmergencyCallStart": this.printEvent,
			"OnTalkingGroupEmergencyCallUpdate": this.printEvent,
			"OnQueryPDTGroupSuccess": this.printEvent,
			"OnQueryPDTGroupFailure": this.printEvent,
			"OnSetListenGroupSuccess": this.printEvent,
			"OnSetListenGroupFailure": this.printEvent,
			"GroupMRSInfoNotify": this.printEvent,
		},
		"GroupNotify": {
			"OnTalkingGroupStatusChange": this.printEvent,
			"OnTalkingGroupMemberChange": this.printEvent,
			"OnDynamicGroupMemberChange": this.printEvent,
			"OnDynamicGroupChangeSuccess": this.printEvent,
			"OnChangePatchGroupFailure": this.printEvent,
			"OnPatchGroupStatusChange": this.printEvent,
			"OnPatchGroupMemberChange": this.printEvent,
			"OnDynamicGroupMemberChangeFailure": this.printEvent,
			"OnDynamicGroupOptFailed": this.printEvent,
			"OnPatchGroupOptFailed": this.printEvent,
			"OnPatchGroupOptSuccess": this.printEvent,
			"OnChangePatchGroupMemberFailure": this.printEvent,
			"OnChangePatchGroupMemberSucess": this.printEvent,
			"OnTalkingGroupMemberChangeFailure": this.printEvent
		},
		"MsNotify": {
			"OnSendDispSMSResult": this.printEvent,
			"OnRecvDispSMSNotify": this.printEvent,
			"OnDispSMSModuleNotify": this.printEvent,
			"OnSendDispMMSResult": this.printEvent,
			"OnRecvDispMMSNotify": this.printEvent,
			"OnDispMMSModuleNotify": this.printEvent,
			"OnRecvStateMsgNotify": this.printEvent
		},
		"GisNotify": {
			"OnSubscribeGISResult": this.printEvent,
			"OnUnSubscribeGISResult": this.printEvent,
			"OnRecvGISNotify": this.printEvent,
			"OnRecvGISTrackNotify":this.printEvent
		},
		"BroadcastInfoNotify": {
			"OnStartCallResult": this.printEvent,
			"OnEndCallResult": this.printEvent,
			"OnQueryBroadcastInfoListNotify": this.printEvent
		},
		"ResourceNotify": {
			"OnPushConnectSuccess": this.printEvent,
			"OnPushConnectFail": this.printEvent,
			"OnPushGetTokenFail": this.printEvent,
			"ResultOfMappingBetweenIsdn2Gbid": this.printEvent
		},
		"ModuleNotify": {
			"OnDisConnection": this.printEvent,
			"OnConnection": this.printEvent,
			"OnDispatchKickOutNotifyEvent": this.printEvent,
			"OnModifyPasswordNotify": this.printEvent,
			"OnDeleteAccountNotify": this.printEvent,
			"OnSetLinkedPhoneSuccess": this.printEvent,
			"OnSetLinkedPhoneFailure": this.printEvent
		},
		"PhoneConfNotify": {
			"OnCreateConfSuccess": this.printEvent,
			"OnCreateConfFailure": this.printEvent,
			"OnEndConfSuccess": this.printEvent,
			"OnEndConfFailure": this.printEvent,
			"OnSubscribeConfSuccess": this.printEvent,
			"OnSubscribeConfFailure": this.printEvent,
			"OnUnsubscribeConfSuccess": this.printEvent,
			"OnUnsubscribeConfFailure": this.printEvent,
			"OnConfDialInRinging": this.printEvent,
			"OnConfDialOutRinging": this.printEvent,
			"OnConfConnect": this.printEvent,
			"OnConfRelease": this.printEvent,
			"OnConfFailure": this.printEvent,
			"OnMuteConfMemberSuccess": this.printEvent,
			"OnMuteConfMemberFailure": this.printEvent,
			"OnUnmuteConfMemberSuccess": this.printEvent,
			"OnUnmuteConfMemberFailure": this.printEvent,
			"OnSetSpokenMemberSuccess": this.printEvent,
			"OnSetSpokenMemberFailure": this.printEvent,
			"OnMuteConfSuccess": this.printEvent,
			"OnMuteConfFailure": this.printEvent,
			"OnUnmuteConfSuccess": this.printEvent,
			"OnUnmuteConfFailure": this.printEvent,
			"OnBroadcastMixPictureSuccess": this.printEvent,
			"OnBroadcastMixPictureFailure": this.printEvent,
			"OnCancelBroadcastMixPictureSuccess": this.printEvent,
			"OnCancelBroadcastMixPictureFailure": this.printEvent,
			"OnBroadcastConfMemberSuccess": this.printEvent,
			"OnBroadcastConfMemberFailure": this.printEvent,
			"OnCancelBroadcastConfMemberSuccess": this.printEvent,
			"OnCancelBroadcastConfMemberFailure": this.printEvent,
			"OnWatchConfMemberSuccess": this.printEvent,
			"OnWatchConfMemberFailure": this.printEvent,
			"OnConfTextTipsSuccess": this.printEvent,
			"OnConfTextTipsFailure":this.printEvent,
			"OnWatchMixPictureSuccess": this.printEvent,
			"OnWatchMixPictureFailure": this.printEvent,
			"OnHangupConfMemberSuccess": this.printEvent,
			"OnHangupConfMemberFailure": this.printEvent,
			"OnCallConfMemberSuccess": this.printEvent,
			"OnCallConfMemberFailure": this.printEvent,
			"OnHoldConfSuccess": this.printEvent,
			"OnHoldConfFailure": this.printEvent,
			"OnUnholdConfSuccess": this.printEvent,
			"OnUnholdConfFailure": this.printEvent,
			"OnConfStatusNotify": this.printEvent,
			"OnQueryConfListByAttendeeResult": this.printEvent,
			"OnQueryRecordURLByConfIDResult": this.printEvent,
			"OnQueryVWallDisplayMatrixInfoResult": this.printEvent,
			"OnQueryConfWallInfoResult": this.printEvent,
			"OnStartConfUploadWallSuccess": this.printEvent,
			"OnStartConfUploadWallFailure": this.printEvent,
			"OnStopConfUploadWallSuccess": this.printEvent,
			"OnStopConfUploadWallFailure": this.printEvent,
			"OnAddConfMembersSuccess": this.printEvent,
			"OnAddConfMembersFailure": this.printEvent,
			"OnQueryVDCAddressBookSuccess": this.printEvent,
            "OnQueryVDCAddressBookFailure": this.printEvent,
            "OnQueryConfereeAddressBookSuccess": this.printEvent,
            "OnQueryConfereeAddressBookFailure": this.printEvent,
			"OnConfException": this.printEvent,
			"OnDelConfMembersSuccess": this.printEvent,
			"OnDelConfMembersFailure": this.printEvent,
			"OnConfProxyFloorSuccess": this.printEvent,
			"OnConfProxyFloorFailure": this.printEvent,
			"OnQueryContinuousPresenceInfoResult": this.printEvent,
			"OnApplyFloor": this.printEvent,
			"OnConfSetChairmanSuccess": this.printEvent,
			"OnConfSetChairmanFailure": this.printEvent,
			"OnConfProlongTimeSuccess": this.printEvent,
			"OnConfProlongTimeFailure": this.printEvent,
			"OnConfLockVideoSourceSuccess": this.printEvent,
			"OnConfLockVideoSourceFailure": this.printEvent,
			"OnConfUnLockVideoSourceSuccess": this.printEvent,
			"OnConfUnLockVideoSourceFailure": this.printEvent,
			"OnConfLockConfSuccess": this.printEvent,
			"OnConfLockConfFailure": this.printEvent,
			"OnConfUnLockConfSuccess": this.printEvent,
			"OnConfUnLockConfFailure": this.printEvent,
			"OnRecvConfListNotifySuccess": this.printEvent,
			"OnRecvConfListNotifyFailure": this.printEvent,
			"OnQueryConferenceByIdSuccess": this.printEvent,
			"OnQueryConferenceByIdFailure": this.printEvent,
			"OnModifyConfSuccess": this.printEvent,
			"OnModifyConfFailure": this.printEvent,
            "OnQueryCasConfInfosSuccess": this.printEvent,
            "OnQueryCasConfInfosFailure": this.printEvent,
            "OnQueryOrgTreeSuccess": this.printEvent,
            "OnQueryOrgTreeFail": this.printEvent,
            "OnQueryAreaOrgInfoSuccess": this.printEvent,
            "OnQueryAreaOrgInfoFail": this.printEvent,
			"OnQueryConfURLSuccess": this.printEvent,
			"OnQueryConfURLFailure": this.printEvent,
			"OnRecvQueryConfInfoNotifySuccess": this.printEvent,
			"OnRecvQueryConfInfoNotifyFailure": this.printEvent,
		},
		"MSPNotify": {
			"OnGetSoundDeviceResult": this.printEvent,
			"OnGetVolumeResult": this.printEvent,
			"OnSetVolumeResult": this.printEvent,
			"OnSetRingVolumeResult": this.printEvent,
			"OnMuteSpeakerResult": this.printEvent,
			"OnUnmuteSpeakerResult": this.printEvent,
			"OnMuteMicResult": this.printEvent,
			"OnUnmuteMicResult": this.printEvent,
			"OnQuerySoundDeviceResult": this.printEvent,
			"OnAssignSoundDeviceResult": this.printEvent,
			"OnSendToDTMFResult": this.printEvent,
			"OnVDMStatusNotify": this.printEvent,
			"OnGetResolutionResult": this.printEvent,
			"OnHideVideoWindowResult": this.printEvent,
			"OnWindowOnTopResult": this.printEvent,
			"OnWindowDragEnableResult": this.printEvent,
			"OnWindowShowBorderResult": this.printEvent,
			"OnWindowOperateNotify": this.printEvent,
			"OnDownloadProgressEvent": this.printEvent,
			"OnInstallMspEvent": this.printEvent,
			"OnUpgradeMspVersionResult": this.printEvent,
			"OnUpdateMsp": this.printEvent,
			"OnCreateWindowResult": this.printEvent,
			"OnDestroyWindowResult": this.printEvent,
			"OnVideoStreamSPSEvent": this.printEvent,
			"OnStartLocalRecordEvent": this.printEvent,
			"OnStopLocalRecordEvent": this.printEvent,
			"OnMspInstallRunStatus": this.printEvent,
			"OnStartWindowShare": this.printEvent,
			"OnSetMediaModeResult": this.printEvent,
			"onMediaNotify": this.printEvent,
		}
	};
};

/**
 * 处理GIS的业务通知
 * @param event
 */
ICPSDK_Dispatch_Event.prototype.delGisNotify = function(event) {
	var reqId = event.reqId;
	if (reqId == null || reqId == undefined) {reqId = "0";}
	var opt = event["opt"];
	if (undefined != opt) {
		delete event.opt;
	}

	if (opt == "sub") {
		// 订阅

		for (var item = 0; item < event["uelist"].length; item++) {
			if (event["uelist"][item]["rsp"] == "0" && !cloudICP.util.includes(cloudICP.dispatch.gis.subList, event[
					"uelist"][item]["isdn"])) {
				cloudICP.dispatch.gis.subList.push(event["uelist"][item]["isdn"]);
			}
		}
		event["eventName"] = "OnSubscribeGISResult";
		this.EVENT_LIST["GisNotify"]["OnSubscribeGISResult"].event = event;
		sendEvent2Page(reqId, this.EVENT_LIST["GisNotify"]["OnSubscribeGISResult"]);
	} else if (opt == "unsub") {
		// 取消订阅

		for (var item = 0; item < event["uelist"].length; item++) {
			if (event["uelist"][item]["rsp"] == "0" && cloudICP.util.includes(cloudICP.dispatch.gis.subList, event[
					"uelist"][item]["isdn"])) {
				var index = cloudICP.dispatch.gis.subList.indexOf(event["uelist"][item]["isdn"]);
				if (index != -1) {
					cloudICP.dispatch.gis.subList.splice(index, 1);
				}
			}
		}

		event["eventName"] = "OnUnSubscribeGISResult";
		this.EVENT_LIST["GisNotify"]["OnUnSubscribeGISResult"].event = event;
		sendEvent2Page(reqId, this.EVENT_LIST["GisNotify"]["OnUnSubscribeGISResult"]);
	} else if (opt == "report") {
		// 上报通知

		for (var item = 0; item < event["list"].length; item++) {
			if (!cloudICP.util.includes(cloudICP.dispatch.gis.subList, event["list"][item]["isdn"])) {
				event["list"].splice(item, 1);
			}
		}

		if (event["list"].length == 0) {
			return;
		}

		event["eventName"] = "OnRecvGISNotify";
		this.EVENT_LIST["GisNotify"]["OnRecvGISNotify"].event = event;
		sendEvent2Page(reqId, this.EVENT_LIST["GisNotify"]["OnRecvGISNotify"]);
	} else if (opt == "track") {
        event["eventName"] = "OnRecvGISTrackNotify";
        this.EVENT_LIST["GisNotify"]["OnRecvGISTrackNotify"].event = event;
		sendEvent2Page(reqId, this.EVENT_LIST["GisNotify"]["OnRecvGISTrackNotify"]);
    } else {
        cloudICP.util.log(undefined, { "logLevel": "error", "logMsg": "delGisNotify: unkown opt. the opt is." + opt });
        return;
    }
}

/**
 * 处理三方广播的结果通知
 * @param event
 */
 ICPSDK_Dispatch_Event.prototype.delBroadcastInfoNotify = function(event) {
    var opt = event["opt"];
    if (undefined != opt) {
        delete event.opt;
    }

    if (opt == "startCall") {
        // 发起广播
        event["eventName"] = "OnStartCallResult";
        this.EVENT_LIST["BroadcastInfoNotify"]["OnStartCallResult"](event);
    } else if (opt == "endCall") {
        // 停止广播
        event["eventName"] = "OnEndCallResult";
        this.EVENT_LIST["BroadcastInfoNotify"]["OnEndCallResult"](event);
    } else if (opt == "getList") {
        // 查询广播信息
        event["eventName"] = "OnQueryBroadcastInfoListNotify";
        this.EVENT_LIST["BroadcastInfoNotify"]["OnQueryBroadcastInfoListNotify"](event);
    } else {
        cloudICP.util.log(undefined, { "logLevel": "error", "logMsg": "delBroadcastInfoNotify: unkown opt. the opt is." + opt });
        return;
    }
}

/**
 * 处理模块的状态通知
 */
ICPSDK_Dispatch_Event.prototype.delModuleNotify = function(event) {
	var reqId = event.reqId;
	if (reqId == null || reqId == undefined) {reqId = "0";}
	var statusType = event["statustype"];
	if (undefined != statusType) {
		delete event.statustype;
	}
	var statusValue = event["statusvalue"];
	if (statusType == "1") {
		//短信
		if (statusValue == "1") {
			//短信溢出
			event["eventName"] = "OnDispSMSModuleNotify";
			this.EVENT_LIST["MsNotify"]["OnDispSMSModuleNotify"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["MsNotify"]["OnDispSMSModuleNotify"]);
		} else if (statusValue == "2") {
			//短信连接断开
			var event = {};
			event["eventName"] = "OnDisConnection";
			event["moduleType"] = "1";
			this.EVENT_LIST["ModuleNotify"]["OnDisConnection"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnDisConnection"]);
		} else if (statusValue == "3") {
			//短信连接恢复
			var event = {};
			event["eventName"] = "OnConnection";
			event["moduleType"] = "1";
			this.EVENT_LIST["ModuleNotify"]["OnConnection"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnConnection"]);
		}

	} else if (statusType == "2") {
		//彩信
		event["eventName"] = "OnDispMMSModuleNotify";
		this.EVENT_LIST["MsNotify"]["OnDispMMSModuleNotify"].event = event;
		sendEvent2Page(reqId, this.EVENT_LIST["MsNotify"]["OnDispMMSModuleNotify"]);
	} else if (statusType == "6") {
		//表示SIP模块
		if (statusValue == "15") {
			//用户已经登出
			clearTimeout(cloudICP.dispatch.auth.sendHeartbeatTimeOutId);
			delete event.statusvalue;
			logoutCleanUp();
			event["eventName"] = "OnDispatchKickOutNotifyEvent";
			this.EVENT_LIST["ModuleNotify"]["OnDispatchKickOutNotifyEvent"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnDispatchKickOutNotifyEvent"]);
		} else if (statusValue == "9") {
			//SIP连接断开
			event = {};
			event["eventName"] = "OnDisConnection";
			event["moduleType"] = "2";
			this.EVENT_LIST["ModuleNotify"]["OnDisConnection"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnDisConnection"]);
		} else if (statusValue == "10") {
			//SIP连接恢复
			event = {};
			event["eventName"] = "OnConnection";
			event["moduleType"] = "2";
			this.EVENT_LIST["ModuleNotify"]["OnConnection"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnConnection"]);
		} else if (statusValue == "22") {
			//账号被删除
			delete event.statusvalue;
			logoutCleanUp();
			event["eventName"] = "OnDeleteAccountNotify";
			this.EVENT_LIST["ModuleNotify"]["OnDeleteAccountNotify"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnDeleteAccountNotify"]);
		} else if (statusValue == "17") {
			//密码被修改
			delete event.statusvalue;
			cloudICP.userInfo["isdn"] = "";
			logoutCleanUp();
			event["eventName"] = "OnModifyPasswordNotify";
			event["rsp"] = "0";
			this.EVENT_LIST["ModuleNotify"]["OnModifyPasswordNotify"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnModifyPasswordNotify"]);
		}
	} else if (statusType == "12") {
		if (statusValue == "43") {
			// push连接断开
			event = {};
			event["eventName"] = "OnPushDisConnection";
			event["moduleType"] = "12";
			this.EVENT_LIST["ModuleNotify"]["OnDisConnection"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnDisConnection"]);
		} else if (statusValue == "44") {
			// push连接
			event = {};
			event["eventName"] = "OnPushConnection";
			event["moduleType"] = "12";
			this.EVENT_LIST["ModuleNotify"]["OnConnection"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnConnection"]);
		}
	} else if (statusType == "9") {
		if (event["statusvalue"] == "16") {
			if (event["param"]["type"] == "4") {
				if (event["param"]["ret"] == 0) {
					var retEvent = {
						"eventName": "OnTransferSuccess",
						"rsp": "0"
					}
					this.EVENT_LIST["VoiceNotify"]["OnTransferSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnTransferSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnTransferFailure",
						"rsp": event["param"]["ret"]
					}
					this.EVENT_LIST["VoiceNotify"]["OnTransferFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnTransferFailure"]);
				}
			} else if (event["param"]["type"] == "5") {
				if (event["param"]["ret"] != "0") {
					var retEvent = {
						"eventName": "OnInterceptFailure",
						"rsp": event["param"]["ret"],
					}
					this.EVENT_LIST["VoiceNotify"]["OnInterceptFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnInterceptFailure"]);
				} else if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnInterceptSuccess",
						"rsp": event["param"]["ret"],
					}
					this.EVENT_LIST["VoiceNotify"]["OnInterceptSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnInterceptSuccess"]);
				}
			} else if (event["param"]["type"] == "0") {
				if (event["param"]["ret"] != "0") {
					var retEvent = {
						"eventName": "OnBreakOffFailure",
						"rsp": event["param"]["ret"],
						"value": {
							"objid": event["param"]["ObjId"]
						}
					}
					this.EVENT_LIST["VoiceNotify"]["OnBreakOffFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnBreakOffFailure"]);
				} else if (event["param"]["ret"] == "0") {
					if (cloudICP.dispatch.group.isCurrentPtt["grpid"] == event["param"]["ObjId"]) {
						cloudICP.dispatch.group.isCurrentPtt = {
							"grpid": event["param"]["ObjId"],
							"isPtt": false
						};
					}

					var retEvent = {
						"eventName": "OnBreakOffSuccess",
						"rsp": event["param"]["ret"],
						"value": {
							"objid": event["param"]["ObjId"]
						}
					}
					this.EVENT_LIST["VoiceNotify"]["OnBreakOffSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnBreakOffSuccess"]);

				}
			} else if (event["param"]["type"] == "1") {
				var retEvent = {
					"eventName": "OnTalkingGroupCallJoinNotify",
					"rsp": event["param"]["ret"],
					"value": {
						"grpid": event["param"]["ObjId"]
					}
				}
				this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallJoinNotify"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallJoinNotify"]);
			} else if (event["param"]["type"] == "7") {
				if (event["param"]["ret"] != "0") {
					var retEvent = {
						"eventName": "OnVideoDispatchFailure",
						"rsp": event["param"]["ret"],
					}
					this.EVENT_LIST["VideoNotify"]["OnVideoDispatchFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VideoNotify"]["OnVideoDispatchFailure"]);
				} else {
					var retEvent = {
						"eventName": "OnVideoDispatchSuccess",
						"rsp": "0",
					}
					this.EVENT_LIST["VideoNotify"]["OnVideoDispatchSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VideoNotify"]["OnVideoDispatchSuccess"]);
				}
			} else if (event["param"]["type"] == "8") {
				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnCancelVideoDispatchSuccess",
						"rsp": "0",
					}
					this.EVENT_LIST["VideoNotify"]["OnCancelVideoDispatchSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VideoNotify"]["OnCancelVideoDispatchSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnCancelVideoDispatchFailure",
						"rsp": event["param"]["ret"],
					}
					this.EVENT_LIST["VideoNotify"]["OnCancelVideoDispatchFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VideoNotify"]["OnCancelVideoDispatchFailure"]);
				}

			} else if (event["param"]["type"] == "9") {
				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnStopVideoUploadWallSuccess",
						"rsp": "0",
						"value": {
							"channel": event["param"]["DstObjId"]
						}
					}
					this.EVENT_LIST["VideoNotify"]["OnStopVideoUploadWallSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VideoNotify"]["OnStopVideoUploadWallSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnStopVideoUploadWallFailure",
						"rsp": event["param"]["ret"],
					}
					this.EVENT_LIST["VideoNotify"]["OnStopVideoUploadWallFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VideoNotify"]["OnStopVideoUploadWallFailure"]);
				}
			} else if (event["param"]["type"] == "10") {
				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnStartVideoUploadWallSuccess",
						"rsp": "0",
						"value": {
							"channel": event["param"]["DstObjId"],
							"src": event["param"]["SrcObjId"]
						}
					}
					this.EVENT_LIST["VideoNotify"]["OnStartVideoUploadWallSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VideoNotify"]["OnStartVideoUploadWallSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnStartVideoUploadWallFailure",
						"rsp": event["param"]["ret"],
					}
					this.EVENT_LIST["VideoNotify"]["OnStartVideoUploadWallFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VideoNotify"]["OnStartVideoUploadWallFailure"]);
				}
			} else if (event["param"]["type"] == "17") {
				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnAddTalkingGroupTempUserSuccess",
						"groupid": event["param"]["groupid"],
						"useid": event["param"]["useid"],
						"rsp": "0",
					}
					this.EVENT_LIST["GroupCallNotify"]["OnAddTalkingGroupTempUserSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnAddTalkingGroupTempUserSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnAddTalkingGroupTempUserFailure",
						"rsp": event["param"]["ret"],
						"groupid": event["param"]["groupid"],
						"useid": event["param"]["useid"],
					}
					this.EVENT_LIST["GroupCallNotify"]["OnAddTalkingGroupTempUserFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnAddTalkingGroupTempUserFailure"]);
				}
			} else if (event["param"]["type"] == "21") {
				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnCancelTransferSuccess",
						"rsp": "0",
					}
					this.EVENT_LIST["VoiceNotify"]["OnCancelTransferSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCancelTransferSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnCancelTransferFailure",
						"rsp": event["param"]["ret"],
					}
					this.EVENT_LIST["VoiceNotify"]["OnCancelTransferFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCancelTransferFailure"]);
				}
			} else if (event["param"]["type"] == "24") {

				// modified by lwx1179774 问题是保持和取消保持偶尔会失败，策略是加了延时处理
				setTimeout(function() {
					var holdCid = "-1";
					var callStatusMgr = cloudICP.util.callStatusMgr;

					for (var index = callStatusMgr.holdMgr.length - 1; index >= 0; index--) {
						var call = callStatusMgr.allCalls[callStatusMgr.holdMgr[index]];
						var callID = "";
						if (call["callee"] == cloudICP.userInfo["isdn"]) {
							callID = call["caller"];
						} else if (call["caller"] == cloudICP.userInfo["isdn"]) {
							callID = call["callee"];
						} else {
							return;
						}
						if (call && callID == event["param"]["isdn"]) {
							holdCid = callStatusMgr.holdMgr[index];
							callStatusMgr.holdMgr.splice(index, 1);
							break;
						}
					}

					if (holdCid == "-1") {
						return;
					}

					if (event["param"]["ret"] == "0") {
						var retEvent = {
							"eventName": "OnHoldSuccess",
							"rsp": "0",
							"value": {
								"peerid": event["param"]["isdn"],
								"cid": holdCid
							}
						}
						callStatusMgr.updateCallStatus(null, callStatusMgr.HOLD, {
							"value": {
								"cid": holdCid
							}
						});
						cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnHoldSuccess"].event = retEvent;
						sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnHoldSuccess"]);
					} else {
						var retEvent = {
							"eventName": "OnHoldFailure",
							"rsp": event["param"]["ret"],
							"value": {
								"peerid": event["param"]["isdn"],
								"cid": holdCid
							}
						}

						cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnHoldFailure"].event = retEvent;
						sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnHoldFailure"]);
					}
				}, 500);
			} else if (event["param"]["type"] == "25") {

				// modified by lwx1179774 问题是保持和取消保持偶尔会失败，策略是加了延时处理
				setTimeout(function() {
					var holdCid = "-1";
					var callStatusMgr = cloudICP.util.callStatusMgr;

					for (var index = callStatusMgr.holdMgr.length - 1; index >= 0; index--) {
						var call = callStatusMgr.allCalls[callStatusMgr.holdMgr[index]];
						var callID = "";
						if (call["callee"] == cloudICP.userInfo["isdn"]) {
							callID = call["caller"];
						} else if (call["caller"] == cloudICP.userInfo["isdn"]) {
							callID = call["callee"];
						} else {
							return;
						}
						if (call && callID == event["param"]["isdn"]) {
							holdCid = callStatusMgr.holdMgr[index];
							callStatusMgr.holdMgr.splice(index, 1);
							break;
						}
					}

					if (holdCid == "-1") {
						return;
					}

					if (event["param"]["ret"] == "0") {
						var retEvent = {
							"eventName": "OnUnholdSuccess",
							"rsp": "0",
							"value": {
								"peerid": event["param"]["isdn"],
								"cid": holdCid
							}
						}

						callStatusMgr.unholdCall(holdCid, reqId);

						callStatusMgr.updateCallStatus(null, callStatusMgr.CONNECTING, {
							"value": {
								"cid": holdCid
							}
						});
						cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnUnholdSuccess"].event = retEvent;
						sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnUnholdSuccess"]);
					} else {
						var retEvent = {
							"eventName": "OnUnholdFailure",
							"rsp": event["param"]["ret"],
							"value": {
								"peerid": event["param"]["isdn"],
								"cid": holdCid
							}
						}

						cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnUnholdFailure"].event = retEvent;
						sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnUnholdFailure"]);
					}
				}, 500);
			} else if (event["param"]["type"] == "26") {
				var holdCid = "-1";
				var callStatusMgr = cloudICP.util.callStatusMgr;

				for (var index = callStatusMgr.holdMgr.length - 1; index >= 0; index--) {
					var call = callStatusMgr.allCalls[callStatusMgr.holdMgr[index]];
					var callID = "";
					if (call["callee"] == cloudICP.userInfo["isdn"]) {
						callID = call["caller"];
					} else if (call["caller"] == cloudICP.userInfo["isdn"]) {
						callID = call["callee"];
					} else {
						return;
					}
					if (call && callID == event["param"]["isdn"]) {
						holdCid = callStatusMgr.holdMgr[index];
						callStatusMgr.holdMgr.splice(index, 1);
						break;
					}
				}

				if (holdCid == "-1") {
					return;
				}

				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnHoldConfSuccess",
						"rsp": "0",
						"value": {
							"confCode": event["param"]["isdn"],
							"cid": holdCid
						}
					}
					callStatusMgr.updateCallStatus(null, callStatusMgr.HOLD, {
						"value": {
							"cid": holdCid
						}
					});
					this.EVENT_LIST["PhoneConfNotify"]["OnHoldConfSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnHoldConfSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnHoldConfFailure",
						"rsp": event["param"]["ret"],
						"value": {
							"confCode": event["param"]["isdn"],
							"cid": holdCid
						}
					}

					this.EVENT_LIST["PhoneConfNotify"]["OnHoldConfFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnHoldConfFailure"]);
				}
			} else if (event["param"]["type"] == "27") {
				var holdCid = "-1";
				var callStatusMgr = cloudICP.util.callStatusMgr;

				for (var index = callStatusMgr.holdMgr.length - 1; index >= 0; index--) {
					var call = callStatusMgr.allCalls[callStatusMgr.holdMgr[index]];
					var callID = "";
					if (call["callee"] == cloudICP.userInfo["isdn"]) {
						callID = call["caller"];
					} else if (call["caller"] == cloudICP.userInfo["isdn"]) {
						callID = call["callee"];
					} else {
						return;
					}
					if (call && callID == event["param"]["isdn"]) {
						holdCid = callStatusMgr.holdMgr[index];
						callStatusMgr.holdMgr.splice(index, 1);
						break;
					}
				}

				if (holdCid == "-1") {
					return;
				}

				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnUnholdConfSuccess",
						"rsp": "0",
						"value": {
							"confCode": event["param"]["isdn"],
							"cid": holdCid
						}
					}

					callStatusMgr.unholdCall(holdCid, reqId);

					callStatusMgr.updateCallStatus(null, callStatusMgr.CONNECTING, {
						"value": {
							"cid": holdCid
						}
					});

					this.EVENT_LIST["PhoneConfNotify"]["OnUnholdConfSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnholdConfSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnUnholdConfFailure",
						"rsp": event["param"]["ret"],
						"value": {
							"confCode": event["param"]["isdn"],
							"cid": holdCid
						}
					}

					this.EVENT_LIST["PhoneConfNotify"]["OnUnholdConfFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnholdConfFailure"]);
				}
			} else if (event["param"]["type"] == "11") {
				if (event["param"]["ret"] != "0") {
					var retEvent = {
						"eventName": "OnDynamicGroupOptFailed",
						"rsp": event["param"]["ret"],
						"grpid": event["param"]["group"],
						"opt": event["param"]["opt"]
					}

					this.EVENT_LIST["GroupNotify"]["OnDynamicGroupOptFailed"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnDynamicGroupOptFailed"]);
				}
			} else if (event["param"]["type"] == "28") {
				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnConfProxyFloorSuccess",
						"rsp": "0",
						"value": {
							"member": event["param"]["member"],
							"operation": event["param"]["operation"]
						}
					}
					this.EVENT_LIST["PhoneConfNotify"]["OnConfProxyFloorSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfProxyFloorSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnConfProxyFloorFailure",
						"rsp": event["param"]["ret"],
						"value": {
							"member": event["param"]["member"],
							"operation": event["param"]["operation"]
						}
					}
					this.EVENT_LIST["PhoneConfNotify"]["OnConfProxyFloorFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfProxyFloorFailure"]);
				}
			} else if (event["param"]["type"] == "29") {
				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnQueryPDTGroupSuccess",
						"rsp": "0",
						"value": {
							"userid": event["param"]["userid"],
							"operation": event["param"]["operation"],
							"groupid": event["param"]["groupid"],
							"result": event["param"]["result"]
						}
					}
					this.EVENT_LIST["GroupCallNotify"]["OnQueryPDTGroupSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnQueryPDTGroupSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnQueryPDTGroupFailure",
						"rsp": event["param"]["ret"],
						"value": {
							"userid": event["param"]["userid"],
							"operation": event["param"]["operation"]
						}
					}
					this.EVENT_LIST["GroupCallNotify"]["OnQueryPDTGroupFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnQueryPDTGroupFailure"]);
				}
			} else if (event["param"]["type"] == "30") {
				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnSetListenGroupSuccess",
						"rsp": "0",
						"value": {
							"operation": event["param"]["operation"],
							"groupid": event["param"]["groupid"],
							"result": event["param"]["result"]
						}
					}
					this.EVENT_LIST["GroupCallNotify"]["OnSetListenGroupSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnSetListenGroupSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnSetListenGroupFailure",
						"rsp": event["param"]["ret"],
						"value": {
							"groupid": event["param"]["groupid"],
							"operation": event["param"]["operation"]
						}
					}
					this.EVENT_LIST["GroupCallNotify"]["OnSetListenGroupFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnSetListenGroupFailure"]);
				}
			} else if (event["param"]["type"] == "31") {
				if (event["param"]["ret"] == "0") {
					var retEvent = {
						"eventName": "OnSetLinkedPhoneSuccess",
						"rsp": "0"

					}
					this.EVENT_LIST["ModuleNotify"]["OnSetLinkedPhoneSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnSetLinkedPhoneSuccess"]);

				} else {
					var retEvent = {
						"eventName": "OnSetLinkedPhoneFailure",
						"rsp": event["param"]["ret"]
					}
					this.EVENT_LIST["ModuleNotify"]["OnSetLinkedPhoneFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["ModuleNotify"]["OnSetLinkedPhoneFailure"]);
				}
			}
		}
	}
};

ICPSDK_Dispatch_Event.prototype.delMsNotify = function(event) {
	var reqId = event.reqId;
	if (reqId == null || reqId == undefined) {reqId = "0";}
	var opt = event["opt"];
	if (undefined != opt) {
		delete event.opt;
	}
	var value = event["value"];
	if (undefined == value) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "delMsNotify: value is undefined."
		});
		return;
	}

	var type = value["type"];
	if (undefined != type) {
		delete value.type;
	}

	if (undefined != value["direction"]) {
		delete value.direction;
	}

	if (type == "0001") {
		//短信
		if (undefined != value["statuscode"]) {
			delete value.statuscode;
		}
		if (undefined != value["speed"]) {
			delete value.speed;
		}

		if (undefined != value["attach"]) {
			delete value.attach;
		}

		if (opt == "recvmsg") {
			//接收短信
			event["rsp"] = "0";

			// 转化时区
			if (value["state"] == 1) {
				var tmpTime = value["date"].split("-");
				var tempDate = value["time"].split(":");
				var time = new Date(Date.UTC(tmpTime[0], parseInt(tmpTime[1]) - 1, tmpTime[2], tempDate[0],
					tempDate[1], tempDate[2]));
				var mYear = time.getFullYear().toString();
				var tempMonth = time.getMonth() + 1;
				var mMonth = (tempMonth < 10) ? "0" + tempMonth.toString() : tempMonth.toString();
				var mDay = (time.getDate() < 10) ? "0" + time.getDate().toString() : time.getDate().toString();
				var mHour = (time.getHours() < 10) ? "0" + time.getHours().toString() : time.getHours().toString();
				var mMinute = (time.getMinutes() < 10) ? "0" + time.getMinutes().toString() : time.getMinutes()
					.toString();
				var mSecond = (time.getSeconds() < 10) ? "0" + time.getSeconds().toString() : time.getSeconds()
					.toString();

				value["date"] = mYear + "-" + mMonth + "-" + mDay;
				value["time"] = mHour + ":" + mMinute + ":" + mSecond;
			}

			if (undefined != value["state"]) {
				delete value.state;
			}

			event["eventName"] = "OnRecvDispSMSNotify";
			if (undefined != event["value"]["audiocontent"]) {
				event["value"]["audiocontent"] = event["value"]["audiocontent"].replace(/-/g, "+");
				event["value"]["audiocontent"] = event["value"]["audiocontent"].replace(/_/g, "/");
			}
			this.EVENT_LIST["MsNotify"]["OnRecvDispSMSNotify"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["MsNotify"]["OnRecvDispSMSNotify"]);
		} else if (opt == "sendresult") {
			//发送短信
			if (undefined != value["groupid"]) {
				delete value.groupid;
			}

			if (undefined != value["state"]) {
				delete value.state;
			}

			if (undefined != value["emerg_groupid"]) {
				delete value.emerg_groupid;
			}
			if (undefined != value["emerg_ueid"]) {
				delete value.emerg_ueid;
			}

			// value.msgid = new Date(value["date"] + " " + value["time"]).getTime().toString()
			event["eventName"] = "OnSendDispSMSResult";
			this.EVENT_LIST["MsNotify"]["OnSendDispSMSResult"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["MsNotify"]["OnSendDispSMSResult"]);
		}
	} else if (type == "0002") {
		//状态短信
		if (undefined != value["state"]) {
			delete value.state;
		}
		if (undefined != event["rsp"]) {
			delete event.rsp;
		}
		if (undefined != value["speed"]) {
			delete value.speed;
		}
		if (undefined != value["emerg_groupid"]) {
			delete value.emerg_groupid;
		}
		if (undefined != value["emerg_ueid"]) {
			delete value.emerg_ueid;
		}
		if (undefined != value["groupid"]) {
			delete value.groupid;
		}
		if (undefined != value["attach"]) {
			delete value.attach;
		}
		event["eventName"] = "OnRecvStateMsgNotify";
		this.EVENT_LIST["MsNotify"]["OnRecvStateMsgNotify"].event = event;
		sendEvent2Page(reqId, this.EVENT_LIST["MsNotify"]["OnRecvStateMsgNotify"]);
	} else if (type == "0003") {
		//表示告警信息

	} else if (type == "0004") {
		if (undefined != value["speed"]) {
			delete value.speed;
		}

		if (undefined != value["statuscode"]) {
			delete value.statuscode;
		}

		//表示彩信
		if (opt == "recvmsg") {
			//接收彩信
			event["rsp"] = "0";

			// 转化时区
			if (value["state"] == 1) {
				var tmpTime = value["date"].split("-");
				var tempDate = value["time"].split(":");
				var time = new Date(Date.UTC(tmpTime[0], parseInt(tmpTime[1]) - 1, tmpTime[2], tempDate[0],
					tempDate[1], tempDate[2]));
				var mYear = time.getFullYear().toString();
				var tempMonth = time.getMonth() + 1;
				var mMonth = (tempMonth < 10) ? "0" + tempMonth.toString() : tempMonth.toString();
				var mDay = (time.getDate() < 10) ? "0" + time.getDate().toString() : time.getDate().toString();
				var mHour = (time.getHours() < 10) ? "0" + time.getHours().toString() : time.getHours().toString();
				var mMinute = (time.getMinutes() < 10) ? "0" + time.getMinutes().toString() : time.getMinutes()
					.toString();
				var mSecond = (time.getSeconds() < 10) ? "0" + time.getSeconds().toString() : time.getSeconds()
					.toString();

				value["date"] = mYear + "-" + mMonth + "-" + mDay;
				value["time"] = mHour + ":" + mMinute + ":" + mSecond;
			}

			if (undefined != value["state"]) {
				delete value.state;
			}

			event["eventName"] = "OnRecvDispMMSNotify";
			this.EVENT_LIST["MsNotify"]["OnRecvDispMMSNotify"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["MsNotify"]["OnRecvDispMMSNotify"]);
		} else if (opt == "sendresult") {
			//发送彩信
			if (undefined != value["groupid"]) {
				delete value.groupid;
			}

			if (undefined != value["state"]) {
				delete value.state;
			}

			if (undefined != value["emerg_groupid"]) {
				delete value.emerg_groupid;
			}
			if (undefined != value["emerg_ueid"]) {
				delete value.emerg_ueid;
			}

			event["eventName"] = "OnSendDispMMSResult";
			this.EVENT_LIST["MsNotify"]["OnSendDispMMSResult"].event = event;
			sendEvent2Page(reqId, this.EVENT_LIST["MsNotify"]["OnSendDispMMSResult"]);
		}

	} else if (type == "0010") {
		//表示ACK消息

	} else if (type == "0011") {
		//表示消息发送失败错误码
	}


}

/**
 * 处理视频通知
 */
ICPSDK_Dispatch_Event.prototype.delVideoNotify = function(event) {
	var reqId = event.reqId;
	if (reqId == null || reqId == undefined) {reqId = "0";}
	var rsp = event["rsp"];
	if (undefined == rsp) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "delVideoNotify: rsp is undefined."
		});
		return;
	}

	var value = event["value"];
	if (undefined == value) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "delVideoNotify: value is undefined."
		});
		return;
	}

	var confInfo = value["confInfo"];
	if (undefined != confInfo) {
		if (!!confInfo["confInternalId"]) {
			value["confId"] = confInfo["confInternalId"];
		}
		delete value.confInfo;
	}

	var from;
	if (undefined != value["from"] && value["from"] != "" && value["caller"] != value["from"]) {
		from = value["from"];
	} else {
		from = value["caller"];
	}

	var newEvent = {
		"eventName": "",
		"rsp": rsp,
		"value": {
			"callee": value["callee"],
			"caller": value["caller"],
			"cid": value["cid"],
			"fmt": value["fmt"]
		}
	}

	if (value["caller"] == cloudICP.userInfo["isdn"]) {
		// 呼出方
		switch (rsp) {
			case "3004":
				var callStatusMgr = cloudICP.util.callStatusMgr;
				if (!!callStatusMgr.windowInfos[value["callee"]]) { //视频点呼、视频监控、主动入会
					callStatusMgr.windowInfos[value["callee"]]["cid"] = event["value"]["cid"];
				}
				if (confInfo != undefined) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.NEW, event, confInfo);
					break;
				}

				if (event["value"]["isVideoDial"] == "0") {
					var callStatusMgr = cloudICP.util.callStatusMgr;
					callStatusMgr.updateCallStatus(callStatusMgr.MONITOR, callStatusMgr.NEW, event);

					newEvent["value"]["calltype"] = "monitor";
					newEvent["eventName"] = "OnDialOutProceeding";
					this.EVENT_LIST["VoiceNotify"]["OnDialOutProceeding"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutProceeding"]);
					break;
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.NEW, event);

				newEvent["value"]["calltype"] = "video";
				newEvent["eventName"] = "OnDialOutProceeding";
				this.EVENT_LIST["VoiceNotify"]["OnDialOutProceeding"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutProceeding"]);
				break;
				// 视频振铃事件
			case "3005":
				// 播放回铃音
				var param =
					"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":1, \"delay\":0, \"duration\":0, \"playcount\":0}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VIDEOCONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.RINGING, event);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "true";
					newEvent["eventName"] = "OnConfDialOutRinging";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfDialOutRinging"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfDialOutRinging"]);
					break;
				} else if (call && call.callType == callStatusMgr.MONITOR) {
					callStatusMgr.updateCallStatus(callStatusMgr.MONITOR, callStatusMgr.RINGING, event);

					newEvent["value"]["calltype"] = "monitor";
					newEvent["eventName"] = "OnDialOutRinging";
					this.EVENT_LIST["VoiceNotify"]["OnDialOutRinging"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutRinging"]);
					break;
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.RINGING, event);

				newEvent["value"]["calltype"] = "video";
				newEvent["eventName"] = "OnDialOutRinging";
				this.EVENT_LIST["VoiceNotify"]["OnDialOutRinging"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutRinging"]);
				break;
				// 视频点呼建立事件
			case "3003":

				// 停止振铃
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 建立音频媒体通道
				if (value["local_audio"] != "0.0.0.0,0" && value["server_audio"] != "0.0.0.0,0") {
					let localArr = value["local_audio"].split(",");
					let serverArr = value["server_audio"].split(",");

					var user = cloudICP.userInfo["isdn"];
					var peerUser = value["callee"];
					if (user == peerUser) {
						peerUser = value["caller"];
					}

					var callStatusMgr = cloudICP.util.callStatusMgr;
					var call = callStatusMgr.allCalls[event["value"]["cid"]];
					if (call && call.callType == callStatusMgr.VIDEOCONF) {
						var param =
							"{\"description\":\"msp_create_audio_channel\", \"cmd\": 65537, \"param\":{\"resID\":{0}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"number\": \"conference\",\"user\":\"{7}\", \"peerUser\":\"{8}\"}}, \"reqId\": \"{9}\"}"
							.format(
								parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0],
								serverArr[1], parseInt(value["audio"]), parseInt(value["audio_ssrc"]), user,
								peerUser, reqId
							)

						cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
					} else {
						var callNumber;
						if (event["value"]["callee"] != cloudICP.userInfo["isdn"]) {
							callNumber = event["value"]["callee"]
						} else {
							callNumber = event["value"]["caller"];
						}


						if (call && call.callType == callStatusMgr.MONITOR) {
							var param =
								"{\"description\":\"msp_create_audio_channel\", \"cmd\": 65537, \"param\":{\"resID\":{0}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"number\": \"{7}\" , \"recvOnly\": true ,\"user\":\"{8}\", \"peerUser\":\"{9}\"}}, \"reqId\": \"{10}\"}"
								.format(
									parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0],
									serverArr[1], parseInt(value["audio"]), parseInt(value["audio_ssrc"]), parseInt(
										callNumber), user, peerUser, reqId
								)

							cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
						} else {
							var param =
								"{\"description\":\"msp_create_audio_channel\", \"cmd\": 65537, \"param\":{\"resID\":{0}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"number\": \"{7}\",\"user\":\"{8}\", \"peerUser\":\"{9}\"} }, \"reqId\": \"{10}\"}"
								.format(
									parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0],
									serverArr[1], parseInt(value["audio"]), parseInt(value["audio_ssrc"]), parseInt(
										callNumber), user, peerUser, reqId
								)

							cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
						}
					}


				}

				var recvOnly = false;
				if (event["value"]["camera"] == "1") {
					recvOnly = true;
				}

				// 建立视频媒体通道
				if (value["local_video"] != "0.0.0.0,0" && value["server_video"] != "0.0.0.0,0") {
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]] = event;

					var windowInfo = cloudICP.util.callStatusMgr.windowInfos[value["cid"]]; //视频点呼被动接听 、被动视频入会
					var windowToolbarInfo = cloudICP.util.callStatusMgr.windowInfos[value[
						"callee"]]; //视频点呼、视频监控、主动入会
					var iuserWindowId = 0;
					var ishowToolbar = 1;
					var ibuttonIDs = 0;
					var bHide = false;
					var bEnableDrag = true;
					var iWidth = 640;
					var iHeight = 480;
					var iPosX = 400;
					var iPosY = 400;
					var fmtType = "TIN";
					var windowMode = cloudICP.config["mode"];
					var videoConfLocalWindow = null;
					var videoResizeMode = cloudICP.config["videoResizeMode"];

					if ((undefined != value["confId"]) && (undefined != cloudICP.config[
							"videoConfLocalWindow"])) { //会议业务
						videoConfLocalWindow = cloudICP.config["videoConfLocalWindow"];
					}
					if (!!windowToolbarInfo) {
						if (undefined != windowToolbarInfo["userWindowId"]) {
							iuserWindowId = parseInt(windowToolbarInfo["userWindowId"]);
							cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["userWindowId"] =
								iuserWindowId;
						}
						if (undefined != windowToolbarInfo["showToolbar"]) {
							ishowToolbar = parseInt(windowToolbarInfo["showToolbar"]);
						}
						if (undefined != windowToolbarInfo["buttonIDs"]) {
							ibuttonIDs = parseInt(windowToolbarInfo["buttonIDs"]);
						}
						if (undefined != windowToolbarInfo["hide"]) {
							bHide = Boolean(windowToolbarInfo["hide"]);
						}
						if (undefined != windowToolbarInfo["enableDrag"]) {
							bEnableDrag = Boolean(windowToolbarInfo["enableDrag"]);
						}
						if (undefined != windowToolbarInfo["mode"]) {
							windowMode = windowToolbarInfo["mode"].toString();
						}
						if (undefined != windowToolbarInfo["fmtType"]) {
							fmtType = windowToolbarInfo["fmtType"].toString();
						}
						if (undefined != windowToolbarInfo["width"]) {
							iWidth = parseInt(windowToolbarInfo["width"]);
						}
						if (undefined != windowToolbarInfo["height"]) {
							iHeight = parseInt(windowToolbarInfo["height"]);
						}
						if (undefined != windowToolbarInfo["posX"]) {
							iPosX = parseInt(windowToolbarInfo["posX"]);
						}
						if (undefined != windowToolbarInfo["posY"]) {
							iPosY = parseInt(windowToolbarInfo["posY"]);
						}
						if (undefined != windowToolbarInfo["videoConfLocalWindow"]) {
							videoConfLocalWindow = windowToolbarInfo["videoConfLocalWindow"];
						}
						if (undefined != windowToolbarInfo["videoResizeMode"]) {
							videoResizeMode = windowToolbarInfo["videoResizeMode"];
						}
					}
					if ((!!windowInfo) && (!!windowInfo["iCid"])) {
						if (undefined != windowInfo["userWindowId"]) {
							iuserWindowId = parseInt(windowInfo["userWindowId"]);
							cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["userWindowId"] =
								iuserWindowId;
						}
						if (undefined != windowInfo["showToolbar"]) {
							ishowToolbar = parseInt(windowInfo["showToolbar"]);
						}
						if (undefined != windowInfo["buttonIDs"]) {
							ibuttonIDs = parseInt(windowInfo["buttonIDs"]);
						}
						if (undefined != windowInfo["mode"]) {
							windowMode = windowInfo["mode"].toString();
						}
						if (undefined != windowInfo["fmtType"]) {
							fmtType = windowInfo["fmtType"].toString();
						}
						if (undefined != windowInfo["width"]) {
							iWidth = parseInt(windowInfo["width"]);
						}
						if (undefined != windowInfo["height"]) {
							iHeight = parseInt(windowInfo["height"]);
						}
						if (undefined != windowInfo["posX"]) {
							iPosX = parseInt(windowInfo["posX"]);
						}
						if (undefined != windowInfo["posY"]) {
							iPosY = parseInt(windowInfo["posY"]);
						}
						if (undefined != windowInfo["hide"]) {
							bHide = Boolean(windowInfo["hide"]);
						}
						if (undefined != windowInfo["enableDrag"]) {
							bEnableDrag = Boolean(windowInfo["enableDrag"]);
						}
						if (undefined != windowInfo["videoConfLocalWindow"]) {
							videoConfLocalWindow = windowInfo["videoConfLocalWindow"];
						}
						if (undefined != windowInfo["videoResizeMode"]) {
							videoResizeMode = windowInfo["videoResizeMode"];
						}
					}

					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["hide"] = bHide;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["enableDrag"] = bEnableDrag;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["showToolbar"] = ishowToolbar;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["buttonIDs"] = ibuttonIDs;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["width"] = iWidth;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["height"] = iHeight;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["posX"] = iPosX;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["posY"] = iPosY;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["winMode"] = windowMode;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["fmtType"] = fmtType;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["direction"] = "out";
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["rsp"] = event["rsp"];
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["videoConfLocalWindow"] = videoConfLocalWindow;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["tabPageId"] = reqId;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["videoResizeMode"] = videoResizeMode;

					if (0 != iuserWindowId) {
						param =
							"{\"description\":\"msp_create_video_window\", \"cmd\": 196610, \"param\":{\"resID\":{0}, \"windowName\": \"{1}\", \"mode\": \"{2}\", \"width\": {3}, \"height\": {4}, \"x\": {5}, \"y\":{6}, \"hide\": {7}, \"enableDrag\":{8}, \"scrollX\": {9}, \"scrollY\":{10}, \"showToolbar\": {11}, \"buttonIDs\": {12}, \"userWindowId\": {13}, \"videoResizeMode\": {15} }, \"reqId\": \"{14}\" }"
							.format(
								parseInt(value["cid"]), parseInt(value["cid"]), windowMode, iWidth, iHeight, iPosX,
								iPosY, bHide, bEnableDrag, 0, 0, ishowToolbar, ibuttonIDs, iuserWindowId, reqId, videoResizeMode
							);
					} else {
						param =
							"{\"description\":\"msp_create_video_window\", \"cmd\": 196610, \"param\":{\"resID\":{0}, \"windowName\": \"{1}\", \"mode\": \"{2}\", \"width\": {3}, \"height\": {4}, \"x\": {5}, \"y\":{6}, \"hide\": {7}, \"enableDrag\":{8}, \"scrollX\": {9}, \"scrollY\":{10}, \"showToolbar\": {11}, \"buttonIDs\": {12}, \"videoResizeMode\": {14} }, \"reqId\": \"{13}\"}"
							.format(
								parseInt(value["cid"]), parseInt(value["cid"]), windowMode, iWidth, iHeight, iPosX,
								iPosY, bHide, bEnableDrag, 0, 0, ishowToolbar, ibuttonIDs, reqId, videoResizeMode
							);
					}
					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true); //modify_by_caohui
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[value["cid"]];
				if (call && call.callType == callStatusMgr.MONITOR) {
					callStatusMgr.updateCallStatus(callStatusMgr.MONITOR, callStatusMgr.CONNECTING, event);
					break; //暂时不通知上层，等创建窗口成功后通知
				} else if (call && call.callType == callStatusMgr.VIDEOCONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.CONNECTING, event);
					callStatusMgr.holdCall(value["cid"], reqId);
					break; //暂时不通知上层，等创建窗口成功后通知
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.CONNECTING, event);
				callStatusMgr.holdCall(event["value"]["cid"], reqId);
				//暂时不通知上层，等创建窗口成功后通知
				//newEvent["value"]["calltype"] = "video";
				//newEvent["value"]["src"] = "";
				//newEvent["value"]["direction"] = "out";
				//newEvent["value"]["ptz"] = event["value"]["ptz"];
				//newEvent["value"]["mute"] = event["value"]["mute"];
				//newEvent["eventName"] = "OnCallConnect";
				//this.EVENT_LIST["VoiceNotify"]["OnCallConnect"](newEvent);
				break;
			case "3008":
                var videoMediaInfo = cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]];
                var delay = 0;
                if (undefined === videoMediaInfo || undefined === videoMediaInfo["isCreateWindowComplete"] || videoMediaInfo["isCreateWindowComplete"] != true) {
                    delay = 5000;
                }
                // 	DTS2023030315978 3008消息与196610消息时序问题导致弹窗关不掉
                setTimeout(() => {
                    // 对端挂起事件
                    //停止振铃
                    var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
                        parseInt(value["cid"]), reqId
                    );

                    cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

                    // 删除音频媒体通道
                    var param =
                        "{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
                        .format(
                            parseInt(value["cid"]), reqId
                        )

                    cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

                    // 删除视频媒体通道
                    var bIsNeedDestroyWindow = false;
                    var windowInfo = cloudICP.util.callStatusMgr.windowInfos[value["cid"]]; //视频点呼被动接听 、被动视频入会
                    var windowToolbarInfo = cloudICP.util.callStatusMgr.windowInfos[value["callee"]]; //视频点呼、视频监控、主动入会
                    var videoMediaWorkingInfo = cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]];
                    var iuserWindowId = 0;
                    if (undefined != windowInfo) {
                        delete cloudICP.util.callStatusMgr.windowInfos[value["cid"]];
                    }
                    if (undefined != windowToolbarInfo) {
                        delete cloudICP.util.callStatusMgr.windowInfos[value["callee"]];
                    }
                    if (undefined != videoMediaWorkingInfo) { //该视频窗口有业务正在使用
                        if (undefined != videoMediaWorkingInfo["value"]["userWindowId"]) {
                            iuserWindowId = videoMediaWorkingInfo["value"]["userWindowId"];
                        }
                        if (undefined != videoMediaWorkingInfo["value"]["isNeedDestroyWindow"] && true ==
                            videoMediaWorkingInfo["value"]["isNeedDestroyWindow"]) { //用户需要释放该视频窗口
                            bIsNeedDestroyWindow = true;
                            delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[iuserWindowId.toString()];
                        }
                        delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]];
                    }
                    if (true == bIsNeedDestroyWindow) {
                        param =
                            "{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}, \"isNeedDestroyWindow\":{1}, \"userWindowId\":{2}}}"
                            .format(
                                parseInt(value["cid"]), bIsNeedDestroyWindow, iuserWindowId
                            );
                    } else if (0 != iuserWindowId) {
                        param =
                            "{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}, \"userWindowId\":{1}}}"
                            .format(
                                parseInt(value["cid"]), iuserWindowId
                            );
                    } else {
                        param =
                            "{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}}}"
                            .format(
                                parseInt(value["cid"])
                            );
                    }
                    cloudICP.util.CleanThisRepeatsMonitor(value["caller"], value["callee"]);

                    cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

                    // 删除内嵌窗口cid
                    cloudICP.util.deleteWssflowModeCid(value["cid"]);
					cloudICP.util.callStatusMgr.delWssflowDemoMap(value["cid"]);

                    var callStatusMgr = cloudICP.util.callStatusMgr;
                    var call = callStatusMgr.allCalls[event["value"]["cid"]];
                    if (call && call.callType == callStatusMgr.MONITOR) {
                        callStatusMgr.updateCallStatus(callStatusMgr.MONITOR, callStatusMgr.RELEASE, event);


                        newEvent["value"]["calltype"] = "monitor";
                        newEvent["value"]["releasetype"] = "other";
                        newEvent["value"]["src"] = "";
                        newEvent["value"]["direction"] = "out";
                        newEvent["eventName"] = "OnCallRelease";
                        this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
                        sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
						cloudICP.util.callStatusMgr.delMediaModeInfo(event["value"]);
                        return;
                    } else if (call && call.callType == callStatusMgr.VIDEOCONF) {
                        callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.RELEASE, event);

                        newEvent["value"]["confId"] = call["confId"];
                        newEvent["value"]["isVideo"] = "true";
                        newEvent["eventName"] = "OnConfRelease";
                        this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"].event = newEvent;
                        sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"]);

                        setTimeout(function() {
                            var mconfInfo = cloudICP.util.callStatusMgr.confMgr[call["confId"]];
                            if (mconfInfo != null && ((mconfInfo["isEnding"] != undefined && mconfInfo[
                                    "isEnding"] == true) || (mconfInfo["value"]["confStatus"]["status"] ==
                                    "End"))) {
                                return;
                            }

                            if (cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] == undefined || cloudICP
                                .util.callStatusMgr.confSubMgr[call["confId"]] != true) {
                                return;
                            }

                            var ajaxCfg = {
                                "type": "POST",
                                "url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
                                "data": {
                                    "opt": "unsub",
                                    "reqId": reqId,
                                    "confId": call["confId"]
                                },
                                "callback": function(data) {
                                    if (data["rsp"] != "0") {
                                        var retEvent = {
                                            "eventName": "OnUnsubscribeConfFailure",
                                            "rsp": data["rsp"],
                                        }

                                        cloudICP.dispatch.event.EVENT_LIST["PhoneConfNotify"][
                                            "OnUnsubscribeConfFailure"
                                        ].event = newEvent;
                                        sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"]);
                                    }
                                }
                            }
                            cloudICP.util.ajax(ajaxCfg);
                        }, 500);
                        return;
                    }

                    var callStatusMgr = cloudICP.util.callStatusMgr;
                    callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.RELEASE, event);

                    newEvent["value"]["calltype"] = "video";
                    newEvent["value"]["releasetype"] = "other";
                    newEvent["value"]["src"] = "";
                    newEvent["value"]["direction"] = "out";
                    newEvent["eventName"] = "OnCallRelease";
                    this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
                    sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					cloudICP.util.callStatusMgr.delMediaModeInfo(event["value"]);
                }, delay);
				break;
			case "3009":
				// DC挂断事件
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				var tmpVideoMediaInfo;
				if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]) {
					tmpVideoMediaInfo = cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]];
				}
				//停止振铃
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 删除音频媒体通道
				var param =
					"{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					)

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				var vidoReleaseTag = false;
				if (undefined != tmpVideoMediaInfo && undefined != tmpVideoMediaInfo["value"]
					&& undefined != tmpVideoMediaInfo["value"]["releaseTag"]) {
					vidoReleaseTag = tmpVideoMediaInfo["value"]["releaseTag"];
				}

				// 删除视频媒体通道
				var bIsNeedDestroyWindow = false;
				var windowInfo = cloudICP.util.callStatusMgr.windowInfos[value["cid"]]; //视频点呼被动接听 、被动视频入会
				var windowToolbarInfo = cloudICP.util.callStatusMgr.windowInfos[value["callee"]]; //视频点呼、视频监控、主动入会
				var videoMediaWorkingInfo = cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]];
				var iuserWindowId = 0;
				if (undefined != windowInfo) {
					delete cloudICP.util.callStatusMgr.windowInfos[value["cid"]];
				}
				if (undefined != windowToolbarInfo) {
					delete cloudICP.util.callStatusMgr.windowInfos[value["callee"]];
				}
				if (undefined != videoMediaWorkingInfo) { //该视频窗口有业务正在使用
					if (undefined != videoMediaWorkingInfo["value"]["userWindowId"]) {
						iuserWindowId = videoMediaWorkingInfo["value"]["userWindowId"];
					}
					if (undefined != videoMediaWorkingInfo["value"]["isNeedDestroyWindow"] && true ==
						videoMediaWorkingInfo["value"]["isNeedDestroyWindow"]) { //用户需要释放该视频窗口
						bIsNeedDestroyWindow = true;
						delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[iuserWindowId.toString()];
					}
					delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]];
				}
				if (true == bIsNeedDestroyWindow) {
					param =
						"{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}, \"isNeedDestroyWindow\":{1}, \"userWindowId\":{2}}}"
						.format(
							parseInt(value["cid"]), bIsNeedDestroyWindow, iuserWindowId
						);
				} else if (0 != iuserWindowId) {
					param =
						"{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}, \"userWindowId\":{1}}}"
						.format(
							parseInt(value["cid"]), iuserWindowId
						);
				} else {
					param =
						"{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}}}"
						.format(
							parseInt(value["cid"])
						);
				}
				cloudICP.util.CleanThisRepeatsMonitor(value["caller"], value["callee"]);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				if (vidoReleaseTag) { // 创建流已经释放场景不需要重复通知释放消息
					break;
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.MONITOR) {
					callStatusMgr.updateCallStatus(callStatusMgr.MONITOR, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "monitor";
					newEvent["value"]["releasetype"] = "self";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "out";
					newEvent["eventName"] = "OnCallRelease";
					this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					cloudICP.util.callStatusMgr.delMediaModeInfo(event["value"]);
					break;
				} else if (call && call.callType == callStatusMgr.VIDEOCONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.RELEASE, event);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "true";
					newEvent["eventName"] = "OnConfRelease";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"]);

					setTimeout(function() {
						var mconfInfo = cloudICP.util.callStatusMgr.confMgr[call["confId"]];
						if (mconfInfo != null && ((mconfInfo["isEnding"] != undefined && mconfInfo[
								"isEnding"] == true) || (mconfInfo["value"]["confStatus"]["status"] ==
								"End"))) {
							return;
						}

						if (cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] == undefined || cloudICP
							.util.callStatusMgr.confSubMgr[call["confId"]] != true) {
							return;
						}

						var ajaxCfg = {
							"type": "POST",
							"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
							"data": {
								"opt": "unsub",
								"reqId": reqId,
								"confId": call["confId"]
							},
							"callback": function(data) {
								if (data["rsp"] != "0") {
									var retEvent = {
										"eventName": "OnUnsubscribeConfFailure",
										"rsp": data["rsp"],
									}

									cloudICP.dispatch.event.EVENT_LIST["PhoneConfNotify"][
										"OnUnsubscribeConfFailure"
									].event = retEvent;
									sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"]);
								}
							}
						}
						cloudICP.util.ajax(ajaxCfg);
					}, 500);


					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.RELEASE, event);

				newEvent["value"]["calltype"] = "video";
				newEvent["value"]["releasetype"] = "self";
				newEvent["value"]["src"] = "";
				newEvent["value"]["direction"] = "out";
				newEvent["eventName"] = "OnCallRelease";
				this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
				cloudICP.util.callStatusMgr.delMediaModeInfo(event["value"]);
				break;
			case "3013":
			case "3015":
			case "3014":
			case "3016": // 被叫号码不存在
			case "3017": // 对端服务不能使用
			case "3021": // 对端不在线
			case "3045": // 对端不支持该操作，可能是没有权限或者不支持该分辨率
			case "3044": // 本端不支持该操作，可能是没有权限
			case "3048": // 对端没有权限
			case "3022": // 对端没有权限
			case "2033": // 对端断电
				// 呼出失败事件

				//停止振铃
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);
				var windowInfo = cloudICP.util.callStatusMgr.windowInfos[value["cid"]]; //视频点呼被动接听 、被动视频入会
				if (undefined != windowInfo) {
					delete cloudICP.util.callStatusMgr.windowInfos[value["cid"]];
				}
				if (undefined != value["callee"]) {
					var windowToolbarInfo = cloudICP.util.callStatusMgr.windowInfos[value[
						"callee"]]; //视频点呼、视频监控、主动入会
					if (undefined != windowToolbarInfo) {
						delete cloudICP.util.callStatusMgr.windowInfos[value["callee"]];
					}
				}
				cloudICP.util.CleanThisRepeatsMonitor(value["caller"], value["callee"]);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				if (rsp == "3013") {
					//对端正在通话中
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":3, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "3014" || rsp == "3048" || rsp == "3021") {
					//对端无法接通
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":4, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "3016") {
					//对端号码不存在
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":5, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "") {
					//本段没有权限
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":6, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "") {
					//对端没有权限
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":7, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "3015") {
					//对端无应答
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":8, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "") {
					//对端正忙
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":9, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "3044" || rsp == "3022" || rsp == "2033" || "3045") {
					//拨打业务无法支持
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":10, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				}


				var callStatusMgr = cloudICP.util.callStatusMgr;
				if (confInfo != undefined) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.RELEASE, event, confInfo);

					newEvent["value"]["confId"] = confInfo["confInternalId"];
					newEvent["value"]["isVideo"] = "true";
					newEvent["eventName"] = "OnConfFailure";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfFailure"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfFailure"]);
					break;
				}

				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.MONITOR) {
					callStatusMgr.updateCallStatus(callStatusMgr.MONITOR, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "monitor";
					newEvent["eventName"] = "OnDialOutFailure";
					this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"]);
					break;
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.RELEASE, event);

				newEvent["value"]["calltype"] = "video";
				newEvent["eventName"] = "OnDialOutFailure";
				this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"]);
				break;
			case "3053": //mrs notify
				if (call && call.callType == callStatusMgr.VIDEOCONF && confInfo != undefined) {
					newEvent["value"]["confId"] = confInfo["confInternalId"];
				}
				newEvent["value"]["downloadurl"] = event["value"]["downloadurl"];
				newEvent["value"]["playurl"] = event["value"]["playurl"];
				newEvent["value"]["recordid"] = event["value"]["recordid"];
				newEvent["eventName"] = "MRSInfoNotify";
				this.EVENT_LIST["VoiceNotify"]["MRSInfoNotify"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["MRSInfoNotify"]);
				break;
			default:
				break;
		}
	} else {
		switch (rsp) {
			case "3002":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				newEvent["value"]["caller"] = from;
				var param =
					"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":1, \"delay\":0, \"duration\": 0, \"playcount\":0}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				if (confInfo != undefined) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.RINGING, event, confInfo);

					newEvent["value"]["confId"] = confInfo["confInternalId"];
					newEvent["value"]["isVideo"] = "true";
					newEvent["eventName"] = "OnConfDialInRinging";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfDialInRinging"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfDialInRinging"]);
					break;
				}
				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.RINGING, event);

				// 视频响铃事件
				newEvent["value"]["calltype"] = "video";
				newEvent["value"]["src"] = "";
				newEvent["eventName"] = "OnDialInRinging";
				this.EVENT_LIST["VoiceNotify"]["OnDialInRinging"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialInRinging"]);
				break;
				// 视频点呼被acked事件
			case "3006":
				// 停止振铃
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}

				newEvent["value"]["caller"] = from;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 建立音频媒体通道
				if (value["local_audio"] != "0.0.0.0,0" && value["server_audio"] != "0.0.0.0,0") {
					let localArr = value["local_audio"].split(",");
					let serverArr = value["server_audio"].split(",");

					var user = cloudICP.userInfo["isdn"];
					var peerUser = value["callee"];
					if (user == peerUser) {
						peerUser = value["caller"];
					}

					var callNumber;
					if (event["value"]["callee"] != cloudICP.userInfo["isdn"]) {
						callNumber = event["value"]["callee"]
					} else {
						callNumber = event["value"]["caller"];
					}

					var param =
						"{\"description\":\"msp_create_audio_channel\", \"cmd\": 65537, \"param\":{\"resID\":{0}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"number\": \"{7}\", \"user\":\"{8}\", \"peerUser\":\"{9}\"}}, \"reqId\": \"{10}\"}"
						.format(
							parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0], serverArr[
								1], parseInt(value["audio"]), parseInt(value["audio_ssrc"]), parseInt(callNumber),
							user,
							peerUser,
							reqId
						)

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				}

				// 建立视频媒体通道
				if (value["local_video"] != "0.0.0.0,0" && value["server_video"] != "0.0.0.0,0") {
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]] = event;

					var windowInfo = cloudICP.util.callStatusMgr.windowInfos[value["cid"]]; //视频点呼被动接听 、被动视频入会
					var windowToolbarInfo = cloudICP.util.callStatusMgr.windowInfos[value[
						"callee"]]; //视频点呼、视频监控、主动入会
					var iuserWindowId = 0;
					var ishowToolbar = 1;
					var ibuttonIDs = 0;
					var bHide = false;
					var bEnableDrag = true;
					var iWidth = 640;
					var iHeight = 480;
					var iPosX = 400;
					var iPosY = 400;
					var fmtType = "TIN";
					var windowMode = cloudICP.config["mode"];
					var videoConfLocalWindow = null;
					var videoResizeMode = cloudICP.config["videoResizeMode"];
					if ((undefined != value["confId"]) && (undefined != cloudICP.config["videoConfLocalWindow"])) {
						videoConfLocalWindow = cloudICP.config["videoConfLocalWindow"];
					}
					if (!!windowToolbarInfo) {
						if (undefined != windowToolbarInfo["userWindowId"]) {
							iuserWindowId = parseInt(windowToolbarInfo["userWindowId"]);
							cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["userWindowId"] =
								iuserWindowId;
							newEvent["value"]["userWindowId"] = iuserWindowId;
						}
						if (undefined != windowToolbarInfo["showToolbar"]) {
							ishowToolbar = parseInt(windowToolbarInfo["showToolbar"]);
						}
						if (undefined != windowToolbarInfo["buttonIDs"]) {
							ibuttonIDs = parseInt(windowToolbarInfo["buttonIDs"]);
						}
						if (undefined != windowToolbarInfo["mode"]) {
							windowMode = windowToolbarInfo["mode"].toString();
						}
						if (undefined != windowToolbarInfo["fmtType"]) {
							fmtType = windowToolbarInfo["fmtType"].toString();
						}
						if (undefined != windowToolbarInfo["width"]) {
							iWidth = parseInt(windowToolbarInfo["width"]);
						}
						if (undefined != windowToolbarInfo["height"]) {
							iHeight = parseInt(windowToolbarInfo["height"]);
						}
						if (undefined != windowToolbarInfo["posX"]) {
							iPosX = parseInt(windowToolbarInfo["posX"]);
						}
						if (undefined != windowToolbarInfo["posY"]) {
							iPosY = parseInt(windowToolbarInfo["posY"]);
						}
						if (undefined != windowToolbarInfo["hide"]) {
							bHide = Boolean(windowToolbarInfo["hide"]);
						}
						if (undefined != windowToolbarInfo["enableDrag"]) {
							bEnableDrag = Boolean(windowToolbarInfo["enableDrag"]);
						}
						if (undefined != windowToolbarInfo["videoConfLocalWindow"]) {
							videoConfLocalWindow = windowToolbarInfo["videoConfLocalWindow"];
						}
					}
					if ((!!windowInfo) && (!!windowInfo["iCid"])) {
						if (undefined != windowInfo["userWindowId"]) {
							iuserWindowId = parseInt(windowInfo["userWindowId"]);
							cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["userWindowId"] =
								iuserWindowId;
							newEvent["value"]["userWindowId"] = iuserWindowId;
						}
						if (undefined != windowInfo["showToolbar"]) {
							ishowToolbar = parseInt(windowInfo["showToolbar"]);
						}
						if (undefined != windowInfo["buttonIDs"]) {
							ibuttonIDs = parseInt(windowInfo["buttonIDs"]);
						}
						if (undefined != windowInfo["mode"]) {
							windowMode = windowInfo["mode"].toString();
						}
						if (undefined != windowInfo["fmtType"]) {
							fmtType = windowInfo["fmtType"].toString();
						}
						if (undefined != windowInfo["width"]) {
							iWidth = parseInt(windowInfo["width"]);
						}
						if (undefined != windowInfo["height"]) {
							iHeight = parseInt(windowInfo["height"]);
						}
						if (undefined != windowInfo["posX"]) {
							iPosX = parseInt(windowInfo["posX"]);
						}
						if (undefined != windowInfo["posY"]) {
							iPosY = parseInt(windowInfo["posY"]);
						}
						if (undefined != windowInfo["videoConfLocalWindow"]) {
							videoConfLocalWindow = windowInfo["videoConfLocalWindow"];
						}
						if (undefined != windowInfo["hide"]) {
							bHide = Boolean(windowInfo["hide"]);
						}
						if (undefined != windowInfo["enableDrag"]) {
							bEnableDrag = Boolean(windowInfo["enableDrag"]);
						}
						if (undefined != windowInfo["videoResizeMode"]) {
							videoResizeMode = windowInfo["videoResizeMode"];
						}
					}

					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["hide"] = bHide;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["enableDrag"] = bEnableDrag;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["showToolbar"] = ishowToolbar;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["buttonIDs"] = ibuttonIDs;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["width"] = iWidth;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["height"] = iHeight;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["posX"] = iPosX;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["posY"] = iPosY;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["winMode"] = windowMode;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["fmtType"] = fmtType;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["direction"] = "in";
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["rsp"] = event["rsp"];
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["videoConfLocalWindow"] = videoConfLocalWindow;
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]]["value"]["videoResizeMode"] = videoResizeMode;

					if (0 != iuserWindowId) {
						param =
							"{\"description\":\"msp_create_video_window\", \"cmd\": 196610, \"param\":{\"resID\":{0}, \"windowName\": \"{1}\", \"mode\": \"{2}\", \"width\": {3}, \"height\": {4}, \"x\": {5}, \"y\":{6}, \"hide\": {7}, \"enableDrag\":{8}, \"scrollX\": {9}, \"scrollY\":{10}, \"showToolbar\": {11}, \"buttonIDs\": {12}, \"userWindowId\": {13}, \"videoResizeMode\": {15} }, \"reqId\":\"{14}\" }"
							.format(
								parseInt(value["cid"]), parseInt(value["cid"]), windowMode, iWidth, iHeight, iPosX,
								iPosY, bHide, bEnableDrag, 0, 0, ishowToolbar, ibuttonIDs, iuserWindowId, reqId, videoResizeMode
							);
					} else {
						param =
							"{\"description\":\"msp_create_video_window\", \"cmd\": 196610, \"param\":{\"resID\":{0}, \"windowName\": \"{1}\", \"mode\": \"{2}\", \"width\": {3}, \"height\": {4}, \"x\": {5}, \"y\":{6}, \"hide\": {7}, \"enableDrag\":{8}, \"scrollX\": {9}, \"scrollY\":{10}, \"showToolbar\": {11}, \"buttonIDs\": {12}, \"videoResizeMode\": {14} }, \"reqId\":\"{13}\" }"
							.format(
								parseInt(value["cid"]), parseInt(value["cid"]), windowMode, iWidth, iHeight, iPosX,
								iPosY, bHide, bEnableDrag, 0, 0, ishowToolbar, ibuttonIDs, reqId, videoResizeMode
							);
					}
					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true); //modify_by_caohui
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[value["cid"]];
				if (call && call.callType == callStatusMgr.VIDEOCONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.CONNECTING, event);
					callStatusMgr.holdCall(value["cid"], reqId);
					break; //暂时不通知上层，等创建窗口成功后通知
				} else if (call && call.callType == callStatusMgr.VIDEODISPATCH) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEODISPATCH, callStatusMgr.CONNECTING, event);
					break; //暂时不通知上层，等创建窗口成功后通知
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.CONNECTING, event);
				callStatusMgr.holdCall(value["cid"], reqId);

				//newEvent["value"]["calltype"] = "video";
				//newEvent["value"]["src"] = "";
				//newEvent["value"]["direction"] = "in";
				//newEvent["value"]["ptz"] = value["ptz"];
				//newEvent["value"]["mute"] = value["mute"];
				//newEvent["eventName"] = "OnCallConnect";
				//this.EVENT_LIST["VoiceNotify"]["OnCallConnect"](newEvent);
				break;
			case "3009":
				// DC挂断事件
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				//停止振铃
				newEvent["value"]["caller"] = from;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 删除音频媒体通道
				var param =
					"{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					)

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 删除视频媒体通道
				var bIsNeedDestroyWindow = false;
				var windowInfo = cloudICP.util.callStatusMgr.windowInfos[value["cid"]]; //视频点呼被动接听 、被动视频入会
				var windowToolbarInfo = cloudICP.util.callStatusMgr.windowInfos[value["callee"]]; //视频点呼、视频监控、主动入会
				var videoMediaWorkingInfo = cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]];
				var iuserWindowId = 0;
				if (undefined != windowInfo) {
					delete cloudICP.util.callStatusMgr.windowInfos[value["cid"]];
				}
				if (undefined != windowToolbarInfo) {
					delete cloudICP.util.callStatusMgr.windowInfos[value["callee"]];
				}
				if (undefined != videoMediaWorkingInfo) { //该视频窗口有业务正在使用
					if (undefined != videoMediaWorkingInfo["value"]["userWindowId"]) {
						iuserWindowId = videoMediaWorkingInfo["value"]["userWindowId"];
					}
					if (undefined != videoMediaWorkingInfo["value"]["isNeedDestroyWindow"] && true ==
						videoMediaWorkingInfo["value"]["isNeedDestroyWindow"]) { //用户需要释放该视频窗口
						bIsNeedDestroyWindow = true;
						delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[iuserWindowId.toString()];
					}
					delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]];
				}
				if (true == bIsNeedDestroyWindow) {
					param =
						"{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}, \"isNeedDestroyWindow\":{1}, \"userWindowId\":{2}}}"
						.format(
							parseInt(value["cid"]), bIsNeedDestroyWindow, iuserWindowId
						);
				} else if (0 != iuserWindowId) {
					param =
						"{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}, \"userWindowId\":{1}}}"
						.format(
							parseInt(value["cid"]), iuserWindowId
						);
				} else {
					param =
						"{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}}}"
						.format(
							parseInt(value["cid"])
						);
				}
				cloudICP.util.CleanThisRepeatsMonitor(value["caller"], value["callee"]);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VIDEOCONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.RELEASE, event);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "true";
					newEvent["eventName"] = "OnConfRelease";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"]);

					setTimeout(function() {
						var mconfInfo = cloudICP.util.callStatusMgr.confMgr[call["confId"]];
						if (mconfInfo != null && ((mconfInfo["isEnding"] != undefined && mconfInfo[
								"isEnding"] == true) || (mconfInfo["value"]["confStatus"]["status"] ==
								"End"))) {
							return;
						}
						if (cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] == undefined || cloudICP
							.util.callStatusMgr.confSubMgr[call["confId"]] != true) {
							return;
						}
						var ajaxCfg = {
							"type": "POST",
							"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
							"data": {
								"opt": "unsub",
								"reqId": reqId,
								"confId": call["confId"]
							},
							"callback": function(data) {
								if (data["rsp"] != "0") {
									var retEvent = {
										"eventName": "OnUnsubscribeConfFailure",
										"rsp": data["rsp"],
									}

									cloudICP.dispatch.event.EVENT_LIST["PhoneConfNotify"][
										"OnUnsubscribeConfFailure"
									].event = retEvent;
									sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"]);
								}
							}
						}
						cloudICP.util.ajax(ajaxCfg);
					}, 500);
					break;
				} else if (call && call.callType == callStatusMgr.VIDEODISPATCH) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEODISPATCH, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "dispatch";
					newEvent["value"]["direction"] = "in";
					newEvent["value"]["releasetype"] = "self";
					newEvent["value"]["src"] = event["value"]["uri"];
					newEvent["eventName"] = "OnCallRelease";
					this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					break;
				}
				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.RELEASE, event);

				newEvent["value"]["calltype"] = "video";
				newEvent["value"]["direction"] = "in";
				newEvent["value"]["releasetype"] = "self";
				newEvent["value"]["src"] = "";
				newEvent["eventName"] = "OnCallRelease";
				this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
				cloudICP.util.callStatusMgr.delMediaModeInfo(event["value"]);
				break;
			case "3008":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				cloudICP.util.callStatusMgr.delWssflowDemoMap(value["cid"]);
			case "3010":
			case "3013":
				// 对端拒接事件
				//停止振铃
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				newEvent["value"]["caller"] = from;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);
				var windowInfo = cloudICP.util.callStatusMgr.windowInfos[value["cid"]]; //视频点呼被动接听 、被动视频入会
				if (undefined != windowInfo) {
					delete cloudICP.util.callStatusMgr.windowInfos[value["cid"]];
				}

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 删除音频媒体通道
				var param =
					"{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					)

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 删除视频媒体通道
				var bIsNeedDestroyWindow = false;
				var windowInfo = cloudICP.util.callStatusMgr.windowInfos[value["cid"]]; //视频点呼被动接听 、被动视频入会
				var windowToolbarInfo = cloudICP.util.callStatusMgr.windowInfos[value["callee"]]; //视频点呼、视频监控、主动入会
				var videoMediaWorkingInfo = cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]];
				var iuserWindowId = 0;
				if (undefined != windowInfo) {
					delete cloudICP.util.callStatusMgr.windowInfos[value["cid"]];
				}
				if (undefined != windowToolbarInfo) {
					delete cloudICP.util.callStatusMgr.windowInfos[value["callee"]];
				}
				if (undefined != videoMediaWorkingInfo) { //该视频窗口有业务正在使用
					if (undefined != videoMediaWorkingInfo["value"]["userWindowId"]) {
						iuserWindowId = videoMediaWorkingInfo["value"]["userWindowId"];
					}
					if (undefined != videoMediaWorkingInfo["value"]["isNeedDestroyWindow"] && true ==
						videoMediaWorkingInfo["value"]["isNeedDestroyWindow"]) { //用户需要释放该视频窗口
						bIsNeedDestroyWindow = true;
						delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[iuserWindowId.toString()];
					}
					delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[value["cid"]];
				}
				if (true == bIsNeedDestroyWindow) {
					param =
						"{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}, \"isNeedDestroyWindow\":{1}, \"userWindowId\":{2}}}"
						.format(
							parseInt(value["cid"]), bIsNeedDestroyWindow, iuserWindowId
						);
				} else if (0 != iuserWindowId) {
					param =
						"{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}, \"userWindowId\":{1}}}"
						.format(
							parseInt(value["cid"]), iuserWindowId
						);
				} else {
					param =
						"{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}}}"
						.format(
							parseInt(value["cid"])
						);
				}
				cloudICP.util.CleanThisRepeatsMonitor(value["caller"], value["callee"]);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VIDEOCONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.RELEASE, event);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "true";
					newEvent["eventName"] = "OnConfRelease";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"]);

					setTimeout(function() {
						var mconfInfo = cloudICP.util.callStatusMgr.confMgr[call["confId"]];
						if (mconfInfo != null && ((mconfInfo["isEnding"] != undefined && mconfInfo[
								"isEnding"] == true) || (mconfInfo["value"]["confStatus"]["status"] ==
								"End"))) {
							return;
						}
						if (cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] == undefined || cloudICP
							.util.callStatusMgr.confSubMgr[call["confId"]] != true) {
							return;
						}
						var ajaxCfg = {
							"type": "POST",
							"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
							"data": {
								"opt": "unsub",
								"reqId": reqId,
								"confId": call["confId"]
							},
							"callback": function(data) {
								if (data["rsp"] != "0") {
									var retEvent = {
										"eventName": "OnUnsubscribeConfFailure",
										"rsp": data["rsp"],
									}

									cloudICP.dispatch.event.EVENT_LIST["PhoneConfNotify"][
										"OnUnsubscribeConfFailure"
									].event = retEvent;
									sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"]);
								}
							}
						}
						cloudICP.util.ajax(ajaxCfg);
					}, 500);
					break;
				} else if (call && call.callType == callStatusMgr.VIDEODISPATCH) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEODISPATCH, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "dispatch";
					newEvent["value"]["direction"] = "in";
					newEvent["value"]["releasetype"] = "other";
					newEvent["value"]["src"] = event["value"]["uri"];
					newEvent["eventName"] = "OnCallRelease";
					this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.RELEASE, event);

				newEvent["value"]["calltype"] = "video";
				newEvent["value"]["direction"] = "in";
				newEvent["value"]["releasetype"] = "other";
				newEvent["value"]["src"] = "";
				newEvent["eventName"] = "OnCallRelease";
				this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
				cloudICP.util.callStatusMgr.delMediaModeInfo(event["value"]);
				break;
			case "3011":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				newEvent["value"]["caller"] = from;
				var param =
					"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":1, \"delay\":0, \"duration\": 0, \"playcount\":0}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.VIDEODISPATCH, callStatusMgr.RINGING, event);

				newEvent["value"]["calltype"] = "dispatch";
				newEvent["eventName"] = "OnDialInRinging";
				newEvent["value"]["src"] = event["value"]["uri"];
				this.EVENT_LIST["VoiceNotify"]["OnDialInRinging"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialInRinging"]);
				break;
			case "3040":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				newEvent["value"]["caller"] = from;
				var callStatusMgr = cloudICP.util.callStatusMgr;
				if (confInfo != undefined) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEOCONF, callStatusMgr.NEW, event);
					break;
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VIDEODISPATCH) {
					callStatusMgr.updateCallStatus(callStatusMgr.VIDEODISPATCH, callStatusMgr.NEW, event);

					newEvent["value"]["calltype"] = "dispatch";
					newEvent["value"]["direction"] = "in";
					newEvent["value"]["src"] = event["value"]["uri"];
					newEvent["eventName"] = "OnConnectProceeding";
					this.EVENT_LIST["VoiceNotify"]["OnConnectProceeding"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnConnectProceeding"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.NEW, event);

				newEvent["value"]["calltype"] = "video";
				newEvent["value"]["direction"] = "in";
				newEvent["value"]["src"] = "";
				newEvent["eventName"] = "OnConnectProceeding";
				this.EVENT_LIST["VoiceNotify"]["OnConnectProceeding"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnConnectProceeding"]);
				break;
			case "3045":
				// 视频监控失败

				// 停止振铃
				newEvent["value"]["caller"] = from;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.MONITOR, callStatusMgr.RELEASE, event);

				newEvent["value"]["calltype"] = "monitor";
				newEvent["eventName"] = "OnDialOutFailure";
				this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"]);
				break;
			case "3053": //mrs notify
				if (call && call.callType == callStatusMgr.VIDEOCONF && confInfo != undefined) {
					newEvent["value"]["confId"] = confInfo["confInternalId"];
				}
				newEvent["value"]["downloadurl"] = event["value"]["downloadurl"];
				newEvent["value"]["playurl"] = event["value"]["playurl"];
				newEvent["value"]["recordid"] = event["value"]["recordid"];
				newEvent["eventName"] = "MRSInfoNotify";
				this.EVENT_LIST["VoiceNotify"]["MRSInfoNotify"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["MRSInfoNotify"]);
				break;
			default:
				break;
		}
	}
}

/**
 * 处理组呼通知
 */
ICPSDK_Dispatch_Event.prototype.delGroupCallNotify = function(event) {
	var reqId = event.reqId;
	if (reqId == null || reqId == undefined) {reqId = "0";}
	var value = event["value"];

	if (event["opt"] == "speaker") {
		if (event["rsp"] == "1001") {
			var retEvent = {
				"eventName": "OnTalkingGroupCallStart",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
					"cid": event["value"]["cid"],
					"from": event["value"]["from"],
					"speaker": event["value"]["speaker"]
				}
			}

			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallStart"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallStart"]);
		} else if (event["rsp"] == "1003") {
			// 组呼放权事件
			var retEvent = {
				"eventName": "OnTalkingGroupCallPTTSuccess",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
					"from": event["value"]["from"]
				}
			}

			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTSuccess"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTSuccess"]);
		} else if (event["rsp"] == "1006") {
			// 建立音频媒体通道
			if (value["local"] != "0.0.0.0,0" && value["server"] != "0.0.0.0,0") {
				let localArr = value["local"].split(",");
				let serverArr = value["server"].split(",");

				var user = cloudICP.userInfo["isdn"];
				var peerUser = value["callee"];
				if (user == peerUser) {
					peerUser = value["caller"];
				}

				var param =
					"{\"description\":\"msp_create_audio_channel\", \"cmd\": 65537, \"param\":{\"resID\":{0}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"number\": \"{7}\",\"user\":\"{8}\", \"peerUser\":\"{9}\"}}, \"reqId\": \"{10}\"}"
					.format(
						parseInt(event["value"]["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0],
						serverArr[1], parseInt(value["audio"]), parseInt(value["audio_ssrc"]), parseInt(event[
							"grpid"]), user, peerUser, reqId
					)

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				cloudICP.dispatch.group.current_groupcall_num += 1;
			}

			var callStatusMgr = cloudICP.util.callStatusMgr;
			callStatusMgr.updateCallStatus(callStatusMgr.GROUP, callStatusMgr.NEW, event);

			// 组呼开始事件
			var retEvent = {
				"eventName": "OnTalkingGroupCallPTTStart",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
					"cid": event["value"]["cid"],
					"from": event["value"]["from"],
					"speaker": event["value"]["speaker"]
				}
			}

			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTStart"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTStart"]);
			// 判断该群组是否被静音，如果在列表中则被静音
            if (cloudICP.util.includes(cloudICP.grpList, event["grpid"])) {
                var meidaSDKparam = "{\"description\":\"msp_quite_audio_channel\", \"cmd\": 65543, \"param\":{\"resID\": {0}, \"peerID\": {0}, \"quiet\":true}}".format(
                    parseInt(event["value"]["cid"]), parseInt(event["grpid"])
                );

                cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
		} else {
		if (cloudICP.grpVolumeList[event["grpid"]]) {
		    var meidaSDKparam = "{\"description\":\"msp_set_audio_Volume\", \"cmd\": 65545, \"param\":{\"resID\": {0}, \"peerID\": {1}, \"newVolume\": {2} }}".format(
		        parseInt(event["value"]["cid"]), parseInt(event["grpid"]), parseFloat(cloudICP.grpVolumeList[event["grpid"]])
		    );
		} else {
		    var meidaSDKparam = "{\"description\":\"msp_set_audio_Volume\", \"cmd\": 65545, \"param\":{\"resID\": {0}, \"peerID\": {1}, \"newVolume\": {2} }}".format(
		        parseInt(event["value"]["cid"]), parseInt(event["grpid"]), "50"
		    );
		}
		cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
		}

		// 添加grpid和cid对到grdList中，用于管理组呼静音
		cloudICP.grpCidList[event["grpid"]] = event["value"]["cid"];
		} else if (event["rsp"] == "1011") {
			// 组呼权限更新通知
			var retEvent = {
				"eventName": "OnTalkingGroupCallPTTNotify",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
					"name": event["value"]["name"],
					"speaker": event["value"]["speaker"]
				}
			}

			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTNotify"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTNotify"]);
		} else if (event["rsp"] == "1012") {
			// 无人占权事件
			var retEvent = {
				"eventName": "OnTalkingGroupCallPTTIdle",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
				}
			}

			if (cloudICP.dispatch.group.isCurrentPtt["grpid"] == event["grpid"]) {
				cloudICP.dispatch.group.isCurrentPtt = {
					"grpid": event["isdn"],
					"isPtt": false
				};
			}

			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTIdle"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTIdle"]);
		}
	} else if (event["opt"] = "state") {
		if (event["rsp"] == "1007") {
			// 退出组呼成功
			var callStatusMgr = cloudICP.util.callStatusMgr;
			var groupCid = callStatusMgr.groupCallMgr[event["grpid"]]

			// 删除音频媒体通道
			var param = "{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
				.format(
					parseInt(groupCid), reqId
				)

			cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

			cloudICP.dispatch.group.current_groupcall_num -= 1;
			if (cloudICP.dispatch.group.isCurrentPtt["grpid"] == event["grpid"]) {
				cloudICP.dispatch.group.isCurrentPtt = {
					"grpid": event["grpid"],
					"isPtt": false
				};
			}

			// 组呼关闭事件
			var retEvent = {
				"eventName": "OnTalkingGroupCallStop",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
				}
			}
			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallStop"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallStop"]);
		} else if (event["rsp"] == "1008") {
			// 删除音频媒体通道
			var callStatusMgr = cloudICP.util.callStatusMgr;
			var groupCid = callStatusMgr.groupCallMgr[event["grpid"]]

			var param = "{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
				.format(
					parseInt(groupCid), reqId
				)

			cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
			cloudICP.dispatch.group.current_groupcall_num -= 1;

			// 组呼关闭事件
			var retEvent = {
				"eventName": "OnTalkingGroupCallRelease",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
				}
			}
			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallRelease"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallRelease"]);
		// 删除grpCidList中对应的item
            	delete cloudICP.grpCidList[event["grpid"]];
		} else if (event["rsp"] == "1005") {
			// 组呼抢权失败事件
			var retEvent = {
				"eventName": "OnTalkingGroupCallPTTFailure",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
				}

			}
			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTFailure"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTFailure"]);
		} else if (event["rsp"] == "1017") {
			// 组呼发起失败事件
			var retEvent = {
				"eventName": "OnTalkingGroupCallFailure",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
				}
			}
			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallFailure"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallFailure"]);
		} else if (event["rsp"] == "1003") {
			// 组呼请求事件
			var retEvent = {
				"eventName": "OnTalkingGroupCallPTTSuccess",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
					"from": event["from"],
				}
			}

			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTSuccess"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupCallPTTSuccess"]);
		} else if (event["rsp"] == "1014") {
			// 紧急组呼通知
			var retEvent = {
				"eventName": "OnTalkingGourpEmergencyCallStart",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
					"name": event["value"]["name"],
					"speaker": event["value"]["speaker"],
				}
			}

			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGourpEmergencyCallStart"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGourpEmergencyCallStart"]);
		} else if (event["rsp"] == "1028") { //mrs notify
			var retEvent = {
				"eventName": "GroupMRSInfoNotify",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
					"cid": event["value"]["cid"],
					"downloadurl": event["value"]["downloadurl"],
					"playurl": event["value"]["playurl"],
					"recordid": event["value"]["recordid"]
				}
			};
			this.EVENT_LIST["GroupCallNotify"]["GroupMRSInfoNotify"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["GroupMRSInfoNotify"]);
		} else if (event["rsp"] == "1015") {
			// 普通组呼 切换为 紧急组呼
			var retEvent = {
				"eventName": "OnTalkingGroupEmergencyCallUpdate",
				"rsp": event["rsp"],
				"value": {
					"grpid": event["grpid"],
					"name": event["value"]["name"],
					"speaker": event["value"]["speaker"],
				}
			}

			this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupEmergencyCallUpdate"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnTalkingGroupEmergencyCallUpdate"]);
		}
	}
}

/**
 * 处理资源状态通知
 */
ICPSDK_Dispatch_Event.prototype.delResourceNotify = function(event) {
	var reqId = event.reqId;
	if (reqId == null || reqId == undefined) {reqId = "0";}
	var opt = event["opt"];
	var statusValue = event["statusvalue"];
	var statusType = event["statustype"];
	switch (statusType) {
		case "18":
			cloudICP.util.callStatusMgr.updateUserStatus(event);

			var retEvent = {
				"eventName": "OnUserStatusNotify",
				"list": [{
					"isdn": event["value"]["isdn"],
					"peerid": event["value"]["peerid"],
					"statustype": event["statustype"],
					"statusvalue": event["statusvalue"],
					"calltype": event["value"]["calltype"]
				}]
			};
			this.EVENT_LIST["VoiceNotify"]["OnUserStatusNotify"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnUserStatusNotify"]);
			break;
		case "20":
			if (statusValue == "4005") {
				var retEvent = {
					"eventName": "OnDiscreetlistenStart",
					"rsp": "0",
					"value": {
						"to": event["value"]["isdn"]
					}
				};
				this.EVENT_LIST["VoiceNotify"]["OnDiscreetlistenStart"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDiscreetlistenStart"]);
			} else if (statusValue == "4006") {
				var retEvent = {
					"eventName": "OnDiscreetlistenStop",
					"rsp": "0",
					"value": {
						"to": event["value"]["isdn"]
					}

				};
				this.EVENT_LIST["VoiceNotify"]["OnDiscreetlistenStop"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDiscreetlistenStop"]);
			}
			break;
		case "21":
			if (statusValue == "4026") {
				var retEvent = {
					"eventName": "OnTalkingGroupMemberChangeFailure",
					"rsp": event["value"]["cause"],
					"value": {
						"grpid": event["value"]["attaching"],
						"isdn": event["isdn"],
					}

				};
				this.EVENT_LIST["GroupNotify"]["OnTalkingGroupMemberChangeFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnTalkingGroupMemberChangeFailure"]);
			} else if (statusValue == "4024") {
				var retEvent = {
					"eventName": "OnDynamicGroupChangeSuccess",
					"rsp": event["value"]["cause"],
					"value": {
						"grpid": event["value"]["attaching"],
						"isdn": event["user"],
					}

				};
				this.EVENT_LIST["GroupNotify"]["OnDynamicGroupChangeSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnDynamicGroupChangeSuccess"]);
			}
			break;
		case "22":
			if (statusValue == "4021" || statusValue == "4022" || statusValue == "4023") {
				var retEvent = {
					"eventName": "OnVideoDispatchStatusNotify",
					"rsp": event["statusvalue"],
					"value": {
						"src": event["value"]["camid"],
						"peerid": event["value"]["peerid"],
					}

				};
				this.EVENT_LIST["VideoNotify"]["OnVideoDispatchStatusNotify"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VideoNotify"]["OnVideoDispatchStatusNotify"]);
			}
			break;
		case "24":
			if (statusValue == "4036" || statusValue == "4038" || statusValue == "4044" || statusValue == "4045") {
				var retEvent = {
					"eventName": "OnChangePatchGroupMemberFailure",
					"rsp": "0",
					"value": {
						"grpid": event["isdn"],
						"isdn": event["value"]["memberid"],
						"rsp": event["value"]["cause"],
						"opt": (statusValue == "4036" || statusValue == "4044") ? "create" : "delete"
					}

				};
				this.EVENT_LIST["GroupNotify"]["OnChangePatchGroupMemberFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnChangePatchGroupMemberFailure"]);
			}
			if (statusValue == "4035" || statusValue == "4037" ) {
				var retEvent = {
					"eventName": "OnChangePatchGroupMemberSucess",
					"rsp": "0",
					"value": {
						"grpid": event["isdn"],
						"isdn": event["value"]["memberid"],
						"rsp": event["value"]["cause"],
						"opt": statusValue == "4035" ? "create" : "delete"
					}
				};
				this.EVENT_LIST["GroupNotify"]["OnChangePatchGroupMemberSucess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnChangePatchGroupMemberSucess"]);
			}
			if (statusValue == "4031" || statusValue == "4033") {
				var retEvent = {
					"eventName": "OnPatchGroupOptSuccess",
					"rsp": "0",
					"value": {
						"grpid": event["isdn"],
						"rsp": event["value"]["cause"],
						"opt": statusValue == "4031" ? "create" : "delete"
					}

				};
				this.EVENT_LIST["GroupNotify"]["OnPatchGroupOptSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnPatchGroupOptSuccess"]);
			}
			if (statusValue == "4032" || statusValue == "4034" || statusValue == "4042" || statusValue == "4043") {
				//派接组创建失败
				var retEvent = {
					"eventName": "OnPatchGroupOptFailed",
					"rsp": "0",
					"value": {
						"grpid": event["isdn"],
						"rsp": event["value"]["cause"],
						"opt": (statusValue == "4032" || statusValue == "4042") ? "create" : "delete"
					}

				};
				this.EVENT_LIST["GroupNotify"]["OnPatchGroupOptFailed"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnPatchGroupOptFailed"]);
				return;
			}
			break;
		case "29":
			var retEvent = {
				"eventName": "OnVDMStatusNotify",
				"rsp": event["statusvalue"]
			};
			this.EVENT_LIST["MSPNotify"]["OnVDMStatusNotify"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnVDMStatusNotify"]);
			break;
		case "30":
			if (statusValue == "4090") {
				// 创会成功
				var retEvent = {
					"eventName": "OnCreateConfSuccess",
					"rsp": "0",
					"value": {
						"confId": event["value"]["confInternalId"],
						"passcode": event["value"]["passcode"],
						"unifiedAccessCode": event["value"]["unifiedAccessCode"],
						"isVideo": event["value"]["isVideo"],
						"url": event["value"]["url"]
					}
				};

				cloudICP.util.callStatusMgr.updateConfInfo(retEvent["value"], true);

			if (undefined != event["value"]["time"] && undefined != event["value"]["timeZone"]) {
				retEvent["value"]["time"] = event["value"]["time"];
				retEvent["value"]["timeZone"] = event["value"]["timeZone"];
				this.EVENT_LIST["PhoneConfNotify"]["OnCreateConfSuccess"].event = retEvent;

				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnCreateConfSuccess"]);
				break;
			} else {
				this.EVENT_LIST["PhoneConfNotify"]["OnCreateConfSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnCreateConfSuccess"]);
			}
				var strWindowinfo = cloudICP.util.callStatusMgr.windowInfos[2147483647];

				cloudICP.util.callStatusMgr.windowInfos[event["value"]["unifiedAccessCode"]] = strWindowinfo;
				var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"];
				var ajaxCfg = {
					"type": "POST",
					"url": url,
					"data": {
						"opt": "join",
						"param": {
							"confId": event["value"]["confInternalId"],
							"fmt": cloudICP.util.callStatusMgr.fmt,
							"video_format": cloudICP.config.cameraInfo.toString(),
							"audio_format": "1",
							"unifiedAccessCode": event["value"]["unifiedAccessCode"],
							"passcode": event["value"]["passcode"],
							"isVideo": event["value"]["isVideo"] == "1" ? "true" : "false",
							"reqId": reqId
						}
					},
					"callback": function(data) {
						cloudICP.util.log(undefined, { "logLevel": "info", "logMsg": "connectToSDKServer: WebSocket message received: joinConf: " + JSON.stringify(data) });
					}
				}
				cloudICP.util.ajax(ajaxCfg);
			} else if (statusValue == "4091") {
				// 创会失败
				var retEvent = {
					"eventName": "OnCreateConfFailure",
					"rsp": event["value"]["rsp"],
					"value": {
						"confId": event["value"]["confInternalId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnCreateConfFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnCreateConfFailure"]);
			}
			break;
		case "31":
			if (statusValue == "4090") {
				// 结束会议成功
				var retEvent = {
					"eventName": "OnEndConfSuccess",
					"rsp": "0",
					"value": {
						"confId": event["value"]["confId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnEndConfSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnEndConfSuccess"]);
			} else if (statusValue == "4091") {
				// 结束会议失败
				var retEvent = {
					"eventName": "OnEndConfFailure",
					"rsp": event["value"]["rsp"],
					"value": {
						"confId": event["value"]["confId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnEndConfFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnEndConfFailure"]);
			}
			break;
		case "32":
			if (statusValue == "4081") {
				var retEvent = {
					"eventName": "PushServerToken register to AAS fail",
					"rsp": event["statusvalue"],
				};
				this.EVENT_LIST["ResourceNotify"]["OnPushConnectFail"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["ResourceNotify"]["OnPushConnectFail"]);
			} else if (statusValue == "4082") {
				var retEvent = {
					"eventName": "Get PushServerToken fail",
					"rsp": event["statusvalue"],
				};
				this.EVENT_LIST["ResourceNotify"]["OnPushGetTokenFail"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["ResourceNotify"]["OnPushGetTokenFail"]);
			} else if (statusValue == "4083") {
				var retEvent = {
					"eventName": "PushServerToken register to AAS success",
					"rsp": event["statusvalue"],
				};
				this.EVENT_LIST["ResourceNotify"]["OnPushConnectSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["ResourceNotify"]["OnPushConnectSuccess"]);
			}

			break;

		case "34":
			var value = event["value"];
			if (statusValue == "4090") {
				var retEvent = {
					"eventName": "OnQueryConferenceByIdSuccess",
					"rsp": "0",
					"value": {
						"confExternalId": value["confExternalId"],
						"confInternalId": value["confInternalId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnQueryConferenceByIdSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConferenceByIdSuccess"]);
			} else if (statusValue == "4091") {
				var retEvent = {
					"eventName": "OnQueryConferenceByIdFailure",
					"rsp": event["value"]["rsp"]
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnQueryConferenceByIdFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConferenceByIdFailure"]);
			}
			break;
		case "35":
			var value = event["value"];

			if (value["msgType"] == "ConfMuteMember") {
				if (value["params"] == "mute") {
					if (statusValue == "4090") {
						var retEvent = {
							"eventName": "OnMuteConfMemberSuccess",
							"rsp": "0",
							"value": {
								"confId": value["confId"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnMuteConfMemberSuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnMuteConfMemberSuccess"]);
					} else if (statusValue == "4091") {
						var retEvent = {
							"eventName": "OnMuteConfMemberFailure",
							"rsp": event["value"]["rsp"],
							"value": {
								"confId": value["confId"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnMuteConfMemberFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnMuteConfMemberFailure"]);
					}
				} else if (value["params"] == "unmute") {
					if (statusValue == "4090") {
						var retEvent = {
							"eventName": "OnUnmuteConfMemberSuccess",
							"rsp": "0",
							"value": {
								"confId": value["confId"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnUnmuteConfMemberSuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnmuteConfMemberSuccess"]);
					} else if (statusValue == "4091") {
						var retEvent = {
							"eventName": "OnUnmuteConfMemberFailure",
							"rsp": event["value"]["rsp"],
							"value": {
								"confId": value["confId"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnUnmuteConfMemberFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnmuteConfMemberFailure"]);
					}
				}
			} else if (value["msgType"] == "SetFloor" || value["msgType"] == "ConfFreeMode") {
				var IsSpoken = "false";
				if (value["msgType"] == "SetFloor") {
					IsSpoken = "true"
				}
				if (value["params"] == "isSpoken") {
					if (statusValue == "4090") {
						var retEvent = {
							"eventName": "OnSetSpokenMemberSuccess",
							"rsp": "0",
							"value": {
								"confId": value["confId"],
								"isSpoken": IsSpoken
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnSetSpokenMemberSuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnSetSpokenMemberSuccess"]);
					} else if (statusValue == "4091") {
						var retEvent = {
							"eventName": "OnSetSpokenMemberFailure",
							"rsp": event["value"]["rsp"],
							"retMsg": event["value"]["retMsg"],
							"value": {
								"confId": value["confId"],
								"isSpoken": IsSpoken
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnSetSpokenMemberFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnSetSpokenMemberFailure"]);
					}
				}
			} else if (value["msgType"] == "ConfMuteConf") {
				if (value["params"] == "mute") {
					if (statusValue == "4090") {
						var retEvent = {
							"eventName": "OnMuteConfSuccess",
							"rsp": "0",
							"value": {
								"confId": value["confId"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnMuteConfSuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnMuteConfSuccess"]);
					} else if (statusValue == "4091") {
						var retEvent = {
							"eventName": "OnMuteConfFailure",
							"rsp": event["value"]["rsp"],
							"value": {
								"confId": value["confId"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnMuteConfFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnMuteConfFailure"]);
					}
					break;
				} else if (value["params"] == "unmute") {
					if (statusValue == "4090") {
						var retEvent = {
							"eventName": "OnUnmuteConfSuccess",
							"rsp": "0",
							"value": {
								"confId": value["confId"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnUnmuteConfSuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnmuteConfSuccess"]);
					} else if (statusValue == "4091") {
						var retEvent = {
							"eventName": "OnUnmuteConfFailure",
							"rsp": event["value"]["rsp"],
							"value": {
								"confId": value["confId"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnUnmuteConfFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnmuteConfFailure"]);
					}
					break;
				}
			} else if (value["msgType"] == "ConfBroadcastMixPicture") {
				if (statusValue == "4090") {
					var retEvent = {
						"eventName": "OnBroadcastMixPictureSuccess",
						"rsp": "0",
						"value": {
							"confId": value["confId"],
							"member": value["members"]
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnBroadcastMixPictureSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnBroadcastMixPictureSuccess"]);
				} else if (statusValue == "4091") {
					var retEvent = {
						"eventName": "OnBroadcastMixPictureFailure",
						"rsp": event["value"]["rsp"],
						"value": {
							"confId": value["confId"],
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnBroadcastMixPictureFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnBroadcastMixPictureFailure"]);
				}
			} else if (value["msgType"] == "ConfCancelBroadcastMixPicture") {
				if (statusValue == "4090") {
					var retEvent = {
						"eventName": "OnCancelBroadcastMixPictureSuccess",
						"rsp": "0",
						"value": {
							"confId": value["confId"],
							"member": value["members"]
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnCancelBroadcastMixPictureSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnCancelBroadcastMixPictureSuccess"]);
				} else if (statusValue == "4091") {
					var retEvent = {
						"eventName": "OnCancelBroadcastMixPictureFailure",
						"rsp": event["value"]["rsp"],
						"value": {
							"confId": value["confId"],
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnCancelBroadcastMixPictureFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnCancelBroadcastMixPictureFailure"]);
				}
			} else if (value["msgType"] == "ConfBroadcastPicture") {
				if (statusValue == "4090") {
					var retEvent = {
						"eventName": "OnBroadcastConfMemberSuccess",
						"rsp": "0",
						"value": {
							"confId": value["confId"],
							"member": value["members"]
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnBroadcastConfMemberSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnBroadcastConfMemberSuccess"]);
				} else if (statusValue == "4091") {
					var retEvent = {
						"eventName": "OnBroadcastConfMemberFailure",
						"rsp": event["value"]["rsp"],
						"value": {
							"confId": value["confId"],
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnBroadcastConfMemberFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnBroadcastConfMemberFailure"]);
				}
			} else if (value["msgType"] == "ConfCancelBroadcastPicture") {
				if (statusValue == "4090") {
					var retEvent = {
						"eventName": "OnCancelBroadcastConfMemberSuccess",
						"rsp": "0",
						"value": {
							"confId": value["confId"],
							"member": value["members"]
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnCancelBroadcastConfMemberSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnCancelBroadcastConfMemberSuccess"]);
				} else if (statusValue == "4091") {
					var retEvent = {
						"eventName": "OnCancelBroadcastConfMemberFailure",
						"rsp": event["value"]["rsp"],
						"value": {
							"confId": value["confId"],
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnCancelBroadcastConfMemberFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnCancelBroadcastConfMemberFailure"]);
				}
			} else if (value["msgType"] == "ConfWatchMember") {
				if (statusValue == "4090") {
					var retEvent = {
						"eventName": "OnWatchConfMemberSuccess",
						"rsp": "0",
						"value": {
							"confId": value["confId"],
							"member": value["members"]
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnWatchConfMemberSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnWatchConfMemberSuccess"]);
				} else if (statusValue == "4091") {
					var retEvent = {
						"eventName": "OnWatchConfMemberFailure",
						"rsp": event["value"]["rsp"],
						"value": {
							"confId": value["confId"],
							"member": value["members"]
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnWatchConfMemberFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnWatchConfMemberFailure"]);
				}
			} else if (value["msgType"] == "ConfWatchMixPicture") {
				if (statusValue == "4090") {
					var retEvent = {
						"eventName": "OnWatchMixPictureSuccess",
						"rsp": "0",
						"value": {
							"confId": value["confId"],
							"member": value["members"]
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnWatchMixPictureSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnWatchMixPictureSuccess"]);
				} else if (statusValue == "4091") {
					var retEvent = {
						"eventName": "OnWatchMixPictureFailure",
						"rsp": event["value"]["rsp"],
						"value": {
							"confId": value["confId"],
							"member": value["members"]
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnWatchMixPictureFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnWatchMixPictureFailure"]);
				}
			} else if (value["msgType"] == "ConfQueryConfListByAttendee") {
				if (statusValue == "4090") {
					var confInfos = [];
					var retEvent = {};
					if (!!value["confInfos"]) {
						for (var index = 0; index < value["confInfos"].length; index++) {
							confInfos.push({
								"confId": value["confInfos"][index]["confInternalId"],
								"isVideo": value["confInfos"][index]["isVideo"],
								"passcode": value["confInfos"][index]["passcode"],
								"unifiedAccessCode": value["confInfos"][index]["unifiedAccessCode"],
								"url": value["confInfos"][index]["url"],
							});

							delete value["confInfos"][index].confExternalId;
							var tmp = value["confInfos"][index]["confInternalId"];
							value["confInfos"][index]["confId"] = tmp;
							delete value["confInfos"][index].confInternalId;
						}
						retEvent = {
							"eventName": "OnQueryConfListByAttendeeResult",
							"rsp": "0",
							"value": {
								"confInfos": value["confInfos"],
								"totalNum" : value["totalNum"]
							}
						};

					} else {
						retEvent = {
							"eventName": "OnQueryConfListByAttendeeResult",
							"rsp": "0",
							"value": {
								"confInfos": []
							}
						};
					}
					this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfListByAttendeeResult"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfListByAttendeeResult"]);
				} else if (statusValue == "4091") {
					var retEvent = {
						"eventName": "OnQueryConfListByAttendeeResult",
						"rsp": "-1",
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfListByAttendeeResult"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfListByAttendeeResult"]);
				}
			} else if (value["msgType"] == "ConfAddMember") {
				if (statusValue == "4090") {
					var retEvent = {
						"eventName": "OnAddConfMembersSuccess",
						"rsp": "0",
						"value": {
							"confId": value["confId"],
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnAddConfMembersSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnAddConfMembersSuccess"]);
				} else if (statusValue == "4091") {
					var retEvent = {
						"eventName": "OnAddConfMembersFailure",
						"rsp": event["value"]["rsp"],
						"value": {
							"confId": value["confId"],
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnAddConfMembersFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnAddConfMembersFailure"]);
				}
			} else if (value["msgType"] == "SetChairman") {
				if (statusValue == "4090") {
					var retEvent = {
						"eventName": "OnConfSetChairmanSuccess",
						"rsp": "0",
						"value": {
							"confId": value["confId"],
							"chairman": value["chairman"]
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnConfSetChairmanSuccess"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfSetChairmanSuccess"]);
				} else if (statusValue == "4091") {
					var retEvent = {
						"eventName": "OnConfSetChairmanFailure",
						"rsp": event["value"]["rsp"],
						"retMsg": event["value"]["retMsg"],
						"value": {
							"confId": value["confId"],
							"chairman": value["chairman"]
						}
					};
					this.EVENT_LIST["PhoneConfNotify"]["OnConfSetChairmanFailure"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfSetChairmanFailure"]);
				}
            } else if (value["msgType"] == "AddressBook"){
					if (statusValue == "4090" && value["pageCount"] != 0) {
						var retEvent = {
							"eventName": "OnQueryVDCAddressBookSuccess",
							"rsp": "0",
							"userCount": value["userCount"],
							"pageCount": value["pageCount"],
                            "totalPages": value["TotalPages"],
                            "totalElements": value["TotalElements"],
							"value": {
								"list": value["list"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnQueryVDCAddressBookSuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryVDCAddressBookSuccess"]);
					}
					else if (statusValue == "4090" && value["pageCount"] == 0)
					{
						var retEvent = {
							"eventName": "OnQueryVDCAddressBookSuccess",
							"rsp": "0",
							"userCount": value["userCount"],
							"value": {
								"list": value["list"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnQueryVDCAddressBookSuccess"].event = retEvent;
                        sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryVDCAddressBookSuccess"]);
					}
					if (statusValue == "4091" && value["pageCount"] != 0) {
						var retEvent = {
							"eventName": "OnQueryVDCAddressBookFailure",
							"rsp": event["value"]["rsp"],
							"userCount": value["userCount"],
							"pageCount": value["pageCount"],
							"retMsg": event["value"]["retMsg"],
							"value": {
								"list": value["list"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnQueryVDCAddressBookFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryVDCAddressBookFailure"]);
					}
					else if (statusValue == "4091" && value["pageCount"] == 0) {
						var retEvent = {
							"eventName": "OnQueryVDCAddressBookFailure",
							"rsp": event["value"]["rsp"],
                            "userCount": value["userCount"],
							"pageCount": value["pageCount"],
							"retMsg": event["value"]["retMsg"],
							"value": {
								"list": value["list"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnQueryVDCAddressBookFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryVDCAddressBookFailure"]);
					}
				} else if (value["msgType"] == "queryConferenceList") {
					if (statusValue == "4090" && value["totalNum"] != 0) {
						var retEvent = {
							"eventName": "OnRecvConfListNotifySuccess",
							"rsp": event["value"]["rsp"],
							"totalNum": value["totalNum"],
							"value": {
								"list": value["conferences"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnRecvConfListNotifySuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnRecvConfListNotifySuccess"]);
					} else if (statusValue == "4090" && value["totalNum"] == 0) {
						var retEvent = {
							"eventName": "OnRecvConfListNotifySuccess",
							"rsp": event["value"]["rsp"],
							"totalNum": "0",
							"value": {
								"list": ""
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnRecvConfListNotifySuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnRecvConfListNotifySuccess"]);
					} else {
						var retEvent = {
							"eventName": "OnRecvConfListNotifyFailure",
							"rsp": event["value"]["rsp"],
							"totalNum": "0",
							"retMsg": event["value"]["retMsg"],
							"value": {
								"list": ""
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnRecvConfListNotifyFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnRecvConfListNotifyFailure"]);
					}
				}else if (value["msgType"] == "queryConferenceInfo") {
					if (statusValue == "4090" && value["totalNum"] != 0) {
						var retEvent = {
							"eventName": "OnRecvQueryConfInfoNotifySuccess",
							"rsp": event["value"]["rsp"],
							"totalNum": value["totalNum"],
							"value": {
								"list": value["conferences"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnRecvQueryConfInfoNotifySuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnRecvQueryConfInfoNotifySuccess"]);
					} else if (statusValue == "4090" && value["totalNum"] == 0) {
						var retEvent = {
							"eventName": "OnRecvQueryConfInfoNotifySuccess",
							"rsp": event["value"]["rsp"],
							"totalNum": "0",
							"value": {
								"list": ""
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnRecvQueryConfInfoNotifySuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnRecvQueryConfInfoNotifySuccess"]);
					} else {
						var retEvent = {
							"eventName": "OnRecvQueryConfInfoNotifyFailure",
							"rsp": event["value"]["rsp"],
							"totalNum": "0",
							"retMsg": event["value"]["retMsg"],
							"value": {
								"list": ""
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnRecvQueryConfInfoNotifyFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnRecvQueryConfInfoNotifyFailure"]);
					}
				} else if (value["msgType"] == "queryaddressbook") {
					if (statusValue == "4090") {
						var retEvent = {
							"eventName": "OnQueryConfereeAddressBookSuccess",
							"rsp": "0",
							"userCount": value["userCount"],
							"value": {
								"list": value["list"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfereeAddressBookSuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfereeAddressBookSuccess"]);
					}
					if (statusValue == "4091" && value["userCount"] != 0) {
						var retEvent = {
							"eventName": "OnQueryConfereeAddressBookFailure",
							"rsp": event["value"]["rsp"],
							"retMsg": event["value"]["retMsg"],
							"userCount": value["userCount"],
							"value": {
								"list": value["list"]
							}
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfereeAddressBookFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfereeAddressBookFailure"]);
					} else if (statusValue == "4091" && value["userCount"] == 0) {
						var retEvent = {
							"eventName": "OnQueryConfereeAddressBookFailure",
							"rsp": event["value"]["rsp"],
							"retMsg": event["value"]["retMsg"],
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfereeAddressBookFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfereeAddressBookFailure"]);
					}
				} else if (value["msgType"] == "queryCasConfInfos") {
                    if (statusValue == "4090") {
                        var retEvent = {
							"eventName": "OnQueryCasConfInfosSuccess",
							"rsp": "0",
                            "value": {
								"name": value["Content"]["name"],
                                "isWeLink": value["Content"]["isWeLink"],
                                "confCasId": value["Content"]["confCasId"],
                                "childConf": value["Content"]["childConf"]
							}
                        };
						this.EVENT_LIST["PhoneConfNotify"]["OnQueryCasConfInfosSuccess"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryCasConfInfosSuccess"]);
                    }
                    if (statusValue == "4091") {
                        var retEvent = {
							"eventName": "OnQueryCasConfInfosFailure",
							"rsp": event["value"]["rsp"],
							"retMsg": event["value"]["retMsg"],
						};
						this.EVENT_LIST["PhoneConfNotify"]["OnQueryCasConfInfosFailure"].event = retEvent;
						sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryCasConfInfosFailure"]);
                    }
                } else if (value["msgType"] == "queryOrgTree") {
                    if (statusValue == "4090") {
                        var retEvent = {
                            "eventName": "OnQueryOrgTreeSuccess",
                            "rsp": "0",
                            "value": event["value"],
                        };
                        this.EVENT_LIST["PhoneConfNotify"]["OnQueryOrgTreeSuccess"].event = retEvent;
                        sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryOrgTreeSuccess"]);
                    }
                    if (statusValue == "4091") {
                        var retEvent = {
                            "eventName": "OnQueryOrgTreeFail",
                            "rsp": event["value"]["rsp"],
                            "retMsg": event["value"]["retMsg"],
                        };
                        this.EVENT_LIST["PhoneConfNotify"]["OnQueryOrgTreeFail"].event = retEvent;
                        sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryOrgTreeFail"]);
                    }
                } else if (value["msgType"] == "queryAreaOrgInfo") {
                    if (statusValue == "4090") {
                        var retEvent = {
                            "eventName": "OnQueryAreaOrgInfoSuccess",
                            "rsp": "0",
                            "value": event["value"],
                        };
                        this.EVENT_LIST["PhoneConfNotify"]["OnQueryAreaOrgInfoSuccess"].event = retEvent;
                        sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryAreaOrgInfoSuccess"]);
                    }
                    if (statusValue == "4091") {
                        var retEvent = {
                            "eventName": "OnQueryAreaOrgInfoFail",
                            "rsp": event["value"]["rsp"],
                            "retMsg": event["value"]["retMsg"],
                            "value": event["value"]
                        };
                        this.EVENT_LIST["PhoneConfNotify"]["OnQueryAreaOrgInfoFail"].event = retEvent;
                        sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryAreaOrgInfoFail"]);
                    }
                } else if (value["msgType"] == "queryConfURL") {
					if (statusValue == "4090") {
                        var retEvent = {
                            "eventName": "OnQueryConfURLSuccess",
                            "rsp": "0",
                            "value": event["value"],
                        };
                        this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfURLSuccess"].event = retEvent;
                        sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfURLSuccess"]);
                    }
                    if (statusValue == "4091") {
                        var retEvent = {
                            "eventName": "OnQueryConfURLFailure",
                            "rsp": event["value"]["rsp"],
                            "retMsg": event["value"]["retMsg"],
                        };
                        this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfURLFailure"].event = retEvent;
                        sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfURLFailure"]);
                    }
				}

			break;
		case "36":
			var retEvent = {
				"eventName": "OnQueryVWallDisplayMatrixInfoResult",
				"rsp": "0",
				"value": event["value"]
			};
			this.EVENT_LIST["PhoneConfNotify"]["OnQueryVWallDisplayMatrixInfoResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryVWallDisplayMatrixInfoResult"]);
			break;
		case "37":
			if (statusValue == "4090") {
				// 上墙成功
				var retEvent = {
					"eventName": "OnStartConfUploadWallSuccess",
					"rsp": "0",
					"value": {
						"confId": event["value"]["confid"],
						"number": event["value"]["number"],
						"layerInfo": event["value"]["layerInfo"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnStartConfUploadWallSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnStartConfUploadWallSuccess"]);
			} else if (statusValue == "4091") {
				// 上墙失败
				var retEvent = {
					"eventName": "OnStartConfUploadWallFailure",
					"rsp": "0",
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnStartConfUploadWallFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnStartConfUploadWallFailure"]);
			}
			break;
		case "38":
			if (statusValue == "4090") {
				// 停止上墙成功
				var retEvent = {
					"eventName": "OnStopConfUploadWallSuccess",
					"rsp": "0",
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnStopConfUploadWallSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnStopConfUploadWallSuccess"]);
			} else if (statusValue == "4091") {
				// 停止上墙失败
				var retEvent = {
					"eventName": "OnStopConfUploadWallFailure",
					"rsp": "0",
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnStopConfUploadWallFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnStopConfUploadWallFailure"]);
			}
			break;
		case "39":
			// 当广播多画面的时候，smc传回的braodcast参数为"(%CP)"，需要改成"mix"
			if (event["value"]["confStatus"]["broadcast"] == "(%CP)") {
				event["value"]["confStatus"]["broadcast"] = "mix";
			}

			cloudICP.util.callStatusMgr.updateConfInfo(event);

			event["value"]["confStatus"]["confName"] = event["value"]["confStatus"]["name"];
			delete event["value"]["confStatus"]["name"];

			// DTS2023060112941 判断如果会议主席不是自己的话，删除 windowInfos[2147483647] 这个字段，如果会议主席切换到自己，将windowInfos[2147483647]赋值
			if (event["value"]["confStatus"]["chair"] && cloudICP.userInfo["isdn"] == event["value"]["confStatus"]["chair"]) {
                if (undefined == cloudICP.util.callStatusMgr.windowInfos[2147483647]) {
                    cloudICP.util.callStatusMgr.windowInfos[2147483647] = "1";  // 此处的1是任意值，不存在任何意义
                }
			} else {
                if (undefined != cloudICP.util.callStatusMgr.windowInfos[2147483647]) {
                    delete cloudICP.util.callStatusMgr.windowInfos[2147483647];
                }
			}
			// 成员信息推送
			var retEvent = {
				"eventName": "OnConfStatusNotify",
				"rsp": "0",
				"value": event["value"]
			};
			this.EVENT_LIST["PhoneConfNotify"]["OnConfStatusNotify"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfStatusNotify"]);
			break;
		case "40":
			if (statusValue == "4090") {
				// 订阅会议成功
				var retEvent = {
					"eventName": "OnQueryConfWallInfoResult",
					"rsp": "0",
					"value": event["value"]
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfWallInfoResult"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfWallInfoResult"]);
			} else if (statusValue == "4091") {
				// 订阅会议失败
				var retEvent = {
					"eventName": "OnQueryConfWallInfoResult",
					"rsp": "-1",
					"value": ""
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfWallInfoResult"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryConfWallInfoResult"]);
			}
			break;
		case "41":
			if (statusValue == "4090") {
				// 订阅会议成功
				var retEvent = {
					"eventName": "OnSubscribeConfSuccess",
					"rsp": "0",
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfSuccess"]);
			} else if (statusValue == "4091") {
				// 订阅会议失败
				var retEvent = {
					"eventName": "OnSubscribeConfFailure",
					"rsp": "0",
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfFailure"]);
			}
			break;
		case "42":
			if (statusValue == "4090") {
				// 去订阅会议成功
				var retEvent = {
					"eventName": "OnUnsubscribeConfSuccess",
					"rsp": "0",
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfSuccess"]);
			} else if (statusValue == "4091") {
				// 去订阅会议失败
				var retEvent = {
					"eventName": "OnUnsubscribeConfFailure",
					"rsp": "0",
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"]);
			}
			break;
		case "43":
			if (statusValue == "4090") {
				// 订阅会议成功
				var retEvent = {
					"eventName": "OnHangupConfMemberSuccess",
					"rsp": "0",
					"value": {
						"confId": event["value"]["confId"],
						"member": event["value"]["members"]
					},
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnHangupConfMemberSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnHangupConfMemberSuccess"]);
			} else if (statusValue == "4091") {
				// 订阅会议失败
				var retEvent = {
					"eventName": "OnHangupConfMemberFailure",
					"rsp": event["value"]["rsp"],
					"value": {
						"confId": event["value"]["confId"],
						"member": event["value"]["members"]
					},
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnHangupConfMemberFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnHangupConfMemberFailure"]);
			}
			break;
		case "44":
			if (statusValue == "4090") {
				// 重呼成员成功
				var retEvent = {
					"eventName": "OnCallConfMemberSuccess",
					"rsp": "0",
					"value": {
						"confId": event["value"]["confId"],
						"member": event["value"]["members"]
					},
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnCallConfMemberSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnCallConfMemberSuccess"]);
			} else if (statusValue == "4091") {
				// 重呼成员失败
				var retEvent = {
					"eventName": "OnCallConfMemberFailure",
					"rsp": event["value"]["rsp"],
					"value": {
						"confId": event["value"]["confId"],
						"member": event["value"]["members"]
					},
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnCallConfMemberFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnCallConfMemberFailure"]);
			}
			break;
		case "45":
			if (statusValue == "4090") {
				var retEvent = {
					"eventName": "OnQueryRecordURLByConfIDResult",
					"rsp": "0",
					"value": {
						"confId": event["value"]["confInternalId"],
						"url": event["value"]["url"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnQueryRecordURLByConfIDResult"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryRecordURLByConfIDResult"]);
			} else if (statusValue == "4091") {
				var retEvent = {
					"eventName": "OnQueryRecordURLByConfIDResult",
					"rsp": "-1",
					"value": {
						"confId": "",
						"url": ""
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnQueryRecordURLByConfIDResult"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryRecordURLByConfIDResult"]);
			}
			break;
		case "46":
			if (statusValue == "4090") {
				var retEvent = {
					"eventName": "OnDelConfMembersSuccess",
					"rsp": "0",
					"value": {
						"confId": event["value"]["confId"],
						"member": event["value"]["members"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnDelConfMembersSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnDelConfMembersSuccess"]);
			} else if (statusValue == "4091") {
				var retEvent = {
					"eventName": "OnDelConfMembersFailure",
					"rsp": event["value"]["rsp"],
					"value": {
						"confId": event["value"]["confId"],
						"member": event["value"]["members"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnDelConfMembersFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnDelConfMembersFailure"]);
			}
			break;
		case "47":
			if (statusValue == "4090") {
				// 查询多画面成功
				var retEvent = {
					"eventName": "OnQueryContinuousPresenceInfoResult",
					"rsp": "0",
					"value": event["value"]
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnQueryContinuousPresenceInfoResult"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryContinuousPresenceInfoResult"]);
			} else if (statusValue == "4091") {
				// 查询多画面失败
				var retEvent = {
					"eventName": "OnQueryContinuousPresenceInfoResult",
					"rsp": "-1",
					"value": ""
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnQueryContinuousPresenceInfoResult"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnQueryContinuousPresenceInfoResult"]);
			}
			break;
		case "48": {
			var retEvent = {
				"eventName": "OnApplyFloor",
				"value": event["value"]
			};
			this.EVENT_LIST["PhoneConfNotify"]["OnApplyFloor"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnApplyFloor"]);
		}
		break;
	case "50":
		if (statusValue == "4092") {
			var retEvent = {
				"eventName": "ResultOfMappingBetweenIsdn2Gbid",
				"value": event["value"]
			};
			this.EVENT_LIST["ResourceNotify"]["ResultOfMappingBetweenIsdn2Gbid"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["ResourceNotify"]["ResultOfMappingBetweenIsdn2Gbid"]);
		}
		break;
	case "51":
		if (statusValue == "4090") {
			var retEvent = {
				"eventName": "OnConfProlongTimeSuccess",
				"value": event["value"]
			};
			this.EVENT_LIST["PhoneConfNotify"]["OnConfProlongTimeSuccess"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfProlongTimeSuccess"]);
		} else if (statusValue == "4091") {
			var retEvent = {
				"eventName": "OnConfProlongTimeFailure",
				"value": event["value"]
			};
			this.EVENT_LIST["PhoneConfNotify"]["OnConfProlongTimeFailure"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfProlongTimeFailure"]);
		}
		break;
	case "52":
		var value = event["value"];
		if (value["params"] == "lock") {
			if (statusValue == "4090") {
				var retEvent = {
					"eventName": "OnConfLockVideoSourceSuccess",
					"rsp": value["rsp"],
					"value": {
						"confId": value["confId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnConfLockVideoSourceSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfLockVideoSourceSuccess"]);
			} else if (statusValue == "4091") {
				var retEvent = {
					"eventName": "OnConfLockVideoSourceFailure",
					"rsp": value["rsp"],
					"value": {
						"confId": value["confId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnConfLockVideoSourceFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfLockVideoSourceFailure"]);
			}
		} else if (value["params"] == "unlock") {
			if (statusValue == "4090") {
				var retEvent = {
					"eventName": "OnConfUnLockVideoSourceSuccess",
					"rsp": value["rsp"],
					"value": {
						"confId": value["confId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnConfUnLockVideoSourceSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfUnLockVideoSourceSuccess"]);
			} else if (statusValue == "4091") {
				var retEvent = {
					"eventName": "OnConfUnLockVideoSourceFailure",
					"rsp": value["rsp"],
					"value": {
						"confId": value["confId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnConfUnLockVideoSourceFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfUnLockVideoSourceFailure"]);
			}
		}
		break;
	case "53":
		var value = event["value"];
		if (value["params"] == "lock") {
			if (statusValue == "4090") {
				var retEvent = {
					"eventName": "OnConfLockConfSuccess",
					"rsp": value["rsp"],
					"value": {
						"confId": value["confId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnConfLockConfSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfLockConfSuccess"]);
			} else if (statusValue == "4091") {
				var retEvent = {
					"eventName": "OnConfLockConfFailure",
					"rsp": value["rsp"],
					"value": {
						"confId": value["confId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnConfLockConfFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfLockConfFailure"]);
			}
		} else if (value["params"] == "unlock") {
			if (statusValue == "4090") {
				var retEvent = {
					"eventName": "OnConfUnLockConfSuccess",
					"rsp": value["rsp"],
					"value": {
						"confId": value["confId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnConfUnLockConfSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfUnLockConfSuccess"]);
			} else if (statusValue == "4091") {
				var retEvent = {
					"eventName": "OnConfUnLockConfFailure",
					"rsp": value["rsp"],
					"value": {
						"confId": value["confId"]
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnConfUnLockConfFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfUnLockConfFailure"]);
			}
		}
		break;
	case "55":
		if (statusValue == "4090") {
			// 修改会议成功
			var retEvent = {
				"eventName": "OnModifyConfSuccess",
				"rsp": "0",
				"value": {
					"confId": event["value"]["confInternalId"],
					"passcode": event["value"]["passcode"],
					"unifiedAccessCode": event["value"]["unifiedAccessCode"],
					"isVideo": event["value"]["isVideo"]
				}
			};

			if (undefined != event["value"]["time"] && undefined != event["value"]["timeZone"]) {
				retEvent["value"]["time"] = event["value"]["time"];
				retEvent["value"]["timeZone"] = event["value"]["timeZone"];
				this.EVENT_LIST["PhoneConfNotify"]["OnModifyConfSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnModifyConfSuccess"]);
			} else {
				this.EVENT_LIST["PhoneConfNotify"]["OnModifyConfSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnModifyConfSuccess"]);
			}
		} else if (statusValue == "4091") {
			// 修改会议失败
			var retEvent = {
				"eventName": "OnModifyConfFailure",
				"rsp": event["value"]["rsp"],
				"value": {
					"confId": event["value"]["confInternalId"]
				}
			};
			this.EVENT_LIST["PhoneConfNotify"]["OnModifyConfFailure"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnModifyConfFailure"]);
		}
		break;
	case "56":
		var value = event["value"];
		if (value["msgType"] == "setTextTips") {
			if (statusValue == "4090") {
				var retEvent = {
					"eventName": "OnConfTextTipsSuccess",
					"rsp": "0",
					"value": {
						"confId": value["confId"],
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnConfTextTipsSuccess"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfTextTipsSuccess"]);
			} else if (statusValue == "4091") {
				var retEvent = {
					"eventName": "OnConfTextTipsFailure",
					"rsp": event["value"]["rsp"],
					"value": {
						"confId": value["confId"],
					}
				};
				this.EVENT_LIST["PhoneConfNotify"]["OnConfTextTipsFailure"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfTextTipsFailure"]);
			}
		}
		break;
	default:
		break;
	}

	if (undefined != event["rid"]) {
		delete event.rid;
	}

	if ("4025" == event["statusvalue"] && "21" == event["statustype"]) {
		//动态组创建失败
		var cause = "";
		if (event["value"]) {
			cause = event["value"]["cause"];
		}
		var retEvent = {
			"eventName": "OnDynamicGroupOptFailed",
			"grpid": event["value"]["attaching"],
			"rsp": cause
		};
		this.EVENT_LIST["GroupNotify"]["OnDynamicGroupOptFailed"].event = retEvent;
		sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnDynamicGroupOptFailed"]);
		return;
	} else if (undefined != event["list"]) {
		for (var index = 0; index < event["list"].length; index++) {
			event["list"][index]["peerid"] = "";
		}
		var retEvent = {
			"eventName": "OnUserStatusNotify",
			"list": event["list"]
		}
		this.EVENT_LIST["VoiceNotify"]["OnUserStatusNotify"].event = retEvent;
		sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnUserStatusNotify"]);
	}

	if (event["statustype"] == "3") {
		var retEvent = {
			"eventName": "OnGroupCallStatusNotify",
			"rsp": event["statusvalue"],
			"value": {
				"grpid": event["isdn"],
				"name": event["value"]["name"],
				"speaker": event["value"]["speaker"]
			}
		}


		if (event["value"]["speaker"] == cloudICP.userInfo["isdn"]) {
			cloudICP.dispatch.group.isCurrentPtt = {
				"grpid": event["isdn"],
				"isPtt": true
			};
		} else {
			if (cloudICP.dispatch.group.isCurrentPtt["grpid"] == event["isdn"]) {
				cloudICP.dispatch.group.isCurrentPtt = {
					"grpid": event["isdn"],
					"isPtt": false
				};
			}
		}
		this.EVENT_LIST["GroupCallNotify"]["OnGroupCallStatusNotify"].event = retEvent;
		sendEvent2Page(reqId, this.EVENT_LIST["GroupCallNotify"]["OnGroupCallStatusNotify"]);
	}
}

/**
 * 群组管理
 */
ICPSDK_Dispatch_Event.prototype.delCommonNotify = function(event) {
	var reqId = event.reqId;
	if (reqId == null || reqId == undefined) {reqId = "0";}
	var mo = event["mo"];
	if (undefined == mo) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "delCommonNotify: mo is undefined."
		});
		return;
	}
	var opt = event["opt"];
	if (undefined == opt) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "delCommonNotify: opt is undefined."
		});
		return;
	}
	var value = event["value"];
	if (undefined != value && undefined != value["servermode"]) {
		delete value.servermode;
	}

	if (undefined != value["vpnid"]) {
		delete value["vpnid"];
	}

	if (mo == "3") {

		var newEvent = {
			"eventName": "OnTalkingGroupStatusChange",
			"opt": opt,
			"groupinfo": value
		}
		this.EVENT_LIST["GroupNotify"]["OnTalkingGroupStatusChange"].event = newEvent;
		sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnTalkingGroupStatusChange"]);
	} else if (mo == "29") {
		//派接组的新建和删除
		if (undefined == value) {
			cloudICP.util.log(tabPageId, {
				"logLevel": "error",
				"logMsg": "delCommonNotify: value is undefined."
			});
			return;
		}

		if (undefined != value["vpnid"]) {
			delete value["vpnid"];
		}

		var newEvent = {
			"eventName": "OnPatchGroupStatusChange",
			"opt": opt,
			"groupinfo": value
		}
		this.EVENT_LIST["GroupNotify"]["OnPatchGroupStatusChange"].event = newEvent;
		sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnPatchGroupStatusChange"]);
	} else if (mo == "7") {
		//动态组的成员的新增和删除
		if (undefined == value) {
			cloudICP.util.log(tabPageId, {
				"logLevel": "error",
				"logMsg": "delCommonNotify: value is undefined."
			});
			return;
		}

        // var newEvent = {
        //     "eventName": "OnTalkingGroupMemberChange",
        //     "opt": opt,
        //     "groupinfo": value
        // }
        // this.EVENT_LIST["GroupNotify"]["OnTalkingGroupMemberChange"](newEvent);
	//	sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnTalkingGroupMemberChange"]);
	} else if (mo == "30") {
		//派接组的成员的新增和删除
		if (undefined == value) {
			cloudICP.util.log(tabPageId, {
				"logLevel": "error",
				"logMsg": "delCommonNotify: value is undefined."
			});
			return;
		}

		var newEvent = {
			"eventName": "OnPatchGroupMemberChange",
			"opt": opt,
			"groupinfo": value
		}
		this.EVENT_LIST["GroupNotify"]["OnPatchGroupMemberChange"].event = newEvent;
		sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnPatchGroupMemberChange"]);
	} else if (mo == "6") {
		//群组成员配置更新
		var newEvent = {
			"eventName": "OnTalkingGroupMemberChange",
			"opt": opt,
			"groupinfo": value
		}
		this.EVENT_LIST["GroupNotify"]["OnTalkingGroupMemberChange"].event = newEvent;
		sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnTalkingGroupMemberChange"]);
	} else if (mo == "5") {
		//群组终端用户配置更新
		var newEvent = {
			"eventName": "OnTalkingGroupMemberChange",
			"opt": opt,
			"groupinfo": value
		}
		this.EVENT_LIST["GroupNotify"]["OnTalkingGroupMemberChange"].event = newEvent;
		sendEvent2Page(reqId, this.EVENT_LIST["GroupNotify"]["OnTalkingGroupMemberChange"]);
	}


}

/**
 * 语音事件通知
 */
ICPSDK_Dispatch_Event.prototype.delVoiceNotify = function(event) {
	var reqId = event.reqId;
	if (reqId == null || reqId == undefined) {reqId = "0";}
	var rsp = event["rsp"];
	if (undefined == rsp) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "delVoiceNotify: rsp is undefined."
		});
		return;
	}
	var value = event["value"];
	if (undefined == value) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "delVoiceNotify: value is undefined."
		});
		return;
	}

	// 建立音频媒体通道
	if (value["local"] != undefined && value["server"] != value["local"] != undefined &&
		"0.0.0.0,0" && value["server"] != "0.0.0.0,0") {
		let localArr = value["local"].split(",");
		let serverArr = value["server"].split(",");

		var user = cloudICP.userInfo["isdn"];
		var peerUser = value["callee"];
		if (user == peerUser) {
			peerUser = value["caller"];
		}

		var callNumber;
		if (event["value"]["callee"] != cloudICP.userInfo["isdn"]) {
			callNumber = event["value"]["callee"]
		} else {
			callNumber = event["value"]["caller"];
		}

		var paramForAudio =
			"{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}}".format(
				parseInt(value["cid"])
			)
		cloudICP.dispatch.webSocket.sendDataToMediaSDK(paramForAudio, true);

		var param
		var callStatusMgr = cloudICP.util.callStatusMgr;
		var audioFile = callStatusMgr.getBroadcastFile(peerUser);
		var audioBase64 = callStatusMgr.getBroadcastBase64(peerUser);
		if (audioFile) {

			var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
				parseInt(value["cid"]), reqId
			);

			cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

			var param =
				"{\"description\":\"msp_create_audio_channel\", \"cmd\": 65537, \"param\":{\"resID\":{0}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"user\":\"{8}\", \"peerUser\":\"{9}\", \"audioFile\": \"{10}\"}}, \"reqId\": \"{11}\"}"
				.format(
					parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0], serverArr[1],
					parseInt(value["audio"]), parseInt(value["audio_ssrc"]), parseInt(callNumber), user, peerUser,
					audioFile, reqId
				)
			callStatusMgr.deleteBroadcastFile(peerUser);
		} else if (audioBase64) {

			var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}}".format(
				parseInt(value["cid"])
			);

			cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

			var param = "{\"description\":\"msp_create_audio_channel\", \"cmd\": 65537, \"param\":{\"resID\":{0}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"user\":\"{8}\", \"peerUser\":\"{9}\", \"audioBase64\": \"{10}\"}}}".format(
				parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0], serverArr[1], parseInt(value["audio"]), parseInt(value["audio_ssrc"]), parseInt(callNumber), user, peerUser, audioBase64
			)
			callStatusMgr.deleteBroadcastBase64(peerUser);
		} else {
			var param =
				"{\"description\":\"msp_create_audio_channel\", \"cmd\": 65537, \"param\":{\"resID\":{0}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"user\":\"{8}\", \"peerUser\":\"{9}\"}}, \"reqId\": \"{10}\"}"
				.format(
					parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0], serverArr[1],
					parseInt(value["audio"]), parseInt(value["audio_ssrc"]), parseInt(callNumber), user, peerUser, reqId
				)
		}

		cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
	}

	var to;
	if (undefined != value["to"]) {
		if ("" != value["to"]) {
			to = value["to"];
		} else if (undefined != value["callee"]) {
			to = value["callee"];
		}
		delete value.to;
	} else {
		to = value["callee"];
	}

	var from;
	if (undefined != value["from"] && value["from"] != "" && value["caller"] != value["from"]) {
		from = value["from"];
	} else {
		from = value["caller"];
	}

	if (undefined != value["from"]) {
		delete value.from;
	}

	var local = value["local"];
	if (undefined != local) {
		delete value.local;
	}

	var server = value["server"];
	if (undefined != server) {
		delete value.server;
	}

	var sipcode = value["sipcode"];
	if (undefined != sipcode) {
		delete value.sipcode;
	}

	var targeter = value["targeter"];
	if (undefined != targeter) {
		delete value.targeter;
	}

	var transfer = value["transfer"];
	if (undefined != transfer) {
		delete value.transfer;
	}

	var inserter = value["inserter"];
	if (undefined != inserter) {
		delete value.inserter;
	}

	var audio = value["audio"];
	if (undefined != audio) {
		delete value.audio;
	}

	var audio_ssrc = value["audio_ssrc"];
	if (undefined != audio_ssrc) {
		delete value.audio_ssrc;
	}

	var discreet_listenee = value["discreetListenee"];
	if (undefined != discreet_listenee) {
		delete value.discreetListenee;
	}

	var sub_type = value["sub_type"];
	if (undefined != sub_type) {
		delete value.sub_type;
	}

	var confInfo = value["confInfo"];
	if (undefined != confInfo) {
		delete value.confInfo;
	}

	var listentype = value["listentype"];
	if (undefined != confInfo) {
		delete value.listentype;
	}

	var newEvent = {
		"eventName": "",
		"rsp": rsp,
		"value": value
	}

	var caller = value["caller"];
	if (caller == cloudICP.userInfo["isdn"]) {
		//表示呼出
		switch (rsp) {
			case "2003":
				// 停止振铃音
				value["callee"] = to;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;

				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VOICECONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.CONNECTING, event);

					callStatusMgr.holdCall(event["value"]["cid"], reqId);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "false";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnConfConnect";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfConnect"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfConnect"]);

					var ajaxCfg = {
						"type": "POST",
						"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
						"data": {
							"opt": "sub",
							"reqId": reqId,
							"confId": call["confId"]
						},
						"callback": function(data) {
							if (data["rsp"] != "0") {
								var retEvent = {
									"eventName": "OnSubscribeConfFailure",
									"rsp": data["rsp"],
								}
								this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfFailure"].event = retEvent;
								sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfFailure"]);
							} else {
								cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] = true;
							}
						}
					}
					cloudICP.util.ajax(ajaxCfg);
					break;
				} else if (call && call.callType == callStatusMgr.HALFVOICE) {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.CONNECTING, event);

					callStatusMgr.holdCall(event["value"]["cid"], reqId);

					newEvent["value"]["calltype"] = "halfdial";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "out";
					newEvent["value"]["fmt"] = "";
					newEvent["value"]["ptz"] = "";
					newEvent["value"]["mute"] = "";
					newEvent["eventName"] = "OnCallConnect";
					this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);
					break;
				} else if (call && call.callType == callStatusMgr.AMBIENCE) {
					callStatusMgr.updateCallStatus(callStatusMgr.AMBIENCE, callStatusMgr.CONNECTING, event);

					newEvent["value"]["calltype"] = "ambience";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "out";
					newEvent["value"]["fmt"] = "";
					newEvent["value"]["ptz"] = "";
					newEvent["value"]["mute"] = "";
					newEvent["eventName"] = "OnCallConnect";
					this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.CONNECTING, event);

				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["src"] = "";
				newEvent["value"]["direction"] = "out";
				newEvent["value"]["fmt"] = "";
				newEvent["value"]["ptz"] = "";
				newEvent["value"]["mute"] = "";
				newEvent["eventName"] = "OnCallConnect";
				this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);
				break;
			case "2005":
				value["callee"] = to;
				var callStatusMgr = cloudICP.util.callStatusMgr;
				if (confInfo != undefined) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.NEW, event, confInfo);
					break;
				}

				if (sub_type && sub_type == "1") {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.NEW, event);
					newEvent["value"]["calltype"] = "halfdial";
				} else if (listentype && listentype == "1") {
					callStatusMgr.updateCallStatus(callStatusMgr.AMBIENCE, callStatusMgr.NEW, event);
					newEvent["value"]["calltype"] = "ambience";
				} else {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.NEW, event);
					newEvent["value"]["calltype"] = "voice";
				}

				newEvent["value"]["fmt"] = "";
				newEvent["eventName"] = "OnDialOutProceeding";
				this.EVENT_LIST["VoiceNotify"]["OnDialOutProceeding"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutProceeding"]);
				break;
			case "2006":
				// 播放回铃音
				value["callee"] = to;
				var param =
					"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":1, \"delay\":0, \"duration\": 0, \"playcount\":0}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					);

				if (local == "0.0.0.0,0" || server == "0.0.0.0,0" || value["local"] == null) {
					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VOICECONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.RINGING, event);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "false";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnConfDialOutRinging";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfDialOutRinging"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfDialOutRinging"]);
					break;
				} else if (call && call.callType == callStatusMgr.HALFVOICE) {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RINGING, event);

					newEvent["value"]["calltype"] = "halfdial";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnDialOutRinging";
					this.EVENT_LIST["VoiceNotify"]["OnDialOutRinging"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutRinging"]);
					break;
				} else if (call && call.callType == callStatusMgr.AMBIENCE) {
					callStatusMgr.updateCallStatus(callStatusMgr.AMBIENCE, callStatusMgr.RINGING, event);

					newEvent["value"]["calltype"] = "ambience";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnDialOutRinging";
					this.EVENT_LIST["VoiceNotify"]["OnDialOutRinging"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutRinging"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.RINGING, event);

				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["fmt"] = "";
				newEvent["eventName"] = "OnDialOutRinging";
				this.EVENT_LIST["VoiceNotify"]["OnDialOutRinging"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutRinging"]);
				break;
			case "2009":
				// 停止振铃音
				value["callee"] = to;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 删除音频媒体通道
				var param =
					"{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					)

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VOICECONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.RELEASE, event);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "false";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnConfRelease";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"]);

					setTimeout(function() {
						var mconfInfo = cloudICP.util.callStatusMgr.confMgr[call["confId"]];
						if (mconfInfo != null && ((mconfInfo["isEnding"] != undefined && mconfInfo[
								"isEnding"] == true) || (mconfInfo["value"]["confStatus"]["status"] ==
								"End"))) {
							return;
						}
						if (cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] == undefined || cloudICP
							.util.callStatusMgr.confSubMgr[call["confId"]] != true) {
							return;
						}
						var ajaxCfg = {
							"type": "POST",
							"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
							"data": {
								"opt": "unsub",
								"reqId": reqId,
								"confId": call["confId"]
							},
							"callback": function(data) {
								if (data["rsp"] != "0") {
									var retEvent = {
										"eventName": "OnUnsubscribeConfFailure",
										"rsp": data["rsp"],
									}
									this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"].event = retEvent;
									sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"]);
								}
							}
						}
						cloudICP.util.ajax(ajaxCfg);
					}, 500);
					break;
				} else if (call && call.callType == callStatusMgr.HALFVOICE) {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "halfdial";
					newEvent["value"]["releasetype"] = "other";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "out";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnCallRelease";
					this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					break;
				} else if (call && call.callType == callStatusMgr.AMBIENCE) {
					callStatusMgr.updateCallStatus(callStatusMgr.AMBIENCE, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "ambience";
					newEvent["value"]["releasetype"] = "other";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "out";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnCallRelease";
					this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.RELEASE, event);

				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["releasetype"] = "other";
				newEvent["value"]["src"] = "";
				newEvent["value"]["direction"] = "out";
				newEvent["value"]["fmt"] = "";
				newEvent["eventName"] = "OnCallRelease";
				this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
				break;
			case "2010":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				// 停止振铃音
				value["callee"] = to;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 删除音频媒体通道
				var param =
					"{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					)

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VOICECONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.RELEASE, event);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "false";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnConfRelease";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"]);

					setTimeout(function() {
						var mconfInfo = cloudICP.util.callStatusMgr.confMgr[call["confId"]];
						if (mconfInfo != null && ((mconfInfo["isEnding"] != undefined && mconfInfo[
								"isEnding"] == true) || (mconfInfo["value"]["confStatus"]["status"] ==
								"End"))) {
							return;
						}
						if (cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] == undefined || cloudICP
							.util.callStatusMgr.confSubMgr[call["confId"]] != true) {
							return;
						}
						var ajaxCfg = {
							"type": "POST",
							"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
							"data": {
								"opt": "unsub",
								"reqId": reqId,
								"confId": call["confId"]
							},
							"callback": function(data) {
								if (data["rsp"] != "0") {
									var retEvent = {
										"eventName": "OnUnsubscribeConfFailure",
										"rsp": data["rsp"],
									}
									this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"].event = retEvent;
									sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"]);
								}
							}
						}
						cloudICP.util.ajax(ajaxCfg);
					}, 500);
					break;
				} else if (call && call.callType == callStatusMgr.HALFVOICE) {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "halfdial";
					newEvent["value"]["releasetype"] = "self";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "out";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnCallRelease";
					this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					break;
				} else if (call && call.callType == callStatusMgr.AMBIENCE) {
					callStatusMgr.updateCallStatus(callStatusMgr.AMBIENCE, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "ambience";
					newEvent["value"]["releasetype"] = "self";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "out";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnCallRelease";
					this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.RELEASE, event);


				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["releasetype"] = "self";
				newEvent["value"]["src"] = "";
				newEvent["value"]["direction"] = "out";
				newEvent["value"]["fmt"] = "";
				newEvent["eventName"] = "OnCallRelease";
				this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
				break;
			case "2013":
			case "2016":
			case "2017":
			case "2018":
			case "2019":
			case "2024":
			case "2049":
			case "2052":
			case "2023":
			case "2033":
			case "2048":
				// 停止振铃音
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				var paramForAudio =
					"{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}}"
					.format(
						parseInt(value["cid"])
					)
				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				cloudICP.dispatch.webSocket.sendDataToMediaSDK(paramForAudio, true);

				var user = cloudICP.userInfo["isdn"];
				var peerUser = value["callee"];
				if (user == peerUser) {
					peerUser = value["caller"];
				}
				cloudICP.util.callStatusMgr.deleteBroadcastFile(peerUser);
                cloudICP.util.callStatusMgr.deleteBroadcastBase64(peerUser);

				if (rsp == "2013") {
					//对端正在通话中
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":3, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "2016" || rsp == "2024" || rsp == "2052") {
					//对端无法接通
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":4, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "2018") {
					//对端号码不存在
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":5, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "") {
					//本段没有权限
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":6, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "") {
					//对端没有权限
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":7, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "2017") {
					//对端无应答
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":8, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "") {
					//对端正忙
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":9, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				} else if (rsp == "2048" || rsp == "2023" || rsp == "2033" || rsp == "2049") {
					//拨打业务无法支持
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":10, \"delay\":0, \"duration\": 0, \"playcount\":1}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);

					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				if (confInfo != undefined) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.RELEASE, event, confInfo);

					newEvent["value"]["confId"] = confInfo["confInternalId"];
					newEvent["value"]["isVideo"] = "false";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnConfFailure";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfFailure"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfFailure"]);
					break;
				}

				if (listentype && listentype == "1") {
					callStatusMgr.updateCallStatus(callStatusMgr.AMBIENCE, callStatusMgr.RELEASE, event);
					newEvent["value"]["calltype"] = "ambience";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnDialOutFailure";
					this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"]);

					break;
				}

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.HALFVOICE) {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "halfdial";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnDialOutFailure";
					this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.RELEASE, event);

				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["fmt"] = "";
				newEvent["eventName"] = "OnDialOutFailure";
				this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialOutFailure"]);
				break;

				break;
			case "2041":
				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.NEW, event);

				newEvent["eventName"] = "OnHalfDialConnectProceeding";
				this.EVENT_LIST["VoiceNotify"]["OnHalfDialConnectProceeding"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnHalfDialConnectProceeding"]);
				break;
			case "2042":
				// 关闭麦克风
				var mediaParam =
					"{\"description\":\"msp_mute_audio_channel\", \"cmd\": 65549, \"param\":{\"resID\": {0}, \"mute\":true}}"
					.format(
						parseInt(value["cid"])
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.CONNECTING, event);

				newEvent["eventName"] = "OnStartRecvHalfDial";
				this.EVENT_LIST["VoiceNotify"]["OnStartRecvHalfDial"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnStartRecvHalfDial"]);
				break;
			case "2043":
				// 关闭麦克风
				var mediaParam =
					"{\"description\":\"msp_mute_audio_channel\", \"cmd\": 65549, \"param\":{\"resID\": {0}, \"mute\":true}}"
					.format(
						parseInt(value["cid"])
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam);

				// var callStatusMgr = cloudICP.util.callStatusMgr;
				// callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RELEASE, event);

				newEvent["eventName"] = "OnStopRecvHalfDial";
				this.EVENT_LIST["VoiceNotify"]["OnStopRecvHalfDial"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnStopRecvHalfDial"]);
				break;
			case "2045":
				// 打开麦克风
				var mediaParam =
					"{\"description\":\"msp_mute_audio_channel\", \"cmd\": 65549, \"param\":{\"resID\": {0}, \"mute\":false}}"
					.format(
						parseInt(value["cid"])
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.CONNECTING, event);

				newEvent["eventName"] = "OnHalfDialSuccess";
				this.EVENT_LIST["VoiceNotify"]["OnHalfDialSuccess"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnHalfDialSuccess"]);
				break;
			case "2046":
				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RELEASE, event);

				newEvent["eventName"] = "OnHalfDialFailure";
				this.EVENT_LIST["VoiceNotify"]["OnHalfDialFailure"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnHalfDialFailure"]);
				break;
			case "2054":
				newEvent["eventName"] = "OnHoldSuccess";
				this.EVENT_LIST["VoiceNotify"]["OnHoldSuccess"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnHoldSuccess"]);
				break;
			case "2055":
				newEvent["eventName"] = "OnUnholdSuccess";
				this.EVENT_LIST["VoiceNotify"]["OnUnholdSuccess"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnUnholdSuccess"]);
				break;
			case "2056":
				newEvent["eventName"] = "OnReceiveDTMF";
				this.EVENT_LIST["VoiceNotify"]["OnReceiveDTMF"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnReceiveDTMF"]);
				break;
			case "2057": //mrs notify
				newEvent["eventName"] = "MRSInfoNotify";
				this.EVENT_LIST["VoiceNotify"]["MRSInfoNotify"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["MRSInfoNotify"]);
				break;
			case "2012":
				newEvent["eventName"] = "OnTalkingPrempted";
				this.EVENT_LIST["VoiceNotify"]["OnTalkingPrempted"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnTalkingPrempted"]);
				break;
		}
	} else {
		//表示呼入
		switch (rsp) {
			case "2003":
				// 停止振铃音
				newEvent["value"]["caller"] = from;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;

				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VOICECONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.CONNECTING, event);

					callStatusMgr.holdCall(event["value"]["cid"], reqId);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "false";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnConfConnect";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfConnect"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfConnect"]);

					var ajaxCfg = {
						"type": "POST",
						"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
						"data": {
							"opt": "sub",
							"reqId": reqId,
							"confId": call["confId"]
						},
						"callback": function(data) {
							if (data["rsp"] != "0") {
								var retEvent = {
									"eventName": "OnSubscribeConfFailure",
									"rsp": data["rsp"],
								}
								this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfFailure"].event = retEvent;
								sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfFailure"]);
							} else {
								cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] = true;
							}
						}
					}
					cloudICP.util.ajax(ajaxCfg);
					break;
				} else if (call && call.callType == callStatusMgr.HALFVOICE) {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.CONNECTING, event);

					callStatusMgr.holdCall(event["value"]["cid"], reqId);

					newEvent["value"]["calltype"] = "halfdial";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "in";
					newEvent["value"]["fmt"] = "";
					newEvent["value"]["ptz"] = "";
					newEvent["value"]["mute"] = "";
					newEvent["eventName"] = "OnCallConnect";
					this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);
					break;
				} else if (call && call.callType == callStatusMgr.AMBIENCE) {
					callStatusMgr.updateCallStatus(callStatusMgr.AMBIENCE, callStatusMgr.CONNECTING, event);

					newEvent["value"]["calltype"] = "ambience";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "in";
					newEvent["value"]["fmt"] = "";
					newEvent["value"]["ptz"] = "";
					newEvent["value"]["mute"] = "";
					newEvent["eventName"] = "OnCallConnect";
					this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.CONNECTING, event);

				callStatusMgr.holdCall(event["value"]["cid"], reqId);

				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["src"] = "";
				newEvent["value"]["direction"] = "in";
				newEvent["value"]["fmt"] = "";
				newEvent["value"]["ptz"] = "";
				newEvent["value"]["mute"] = "";
				newEvent["eventName"] = "OnCallConnect";
				this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);
				break;
			case "2002":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				if (cloudICP.util.ringType != "1") {
					// 播放振铃音
					newEvent["value"]["caller"] = from;
					var param =
						"{\"description\":\"msp_play_tone\", \"cmd\": 131075, \"param\":{\"cid\": {0}, \"type\":0, \"delay\":0, \"duration\": 0, \"playcount\":0}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(value["cid"]), reqId
						);
					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
				}
				if (discreet_listenee != undefined && discreet_listenee != "0" && discreet_listenee != "") {
					var callStatusMgr = cloudICP.util.callStatusMgr;
					callStatusMgr.updateCallStatus(callStatusMgr.DISCREET, callStatusMgr.RINGING, event);

					newEvent["value"]["calltype"] = "discreetlisten";
					newEvent["value"]["src"] = discreet_listenee;
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnDialInRinging";
					this.EVENT_LIST["VoiceNotify"]["OnDialInRinging"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialInRinging"]);
					break;
				};

				var callStatusMgr = cloudICP.util.callStatusMgr;
				if (confInfo != undefined) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.RINGING, event, confInfo);

					newEvent["value"]["confId"] = confInfo["confInternalId"];
					newEvent["value"]["isVideo"] = "false";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnConfDialInRinging";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfDialInRinging"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfDialInRinging"]);
					break;
				}

				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.HALFVOICE) {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RINGING, event);

					newEvent["value"]["calltype"] = "halfdial";
					newEvent["value"]["src"] = "";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnDialInRinging";
					this.EVENT_LIST["VoiceNotify"]["OnDialInRinging"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialInRinging"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.RINGING, event);

				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["src"] = "";
				newEvent["value"]["fmt"] = "";
				newEvent["eventName"] = "OnDialInRinging";
				this.EVENT_LIST["VoiceNotify"]["OnDialInRinging"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnDialInRinging"]);
				break;
			case "2007":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				// 停止振铃音
				newEvent["value"]["caller"] = from;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;


				if (discreet_listenee != undefined && discreet_listenee != "0" && discreet_listenee != "") {
					var callStatusMgr = cloudICP.util.callStatusMgr;
					callStatusMgr.updateCallStatus(callStatusMgr.DISCREET, callStatusMgr.CONNECTING, event);

					callStatusMgr.holdCall(event["value"]["cid"], reqId);

					newEvent["value"]["calltype"] = "discreetlisten";
					newEvent["value"]["src"] = discreet_listenee;
					newEvent["value"]["direction"] = "in";
					newEvent["value"]["fmt"] = "";
					newEvent["value"]["ptz"] = "";
					newEvent["value"]["mute"] = "";
					newEvent["eventName"] = "OnCallConnect";
					this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);
					break;
				};

				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VOICECONF && confInfo != undefined) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.CONNECTING, event,
						confInfo);

					callStatusMgr.holdCall(event["value"]["cid"], reqId);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "false";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnConfConnect";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfConnect"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfConnect"]);

					var ajaxCfg = {
						"type": "POST",
						"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
						"data": {
							"opt": "sub",
							"reqId": reqId,
							"confId": call["confId"]
						},
						"callback": function(data) {
							if (data["rsp"] != "0") {
								var retEvent = {
									"eventName": "OnSubscribeConfFailure",
									"rsp": data["rsp"],
								}
								this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfFailure"].event = retEvent;
								sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfFailure"]);
							} else {
								cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] = true;
							}
						}
					}
					cloudICP.util.ajax(ajaxCfg);
					break;
				} else if (call && call.callType == callStatusMgr.HALFVOICE) {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.CONNECTING, event);

					callStatusMgr.holdCall(event["value"]["cid"], reqId);

					newEvent["value"]["calltype"] = "halfdial";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "in";
					newEvent["value"]["fmt"] = "";
					newEvent["value"]["ptz"] = "";
					newEvent["value"]["mute"] = "";
					newEvent["eventName"] = "OnCallConnect";
					this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.CONNECTING, event);

				callStatusMgr.holdCall(event["value"]["cid"], reqId);

				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["src"] = "";
				newEvent["value"]["direction"] = "in";
				newEvent["value"]["fmt"] = "";
				newEvent["eventName"] = "OnCallConnect";
				this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);
				break;
			case "2040":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				newEvent["value"]["caller"] = from;
				var callStatusMgr = cloudICP.util.callStatusMgr;
				if (confInfo != undefined) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.NEW, event, confInfo);
					break;
				}
				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.NEW, event);

				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["direction"] = "in";
				newEvent["value"]["src"] = "";
				newEvent["value"]["fmt"] = "";
				newEvent["eventName"] = "OnConnectProceeding";
				this.EVENT_LIST["VoiceNotify"]["OnConnectProceeding"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnConnectProceeding"]);
				break;
			case "2009":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
			case "2011":
				// 停止振铃音
				newEvent["value"]["caller"] = from;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 删除音频媒体通道
				var param =
					"{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					)

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				if (event["rsp"] == "2009" && discreet_listenee != undefined && discreet_listenee != "0" &&
					discreet_listenee != "") {
					var callStatusMgr = cloudICP.util.callStatusMgr;
					callStatusMgr.updateCallStatus(callStatusMgr.DISCREET, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "discreetlisten";
					newEvent["value"]["releasetype"] = "other";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "in";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnCallRelease";
					this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					break;
				};

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VOICECONF && confInfo != undefined) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.RELEASE, event, confInfo);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "false";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnConfRelease";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"]);

					setTimeout(function() {
						var mconfInfo = cloudICP.util.callStatusMgr.confMgr[call["confId"]];
						if (mconfInfo != null && ((mconfInfo["isEnding"] != undefined && mconfInfo[
								"isEnding"] == true) || (mconfInfo["value"]["confStatus"]["status"] ==
								"End"))) {
							return;
						}
						if (cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] == undefined || cloudICP
							.util.callStatusMgr.confSubMgr[call["confId"]] != true) {
							return;
						}
						var ajaxCfg = {
							"type": "POST",
							"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
							"data": {
								"opt": "unsub",
								"reqId": reqId,
								"confId": call["confId"]
							},
							"callback": function(data) {
								if (data["rsp"] != "0") {
									var retEvent = {
										"eventName": "OnUnsubscribeConfFailure",
										"rsp": data["rsp"],
									}
									this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"].event = retEvent;
									sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"]);

								}
							}
						}
						cloudICP.util.ajax(ajaxCfg);
					}, 500);
					break;
				} else if (call && call.callType == callStatusMgr.HALFVOICE) {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "halfdial";
					newEvent["value"]["releasetype"] = "other";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "in";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnCallRelease";
					this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.RELEASE, event);

				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["releasetype"] = "other";
				newEvent["value"]["src"] = "";
				newEvent["value"]["direction"] = "in";
				newEvent["value"]["fmt"] = "";
				newEvent["eventName"] = "OnCallRelease";
				this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
				break;
			case "2010":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				// 停止振铃音
				newEvent["value"]["caller"] = from;
				var param = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}, \"reqId\": \"{1}\"}".format(
					parseInt(value["cid"]), reqId
				);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				// 删除音频媒体通道
				var param =
					"{\"description\":\"msp_delete_audio_channel\", \"cmd\": 65538, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
					.format(
						parseInt(value["cid"]), reqId
					)

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[event["value"]["cid"]];
				if (call && call.callType == callStatusMgr.VOICECONF) {
					callStatusMgr.updateCallStatus(callStatusMgr.VOICECONF, callStatusMgr.RELEASE, event);

					newEvent["value"]["confId"] = call["confId"];
					newEvent["value"]["isVideo"] = "false";
					newEvent["value"]["fmt"] = "";
					newEvent["eventName"] = "OnConfRelease";
					this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfRelease"]);

					setTimeout(function() {
						var mconfInfo = cloudICP.util.callStatusMgr.confMgr[call["confId"]];
						if (mconfInfo != null && ((mconfInfo["isEnding"] != undefined && mconfInfo[
								"isEnding"] == true) || (mconfInfo["value"]["confStatus"]["status"] ==
								"End"))) {
							return;
						}
						if (cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] == undefined || cloudICP
							.util.callStatusMgr.confSubMgr[call["confId"]] != true) {
							return;
						}
						var ajaxCfg = {
							"type": "POST",
							"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
							"data": {
								"opt": "unsub",
								"reqId": reqId,
								"confId": call["confId"]
							},
							"callback": function(data) {
								if (data["rsp"] != "0") {
									var retEvent = {
										"eventName": "OnUnsubscribeConfFailure",
										"rsp": data["rsp"],
									}
									this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"].event = retEvent;
									sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnUnsubscribeConfFailure"]);

								}
							}
						}
						cloudICP.util.ajax(ajaxCfg);
					}, 500);
					break;
				} else if (call && call.callType == callStatusMgr.HALFVOICE) {
					callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RELEASE, event);

					newEvent["value"]["calltype"] = "halfdial";
					newEvent["value"]["releasetype"] = "self";
					newEvent["value"]["fmt"] = "";
					newEvent["value"]["src"] = "";
					newEvent["value"]["direction"] = "in";
					newEvent["eventName"] = "OnCallRelease";
					this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					break;
				}

				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.RELEASE, event);

				newEvent["value"]["calltype"] = "voice";
				newEvent["value"]["releasetype"] = "self";
				newEvent["value"]["fmt"] = "";
				newEvent["value"]["src"] = "";
				newEvent["value"]["direction"] = "in";
				newEvent["eventName"] = "OnCallRelease";
				this.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
				break;
			case "2027":
				// 被人工转接通知
				newEvent["eventName"] = "OnTransferNotify";
				this.EVENT_LIST["VoiceNotify"]["OnTransferNotify"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnTransferNotify"]);
				break;
			case "2012":
				newEvent["eventName"] = "OnTalkingPrempted";
				this.EVENT_LIST["VoiceNotify"]["OnTalkingPrempted"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnTalkingPrempted"]);
				break;
			case "2029":
				newEvent["eventName"] = "OnTalkingEmergencyCallStart";
				this.EVENT_LIST["VoiceNotify"]["OnTalkingEmergencyCallStart"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnTalkingEmergencyCallStart"]);
				break;
			case "2041":
				if (reqId == undefined || reqId == '' || reqId == '0') {
					reqId = mainPageId;
				}
				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.NEW, event);

				newEvent["value"]["calltype"] = "halfdial";
				newEvent["value"]["direction"] = "in";
				newEvent["value"]["src"] = "";
				newEvent["value"]["fmt"] = "";
				newEvent["eventName"] = "OnConnectProceeding";
				this.EVENT_LIST["VoiceNotify"]["OnConnectProceeding"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnConnectProceeding"]);
				break;
			case "2042":
				// 关闭麦克风
				var mediaParam =
					"{\"description\":\"msp_mute_audio_channel\", \"cmd\": 65549, \"param\":{\"resID\": {0}, \"mute\":true}}"
					.format(
						parseInt(value["cid"])
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.CONNECTING, event);

				newEvent["eventName"] = "OnStartRecvHalfDial";
				this.EVENT_LIST["VoiceNotify"]["OnStartRecvHalfDial"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnStartRecvHalfDial"]);
				break;
			case "2043":
				// 关闭麦克风
				var mediaParam =
					"{\"description\":\"msp_mute_audio_channel\", \"cmd\": 65549, \"param\":{\"resID\": {0}, \"mute\":true}}"
					.format(
						parseInt(value["cid"])
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam);

				// var callStatusMgr = cloudICP.util.callStatusMgr;
				// callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RELEASE, event);

				newEvent["eventName"] = "OnStopRecvHalfDial";
				this.EVENT_LIST["VoiceNotify"]["OnStopRecvHalfDial"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnStopRecvHalfDial"]);
				break;
			case "2045":
				// 打开麦克风
				var mediaParam =
					"{\"description\":\"msp_mute_audio_channel\", \"cmd\": 65549, \"param\":{\"resID\": {0}, \"mute\":false}}"
					.format(
						parseInt(value["cid"])
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.CONNECTING, event);

				newEvent["eventName"] = "OnHalfDialSuccess";
				this.EVENT_LIST["VoiceNotify"]["OnHalfDialSuccess"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnHalfDialSuccess"]);
				break;
			case "2046":
				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.HALFVOICE, callStatusMgr.RELEASE, event);

				newEvent["eventName"] = "OnHalfDialFailure";
				this.EVENT_LIST["VoiceNotify"]["OnHalfDialFailure"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnHalfDialFailure"]);
				break;
			case "2054":
				newEvent["eventName"] = "OnHoldSuccess";
				this.EVENT_LIST["VoiceNotify"]["OnHoldSuccess"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnHoldSuccess"]);
				break;
			case "2055":
				newEvent["eventName"] = "OnUnholdSuccess";
				this.EVENT_LIST["VoiceNotify"]["OnUnholdSuccess"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnUnholdSuccess"]);
				break;
			case "2056":
				newEvent["eventName"] = "OnReceiveDTMF";
				this.EVENT_LIST["VoiceNotify"]["OnReceiveDTMF"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnReceiveDTMF"]);
				break;
			case "2057": //mrs notify
				newEvent["eventName"] = "MRSInfoNotify";
				this.EVENT_LIST["VoiceNotify"]["MRSInfoNotify"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["MRSInfoNotify"]);
				break;
			case "2058": //voice linkphone notify
				newEvent["eventName"] = "OnLinkedPhoneNotify";
				this.EVENT_LIST["VoiceNotify"]["OnLinkedPhoneNotify"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnLinkedPhoneNotify"]);
		}
	};
}
function ICPSDK_Dispatch_GIS() {
	this.MAX_SUB_LIST = 200;
	this.MAX_ERPRIED_TIME = 120;

	this.MAX_SUB_NUMBER = 8000;

	// 保持订阅参数
	this.DEFAULT_EXPIRE_TIME = "90";
	this.INTERVAL_TIME = 3000000;

	this.subList = [];

	var me = this;

	// 由于gis订阅过期后不会立马刷新，经过讨论决定通过重复订阅的方式，将之保持订阅，直到被取消订阅
	setInterval(function() {
		var uelist = []
		for (var index = 0; index < me.subList.length; index++) {
			if (uelist.length < 190) {
				uelist.push({
					"isdn": me.subList[index]
				});
			} else {
				me.subscribeGIS({
					"uelist": uelist,
					"callback": function() {}
				});

				uelist = [];
			}
		}

		if (uelist.length != 0) {
			me.subscribeGIS({
				"uelist": uelist,
				"callback": function() {}
			});

			uelist = [];
		}
	}, me.INTERVAL_TIME);
};

/**
 * subscribeGIS
 * @param {
 *  uelist :
 *  callback :
 * }
 */
ICPSDK_Dispatch_GIS.prototype.subscribeGIS = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "subscribeGIS: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "subscribeGIS: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "subscribeGIS: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";
	var requestParam = {};
	if (!(param["uelist"] && Array.isArray(param["uelist"]) && param["uelist"].length > 0 && param["uelist"]
			.length <= this.MAX_SUB_LIST)) {
		result["desc"] = "subscribeGIS: uelist is invalid";
		param["callback"](result);
		return;
	}

	var length = param["uelist"].length;

	if (length + this.subList.length > this.MAX_SUB_NUMBER) {
		result["rsp"] = "-4";
		result["desc"] = "subscribeGIS: The number of usbscribe user is limited to " + this.MAX_SUB_NUMBER;
		param["callback"](result);
		return;
	}

	for (var i = 0; i < length; i++) {
		var user = param["uelist"][i];
		if (!cloudICP.util.checkIsdn(user["isdn"])) {
			result["desc"] = "subscribeGIS: isdn is invalid";
			param["callback"](result);
			return;
		}
	}

	requestParam["expiredtime"] = this.DEFAULT_EXPIRE_TIME;
	requestParam["uelist"] = param["uelist"];
	requestParam["reqId"] = tabPageId;

	var url = cloudICP.getSdkServerUrl() + "/v1/gis/" + cloudICP.userInfo["isdn"] + "/sub";
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);

}

/**
 * unsubscribeGIS
 * @param {
 *  uelist :
 *  callback :
 * }
 */
ICPSDK_Dispatch_GIS.prototype.unsubscribeGIS = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unsubscribeGIS: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unsubscribeGIS: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "unsubscribeGIS: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";
	var requestParam = {};
	if (!(param["uelist"] && Array.isArray(param["uelist"]) && param["uelist"].length > 0 && param["uelist"]
			.length <= this.MAX_SUB_LIST)) {
		result["desc"] = "unsubscribeGIS: uelist is invalid";
		param["callback"](result);
		return;
	}

	var length = param["uelist"].length;
	for (var i = 0; i < length; i++) {
		var user = param["uelist"][i];
		if (!cloudICP.util.checkIsdn(user["isdn"])) {
			result["desc"] = "unsubscribeGIS: isdn is invalid";
			param["callback"](result);
			return;
		}
	}

	requestParam["uelist"] = param["uelist"];
	requestParam["reqId"] = tabPageId;

	var url = cloudICP.getSdkServerUrl() + "/v1/gis/" + cloudICP.userInfo["isdn"] + "/unsub";
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}


/**
 * query GIS SubList
 * @param {
 *  callback :
 * }
 */
ICPSDK_Dispatch_GIS.prototype.queryGISSubList = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryGISSubList: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "queryGISSubList: allback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryGISSubList: The user does not logined";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/gis/" + cloudICP.userInfo["isdn"] + "/sublist" + "?reqId=" + tabPageId;
	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
};
function ICPSDK_Dispatch_Group() {
	this.MAX_DYNAMICGROUP_ID = 9999999999999;
	this.MIN_DYNAMICGROUP_ID = 1;
	this.MAX_DYNAMICGROUP_MAXPERIOD = 65535;
	this.MAX_DYNAMICGROUP_PRIORITY = 255;
	this.MAX_DYNAMICGROUP_GROUPLIST_LENGTH = 8;
	this.MAX_DYNAMICGROUP_USERLIST_LENGTH = 200;
	this.MAX_PATCHGROUP_NAME_LENGTH = 32;
	this.MAX_PATCHGROUP_USERLIST_LENGTH = 20;
	// 最大支持组呼并发数
	this.MAX_GROUP_CALL_NUM = 30;

	// 当前组呼数量
	this.current_groupcall_num = 0;

	this.isCurrentPtt = {
		"grpid": "-1",
		"isPtt": false
	};
};

/**
 * 新增动态群组
 * @param param {
 *  "alias" : ,
 *  "grpid" : ,
 *  "maxperiod":,
 *  "priority" : ,
 *  "grouplist" :
 *  "uelist" :
 *  "callback"
 * }
 */
ICPSDK_Dispatch_Group.prototype.addDynamicGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "addDynamicGroup: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "addDynamicGroup: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "addDynamicGroup: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";
	var requestParam = {
		"opt": "create",
		"param": {}
	};
	if (!cloudICP.util.checkAliasOfGroup(param["alias"])) {
		result["desc"] = "addDynamicGroup: The alias is invalid";
		param["callback"](result);
		return;
	}
	requestParam["param"]["alias"] = param["alias"];
	requestParam["param"]["dcid"] = cloudICP.userInfo["isdn"];
	if (!(cloudICP.util.isNumber(param["grpid"]) &&
			(parseInt(param["grpid"]) == 0 || (parseInt(param["grpid"]) >= this.MIN_DYNAMICGROUP_ID &&
				parseInt(param["grpid"]) <= this.MAX_DYNAMICGROUP_ID)))) {
		result["desc"] = "addDynamicGroup: The grpid is invalid";
		param["callback"](result);
		return;
	}
	requestParam["param"]["grpid"] = param["grpid"];

	if (!(cloudICP.util.isNumber(param["maxperiod"]) &&
			(parseInt(param["maxperiod"]) >= 1 && parseInt(param["maxperiod"]) <= this.MAX_DYNAMICGROUP_MAXPERIOD)
		)) {
		result["desc"] = "addDynamicGroup: The maxperiod is invalid";
		param["callback"](result);
		return;
	}
	requestParam["param"]["maxperiod"] = param["maxperiod"];

	if (!(cloudICP.util.isNumber(param["priority"]) &&
			(parseInt(param["priority"]) >= 1 && parseInt(param["priority"]) <= this.MAX_DYNAMICGROUP_PRIORITY))) {
		result["desc"] = "addDynamicGroup: The priority is invalid";
		param["callback"](result);
		return;
	}
	requestParam["param"]["priority"] = param["priority"];


	if (!Array.isArray(param["grouplist"]) || !Array.isArray(param["uelist"])) {
		result["desc"] = "addDynamicGroup: The grouplist or uelist is not array";
		param["callback"](result);
		return;
	}

	if (param["grouplist"].length == 0 && param["uelist"].length == 0) {
		result["desc"] = "addDynamicGroup: The grouplist or uelist must have one isdn at least";
		param["callback"](result);
		return;
	}

	if (param["grouplist"].length > this.MAX_DYNAMICGROUP_GROUPLIST_LENGTH) {
		result["desc"] = "addDynamicGroup: grouplist is invalid";
		param["callback"](result);
		return;
	}
	var length = param["grouplist"].length;
	for (var i = 0; i < length; i++) {
		var group = param["grouplist"][i];
		if (!cloudICP.util.checkGroupID(group["isdn"])) {
			result["desc"] = "addDynamicGroup: isdn is invalid in grouplist";
			param["callback"](result);
			return;
		}
	}
	requestParam["param"]["grouplist"] = param["grouplist"];

	if (param["uelist"].length > this.MAX_DYNAMICGROUP_USERLIST_LENGTH) {
		result["desc"] = "addDynamicGroup: grouplist is invalid";
		param["callback"](result);
		return;
	}
	var length = param["uelist"].length;
	for (var i = 0; i < length; i++) {
		var user = param["uelist"][i];
		if (!cloudICP.util.checkIsdn(user["isdn"])) {
			result["desc"] = "addDynamicGroup: isdn is invalid in uelist";
			param["callback"](result);
			return;
		}
	}
	requestParam["param"]["uelist"] = param["uelist"];
	requestParam["param"]["reqId"] = tabPageId;

	var url = cloudICP.getSdkServerUrl() + "/v1/group/" + cloudICP.userInfo["isdn"] + "/dynamicgroup";
	var ajaxCfg = {
		"type": "PUT",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * 删除动态群组
 * @param param {
 *  "grpid" : ,
 *  "callback"
 * }
 */
ICPSDK_Dispatch_Group.prototype.deleteDynamicGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "deleteDynamicGroup: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "deleteDynamicGroup: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "deleteDynamicGroup: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= this.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= this.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "deleteDynamicGroup: The grpid is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/group/" + cloudICP.userInfo["isdn"] + "/dynamicgroup/cancel/" + param["grpid"];
	var requestParam = {};
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "DELETE",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * 修改动态群组
 * @param param {
 *  "alias" : ,
 *  "grpid" : ,
 *  "maxperiod":,
 *  "priority" : ,
 *  "grouplist" :
 *  "uelist" :
 *  "callback"
 * }
 */
ICPSDK_Dispatch_Group.prototype.modifyDynamicGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "modifyDynamicGroup: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "modifyDynamicGroup: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "modifyDynamicGroup: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";
	var requestParam = {
		"opt": "modify",
		"param": {}
	};
	requestParam["param"]["dcid"] = cloudICP.userInfo["isdn"];
	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= this.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= this.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "modifyDynamicGroup: The grpid is invalid";
		param["callback"](result);
		return;
	}
	requestParam["param"]["grpid"] = param["grpid"];

	if (!Array.isArray(param["addlist"]) || !Array.isArray(param["dellist"])) {
		result["desc"] = "modifyDynamicGroup: The addlist or dellist is not array";
		param["callback"](result);
		return;
	}

	if (param["dellist"].length == 0 && param["addlist"].length == 0) {
		result["desc"] = "modifyDynamicGroup: The addlist or dellist must have one isdn at least";
		param["callback"](result);
		return;
	}

	if (param["addlist"].length > this.MAX_DYNAMICGROUP_USERLIST_LENGTH) {
		result["desc"] = "modifyDynamicGroup: addlist is invalid";
		param["callback"](result);
		return;
	}
	var length = param["addlist"].length;
	for (var i = 0; i < length; i++) {
		var user = param["addlist"][i];
		if (!cloudICP.util.checkIsdn(user["isdn"])) {
			result["desc"] = "modifyDynamicGroup: isdn is invalid in grouplist in addlist";
			param["callback"](result);
			return;
		}
	}
	requestParam["param"]["addlist"] = param["addlist"];

	if (param["dellist"].length > this.MAX_DYNAMICGROUP_USERLIST_LENGTH) {
		result["desc"] = "modifyDynamicGroup: dellist is invalid";
		param["callback"](result);
		return;
	}
	var length = param["dellist"].length;
	for (var i = 0; i < length; i++) {
		var user = param["dellist"][i];
		if (!cloudICP.util.checkIsdn(user["isdn"])) {
			result["desc"] = "modifyDynamicGroup: isdn is invalid in dellist";
			param["callback"](result);
			return;
		}
	}
	requestParam["param"]["dellist"] = param["dellist"];

	cloudICP.dispatch.query.queryDynamicGroupMembers({
		"grpid": requestParam["param"]["grpid"],
		"callback": function(data) {
			if (data["rsp"] != "-1" && data["list"] != null) {
				var memberlist = data["list"];
				var members = [];
				for (var index = 0; index < memberlist.length; index++) {
					members.push(memberlist[index]["isdn"]);
				}

				for (var index = 0; index < requestParam["param"]["dellist"].length; index++) {
					var isdn = requestParam["param"]["dellist"][index]["isdn"];

					if (!cloudICP.util.includes(members, isdn)) {
						result["rsp"] = "-2";
						result["desc"] = "modifyDynamicGroup: dellist is invalid.";
						param["callback"](result);
						return;
					}
				}

				for (var index = 0; index < requestParam["param"]["addlist"].length; index++) {
					var isdn = requestParam["param"]["addlist"][index]["isdn"];

					if (cloudICP.util.includes(members, isdn)) {
						result["rsp"] = "-2";
						result["desc"] = "modifyDynamicGroup: addlist is invalid.";
						param["callback"](result);
						return;
					}
				}

				var url = cloudICP.getSdkServerUrl() + "/v1/group/" + cloudICP.userInfo["isdn"] + "/dynamicgroup";
				requestParam["param"]["reqId"] = tabPageId;
				var ajaxCfg = {
					"type": "POST",
					"url": url,
					"data": requestParam,
					"callback": param["callback"]
				}
				cloudICP.util.ajax(ajaxCfg);

			} else {
				result["rsp"] = "-2";
				result["desc"] = "modifyDynamicGroup: grpid is invalid";
				param["callback"](result);
				return;
			}
		}
	})
}

/**
 * 新增派接组
 */
ICPSDK_Dispatch_Group.prototype.addPatchGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}

	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "addPatchGroup: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "addPatchGroup: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "addPatchGroup: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";
	var requestParam = {
		"opt": "create",
		"param": {}
	};

	if (param["name"] == undefined || param["name"] == null) {
		result["desc"] = "addPatchGroup: name is invalid";
		param["callback"](result);
		return;
	}

	if (param["name"].length > this.MAX_PATCHGROUP_NAME_LENGTH) {
		result["desc"] = "addPatchGroup: name is invalid";
		param["callback"](result);
		return;
	}

	var regex = /^[\u4e00-\u9fa5_0-9A-Za-z]+$/;
	if (param["name"] != "" && !regex.test(param["name"])) {
		result["desc"] = "addPatchGroup: name is invalid";
		param["callback"](result);
		return;
	}
	requestParam["param"]["name"] = param["name"];

	var url = cloudICP.getSdkServerUrl() + "/v1/group/" + cloudICP.userInfo["isdn"] + "/patchgroup";
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "PUT",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * 删除派接组
 * @param param {
 *  "grpid" : ,
 *  "callback"
 * }
 */
ICPSDK_Dispatch_Group.prototype.deletePatchGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "deletePatchGroup: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "deletePatchGroup: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "deletePatchGroup: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= this.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= this.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "deletePatchGroup: The grpid is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/group/" + cloudICP.userInfo["isdn"] + "/patchgroup/cancel/" + param["grpid"];
	var requestParam = {};
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "DELETE",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}


/**
 * 新增派接组成员
 * @param param {
 *  "grpid" : ,
 *  "callback"
 * }
 */
ICPSDK_Dispatch_Group.prototype.addPatchGroupMember = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "addPatchGroupMember: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "addPatchGroupMember: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "addPatchGroupMember: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";

	var requestParam = {
		"opt": "add",
		"param": {}
	};
	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= this.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= this.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "addPatchGroupMember: The grpid is invalid";
		param["callback"](result);
		return;
	}
	requestParam["param"]["grpid"] = param["grpid"];

	if (!Array.isArray(param["memberlist"]) ||
		param["memberlist"].length == 0 || param["memberlist"].length > this.MAX_PATCHGROUP_USERLIST_LENGTH) {
		result["desc"] = "addPatchGroupMember: memberlist is invalid";
		param["callback"](result);
		return;
	}
	var length = param["memberlist"].length;
	for (var i = 0; i < length; i++) {
		var user = param["memberlist"][i];
		if (!cloudICP.util.checkGroupID(user["isdn"])) {
			result["desc"] = "addPatchGroupMember: isdn is invalid in memberlist";
			param["callback"](result);
			return;
		}
	}
	requestParam["param"]["memberlist"] = param["memberlist"];

	var url = cloudICP.getSdkServerUrl() + "/v1/group/" + cloudICP.userInfo["isdn"] + "/patchgroup";
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}



/**
 * 删除派接组成员
 * @param param {
 *  "grpid" : ,
 *  "callback"
 * }
 */
ICPSDK_Dispatch_Group.prototype.deletePatchGroupMember = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "deletePatchGroupMember: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "deletePatchGroupMember: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "deletePatchGroupMember: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";

	var requestParam = {
		"opt": "del",
		"param": {}
	};
	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= this.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= this.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "deletePatchGroupMember: The grpid is invalid";
		param["callback"](result);
		return;
	}
	requestParam["param"]["grpid"] = param["grpid"];

	if (!Array.isArray(param["memberlist"]) ||
		param["memberlist"].length == 0 || param["memberlist"].length > this.MAX_PATCHGROUP_USERLIST_LENGTH) {
		result["desc"] = "deletePatchGroupMember: memberlist is invalid";
		param["callback"](result);
		return;
	}
	var length = param["memberlist"].length;
	for (var i = 0; i < length; i++) {
		var user = param["memberlist"][i];
		if (!cloudICP.util.checkGroupID(user["isdn"])) {
			result["desc"] = "deletePatchGroupMember: isdn is invalid in memberlist";
			param["callback"](result);
			return;
		}
	}
	requestParam["param"]["memberlist"] = param["memberlist"];

	var url = cloudICP.getSdkServerUrl() + "/v1/group/" + cloudICP.userInfo["isdn"] + "/patchgroup";
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * subscribeTalkingGroup() subscribe talking group
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.subscribeTalkingGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"subscribeTalkingGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"subscribeTalkingGroup": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "subscribeTalkingGroup: User not logged.";
		param["callback"](result);
		return;
	};

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "subscribeTalkingGroup: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "sub";
	resParam["to"] = param["grpid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}


/**
 * unsubscribeTalkingGroup() unsubscribe talking group
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.unsubscribeTalkingGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"unsubscribeTalkingGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"unsubscribeTalkingGroup": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "unsubscribeTalkingGroup: User not logged.";
		param["callback"](result);
		return;
	};

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "unsubscribeTalkingGroup: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "unsub";
	resParam["to"] = param["grpid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * subjoinTalkingGroup() subjoin talking group
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.subjoinTalkingGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"subjoinTalkingGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"subjoinTalkingGroup": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "subjoinTalkingGroup: User not logged.";
		param["callback"](result);
		return;
	};

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "subjoinTalkingGroup: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "subjoin";
	resParam["to"] = param["grpid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * joinTalkingGroup() subjoin talking group
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.joinTalkingGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"joinTalkingGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"joinTalkingGroup": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "joinTalkingGroup: User not logged.";
		param["callback"](result);
		return;
	};

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "joinTalkingGroup: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "join";
	resParam["to"] = param["grpid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * pttTalkingGroup() ptt talking group
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.pttTalkingGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"pttTalkingGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"pttTalkingGroup": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "pttTalkingGroup: User not logged.";
		param["callback"](result);
		return;
	};

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "pttTalkingGroup: grpid is invalid.";
		param["callback"](result);
		return;
	}

	if (cloudICP.dispatch.group.current_groupcall_num >= cloudICP.dispatch.group.MAX_GROUP_CALL_NUM) {
		result["rsp"] = "-4";
		result["desc"] = "pttTalkingGroup: Exceeded the maximum number of group calls.";
		param["callback"](result);
		return;
	}

	if (cloudICP.dispatch.group.isCurrentPtt["isPtt"]) {
		result["rsp"] = "-5";
		result["desc"] = "pttTalkingGroup: The number of ptt is limited to 1.";
		param["callback"](result);
		return;
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE].length != 0) {
		result["rsp"] = "-6";
		result["desc"] = "pttTalkingGroup: The group call ptt is confliced with halfdial.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "snatch";
	resParam["to"] = param["grpid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * pttreleaseTalkingGroup() ptt release talking group
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.pttreleaseTalkingGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"pttreleaseTalkingGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"pttreleaseTalkingGroup": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "pttreleaseTalkingGroup: User not logged.";
		param["callback"](result);
		return;
	};

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "pttreleaseTalkingGroup: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "release";
	resParam["to"] = param["grpid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * leaveTalkingGroup() ptt release talking group
 * @param {Object} param {
 *  "cid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.leaveTalkingGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"leaveTalkingGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"leaveTalkingGroup": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "leaveTalkingGroup: User not logged.";
		param["callback"](result);
		return;
	};

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "leaveTalkingGroup: The cid is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "hangup";
	resParam["cid"] = param["cid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * pttTalkingGroupEmergencyCall() ptt talking group emergency call
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.pttTalkingGroupEmergencyCall = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"pttTalkingGroupEmergencyCall": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"pttTalkingGroupEmergencyCall": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "pttTalkingGroupEmergencyCall: User not logged.";
		param["callback"](result);
		return;
	};

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "pttTalkingGroupEmergencyCall: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "emergency";
	resParam["to"] = param["grpid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * breakoffTalkingGroup() break off talking group
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.breakoffTalkingGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"breakoffTalkingGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"breakoffTalkingGroup": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "breakoffTalkingGroup: User not logged.";
		param["callback"](result);
		return;
	};

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "breakoffTalkingGroup: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"] + "/breakoff";
	resParam["opt"] = "group";
	resParam["to"] = param["grpid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * addTalkingGroupTempUser() add talking group temp user
 * @param {Object} param {
 *  "grpid": ,
 *  "userid" ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.addTalkingGroupTempUser = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"addTalkingGroupTempUser": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"addTalkingGroupTempUser": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "addTalkingGroupTempUser: User not logged.";
		param["callback"](result);
		return;
	};

	if (!cloudICP.util.checkIsdn(param["userid"])) {
		result["rsp"] = "-2";
		result["desc"] = "addTalkingGroupTempUser: userid is invalid";
		param["callback"](result);
		return;
	}

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "addTalkingGroupTempUser: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "adduser";
	resParam["to"] = param["grpid"];
	resParam["userid"] = param["userid"];
	resParam["reqId"] = tabPageId;

	if (undefined != param["waitcallresult"] && "0" != param["waitcallresult"]) {
        resParam["waitcallresult"] = "1";
    } else {
        resParam["waitcallresult"] = "0";
    }

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * breakinTalkingGroup() break in talking group
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.breakinTalkingGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"breakinTalkingGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"breakinTalkingGroup": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "breakinTalkingGroup: User not logged.";
		param["callback"](result);
		return;
	};

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "breakinTalkingGroup: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "breakin";
	resParam["to"] = param["grpid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * getPDTGroupInfo() get pdt group info
 * @param {Object} param {
 *  "operation": ,
 *  "userid" ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.getPDTGroupInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"getPDTGroupInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"getPDTGroupInfo": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "getPDTGroupInfo: User not logged.";
		param["callback"](result);
		return;
	};

	if (!cloudICP.util.checkIsdn(param["userid"])) {
		result["rsp"] = "-2";
		result["desc"] = "getPDTGroupInfo: userid is invalid";
		param["callback"](result);
		return;
	}

	if (!param["operation"] || typeof param["operation"] != "string") {
		result["desc"] = "getPDTGroupInfo: operation is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "pdt_query";
	resParam["operation"] = param["operation"];
	resParam["userId"] = param["userid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * setListenGroup() get pdt group info
 * @param {Object} param {
 *  "operation": ,
 *  "userid" ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Group.prototype.setListenGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"setListenGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"setListenGroup": "callback is not a function"
		});
		return;
	}

	var resParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "setListenGroup: User not logged.";
		param["callback"](result);
		return;
	};

	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["rsp"] = "-2";
		result["desc"] = "setListenGroup: grpid is invalid.";
		param["callback"](result);
		return;
	}

	if (!param["operation"] || typeof param["operation"] != "string") {
		result["desc"] = "setListenGroup: operation is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/groupcall/" + cloudICP.userInfo["isdn"];
	resParam["opt"] = "set_listen";
	resParam["operation"] = param["operation"];
	resParam["grpid"] = param["grpid"];
	resParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": resParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};
function ICPSDK_Dispatch_Query() {};
var PAGE_QUERY_LIMIT_MAX = 5000;
/**
 * queryTalkingGroup() query talking group
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryTalkingGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroup": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryTalkingGroup: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/group/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryTalkingGroupV1() query talking group
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryTalkingGroupV1 = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroupV1": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroupV1": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryTalkingGroupV1: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";

	if (undefined == param["offset"]) {
		param["offset"] = "0";
	}

	if (undefined == param["limit"]) {
		param["limit"] = "1000";
	}

	if (!(cloudICP.util.isNumber(param["category"])) ) {
        param["category"] = "10";
    }

	if (!(cloudICP.util.isNumber(param["offset"]))) {
		result["desc"] = "queryTalkingGroupV1: offset invalid.";
		param["callback"](result);
		return;
	}

	if (!(cloudICP.util.isNumber(param["limit"])) || param["limit"] > PAGE_QUERY_LIMIT_MAX) {
		result["desc"] = "queryTalkingGroupV1: limit invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v2/" + cloudICP.userInfo["isdn"] + "/group?offset={0}&limit={1}&reqId={2}&category={3}"
		.format(param["offset"], param["limit"], tabPageId, param["category"]);


	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryTalkingGroupMembers() query talking group members
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryTalkingGroupMembers = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroupMembers": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroupMembers": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryTalkingGroupMembers: User not logged.";
		param["callback"](result);
		return;
	};

	result["rsp"] = "-2";
	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "queryTalkingGroupMembers: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/" + param["grpid"] + "/groupmember/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryTalkingGroupMembersV1() query talking group members
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryTalkingGroupMembersV1 = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroupMembersV1": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroupMembersV1": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryTalkingGroupMembersV1: User not logged.";
		param["callback"](result);
		return;
	};

	result["rsp"] = "-2";
	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "queryTalkingGroupMembersV1: grpid is invalid.";
		param["callback"](result);
		return;
	}

	if (undefined == param["offset"]) {
		param["offset"] = "0";
	}

	if (undefined == param["limit"]) {
		param["limit"] = "1000";
	}

	if (!(cloudICP.util.isNumber(param["offset"]))) {
		result["desc"] = "queryTalkingGroupMembersV1: offset is invalid.";
		param["callback"](result);
		return;
	}

	if (!(cloudICP.util.isNumber(param["limit"])) || param["limit"] > PAGE_QUERY_LIMIT_MAX) {
		result["desc"] = "queryTalkingGroupMembersV1: limit is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v2/" + cloudICP.userInfo["isdn"] +
		"/groupmember?number={0}&offset={1}&limit={2}&reqId={3}".format(
			param["grpid"], param["offset"], param["limit"], tabPageId);

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryDynamicGroupMembers() query talking group members
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryDynamicGroupMembers = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDynamicGroupMembers": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDynamicGroupMembers": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryDynamicGroupMembers: User not logged.";
		param["callback"](result);
		return;
	};

	result["rsp"] = "-2";
	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "queryDynamicGroupMembers: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/" + param["grpid"] +
		"/dynamicgroupmember/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}


/**
 * queryDynamicGroup() query dynamic group
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryDynamicGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDynamicGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDynamicGroup": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryDynamicGroup: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/dynamicgroup/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			if (undefined != data["list"] && data["list"] != null) {
				var index = 0;
				for (var index = 0; index < data["list"].length; index++) {
					if (undefined != data["list"][index]["speakerfixed"]) {
						delete data["list"][index]["speakerfixed"];
					}
				}
			}

			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryStaticGroup() query static group
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryStaticGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryStaticGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryStaticGroup": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryStaticGroup: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/staticgroup/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryStaticGroupV1() query static group
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryStaticGroupV1 = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryStaticGroupV1": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryStaticGroupV1": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryStaticGroupV1: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";

	if (undefined == param["offset"]) {
		param["offset"] = "0";
	}

	if (undefined == param["limit"]) {
		param["limit"] = "1000";
	}

	if (!(cloudICP.util.isNumber(param["offset"]))) {
		result["desc"] = "queryStaticGroupV1: offset invalid.";
		param["callback"](result);
		return;
	}

	if (!(cloudICP.util.isNumber(param["limit"])) || param["limit"] > PAGE_QUERY_LIMIT_MAX ) {
		result["desc"] = "queryStaticGroupV1: limit invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v2/" + cloudICP.userInfo["isdn"] + "/staticgroup?offset={0}&limit={1}&reqId={2}"
		.format(
			param["offset"], param["limit"], tabPageId);

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryPatchGroup() query patch group
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryPatchGroup = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryPatchGroup": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryPatchGroup": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryPatchGroup: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/patchgroup/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryPatchGroupMembers() query patch group members
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryPatchGroupMembers = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryPatchGroupMembers": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryPatchGroupMembers": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryPatchGroupMembers: User not logged.";
		param["callback"](result);
		return;
	};

	result["rsp"] = "-2";
	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "queryPatchGroupMembers: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/" + param["grpid"] +
		"/patchgroupmember/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryUserList() query user
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryUserList = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryUserList": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryUserList": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryUserList: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/user/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryUserListV1() query user
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryUserListV1 = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryUserListV1": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryUserListV1": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryUserListV1: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";

	if (undefined == param["offset"]) {
		param["offset"] = "0";
	}

	if (undefined == param["limit"]) {
		param["limit"] = "1000";
	}

	if ( !(cloudICP.util.isNumber(param["category"])) ) {
        param["category"] = "-1";
    }

	if (!(cloudICP.util.isNumber(param["offset"]))) {
		result["desc"] = "queryUserListV1: offset invalid.";
		param["callback"](result);
		return;
	}

	if (!(cloudICP.util.isNumber(param["limit"])) || param["limit"] > PAGE_QUERY_LIMIT_MAX) {
		result["desc"] = "queryUserListV1: limit invalid.";
		param["callback"](result);
		return;
	}
	let departmentId = "";
	if ((undefined != param["departmentid"]) && param["departmentid"] != "") {
		if (!(cloudICP.util.isNumber(param["departmentid"]))|| param["departmentid"].length > 18) {
			result["desc"] = "queryUserListV1: departmentid invalid.";
			param["callback"](result);
			return;
		} else {
			departmentId = param["departmentid"];
		}
	}

	var url = cloudICP.getSdkServerUrl() + "/v2/" + cloudICP.userInfo["isdn"] + "/user?offset={0}&limit={1}&reqId={2}&category={3}&departmentid={4}".format(
		param["offset"], param["limit"], tabPageId, param["category"], departmentId);

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryCameras() query cameras
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryCameras = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameras": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameras": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryCameras: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/camera/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

ICPSDK_Dispatch_Query.prototype.queryCamerasV1 = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;

	if (param["value"]) {
		var param = param["value"];
	}

    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "queryCamerasV1": "param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "queryCamerasV1": "callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryCamerasV1: User not logged.";
        param["callback"](result);
        return;
    }

	result["rsp"] = "-2";

	if (undefined == param["offset"]) {
		param["offset"] = "0";
	}

	if (undefined == param["limit"]) {
		param["limit"] = "1000";
	}

    if ( !(cloudICP.util.isNumber(param["offset"])) ) {
        result["desc"] = "queryCamerasV1: offset invalid.";
		param["callback"](result);
		return;
    }

    if ( !(cloudICP.util.isNumber(param["limit"])) || param["limit"] > PAGE_QUERY_LIMIT_MAX) {
        result["desc"] = "queryCamerasV1: limit invalid.";
		param["callback"](result);
		return;
    }

    var url = cloudICP.getSdkServerUrl() + "/v2/" + cloudICP.userInfo["isdn"] + "/camera?offset={0}&limit={1}&reqId={2}".format(
        param["offset"], param["limit"], tabPageId);

    var ajaxCfg = {
        "type": "GET",
        "url": url,
        "async": true,
        "data": null,
        "callback": function(data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
}

/**
 * query cameras at a specified level.
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.querySpecifiedLevelCamera = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"querySpecifiedLevelCamera": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"querySpecifiedLevelCamera": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "querySpecifiedLevelCamera: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!(cloudICP.util.isNumber(param["levelnumber"]))) {
		result["desc"] = "querySpecifiedLevelCamera: levelnumber is invalid.";
		param["callback"](result);
		return;
	}

	if (undefined == param["offset"]) {
		param["offset"] = "0";
	}

	if (!(cloudICP.util.isNumber(param["offset"]))) {
		result["desc"] += " querySpecifiedLevelCamera: offset is invalid";
		param["callback"](result);
		return;
	}

	if (undefined == param["limit"]) {
		param["limit"] = "1000";
	}

	if (!(cloudICP.util.isNumber(param["limit"]))) {
		result["desc"] += " querySpecifiedLevelCamera: limit is invalid";
		param["callback"](result);
		return;
	} else {
		var iLimit = parseInt(param["limit"]);
		if (1 > iLimit || 5000 < iLimit) {
			result["desc"] += " querySpecifiedLevelCamera: limit not in [1,5000]";
			param["callback"](result);
			return;
		}
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] +
		"/querylevelcameras";

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": true,
		"data": param,
		"callback": function(data) {
			data["desc"] += result["desc"];
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}


/**
 * queryDecoder() query decoder
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryDecoder = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDecoder": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDecoder": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryDecoder: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/decoder/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryDepartmentList() query department list
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryDepartmentList = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDepartmentList": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDepartmentList": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryDepartmentList: User not logged.";
		param["callback"](result);
		return;
	}

	if (undefined == param["name"]) {
        param["name"] = "";
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/department" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": param,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryCameraLevel() query camera level
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryCameraLevel = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameraLevel": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameraLevel": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryCameraLevel: User not logged.";
		param["callback"](result);
		return;
	}

	if (undefined == param["name"]) {
        param["name"] = "";
    }

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/cameralevel" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": param,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryCameraLevelPermission() query camera level permission
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryCameraLevelPermission = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameraLevelPermission": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameraLevelPermission": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryCameraLevelPermission: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/cameralevelpermission/-1" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryTalkingGroupInfo() query talking group info
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryTalkingGroupInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroupInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroupInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryTalkingGroupInfo: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "queryTalkingGroupInfo: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/queryattribute/" + cloudICP.userInfo["isdn"] + "/group/" + param["grpid"] + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryPatchGroupInfo() query patch group info
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryPatchGroupInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryPatchGroupInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryPatchGroupInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryPatchGroupInfo: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "queryPatchGroupInfo: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/queryattribute/" + cloudICP.userInfo["isdn"] + "/patchgroup/" + param["grpid"] + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryTalkingGroupPatchedInfo() query patched info
 * @param {Object} param {
 *  "grpid": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryTalkingGroupPatchedInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroupPatchedInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryTalkingGroupPatchedInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryTalkingGroupPatchedInfo: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!(cloudICP.util.isNumber(param["grpid"]) && (parseInt(param["grpid"]) >= cloudICP.dispatch.group
			.MIN_DYNAMICGROUP_ID &&
			parseInt(param["grpid"]) <= cloudICP.dispatch.group.MAX_DYNAMICGROUP_ID))) {
		result["desc"] = "queryTalkingGroupPatchedInfo: grpid is invalid.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/queryattribute/" + cloudICP.userInfo["isdn"] + "/patched/" + param["grpid"] + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryUserInfo() query user info
 * @param {Object} param {
 *  "isdn": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryUserInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryUserInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryUserInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryUserInfo: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["isdn"])) {
		result["desc"] = "queryUserInfo: isdn is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/queryattribute/" + cloudICP.userInfo["isdn"] + "/user/" + param["isdn"] + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryCameraInfo() query camera info
 * @param {Object} param {
 *  "isdn": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryCameraInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameraInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameraInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryCameraInfo: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["isdn"])) {
		result["desc"] = "queryCameraInfo: isdn is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/queryattribute/" + cloudICP.userInfo["isdn"] + "/camera/" + param["isdn"] + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryCameraPermissionInfo() query camera permission info
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryCameraPermissionInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameraPermissionInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameraPermissionInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryCameraPermissionInfo: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/queryattribute/" + cloudICP.userInfo["isdn"] + "/camerapermission" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryCallInfo() query decoder info
 * @param {Object} param {
 *  "isdn": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryCallInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCallInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCallInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryCallInfo: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["isdn"])) {
		result["desc"] = "queryCallInfo: isdn is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/queryattribute/" + cloudICP.userInfo["isdn"] + "/decoder/" + param["isdn"] + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryDCInfo() query DC info
 * @param {Object} param {
 *  "isdn": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryDCInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDCInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDCInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryDCInfo: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["isdn"])) {
		result["desc"] = "queryDCInfo: isdn is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/queryattribute/" + cloudICP.userInfo["isdn"] + "/dc/" + param[
		"isdn"] + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryUEInfo() query UE info
 * @param {Object} param {
 *  "isdn": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryUEInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryUEInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryUEInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryUEInfo: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["isdn"])) {
		result["desc"] = "queryUEInfo: isdn is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/queryattribute/" + cloudICP.userInfo["isdn"] + "/ue/" + param[
		"isdn"] + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * queryUEGisInfo() query UE gis info
 * @param {Object} param {
 *  "isdn": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryUEGisInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryUEGisInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryUEGisInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryUEGisInfo: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["isdn"])) {
		result["desc"] = "queryUEGisInfo: isdn is invalid";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/queryattribute/" + cloudICP.userInfo["isdn"] + "/uegis/" + param[
		"isdn"] + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryGISTrack() query department list
 * @param {Object} param {
 *  "ueId": ,
 *  "start_sec": ,
 *  "end_sec": ,
 *  "offset",
 *  "limit",
 *  "policy",
 *  "maxNum",
 *  "callback": ,
 * }
 */
 ICPSDK_Dispatch_Query.prototype.queryGISTrack = function(param) {
	cloudICP.util.LogInputParam(param);
    var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
    /**检查是不是合法年月日输入 */
    if (undefined == param) {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "queryGISTrack": "param is invalid" });
        return;
    }

    if (undefined == param["callback"] || typeof param["callback"] != "function") {
        cloudICP.util.log(tabPageId, { "logLevel": "error", "queryGISTrack": "callback is not a function" });
        return;
    }

    var result = {
        "rsp": "-3",
        "desc": ""
    };

    if (!cloudICP.userInfo["isdn"]) {
        result["desc"] = "queryGISTrack: User not logged.";
        param["callback"](result);
        return;
    }

    result["rsp"] = "-2";
    if (!(cloudICP.util.isNumber(param["ueid"]))) {
        result["desc"] += " queryGISTrack: ueid is invalid";
        param["callback"](result);
        return;
    }

    var tmpBeginStr;
    if (!(dateStrIsValid(param["begintime"])) || (undefined == param["begintime"])) {
        result["desc"] = "queryGISTrack: begintime is invalid";
        param["callback"](result);
        return;
    } else {
        tmpBeginStr = param["begintime"].replace(/-/g,"/");
        if (isNaN(Date.parse(tmpBeginStr))) {
            result["desc"] = "queryGISTrack: begintime is invalid";
            param["callback"](result);
            return;
        }
    }

    var tmpEndStr;

    if (!(dateStrIsValid(param["endtime"])) || (undefined == param["endtime"])) {
        result["desc"] = "queryGISTrack: endtime is invalid";
        param["callback"](result);
        return;
    } else {
        tmpEndStr = param["endtime"].replace(/-/g,"/");
        if (isNaN(Date.parse(tmpEndStr))) {
            result["desc"] = "queryGISTrack: endtime is invalid";
            param["callback"](result);
            return;
        }
    }

    if (((Date.parse(tmpEndStr) - Date.parse(tmpBeginStr))/1000) > 604800 ||
            ((Date.parse(tmpEndStr) - Date.parse(tmpBeginStr))/1000) < 0) {//604,800 为 7天的 s数 3600*24*7
        result["desc"] = "The start time must be earlier than the end time and The difference between the start time and end time cannot exceed 7 days.";
        param["callback"](result);
        return;
    }

    if (undefined == param["offset"]) {
        param["offset"] = "0";
    } else if (!(cloudICP.util.isNumber(param["offset"]))) {
        result["desc"] += " queryGISTrack: offset is invalid..";
        param["callback"](result);
        return;
    }

    if (undefined == param["limit"]) {
        param["limit"] = "1000";
    } else if (!(cloudICP.util.isNumber(param["limit"]))) {
        result["desc"] += " queryGISTrack: limit is error or null.";
        param["callback"](result);
        return;
    } else {
        var iLimit = parseInt(param["limit"]);
        if (1 > iLimit || 2000 < iLimit) {
            result["desc"] += " queryGISTrack: limit not in [1,2000]";
            param["callback"](result);
            return;
        }
    }

    if (undefined == param["policy"]) {
        param["policy"] = "1";
    } else if (!(cloudICP.util.isNumber(param["policy"]))) {
        result["desc"] += " queryGISTrack: policy is invalid, policy not in [0,1]";
        param["callback"](result);
        return;
    }

    if (undefined == param["maxnum"]) {
        param["maxnum"] = "2147483647";
    } else if (!(cloudICP.util.isNumber(param["maxnum"]))) {
        result["desc"] += " queryGISTrack: maxnum is invalid";
        param["callback"](result);
        return;
    }

    var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/gistrack";

    var requestParam = {
        "ueid": param["ueid"],
        "begintime": param["begintime"],
        "endtime": param["endtime"],
        "offset": param["offset"],
        "limit": param["limit"],
        "policy": param["policy"],
        "maxnum": param["maxnum"]
    };

    var ajaxCfg = {
        "type": "POST",
        "url": url,
        "async": true,
        "data": requestParam,
        "callback": function(data) {
            param["callback"](data);
        }
    }

    cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryConfWallInfo() query department list
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryConfWallInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryConfWallInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryConfWallInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryDepartmentList: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"] + "/querywallinfo";
	requestParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": true,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryRecord() query department list
 * @param {Object} param {
 *  "call_type": ,
 *  "callee": ,
 *  "caller": ,
 *  "resource_id": ,
 *  "start_sec": ,
 *  "end_sec": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryRecord = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryRecord": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryRecord": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryRecord: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	var types = ["0", "1", "2", "3", "5", "6", "7", "8"];
	if (!param["call_type"] || !cloudICP.util.includes(types, param["call_type"])) {
		result["desc"] = "queryRecord: The call_type is invalid";
		param["callback"](result);
		return;
	}

	if (param["caller"] != "" && !cloudICP.util.checkIsdn(param["caller"])) {
		result["desc"] = "queryRecord: caller is invalid";
		param["callback"](result);
		return;
	}

	if (param["callee"] != "" && !cloudICP.util.checkIsdn(param["callee"])) {
		result["desc"] = "queryRecord: callee is invalid";
		param["callback"](result);
		return;
	}

	if (!(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(param["end_sec"]))) {
		result["desc"] = "queryRecord: end_sec is invalid";
		param["callback"](result);
		return;
	} else {
		var tmpStr = param["end_sec"].replace(/-/g, "/")

		if (isNaN(Date.parse(tmpStr))) {
			result["desc"] = "queryRecord: end_sec is invalid";
			param["callback"](result);
			return;
		}
	}

	if (!(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(param["start_sec"]))) {
		result["desc"] = "queryRecord: start_sec is invalid";
		param["callback"](result);
		return;
	} else {
		var tmpStr = param["start_sec"].replace(/-/g, "/")

		if (isNaN(Date.parse(tmpStr))) {
			result["desc"] = "queryRecord: start_sec is invalid";
			param["callback"](result);
			return;
		}
	}

    if (undefined == param["offset"]) {
        param["offset"] = "0";
    } else if (!(cloudICP.util.isNumber(param["offset"]))) {
        result["desc"] += " queryRecord: offset is invalid";
        param["callback"](result);
        return;
    }

    if (undefined == param["limit"]) {
        param["limit"] = "100";
    } else if (!(cloudICP.util.isNumber(param["limit"]))) {
        result["desc"] += " queryRecord: limit is error or null.";
        param["callback"](result);
        return;
    } else {
        var iLimit = parseInt(param["limit"]);
        if (1 > iLimit || 1000 < iLimit) {
            result["desc"] += " queryRecord: limit not in [1,1000].";
            param["callback"](result);
            return;
        }
    }


	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/recfileinfo";

	var requestParam = {
		"call_type": param["call_type"],
		"callee": param["callee"],
		"caller": param["caller"],
		"resource_id": param["resource_id"],
		"start_sec": param["start_sec"],
		"end_sec": param["end_sec"],
		"offset": param["offset"],
        "fileNum": param["limit"]
	};

	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": true,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryCameraGPSInfo() query department list
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryCameraGPSInfo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameraGPSInfo": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryCameraGPSInfo": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryCameraGPSInfo: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/gisipc" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};

/**
 * queryMRS() query department list
 * @param {Object} param {
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Query.prototype.queryMRS = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryMRS": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryMRS": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryMRS: User not logged.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/mrsnodes" + "?reqId=" + tabPageId;

	var ajaxCfg = {
		"type": "GET",
		"url": url,
		"async": true,
		"data": null,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};

/**
 * query ralationship of GBID and ISDN
 * @param {Object} param {
 *  "callback": ,
 * }
 */
 ICPSDK_Dispatch_Query.prototype.queryDeviceIDRelationship = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
		param.reqId = tabPageId;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDeviceIDRelationship": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"queryDeviceIDRelationship": "callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "queryDeviceIDRelationship: User not logged.";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (undefined == param["Type"] || ('0' != param["Type"] && '2' != param["Type"] && '1' != param["Type"])) {
		result["desc"] = " queryDeviceIDRelationship: Type is invalid.";
		param["callback"](result);
		return;
	}

	if (!cloudICP.util.checkIdList(param["IDList"]) && '0' == param["Type"]) {
		result["desc"] = " queryDeviceIDRelationship: IDList is invalid.";
		param["callback"](result);
		return;
	}

    if (param["Method"] != 'async' && param["Method"] != 'sync' && param["Method"] != '') {
        result["desc"] = " queryDeviceIDRelationship: Method is invalid.";
        param["callback"](result);
        return;
    }

	if (undefined == param["OffSet"] && '2' == param["Type"]) {
        param["OffSet"] = "0";
    } else if (!(cloudICP.util.isNumber(param["OffSet"])) && '2' == param["Type"]) {
        result["desc"] += " queryDeviceIDRelationship: OffSet is error or null.";
        param["callback"](result);
        return;
    } else if ('2' == param["Type"]) {
        var iLimit = parseInt(param["OffSet"]);
        if (0 > iLimit || 1000000 < iLimit) {
            result["desc"] += " queryDeviceIDRelationship: OffSet not in [1,1000].";
            param["callback"](result);
            return;
        }
    }

	if (undefined == param["Limit"] && '2' == param["Type"]) {
        param["Limit"] = "10000";
    } else if (!(cloudICP.util.isNumber(param["Limit"])) && '2' == param["Type"]) {
        result["desc"] += " queryDeviceIDRelationship: Limit is error or null.";
        param["callback"](result);
        return;
    }


	var url = cloudICP.getSdkServerUrl() + "/v1/querylist/" + cloudICP.userInfo["isdn"] + "/deviceidelationship";

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": true,
		"data": param,
		"callback": function(data) {
			data["desc"] += result["desc"];
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

dateStrIsValid = function(dateStr) {
    var regStr = "^(?:(?!0000)[0-9]{4}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1[0-9]|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)"
                +"|(?:0[13578]|1[02])-31)|(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:0[48]|[2468][048]|[13579]"
                +"[26])00)-02-29)\\s+([0-1]?[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])$";
    return new RegExp(regStr).test(dateStr);
};
function ICPSDK_Dispatch_Sms() {
	this.MAX_MESSAGE_CONTENT = 10000;
	this.MAX_DEST_COUNT = 200;

	this.lastSendTime = 0;
};

/**
 * sendDispSMS() send short message
 * @param {Object} param {
 *  "dest": ,
 *  "content": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Sms.prototype.sendDispSMS = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"sendDispSMS": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"sendDispSMS": "callback is not a function"
		});
		return;
	}

	var sendDispSMSParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	var oldTime = this.lastSendTime;
	this.lastSendTime = new Date().getTime();

	if (this.lastSendTime - oldTime <= 1000) {
		result["rsp"] = "-4";
		result["desc"] = "sendDispSMS: The interval of sending message should be larger than 1 second.";
		param["callback"](result);
		return;
	}

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "sendDispSMS: User not logged.";
		param["callback"](result);
		return;
	}

	if (!param["content"]) {
		result["rsp"] = "-2";
		result["desc"] = "sendDispSMS: content is invalid.";
		param["callback"](result);
		return;
	} else {
		var clength = 0;
		for (var index = 0; index < param["content"].length; index++) {
			if (encodeURI(param["content"][index]).length > 1) {
				clength += 3;
			} else {
				clength += 1;
			}
		}

		if (clength > this.MAX_MESSAGE_CONTENT) {
			result["rsp"] = "-2";
			result["desc"] = "sendDispSMS: content is invalid.";
			param["callback"](result);
			return;
		}
	}

	if ((!param["dest"]) || cloudICP.util.includes(param["dest"], " ") || !(/^(([1-9]+[0-9]*)+;)*(([1-9]+[0-9]*)+)$/
			.test(param["dest"]))) {
		result["rsp"] = "-2";
		result["desc"] = "sendDispSMS: dest is invalid.";
		param["callback"](result);
		return;
	}

	if (param["dest"].split(";").length > this.MAX_DEST_COUNT) {
		result["rsp"] = "-2";
		result["desc"] = "sendDispSMS: dest is invalid.";
		param["callback"](result);
		return;
	}

	sendDispSMSParam["param"] = {};
	sendDispSMSParam["param"]["msgid"] = "";
	sendDispSMSParam["param"]["content"] = param["content"];
	sendDispSMSParam["param"]["dest"] = param["dest"];
	sendDispSMSParam["param"]["locationshare"] = param["locationshare"];
	if (undefined != param["msgid"]) {
		sendDispSMSParam["param"]["msgid"] = param["msgid"];
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/sds/" + cloudICP.userInfo["isdn"] + "/sms";
	sendDispSMSParam["param"]["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": sendDispSMSParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
}

/**
 * sendDispMMS() send multi media message
 * @param {Object} param {
 *  "dest": [
 *   {
 *     "isdn": "",
 *     "msgid": "",
 *   }
 *  ],
 *  "content": ,
 *  "attach": ,
 *  "callback": ,
 * }
 */
ICPSDK_Dispatch_Sms.prototype.sendDispMMS = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"sendDispMMS": "param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"sendDispMMS": "callback is not a function"
		});
		return;
	}

	var sendDispMMSParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "sendDispMMS: User not logged.";
		param["callback"](result);
		return;
	}

	if (!(param["dest"] && Array.isArray(param["dest"])) || param["dest"].length >= this.MAX_DEST_COUNT || param[
			"dest"].length <= 0) {
		result["rsp"] = "-2";
		result["desc"] = "sendDispMMS: dest is invalid";
		param["callback"](result);
		return;
	}

	var dests = param["dest"];
	for (var item = 0; item < dests.length; item++) {
		if (!cloudICP.util.isNumber(dests[item]["msgid"])) {
			result["rsp"] = "-2";
			result["desc"] = "sendDispMMS: msgid is invalid";
			param["callback"](result);
			return;
		}
	}

	if (undefined != param["audioinfo"]) {
		if (!cloudICP.util.isNumber(param["audioinfo"])) {
			result["rsp"] = "-2";
			result["desc"] = "sendDispMMS: audioinfo is invalid";
			param["callback"](result);
			return;
		} else if ("" == param["audiocontent"]) {
			result["rsp"] = "-2";
			result["desc"] = "sendDispMMS: audioinfo is valid, audiocontent is invalid";
			param["callback"](result);
			return;
		}
	}
	if (undefined != param["audiocontent"]) {
		if ("" == param["audiocontent"]) {
			result["rsp"] = "-2";
			result["desc"] = "sendDispMMS: audiocontent is invalid";
			param["callback"](result);
			return;
		} else if (!param["audiocontent"]) {
			result["rsp"] = "-2";
			result["desc"] = "sendDispMMS: audiocontent is valid, audioinfo is invalid";
			param["callback"](result);
			return;
		}
	}

	sendDispMMSParam["param"] = {};
	sendDispMMSParam["param"]["msgid"] = "";
	sendDispMMSParam["param"]["dest"] = "";

	if (undefined !=param["attach"]) {
		sendDispMMSParam["param"]["attach"] = param["attach"];
	}

	if (undefined !=param["attach_thumb"]) {
		sendDispMMSParam["param"]["attach_thumb"] = param["attach_thumb"];
	}

	if (undefined !=param["original_image"]) {
		sendDispMMSParam["param"]["original_image"] = param["original_image"];
	}

	var length = param["dest"].length;
	for (var i = 0; i < length; i++) {
		var user = param["dest"][i];

		if (!cloudICP.util.checkIsdn(user["isdn"])) {
			result["rsp"] = "-2";
			result["desc"] = "sendDispMMS: isdn is invalid";
			param["callback"](result);
			return;
		}

		if (user["msgid"] == undefined || !user["msgid"]) {
			result["rsp"] = "-2";
			result["desc"] = "sendDispMMS: msgid is invalid";
			param["callback"](result);
			return;
		}

		if (i == length - 1) {
			sendDispMMSParam["param"]["msgid"] += user["msgid"];
			sendDispMMSParam["param"]["dest"] += user["isdn"];
		} else {
			sendDispMMSParam["param"]["msgid"] += user["msgid"] + ";";
			sendDispMMSParam["param"]["dest"] += user["isdn"] + ";";
		}
	}

	if (param["audiocontent"] || param["audioinfo"]) {
		if (param["audiocontent"] && param["audioinfo"]) {
			sendDispMMSParam["param"]["audiocontent"] = param["audiocontent"];
			sendDispMMSParam["param"]["audioinfo"] = param["audioinfo"];
		} else {
			result["rsp"] = "-2";
			result["desc"] = "sendDispMMS: audio msg must has audiocontent and audioinfo";
			param["callback"](result);
			return;
		}
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/sds/" + cloudICP.userInfo["isdn"] + "/mms";
	sendDispMMSParam["param"]["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": sendDispMMSParam,
		"callback": function(data) {

			param["callback"](data);
		}
	}

	cloudICP.util.ajax(ajaxCfg);
};
function ICPSDK_Dispatch_Video() {
	this.MAX_DISPATCH_LIST = 100;

	this.MAX_VIDEO_ABILITY = 50;

	// 当前视频能力，目前默认最大支持50单位的视频能力。
	this.currentVideoAbility = 0;

	this.MAX_VIDEO_DIAL_NUM = 1;
};

/**
 * monitorVideo
 * @param {Object} param {
 *  to: ,
 *  monitorParam: {
 *      fmt: ,
 *      mute: ,
 *      confirm: ,
 *      camera: ,
 *      videooffer: ,
 *  }
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.monitorVideo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	cloudICP.util.log(tabPageId, {
		"logLevel": "INFO",
		"logMsg": "monitorVideo entry"
	});
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "monitorVideo: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "monitorVideo: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "monitorVideo: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "monitorVideo: The to is invalid";
		param["callback"](result);
		return;
	}

	if (!param["monitorParam"]) {
		result["desc"] = "monitorVideo: The monitorParam is invalid";
		param["callback"](result);
		return;
	}

	if (!param["monitorParam"]["fmt"]) {
		param["monitorParam"]["fmt"] = "4K";
	}

	var fmts = ["4K", "2K", "1080P", "720P", "D1", "CIF", "QCIF", "GCIF"];
	if (!param["monitorParam"]["fmt"] || !cloudICP.util.includes(fmts, param["monitorParam"]["fmt"])) {
		result["desc"] = "monitorVideo: The fmt is invalid";
		param["callback"](result);
		return;
	}

	var mutes = ["0", "1"];
	if (!param["monitorParam"]["mute"] || !cloudICP.util.includes(mutes, param["monitorParam"]["mute"])) {
		result["desc"] = "monitorVideo: The mute is invalid";
		param["callback"](result);
		return;
	}

	var confirms = ["0", "1"];
	if (!param["monitorParam"]["confirm"] || !cloudICP.util.includes(confirms, param["monitorParam"]["confirm"])) {
		result["desc"] = "monitorVideo: The confirm is invalid";
		param["callback"](result);
		return;
	}

	var cameras = ["0", "1", "2"];
	if (!param["monitorParam"]["camera"] || !cloudICP.util.includes(cameras, param["monitorParam"]["camera"])) {
		result["desc"] = "monitorVideo: The camera is invalid";
		param["callback"](result);
		return;
	}

	var strWindowinfo = {};
	if (undefined !=param["monitorParam"]) {
		strWindowinfo["fmt"] = param["monitorParam"]["fmt"];
		strWindowinfo["mute"] = param["monitorParam"]["mute"];
		strWindowinfo["confirm"] = param["monitorParam"]["confirm"];
		strWindowinfo["camera"] = param["monitorParam"]["camera"];
		strWindowinfo["videooffer"] = param["monitorParam"]["mode"];
		strWindowinfo["mode"] = param["monitorParam"]["mode"];
        strWindowinfo["showToolbar"] = param["monitorParam"]["showToolbar"];
        strWindowinfo["buttonIDs"] = param["monitorParam"]["buttonIDs"];
        strWindowinfo["posX"] = param["monitorParam"]["posX"];
        strWindowinfo["posY"] = param["monitorParam"]["posY"];
        strWindowinfo["width"] = param["monitorParam"]["width"];
        strWindowinfo["height"] = param["monitorParam"]["height"];
		strWindowinfo["hide"] = param["monitorParam"]["hide"];
		strWindowinfo["enableDrag"] = param["monitorParam"]["enableDrag"];
        strWindowinfo["videoResizeMode"] = param["monitorParam"]["videoResizeMode"];
	}
	if (undefined == param["monitorParam"]["mode"] || !cloudICP.util.checkmode(param["monitorParam"]["mode"])) {
		result["desc"] += "monitorVideo: The mode is invalid, use defult value " + cloudICP.config["mode"] + "; ";
		strWindowinfo["mode"] = cloudICP.config["mode"];
	}

	if ("wssflow" == strWindowinfo["mode"]) { //推流模式
		if (undefined == param["monitorParam"]["width"] || !cloudICP.util.isNumber(param["monitorParam"][
				"width"
			]) || param["monitorParam"]["width"] > 2147483647 || param["monitorParam"]["width"] < 0) {
			result["desc"] = "monitorVideo: The width is invalid.";
			param["callback"](result);
			return;
		}
		if (undefined == param["monitorParam"]["height"] || !cloudICP.util.isNumber(param["monitorParam"][
				"height"
			]) || param["monitorParam"]["height"] > 2147483647 || param["monitorParam"]["height"] < 0) {
			result["desc"] = "monitorVideo: The height is invalid.";
			param["callback"](result);
			return;
		}
		delete strWindowinfo["userWindowId"];
	} else if ("window" == strWindowinfo["mode"]) { //弹窗模式
		if (!cloudICP.util.checkWindowParam(param["monitorParam"]["posX"])) {
			result["desc"] += "monitorVideo: The posX is invalid, use defult value 400; ";
			strWindowinfo["posX"] = 400;
		}
		if (!cloudICP.util.checkWindowParam(param["monitorParam"]["posY"])) {
			result["desc"] += "monitorVideo: The posY is invalid, use defult value 400; ";
			strWindowinfo["posY"] = 400;
		}
		if (!cloudICP.util.checkWindowParam(param["monitorParam"]["width"])) {
			result["desc"] += "monitorVideo: The width is invalid, use defult value 640; ";
			strWindowinfo["width"] = 640;
		}
		if (!cloudICP.util.checkWindowParam(param["monitorParam"]["height"])) {
			result["desc"] += "monitorVideo: The height is invalid, use defult value 480; ";
			strWindowinfo["height"] = 480;
		}
		if (undefined == param["monitorParam"]["showToolbar"] || !cloudICP.util.checkshowToolbar(param[
				"monitorParam"]["showToolbar"])) {
			result["desc"] += "monitorVideo: The showToolbar is invalid, use defult value 1; ";
			strWindowinfo["showToolbar"] = 1;
		}
		if (undefined == param["monitorParam"]["buttonIDs"] || !cloudICP.util.checkbuttonIDs(param["monitorParam"][
				"buttonIDs"
			])) {
			result["desc"] += "monitorVideo: The buttonIDs is invalid, use defult value 0; ";
			strWindowinfo["buttonIDs"] = 0;
		}
		if (undefined == param["monitorParam"]["hide"]) {
			result["desc"] += "monitorVideo: The hide is invalid, use defult value false; ";
			strWindowinfo["hide"] = false;
		}
		if (undefined == param["monitorParam"]["enableDrag"]) {
			result["desc"] += "monitorVideo: The enableDrag is invalid, use defult value true; ";
			strWindowinfo["enableDrag"] = true;
		}
		if (undefined != param["monitorParam"]["userWindowId"]) { //检测userWindowId是否被占用、值是否有效
			var iResult = cloudICP.util.checkUserWindowId(param["monitorParam"]["userWindowId"], "use");
			if (0 == iResult) {
				result["desc"] = "monitorVideo: The userWindowId is invalid.";
				param["callback"](result);
				return;
			} else if (2 == iResult) {
				result["desc"] = "monitorVideo: The userWindowId not create window or used in other video operate.";
				param["callback"](result);
				return;
			}
		}
	} else {
		result["desc"] = "monitorVideo: The mode is invalid.";
		param["callback"](result);
		return;
	}

	//监控相同摄像头的视频流   不支持用户窗口
	var jsonMediaValue = {
		"value": ""
	};
	var videoResizeMode = cloudICP.config["videoResizeMode"];
	if (cloudICP.util.createNewCopyMonitor(cloudICP.userInfo["isdn"], param["to"], jsonMediaValue)) {
		if ("" == jsonMediaValue["value"]) {
			result["desc"] = "monitorVideo: The " + param["to"] + " is not get video stream, please wait a minute.";
			result["rsp"] = "-5";
			param["callback"](result);
			return;
		}
		var repeatsId = jsonMediaValue["value"]["cid"];
		var monitorRepeatsCid = cloudICP.util.getMonitorRepeatsCid();
		if (undefined == cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]) {
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()] = {};
		}
		if (undefined != strWindowinfo["videoResizeMode"]) {
			videoResizeMode = strWindowinfo["videoResizeMode"];
		}

		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"] = jsonMediaValue["value"];
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["videoResizeMode"] = videoResizeMode;
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["cid"] = monitorRepeatsCid;
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["tabPageId"] = tabPageId;
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["repeatsMonitor"] = true;
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["createId"] = repeatsId; // 创建流监控cid
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["releaseTag"] = false; // 创建流监控是否释放
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["winMode"] = strWindowinfo["mode"];
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["posX"] = strWindowinfo["posX"];
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["posY"] = strWindowinfo["posY"];
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["width"] = strWindowinfo["width"];
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["height"] = strWindowinfo["height"];
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["buttonIDs"] = strWindowinfo["buttonIDs"];
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["showToolbar"] = strWindowinfo["showToolbar"];
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["enableDrag"] = strWindowinfo["enableDrag"];
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["hide"] = strWindowinfo["hide"];
		cloudICP.util.callStatusMgr.videoMediaInfoBuf[monitorRepeatsCid.toString()]["value"]["repeats"] = true;
		var params;
		if (undefined != param["monitorParam"]["userWindowId"]) {
			params =
				"{\"description\":\"msp_create_video_window\", \"cmd\": 196610, \"param\":{\"resID\":{0}, \"windowName\": \"{1}\", \"mode\": \"{2}\", \"width\": {3}, \"height\": {4}, \"x\": {5}, \"y\":{6}, \"hide\": {7}, \"enableDrag\":{8}, \"scrollX\": {9}, \"scrollY\":{10}, \"showToolbar\": {11}, \"buttonIDs\": {12}, \"userWindowId\": {13}, \"repeatsMonitor\": {14}, \"videoResizeMode\": {15}, \"createId\": {16} }}"
				.format(
					monitorRepeatsCid, monitorRepeatsCid, strWindowinfo["mode"], strWindowinfo["width"],
					strWindowinfo["height"], strWindowinfo["posX"], strWindowinfo["posY"], strWindowinfo["hide"],
					strWindowinfo["enableDrag"], 0, 0, strWindowinfo["showToolbar"], strWindowinfo["buttonIDs"],
					param["monitorParam"]["userWindowId"], true, videoResizeMode, repeatsId
				);
		} else if (strWindowinfo["mode"] != 'wssflow') {
			params =
				"{\"description\":\"msp_create_video_window\", \"cmd\": 196610, \"param\":{\"resID\":{0}, \"windowName\": \"{1}\", \"mode\": \"{2}\", \"width\": {3}, \"height\": {4}, \"x\": {5}, \"y\":{6}, \"hide\": {7}, \"enableDrag\":{8}, \"scrollX\": {9}, \"scrollY\":{10}, \"showToolbar\": {11}, \"buttonIDs\": {12}, \"repeatsMonitor\": {13}, \"reqId\": \"{14}\", \"videoResizeMode\": {16}, \"createId\": {17} }, \"reqId\": \"{15}\" }"
				.format(
					monitorRepeatsCid, monitorRepeatsCid, strWindowinfo["mode"], strWindowinfo["width"],
					strWindowinfo["height"], strWindowinfo["posX"], strWindowinfo["posY"], strWindowinfo["hide"],
					strWindowinfo["enableDrag"], 0, 0, strWindowinfo["showToolbar"], strWindowinfo["buttonIDs"],
					true, tabPageId, tabPageId, videoResizeMode, repeatsId
				);
		} else { // 内嵌模式重复视频监控
			params =
				"{\"description\":\"msp_create_video_window\", \"cmd\": 196610, \"param\":{\"resID\":{0}, \"windowName\": \"{1}\", \"mode\": \"{2}\", \"width\": {3}, \"height\": {4}, \"repeatsMonitor\": {5} , \"reqId\": \"{6}\", \"videoResizeMode\": {8}, \"createId\": {9} }, \"reqId\": \"{7}\" }"
				.format(
					monitorRepeatsCid, monitorRepeatsCid, strWindowinfo["mode"], strWindowinfo["width"],
					strWindowinfo["height"],
					true, tabPageId, tabPageId, videoResizeMode, repeatsId
				);
		}
		cloudICP.dispatch.webSocket.sendDataToMediaSDK(params, true);

		result["desc"] = "monitorVideo: the same camera success.";
		result["rsp"] = "0";
		param["callback"](result);
		return;
	} else if ("" != jsonMediaValue["value"]) {
		result["desc"] = "monitorVideo: The object(" + param["to"] +
			") is exist three monitor, can't monitor again.";
		result["rsp"] = "-5";
		param["callback"](result);
		return;
	}

	if (cloudICP.dispatch.video.currentVideoAbility >= cloudICP.dispatch.video.MAX_VIDEO_ABILITY) {
		result["rsp"] = "-5";
		result["desc"] = "monitorVideo: The ability of monitorVideo is limited";
		param["callback"](result);
		return;
	}

	var videoOffers = ["0", "1", "2", "3"];
	if (param["monitorParam"]["videooffer"]) {
		if(!cloudICP.util.includes(videoOffers, param["monitorParam"]["videooffer"])) {
			result["desc"] = "monitorVideo: The videooffer is invalid";
			param["callback"](result);
			return;
		}
	}
	else {
		param["monitorParam"]["videooffer"] = "2";
	}

	var requestParam = {
		"opt": "monitor",
		"to": param["to"],
		"param": {
			"fmt": param["monitorParam"]["fmt"],
			"mute": param["monitorParam"]["mute"],
			"confirm": param["monitorParam"]["confirm"],
			"camera": param["monitorParam"]["camera"],
			"videooffer": param["monitorParam"]["videooffer"]
		}
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/video/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "PUT",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (undefined != data["rsp"] && "0" == data["rsp"].toString()) {
				cloudICP.util.callStatusMgr.windowInfos[param["to"]] = strWindowinfo;
				if (undefined != param["monitorParam"]["userWindowId"] && undefined != cloudICP.util
					.callStatusMgr.videoMediaInfoBuf[param["monitorParam"]["userWindowId"].toString()]) {
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[param["monitorParam"]["userWindowId"]
						.toString()]["value"]["to"] = parseInt(param["to"]);
				}
			}
			param["callback"](data)
		}
	}
	cloudICP.util.log(tabPageId, {
		"logLevel": "INFO",
		"logMsg": requestParam
	});
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * dialVideo
 * @param {Object} param {
 *  to: ,
 *  dialVideoParam: {
 *      fmt: ,
 *  }
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.dialVideo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "dialVideo: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "dialVideo: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "dialVideo: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "dialVideo: The to is invalid";
		param["callback"](result);
		return;
	}

	if (!param["dialVideoParam"]) {
		result["desc"] = "dialVideo: The dialVideoParam is invalid";
		param["callback"](result);
		return;
	}

	if (!param["dialVideoParam"]["fmt"]) {
		param["dialVideoParam"]["fmt"] = cloudICP.util.getDefaultFmt();
	}

	var fmts = ["4K", "2K", "1080P", "720P", "D1", "CIF", "QCIF", "GCIF"];
	if (!param["dialVideoParam"]["fmt"] || !cloudICP.util.includes(fmts, param["dialVideoParam"]["fmt"])) {
		result["desc"] = "dialVideo: The fmt is invalid";
		param["callback"](result);
		return;
	}

	if (cloudICP.config.cameraInfo == 0) {
		result["rsp"] = "-6";
		result["desc"] = "dialVideo: The camera is unavailable."
		param["callback"](result);
		return;
	}

	// if (!cloudICP.util.checkFmt(param["dialVideoParam"]["fmt"])) {
	// 	result["rsp"] = "-5";
	// 	result["desc"] = "dialVideo: The fmt is not supported by the camera.";
	// 	param["callback"](result);
	// 	return;
	// }

	if (cloudICP.util.callStatusMgr.currentCallState == cloudICP.util.callStatusMgr.RINGING) {
		result["rsp"] = "-4";
		result["desc"] = "dialVideo: There is a call is ringing."
		param["callback"](result);
		return;
	}

	var strWindowinfo = {};
	if (undefined != param["dialVideoParam"]) {
		strWindowinfo["fmt"] = param["dialVideoParam"]["fmt"];
		strWindowinfo["mode"] = param["dialVideoParam"]["mode"];
        strWindowinfo["showToolbar"] = param["dialVideoParam"]["showToolbar"];
        strWindowinfo["buttonIDs"] = param["dialVideoParam"]["buttonIDs"];
        strWindowinfo["posX"] = param["dialVideoParam"]["posX"];
        strWindowinfo["posY"] = param["dialVideoParam"]["posY"];
        strWindowinfo["width"] = param["dialVideoParam"]["width"];
        strWindowinfo["height"] = param["dialVideoParam"]["height"];
        strWindowinfo["videoResizeMode"] = param["dialVideoParam"]["videoResizeMode"];
	}
	if (undefined == param["dialVideoParam"]["mode"] || !cloudICP.util.checkmode(param["dialVideoParam"]["mode"])) {
		cloudICP.util.log(tabPageId, { "logLevel": "info", "logMsg": "dialVideo: The mode is invalid, use defult value " + cloudICP.config["mode"] });
		strWindowinfo["mode"] = cloudICP.config["mode"];
	}

	if ("wssflow" == strWindowinfo["mode"]) { //推流模式
		if (undefined == param["dialVideoParam"]["width"] || !cloudICP.util.isNumber(param["dialVideoParam"][
				"width"
			]) || param["dialVideoParam"]["width"] > 2147483647 || param["dialVideoParam"]["width"] <
			0) {
			result["desc"] = "dialVideo: The width is invalid.";
			param["callback"](result);
			return;
		}
		if (undefined == param["dialVideoParam"]["height"] || !cloudICP.util.isNumber(param["dialVideoParam"][
				"height"
			]) || param["dialVideoParam"]["height"] > 2147483647 || param["dialVideoParam"]["height"] < 0) {
			result["desc"] = "dialVideo: The height is invalid.";
			param["callback"](result);
			return;
		}
	} else if ("window" == strWindowinfo["mode"]) { //弹窗模式
		if (!cloudICP.util.checkWindowParam(param["dialVideoParam"]["posX"])) {
			result["desc"] += "dialVideo: The posX is invalid, use defult value 400; ";
			strWindowinfo["posX"] = 400;
		}
		if (!cloudICP.util.checkWindowParam(param["dialVideoParam"]["posY"])) {
			result["desc"] += "dialVideo: The posY is invalid, use defult value 400; ";
			strWindowinfo["posY"] = 400;
		}
		if (!cloudICP.util.checkWindowParam(param["dialVideoParam"]["width"])) {
			result["desc"] += "dialVideo: The width is invalid, use defult value 640; ";
			strWindowinfo["width"] = 640;
		}
		if (!cloudICP.util.checkWindowParam(param["dialVideoParam"]["height"])) {
			result["desc"] += "dialVideo: The height is invalid, use defult value 480; ";
			strWindowinfo["height"] = 480;
		}
		if (undefined == param["dialVideoParam"]["showToolbar"] || !cloudICP.util.checkshowToolbar(param[
				"dialVideoParam"]["showToolbar"])) {
			result["desc"] += "dialVideo: The showToolbar is invalid, use defult value 1; ";
			strWindowinfo["showToolbar"] = 1;
		}
		if (undefined == param["dialVideoParam"]["buttonIDs"] || !cloudICP.util.checkbuttonIDs(param[
				"dialVideoParam"]["buttonIDs"])) {
			result["desc"] += "dialVideo: The buttonIDs is invalid, use defult value 0; ";
			strWindowinfo["buttonIDs"] = 0;
		}
	} else {
		result["desc"] = "dialVideo: The mode is invalid.";
		param["callback"](result);
		return;
	}


	var callStatusMgr = cloudICP.util.callStatusMgr;
	//strWindowinfo["to"] = param["to"];
	if (callStatusMgr.numberOfCall[callStatusMgr.VIDEO] + callStatusMgr.numberOfCall[callStatusMgr.VIDEOCONF] >=
		cloudICP.dispatch.video.MAX_VIDEO_DIAL_NUM ||
		cloudICP.dispatch.video.currentVideoAbility >= cloudICP.dispatch.video.MAX_VIDEO_ABILITY) {
		result["rsp"] = "-7";
		result["desc"] = "dialVideo: The ability of video call is limited";
		param["callback"](result);
		return;
	}

	if (callStatusMgr.isAnyCallExisted()) {
		result["rsp"] = "-8";
		result["desc"] = "dialVideo: The voice dial is conflicted with other calls";
		param["callback"](result);
		return;
	}
	callStatusMgr.windowInfos[param["to"]] = strWindowinfo;
	var requestParam = {
		"opt": "dial",
		"to": param["to"],
		"param": {
			"fmt": param["dialVideoParam"]["fmt"],
			"video_format": cloudICP.config.cameraInfo.toString(),
			"audio_format": "1"
		}
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/video/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "PUT",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * answer
 * @param {Object} param {
 *  cid: ,
 *  windowInfo: {
 *      width:
 *      height:
 *      posX:
 *      posY:
 *  }
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.answer = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "answer: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "answer: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "answer: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "answer: The cid is invalid";
		param["callback"](result);
		return;
	}
	var strWindowinfo = {};
	if (param["windowInfo"] == undefined || undefined == param["windowInfo"]["mode"] || !cloudICP.util.checkmode(param["windowInfo"]["mode"])) {
		cloudICP.util.log(tabPageId, { "logLevel": "info", "logMsg": "answer: The mode is invalid, use defult value " + cloudICP.config["mode"] });
		strWindowinfo["mode"] = cloudICP.config["mode"];
	}
	if (param["windowInfo"] != undefined) {
		strWindowinfo["mode"] = param["windowInfo"]["mode"];
        strWindowinfo["showToolbar"] = param["windowInfo"]["showToolbar"];
        strWindowinfo["buttonIDs"] = param["windowInfo"]["buttonIDs"];
        strWindowinfo["posX"] = param["windowInfo"]["posX"];
        strWindowinfo["posY"] = param["windowInfo"]["posY"];
        strWindowinfo["width"] = param["windowInfo"]["width"];
        strWindowinfo["height"] = param["windowInfo"]["height"];
        strWindowinfo["videoResizeMode"] = param["windowInfo"]["videoResizeMode"];
		if ("wssflow" == strWindowinfo["mode"]) { //推流模式
			if (undefined == param["windowInfo"]["width"] || !cloudICP.util.isNumber(param["windowInfo"][
					"width"
				]) || param["windowInfo"]["width"] > 2147483647 || param["windowInfo"]["width"] < 0) {
				result["desc"] = "answer: The width is invalid.";
				param["callback"](result);
				return;
			}
			if (undefined == param["windowInfo"]["height"] || !cloudICP.util.isNumber(param["windowInfo"][
					"height"
				]) || param["windowInfo"]["height"] > 2147483647 || param["windowInfo"]["height"] < 0) {
				result["desc"] = "answer: The height is invalid.";
				param["callback"](result);
				return;
			}
		} else if ("window" == strWindowinfo["mode"]) { //弹窗模式
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["posX"])) {
				result["desc"] += "answer: The posX is invalid, use defult value 400; ";
				strWindowinfo["posX"] = 400;
			}
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["posY"])) {
				result["desc"] += "answer: The posY is invalid, use defult value 400; ";
				strWindowinfo["posY"] = 400;
			}
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["width"])) {
				result["desc"] += "answer: The width is invalid, use defult value 640; ";
				strWindowinfo["width"] = 640;
			}
			if (!cloudICP.util.checkWindowParam(param["windowInfo"]["height"])) {
				result["desc"] += "answer: The height is invalid, use defult value 480; ";
				strWindowinfo["height"] = 480;
			}
			if (undefined == param["windowInfo"]["showToolbar"] || !cloudICP.util.checkshowToolbar(param[
					"windowInfo"]["showToolbar"])) {
				result["desc"] += "answer: The showToolbar is invalid, use defult value 1; ";
				strWindowinfo["showToolbar"] = 1;
			}
			if (undefined == param["windowInfo"]["buttonIDs"] || !cloudICP.util.checkbuttonIDs(param["windowInfo"][
					"buttonIDs"
				])) {
				result["desc"] += "answer: The buttonIDs is invalid, use defult value 0; ";
				strWindowinfo["buttonIDs"] = 0;
			}
		} else {
			result["desc"] = "answer: The mode is invalid.";
			param["callback"](result);
			return;
		}
		strWindowinfo["iCid"] = param["cid"];
	} else {
		strWindowinfo["showToolbar"] = 1;
		strWindowinfo["buttonIDs"] = 0;
		strWindowinfo["iCid"] = param["cid"];
		strWindowinfo["width"] = 640;
		strWindowinfo["height"] = 480;
	}
	var callStatusMgr = cloudICP.util.callStatusMgr;
	callStatusMgr.windowInfos[param["cid"]] = strWindowinfo;

	var calls = cloudICP.util.callStatusMgr.allCalls;
	if (calls[param["cid"]] && calls[param["cid"]]["callState"] >= cloudICP.util.callStatusMgr.CONNECTING) {
		var str;
		if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.CONNECTING) {
			str = "answer: The cid is answered";
		} else if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.HOLD) {
			str = "answer: The cid is holded";
		} else if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.RELEASE) {
			str = "answer: The cid is ended";
		} else {
			str = "answer: The cid is invalid";
		}
		result["rsp"] = "-4";
		result["desc"] = str;
		param["callback"](result);
		return;
	}

	var call = calls[param["cid"]];
	if (call && call["callType"] == cloudICP.util.callStatusMgr.VIDEODISPATCH) {
		// current do nothing
	} else {
		if (cloudICP.config.cameraInfo == 0) {
			result["rsp"] = "-5";
			result["desc"] = "answer: The camera is unavailable."
			param["callback"](result);
			return;
		};

		var callStatusMgr = cloudICP.util.callStatusMgr;
		if (callStatusMgr.connectingCallMgr[callStatusMgr.VIDEO].length != 0 ||
			callStatusMgr.connectingCallMgr[callStatusMgr.DISCREET].length != 0 ||
			callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE].length != 0 ||
			callStatusMgr.connectingCallMgr[callStatusMgr.VIDEOCONF].length != 0) {
			result["rsp"] = "-6";
			result["desc"] = "answer: The call is conflicted with other calls"
			param["callback"](result);
			return;
		};
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (cloudICP.dispatch.video.currentVideoAbility >= cloudICP.dispatch.video.MAX_VIDEO_ABILITY) {
		result["rsp"] = "-7";
		result["desc"] = "answer: The ability of video is limited";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "recv",
		"cid": param["cid"],
		"param": {
			"video_format": cloudICP.config.cameraInfo.toString(),
			"audio_format": "1"
		}
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/video/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * reject
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.reject = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "reject: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "reject: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "reject: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "reject: The cid is invalid";
		param["callback"](result);
		return;
	}

	var calls = cloudICP.util.callStatusMgr.allCalls;
	if (calls[param["cid"]] && calls[param["cid"]]["callState"] >= cloudICP.util.callStatusMgr.CONNECTING) {
		var str;
		if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.CONNECTING) {
			str = "reject: The cid is answered";
		} else if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.HOLD) {
			str = "reject: The cid is holded";
		} else if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.RELEASE) {
			str = "reject: The cid is ended";
		} else {
			str = "reject: The cid is invalid";
		}
		result["rsp"] = "-4";
		result["desc"] = str;
		param["callback"](result);
		return;
	}
	var requestParam = {
		"opt": "reject",
		"cid": param["cid"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/video/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "0") {
				// 停止振铃音
				var mediaParam =
					"{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}}".format(
						parseInt(param["cid"])
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.VIDEO, callStatusMgr.RELEASE, {
					"value": {
						"cid": param["cid"]
					}
				});
			}

			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * release
 * @param {Object} param {
 *  cid: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.release = function(param) {
	cloudICP.util.callStatusMgr.delWssflowDemoMap(param["cid"]);
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "release: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "release: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "release: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "release: The cid is invalid";
		param["callback"](result);
		return;
	}
	var cid = param["cid"];
	var callee = 0;
	Object.keys(cloudICP.util.callStatusMgr.windowInfos).forEach(function(key) {
		if (undefined != cloudICP.util.callStatusMgr.windowInfos[key]["userWindowId"] && cloudICP.util
			.callStatusMgr.windowInfos[key]["userWindowId"] == param["cid"]) {
			callee = key;
		}
	});
	if (0 != callee) {
		var bRuning = false;
		Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function(key) {
			if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"] && cloudICP.util
				.callStatusMgr.videoMediaInfoBuf[key]["value"]["userWindowId"] == param["cid"]) {
				bRuning = true;
				cid = key;
			}
		});
		if (false == bRuning) {
			delete cloudICP.util.callStatusMgr.windowInfos[callee.toString()];
			result["rsp"] = "-2";
			result["desc"] = "release: The userWindowId(" + param["cid"] + ") is release.";
			param["callback"](result);
			return;
		}
	}

	var call = cloudICP.util.callStatusMgr.allCalls[cid];
	if (call && call["caller"] != cloudICP.userInfo["isdn"] && call.callState == cloudICP.util.callStatusMgr
		.RINGING) {
		result["rsp"] = "-4";
		result["desc"] = "release: The callee can't release a ringing call.";
		param["callback"](result);
		return;
	}
	var repeats;
	//判断监控对象是否被多次监控
	var jsonMediaValue = {
		"value": ""
	};
	var bExistRepeatsMonitor = false;
	var callee;
	if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]) {
		callee = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["callee"];
	}
	if (!cloudICP.util.checkUserIsMonitor(cloudICP.userInfo["isdn"], callee, jsonMediaValue)) {
		if ("" != jsonMediaValue["value"]) {
			repeats = true;
		}
	}
	if (undefined != repeats && true == repeats && undefined == cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid][
			"value"
		]["repeats"]) {
		bExistRepeatsMonitor = true;
	}
	var createReleaseTag = true;
	//重复视频监控的业务需要释放
	if (undefined != repeats && repeats == true) {
		var newEvent = {
			"eventName": "",
			"rsp": "3009",
			"value": {}
		}
		if (true == bExistRepeatsMonitor) { //首次监控该对象发起的业务结束，重复监控业务也要被关闭
			var params = "{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
				.format(
					parseInt(cid), tabPageId
				);
			cloudICP.dispatch.webSocket.sendDataToMediaSDK(params, true);

			//通知上层OnCallRelease消息
			newEvent["value"]["caller"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["caller"];
			newEvent["value"]["callee"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["callee"];
			newEvent["value"]["cid"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["cid"];
			newEvent["value"]["fmt"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["fmt"];
			newEvent["value"]["calltype"] = "monitor";
			newEvent["value"]["releasetype"] = "self";
			newEvent["value"]["src"] = "";
			newEvent["value"]["direction"] = "out";
			newEvent["eventName"] = "OnCallRelease";
			cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
			sendEvent2Page(tabPageId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
			cloudICP.util.callStatusMgr.delMediaModeInfo(param);
			// 创建流被释放

			cloudICP.util.updateCopyMonitorReleaseTag(cloudICP.userInfo["isdn"], callee);
			createReleaseTag = false;
		} else { //只关闭重复监控的业务
			// 删除视频媒体通道
			var params = "{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
				.format(
					parseInt(cid), tabPageId
				);
			cloudICP.dispatch.webSocket.sendDataToMediaSDK(params, true);

			//通知上层OnCallRelease消息
			newEvent["value"]["caller"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["caller"];
			newEvent["value"]["callee"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["callee"];
			newEvent["value"]["cid"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["cid"];
			newEvent["value"]["fmt"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["fmt"];
			newEvent["value"]["calltype"] = "monitor";
			newEvent["value"]["releasetype"] = "other";
			newEvent["value"]["src"] = "";
			newEvent["value"]["direction"] = "out";
			newEvent["eventName"] = "OnCallRelease";
			cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
			sendEvent2Page(tabPageId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
			cloudICP.util.callStatusMgr.delMediaModeInfo(param);
			var createId;
			if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["createId"]) {
				createId = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["createId"];
			}
			createReleaseTag = cloudICP.util.getCopyMonitorReleaseTag(newEvent["value"]["caller"],
				newEvent["value"]["callee"]);

			if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]) { //该视频窗口有业务正在使用
				delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid];
			}


			result["rsp"] = "0";
			result["desc"] = "release: repeats monitor success.";
			param["callback"](result);
			if (createReleaseTag) { // 原视频流已经被释放场景，将cid赋值为创建视频流id 走释放场景
				cid = createId;
			}
		}
	}

	cloudICP.util.log(undefined, {
		"logLevel": "info",
		"logMsg": "bExistRepeatsMonitor: " + bExistRepeatsMonitor + " createReleaseTag: " + createReleaseTag + " cid: " + cid
	});
	if (!bExistRepeatsMonitor && createReleaseTag) {
		if(undefined == repeats || false == repeats) { // 不存在复制流时
			// 停止提示音
			var stopRingParam = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}}".format(
				parseInt(cid)
			);
			cloudICP.dispatch.webSocket.sendDataToMediaSDK(stopRingParam, true);
		}

		var requestParam = {
			"opt": "close",
			"cid": cid
		};
		var url = cloudICP.getSdkServerUrl() + "/v1/video/" + cloudICP.userInfo["isdn"] + "/close/" + cid;
		requestParam["reqId"] = tabPageId;
		var ajaxCfg = {
			"type": "DELETE",
			"url": url,
			"data": requestParam,
			"callback": function(data) {
				if (data["rsp"] == "1") {
					data["rsp"] = "0";
				}
				param["callback"](data);
			}
		}
		cloudICP.util.ajax(ajaxCfg);
	}

}

/**
 * dispatchVideo
 * @param {Object} param {
 *  src: ,
 *  fmt: ,
 *  dest: [
 *      {"isdn": ,}
 *  ]
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.dispatchVideo = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "dispatchVideo: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "dispatchVideo: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "dispatchVideo: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["src"])) {
		result["desc"] = "dispatchVideo: The src is invalid";
		param["callback"](result);
		return;
	}

	if (!param["fmt"]) {
		param["fmt"] = "720P";
	}

	var fmts = ["1080P", "720P", "D1", "CIF", "NO"];
	if (!param["fmt"] || !cloudICP.util.includes(fmts, param["fmt"])) {
		result["desc"] = "dispatchVideo: The fmt is invalid";
		param["callback"](result);
		return;
	}

	if (param["dest"] != undefined) {
		for (var item = 0; item < param["dest"].length; item++) {
			if (!cloudICP.util.checkCallNumber(param["dest"][item]["isdn"])) {
				result["desc"] = "dispatchVideo: reslist is invalid";
				param["callback"](result);
				return;
			}
		}
	}

	if (param["destGroups"] != undefined) {
		for (var item = 0; item < param["destGroups"].length; item++) {
			if (!cloudICP.util.checkCallNumber(param["destGroups"][item]["isdn"])) {
				result["desc"] = "dispatchVideo: destGroups is invalid";
				param["callback"](result);
				return;
			}
		}
	}

	var requestParam = {
		"opt": "dispatch",
		"param": {
			"src": param["src"],
			"fmt": param["fmt"],
			"dest": param["dest"],
			"destGroups": param["destGroups"]
		}
	};
	var url = cloudICP.getSdkServerUrl() + "/v1/video/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * cancelVideoDispatch
 * @param {Object} param {
 *  src: ,
 *  dest: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.cancelVideoDispatch = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "cancelVideoDispatch: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "cancelVideoDispatch: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "cancelVideoDispatch: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["src"])) {
		result["desc"] = "cancelVideoDispatch: The src is invalid";
		param["callback"](result);
		return;
	}

	if (!cloudICP.util.checkIsdn(param["dest"]) && !cloudICP.util.checkIsdn(param["destGroup"])) {
		result["desc"] = "cancelVideoDispatch: The dest and destGroup is invalid";
		param["callback"](result);
		return;
	}


	var requestParam = {
		"opt": "dispatchDelete",
		"param": {
			"src": param["src"],
			"dest": param["dest"],
			"destGroup": param["destGroup"]
		}
	};
	var url = cloudICP.getSdkServerUrl() + "/v1/video/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * startVideoUploadWall
 * @param {Object} param {
 *  src: ,
 *  channel: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.startVideoUploadWall = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "startVideoUploadWall: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "startVideoUploadWall: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "startVideoUploadWall: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["src"])) {
		result["desc"] = "startVideoUploadWall: The src is invalid";
		param["callback"](result);
		return;
	}

	if (!param["channel"] || !cloudICP.util.isNumber(param["channel"])) {
		result["desc"] = "startVideoUploadWall: The channel is invalid";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "start",
		"param": {
			"src": param["src"],
			"channel": param["channel"]
		}
	};
	var url = cloudICP.getSdkServerUrl() + "/v1/video/" + cloudICP.userInfo["isdn"] + "/videowall";
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * stopVideoUploadWall
 * @param {Object} param {
 *  src: ,
 *  channel: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.stopVideoUploadWall = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "stopVideoUploadWall: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "stopVideoUploadWall: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "stopVideoUploadWall: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["src"])) {
		result["desc"] = "stopVideoUploadWall: The src is invalid";
		param["callback"](result);
		return;
	}

	if (!param["channel"] || !cloudICP.util.isNumber(param["channel"])) {
		result["desc"] = "stopVideoUploadWall: The channel is invalid";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "stop",
		"param": {
			"src": param["src"],
			"channel": param["channel"]
		}
	};
	var url = cloudICP.getSdkServerUrl() + "/v1/video/" + cloudICP.userInfo["isdn"] + "/videowall";
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * ptzctrlCamera
 * @param {Object} param {
 *  to: ,
 *  act: ,
 *  value: ,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.ptzctrlCamera = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param == undefined || typeof param == 'undefined') {
		return;
	}

	if (typeof param["tabPageId"] != 'undefined' && param["tabPageId"] != undefined) {
		var param = param["value"];
	}

	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "ptzctrlCamera: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "ptzctrlCamera: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "ptzctrlCamera: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "ptzctrlCamera: The to is invalid";
		param["callback"](result);
		return;
	}

	if (!param["act"] || !cloudICP.util.isNumber(param["act"]) || parseInt(param["act"]) < 1 || parseInt(param[
			"act"]) > 18) {
		result["desc"] = "ptzctrlCamera: The act is invalid";
		param["callback"](result);
		return;
	}

	if (!param["value"] || !cloudICP.util.isNumber(param["value"]) || parseInt(param["value"]) < 1 || parseInt(
			param["value"]) > 10) {
		result["desc"] = "ptzctrlCamera: The value is invalid";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"to": param["to"],
		"param": {
			"act": param["act"],
			"value": param["value"]
		}
	};
	var url = cloudICP.getSdkServerUrl() + "/v1/video/" + cloudICP.userInfo["isdn"] + "/ptzcontrol";
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * setWssflowWinFmttype
 * @param {Object} param {
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Video.prototype.setWssflowWinFmttype = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	var msg = {
		"description": "msp_set_wssflow_window_fmttype",
		"cmd": 0x10014,
		"param": {}
	};
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "setWssflowWinFmttype: param is invalid"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["resID"].toString())) {
		result["desc"] = "setWssflowWindowFmttype: The resID is invalid";
		if (!!param["callback"]) {
			param["callback"](result);
		}
		return;
	}
	msg["param"]["resID"] = parseInt(param["resID"]);
	msg["param"]["width"] = parseInt(param["width"]);
	msg["param"]["height"] = parseInt(param["height"]);
	cloudICP.dispatch.webSocket._mediaSdkRspFuncs[msg.cmd.toString()] = param["callback"];
	var curTime = cloudICP.util.getCurrentDate();
	cloudICP.util.log(undefined, { "logLevel": "info", "logMsg": "setWssflowWinFmttype send msg=" + JSON.stringify(msg) });
	cloudICP.dispatch.webSocket.sendDataToMediaSDK(msg, true);
};
function ICPSDK_Dispatch_Voice() {
	this.MAX_SUB_LIST = 200;

	this.MAX_AMBIENCE_NUM = 1;
	this.MAX_HALF_DIAL_NUM = 1;
	this.MAX_VOICE_NUM = 30;
};

/**
 * subscribeUserStatus() subscribe users
 * @param {Object} param {
 *  reslist: [
 *      {isdn: ""},
 *  ],
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.subscribeUserStatus = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "subscribeUserStatus: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "subscribeUserStatus: callback is not a function"
		});
		return;
	}


	var subUserParam = {};
	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "subscribeUserStatus: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!(Array.isArray(param["reslist"]) && param["reslist"].length > 0 && param["reslist"].length <= this
			.MAX_SUB_LIST)) {
		result["desc"] = "subscribeUserStatus: reslist is invalid";
		param["callback"](result);
		return;
	}

	for (var item = 0; item < param["reslist"].length; item++) {
		if (!cloudICP.util.checkIsdn(param["reslist"][item]["isdn"])) {
			cloudICP.util.log(tabPageId, {
				"logLevel": "error",
				"logMsg": "subscribeUserStatus: isdn is invalid."
			});
			result["desc"] = "subscribeUserStatus: reslist is invalid";
			param["callback"](result);
			return;
		}
	}

	subUserParam["type"] = "sub";
	subUserParam["reslist"] = param["reslist"];
	var url = cloudICP.getSdkServerUrl() + "/v1/person/" + cloudICP.userInfo["isdn"];
	subUserParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": subUserParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * unsubscribeUserStatus() unsubUser users
 * @param {Object} param {
 *  reslist: [
 *      {isdn: ""},
 *  ],
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.unsubscribeUserStatus = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unsubscribeUserStatus: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unsubscribeUserStatus: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "unsubscribeUserStatus: The user does not logined";
		param["callback"](result);
		return;
	}

	//check param valid
	result["rsp"] = "-2";
	if (!(Array.isArray(param["reslist"]) && param["reslist"].length > 0 && param["reslist"].length <= this
			.MAX_SUB_LIST)) {
		result["desc"] = "unsubscribeUserStatus: reslist is is invalid";
		param["callback"](result);
		return;
	}

	for (var item = 0; item < param["reslist"].length; item++) {
		if (!cloudICP.util.checkIsdn(param["reslist"][item]["isdn"])) {
			cloudICP.util.log(tabPageId, {
				"logLevel": "error",
				"logMsg": "unsubscribeUserStatus: isdn is invalid."
			});
			result["desc"] = "unsubscribeUserStatus: isdn is invalid";
			param["callback"](result);
			return;
		}
	}

	var unsubUserParam = {};

	unsubUserParam["type"] = "unsub";
	unsubUserParam["reslist"] = param["reslist"];
	var url = cloudICP.getSdkServerUrl() + "/v1/person/" + cloudICP.userInfo["isdn"];
	unsubUserParam["reqId"] = tabPageId;

	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"async": false,
		"data": unsubUserParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}


/**
 * dial
 * @param {Object} param {
 *  to:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.dial = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "dial: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "dial: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "dial: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "dial: The to is invalid";
		param["callback"](result);
		return;
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (callStatusMgr.currentCallState == callStatusMgr.RINGING) {
		result["rsp"] = "-4";
		result["desc"] = "dial: There is a call is ringing."
		param["callback"](result);
		return;
	}

	if (callStatusMgr.numberOfCall[callStatusMgr.VOICE] + callStatusMgr.numberOfCall[callStatusMgr.VOICECONF] >
		cloudICP.dispatch.voice.MAX_VOICE_NUM) {
		result["rsp"] = "-5";
		result["desc"] = "dial: The number of voice call is limited to " + cloudICP.dispatch.voice.MAX_VOICE_NUM;
		param["callback"](result);
		return;
	}

	if (callStatusMgr.isAnyCallExisted()) {
		result["rsp"] = "-6";
		result["desc"] = "dial: The voice dial is conflicted with other calls";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "dial",
		"to": param["to"],
		"Answer-Mode": param["Answer-Mode"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "PUT",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * answer
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.answer = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "answer: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "answer: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "answer: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "answer: The cid is invalid";
		param["callback"](result);
		return;
	}

	var calls = cloudICP.util.callStatusMgr.allCalls;
	if (calls[param["cid"]] && calls[param["cid"]]["callState"] >= cloudICP.util.callStatusMgr.CONNECTING) {
		result["rsp"] = "-40002";
		result["desc"] = "";
		param["callback"](result);
		return;
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (callStatusMgr.connectingCallMgr[callStatusMgr.VIDEO].length != 0 ||
		callStatusMgr.connectingCallMgr[callStatusMgr.DISCREET].length != 0 ||
		callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE].length != 0 ||
		callStatusMgr.connectingCallMgr[callStatusMgr.VIDEOCONF].length != 0) {
		result["rsp"] = "-5";
		result["desc"] = "answer: The call is conflicted with other calls"
		param["callback"](result);
		return;
	};

	var call = calls[param["cid"]];
	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (call) {
		var callType = call.callType;

		switch (callType) {
			case callStatusMgr.AMBIENCE:
				break;
			case callStatusMgr.VOICE:
				if (callStatusMgr.numberOfCall[callStatusMgr.VOICE] + callStatusMgr.numberOfCall[callStatusMgr
						.VOICECONF] > cloudICP.dispatch.voice.MAX_VOICE_NUM) {
					result["rsp"] = "-4";
					result["desc"] = "answer: The number of voice call is limited to " + cloudICP.dispatch.voice
						.MAX_VOICE_NUM;
					param["callback"](result);
					return;
				}
				break;
			case callStatusMgr.HALFVOICE:
				if (callStatusMgr.numberOfCall[callStatusMgr.HALFVOICE] >= cloudICP.dispatch.voice
					.MAX_HALF_DIAL_NUM) {
					result["rsp"] = "-4";
					result["desc"] = "answer: The number of halfDial is limited to " + cloudICP.dispatch.voice
						.MAX_HALF_DIAL_NUM;
					param["callback"](result);
					return;
				}
				break;
			default:
				break;
		}
	}

	var requestParam = {
		"opt": "recv",
		"cid": param["cid"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * reject
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.reject = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "reject: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "reject: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "reject: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "reject: The cid is invalid";
		param["callback"](result);
		return;
	}

	var calls = cloudICP.util.callStatusMgr.allCalls;
	if (calls[param["cid"]] && calls[param["cid"]]["callState"] >= cloudICP.util.callStatusMgr.CONNECTING) {
		var str;
		if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.CONNECTING) {
			str = "reject: The cid is answered";
		} else if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.HOLD) {
			str = "reject: The cid is holded";
		} else if (calls[param["cid"]]["callState"] == cloudICP.util.callStatusMgr.RELEASE) {
			str = "reject: The cid is ended";
		} else {
			str = "reject: The cid is invalid";
		}
		result["rsp"] = "-4";
		result["desc"] = str;
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "reject",
		"cid": param["cid"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "0") {
				// 停止振铃音
				var mediaParam =
					"{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}}".format(
						parseInt(param["cid"])
					);

				cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam, true);

				var callStatusMgr = cloudICP.util.callStatusMgr;
				callStatusMgr.updateCallStatus(callStatusMgr.VOICE, callStatusMgr.RELEASE, {
					"value": {
						"cid": param["cid"]
					}
				});
			}

			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * release
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.release = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "release: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "release: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "release: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "release: The cid is invalid";
		param["callback"](result);
		return;
	}

	var call = cloudICP.util.callStatusMgr.allCalls[param["cid"]];
	if (call && call["caller"] != cloudICP.userInfo["isdn"] && call.callState == cloudICP.util.callStatusMgr
		.RINGING) {
		result["rsp"] = "-4";
		result["desc"] = "release: The callee can't release a ringing call.";
		param["callback"](result);
		return;
	}

	// 停止提示音
	var stopRingParam = "{\"description\":\"msp_stop_tone\", \"cmd\": 131076, \"param\":{\"cid\": {0}}}".format(
		parseInt(param["cid"])
	);
	cloudICP.dispatch.webSocket.sendDataToMediaSDK(stopRingParam, true);

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"] + "/close/" + param["cid"];
	var requestParam = {};
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "DELETE",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "1") {
				data["rsp"] = "0";
			}
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * transfer
 * @param {Object} param {
 *  to:,
 *  speaker:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.transfer = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "transfer: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "transfer: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "transfer: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "transfer: The to is invalid";
		param["callback"](result);
		return;
	}

	if (!cloudICP.util.checkIsdn(param["speaker"])) {
		result["desc"] = "transfer: The speaker is invalid";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "transfer",
		"to": param["to"],
		"speaker": param["speaker"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);

			if (data["rsp"] != 0) {
				var retEvent = {
					"eventName": "OnTransferFailure",
					"rsp": "0"
				}
				cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnTransferFailure"].event = retEvent;
				sendEvent2Page(tabPageId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"][
					"OnTransferFailure"]);
			}
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}


/**
 * cancelTransfer
 * @param {Object} param {
 *  to:,
 *  speaker:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.cancelTransfer = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "cancelTransfer: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "cancelTransfer: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "cancelTransfer: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "cancelTransfer: The to is invalid";
		param["callback"](result);
		return;
	}

	if (!cloudICP.util.checkIsdn(param["speaker"])) {
		result["desc"] = "cancelTransfer: The speaker is invalid";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "transfercancel",
		"to": param["to"],
		"speaker": param["speaker"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * breakOff
 * @param {Object} param {
 *  to:,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.breakOff = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "breakOff: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "breakOff: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "breakOff: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "breakOff: The to is invalid";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "P2P",
		"to": param["to"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"] + "/breakoff";
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);

			if (data["rsp"] != 0) {
				var retEvent = {
					"eventName": "OnBreakOffFailure",
					"rsp": "0"
				}
				cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnBreakOffFailure"].event = retEvent;
                sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnBreakOffFailure"])
			}
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * halfDial
 * @param {Object} param {
 *  to:,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.halfDial = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "halfDial: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "halfDial: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "halfDial: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "halfDial: The to is invalid";
		param["callback"](result);
		return;
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (callStatusMgr.currentCallState == callStatusMgr.RINGING) {
		result["rsp"] = "-4";
		result["desc"] = "halfDial: There is a call is ringing."
		param["callback"](result);
		return;
	}


	if (callStatusMgr.numberOfCall[callStatusMgr.HALFVOICE] >= cloudICP.dispatch.voice.MAX_HALF_DIAL_NUM) {
		var cid = callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE][0];
		var call = callStatusMgr.allCalls[cid];
		if (call["callee"] != param["to"] && call["caller"] != param["to"]) {
			result["rsp"] = "-5";
			result["desc"] = "halfDial: The number of halfDial is limited to " + cloudICP.dispatch.voice
				.MAX_HALF_DIAL_NUM;
			param["callback"](result);
			return;
		}
	}

	if (callStatusMgr.connectingCallMgr[callStatusMgr.VIDEO].length != 0 ||
		callStatusMgr.connectingCallMgr[callStatusMgr.DISCREET].length != 0 ||
		callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE].length != 0 ||
		callStatusMgr.connectingCallMgr[callStatusMgr.VIDEOCONF].length != 0) {

		if (callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE].length != 0) {
			var cid = callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE][0];
			var call = callStatusMgr.allCalls[cid];
			if (call["callee"] != param["to"] && call["caller"] != param["to"]) {
				result["rsp"] = "-6";
				result["desc"] = "halfDial: The voice call is conflicted with other calls";
				param["callback"](result);
				return;
			}
		}
	}

	if (callStatusMgr.isAnyCallExisted()) {
		if (callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE].length != 0) {
			var cid = callStatusMgr.connectingCallMgr[callStatusMgr.HALFVOICE][0];
			var call = callStatusMgr.allCalls[cid];
			if (call["callee"] != param["to"] && call["caller"] != param["to"]) {
				result["rsp"] = "-6";
				result["desc"] = "halfDial: The voice call is conflicted with other calls";
				param["callback"](result);
				return;
			}
		} else {
			result["rsp"] = "-6";
			result["desc"] = "halfDial: The voice call is conflicted with other calls";
			param["callback"](result);
			return;
		}
	}

	if (cloudICP.dispatch.group.isCurrentPtt["isPtt"]) {
		result["rsp"] = "-6";
		result["desc"] = "halfDial: The The voice call is conflicted with other calls.";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "halfDial",
		"to": param["to"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * releaseHalfDial
 * @param {Object} param {
 *  cid:,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.releaseHalfDial = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "releaseHalfDial: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "releaseHalfDial: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "releaseHalfDial: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "releaseHalfDial: The cid is invalid";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "halfDialRelease",
		"cid": param["cid"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			// 关闭麦克风
			var mediaParam =
				"{\"description\":\"msp_mute_audio_channel\", \"cmd\": 65549, \"param\":{\"resID\": {0}, \"mute\":true}}"
				.format(
					parseInt(param["cid"])
				);

			cloudICP.dispatch.webSocket.sendDataToMediaSDK(mediaParam, true);

			param["callback"](data)
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * closeHalfDial
 * @param {Object} param {
 *  cid:,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.closeHalfDial = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "closeHalfDial: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "closeHalfDial: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "closeHalfDial: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "closeHalfDial: The cid is invalid";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "close",
		"cid": param["cid"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data)
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * discreetListen
 * @param {Object} param {
 *  opType :  start|stop
 *  to:,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.discreetListen = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "discreetListen: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "discreetListen: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "discreetListen: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if ("start" != param["opType"] && "stop" != param["opType"]) {
		result["opType"] = "discreetListen: opType is invalid";
		param["callback"](result);
		return;
	}

	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "discreetListen: The to is invalid";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": param["opType"],
		"to": param["to"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"] + "/discreetlisten";
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);

			if (param["opType"] == "start") {
				if (data["rsp"] == 0) {
					var retEvent = {
						"eventName": "OnStartDiscreetlistenSuccess",
						"rsp": "0",
					}
					cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnStartDiscreetlistenSuccess"].event = retEvent;
					sendEvent2Page(tabPageId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"][
						"OnStartDiscreetlistenSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnStartDiscreetlistenFailure",
						"rsp": "0",
					}
					cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnStartDiscreetlistenFailure"].event = retEvent;
					sendEvent2Page(tabPageId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"][
						"OnStartDiscreetlistenFailure"]);
				}
			} else {
				if (data["rsp"] == 0) {
					var retEvent = {
						"eventName": "OnStopDiscreetlistenSuccess",
						"rsp": "0",
					}
					cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnStopDiscreetlistenSuccess"].event = retEvent;
					sendEvent2Page(tabPageId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"][
						"OnStopDiscreetlistenSuccess"]);
				} else {
					var retEvent = {
						"eventName": "OnStopDiscreetlistenFailure",
						"rsp": "0",
					}
					cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnStopDiscreetlistenFailure"].event = retEvent;
					sendEvent2Page(tabPageId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"][
						"OnStopDiscreetlistenFailure"]);
				}
			}
		}
	}
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * ambienceListen
 * @param {Object} param {
 *  to:,
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.ambienceListen = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "ambienceListen: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "ambienceListen: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "ambienceListen: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "ambienceListen: The to is invalid";
		param["callback"](result);
		return;
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (callStatusMgr.currentCallState == callStatusMgr.RINGING) {
		result["rsp"] = "-4";
		result["desc"] = "ambienceListen: There is a call is ringing."
		param["callback"](result);
		return;
	}

	if (callStatusMgr.isAnyCallExisted()) {
		result["rsp"] = "-6";
		result["desc"] = "ambienceListen: The voice dial is conflicted with other calls";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"to": param["to"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"] + "/ambiencelisten";
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * dialout
 * @param {Object} param {
 *  to:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.dialout = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "dialout: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "dialout: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "dialout: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "dialout: The to is invalid";
		param["callback"](result);
		return;
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (callStatusMgr.currentCallState == callStatusMgr.RINGING) {
		result["rsp"] = "-4";
		result["desc"] = "dialout: There is a call is ringing."
		param["callback"](result);
		return;
	}

	if (callStatusMgr.numberOfCall[callStatusMgr.VOICE] + callStatusMgr.numberOfCall[callStatusMgr.VOICECONF] >
		cloudICP.dispatch.voice.MAX_VOICE_NUM) {
		result["rsp"] = "-5";
		result["desc"] = "dialout: The number of voice call is limited to " + cloudICP.dispatch.voice.MAX_VOICE_NUM;
		param["callback"](result);
		return;
	}

	if (callStatusMgr.isAnyCallExisted()) {
		result["rsp"] = "-6";
		result["desc"] = "dialout: The voice dial is conflicted with other calls";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "ticDial",
		"to": param["to"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "PUT",
		"url": url,
		"data": requestParam,
		"callback": param["callback"]
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * voiceBroadcast
 * @param {Object} param {
 *  to:
 *  audioFile:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.voiceBroadcast = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "dial: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || (undefined == param["audioFile"] && undefined == param["audioBase64"]) || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "dial: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "dial: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "dial: The to is invalid";
		param["callback"](result);
		return;
	}

	if (undefined != param["audioFile"] && !cloudICP.util.checkFileType(param["audioFile"],"wav")) {
        result["desc"] = "dial: The audioFile is invalid";
        param["callback"](result);
        return;
    }

	var requestParam = {
		"opt": "ticDial",
		"to": param["to"]
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "PUT",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	callStatusMgr.saveBroadcastFile(param["to"], param["audioFile"], param["audioBase64"]);
	cloudICP.util.ajax(ajaxCfg);
}

/**
 * releaseDialout
 * @param {Object} param {
 *  cid:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.releaseDialout = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "releaseDialout: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "releaseDialout: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "releaseDialout: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "releaseDialout: The cid is invalid";
		param["callback"](result);
		return;
	}

	var call = cloudICP.util.callStatusMgr.allCalls[param["cid"]];
	if (call && call["caller"] != cloudICP.userInfo["isdn"] && call.callState == cloudICP.util.callStatusMgr
		.RINGING) {
		result["rsp"] = "-4";
		result["desc"] = "releaseDialout: The callee can't release a ringing call.";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"] + "/ticclose/" + param[
		"cid"];
	var requestParam = {};
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "DELETE",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "1") {
				data["rsp"] = "0";
			}
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * intercept
 * @param {Object} param {
 *  to:
 *  callback: ,
 * }
 */
ICPSDK_Dispatch_Voice.prototype.intercept = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "intercept: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "intercept: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "intercept: The user does not logined";
		param["callback"](result);
		return;
	}

	result["rsp"] = "-2";
	if (!cloudICP.util.checkIsdn(param["to"])) {
		result["desc"] = "intercept: The to is invalid";
		param["callback"](result);
		return;
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (callStatusMgr.currentCallState == callStatusMgr.RINGING) {
		result["rsp"] = "-4";
		result["desc"] = "intercept: There is a call is ringing."
		param["callback"](result);
		return;
	}

	if (callStatusMgr.numberOfCall[callStatusMgr.VOICE] + callStatusMgr.numberOfCall[callStatusMgr.VOICECONF] >
		cloudICP.dispatch.voice.MAX_VOICE_NUM) {
		result["rsp"] = "-5";
		result["desc"] = "intercept: The number of voice call is limited to " + cloudICP.dispatch.voice
			.MAX_VOICE_NUM;
		param["callback"](result);
		return;
	}

	if (callStatusMgr.isAnyCallExisted()) {
		result["rsp"] = "-6";
		result["desc"] = "intercept: The voice dial is conflicted with other calls";
		param["callback"](result);
		return;
	}

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"] + "/breakin/" + param["to"];
	var requestParam = {};
	requestParam["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "PUT",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * hold
 * @param {
 *  cid:
 *  callback:
 * }
 */
ICPSDK_Dispatch_Voice.prototype.hold = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "hold: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "hold: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "hold: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "hold: The cid is invalid";
		param["callback"](result);
		return;
	}

	var call = cloudICP.util.callStatusMgr.allCalls[param["cid"]];
	if (!call) {
		result["rsp"] = "-4";
		result["desc"] = "hold: The call is not existed";
		param["callback"](result);
		return;
	} else if (call["callState"] != cloudICP.util.callStatusMgr.CONNECTING) {
		result["rsp"] = "-4";
		result["desc"] = "hold: The call is not existed";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "hold",
		"param": {
			type: "0",
			cid: param["cid"]
		}
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "0") {
				cloudICP.util.callStatusMgr.holdMgr.push(param["cid"]);
			}
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};

/**
 * unhold
 * @param {
 *  cid:
 *  callback:
 * }
 */
ICPSDK_Dispatch_Voice.prototype.unhold = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unhold: param is invalid"
		});
		return;
	}

	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "unhold: callback is not a function"
		});
		return;
	}

	var result = {
		"rsp": "-3",
		"desc": ""
	};

	if (!cloudICP.userInfo["isdn"]) {
		result["desc"] = "unhold: The user does not logined";
		param["callback"](result);
		return;
	}
	//check param valid
	result["rsp"] = "-2";
	if (!cloudICP.util.checkCid(param["cid"])) {
		result["desc"] = "unhold: The cid is invalid";
		param["callback"](result);
		return;
	}

	var call = cloudICP.util.callStatusMgr.allCalls[param["cid"]];
	if (!call) {
		result["rsp"] = "-4";
		result["desc"] = "unhold: The call is not existed";
		param["callback"](result);
		return;
	} else if (call["callState"] != cloudICP.util.callStatusMgr.HOLD) {
		result["rsp"] = "-2";
		result["desc"] = "unhold: The cid is not holded";
		param["callback"](result);
		return;
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (cloudICP.util.callStatusMgr.numberOfCall[callStatusMgr.VIDEO] + cloudICP.util.callStatusMgr.numberOfCall[
			callStatusMgr.VIDEOCONF] > 0) {
		result["rsp"] = "-7";
		result["desc"] = "unhold: There is a video existed";
		param["callback"](result);
		return;
	}

	var callStatusMgr = cloudICP.util.callStatusMgr;
	if (cloudICP.util.callStatusMgr.numberOfCall[callStatusMgr.DISCREET] + cloudICP.util.callStatusMgr.numberOfCall[
			callStatusMgr.HALFVOICE] > 0) {
		result["rsp"] = "-8";
		result["desc"] = "unhold: There is a discreet or halfdial existed";
		param["callback"](result);
		return;
	}

	var requestParam = {
		"opt": "unhold",
		"param": {
			type: "0",
			cid: param["cid"]
		}
	};

	var url = cloudICP.getSdkServerUrl() + "/v1/voicecall/" + cloudICP.userInfo["isdn"];
	requestParam["param"]["reqId"] = tabPageId;
	var ajaxCfg = {
		"type": "POST",
		"url": url,
		"data": requestParam,
		"callback": function(data) {
			if (data["rsp"] == "0") {
				cloudICP.util.callStatusMgr.holdMgr.push(param["cid"]);
			}
			param["callback"](data);
		}
	}
	cloudICP.util.ajax(ajaxCfg);
};
function ICPSDK_Dispatch_WebSocket() {
	this._sdkServerWs = null;
	this._localDeamonWs = null;
	this._medisServerWs = null;
	this._mediaSdkRspFuncs = {};
	this._mediaDataServerWs = null;

	this.reconnectMediaServiceId = -1;
	this.reconnectMediaDataServiceId = -1;
	this.reconnectLocalDeamonId = -1;

	this.isMSPConflicted = false;
	this.needRecontMediaServer = true;
	this.needRecontMediaDataServer = true;
	this.needRecontLocalDeamonServer = true;
};



/**
 * 连接本地Deamon线程
 */
ICPSDK_Dispatch_WebSocket.prototype.connectToLocalDeamon = function() {
	if (this._localDeamonWs) {
		return;
	}

	var curTime = cloudICP.util.getCurrentDate();
	if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToLocalDeamon: WebSocket connection begin.")) {
		console.log("[INFO][" + curTime + "] connectToLocalDeamon: WebSocket connection begin.");
	};

	if (typeof MozWebSocket != "undefined") {
		this._localDeamonWs = new MozWebSocket(cloudICP.getLocalDeamonUrl());
	} else {
		this._localDeamonWs = new WebSocket(cloudICP.getLocalDeamonUrl());
	}

	// 监听sdkserver websocket建立事件
	this._localDeamonWs.onopen = function(event) {
		clearInterval(cloudICP.dispatch.webSocket.reconnectLocalDeamonId);
		cloudICP.dispatch.webSocket.reconnectLocalDeamonId = -1;
		clearInterval(cloudICP.dispatch.webSocket.reconnectMediaServiceId);
		cloudICP.dispatch.webSocket.reconnectMediaServiceId = -1;

		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToLocalDeamon: WebSocket connection open.")) {
			console.log("[INFO][" + curTime + "] connectToLocalDeamon: WebSocket connection open.");
		};
	};

	this._localDeamonWs.onclose = function(event) {
		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToLocalDeamon: WebSocket connection closed.")) {
			console.log("[INFO][" + curTime + "] connectToLocalDeamon: WebSocket connection closed.");
		};
		cloudICP.dispatch.webSocket._localDeamonWs = null;
		clearInterval(cloudICP.dispatch.webSocket.reconnectMediaServiceId);
		cloudICP.dispatch.webSocket.reconnectMediaServiceId = -1;

		if (cloudICP.dispatch.webSocket.reconnectLocalDeamonId == -1 && cloudICP.dispatch.webSocket.needRecontLocalDeamonServer) {
			cloudICP.dispatch.webSocket.reconnectLocalDeamonId = setInterval(function() {
				if (cloudICP.dispatch.webSocket._localDeamonWs == null ) {
					cloudICP.dispatch.webSocket.connectToLocalDeamon();
				}
			}, 5000);
		}

		cloudICP.reportDispatchSdkStatus({
			"status": "1",
			"desc": "connect to deamon thread failed."
		});
	};

	// 监听sdkserver websocket错误事件
	this._localDeamonWs.onerror = function(event) {
		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToLocalDeamon: WebSocket error observed.")) {
			console.error("[INFO][" + curTime + "] connectToLocalDeamon: WebSocket error observed.");
		};
	};

	// 监听sdkserver websocket消息事件
	this._localDeamonWs.onmessage = function(event) {
		if (event.data == "this session has been downgraded") {
			cloudICP.dispatch.webSocket.needRecontLocalDeamonServer = false;
			var param = "{\"description\":\"end_this_link_MSP_D\", \"cmd\": 196613}";
			this._localDeamonWs.send(param);
		}
		var data = JSON.parse(event.data);
		cloudICP.dispatch.webSocket.connectToMediaServer();
		cloudICP.dispatch.webSocket.connectToMediaDataServer();
		if (data["rsp"] != "131074") {
			setTimeout(function() {
				cloudICP.util.log(0, { "logLevel": "info", "logMsg": "recv Deamon msg:" + event.data });
			}, 0);
		}
		if (data["msgType"] == "MspServiceStartUp") {
			cloudICP.userInfo["mspVersion"] = data["version"];
		}
		cloudICP.dispatch.event.delMSPNotify(data);
	};
}

/**
 * 连接本地媒体线程
 */
ICPSDK_Dispatch_WebSocket.prototype.connectToMediaServer = function() {
	if (this._medisServerWs) {
		return;
	}
	var curTime = cloudICP.util.getCurrentDate();
	if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToMediaServer: WebSocket connection begin.")) {
		console.log("[INFO][" + curTime + "] connectToMediaServer: WebSocket connection begin.");
	};

	if (typeof MozWebSocket != "undefined") {
		this._medisServerWs = new MozWebSocket(cloudICP.getMediaServerUrl());
	} else {
		this._medisServerWs = new WebSocket(cloudICP.getMediaServerUrl());
	}
	// 监听sdkserver websocket建立事件
	this._medisServerWs.onopen = function(event) {
		clearInterval(cloudICP.dispatch.webSocket.reconnectMediaServiceId);
		cloudICP.dispatch.webSocket.reconnectMediaServiceId = -1;

		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToMediaServer: WebSocket connection open.")) {
			console.log("[INFO][" + curTime + "] connectToMediaServer: WebSocket connection open.");
		};

		//this._medisServerWs.send("Login");
		this._medisServerWs.send("ForceLogin");
	}.bind(this);

	this._medisServerWs.onclose = function(event) {
		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToMediaServer: WebSocket connection closed.")) {
			console.log("[INFO][" + curTime + "] connectToMediaServer: WebSocket connection closed.");
		};

		cloudICP.dispatch.webSocket._medisServerWs = null;

		if (cloudICP.dispatch.webSocket.reconnectMediaServiceId == -1 && cloudICP.dispatch.webSocket
			.reconnectLocalDeamonId == -1 && cloudICP.dispatch.webSocket.needRecontMediaServer) {
			cloudICP.dispatch.webSocket.reconnectMediaServiceId = setInterval(function() {
				if (cloudICP.dispatch.webSocket._medisServerWs == null ) {
					cloudICP.dispatch.webSocket.connectToMediaServer();
				}
			}, 5000);
		}

		cloudICP.reportDispatchSdkStatus({
			"status": "2",
			"desc": "connect to media server failed."
		});
	};

	// 监听sdkserver websocket错误事件
	this._medisServerWs.onerror = function(event) {
		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToMediaServer: WebSocket error observed.")) {
			console.error("[INFO][" + curTime + "] connectToMediaServer: WebSocket error observed.");
		};
	};

	// 监听sdkserver websocket消息事件
	this._medisServerWs.onmessage = function(event) {
		var data = {};
		if (cloudICP.util.isJsonString(event.data)) {
			var curTime = cloudICP.util.getCurrentDate();
			data = JSON.parse(event.data);
			if (data && data["rsp"] != "131074") {
				setTimeout(function() {
					cloudICP.util.log(0, { "logLevel": "debug", "logMsg": "recv mediaServer msg:" + event.data });
				}, 0);
			}
		} else {
			setTimeout(function() {
				console.log("recv mediaServer msg:" + event.data);
			}, 0);
		}
		var ret = this.dealMSPEvent(event);
		if (ret == true) {
			return;
		}
		var cmd = data["rsp"] + "";
		if (typeof this._mediaSdkRspFuncs[cmd] == "function") {
			this._mediaSdkRspFuncs[cmd](data);
		} else {
			cloudICP.dispatch.event.delMSPNotify(data);
		}
	}.bind(this);
}

/**
 * 连接本地媒体数据透传线程
 */
ICPSDK_Dispatch_WebSocket.prototype.connectToMediaDataServer = function() {
	if (this._mediaDataServerWs) {
		return;
	}
	var curTime = cloudICP.util.getCurrentDate();
	if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToMediaDataServer: WebSocket connection begin.")) {
		console.log("[INFO][" + curTime + "] connectToMediaDataServer: WebSocket connection begin.");
	};

	if (typeof MozWebSocket != "undefined") {
		this._mediaDataServerWs = new MozWebSocket(cloudICP.getMediaDataServerUrl());
	} else {
		this._mediaDataServerWs = new WebSocket(cloudICP.getMediaDataServerUrl());
	}
	this._mediaDataServerWs.binaryType = 'arraybuffer';
	// 监听透传数据 websocket建立事件
	this._mediaDataServerWs.onopen = function(event) {
		clearInterval(cloudICP.dispatch.webSocket.reconnectMediaDataServiceId);
		cloudICP.dispatch.webSocket.reconnectMediaDataServiceId = -1;

		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToMediaDataServer: WebSocket connection open.")) {
			console.log("[INFO][" + curTime + "] connectToMediaDataServer: WebSocket connection open.");
		};
	}.bind(this);

	this._mediaDataServerWs.onclose = function(event) {
		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToMediaDataServer: WebSocket connection closed.")) {
			console.log("[INFO][" + curTime + "] connectToMediaDataServer: WebSocket connection closed.");
		};

		cloudICP.dispatch.webSocket._mediaDataServerWs = null;

		if (cloudICP.dispatch.webSocket.reconnectMediaDataServiceId = -1 && cloudICP.dispatch.webSocket.needRecontMediaDataServer) {
			cloudICP.dispatch.webSocket.reconnectMediaDataServiceId = setInterval(function() {
				if (cloudICP.dispatch.webSocket._mediaDataServerWs == null ) {
					cloudICP.dispatch.webSocket.connectToMediaDataServer();
				}
			}, 5000);
		}

		cloudICP.reportDispatchSdkStatus({
			"status": "2",
			"desc": "connect to media data server failed."
		});
	};

	// 监听透传数据 websocket错误事件
	this._mediaDataServerWs.onerror = function(event) {
		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToMediaDataServer: WebSocket error observed.")) {
			console.error("[INFO][" + curTime + "] connectToMediaDataServer: WebSocket error observed.");
		};
	};

	// 监听透传数据 websocket消息事件
	this._mediaDataServerWs.onmessage = function(event) {
		var data = {};
		if (event.data instanceof ArrayBuffer) {
			const decoder = new TextDecoder("utf-8");
            const textData = decoder.decode(event.data);

			cloudICP.util.parseMediaData(event.data, data);
		}

		cloudICP.dispatch.event.delMSPNotify(data);
	}.bind(this);
}


/**
 * 发送消息给本地媒体
 */
ICPSDK_Dispatch_WebSocket.prototype.sendDataToMediaSDK = function(data, isLog) {
	var sendStr;
	if (isLog == true) {
		setTimeout(function() {
			if (undefined != data["param"] && undefined != data["param"]["mediaInfo"] && undefined != data["param"]["mediaInfo"]["audioBase64"]) {
			} else {
				cloudICP.util.log(0, { "logLevel": "debug", "logMsg": "send mediaServer msg:" + JSON.stringify(data) });
			}
		}, 0);
	}
	if (undefined != data["cmd"]) {
		sendStr = JSON.stringify(data);
	} else {
		sendStr = "" + data;
	}
	if (!this._medisServerWs) {
		return;
	}
	this._medisServerWs.send(sendStr);
}

/**
 * 发送消息给本地媒体
 */
ICPSDK_Dispatch_WebSocket.prototype.sendDataToDeamonSDK = function(data, isLog) {
	if (isLog == true) {
		setTimeout(function() {
			cloudICP.util.log(0, { "logLevel": "info", "logMsg": "send Deamon msg:" + JSON.stringify(data) });
		}, 0);
	}
	if (!this._localDeamonWs) {
		return;
	}
	if (undefined != data["cmd"]) {
		var sendStr = JSON.stringify(data);

		this._localDeamonWs.send(sendStr);

	} else {
		this._localDeamonWs.send(data);
	}
}



/**
 * 获取ip
 * @param param {
 * callback :
 * }
 */
ICPSDK_Dispatch_WebSocket.prototype.getLocalIp = function(param) {
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	var msg = {
		"description": "msp_get_local_ip",
		"cmd": 0x20001,
		"param": {
			"server": cloudICP.config["serverAddress"],
			"serverDomain": cloudICP.config["serverDomain"]
		}
	};

	if (undefined == param) {
		cloudICP.util.log(undefined, { "logLevel": "warn", "logMsg": "getLocalIp: param is invalid."});
	}
	if (undefined == param["callback"] || typeof param["callback"] != "function") {
		cloudICP.util.log(undefined, { "logLevel": "warn", "logMsg": "getLocalIp: callback is not a function."});
	}
	if ((undefined != param) && (undefined != param["callback"]) && (typeof param["callback"] == "function")) {
		this._mediaSdkRspFuncs[msg.cmd + ""] = param["callback"];
	} else {
		this._mediaSdkRspFuncs[msg.cmd + ""] = this.getLocalIpCallBack;
	}
	cloudICP.util.log(undefined, { "logLevel": "info", "logMsg": "send getLocalIp."});
	this.sendDataToMediaSDK(msg, true);
}

/**
 * 修改mdc ip
 * @param param {
 * callback :
 * }
 */
ICPSDK_Dispatch_WebSocket.prototype.ModifyMdcIp = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;
	if (param["value"] != undefined) {
		param = param.value;
	}
	if (undefined == param) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "ModifyMdcIp: mdcip is null"
		});
		return;
	}
	if (!cloudICP.util.checkIp(param["mdcip"])) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "ModifyMdcIp: mdcip is invalid"
		});
		return;
	}

	var msg = {
		"description": "modify_mdcip",
		"cmd": 0x20008,
		"param": {
			"mdcip": param["mdcip"],
			"serverDomain": cloudICP.config["serverDomain"]
		},
		"reqId": tabPageId
	};

	this._mediaSdkRspFuncs[msg.cmd + ""] = this.getModifyMdcip.bind(this);

	this.sendDataToMediaSDK(msg, true);
}
/**
 * 获取摄像头信息
 * @param param {
 * callback :
 * }
 */
ICPSDK_Dispatch_WebSocket.prototype.getCameraInfo = function(param) {
	var msg = {
		"description": "msp_get_video_support",
		"cmd": 0x20007
	};
	this._mediaSdkRspFuncs[msg.cmd + ""] = this.getCameraInfoCallBack;
	this.sendDataToMediaSDK(msg, true);
}

/**
 * 处理获取IP的结果
 */
ICPSDK_Dispatch_WebSocket.prototype.getCameraInfoCallBack = function(data) {
	var reqId = 0;
	if (0 == data["result"]) {
		if (data["support"] == 0) {
			data["support"] = 127;
		}
		cloudICP.config["cameraInfo"] = data["support"];
		if (cloudICP.config["cameraInfo"] && cloudICP.config["localIP"]) {
			cloudICP.reportDispatchSdkStatus({
				"status": "0",
				"desc": "get localip and cameraInfo success.",
				"mdcip": cloudICP.config["mdcip"]
			});
		}
		if ("begin" == cloudICP.config["installMspStatus"] || true == data["msp_first_use"]) {	// 提示MSP刚安装好，首次使用
			var retEvent = {
				"eventName": "OnInstallMspEvent",
				"status": "end"
			}
			delete cloudICP.config.installMspStatus;
			cloudICP.dispatch.event.EVENT_LIST["MSPNotify"]["OnInstallMspEvent"].event = retEvent;
			sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["MSPNotify"]["OnInstallMspEvent"]);
			cloudICP.dispatch.webSocket.ModifyMdcIp({ mdcip: cloudICP.config["mdcip"],callback: cloudICP.dispatch.webSocket.getModifyMdcip });
		}
		if (true == data["msp_first_use"]) {// 刷新MSP状态，不再是首次使用
			var updateMspUseStatus = "{\"description\":\"msp_use_status_update\", \"cmd\": 131085}".format();
			cloudICP.dispatch.webSocket.sendDataToMediaSDK(updateMspUseStatus, true);
		}
		cloudICP.dispatch.webSocket.getLocalIp({
			callback: cloudICP.dispatch.webSocket.getLocalIpCallBack
		});

	} else {
		cloudICP.reportDispatchSdkStatus({
			"status": "4",
			"desc": "get cameraInfo failed !"
		});
	}
}

ICPSDK_Dispatch_WebSocket.prototype.getLocalIpCallBack = function(data) {
	var reqId = data.reqId;
	reqId = 0;
	if (0 == data["result"]) {
		cloudICP.config["localIP"] = data["localIP"];
		cloudICP.config["mdcip"] = data["mdcip"];
		if (cloudICP.config["mspVersion"] != data["mspVersion"]) {
			var event = {};
			event["eventName"] = "OnUpdateMsp";
			event["moduleType"] = "1";
			event["mspVersion"] = data["mspVersion"];
			event["desc"] = "msp版本(" + data["mspVersion"] + ")与jssdk版本(" + cloudICP.config["mspVersion"] +
				")不同，请安装对应版本msp";
			event["downloadUrl"] = "http://" + cloudICP.config.serverDomain + ":8009/mspversion/" + cloudICP.config.mspVersion + "/eSDK_ICP_MSP.zip";
			cloudICP.dispatch.event.EVENT_LIST["MSPNotify"]["OnUpdateMsp"].event = event;
			sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["MSPNotify"]["OnUpdateMsp"]);
			try {
				if (undefined != typeof(document)) {
					var messageElement = document.getElementById('open-msp-result-message');
					if (messageElement) {
						var strUrl = "http://" + cloudICP.config.serverDomain + ":8009/mspversion/" + cloudICP.config.mspVersion + "/eSDK_ICP_MSP.zip";
						var strVersion = "MSP_V" + cloudICP.config.mspVersion + ".zip";
						messageElement.innerHTML = '<b id="checkmsp-message" style="text-align:left;color:#0000FF;font-size:18px">检测结果：</b>'
						+ '<b style="text-align:left;font-size:18px"> MSP 插件已运行安装版本：</b>'
						+ '<b style="color:blue;font-size:14px">MSP_V' + data["mspVersion"] + '.zip</b>'
						+ '<b style="text-align:left;font-size:18px">，目标版本下载链接: </b>'
						+ '<a style="color:red;font-size:16px" href="' + strUrl + '">' + strVersion + '</a>';
					}
				}
			} catch(e) {}
		}
		cloudICP.util.log(undefined, { "logLevel": "info", "logMsg": "{\"localIP\":\"" + data["localIP"] + "\", mdcip:\"" + data["mdcip"] +
		"\", mspVersion:\"" + data["mspVersion"] + "\"}" });
		// $('#msp-version').html("[mspVersion:" + cloudICP.config["mspVersion"] + "]");
		if (cloudICP.config["cameraInfo"] && cloudICP.config["localIP"]) {
			cloudICP.reportDispatchSdkStatus({
				"status": "0",
				"desc": "get localip and cameraInfo success."
			});
		}
	} else {
		cloudICP.reportDispatchSdkStatus({
			"status": "3",
			"desc": "get localIP failed."
		});
	}
}

/**
 * connectToSDKServer()
 */
ICPSDK_Dispatch_WebSocket.prototype.connectToSDKServer = function() {
	if (this._sdkServerWs) {
		this._sdkServerWs.close();
		this._sdkServerWs = null;
	}
	var curTime = cloudICP.util.getCurrentDate();
	if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToSDKServer: WebSocket connection begin.")) {
		console.log("[INFO][" + curTime + "] connectToSDKServer: WebSocket connection begin.");
	};
	if (typeof MozWebSocket != "undefined") {
		this._sdkServerWs = new MozWebSocket(cloudICP.getSdkServerWssUrl());
	} else {
		this._sdkServerWs = new WebSocket(cloudICP.getSdkServerWssUrl());
	}
	// 监听sdkserver websocket建立事件
	this._sdkServerWs.onopen = function(event) {
		var curTime = cloudICP.util.getCurrentDate();
		var request = {
			cmd: 'registerEvents',
			isdn: cloudICP.userInfo.isdn,
			session: cloudICP.userInfo.session
		}
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToSDKServer: WebSocket connection opened.")) {
			console.log("[INFO][" + curTime + "] connectToSDKServer: WebSocket connection opened.");
		};
		this.sendAuthToSDKServer(request);
	}.bind(this);

	this._sdkServerWs.onclose = function(event) {
		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToSDKServer: WebSocket connection closed.")) {
			console.log("[INFO][" + curTime + "] connectToSDKServer: WebSocket connection closed.");
		};
	};

	// 监听sdkserver websocket错误事件
	this._sdkServerWs.onerror = function(event) {
		var curTime = cloudICP.util.getCurrentDate();
		if (!sendMsg2Page(0, messageType.log, "[INFO][" + curTime + "] connectToSDKServer: WebSocket error observed.")) {
			console.error("[INFO][" + curTime + "] connectToSDKServer: WebSocket error observed.");
		};
	};

	// 监听sdkserver websocket消息事件
	this._sdkServerWs.onmessage = function(event) {
		setTimeout(function() {
			cloudICP.util.log(0, { "logLevel": "info", "logMsg": "recv sdkServer msg:" + event.data });
		}, 0);
		this.dealMessage(event.data);
	}.bind(this);
}

/**
 * handle the websocket event
 */
ICPSDK_Dispatch_WebSocket.prototype.dealMessage = function(event) {
	var jsonObject = JSON.parse(event);
	var cmd = jsonObject["cmd"];
	var rid = jsonObject["rid"];
	if (undefined == cmd) {
		cloudICP.util.log(undefined, { "logLevel": "warn", "logMsg": "dealMessage: cmd is invalid." });
		return;
	}
	if (undefined != jsonObject["rid"]) {
		delete jsonObject.rid;
	}
	if (undefined != jsonObject["seq"]) {
		delete jsonObject.seq;
	}
	delete jsonObject.cmd;
	switch (cmd) {
		case "voiceNotify": //语音状态通知处理
			cloudICP.dispatch.event.delVoiceNotify(jsonObject);
			break;
		case "videoNotify": //视频状态通知处理
			cloudICP.dispatch.event.delVideoNotify(jsonObject);
		case "groupCallNotify": //组呼状态通知处理
			cloudICP.dispatch.event.delGroupCallNotify(jsonObject);
			break;
		case "commonNotify": //群组更新通知处理
			cloudICP.dispatch.event.delCommonNotify(jsonObject);
			break;
		case "msNotify": //短彩信更新通知处理
			cloudICP.dispatch.event.delMsNotify(jsonObject);
			break;
		case "gisNotify": //gis状态通知处理
			cloudICP.dispatch.event.delGisNotify(jsonObject);
			break;
		case "resourceNotify": //资源状态通知处理
			jsonObject["rid"] = rid;
			cloudICP.dispatch.event.delResourceNotify(jsonObject);
			break;
		case "moduleNotify": //模块状态通知处理
			cloudICP.dispatch.event.delModuleNotify(jsonObject);
			break;
		case "phoneConfNotify": //音视频会议通知
			break;
		case "200006": //组呼音频通知
			cloudICP.dispatch.event.delResourceNotify(JSON.parse(event));
			break;
		case "registerEvents": //websocket连接建立成功通知
			cloudICP.loginFlag = true;
			break;
	        case "broadcastInfoEvents": //处理三方广播通知消息
	            cloudICP.dispatch.event.delBroadcastInfoNotify(jsonObject);
	            break;
		default:
			cloudICP.util.log(undefined, { "logLevel": "warn", "logMsg": "dealMessage: unknow event. The event is " + JSON.stringify(event) });
	}
}

/**
 * send() send request by websocket
 * @param {Object} req request
 */
ICPSDK_Dispatch_WebSocket.prototype.sendAuthToSDKServer = function(req) {
	this._sdkServerWs.send(JSON.stringify(req));
};

ICPSDK_Dispatch_WebSocket.prototype.dealMSPEvent = function(event) {
	var reqId = event.reqId;
	reqId = 0;
	if (event != undefined && event.data != undefined) {
		if (event.data == "LoginSuccessful") {
			var mspStatusEvent = {};
			mspStatusEvent["eventName"] = "OnMspInstallRunStatus";
			mspStatusEvent["moduleType"] = "1";
			mspStatusEvent["status"] = 4;//1: 未安装 2:已安装 3:未运行 4:运行中
			cloudICP.dispatch.event.EVENT_LIST["MSPNotify"]["OnMspInstallRunStatus"].event = mspStatusEvent;
			sendEvent2Page(0, cloudICP.dispatch.event.EVENT_LIST["MSPNotify"]["OnMspInstallRunStatus"]);
			if (true == cloudICP.config["hasExeCheckMsp"]) {
				cloudICP.dispatch.webSocket.ModifyMdcIp({ mdcip: cloudICP.config.setMDCIP, callback: cloudICP.dispatch.webSocket.getModifyMdcip });
			}
			this.getCameraInfo();

			cloudICP.dispatch.webSocket.isMSPConflicted = false;
			return true;
		} else if (event.data == "LoginFailed") {
			var event = {};
			event["eventName"] = "OnDisConnection";
			event["moduleType"] = "4";
			cloudICP.dispatch.event.EVENT_LIST["ModuleNotify"]["OnDisConnection"].event = event;
			sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["ModuleNotify"]["OnDisConnection"]);

			cloudICP.dispatch.webSocket.isMSPConflicted = true;
			delete cloudICP.config["localIP"];
			delete cloudICP.config["cameraInfo"];

			return true;
		} else if (event.data == "this session has been downgraded") {
			cloudICP.dispatch.webSocket.needRecontMediaServer = false;
			cloudICP.dispatch.webSocket.needRecontMediaDataServer = false;
			var param = "end_this_link_MSP_S";
			this._medisServerWs.send(param);
			var event = {};
			event["eventName"] = "OnDisConnection";
			event["moduleType"] = "4";
			cloudICP.dispatch.event.EVENT_LIST["ModuleNotify"]["OnDisConnection"].event = event;
			sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["ModuleNotify"]["OnDisConnection"]);

			cloudICP.dispatch.webSocket.isMSPConflicted = true;
			delete cloudICP.config["localIP"];
			delete cloudICP.config["cameraInfo"];

			cloudICP.dispatch.auth.unifiedLogout({
				"callback": function(data) {
					// Do nothing
				}
			});

			return true;
		} else if (event.data == "this session has been downgraded, cannot make any process") {
			delete cloudICP.config["localIP"];
			delete cloudICP.config["cameraInfo"];
			return true;
		}

		return false;
	} else {
		return false;
	}
};
/* eslint-disable no-undef */

/**
 * CloudICP 22.0
 * version: ICP eAPP610_TD V100R022C00SPC620
 */
var sdkVersion = "022.650.160";
var cloudICP;

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
    /**
     *
     * @param {*} config {
     *  serverAddress: "", //eg:
     *  serverWSPort: "8015", //
     *  serverHttpPort : "9898",
     *  debugMode: "false",
     *  localMediaServiceAudioPort : ""，
     *  localMediaServiceVideoPort : "",
     *  localMediaServiceWSPort : "",
	 *  localMediaDataServiceWSPort : "",
     *  localDeamonServiceWSPort : "",
     *  sdkStatusNotify:
     * }
     */
     function ICPSDK(config) {

        this.util = new ICPSDK_Util();
        if (!config["serverAddress"] || !this.util.checkIp(config["serverAddress"])) {
            this.util.log({ "logLevel": "error", "logMsg": "serverAddress is invalid" });
            throw new Error("serverAddress is invalid");
            return;
        }

		if (!config["serverDomain"]) {
			config["serverDomain"] = "sdkserver.cloudicp.huawei.com";
		}

        if (undefined == config["videoConfLocalWindow"]) {
            config["videoConfLocalWindow"] = true;
        } else {
            config["videoConfLocalWindow"] = config["videoConfLocalWindow"];
        }

        if (undefined == config["videoResizeMode"]) {
            config["videoResizeMode"] = 0;
        } else {
            config["videoResizeMode"] = config["videoResizeMode"];
        }

        if (undefined == config["demo_wssflow_enable"]) {
            config["demo_wssflow_enable"] = false;
        } else {
            config["demo_wssflow_enable"] = config["demo_wssflow_enable"];
        }

        if (!config["serverWSPort"] || !this.util.checkPort(config["serverWSPort"])) {
            config["serverWSPort"] = "8015";
        }

        if (!config["serverHttpPort"] || !this.util.checkPort(config["serverHttpPort"])) {
            config["serverHttpPort"] = "8015";
        }

        if (!config["debugMode"] || (config["debugMode"] != "true" && config["debugMode"] != "false")) {
            config["debugMode"] = "false";
        }

        if (!config["localMediaServiceAudioPort"] || !this.util.checkPort(config["localMediaServiceAudioPort"])) {
            config["localMediaServiceAudioPort"] = "8002";
        }

        if (!config["localMediaServiceVideoPort"] || !this.util.checkPort(config["localMediaServiceVideoPort"])) {
            config["localMediaServiceVideoPort"] = "8002";
        }

        if (!config["localMediaServiceWSPort"] || !this.util.checkPort(config["localMediaServiceWSPort"])) {
            config["localMediaServiceWSPort"] = "17802";
        }

        if (!config["localDeamonServiceWSPort"] || !this.util.checkPort(config["localDeamonServiceWSPort"])) {
            config["localDeamonServiceWSPort"] = "17801";
        }

        if (!config["mrsServerIP"] || !this.util.checkIp(config["mrsServerIP"])) {
            config["mrsServerIP"] = "";
        }

        if (!config["mode"]) {
            config["mode"] = "window";
        }

        if (!config["ringFlag"]) {
            config["ringFlag"] = "0";
        }

        if (!config["HDVideoMode"]) {
            config["HDVideoMode"] = false;
        }

		// 录音录像路径 默认为： MSP安装目录下的\data\rec目录中
		if (undefined == config["localRecordPath"]) {
			config["localRecordPath"] = "";
		} else {
			config["localRecordPath"] = config["localRecordPath"];
		}

		// 本地录音录像文件所在的硬盘的最小空间大小 单位 GB；
		if (undefined == config["localRecordMinDiskCap"]) {
			config["localRecordMinDiskCap"] = 5;
		} else {
			config["localRecordMinDiskCap"] = config["localRecordMinDiskCap"];
		}

        config["mspVersion"] = sdkVersion;
        config["sdkServerHttpTimeout"] = "60000";

        this.config = config;
        this.loginFlag = false;
        this.dispatch = {};
        this.grpList = [];
        this.grpCidList = {};
        this.grpVolumeList = {};
        this.dispatch.auth = new ICPSDK_Dispatch_Auth();
        this.dispatch.event = new ICPSDK_Dispatch_Event();
        this.dispatch.gis = new ICPSDK_Dispatch_GIS();
        this.dispatch.sms = new ICPSDK_Dispatch_Sms();
        this.dispatch.voice = new ICPSDK_Dispatch_Voice();
        this.dispatch.video = new ICPSDK_Dispatch_Video();
        this.dispatch.group = new ICPSDK_Dispatch_Group();
        this.dispatch.webSocket = new ICPSDK_Dispatch_WebSocket();
        this.dispatch.device = new ICPSDK_Dispatch_Device();
        this.dispatch.query = new ICPSDK_Dispatch_Query();
        this.dispatch.conf = new ICPSDK_Dispatch_Conf();
        this.userInfo = {
            "session": "",
            "isdn": "",
            "sdkVersion": sdkVersion
        };
        this.dispatchSdkStatus = "-1";
        cloudICP = this;

        // 建立一个定时器，监控是否离线
        setInterval(function() {
            if (!navigator.onLine) {
                logoutCleanUp();
                var event = {};
                event["eventName"] = "OnDisConnection";
                event["moduleType"] = "3";
                cloudICP.dispatch.event.EVENT_LIST["ModuleNotify"]["OnDisConnection"](event);
            }
        }, 10 * 1000);

        return this;
    };

    /**
     * @param data {
     *  "status" : "" 0//sdk初始化成功; 1: sdk连接mediaserver的守护进程失败；2: sdk连接mediaserver的业务进程失败；3: sdk获取本地ip失败
     *  "desc" : ""
     * }
     */
    ICPSDK.prototype.reportDispatchSdkStatus = function(data) {
        cloudICP.util.sendLogOut("sdkStatusNotify: sdkstatus is " + JSON.stringify(data));
        this.dispatchSdkStatus = data["status"];
        data["funcId"] = sdkStatusFuncId;
		sendMsg2Page(mainPageId, messageType.response, data);
    }

    ICPSDK.prototype.getSdkServerUrl = function() {
        return "https://" + this.config["serverDomain"] + ":" + this.config["serverHttpPort"] + "/sdkserver";
    }

    ICPSDK.prototype.getSdkServerWssUrl = function() {
        return "wss://" + this.config["serverDomain"] + ":" + this.config["serverWSPort"] + "/sdkserver?agentVersion=2.0&wsClientID=" + cloudICP.userInfo["loginUserName"];
    }

    ICPSDK.prototype.getMediaServerUrl = function() {
        if ((undefined == cloudICP.config["ssl_enable"]) || (cloudICP.config.ssl_enable == true)) {
            return "wss://localhost.cloudicp.huawei.com:" + this.config["localMediaServiceWSPort"];
        } else {
            return "ws://localhost.cloudicp.huawei.com:" + this.config["localMediaServiceWSPort"];
        }
    }

	ICPSDK.prototype.getMediaDataServerUrl = function() {
        if ((undefined == cloudICP.config["ssl_enable"]) || (cloudICP.config.ssl_enable == true)) {
            return "wss://localhost.cloudicp.huawei.com:" + this.config["localMediaDataServiceWSPort"];
        } else {
            return "ws://localhost.cloudicp.huawei.com:" + this.config["localMediaDataServiceWSPort"];
        }
    }

    ICPSDK.prototype.getLocalDeamonUrl = function() {
        if ((undefined == cloudICP.config["ssl_enable"]) || (cloudICP.config.ssl_enable == true)) {
            return "wss://localhost.cloudicp.huawei.com:" + this.config["localDeamonServiceWSPort"];
        } else {
            return "ws://localhost.cloudicp.huawei.com:" + this.config["localDeamonServiceWSPort"];
        }
    }

    return ICPSDK;
}));

/**
 * msp返回事件通知，UMP多窗口版本注释196610事件的创建内嵌窗口
 */
 ICPSDK_Dispatch_Event.prototype.delMSPNotify = function(event) {
	var reqId = event.reqId;
	if (reqId == null || reqId == undefined) {reqId = "0";}

	switch (event["rsp"]) {
		case 196612:
			var retEvent = {
				"eventName": "OnGetResolutionResult",
				"rsp": event["result"],
				"value": {
					"mainScreen": event["mainScreen"],
					"totalScreen": event["totalScreen"],
					"screenNum": event["screenNum"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnGetResolutionResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnGetResolutionResult"]);
			break;
		case 196610:
			//窗口创建成功
			var videoEvent = cloudICP.util.callStatusMgr.videoMediaInfoBuf[event["resID"].toString()];
			if (undefined == videoEvent) {
				if (undefined != event["userWindowId"]) { //创建用户窗口成功
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[event["userWindowId"].toString()] = {};
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[event["userWindowId"].toString()]["value"] = {};
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[event["userWindowId"].toString()]["value"][
						"windowHandle"
					] = event["windowHandle"];
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[event["userWindowId"].toString()]["value"][
						"windowId"
					] = event["userWindowId"];
					var retEvent = {
						"eventName": "OnCreateWindowResult",
						"rsp": event["result"].toString(),
						"value": {
							"userWindowId": event["userWindowId"],
							"windowHandle": event["windowHandle"],
							"width": event["width"],
							"height": event["height"],
							"posX": event["posX"],
							"posY": event["posY"],
							"showToolbar": event["showToolbar"],
							"buttonIDs": event["buttonIDs"],
							"hide": event["hide"],
							"enableDrag": event["enableDrag"]
						}
					}
					this.EVENT_LIST["MSPNotify"]["OnCreateWindowResult"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnCreateWindowResult"]);
				}
				return;
			}

			var value = videoEvent["value"];
			//通知上层OnCallConnect事件
			var callStatusMgr = cloudICP.util.callStatusMgr;
			var call = callStatusMgr.allCalls[value["cid"]];

			// 处理二次视频监控时call为NULL，导致返回给二开的calltype被设置为video的问题，应设置为monitor
			var isRepeatMonitor = false;
			if (parseInt(value["cid"]) >= 2000001001 && parseInt(value["cid"]) <= 2000002000) {
				if (typeof value["cild"] == 'number') {
					value["cid"] = value["cid"].toString();
				}
				isRepeatMonitor = true;
			}
			var newEvent = {
				"eventName": "",
				"rsp": value["rsp"],
				"value": {
					"callee": value["callee"],
					"caller": value["caller"],
					"cid": value["cid"],
					"fmt": value["fmt"],
					"width": event["width"],
					"height": event["height"]
				}
			}
			var strWssUrl = "";
			var strFmtType = value["fmtType"];
			if ((value["winMode"] == "wssflow") && (undefined != event["wssflowSslEnable"])) {
				if (event["wssflowSslEnable"] == true) {
					strWssUrl = "wss://localhost.cloudicp.huawei.com:" + event["wssflowPort"].toString();
					newEvent["value"]["wssUrl"] = strWssUrl;
				} else {
					strWssUrl = "ws://localhost.cloudicp.huawei.com:" + event["wssflowPort"].toString();
					newEvent["value"]["wssUrl"] = strWssUrl;
				}
			}
			var bNeedNotify = true;
			if ((call && call.callType == callStatusMgr.MONITOR) || isRepeatMonitor) {
				newEvent["value"]["calltype"] = "monitor";
				if (undefined != value["userWindowId"]) {
					newEvent["value"]["userWindowId"] = value["userWindowId"];
				}
			} else if (call && call.callType == callStatusMgr.VIDEOCONF) {
				newEvent["value"]["confId"] = call["confId"];
				newEvent["value"]["isVideo"] = "true";
				newEvent["eventName"] = "OnConfConnect";
				this.EVENT_LIST["PhoneConfNotify"]["OnConfConnect"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfConnect"]);

				var ajaxCfg = {
					"type": "POST",
					"url": cloudICP.getSdkServerUrl() + "/v1/phoneconf/" + cloudICP.userInfo["isdn"],
					"data": {
						"opt": "sub",
						"reqId": reqId,
						"confId": call["confId"]
					},
					"callback": function(data) {
						if (data["rsp"] != "0") {
							var retEvent = {
								"eventName": "OnSubscribeConfFailure",
								"rsp": data["rsp"],
							}
							this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfFailure"].event = retEvent;
							sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnSubscribeConfFailure"]);

						} else {
							cloudICP.util.callStatusMgr.confSubMgr[call["confId"]] = true;
						}
					}
				}
				cloudICP.util.ajax(ajaxCfg);
				bNeedNotify = false;
			} else if (call && call.callType == callStatusMgr.VIDEODISPATCH) {
				newEvent["value"]["calltype"] = "dispatch";
				newEvent["value"]["src"] = value["uri"];
				newEvent["value"]["direction"] = "in";
				newEvent["value"]["ptz"] = value["ptz"];
				newEvent["value"]["mute"] = value["mute"];
				newEvent["eventName"] = "OnCallConnect";
				this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);
				bNeedNotify = false;
			} else {
				newEvent["value"]["calltype"] = "video";
			}
			if (true == bNeedNotify) {
				newEvent["value"]["src"] = "";
				newEvent["value"]["direction"] = value["direction"];
				newEvent["value"]["ptz"] = value["ptz"];
				newEvent["value"]["mute"] = value["mute"];
				newEvent["eventName"] = "OnCallConnect";
				this.EVENT_LIST["VoiceNotify"]["OnCallConnect"].event = newEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallConnect"]);

			}
			//完成通知

			var user = cloudICP.userInfo["isdn"];
			var peerUser = value["callee"];
			if (user == peerUser) {
				peerUser = value["caller"];
			}

			var recvOnly = false;
			if (videoEvent["value"]["camera"] != "-1" &&
				videoEvent["value"]["camera"] != "0") {
				recvOnly = true;
			}

			var iPort = -1;
			var iWidth = 176;
			var iHeight = 144;
			if ((event["mode"] == "wssflow") && (event["wssflowPort"] != undefined)) {
				iPort = parseInt(event["wssflowPort"]);
				iWidth = parseInt(event["width"]);
				iHeight = parseInt(event["height"]);
				// ----/////////////////////////////////// 调用——start
				// if (!!cloudICP.config["demo_wssflow_enable"]) {
				//     if (!window.yuvIndex) {
				//         window.yuvIndex = 1;
				//     } else {
				//         window.yuvIndex++;
				//         window.yuvIndex %= 17;
				//         window.yuvIndex = window.yuvIndex == 0 ? 1 : window.yuvIndex;
				//     }
				//     const notifyMSP = function () {
				//         console.info("调用改变分辨率接口");
				//     };
				//     const box = document.getElementById(`yuv-player-00${window.yuvIndex}`);
				//     console.info('box---------', box)
				//     var baseWidth = 16;
				//     var baseHeight = 9;
				//     var baseWidthHeight = parseInt(iWidth * 9 / 16);
				//     var baseHeightWidth = parseInt(iHeight * 16 / 9);
				//     if (baseWidthHeight <= iHeight) {
				//         baseWidth = iWidth;
				//         baseHeight = baseWidthHeight;
				//     } else if (baseHeightWidth <= iWidth) {
				//         baseWidth = baseHeightWidth;
				//         baseHeight = iHeight;
				//     }
				//     box.style.width = baseWidth + 'px';
				//     box.style.height = baseHeight + 'px';
				//     new CREATE_PLAYER_DEMO({
				//         wsUrl: strWssUrl,
				//         port: iPort,
				//         box,
				//         sharpType: {
				//             width: iWidth,
				//             height: iHeight
				//         },
				//         cid: value["cid"],
				//         changeCB: notifyMSP,
				//     });
				// }
				// ----///////////////////////////////////——end
			}

			if (value && value["cid"] != undefined) {
				var callStatusMgr = cloudICP.util.callStatusMgr;
				var call = callStatusMgr.allCalls[value["cid"]];

				if (call && call.callType == callStatusMgr.VIDEOCONF) {
					recvOnly = false;
				}

				if (call && (call.callType == callStatusMgr.MONITOR || call.callType == callStatusMgr
						.VIDEODISPATCH)) {
					recvOnly = true;
				}
			}

			cloudICP.util.callStatusMgr.videoMediaInfoBuf[event["resID"].toString()]["hwnd"] = event[
				"windowHandle"];
			var bHide = false;
			if (undefined != event["hide"]) {
				bHide = event["hide"];
			}
			let localArr = value["local_video"].split(",");
			let serverArr = value["server_video"].split(",");
			var videoConfLocalWindow = true;
			if (undefined != value["videoConfLocalWindow"]) {
				videoConfLocalWindow = value["videoConfLocalWindow"];
			}
			if (value["rts_ssrc"] != undefined) {
				if (undefined != event["repeatsMonitor"]) {
					param =
						"{\"description\":\"msp_create_video_channel\", \"cmd\": 65557,  \"param\":{\"resID\":{0}, \"winHandle\":{9}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"fmt\":\"{7}\", \"recvOnly\":{8}, \"rtsSSrc\": {10}, \"user\":\"{11}\", \"peerUser\":\"{12}\", \"mode\":\"{13}\", \"port\":{14}, \"fmtType\":\"{15}\", \"width\": {16}, \"height\": {17}, \"videoConfLocalWindow\": {18}, \"videoResizeMode\": {19}, \"HDVideoMode\": {20}  }}}"
						.format(
							parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0], serverArr[
								1], parseInt(value["video"]), parseInt(value["video_ssrc"]), value["fmt"], recvOnly,
							parseInt(event["windowHandle"]), value["rts_ssrc"], user, peerUser, event["mode"],
							iPort, strFmtType, iWidth, iHeight, videoConfLocalWindow, event["videoResizeMode"], cloudICP.config["HDVideoMode"]
						);
				} else if (undefined != event["userWindowId"]) {
					param =
						"{\"description\":\"msp_create_video_channel\", \"cmd\": 65539,  \"param\":{\"resID\":{0}, \"winHandle\":{9}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"fmt\":\"{7}\", \"recvOnly\":{8}, \"rtsSSrc\": {10}, \"user\":\"{11}\", \"peerUser\":\"{12}\", \"mode\":\"{13}\", \"port\":{14}, \"fmtType\":\"{15}\", \"width\": {16}, \"height\": {17}, \"videoConfLocalWindow\": {18}, \"userWindowId\":{19}, \"videoResizeMode\": {20}, \"HDVideoMode\": {21}  }}}"
						.format(
							parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0], serverArr[
								1], parseInt(value["video"]), parseInt(value["video_ssrc"]), value["fmt"], recvOnly,
							parseInt(event["windowHandle"]), value["rts_ssrc"], user, peerUser, event["mode"],
							iPort, strFmtType, iWidth, iHeight, videoConfLocalWindow, event["userWindowId"], event["videoResizeMode"], cloudICP.config["HDVideoMode"]
						);
				} else {
					param =
						"{\"description\":\"msp_create_video_channel\", \"cmd\": 65539,  \"param\":{\"resID\":{0}, \"winHandle\":{9}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"fmt\":\"{7}\", \"recvOnly\":{8}, \"rtsSSrc\": {10}, \"user\":\"{11}\", \"peerUser\":\"{12}\", \"mode\":\"{13}\", \"port\":{14}, \"fmtType\":\"{15}\", \"width\": {16}, \"height\": {17}, \"videoConfLocalWindow\": {18}, \"videoResizeMode\": {19}, \"HDVideoMode\": {20}  }}}"
						.format(
							parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0], serverArr[
								1], parseInt(value["video"]), parseInt(value["video_ssrc"]), value["fmt"], recvOnly,
							parseInt(event["windowHandle"]), value["rts_ssrc"], user, peerUser, event["mode"],
							iPort, strFmtType, iWidth, iHeight, videoConfLocalWindow, event["videoResizeMode"], cloudICP.config["HDVideoMode"]
						);
				}
			} else {
				if (undefined != event["repeatsMonitor"]) {
					param =
						"{\"description\":\"msp_create_video_channel\", \"cmd\": 65557,  \"param\":{\"resID\":{0}, \"winHandle\":{9}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"fmt\":\"{7}\", \"recvOnly\":{8}, \"user\":\"{10}\", \"peerUser\":\"{11}\", \"mode\":\"{12}\", \"port\":{13}, \"fmtType\":\"{14}\", \"width\": {15}, \"height\": {16}, \"videoConfLocalWindow\": {17}, \"videoResizeMode\": {18}, \"HDVideoMode\": {19}  }}}"
						.format(
							parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0], serverArr[
								1], parseInt(value["video"]), parseInt(value["video_ssrc"]), value["fmt"], recvOnly,
							parseInt(event["windowHandle"]), user, peerUser, event["mode"], iPort, strFmtType,
							iWidth, iHeight, videoConfLocalWindow, event["videoResizeMode"], cloudICP.config["HDVideoMode"]
						);
				} else if (undefined != event["userWindowId"]) {
					param =
						"{\"description\":\"msp_create_video_channel\", \"cmd\": 65539,  \"param\":{\"resID\":{0}, \"winHandle\":{9}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"fmt\":\"{7}\", \"recvOnly\":{8}, \"user\":\"{10}\", \"peerUser\":\"{11}\", \"mode\":\"{12}\", \"port\":{13}, \"fmtType\":\"{14}\", \"width\": {15}, \"height\": {16}, \"videoConfLocalWindow\": {17}, \"userWindowId\":{18}, \"videoResizeMode\": {19}, \"HDVideoMode\": {20}  }}}"
						.format(
							parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0], serverArr[
								1], parseInt(value["video"]), parseInt(value["video_ssrc"]), value["fmt"], recvOnly,
							parseInt(event["windowHandle"]), user, peerUser, event["mode"], iPort, strFmtType,
							iWidth, iHeight, videoConfLocalWindow, event["userWindowId"], event["videoResizeMode"], cloudICP.config["HDVideoMode"]
						);
				} else {
					param =
						"{\"description\":\"msp_create_video_channel\", \"cmd\": 65539,  \"param\":{\"resID\":{0}, \"winHandle\":{9}, \"mediaInfo\":{\"localIP\":\"{1}\", \"localPort\":{2}, \"remoteIP\":\"{3}\",\"remotePort\":{4}, \"playload\":{5}, \"ssrc\":{6}, \"fmt\":\"{7}\", \"recvOnly\":{8}, \"user\":\"{10}\", \"peerUser\":\"{11}\", \"mode\":\"{12}\", \"port\":{13}, \"fmtType\":\"{14}\", \"width\": {15}, \"height\": {16}, \"videoConfLocalWindow\": {17}, \"videoResizeMode\": {18}, \"HDVideoMode\": {19}  }}}"
						.format(
							parseInt(value["cid"]), cloudICP.config.localIP, localArr[1], serverArr[0], serverArr[
								1], parseInt(value["video"]), parseInt(value["video_ssrc"]), value["fmt"], recvOnly,
							parseInt(event["windowHandle"]), user, peerUser, event["mode"], iPort, strFmtType,
							iWidth, iHeight, videoConfLocalWindow, event["videoResizeMode"], cloudICP.config["HDVideoMode"]
						);
				}
			}

			cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[event["resID"].toString()]["isCreateWindowComplete"] = true;
			break;
		case 196618:
			if (undefined != event["userWindowId"]) {
				var meidaSDKparam =
					"{\"description\":\"msp_stop_use_userWindow\", \"cmd\": 196618, \"param\":{\"userWindowId\":{0}}}"
					.format(
						parseInt(event["userWindowId"])
					);
				cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
				if ("-1" == event["userWindowId"].toString()) { //清除全部媒体面时触发
					Object.keys(cloudICP.util.callStatusMgr.windowInfos).forEach(function(key) {
						if (cloudICP.util.callStatusMgr.windowInfos[key]["userWindowId"] != undefined) {
							delete cloudICP.util.callStatusMgr.windowInfos[key];
						}
					});
				}
			}
			break;
		case 131077:
			var retEvent = {
				"eventName": "OnGetSoundDeviceResult",
				"rsp": "0",
				"value": {
					"speaker": event["output"],
					"mic": event["input"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnGetSoundDeviceResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnGetSoundDeviceResult"]);
			break;
		case 131082:
			var retEvent = {
				"eventName": "OnSetRingVolumeResult",
				"rsp": "0",
				"value": {
					"volume": event["newVolume"],
				}
			}

			this.EVENT_LIST["MSPNotify"]["OnSetRingVolumeResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnSetRingVolumeResult"]);
			break;
		case 65556:
			var retEvent = {
				"eventName": "OnVideoStreamSPSEvent",
				"value": {
					"width": event["width"].toString(),
					"height": event["height"].toString(),
					"video": event["video"].toString()
					}
			}
			this.EVENT_LIST["MSPNotify"]["OnVideoStreamSPSEvent"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnVideoStreamSPSEvent"]);
			break;
		case 65555:
				var retEvent = {
					"eventName": "PSTNBroadcastOnceSendOverNotify",
					"value": {
						"cid": event["cid"].toString()
					}
				}
				this.EVENT_LIST["VoiceNotify"]["PSTNBroadcastOnceSendOverNotify"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["PSTNBroadcastOnceSendOverNotify"]);
				break;
		case 65554:
			var retEvent = {
				"eventName": "stop_LocalRecord",
				"rsp": event["result"],
				"value": {
					"cid": event["cid"].toString()
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnStopLocalRecordEvent"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnStopLocalRecordEvent"]);

			break;
		case 65553:
			var retEvent = {
				"eventName": "start_LocalRecord",
				"rsp": event["result"],
				"value": {
					"cid": event["cid"].toString()
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnStartLocalRecordEvent"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnStartLocalRecordEvent"]);

			break;
		case 65544:
			var retEvent = {
				"eventName": "OnGetVolumeResult",
				"rsp": event["result"] == 0 ? "0" : "-1",
				"value": {
					"VolumeDesc": event["AudioVolume"],
					"cid": event["resID"].toString()
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnGetVolumeResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnGetVolumeResult"]);

			break;
		case 65545:
			var retEvent = {
				"eventName": "OnSetVolumeResult",
				"rsp": event["result"] == 0 ? "0" : "-1",
				"value": {
					"cid": event["resID"].toString(),
					"Volume": event["newVolume"].toString(),
				}
			}
			if(event["result"] == 0){
                cloudICP.grpVolumeList[event["peerID"]] = event["newVolume"].toString();
            }
			this.EVENT_LIST["MSPNotify"]["OnSetVolumeResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnSetVolumeResult"]);
			break;
		case 65543:
			if (event["quiet"] && event["quiet"] == true) {
				var retEvent = {
					"eventName": "OnMuteSpeakerResult",
					"rsp": event["result"] == 0 ? "0" : "-1",
					"value": {
						"cid": event["resID"].toString()
					}
				}
				this.EVENT_LIST["MSPNotify"]["OnMuteSpeakerResult"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnMuteSpeakerResult"]);
			} else {
				var retEvent = {
					"eventName": "OnUnmuteSpeakerResult",
					"rsp": event["result"] == 0 ? "0" : "-1",
					"value": {
						"cid": event["resID"].toString()
					}
				}
				this.EVENT_LIST["MSPNotify"]["OnUnmuteSpeakerResult"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnUnmuteSpeakerResult"]);
			}

			break;
		case 65546:
			var retEvent = {
				"eventName": "OnAssignSoundDeviceResult",
				"rsp": event["result"] == 0 ? "0" : "-1",
				"value": {
					"outputDev": event["mediaInfo"]["outputDev"],
					"inputDev": event["mediaInfo"]["inputDev"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnAssignSoundDeviceResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnAssignSoundDeviceResult"]);
			break;
		case 65547:
			var retEvent = {
				"eventName": "OnQuerySoundDeviceResult",
				"rsp": event["result"] == 0 ? "0" : "-1",
				"value": {
					"devices": event["assignList"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnQuerySoundDeviceResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnQuerySoundDeviceResult"]);
			break;
		case 65542:
			if (event["mute"] != undefined && event["mute"] == false) {
				var retEvent = {
					"eventName": "OnUnmuteMicResult",
					"rsp": event["result"] == 0 ? "0" : "-1",
					"value": {
						"cid": event["resID"].toString()
					}
				}
				this.EVENT_LIST["MSPNotify"]["OnUnmuteMicResult"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnUnmuteMicResult"]);
			} else {
				var retEvent = {
					"eventName": "OnMuteMicResult",
					"rsp": event["result"] == 0 ? "0" : "-1",
					"value": {
						"cid": event["resID"].toString()
					}
				}
				this.EVENT_LIST["MSPNotify"]["OnMuteMicResult"].event = retEvent;
				sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnMuteMicResult"]);
			}

			break;

		case 65548:
			cloudICP.dispatch.device.isDTMFDone = true;
			var retEvent = {
				"eventName": "OnSendToDTMFResult",
				"rsp": event["result"] == 0 ? "0" : "-1",
				"value": {
					"cid": event["resID"].toString(),
					"function": event["functionCode"].toString()
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnSendToDTMFResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnSendToDTMFResult"]);

			break;
		case 16384: //0x4000
			if (event["resID"] == undefined) return;

			//不检查组呼
			//其它
			var callStatusMgr = cloudICP.util.callStatusMgr;
			var call = callStatusMgr.allCalls[event["resID"]];
			if (call) {
				if (call.callType == callStatusMgr.VOICE ||
					call.callType == callStatusMgr.VIDEO ||
					call.callType == callStatusMgr.HALFVOICE ||
					call.callType == callStatusMgr.DISCREET ||
					call.callType == callStatusMgr.MONITOR ||
					call.callType == callStatusMgr.AMBIENCE) {

					var retEvent = {
						"eventName": "OnCallException",
						"rsp": "20000",
						"value": {
							"callee": call["callee"],
							"caller": call["caller"],
							"cid": event["resID"].toString(),
							"calltype": callStatusMgr.callTypeStr[call.callType]
						}
					}
					this.EVENT_LIST["VoiceNotify"]["OnCallException"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["VoiceNotify"]["OnCallException"]);

				}


				if (call.callType == callStatusMgr.VOICECONF ||
					call.callType == callStatusMgr.VIDEOCONF) {

					var retEvent = {
						"eventName": "OnConfException",
						"rsp": "20000",
						"value": {
							"cid": event["resID"].toString(),
							"confId": call["confId"]
						}
					}
					this.EVENT_LIST["PhoneConfNotify"]["OnConfException"].event = retEvent;
					sendEvent2Page(reqId, this.EVENT_LIST["PhoneConfNotify"]["OnConfException"]);

				}
			}
			break;
		case 20480: //收到点击窗口关闭按键事件
			var mediaParam = cloudICP.util.callStatusMgr.mediaModeMap.get(parseInt(event["cid"]))
			if (undefined != mediaParam) {
				var meidaSDKparam =
				"{\"description\":\"setMediaMode\", \"cmd\": 65561, \"param\":{\"cid\":{0}, \"type\":{1}, \"switch\":0}}".format(
					parseInt(event["cid"]), parseInt(mediaParam["type"])
				);
				cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);
			}

			var resID;
			Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function(key) {
				if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"][
						"repeatsMonitor"
					] &&
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[key].hwnd == event["windowHandle"]) {
					resID = key;
				}
			});
			if (undefined != resID) { //如果释放的是重复视频监控的业务 不支持用户窗口
				if (cloudICP.util.callStatusMgr.videoMediaInfoBuf[resID]["value"]["repeatsMonitor"] == true) {
					// 删除视频媒体通道
					var cid = cloudICP.util.callStatusMgr.videoMediaInfoBuf[resID]["value"]["cid"];
					var createReleaseTag = false;
					var createId;
					// 20480是JSSDK登陆时MSP返回的事件，因为是MSP主动推送的，reqId为undefined，此处设置为0
					if (reqId == undefined || typeof reqId == 'undefined') {
						reqId = 0;
					}
					param =
						"{\"description\":\"msp_delete_video_channel\", \"cmd\": 65540, \"param\":{\"resID\":{0}}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(resID), reqId
						);
					cloudICP.dispatch.webSocket.sendDataToMediaSDK(param, true);
                    //通知上层OnCallRelease消息
                    var newEvent = {
                        "eventName": "",
                        "rsp": "3009",
                        "value": {}
                    }
                    newEvent["value"]["caller"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["caller"];
                    newEvent["value"]["callee"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["callee"];
                    newEvent["value"]["cid"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["cid"];
                    newEvent["value"]["fmt"] = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["fmt"];
                    newEvent["value"]["calltype"] = "monitor";
                    newEvent["value"]["releasetype"] = "other";
                    newEvent["value"]["src"] = "";
                    newEvent["value"]["direction"] = "out";
                    newEvent["eventName"] = "OnCallRelease";
                    cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnCallRelease"].event = newEvent;
					sendEvent2Page(reqId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnCallRelease"]);
					cloudICP.util.callStatusMgr.delMediaModeInfo(newEvent["value"]);
					if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["createId"]) {
						createId = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid]["value"]["createId"];
					}
					createReleaseTag = cloudICP.util.getCopyMonitorReleaseTag(newEvent["value"]["caller"],
						newEvent["value"]["callee"]);
					var videoMediaWorkingInfo = cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid];
					if (undefined != videoMediaWorkingInfo) { // 该视频窗口有业务正在使用
						delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[cid];
					}

					if (createReleaseTag && undefined != createId) {
						cloudICP.util.log(undefined, { "logLevel": "info", "logMsg": "createId : " + createId + " createReleaseTag: " + createReleaseTag });
						var requestParam = {
							"cid": String(createId),
							"callback": function(data) {;}
						};

						cloudICP.dispatch.video.release(requestParam);
					}
				}
				return;
			}
			Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function(key) {
				if (cloudICP.util.callStatusMgr.videoMediaInfoBuf[key].hwnd == event["windowHandle"]) {
					resID = key;
				}
			});
			if (resID == undefined) {
				if (undefined != event["userWindowId"] && undefined != event["isNeedDestroyWindow"] && true ==
					event["isNeedDestroyWindow"]) {
					// 20480是JSSDK登陆时MSP返回的事件，因为是MSP主动推送的，reqId为undefined，此处设置为0
					if (reqId == undefined || typeof 'undefined') {
						reqId = 0;
					}
					var meidaSDKparam =
						"{\"description\":\"msp_delete_close_userWindow\", \"cmd\": 196613, \"param\":{\"userWindowId\":{0}}, \"reqId\": \"{1}\"}"
						.format(
							parseInt(event["userWindowId"]), reqId
						);
					cloudICP.dispatch.webSocket.sendDataToMediaSDK(meidaSDKparam, true);

					if ("-1" == event["userWindowId"].toString()) { //去除用户窗被业务使用，但未创建媒体面的标记
						Object.keys(cloudICP.util.callStatusMgr.windowInfos).forEach(function(key) {
							if (cloudICP.util.callStatusMgr.windowInfos[key]["userWindowId"] != undefined) {
								delete cloudICP.util.callStatusMgr.windowInfos[key];
							}
						});
					}
				}
				return;
			}
			var callStatusMgr = cloudICP.util.callStatusMgr;
			var call = callStatusMgr.allCalls[resID];
			if (call) {
				var param = {
					cid: resID,
                    tabPageId: reqId,
					callback: function(data) {
						setTimeout(function() {
							cloudICP.util.sendLogOut({"logLevelNum" : 1, "logMsg" : "close [" + resID + "] window rsp: " + JSON.stringify(data)});
						}, 0);
					}
				}
				if (call.callType == callStatusMgr.VIDEO || call.callType == callStatusMgr.MONITOR || call
					.callType == callStatusMgr.VIDEODISPATCH) {
					cloudICP.dispatch.video.release(param);
					return;
				}
				if (call.callType == callStatusMgr.VIDEOCONF) {
					var confInfo = cloudICP.util.callStatusMgr.confMgr[cloudICP.util.callStatusMgr
						.currentGoingConf];
					if (confInfo != undefined) {
						if (cloudICP.userInfo["isdn"] == confInfo["value"]["confStatus"]["chair"]) {
							param.confId = cloudICP.util.callStatusMgr.currentGoingConf;
							cloudICP.dispatch.conf.endConf(param);
							return;
						}
					}
					cloudICP.dispatch.conf.exitVideoConf(param);
				}
			}
			break;
		case 20481:
			delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[event["userWindowId"].toString()];
			var retEvent = {
				"eventName": "OnDestroyWindowResult",
				"rsp": event["result"].toString(),
				"value": {
					"windowHandle": event["windowHandle"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnDestroyWindowResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnDestroyWindowResult"]);
			break;
		case 196614:
			var retEvent = {
				"eventName": "OnWindowOnTopResult",
				"rsp": event["result"],
				"value": {
					"cid": event["cid"],
					"isOnTop": event["isOnTop"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnWindowOnTopResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnWindowOnTopResult"]);
			break;
		case 131084:
			var retEvent = {
				"eventName": "OnUpgradeMspVersionResult",
				"rsp": event["result"],
				"value": event["deamonResultParams"]
			}
			this.EVENT_LIST["MSPNotify"]["OnUpgradeMspVersionResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnUpgradeMspVersionResult"]);
			break;
		case 196615:
			var retEvent = {
				"eventName": "OnWindowDragEnableResult",
				"rsp": event["result"],
				"value": {
					"cid": event["cid"],
					"isDrag": event["isDrag"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnWindowDragEnableResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnWindowDragEnableResult"]);
			break;
		case 196616:
			var retEvent = {
				"eventName": "OnHideVideoWindowResult",
				"rsp": event["result"],
				"value": {
					"cid": event["cid"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnHideVideoWindowResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnHideVideoWindowResult"]);
			break;
		case 196617:
			var retEvent = {
				"eventName": "OnWindowShowBorderResult",
				"rsp": event["result"],
				"value": {
					"resID": event["resID"],
					"border": event["border"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnWindowShowBorderResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnWindowShowBorderResult"]);
			break;
		case 258049:
			var retEvent = {
				"eventName": "OnWindowOperateNotify",
				"rsp": event["result"],
				"value": {
					"cid": event["param"]["cid"],
					"eventID": event["param"]["eventID"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["OnWindowOperateNotify"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnWindowOperateNotify"]);
			break;
		case 258050:
			var retEvent = {
				"eventName": "OnDownloadProgressEvent",
				"rsp": event["result"],
				"value": event["param"]
			}
			this.EVENT_LIST["MSPNotify"]["OnDownloadProgressEvent"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnDownloadProgressEvent"]);
			break;
		case 258051:
			var retEvent = {
				"eventName": "OnDownloadProgressEvent",
				"rsp": event["result"],
				"value": event["param"]
			}
			this.EVENT_LIST["MSPNotify"]["OnDownloadProgressEvent"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnDownloadProgressEvent"]);
			var retEvent = {
				"eventName": "OnInstallMspEvent",
				"status": "begin"
			}
			cloudICP.config["installMspStatus"] = "begin";
			this.EVENT_LIST["MSPNotify"]["OnInstallMspEvent"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnInstallMspEvent"]);
			break;
		case 65559:
			var infostr;
			if (event["result"] == 0)
				infostr = "Success";
			else if (event["result"] == 6)
				infostr = "Can not find windowName";
			else if (event["result"] == 3)
				infostr = "Can't find cid";
			else if (event["result"] == 1)
				infostr = "Failed,local camera not detected";
			else if (event["result"] == 4)
				infostr = "Failed,windowName is not visible";
            var retEvent = {
                "eventName": "OnStartWindowShare",
				"rsp": event["result"].toString(),
                "value": {
                    "cid": event["cid"].toString(),
					"info": infostr
                }
            }
            this.EVENT_LIST["MSPNotify"]["OnStartWindowShare"](retEvent);
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnStartWindowShare"]);
            break;
		case 65561:
			if (event["result"] == 0) {
				if (event["switch"] == 1) {
					cloudICP.util.callStatusMgr.addMediaModeInfo(event);
				} else {
					cloudICP.util.callStatusMgr.delMediaModeInfo(event);
				}
			}
			var retEvent = {
				"eventName": "OnSetMediaModeResult",
				"rsp": event["result"] == 0 ? "0" : "-1"
			}
			this.EVENT_LIST["MSPNotify"]["OnSetMediaModeResult"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["OnSetMediaModeResult"]);
			break;
		case 655611:
			var retEvent = {
				"eventName": "OnMediaNotifyEvent",
				"rsp": event["result"] == 0 ? "0" : "-1",
				"value": {
					"cid": event["cid"],
					"format": event["format"],
					"data": event["data"]
				}
			}
			this.EVENT_LIST["MSPNotify"]["onMediaNotify"].event = retEvent;
			sendEvent2Page(reqId, this.EVENT_LIST["MSPNotify"]["onMediaNotify"]);
			break;
		default:
			break;
	}
};

/**
 * @param {
 * eventType:
 * eventName:
 * callback:
 * }
 */
ICPSDK_Dispatch_Event.prototype.register = function(param) {
	cloudICP.util.LogInputParam(param);
	var tabPageId = param.tabPageId;

	if (param["value"] != undefined) {
		param = param.value;
	}

	var eventType = param["eventType"];
	var eventName = param["eventName"];
	var eventFuncId = param.funcId;

	if (!eventType) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "register: register failed. Because eventType is invalid."
		});
		return;
	}
	if (!eventName) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "register: register failed. Because eventName is invalid."
		});
		return;
	}

	var eventTypeObject = this.EVENT_LIST[eventType];
	if (undefined == eventTypeObject) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "register: register failed. Because eventType is invalid."
		});
		return;
	}
	var eventObject = eventTypeObject[eventName];
	if (undefined == eventObject) {
		cloudICP.util.log(tabPageId, {
			"logLevel": "error",
			"logMsg": "register: register failed. Because eventName is invalid"
		});
		return;
	}

	this.EVENT_LIST[eventType][eventName] = {
		funcId: eventFuncId,
		event : null,
	}

}

ICPSDK_Dispatch_Event.prototype.printEvent = function(data) {
	try {
		var newJson = JSON.parse(JSON.stringify(data));
		newJson.logType = "ICPSDK event";
		cloudICP.util.sendLogOut({"logLevelNum" : 1, "logMsg" : JSON.stringify(newJson)});
	} catch (e) {
		//alert(e); // error in the above string (in this case, yes)!
		cloudICP.util.sendLogOut({"logLevelNum" : 1, "logMsg" : "ICPSDK event: " + JSON.stringify(newJson)});
	}
}

/**
 * @param cfg {
 *  "type" : "POST|PUT|DELETE|GET"
 *  "url" :
 *  "async" :
 *  "timeout" :
 *  "contentType" :
 *  "data" : //post和put请求时设置
 *  "callback" :
 * }
 */

 ICPSDK_Util.prototype.ajax = function (cfg) {
    function alter(data) {
        var arr = []
        for (var k in data) {
            var str = k + '=' + data[k]
            arr.push(str)
        }
        return arr.join('&')
    }

    function ajaxSendData(option) {
        var xhr = new XMLHttpRequest()
        var qs = "{}"
        cloudICP.util.log(0, {
            "logMsg": "ajax 1"
        });
        if (undefined != option.data && null != option.data) {
            qs = JSON.stringify(option.data); //alter(option.data)
        }
        var session = cloudICP.userInfo["session"];

        var methodName = null;
        var moduleName = null;
		var user = "";
        if (cfg.data) {
            // 打印接口发送日志
            var tmpStr;
			user = cfg.data["loginUser"];
            try {
                throw new Error();
            } catch (n) {
                tmpStr = n.stack.split("\n");
            }
            for (var index = 0; index < tmpStr.length; index++) {
                if (tmpStr[index].indexOf("ICPSDK_Dispatch") != -1) {
                    methodName = tmpStr[index].split(/\W/)[6];
                    moduleName = tmpStr[index].split(/\W/)[5];
                    if (moduleName != "ICPSDK_Dispatch_Event") {
						cloudICP.util.sendLogOut({"logLevelNum" : 1, "logMsg" : "ICPSDK request: " + "[interface: " + methodName + ", param: " +
						qs + " ]"});
                    }
                    break;
                }
            }
        }
        cloudICP.util.log(0, {
            "logMsg": "ajax 2" + option.url
		});

		var strMethod = option.method.toUpperCase();
		if (strMethod === 'GET' || strMethod === 'POST' || strMethod === 'PUT' || strMethod === 'DELETE') {
            xhr.open(option.method, option.url)
			if(user){
				xhr.setRequestHeader("WS-CLIENT-ID", user);
			} else {
				xhr.setRequestHeader("WS-CLIENT-ID", cloudICP.userInfo["loginUserName"]);
			}
		}
        if (option.method.toUpperCase() === 'GET') {
            if (session) {
                xhr.setRequestHeader("session", session);
            }
            xhr.send()
        } else if (option.method.toUpperCase() === 'POST') {
            if (session) {
                xhr.setRequestHeader("session", session);
            }
            xhr.setRequestHeader("Content-Type", "application/json")
            xhr.send(qs)
        } else if (option.method.toUpperCase() === 'PUT') {
            if (session) {
                xhr.setRequestHeader("session", session);
            }
            xhr.setRequestHeader("Content-Type", "application/json")
            xhr.send(qs)
        } else if (option.method.toUpperCase() === 'DELETE') {
            if (session) {
                xhr.setRequestHeader("session", session);
            }
            xhr.setRequestHeader("Content-Type", "application/json")
            xhr.send(qs)
        }
        cloudICP.util.log(0, {
            "logMsg": "ajax 3"
        });

        xhr.onreadystatechange = function () {
            if (xhr.readyState === 4) {
                var data = xhr.responseText
                if (xhr.status === 200) {
                    var jsonObject = JSON.parse(data);
                    if (undefined != jsonObject["cmd"]) {
                        delete jsonObject.cmd;
                    }
                    if (undefined == jsonObject["desc"]) {
                        jsonObject["desc"] = "";
                    }

                    if (undefined != jsonObject["list"] && jsonObject["list"] != null) {
                        for (var index = 0; index < jsonObject["list"].length; index++) {
                            if (undefined != jsonObject["list"][index]["vpnid"]) {
                                delete jsonObject["list"][index]["vpnid"];
                            }

                            if (undefined != jsonObject["list"][index]["vpnin"]) {
                                delete jsonObject["list"][index]["vpnin"];
                            }


                            if (undefined != jsonObject["list"][index]["vpnout"]) {
                                delete jsonObject["list"][index]["vpnout"];
                            }
                        }
                    }

                    if (undefined != jsonObject["value"] && jsonObject["value"] != null) {
                        if (undefined != jsonObject["value"]["vpnid"]) {
                            delete jsonObject["value"]["vpnid"];
                        }

                        if (undefined != jsonObject["value"]["vpnin"]) {
                            delete jsonObject["value"]["vpnin"];
                        }

                        if (undefined != jsonObject["value"]["vpnout"]) {
                            delete jsonObject["value"]["vpnout"];
                        }
                    }

                    if (moduleName != null && moduleName != "ICPSDK_Dispatch_Event") {
						cloudICP.util.sendLogOut({"logLevelNum" : 1, "logMsg" : "ICPSDK request: " + "[interface: " + methodName +
						", response: " + JSON.stringify(jsonObject) + " ]"});
                    }
                    cfg.callback(jsonObject);
                } else {
                    var result = {
                        "rsp": "-1",
                        "desc": "unkown exception when send request. See the brower's console."
                    };
                    cfg.callback(result);
                    cloudICP.util.log(0, {
                        "logLevel": "error",
                        "logMsg": "unkown exception when send request. See the brower's console." + JSON
                            .stringify(data)
                    });
                }
            }
        }
    }
    cfg.method = cfg.type;
    ajaxSendData(cfg);
}

ICPSDK_Util.prototype.closeWssflowWindow = function (param) {

	if (undefined == param) {
		return;
	}

	var param = param["value"];

	if (undefined == param["param1"] || undefined == param["param2"]) {
		return;
	}

	param["param1"]["callback"] = param["callback"];
	param["param2"]["callback"] = param["callback"];

	var param1 = param["param1"];
	var param2 = param["param2"];

	if (param2["confId"] == "cloudICP.util.callStatusMgr.currentGoingConf") {
		param2["confId"] = cloudICP.util.callStatusMgr.currentGoingConf;
	}

	if (param2["cid"] == "cloudICP.util.callStatusMgr.currentGoingConfCid") {
		param2["cid"] = cloudICP.util.callStatusMgr.currentGoingConfCid;
	}

	var _cloudICP$dispatch, _cloudICP$dispatch$vi, _cloudICP$dispatch$vi2;
	var _cloudICP$dispatch$conf;

	// 挂断的是会议呼叫时，结束会议或退出会议
	if (param1["cid"] != undefined && param1["cid"] == param2["cid"])
	{
		if (undefined != cloudICP.util.callStatusMgr.windowInfos[2147483647]) {
			(_cloudICP$dispatch = cloudICP.dispatch) === null || _cloudICP$dispatch === void 0 ? void 0 : (_cloudICP$dispatch$conf = cloudICP.dispatch.conf) === null || _cloudICP$dispatch$conf === void 0 ? void 0 : _cloudICP$dispatch$conf.endConf(param2);
		} else {
			(_cloudICP$dispatch = cloudICP.dispatch) === null || _cloudICP$dispatch === void 0 ? void 0 : (_cloudICP$dispatch$conf = cloudICP.dispatch.conf) === null || _cloudICP$dispatch$conf === void 0 ? void 0 : _cloudICP$dispatch$conf.exitVideoConf(param2);
		};
	}
	else
	{
		(_cloudICP$dispatch = cloudICP.dispatch) === null || _cloudICP$dispatch === void 0 ? void 0 : (_cloudICP$dispatch$vi = _cloudICP$dispatch.video) === null || _cloudICP$dispatch$vi === void 0 ? void 0 : (_cloudICP$dispatch$vi2 = _cloudICP$dispatch$vi.release) === null || _cloudICP$dispatch$vi2 === void 0 ? void 0 : _cloudICP$dispatch$vi2.call(_cloudICP$dispatch$vi, param1);
	}
}

ICPSDK_Dispatch_WebSocket.prototype.getModifyMdcip = function(data) {
	cloudICP.util.LogInputParam(data);
	if (0 == data["mdc_result"]) {
		cloudICP.config["mdcip"] = data["mdcip"];
		var event = {
			"status": "0",
			"desc": "modify mdcip success !",
			"mdcip": data["mdcip"]
		}
		sendMsg2Page(0, messageType.event, event);
	} else {
		var event = {
			"status": "3",
			"desc": "modify mdcip failure !"
		}
		sendMsg2Page(0, messageType.event, event);
	}
}// 消息类型
var messageType = {
	lifecycle: "lifecycle",
	log: "log",
	request: "request",
	response: "response",
	event: "event",
}

var mainPageId = undefined;

function ICPSDK_Util() {
	this.callStatusMgr = new CALL_STATUS_MGR();
	this.sensitiveInfo = new SENSITIVE_INFOS();
};

ICPSDK_Util.prototype.strJsonParse = function(strJson) {
	try {
		var a = JSON.parse(strJson, function(k, v) {
			if (null != v && v.indexOf && v.indexOf('function') > -1) {
				return eval("(function(){return " + v + " })()")
			} else if (null != v && v.indexOf && v.indexOf('=>') > -1) { // 支持箭头函数的转换
				return eval("(function(){return " + v + " })()");
            }
			return v;
		});
		return a;
	} catch (e) {
        cloudICP.util.log(0, { "logLevel": "error", "logMsg": "strJsonParse error: " + strJson });
		return null;
	}
}

ICPSDK_Util.prototype.copyJsonObject = function(jsonObject) {
	try {
		var strJson = JSON.stringify(jsonObject, function(key, val) {
			if (typeof val === 'function') {
				return val + '';
			}
			return val;
		});
		var a = JSON.parse(strJson, function(k, v) {
			if (null != v && v.indexOf && v.indexOf('function') > -1) {
				return eval("(function(){return " + v + " })()")
			} else if (null != v && v.indexOf && v.indexOf('=>') > -1) { // 支持箭头函数的转换
				return eval("(function(){return " + v + " })()");
            }
			return v;
		});
		return a;
	} catch (e) {
		cloudICP.util.log(0, { "logLevel": "error", "logMsg": "copyJsonObject json failure !" });
		return null;
	}
}

/**
 * logParam is json object
 * {
 *   "logLevel" : debug|info|warn|error
 *   "logMsg" : message
 * }
 */
ICPSDK_Util.prototype.log = function(tabPageId, logParam) {
	var date = cloudICP.util.getCurrentDate(); //new Date().format("yyyy-MM-dd hh:mm:ss");

	logParam.date = date;

	// 多窗口版本WorkerJs需要将日志发送给Demo页签
	if (typeof tabPageId != 'undefined' || tabPageId != undefined) {
		sendMsg2Page(tabPageId, messageType.log, JSON.stringify(logParam));
	}
	if (logParam["logLevel"] == "debug") {
		logParam["logLevelNum"] = 0;
	} else if (logParam["logLevel"] == "info") {
		logParam["logLevelNum"] = 1;
	} else if (logParam["logLevel"] == "warn") {
		logParam["logLevelNum"] = 2;
	} else if (logParam["logLevel"] == "error") {
		logParam["logLevelNum"] = 3;
	} else {
		logParam["logLevelNum"] = 0;
	}
	if (cloudICP.config.debugMode == "true") {
		// F12控制台 日志打印
		switch (logParam["logLevelNum"]) {
			case 0:
				console.debug("[DEBUG][" + date + "] " + logParam["logMsg"]);
				break;
			case 1:
				console.info("[INFO][" + date + "] " + logParam["logMsg"]);
				break;
			case 2:
				console.warn("[WARN][" + date + "] " + logParam["logMsg"]);
				break;
			case 3:
				console.error("[ERROR][" + date + "] " + logParam["logMsg"]);
				break;
			default:
				console.debug("[DEFAULT][" + date + "] " + logParam["logMsg"]);
				break;
		}
	}
	logParam.logType = "HuaweiICPSDK's Log";
	cloudICP.util.sendLogOut(logParam);
};

/**
 * check str is or not json object strings
 * @param str
 */
ICPSDK_Util.prototype.isJsonString = function(str) {
    try {
      if (typeof JSON.parse(str) == "object") {
        return true;
      }
    } catch(e) {}
    return false;
}

/**
 * check the string param is number
 * @param param
 */
ICPSDK_Util.prototype.isNumber = function(param) {
    if (undefined == param || "" == param.toString()) {
        return false;
    }
    var regex = /^[1-9]+[0-9]*$/;
    if (regex.test(param) || /^0$/.test(param)) {
        return true;
    }
    return false;
}

/**
 * check the windowinfo param is valid
 * @param param
 */
ICPSDK_Util.prototype.checkWindowParam = function(param) {

	if (false == cloudICP.util.isNumber(param)) {
		return false;
	}
	var iValue = parseInt(param);
	if (0 > iValue || 0x7FFFFFFF < iValue) {
		return false;
	}
	return true;
}

/**
 * check the struiing param is letter
 * @param param
 */
ICPSDK_Util.prototype.isLetter = function(param) {

	if (undefined == param) {
		return false;
	}
	var regex = /^[A-Za-z]+[A-Za-z]*$/
	if (regex.test(param)) {
		return true;
	}
	return false;
}

/**
 * check the string is one of str in array
 * @param array
 */
ICPSDK_Util.prototype.isStrInArray = function(str, array) {
	for (var type = 0; type < array.length; type++) {
		if (array[type] === str) {
			return true;
		}
	}
	return false;
}

/**
 * check the string is legal: number or letter
 * @param param
 */
ICPSDK_Util.prototype.isStrLegal = function(param) {

	if (undefined == param) {
		return false;
	}
	var regex = /^[A-Za-z0-9]+[A-Za-z0-9]*$/
	if (regex.test(param)) {
		return true;
	}
	return false;
}

/**
 * check the string param is number(int or float)
 * @param param
 */
ICPSDK_Util.prototype.checkRate = function(param) {

	if (undefined == param) {
		return false;
	}

	if (isNaN(parseInt(param)) || isNaN(parseFloat(param))) {
		return false;
	}

	return true;
}


/**
 * check the string param is bool
 * @param param
 */
ICPSDK_Util.prototype.isBool = function(param) {

	if (undefined == param) {
		return false;
	}

	if (param == "true" || param == "false") {
		return true;
	}
	return false;
}

/**
 * check isdn is valid
 * @param isdn
 */
ICPSDK_Util.prototype.checkIsdn = function(isdn) {
	if (isdn && isdn.length <= 19) {
		return true;
	}
	return false;
}

/**
 * check file type is right
 * @param {*} isdn
 */
 ICPSDK_Util.prototype.checkFileType=function(filePath, type){
    if(!filePath){
        return false;
    }
    var suffer = filePath.substring(filePath.lastIndexOf(".")+1);
    if(!suffer || suffer != type){
        return false;
    }
    return true;
}


ICPSDK_Util.prototype.checkCallNumber = function(isdn) {
	if (isdn && isdn.length <= 13 && this.isNumber(isdn)) {
		return true;
	}
	return false;
}

ICPSDK_Util.prototype.checkGroupID = function(grpid) {
	if (grpid && grpid.length <= 13 && this.isNumber(grpid)) {
		return true;
	}
	return false;
}

/**
 * check cid is valid
 * @param cid
 */
ICPSDK_Util.prototype.checkCid = function(cid) {
	if (cid && cid.length <= 32 && this.isNumber(cid) && (parseInt(cid, 10) <= 2147483647)) {
		return true;
	}
	return false;
}

/**
 * getUserWindowId
 * @param NULL
 */
ICPSDK_Util.prototype.getUserWindowId = function() {
	var userWindowId = 2000000001;
	var bIsContinue = false;
	for (; userWindowId <= 2000001000; userWindowId++) {
		if (undefined == cloudICP.util.callStatusMgr.videoMediaInfoBuf[userWindowId.toString()]) {
			Object.keys(cloudICP.util.callStatusMgr.windowInfos).forEach(function(key) {
				if (undefined != cloudICP.util.callStatusMgr.windowInfos[key]["userWindowId"] && cloudICP
					.util.callStatusMgr.windowInfos[key]["userWindowId"] == userWindowId) {
					bIsContinue = true;
					cloudICP.util.log(0, {
						"logLevel": "warn",
						"logMsg": "[getUserWindowId] userWindowId(" + userWindowId + ") is release, but server not return over event."
					});
				}
			});
			if (false == bIsContinue) {
				return userWindowId;
			}
			bIsContinue = false;
		}
	}
	return userWindowId;
}

/**
 * get monitor repeats cid
 * @param NULL
 */
ICPSDK_Util.prototype.getMonitorRepeatsCid = function() {
	var monitorRepeatsCid = 2000001001;
	for (; monitorRepeatsCid <= 2000002000; monitorRepeatsCid++) {
		var bIsContinue = true;
		Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function(key) {
			if (key == monitorRepeatsCid) {
				bIsContinue = false;
				return;
			}
		});
		if (bIsContinue == true) {
			break;
		}
	}
	return monitorRepeatsCid;
}

/**
 * check userWindowId is valid
 * @param cid
 */
ICPSDK_Util.prototype.checkUserWindowId = function(userWindowId, type) {
	var iResult = 0;
	if (userWindowId && this.isNumber(userWindowId) && (parseInt(userWindowId, 10) <= 2000001000) && (parseInt(
			userWindowId, 10) >= 2000000001)) {
		iResult = 1;
		var bUserWindowIdIsUsed = false;
		Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function(key) {
			if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["windowId"] &&
				cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["windowId"] == userWindowId) {
				bUserWindowIdIsUsed = true;
			}
		});
		Object.keys(cloudICP.util.callStatusMgr.windowInfos).forEach(function(key) {
			if (undefined != cloudICP.util.callStatusMgr.windowInfos[key]["userWindowId"] && cloudICP.util
				.callStatusMgr.windowInfos[key]["userWindowId"] == userWindowId) {
				iResult = 2;
			}
		});
		if ("create" == type && true == bUserWindowIdIsUsed) {
			iResult = 2;
		} else if ("use" == type && false == bUserWindowIdIsUsed) {
			iResult = 2;
		} else if ("destroy" == type) {
			return iResult;
		}
	}
	return iResult;
}

/**
 * check User is Monitor
 * @param caller callee
 */
ICPSDK_Util.prototype.checkUserIsMonitor = function (caller, callee, jsonMediaValue) {
	var iResult = false;
	Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function (key) {
		if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["caller"] && caller ==
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["caller"] &&
			undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["callee"] && callee ==
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["callee"]) {
			if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"][
				"repeatsMonitor"
			]) {
				jsonMediaValue["value"] = JSON.parse(JSON.stringify(cloudICP.util.callStatusMgr
					.videoMediaInfoBuf[key]["value"]));
				iResult = false;
				return;
			}
			iResult = true;
			jsonMediaValue["value"] = JSON.parse(JSON.stringify(cloudICP.util.callStatusMgr
				.videoMediaInfoBuf[key]["value"])); // 复制对象
		}
	});
	if ("" != jsonMediaValue["value"]) {
		return iResult;
	}
	Object.keys(cloudICP.util.callStatusMgr.windowInfos).forEach(function (key) {
		if (key == callee) {
			iResult = true;
		}
	});
	return iResult;
}

/**
 * 是否创建复制流
 * @param caller callee
 */
ICPSDK_Util.prototype.createNewCopyMonitor = function (caller, callee, jsonMediaValue) {
	var iResult = false;
	var activeStream = 0; // 判断创建时候的标记数，当前规格最大支持三路
	Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function (key) {
		if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["caller"] && caller ==
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["caller"] &&
			undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["callee"] && callee ==
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["callee"]) {
			if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["repeatsMonitor"]) {
				activeStream = activeStream + 1;
			} else {
				// 创建流判断是否以释放 未释放时标记累加
				if (undefined == cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["releaseTag"] || false ==
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["releaseTag"]) {
					activeStream = activeStream + 1;
				}
				// 复制主叫流信息
				jsonMediaValue["value"] = JSON.parse(JSON.stringify(cloudICP.util.callStatusMgr
					.videoMediaInfoBuf[key]["value"]));
			}
		}
	});
	if (activeStream < 3 && activeStream > 0) {
		iResult = true;
	}
	if ("" != jsonMediaValue["value"]) {
		return iResult;
	}

	Object.keys(cloudICP.util.callStatusMgr.windowInfos).forEach(function (key) {
		if (key == callee) {
			iResult = true;
		}
	});
	return iResult;
}

/**
 * 更新主监控界面离开标志
 * @param caller callee
 */
ICPSDK_Util.prototype.updateCopyMonitorReleaseTag = function (caller, callee) {
	Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function (key) {
		if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["caller"] && caller ==
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["caller"] &&
			undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["callee"] && callee ==
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["callee"]) {
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["releaseTag"] = true;
		}
	});
}

/**
 * 获取复制窗口结束标志
 * @param caller callee
 */
ICPSDK_Util.prototype.getCopyMonitorReleaseTag = function (caller, callee) {
	var recTag = false;
	var activeStream = 0;
	Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function (key) {
		if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["caller"] && caller ==
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["caller"] &&
			undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["callee"] && callee ==
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["callee"]) {
			// 创建流存在
			if (undefined == cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["releaseTag"] ||
				cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["releaseTag"] == false) {
				activeStream = 0;
				return;
			}
			if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["repeatsMonitor"]) {
				activeStream = activeStream + 1;
			}
		}
	});
	if (activeStream == 1) { // 该监控有且仅有一路时候释放
		recTag = true;
	}
	return recTag;
}

/**
 * clean repeats monitor Cache data
 * @param caller callee
 */
ICPSDK_Util.prototype.CleanThisRepeatsMonitor = function(caller, callee) {
	var strCids = [];
	Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function(key) {
		if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["caller"] && caller ==
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["caller"] &&
			undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["callee"] && callee ==
			cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["callee"]) {
			if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"][
					"repeatsMonitor"
				]) {
				strCids.push(key);
			}
		}
	});
	for (let i = 0; i < strCids.length; i++) {
		var tCid = strCids[i];
		if (undefined != tCid) {
			delete cloudICP.util.callStatusMgr.videoMediaInfoBuf[tCid];
		}
	}
}

/**
 * check ip is valid
 * @param ip
 */
ICPSDK_Util.prototype.checkIp = function(ip) {
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
 * check port is valid
 * @param port
 */
ICPSDK_Util.prototype.checkPort = function(port) {
	if (undefined == port) {
		return false;
	}
	var regex = /^([0-9]|[1-9]\d|[1-9]\d{2}|[1-9]\d{3}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5])$/;
	if (regex.test(port)) {
		return true;
	}
	return false;
}

/**
 * check vpnid is valid
 * @param {int} vpnid
 */
ICPSDK_Util.prototype.checkVpnid = function(vpnid) {
	if (undefined == vpnid || vpnid == null) {
		return false;
	}

	if (typeof vpnid != "number") {
		return false;
	}

	if (vpnid < -1 || vpnid >= 65535) {
		return false;
	}

	return true;
}

/**
 * check the alias of group
 */
ICPSDK_Util.prototype.checkAliasOfGroup = function(alias) {
	if (undefined == alias) {
		return false;
	}
	if ("" == alias.trim()) {
		return false;
	}
	var realLength = 0;
	var encodeStr = encodeURI(alias);
	var len = encodeStr.length;
	var charCode = -1;
	for (var i = 0; i < len; i++) {
		charCode = encodeStr.charCodeAt(i);
		realLength += 3;
		if (charCode == 37) {
			i += 2;
		}
	}

	if (realLength > (3 * 32)) {
		return false;
	}

	//不能为空，字符串最大长度为32个字节，不能用中英文逗号、单引号、双引号、分号、/、\符号、&符号、<符号、>符号，不能有连续的空格及连续的百分号，3个字节对应1个中文字。
	if (!(/[,\/\&\'\"\;<>\\\uff1b\u2019\u2018\uff0c\u201d\u201c\u3008\u3009\uff1e\uff1c]/.test(alias)) && !(/\s{2}/
			.test(alias)) && !(/\%{2}/.test(alias))) {
		return true;
	}
	return false;
}


/**
 * check showToolbar is valid
 * @param {int} buttonIDs
 */
ICPSDK_Util.prototype.checkshowToolbar = function(showToolbar) {
	if (undefined == showToolbar || showToolbar == null) {
		return false;
	}

	if (typeof showToolbar != "number") {
		return false;
	}

	if (showToolbar < 0 || showToolbar > 1) {
		return false;
	}

	return true;
}


/**
 * check buttonIDs is valid
 * @param {int} buttonIDs
 */
ICPSDK_Util.prototype.checkbuttonIDs = function(buttonIDs) {
	if (undefined == buttonIDs || buttonIDs == null) {
		return false;
	}

	if (typeof buttonIDs != "number") {
		return false;
	}

	if (buttonIDs < 0) {
		return false;
	}

	return true;
}


/**
 * check mode is valid
 * @param {string} mode
 */
ICPSDK_Util.prototype.checkmode = function(mode) {
	if (undefined == mode || mode == null) {
		return false;
	}

	if (typeof mode != "string") {
		return false;
	}
	let modes = new Set(["window", "wssflow", "plugins"])
	if (!modes.has(mode)) {
		return false;
	}

	return true;
}

/**
 * check the user name
 */
ICPSDK_Util.prototype.checkUserName = function(user) {
	if (undefined == user) {
		return false;
	}

	if (user == "") {
		return false;
	}

	var clength = 0;
	for (var index = 0; index < user.length; index++) {
		if (encodeURI(user[index]).length > 1) {
			clength += 3;
		} else {
			clength += 1;
		}
	}

	if (clength >= 33) {
		return false;
	}

	if (!(/[,\/\&\'\"\;<>\\\uff1b\u2019\u2018\uff0c\u201d\u201c\u3008\u3009\uff1e\uff1c]/.test(user)) && !(/\s{2}/
			.test(user)) && !(/\%{2}/.test(user))) {
		return true;
	}
	return false;
}

/**
 * check IdList is valid
 * @param string IdList
 */
ICPSDK_Util.prototype.checkIdList = function(IdList) {
	if (undefined == IdList || "" == IdList) {
		return false;
	}
	var regex = /[0-9]/;
	for (var i = 0; i < IdList.length; i++) {
		let pChar = IdList.charAt(i);
		if (!regex.test(pChar) && ';' != pChar) {
			return false;
		}
	}
	return true;
}


/**
 * get current date "yyyy-mm-dd HH:MM:SS.mmm"
 * @param {}
 */
ICPSDK_Util.prototype.getCurrentDate = function() {
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

/**
 * delete cid of wssflow mode
 * @param string cidStr
 */
 ICPSDK_Util.prototype.deleteWssflowModeCid = function (cidStr) {

    // 删除内嵌窗口cid
    var wssflowModeCids = cloudICP.util.callStatusMgr.wssflowModeCids
    if (undefined != wssflowModeCids[cidStr] && "wssflow" == wssflowModeCids[cidStr]) {
        delete cloudICP.util.callStatusMgr.wssflowModeCids[cidStr];
    }

    return;
}

Date.prototype.format = function(fmt) {
	var o = {
		"M+": this.getMonth() + 1, //月份
		"d+": this.getDate(), //日
		"h+": this.getHours(), //小时
		"m+": this.getMinutes(), //分
		"s+": this.getSeconds(), //秒
		"q+": Math.floor((this.getMonth() + 3) / 3), //季度
		"S": this.getMilliseconds() //毫秒
	};
	if (/(y+)/.test(fmt)) {
		fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
	}

	for (var k = 0; k < o.length; k++) {
		if (new RegExp("(" + k + ")").test(fmt)) {
			fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k])
				.length)));
		}
	}
	return fmt;
};



String.prototype.trim = function() {
	return this.replace(/(^\s*)|(\s*$)/g, "");
}

ICPSDK_Util.prototype.generateUUID = function() {
	return new Date().setMilliseconds(0).toString();
};

/**
 * resolve fmt
 * @param {string} fmt
 */
ICPSDK_Util.prototype.resolveFmt = function(fmt) {
	var ret = "720P"
	var fmtNum = parseInt(fmt);

	if (((fmtNum >> 4 & 1) == 1) && ((cloudICP.config.cameraInfo >> 4 & 1) == 1)) {
		ret = "1080P";
	} else if (((fmtNum >> 3 & 1) == 1) && ((cloudICP.config.cameraInfo >> 3 & 1) == 1)) {
		ret = "720P";
	} else if (((fmtNum >> 2 & 1) == 1) && ((cloudICP.config.cameraInfo >> 2 & 1) == 1)) {
		ret = "D1";
	} else if (((fmtNum >> 1 & 1) == 1) && ((cloudICP.config.cameraInfo >> 1 & 1) == 1)) {
		ret = "CIF";
	} else if (((fmtNum & 1) == 1) && ((cloudICP.config.cameraInfo & 1) == 1)) {
		ret = "QCIF";
	}

	return ret;
};

/**
 * resolve fmt
 * @param {string} fmt
 */
ICPSDK_Util.prototype.checkFmt = function(fmt) {
	var fmts = {
		"QCIF": 0,
		"CIF": 1,
		"D1": 2,
		"720P": 3,
		"1080P": 4,
		"2K": 5,
		"4K": 6
	};

	if ((cloudICP.config.cameraInfo >> fmts[fmt] & 1) == 1) {
		return true;
	} else {
		false;
	}
};

/**
 * includes implement
 * @param {string} fmt
 */
ICPSDK_Util.prototype.includes = function(arr, element) {
	for (var index = 0; index < arr.length; index++) {

		if (arr[index] == element) {
			return true;
		}
	}

	return false;
};

/**
 * name: getDefaultFmt
 * function: get default fmt from camera capabilities.
 */
ICPSDK_Util.prototype.getDefaultFmt = function() {
	var fmtsValueJson = {
		"1080P": 4,
		"720P": 3,
		"D1": 2,
		"CIF": 1,
		"QCIF": 0
	};
	var fmtValue = "1080P";
	var fmts = ["1080P", "720P", "D1", "CIF", "QCIF"];

	for (let index = 0; index < fmts.length; index++) {
		var fmt = fmts[index];
		if ((cloudICP.config.cameraInfo >> fmtsValueJson[fmt] & 1) == 1) {
			return fmts[index];
		}
	}
	return fmtValue;
};

/**
 * save log message to msp log files
 * @param {string} fmt
 */
ICPSDK_Util.prototype.sendLogOut = function(msg) {
	if (cloudICP.dispatch.webSocket._medisServerWs &&
		cloudICP.dispatch.webSocket._medisServerWs.readyState == cloudICP.dispatch.webSocket._medisServerWs.OPEN
	) {
		var param = {
			"description": "msp_client_log_out",
			"cmd": 131074,
			"param": {
				"logLevel": msg.logLevelNum,
				"logMsg": msg.logMsg,
			}
		}
		var logStr = cloudICP.util.sensitiveInfo.moveSensitiveInfo(JSON.stringify(param));
		cloudICP.dispatch.webSocket.sendDataToMediaSDK(logStr, false);
	}
};

ICPSDK_Util.prototype.findDuplicates = function(arr) {
	return arr.filter(function(item, index) {
		return arr.indexOf(item) != index
	})
};

/**
 * save log message to msp log files
 * @param {string} fmt
 */
ICPSDK_Util.prototype.checkConfId = function(param) {


	//SMC3.0开始ConfId是字符串，不再校验
	if (!param || "" == param || undefined == param) {
        return false;
    }
	return true;

	if (typeof param != "string") {
		return false;
	}

	if (!cloudICP.util.isNumber(param)) {
		return false;
	}

	if (parseInt(param) < 0) {
		return false;
	}

	if (param.length > cloudICP.dispatch.conf.MAX_CONF_ID.length) {
		return false;
	} else if (param.length < cloudICP.dispatch.conf.MAX_CONF_ID.length) {
		return true;
	} else {
		var index = 0;
		var i;
		var j;
		while (index < param.length) {
			i = param.charAt(index);
			j = cloudICP.dispatch.conf.MAX_CONF_ID.charAt(index);

			if (parseInt(i) > parseInt(j)) {
				return false;
			} else if (parseInt(i) < parseInt(j)) {
				return true;
			}

			index += 1
		}

		return true;
	}

	return true;
};

/*
* print jssdk interface input param.
* @param {}
*/
ICPSDK_Util.prototype.LogInputParam = function(param) {
	// 打印接口入参
	var tabPageId = param.tabPageId;
	var curTime = cloudICP.util.getCurrentDate();
	var methodName = null;
	var moduleName = null;
	var tmpStr;
	try {
		throw new Error();
	} catch (n) {
		tmpStr = n.stack.split("\n");
	}

	for (var index = 0; index < tmpStr.length; index++) {
		if (tmpStr[index].indexOf("ICPSDK_Dispatch") != -1) {
			methodName = tmpStr[index].split(/\W/)[6];
			moduleName = tmpStr[index].split(/\W/)[5];
			break;
		}
	}
	var logStr = cloudICP.util.sensitiveInfo.moveSensitiveInfo(JSON.stringify(param));
	cloudICP.util.log(tabPageId, { "logLevel": "info", "logMsg": "Calling [ " + moduleName + "." + methodName + " ], input param: " + logStr + " ]" });
	return true;
};

function base64UrlToStandard(base64) {
    return base64.replace(/-/g, '+').replace(/_/g, '/');
}

function addBase64Padding(base64) {
    return base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
}

function decodeBase64(base64) {
    base64 = base64UrlToStandard(base64);    // 替换 URL 特殊字符
    base64 = addBase64Padding(base64);       // 补全 `=`
    return atob(base64);                     // 使用 atob 解码
}

function CALL_STATUS_MGR() {
	var _mthis = this;
	// 通话状态码
	this.IDLE = -1;
	this.NEW = 0;
	this.RINGING = 1;
	this.CONNECTING = 2;
	this.HOLD = 3;
	this.RELEASE = 4;

	// 通话类型 TODO
	this.VOICE = 0;
	this.VIDEO = 1;
	this.HALFVOICE = 2;
	this.DISCREET = 3;
	this.MONITOR = 4;
	this.VIDEODISPATCH = 5;
	this.AMBIENCE = 6;
	this.VOICECONF = 7;
	this.VIDEOCONF = 8;
	this.GROUP = 9;

	//
	this.callTypeStr = [
		"voice", // 0 voice
		"video", // 1 video
		"halfdial", // 2 halfvoice
		"discreenlisten", // 3 discreet
		"monitor", // 4 monitor
		"dispatch", // 5 videodispatch
		"ambience", // 6 ambience
		"voiceconf", // 7 voiceconf
		"videoconf", // 8 videoconf
		"group", // 8 group
	];

	// 当前存在的各个通话的数量
	this.numberOfCall = [
		0, // voice
		0, // video
		0, // halfvoice
		0, // discreet
		0, // monitor
		0, // videodispatch
		0, // ambience
		0, // voiceconf
		0, // videoconf
	];

	// 正在通话的call
	this.connectingCallMgr = [
		[], // voice
		[], // video
		[], // halfvoice
		[], // discreet
		[], // monitor
		[], // videodispatch
		[], // ambience
		[], // voiceconf
		[], // videoconf
	];

	// 视频媒体信息
	// 由于创建窗口到创建视频通道是由不同事件驱动的，所以维护该信息用于传输
	this.videoMediaInfoBuf = {};

	// 内嵌模式视频监控的数量
    this.wssflowModeCids = {};

	// 维护保持去保持接口调用的关系。
	this.holdMgr = [];

	// 保存组呼的cid用于与msp进行交互。
	// 主要是规避有些场景，只能获取group id不能获取组呼cid的问题。
	this.groupCallMgr = {};

	// 维护推送的会议状态信息
	this.confMgr = {
		"accessInfo": {}
	};

	// 当前用户通话状态信息
	this.userStatusMgr = {};

	// 当前正在接通的会议的cid
    this.currentGoingConfCid = 0;

	// 当前正在接通的会议，0表示无会议，其余value位会议id
	this.currentGoingConf = 0;

	// 当前订阅的会议
	this.confSubMgr = {};

	// 视频窗口信息
	this.windowInfos = [];

	this.broadcastFile = {};
	this.broadcastBase64 = {};

	// 所有通话信息
	this.allCalls = {
		"-1": {
			// 通话状态
			callState: -1,

			// 通话接收时间
			startTime: new Date().getTime(),

			caller: "0",

			callee: "0",

			callType: 0,

			fmt: "0",

			grpid: "0",

			members: null
		}
	};

	// 正在呼叫通话
	this.callingCid = -1;

	// 当前通话状态
	this.currentCallState = this.IDLE;

	this.deleteBroadcastFile = function(isdn) {
		if (undefined != this.broadcastFile[isdn]) {
			delete this.broadcastFile[isdn];
			return;
		}

		var userName = "0" + isdn;
		if (undefined != this.broadcastFile[userName]) {
			delete this.broadcastFile[userName];
			return;
		}

		var userName1 = "00" + isdn;
		if (undefined != this.broadcastFile[userName1]) {
			delete this.broadcastFile[userName1];
			return;
		}
	}

	this.deleteBroadcastBase64 = function (isdn) {
        if (undefined != this.broadcastBase64[isdn]) {
            delete this.broadcastBase64[isdn];
            return;
        }

        var userName = "0" + isdn;
        if (undefined != this.broadcastBase64[userName]) {
            delete this.broadcastBase64[userName];
            return;
        }

        var userName1 = "00" + isdn;
        if (undefined != this.broadcastBase64[userName1]) {
            delete this.broadcastBase64[userName1];
            return;
        }
    }

	this.getBroadcastFile = function(isdn) {
		if (undefined != this.broadcastFile[isdn]) {
			return this.broadcastFile[isdn];
		}

		var userName = "0" + isdn;
		if (undefined != this.broadcastFile[userName]) {
			return this.broadcastFile[userName];
		}

		var userName1 = "00" + isdn;
		if (undefined != this.broadcastFile[userName1]) {
			return this.broadcastFile[userName1];
		}

		return null;
	}

    this.getBroadcastBase64 = function (isdn) {
        if (undefined != this.broadcastBase64[isdn]) {
            return this.broadcastBase64[isdn];
        }

        var userName = "0" + isdn;
        if (undefined != this.broadcastBase64[userName]) {
            return this.broadcastBase64[userName];
        }

        var userName1 = "00" + isdn;
        if (undefined != this.broadcastBase64[userName1]) {
            return this.broadcastBase64[userName1];
        }

        return null;
    }

    this.saveBroadcastFile = function (isdn, audioFile, audioBase64) {
        this.broadcastFile[isdn] = audioFile;
        this.broadcastBase64[isdn] = audioBase64;
    }

	this.updateConfInfo = function(data, isSaveAccessInfo) {
		if (isSaveAccessInfo == true) {
			if (_mthis.confMgr["accessInfo"] == undefined) {
				_mthis.confMgr["accessInfo"] = {};
			}
			_mthis.confMgr["accessInfo"][data["confId"]] = data;
		} else {
			_mthis.confMgr[data["value"]["confId"]] = data;
		}
	}

	this.updateUserStatus = function(data) {
		_mthis.userStatusMgr[data["value"]["peerid"]] = data["statusvalue"];
		_mthis.userStatusMgr[data["value"]["isdn"]] = data["statusvalue"];

		// 用于规避问题
		// 当通话被转接或抢话的时候，caller和callee的信息会出错，需要icpsdk来矫正。
		if (cloudICP.util.callStatusMgr.connectingCallMgr[0].length != 0 && data["statusvalue"] == "4022") {
			var cid = cloudICP.util.callStatusMgr
				.callingCid; //cloudICP.util.callStatusMgr.connectingCallMgr[0][cloudICP.util.callStatusMgr.connectingCallMgr[0].length - 1];

			var call = cloudICP.util.callStatusMgr.allCalls[cid];

			if (data["value"]["isdn"] == cloudICP.userInfo["isdn"]) {
				call["callee"] = data["value"]["isdn"];
				call["caller"] = data["value"]["peerid"];
			} else {
				call["callee"] = data["value"]["peerid"];
				call["caller"] = data["value"]["isdn"];
			}
		}
	}

	this.updateCallStatus = function(callType, callState, data, confInfo) {
		var cid = data["value"]["cid"];

		if (callType == _mthis.GROUP) {
			_mthis.groupCallMgr[data["grpid"]] = cid;
			return;
		}

		if (_mthis.allCalls[cid] == undefined) {
			_mthis.allCalls[cid] = {
				callType: callType,
				caller: data["value"]["caller"],
				callee: data["value"]["callee"],
				startTime: new Date().getTime(),
			};

			_mthis.numberOfCall[callType] += 1;
		}

		var call = _mthis.allCalls[cid];

		call["callState"] = callState;

		if (callType == _mthis.VIDEOCONF) {
			call.callType = _mthis.VIDEOCONF;
		} else {
			callType = call.callType;
		}

		if (confInfo) {
			call["confId"] = confInfo["confInternalId"];
		} else if (data["value"] && data["value"]["confInfo"] != undefined) {
			call["confId"] = data["value"]["confInfo"]["confInternalId"];
		}

		switch (callState) {
			case _mthis.RINGING:
				if (call.callType != _mthis.MONITOR) {
					_mthis.callingCid = cid;
					_mthis.currentCallState = _mthis.RINGING;
				}
				break;
			case this.CONNECTING:
				// 对视频能力进行维护
				if (call.callType == _mthis.VIDEOCONF || call.callType == _mthis.VIDEO || call.callType == _mthis
					.MONITOR || call.callType == _mthis.VIDEODISPATCH) {
					call["fmt"] = data["value"]["fmt"];

					switch (data["value"]["fmt"]) {
						case "0":
							cloudICP.dispatch.video.currentVideoAbility += 0.5
							break;
						case "1":
							cloudICP.dispatch.video.currentVideoAbility += 0.5
							break;
						case "2":
							cloudICP.dispatch.video.currentVideoAbility += 1
							break;
						case "3":
							cloudICP.dispatch.video.currentVideoAbility += 2
							break;
						case "4":
							cloudICP.dispatch.video.currentVideoAbility += 4
							break;
						case "5":
							cloudICP.dispatch.video.currentVideoAbility += 8
							break;
						case "6":
							cloudICP.dispatch.video.currentVideoAbility += 16
							break;
						default:
							break;
					}
				}
				_mthis.callingCid = cid;
				if (call.callType != _mthis.MONITOR) {
					_mthis.currentCallState = _mthis.CONNECTING;
				}

				if (!cloudICP.util.includes(_mthis.connectingCallMgr[callType], cid)) {
					_mthis.connectingCallMgr[callType].push(cid);
				}

				if (call.callType == _mthis.VIDEOCONF || call.callType == _mthis.VOICECONF) {
                    _mthis.currentGoingConf = call["confId"];
                    _mthis.currentGoingConfCid = cid;
                }
				break;
			case _mthis.RELEASE:
				// 对视频能力进行维护
				if (call.callType == _mthis.VIDEOCONF || call.callType == _mthis.VIDEO || call.callType == _mthis
					.MONITOR || call.callType == _mthis.VIDEODISPATCH) {
					switch (call["fmt"]) {
						case "0":
							cloudICP.dispatch.video.currentVideoAbility -= 0.5
							break;
						case "1":
							cloudICP.dispatch.video.currentVideoAbility -= 0.5
							break;
						case "2":
							cloudICP.dispatch.video.currentVideoAbility -= 1
							break;
						case "3":
							cloudICP.dispatch.video.currentVideoAbility -= 2
							break;
						case "4":
							cloudICP.dispatch.video.currentVideoAbility -= 4
							break;
						case "5":
							cloudICP.dispatch.video.currentVideoAbility -= 8
							break;
						case "6":
							cloudICP.dispatch.video.currentVideoAbility -= 16
							break;
						default:
							break;
					}
				}
				if (_mthis.callingCid == cid) {
                    _mthis.callingCid = -1;
                    if (call.callType != _mthis.MONITOR) {
                    	_mthis.currentCallState = _mthis.IDLE;
                    }
                }
                _mthis.numberOfCall[callType] -=1;
                var index = _mthis.connectingCallMgr[callType].indexOf(cid);
                if (index != -1) {
                    _mthis.connectingCallMgr[callType].splice(index, 1);
                }

                if (call.callType == _mthis.VIDEOCONF || call.callType == _mthis.VOICECONF) {
                    if (_mthis.currentGoingConf == call["confId"]) {
                        _mthis.currentGoingConf = 0;
                        _mthis.currentGoingConfCid = cid;
                    }
                    if (undefined != cloudICP.util.callStatusMgr.windowInfos[2147483647]){
                        delete cloudICP.util.callStatusMgr.windowInfos[2147483647];
                    }
                }
                break;
			case _mthis.HOLD:
				var index = _mthis.connectingCallMgr[callType].indexOf(cid);
				if (index != -1) {
					_mthis.connectingCallMgr[callType].splice(index, 1);
				}
				break;
			default:
				break;
		}

		return true;
	};

	this.holdCall = function(cid, tabPageId) {
		var currentCall = _mthis.allCalls[cid];

		// 因为视频监控和视频分发可并发，所以不需要进行自动保持
		if (
			currentCall.callType == _mthis.MONITOR ||
			currentCall.callType == _mthis.VIDEODISPATCH
		) {
			return false;
		}

		if (currentCall.callType != _mthis.DISCREET &&
			currentCall.callType != _mthis.HALFVOICE &&
			currentCall.callType != _mthis.VIDEO &&
			currentCall.callType != _mthis.VIDEOCONF) {
			if (_mthis.connectingCallMgr[_mthis.VIDEO].length != 0 ||
				_mthis.connectingCallMgr[_mthis.DISCREET].length != 0 ||
				_mthis.connectingCallMgr[_mthis.HALFVOICE].length != 0 ||
				_mthis.connectingCallMgr[_mthis.VIDEOCONF].length != 0) {
				return _mthis.holdHelper(currentCall, cid, tabPageId);
			};
		}

		var toHoldCall;
		var checkArr = [_mthis.VOICE, _mthis.VOICECONF, _mthis.AMBIENCE];
		for (var i = 0; i < checkArr.length; i++) {
			var type = checkArr[i];

			for (var j = 0; j < _mthis.connectingCallMgr[type].length; j++) {
				if (_mthis.connectingCallMgr[type][j] != cid) {
					toHoldCall = _mthis.allCalls[_mthis.connectingCallMgr[type][j]];

					var ret = _mthis.holdHelper(toHoldCall, _mthis.connectingCallMgr[type][j], tabPageId);
					if (ret == true) {
						var index = _mthis.connectingCallMgr[type].indexOf(_mthis.connectingCallMgr[type][j]);
						if (index != -1) {
							_mthis.connectingCallMgr[type].splice(index, 1);
						}
					}

					return ret;
				}
			}
		}

		return false;
	};

	this.unholdCall = function(cid, tabPageId) {
		var currentCall = this.allCalls[cid];

		if (_mthis.connectingCallMgr[_mthis.VIDEO].length != 0 ||
			_mthis.connectingCallMgr[_mthis.DISCREET].length != 0 ||
			_mthis.connectingCallMgr[_mthis.HALFVOICE].length != 0 ||
			_mthis.connectingCallMgr[_mthis.VIDEOCONF].length != 0) {
			return false;
		};

		var toHoldCall;
		var checkArr = [_mthis.VOICE, _mthis.VOICECONF, _mthis.AMBIENCE];
		for (var i = 0; i < checkArr.length; i++) {
			var type = checkArr[i];

			for (var j = 0; j < _mthis.connectingCallMgr[type].length; j++) {
				if (_mthis.connectingCallMgr[type][j] != cid) {
					toHoldCall = _mthis.allCalls[_mthis.connectingCallMgr[type][j]];

					var ret = _mthis.holdHelper(toHoldCall, _mthis.connectingCallMgr[type][j], tabPageId);
					if (ret == true) {
						if (currentCall) {
							var type = currentCall.callType;
							_mthis.connectingCallMgr[type].push(cid);
						}

						var index = _mthis.connectingCallMgr[type].indexOf(_mthis.connectingCallMgr[type][j]);
						if (index != -1) {
							_mthis.connectingCallMgr[type].splice(index, 1);
						}
					}
					return ret;
				}
			}
		}
		return false;
	}

	this.holdHelper = function(call, cid, tabPageId) {
		if (call) {
			if (call["callee"] && call["callee"] == cloudICP.userInfo["isdn"]) {
				if (call.callType != _mthis.VOICECONF) {
					cloudICP.dispatch.voice.hold({
						"to": call["caller"],
						"cid": cid,
						"callback": function(data) {
							if (data["rsp"] != "0") {
								var retEvent = {
									"eventName": "OnHoldFailure",
									"rsp": data["rsp"],
									"value": {
										"peerid": "",
										"cid": ""
									}
								}
								cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnHoldFailure"].event = retEvent;
								sendEvent2Page(tabPageId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnHoldFailure"]);
							}
						}
					});
				} else {
					cloudICP.dispatch.conf.holdConf({
						"to": call["caller"],
						"cid": cid,
						"callback": function(data) {
							if (data["rsp"] != "0") {
								var retEvent = {
									"eventName": "OnHoldFailure",
									"rsp": data["rsp"],
									"value": {
										"peerid": "",
										"cid": ""
									}
								}
								cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnHoldFailure"].event = retEvent;
								sendEvent2Page(0, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnHoldFailure"]);
							}
						}
					});
				}
				return true;
			} else if (call["caller"] && call["caller"] == cloudICP.userInfo["isdn"]) {
				if (call.callType != _mthis.VOICECONF) {
					cloudICP.dispatch.voice.hold({
						"to": call["callee"],
						"cid": cid,
						"callback": function(data) {
							if (data["rsp"] != "0") {
								var retEvent = {
									"eventName": "OnHoldFailure",
									"rsp": data["rsp"],
									"value": {
										"peerid": "",
										"cid": ""
									}
								}
								cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnHoldFailure"].event = retEvent;
								sendEvent2Page(tabPageId, cloudICP.dispatch.event.EVENT_LIST["VoiceNotify"]["OnHoldFailure"]);
							}
						}
					});
				} else {
					cloudICP.dispatch.conf.holdConf({
						"to": call["callee"],
						"cid": cid,
						"callback": function(data) {
							if (data["rsp"] != "0") {
								var retEvent = {
									"eventName": "OnHoldConfFailure",
									"rsp": data["rsp"],
									"value": {
										"confCode": "",
										"cid": ""
									}
								}
								cloudICP.dispatch.event.EVENT_LIST["PhoneConfNotify"]["OnHoldConfFailure"].event = retEvent;
								sendEvent2Page(tabPageId, cloudICP.dispatch.event.EVENT_LIST["PhoneConfNotify"]["OnHoldConfFailure"]);
							}
						}
					});
				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	// 确认是否有无法并发的通话存在
	this.isAnyCallExisted = function() {
		return (_mthis.connectingCallMgr[_mthis.VIDEO].length != 0 ||
			_mthis.connectingCallMgr[_mthis.DISCREET].length != 0 ||
			_mthis.connectingCallMgr[_mthis.HALFVOICE].length != 0 ||
			_mthis.connectingCallMgr[_mthis.VOICE].length != 0 ||
			_mthis.connectingCallMgr[_mthis.AMBIENCE].length != 0 ||
			_mthis.connectingCallMgr[_mthis.VOICECONF].length != 0 ||
			_mthis.connectingCallMgr[_mthis.VIDEOCONF].length != 0)
	}

	this.cleanUp = function() {
		_mthis.currentGoingConf = 0;
		_mthis.currentCallState = _mthis.IDLE;
		_mthis.callingCid = -1;
		_mthis.allCalls = {};


		_mthis.numberOfCall = [
			0, // voice
			0, // video
			0, // halfvoice
			0, // discreet
			0, // monitor
			0, // videodispatch
			0, // ambience
			0, // voiceconf
			0, // videoconf
		];

		_mthis.connectingCallMgr = [
			[], // voice
			[], // video
			[], // halfvoice
			[], // discreet
			[], // monitor
			[], // videodispatch
			[], // ambience
			[], // voiceconf
			[], // videoconf
		];

		_mthis.videoMediaInfoBuf = {

		}

		_mthis.holdMgr = [];

		_mthis.groupCallMgr = {};

		_mthis.confMgr = {};

		_mthis.userStatusMgr = {}
		_mthis.mediaModeMap.clear();
	}

	this.mediaModeMap = new Map();
	this.checkIsExist = function(param) {
		if (this.mediaModeMap.size === 0) {
			return true;
		} else {
			let mediaInfo = this.mediaModeMap.get(parseInt(param["cid"]));
			if (undefined == mediaInfo) {
				return false;
			}
			let switch_int = mediaInfo["switch"];
			if (switch_int == undefined || switch_int == parseInt(param["switch"])) {
				return false;
			} else {
				return true;
			}
		}
	};

	this.addMediaModeInfo = function(param) {
		let mediaInfo = {
			"type":  param["type"],
			"switch" : param["switch"]
		}
		return this.mediaModeMap.set(param["cid"], mediaInfo);
	};

	this.delMediaModeInfo = function(param) {
		return this.mediaModeMap.delete(parseInt(param["cid"]));
	};

	//	记录 内嵌窗口 相关信息 用来退出时 关闭内嵌窗口
	this.wssflowDemoMap = new Map();
	this.addWssflowDemoMap = function(cid , demo) {
		this.wssflowDemoMap.set(parseInt(cid), demo);
	}
	this.delWssflowDemoMap = function(cid) {
		var demo = this.wssflowDemoMap.get(parseInt(cid));
		if (demo != undefined && typeof demo.closeFunc === 'function') {
			demo.closeFunc();
		}
		this.wssflowDemoMap.delete(parseInt(cid));
	}

	this.cleanWssflowDemoMap = function() {
		this.wssflowDemoMap.forEach((item, key) => {
			if (item !== undefined && typeof item.closeFunc === 'function') {
				item.closeFunc();  // 调用 closeFunc 方法
			}
		});
		this.wssflowDemoMap.clear();
	}
};

function SENSITIVE_INFOS() {
	//敏感信息关键字,false表示部分保留，true便是全去敏
	this.infos = [
		["isdn", false], //成员号
		["seq", false], //序列号
		["session", true],
		["user", false],
		["localIP", false], //本地ip和端口
		["serverIP", false], //服务器ip和端口
		["remoteIP", false], //服务器ip和端口
		["password", true], //密码
		["serverip", false], //服务器ip和端口
		["ssotoken", true], //ssotoken
		["ipccserveripport", true], //服务器ip和端口
		//语音状态通知
		["callee", false], //主叫号
		["caller", false], //被叫号
		["targeter", false], //转接方
		["transfer", false], //被转接方
		["local", false], //本地ip和端口
		["server", false], //服务器ip和端口
		//视频状态通知
		["from", false], //主叫号
		["to", false], //被叫号
		["local_audio", false], //本地语音ip和端口
		["local_video", false], //本地视频ip和端口
		["server_audio", false], //服务器语音ip和端口
		["server_video", false], //服务器视频ip和端口
		//群组配置更新通知
		["departmentid", false], //部门id
		["group", false], //群组号
		["name", false], //群组名
		["setupdcid", false], //创建者用户号
		["rid", false], //接收方用户号，0：表示广播
		["dcpatchindex", false], //派接组序列号
		["grpnumber", false], //群组号
		["pgname", false], //群组名
		["groupnumber", false], //群组号
		["membergroup", false], //成员群组号
		["vpnid", false], //vpn号
		//短彩信更新通知
		["content", true], //消息内容
		["emerg_groupid", false], //紧急状态的群组ID
		["emerg_ueid", false], //紧急状态的终端ID
		["groupid", false], //群组号
		["attach", false], //彩信附件地址
		//GIS状态通知
		["cellname", false], //终端名称
		["location", false], //位置信息
		["cellid", false],

		//资源状态通知
		["speaker", false], //组呼状态通知，发言人
		["attaching", false], //注册状态通知,用户当前加入的组号
		["peerid", false],
		["speakid", false],
		["resid", false],
		["memberid", false],
		["localip", false],
		["newip", false],
		//创建会议通知
		["passcode", true], //密码
		["ObhId", false],
		["SpeakerId", false],
		//音视频会议通知
		["Number", false], //成员号码
		["Name", false], //成员名称
		["number", false],
		["owner", false], //上墙发起者名称
		["termName", false], //终端名称
	];

	/**
	 * 处理敏感信息
	 */
	this.moveSensitiveInfo = function(data) {
		var strReturn = ('' + data).slice(0);
		for (var i = 0; i < this.infos.length; i++) {
			var targetInfo = this.infos[i][0];
			var isSecret = this.infos[i][1];
			var patternStr = '"' + targetInfo + '[\\\\"]"?.?:.?[\\\\"]?"?([\\w\\.]*?)[\\\\"]?"?[,}]';
			var parten = new RegExp(patternStr);
			var resArr = strReturn.match(parten);
			for (var it in resArr) {
				var strReplace = resArr[it];
				var reg = /:.*?([\w\.]+).*?[,}]/;
				var texts = reg.exec(strReplace);
				if (!texts) {
					break;
				}
				var text = "";
				if (!isSecret) {
					text = '****' + texts[1].charAt(texts[1].length - 1);
				} else {
					text = '*****';
				}
				var strRes = strReplace.replace(texts[1], text);
				strReturn = strReturn.replace(strReplace, strRes);
			}
		}
		return strReturn;
	};
}

if (!String.prototype.format) {
	String.prototype.format = function() {
		var args = arguments;
		return this.replace(/{(\d+)}/g, function(match, number) {
			return typeof args[number] != 'undefined' ?
				args[number] :
				match;
		});
	};
}

ICPSDK_Util.prototype.parseMediaData = function(buf, data) {
    // 创建 DataView 用于解析二进制数据
    let view = new DataView(buf);

    // 读取 result（1 字节）
    let result = view.getUint32(0, true);
	data["result"] = result;
	// console.debug("result:" + result);

    // 读取 rsp（4 字节）
    let rsp = view.getUint32(4, true);  // 从第 1 字节开始读取 4 字节
	data["rsp"] = rsp;
	// console.debug("rsp:" + rsp);

    // 读取 format（16 字节）
    let format = '';
    for (let i = 8; i < 24; i++) {
    const charCode = view.getUint8(i);
		if (charCode === 0) break;  // 遇到填充字节 (null) 时停止
		format += String.fromCharCode(charCode);
	}
	data["format"] = format;
	// console.debug("format:" + format);

    // 读取 len（4 字节）
    let len = view.getUint32(24, true);
	// console.debug("len:" + len);

    // 读取 cid（4 字节）
    let cid = view.getUint32(28, true);
	data["cid"] = cid;
	// console.debug("cid:" + cid);

    // 读取 buf（len 字节）
    let dataArray = new Uint8Array(buf, 32, len);
	data["data"] = dataArray;

    // 可以根据需要对 data 进行进一步的处理，例如解码 base64 数据等
}
var portList = {};
// var isSupportimportScripts = false;
// if(Window.importScripts){
//   isSupportimportScripts = true;//WorkerGlobalScope.importScripts()
// }

// 将消息广播给每一个页面
broadcast = function(type, jsonMsg) {
	//return;
	Object.keys(portList).forEach(function(key) {
		sendMsg2Page(key, type, jsonMsg);
	});
}

function sendMsg2Page(tabPageId, type, param) {
	if (tabPageId == 0) {
		broadcast(type, param)
		return false;
	}
	if (typeof param != "string") {
		param = json2String(param);
	}
	var toPageJsonMsg = {
		"type": type, // 消息的类型如：：log:日志、response:接口调用返回、event：异步事件、request:请求、lifecycle:生命周期
		"tabPageId": tabPageId, // 对应页面ID
		"value": param, // 回应的所有消息
	};
	console.log(tabPageId, "sendMsg2Page->", toPageJsonMsg)
	portList[tabPageId].postMessage(json2String(toPageJsonMsg));
	return true;
}

function sendEvent2Page(tabPageId, param) {
	// if (typeof param != "string") {
	// 	param = json2String(param);
	// }

	// 如果是没有进行注册的事件则找不到回调函数和event对象，此时调用默认处理函数printEvent
	if (param.event == undefined || param.funcId == undefined) {
		cloudICP.dispatch.event.printEvent(param.event)
		return;
	}

	param["event"]["funcId"] = param.funcId;
	// 默认返回的窗口对象为主窗口
	var recvTabPageId = mainPageId;

	if (tabPageId != "0" && tabPageId != 0) {
		Object.keys(portList).forEach(function(key) {
			// 如果tabPageId存在于portList中，recvTabPageId改变，结束循环
			if (key == tabPageId) {
				recvTabPageId = tabPageId;
			}
		});
	} else {
		recvTabPageId = 0;
	}

	var toPageJsonMsg = {
		"type": messageType.event, // 消息的类型如：：log:日志、response:接口调用返回、event：异步事件、request:请求、lifecycle:生命周期
		"tabPageId": recvTabPageId, // 对应页面ID
		"value": param.event, // 回应的所有消息
		"time": new Date().valueOf() + "-" + Math.ceil(Math.random() * 1000),
	};
	console.log(recvTabPageId, "sendEvent2Page->", toPageJsonMsg)
	console.log(json2String(toPageJsonMsg))
	if (recvTabPageId == 0) {
		Object.keys(portList).forEach(function(key) {
			portList[key].postMessage(json2String(toPageJsonMsg));
		});
		return;
	}
	cloudICP.util.sendLogOut({"logLevelNum" : 1, "logMsg" : "ICPSDK event: " + JSON.stringify(toPageJsonMsg)});
	portList[recvTabPageId].postMessage(json2String(toPageJsonMsg));
}

var initFlag = false;
var cloudICPError = {
	"ret": -1,
	"msg": "cloudICP object is null."
};

var paramError = {
	"ret": -2,
	"msg": "not get type parameter."
};

string2Json = function(strJson) {
	try {
		var a = JSON.parse(strJson, function(k, v) {
			if (null != v && v.indexOf && v.indexOf('function') > -1) {
				return eval("(function(){return " + v + " })()")
			} else if (null != v && v.indexOf && v.indexOf('=>') > -1) { // 支持箭头函数的转换
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

var loginUserId = undefined;
var sdkStatusFuncId = undefined;

onconnect = function(e) {
	var port = e.ports[0];
	var bExist = false;
	port.onmessage = function(e) {
		var jsonMsg = e.data[0];
		var recvEDataJsonMsg = string2Json(jsonMsg);
		console.log("recvEDataJsonMsg->", recvEDataJsonMsg)
		console.log(recvEDataJsonMsg)

		//过滤无效的请求消息
		if (undefined == recvEDataJsonMsg["type"] || undefined == recvEDataJsonMsg["value"]) {
			var msg = {
				type: messageType.lifecycle,
				tabPageId: recvEDataJsonMsg.tabPageId,
				value: json2String(paramError),
			}
			port.postMessage(json2String(msg));
			return;
		}

		var operateId = recvEDataJsonMsg.operateId;
		// recvEDataJsonMsg.value.callback = function(data) {
		// 	data['callback'] = callback;
		// 	sendMsg2Page(tabPageId, messageType.response, data);
		// }
		//第一次初始化MSP插件
		if (undefined == cloudICP && recvEDataJsonMsg["type"] != messageType.lifecycle) {
			var msg = {
				type: 1,
				jsonMsg: json2String(cloudICPError),
			}
			port.postMessage(json2String(msg));
			return;
		}
		var tabPageId = recvEDataJsonMsg.tabPageId

		var operateId = recvEDataJsonMsg.operateId;
		// 把JSSDK传入worker的funcId设置到ajax的回调函数里面返回给JSSDK，JSSDK根据funcId调用对应的缓存函数
		var originFuncId = recvEDataJsonMsg.value.funcId;
		recvEDataJsonMsg.value.callback = function(data) {
			data["funcId"] = originFuncId;
			sendMsg2Page(tabPageId, messageType.response, data);
		}
		if (operateId >= 0 && operateId < 100) {
			lifecycleTreatments(port, jsonMsg);
		} else if (operateId >= 100 && operateId < 200) {
			authenticationTreatments(recvEDataJsonMsg, originFuncId);
		} else if (operateId >= 200 && operateId < 300) {
			voiceTreatments(recvEDataJsonMsg);
		} else if (operateId >= 300 && operateId < 400) {
			videoTreaments(recvEDataJsonMsg);
		} else if (operateId >= 400 && operateId < 500) {
			groupTreatments(recvEDataJsonMsg);
		} else if (operateId >= 500 && operateId < 600) {
			confTreatments(recvEDataJsonMsg);
		} else if (operateId >= 600 && operateId < 700) {
			smsTreatments(recvEDataJsonMsg);
		} else if (operateId >= 700 && operateId < 800) {
			gisTreaments(recvEDataJsonMsg);
		} else if (operateId >= 800 && operateId < 900) {
			queryTreatments(recvEDataJsonMsg);
		} else if (operateId >= 900 && operateId < 1000) {
			deviceTreatments(recvEDataJsonMsg);
		} else if (operateId >= 1000 && operateId < 1100) {
			eventRegisterTreatments(recvEDataJsonMsg);
		} else if (operateId >= 1100 && operateId < 1200) {
			ModifyMdcIpTreatments(recvEDataJsonMsg);
		} else if (operateId >= 1200 && operateId < 1300) {
			UtilTreatments(recvEDataJsonMsg);
		}
	}
}


// 生命周期处理
function lifecycleTreatments(port, jsonMsg) {
	var recvEDataJsonMsg = string2Json(jsonMsg);
	var operateId = recvEDataJsonMsg.operateId;
	var tabPageId = recvEDataJsonMsg.tabPageId;
	if (!(operateId >= 0 && operateId < 100)) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	console.log("shareWork recv msg2:" + recvEDataJsonMsg);
	var bExist = false;
	switch (operateId) {
		case 1: // page第一次握手，分配tabPageId
		{
			var msg = {
				type: messageType.lifecycle,
				tabPageId: new Date().valueOf() + "-" + Math.ceil(Math.random() * 1000),
			}
			port.postMessage(json2String(msg));
		}
		break;
	case 2: // page第二次握手，检测连接是否存在，不存在则将连接放入列表
	{
		var recvEDataJsonMsgValue = recvEDataJsonMsg.value;
		// 若列表里面没有这个连接则将其放入列表，否则直接返回
		Object.keys(portList).forEach(function(key) {
			if (portList[key] === port) {
				bExist = true;
				return;
			}
		});
		if (false == bExist) {
			if (Object.keys(portList).length == 0) {
				mainPageId = recvEDataJsonMsg.tabPageId;
			}
			portList[recvEDataJsonMsg.tabPageId] = port; // 将连接放入列表
		}
		// 根据cloudICP是否存在来判断是否已经初始化MSP，如果已经初始化MSP就设置status为1并返回；
		// JSSDK根据status是否为1来决定是否注册事件；0：注册事件；1：JSSDK自行加载事件的funcId
		if (undefined != cloudICP) {
			recvEDataJsonMsgValue['status'] = "1";
		}
		if (undefined != cloudICP && true == cloudICP.loginFlag) {
			//sendMsg2Page(port, 1, "{\"msg\":\"cloudICP is Init, user is login!\"}");
			jsonMsg.msg = "cloudICP is Init, user is login!";
			recvEDataJsonMsgValue['rsp'] = "0";
			let msg = {
				isdn: loginUserId,
				type: messageType.lifecycle,
				tabPageId: recvEDataJsonMsg.tabPageId,
				value: recvEDataJsonMsgValue,
			}
			port.postMessage(json2String(msg));
			break;
		} else if (undefined != cloudICP) {
			//sendMsg2Page(port, 1, "{\"msg\":\"cloudICP is Init, user is logout!\"}");
			jsonMsg.msg = "cloudICP is Init, user is logout!";
			let msg = {
				type: messageType.lifecycle,
				tabPageId: recvEDataJsonMsg.tabPageId,
				value: recvEDataJsonMsgValue,
			}
			port.postMessage(json2String(msg));
			break;
		}
		console.log("start init icpsdk")
		cloudICP = new ICPSDK(recvEDataJsonMsgValue);
		// 第一次初始化时,记录下sdkStatusNotify的funcId,后续其他页面初始化时在reportDispatchSdkStatus()需要用到
		sdkStatusFuncId = recvEDataJsonMsgValue.funcId;
		console.log("end init icpsdk")
		//sendMsg2Page(port, 1, jsonMsg);
		let msg = {
			type: messageType.lifecycle,
			tabPageId: recvEDataJsonMsg.tabPageId,
			value: recvEDataJsonMsgValue,
		}
		port.postMessage(json2String(msg));
		if (false == initFlag) {
			cloudICP.dispatch.webSocket.connectToLocalDeamon();
			initFlag = true;
		}
	}
	break;
	case 3: // page销毁自身请求
	{
		if (portList[recvEDataJsonMsg.tabPageId] != undefined) {
			if (mainPageId == recvEDataJsonMsg.tabPageId) {
				portList[recvEDataJsonMsg.tabPageId].close(); // Terminates the worker.
				delete portList[recvEDataJsonMsg.tabPageId];
				mainPageId = Object.keys(portList)[0];
			}
			Object.keys(cloudICP.util.callStatusMgr.videoMediaInfoBuf).forEach(function(key) {
				if (undefined != cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["tabPageId"] && recvEDataJsonMsg.tabPageId ==
					cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["tabPageId"]) {
					// 结束该路会话
					var videoCid = cloudICP.util.callStatusMgr.videoMediaInfoBuf[key]["value"]["cid"];
					console.log("video media info buf cid: " + videoCid + " tabPageId: " + recvEDataJsonMsg.tabPageId);

					var requestParam = {
						"cid": String(videoCid),
						"callback": function(data) {console.log(data);}
					};
					cloudICP.dispatch.video.release(requestParam);
				}
			});
		}
	}
	break;
	default:
		break;
	}
	console.log("out portList:"+JSON.stringify(portList));
}


// // 100 鉴权处理
// function authenticationTreatments(recvEDataJsonMsg) {
// 	var operateId = recvEDataJsonMsg.operateId;
// 	var tabPageId = recvEDataJsonMsg.tabPageId;
// 	if (!(operateId >= 100 && operateId < 200) || operateId == undefined) {
// 		console.error("the operateId:", operateId, " is not query operate!")
// 		return;
// 	}
// 	recvEDataJsonMsg.value.callback = function(data) {
// 		//port.postMessage(json2String(data));
// 		sendMsg2Page(tabPageId, 1, json2String(data));
// 	}
// 	treatmentList.auth[operateId].action(recvEDataJsonMsg);
// }

// 100 鉴权处理
function authenticationTreatments(recvEDataJsonMsg, originFuncId) {
	var operateId = recvEDataJsonMsg.operateId;
	var tabPageId = recvEDataJsonMsg.tabPageId;
	if (!(operateId >= 100 && operateId < 200) || operateId == undefined) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}

	treatmentList.auth[operateId].action(recvEDataJsonMsg, originFuncId);
}

// 200语音的所有处理
function voiceTreatments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (!(operateId >= 200 && operateId < 300) || operateId == undefined) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.voice[operateId].action(recvEDataJsonMsg);
}

// 300视频的所有处理
function videoTreaments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (!(operateId >= 300 && operateId < 400) || operateId == undefined) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.video[operateId].action(recvEDataJsonMsg);
}

// 400群组的所有处理
function groupTreatments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (!(operateId >= 400 && operateId < 500) || operateId == undefined) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.group[operateId].action(recvEDataJsonMsg);
}

// 500会议的所有处理
function confTreatments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (!(operateId >= 500 && operateId < 600) || operateId == undefined) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.conf[operateId].action(recvEDataJsonMsg);
}

// 600 短彩信业务
function smsTreatments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (!(operateId >= 600 && operateId < 700) || operateId == undefined) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.sms[operateId].action(recvEDataJsonMsg);
}

// 700 GIS业务
function gisTreaments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (!(operateId >= 700 && operateId < 800) || operateId == undefined) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.gis[operateId].action(recvEDataJsonMsg);
}


// 800查询的所有处理
function queryTreatments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (!(operateId >= 800 && operateId < 900) || operateId == undefined) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.query[operateId].action(recvEDataJsonMsg);
}

// 900媒体的所有处理
function deviceTreatments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (!(operateId >= 900 && operateId < 1000) || operateId == undefined) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.device[operateId].action(recvEDataJsonMsg);
}

// 1001 事件注册
function eventRegisterTreatments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (operateId != 1001) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.event[operateId].action(recvEDataJsonMsg);
}

// 1101 修改或设置mdcip
function ModifyMdcIpTreatments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (!(operateId >= 1100 && operateId < 1200) || operateId == undefined) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.websocket[operateId].action(recvEDataJsonMsg);
}

// 1201 内置接口调用
function UtilTreatments(recvEDataJsonMsg) {
	var operateId = recvEDataJsonMsg.operateId;
	if (operateId != 1201) {
		console.error("the operateId:", operateId, " is not query operate!")
		return;
	}
	treatmentList.util[operateId].action(recvEDataJsonMsg);
}

var treatmentList = {
	auth: {
		"101": {
			interfaces: "unifiedLogin",
			comment: "统一登录",
			action: function(recvEDataJsonMsg, originFuncId) {
				recvEDataJsonMsg.value.callback = function(data) {
					var tabPageId = recvEDataJsonMsg.tabPageId;
					if (data['rsp'] == 0) {
						loginUserId = recvEDataJsonMsg['value']['user']
					}
					var msg = {};
					data["funcId"] = originFuncId;
					if (undefined != cloudICP && true == cloudICP.loginFlag) {
						msg = {
							isdn: loginUserId,
							type: messageType.lifecycle,
							tabPageId: tabPageId,
							value: data,
							msg : "cloudICP is Init, user is login!"
						}
					} else if (undefined != cloudICP) {
						msg = {
							type: messageType.lifecycle,
							tabPageId: tabPageId,
							value: data,
							msg : "cloudICP is Init, user is logout!"
						}
					}
					Object.keys(portList).forEach(function(key) {
						portList[key].postMessage(json2String((msg)));
					});
					// sendMsg2Page(0, messageType.response, data);
				}
				cloudICP.dispatch.auth.unifiedLogin(recvEDataJsonMsg);
			}
		},
		"102": {
			interfaces: "unifiedLogout",
			comment: "统一登出",
			action: function(recvEDataJsonMsg, originFuncId) {
				recvEDataJsonMsg.value.callback = function(data) {
					var tabPageId = recvEDataJsonMsg.tabPageId;
					var msg = {};
					data["funcId"] = originFuncId;
					msg = {
						isdn: "",
						type: messageType.lifecycle,
						tabPageId: tabPageId,
						value: data,
						msg : "user is logout!"
					}
					Object.keys(portList).forEach(function(key) {
						portList[key].postMessage(json2String((msg)));
					});
				}
				cloudICP.dispatch.auth.unifiedLogout(recvEDataJsonMsg);
			}
		},
		"103": {
			interfaces: "unifiPasswordChange",
			comment: "统一修改密码",
			action: function(recvEDataJsonMsg, originFuncId) {
				recvEDataJsonMsg.value.callback = function(data) {
					var tabPageId = recvEDataJsonMsg.tabPageId;
					var msg = {};
					data["funcId"] = originFuncId;
					msg = {
						isdn: "",
						type: messageType.lifecycle,
						tabPageId: tabPageId,
						value: data,
						msg : "mod password success!"
					}
					Object.keys(portList).forEach(function(key) {
						portList[key].postMessage(json2String((msg)));
					});
				}
				cloudICP.dispatch.auth.unifiPasswordChange(recvEDataJsonMsg);
			}
		},
		"104": {
			interfaces: "GetLoginStatus",
			comment: "获取用户登录状态",
			action: function(recvEDataJsonMsg, originFuncId) {
				recvEDataJsonMsg.value.callback = function(data) {
					var tabPageId = recvEDataJsonMsg.tabPageId;
					var msg = {};
					data["funcId"] = originFuncId;
					msg = {
						isdn: "",
						type: messageType.lifecycle,
						tabPageId: tabPageId,
						value: data,
						msg : "GetLoginStatus success!"
					}
					portList[tabPageId].postMessage(json2String((msg)));
				}
				cloudICP.dispatch.auth.GetLoginStatus(recvEDataJsonMsg);
			}
		},
	},
	voice: {
		"201": {
			interfaces: "dial",
			comment: "语音点呼",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.dial(recvEDataJsonMsg);
			}
		},
		"202": {
			interfaces: "answer",
			comment: "语音接听",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.answer(recvEDataJsonMsg);
			}
		},
		"203": {
			interfaces: "reject",
			comment: "语音拒接",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.reject(recvEDataJsonMsg);
			}
		},
		"204": {
			interfaces: "release",
			comment: "语音挂断",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.release(recvEDataJsonMsg);
			}
		},
		"205": {
			interfaces: "intercept",
			comment: "语音抢话",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.intercept(recvEDataJsonMsg);
			}
		},
		"206": {
			interfaces: "hold",
			comment: "保持",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.hold(recvEDataJsonMsg);
			}
		},
		"207": {
			interfaces: "unhold",
			comment: "取保持",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.unhold(recvEDataJsonMsg);
			}
		},
		"208": {
			interfaces: "transfer",
			comment: "点呼人工转接",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.transfer(recvEDataJsonMsg);
			}
		},
		"209": {
			interfaces: "cancelTransfer",
			comment: "点呼人工转接取消",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.cancelTransfer(recvEDataJsonMsg);
			}
		},
		"210": {
			interfaces: "breakOff",
			comment: "点呼强拆",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.breakOff(recvEDataJsonMsg);
			}
		},
		"211": {
			interfaces: "halfDial",
			comment: "半双工点呼发起/抢话",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.halfDial(recvEDataJsonMsg);
			}
		},
		"212": {
			interfaces: "releaseHalfDial",
			comment: "半双工点呼释放",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.releaseHalfDial(recvEDataJsonMsg);
			}
		},
		"213": {
			interfaces: "closeHalfDial",
			comment: "半双工点呼挂断",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.closeHalfDial(recvEDataJsonMsg);
			}
		},
		"214": {
			interfaces: "discreetListen",
			comment: "开启/关闭缜密侦听",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.discreetListen(recvEDataJsonMsg);
			}
		},
		"215": {
			interfaces: "ambienceListen",
			comment: "开启环境侦听",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.ambienceListen(recvEDataJsonMsg);
			}
		},
		"216": {
			interfaces: "subscribeUserStatus",
			comment: "订阅用户",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.subscribeUserStatus(recvEDataJsonMsg);
			}
		},
		"217": {
			interfaces: "unsubscribeUserStatus",
			comment: "取消用户订阅",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.unsubscribeUserStatus(recvEDataJsonMsg);
			}
		},
		"218": {
			interfaces: "dialout",
			comment: "发起PSTN/PLMN 电话呼叫",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.dialout(recvEDataJsonMsg);
			}
		},
		"219": {
			interfaces: "releaseDialout",
			comment: "挂断PSTN/PLMN 电话呼叫",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.releaseDialout(recvEDataJsonMsg);
			}
		},
		"220": {
			interfaces: "voiceBroadcast",
			comment: "发起PSTN/PLMN电话呼叫广播",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.voice.voiceBroadcast(recvEDataJsonMsg);
			}
		},
	},
	video: {
		"301": {
			interfaces: "monitorVideo",
			comment: "视频监控",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.monitorVideo(recvEDataJsonMsg);
			}
		},
		"302":{
			interfaces: "dialVideo",
			comment: "视频点呼",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.dialVideo(recvEDataJsonMsg);
			}
		},
		"303":{
			interfaces: "answerVideo",
			comment: "视频接听",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.answer(recvEDataJsonMsg);
			}
		},
		"304":{
			interfaces: "rejectVideo",
			comment: "视频拒接",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.reject(recvEDataJsonMsg);
			}
		},
		"305":{
			interfaces: "releaseVideo",
			comment: "视频挂断",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.release(recvEDataJsonMsg);
			}
		},
		"306":{
			interfaces: "dispatchVideo",
			comment: "视频分发",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.dispatchVideo(recvEDataJsonMsg);
			}
		},
		"307":{
			interfaces: "cancelVideoDispatch",
			comment: "视频分发取消",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.cancelVideoDispatch(recvEDataJsonMsg);
			}
		},
		"308":{
			interfaces: "startVideoUploadWall",
			comment: "视频上墙",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.startVideoUploadWall(recvEDataJsonMsg);
			}
		},
		"309":{
			interfaces: "stopVideoUploadWall",
			comment: "视频下墙",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.stopVideoUploadWall(recvEDataJsonMsg);
			}
		},
		"310":{
			interfaces: "ptzctrlCamera",
			comment: "PTZ操作",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.ptzctrlCamera(recvEDataJsonMsg);
			}
		},
		"311":{
			interfaces: "setWssflowWinFmttype",
			comment: "改变分辨率",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.video.setWssflowWinFmttype(recvEDataJsonMsg);
			}
		},
	},
	group: {
		"401": {
			interfaces: "addDynamicGroup",
			comment: "动态群组创建",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.addDynamicGroup(recvEDataJsonMsg);
			}
		},
		"402": {
			interfaces: "deleteDynamicGroup",
			comment: "动态群组删除",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.deleteDynamicGroup(recvEDataJsonMsg);
			}
		},
		"403": {
			interfaces: "modifyDynamicGroup",
			comment: "动态群组修改",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.modifyDynamicGroup(recvEDataJsonMsg);
			}
		},
		"404": {
			interfaces: "queryDynamicGroup",
			comment: "查询动态组（移动到了查询分组）",
			action: function(recvEDataJsonMsg) {
				sendMsg2Page(recvEDataJsonMsg.tabPageId, "log", "当前事件未定义")
				// cloudICP.dispatch.query.queryDynamicGroup(recvEDataJsonMsg);
			}
		},
		"405": {
			interfaces: "addPatchGroup",
			comment: "派接组创建",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.addPatchGroup(recvEDataJsonMsg);
			}
		},
		"406": {
			interfaces: "deletePatchGroup",
			comment: "派接组删除",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.deletePatchGroup(recvEDataJsonMsg);
			}
		},
		"407": {
			interfaces: "addPatchGroupMember",
			comment: "添加派接组成员",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.addPatchGroupMember(recvEDataJsonMsg);
			}
		},
		"408": {
			interfaces: "deletePatchGroupMember",
			comment: "删除派接组成员",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.deletePatchGroupMember(recvEDataJsonMsg);
			}
		},
		"409": {
			interfaces: "queryPatchGroup",
			comment: "查询派接组（移动到了查询分组）",
			action: function(recvEDataJsonMsg) {
				sendMsg2Page(recvEDataJsonMsg.tabPageId, "log", "当前事件未定义")
				// cloudICP.dispatch.query.queryPatchGroup(recvEDataJsonMsg);
			}
		},
		"410": {
			interfaces: "queryPatchGroupMembers",
			comment: "查询派接组成员（移动到了查询分组）",
			action: function(recvEDataJsonMsg) {
				sendMsg2Page(recvEDataJsonMsg.tabPageId, "log", "当前事件未定义")
				// cloudICP.dispatch.query.queryPatchGroupMembers(recvEDataJsonMsg);
			}
		},
		"411": {
			interfaces: "subscribeTalkingGroup",
			comment: "订阅群组",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.subscribeTalkingGroup(recvEDataJsonMsg);
			}
		},
		"412": {
			interfaces: "unsubscribeTalkingGroup",
			comment: "去订阅群组",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.unsubscribeTalkingGroup(recvEDataJsonMsg);
			}
		},
		"413": {
			interfaces: "joinTalkingGroup",
			comment: "加入组呼",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.joinTalkingGroup(recvEDataJsonMsg);
			}
		},
		"414": {
			interfaces: "subjoinTalkingGroup",
			comment: "订阅并自动加入组呼",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.subjoinTalkingGroup(recvEDataJsonMsg);
			}
		},
		"415": {
			interfaces: "pttTalkingGroup",
			comment: "组呼发起或抢权",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.pttTalkingGroup(recvEDataJsonMsg);
			}
		},
		"416": {
			interfaces: "pttreleaseTalkingGroup",
			comment: "组呼放权",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.pttreleaseTalkingGroup(recvEDataJsonMsg);
			}
		},
		"417": {
			interfaces: "leaveTalkingGroup",
			comment: "退出组呼",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.leaveTalkingGroup(recvEDataJsonMsg);
			}
		},
		"418": {
			interfaces: "pttTalkingGroupEmergencyCall",
			comment: "紧急组呼",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.pttTalkingGroupEmergencyCall(recvEDataJsonMsg);
			}
		},
		"419": {
			interfaces: "breakoffTalkingGroup",
			comment: "组呼强拆",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.breakoffTalkingGroup(recvEDataJsonMsg);
			}
		},
		"420": {
			interfaces: "addTalkingGroupTempUser",
			comment: "组呼添加临时用户",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.addTalkingGroupTempUser(recvEDataJsonMsg);
			}
		},
		"421": {
			interfaces: "breakinTalkingGroup",
			comment: "组呼强插",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.breakinTalkingGroup(recvEDataJsonMsg);
			}
		},
		"422": {
			interfaces: "getPDTGroupInfo",
			comment: "查询PDT用户当前组",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.getPDTGroupInfo(recvEDataJsonMsg);
			}
		},
		"423": {
			interfaces: "setListenGroup",
			comment: "设置监听组",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.group.setListenGroup(recvEDataJsonMsg);
			}
		},
	},
	conf: {
		"501": {
			interfaces: "createConf",
			comment: "创建音视频会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.createConf(recvEDataJsonMsg);
			}
		},
		"502": {
			interfaces: "joinConf",
			comment: "主动加入音视频会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.joinConf(recvEDataJsonMsg);
			}
		},
		"503": {
			interfaces: "acceptAudioConf",
			comment: "音频被动入会",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.acceptAudioConf(recvEDataJsonMsg);
			}
		},
		"504": {
			interfaces: "acceptVideoConf",
			comment: "视频被动入会",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.acceptVideoConf(recvEDataJsonMsg);
			}
		},
		"505": {
			interfaces: "rejectAudioConf",
			comment: "音频拒接入会",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.rejectAudioConf(recvEDataJsonMsg);
			}
		},
		"506": {
			interfaces: "rejectVideoConf",
			comment: "视频拒接入会",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.rejectVideoConf(recvEDataJsonMsg);
			}
		},
		"507": {
			interfaces: "exitAudioConf",
			comment: "离开音频会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.exitAudioConf(recvEDataJsonMsg);
			}
		},
		"508": {
			interfaces: "exitVideoConf",
			comment: "离开视频会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.exitVideoConf(recvEDataJsonMsg);
			}
		},
		"509": {
			interfaces: "addConfMembers",
			comment: "增加与会成员",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.addConfMembers(recvEDataJsonMsg);
			}
		},
		"510": {
			interfaces: "hangupConfMember",
			comment: "挂断成员",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.hangupConfMember(recvEDataJsonMsg);
			}
		},
		"511": {
			interfaces: "callConfMember",
			comment: "重呼成员",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.callConfMember(recvEDataJsonMsg);
			}
		},
		"512": {
			interfaces: "endConf",
			comment: "结束会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.endConf(recvEDataJsonMsg);
			}
		},
		"513": {
			interfaces: "muteConfMember",
			comment: "静音会议成员",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.muteConfMember(recvEDataJsonMsg);
			}
		},
		"514": {
			interfaces: "muteConf",
			comment: "静音会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.muteConf(recvEDataJsonMsg);
			}
		},
		"515": {
			interfaces: "holdConf",
			comment: "保持会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.holdConf(recvEDataJsonMsg);
			}
		},
		"516": {
			interfaces: "unholdConf",
			comment: "取消保持会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.unholdConf(recvEDataJsonMsg);
			}
		},
		"517": {
			interfaces: "watchConfMember",
			comment: "选看与会者",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.watchConfMember(recvEDataJsonMsg);
			}
		},
		"518": {
			interfaces: "watchMixPicture",
			comment: "选看多画面",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.watchMixPicture(recvEDataJsonMsg);
			}
		},
		"519": {
			interfaces: "broadcastConfMember",
			comment: "广播与会者",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.broadcastConfMember(recvEDataJsonMsg);
			}
		},
		"520": {
			interfaces: "broadcastMixPicture",
			comment: "广播多画面",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.broadcastMixPicture(recvEDataJsonMsg);
			}
		},
		"521": {
			interfaces: "cancelBroadcastConfMember",
			comment: "取消广播与会者",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.cancelBroadcastConfMember(recvEDataJsonMsg);
			}
		},
		"522": {
			interfaces: "cancelBroadcastMixPicture",
			comment: "取消广播多画面",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.cancelBroadcastMixPicture(recvEDataJsonMsg);
			}
		},
		"523": {
			interfaces: "startConfUploadWall",
			comment: "开始会议上墙",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.startConfUploadWall(recvEDataJsonMsg);
			}
		},
		"524": {
			interfaces: "stopConfUploadWall",
			comment: "停止会议上墙",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.stopConfUploadWall(recvEDataJsonMsg);
			}
		},
		"525": {
			interfaces: "queryConfListByAttendee",
			comment: "查看调度员所在的会议信息",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryConfListByAttendee(recvEDataJsonMsg);
			}
		},
		"526": {
			interfaces: "queryRecordURLByConfID",
			comment: "会议录制信息查询",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryRecordURLByConfID(recvEDataJsonMsg);
			}
		},
		"527": {
			interfaces: "queryVWallDisplayMatrixInfo",
			comment: "查询大屏上墙显示模式",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryVWallDisplayMatrixInfo(recvEDataJsonMsg);
			}
		},
		"528": {
			interfaces: "queryConfWallInfo",
			comment: "查询大屏上墙图层",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryConfWallInfo(recvEDataJsonMsg);
			}
		},
		"529": {
			interfaces: "setChairmanName",
			comment: "设置主席名字",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.setChairmanName(recvEDataJsonMsg);
			}
		},
		"530": {
			interfaces: "setSpokenMember",
			comment: "指定发言方",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.setSpokenMember(recvEDataJsonMsg);
			}
		},
		"531": {
			interfaces: "delConfMember",
			comment: "删除成员",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.delConfMember(recvEDataJsonMsg);
			}
		},
		"532": {
			interfaces: "proxyFloorGroup",
			comment: "群组用户话权代理",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.proxyFloorGroup(recvEDataJsonMsg);
			}
		},
		"533": {
			interfaces: "proxyFloorGateway",
			comment: "网关用户话权代理",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.proxyFloorGateway(recvEDataJsonMsg);
			}
		},
		"534": {
			interfaces: "queryContinuousPresenceInfo",
			comment: "查询多画面信息",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryContinuousPresenceInfo(recvEDataJsonMsg);
			}
		},
		"535": {
			interfaces: "acceptApplyFloor",
			comment: "接受举手申请",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.acceptApplyFloor(recvEDataJsonMsg);
			}
		},
		"536": {
			interfaces: "rejectApplyFloor",
			comment: "拒绝举手申请",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.rejectApplyFloor(recvEDataJsonMsg);
			}
		},
		"537": {
			interfaces: "applyFloor",
			comment: "举手申请",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.applyFloor(recvEDataJsonMsg);
			}
		},
		"538": {
			interfaces: "setChairman",
			comment: "切换会议主席",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.setChairman(recvEDataJsonMsg);
			}
		},
		"539": {
			interfaces: "prolongConfTime",
			comment: "延长会议时间",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.prolongConfTime(recvEDataJsonMsg);
			}
		},
		"540": {
			interfaces: "queryVDCAddressBook",
			comment: "查询VDC通讯录",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryVDCAddressBook(recvEDataJsonMsg);
			}
		},
		"541": {
			interfaces: "queryConferenceList",
			comment: "查询会议列表",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryConferenceList(recvEDataJsonMsg);
			}
		},
		"542": {
			interfaces: "queryConferenceInfo",
			comment: "查询指定会议ID信息",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryConferenceInfo(recvEDataJsonMsg);
			}
		},
		"543": {
			interfaces: "queryConfereeAddressBook",
			comment: "查询与会人通讯录",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryConfereeAddressBook(recvEDataJsonMsg);
			}
		},
		"544": {
			interfaces: "lockVideoSource",
			comment: "锁定视频源",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.lockVideoSource(recvEDataJsonMsg);
			}
		},
		"545": {
			interfaces: "lockConf",
			comment: "锁定会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.lockConf(recvEDataJsonMsg);
			}
		},
		"546": {
			interfaces: "queryConferenceById",
			comment: "根据外部会议ID查询内部会议ID",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryConferenceById(recvEDataJsonMsg);
			}
		},
		"547": {
			interfaces: "modConf",
			comment: "修改会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.modConf(recvEDataJsonMsg);
			}
		},
		"548": {
			interfaces: "delConf",
			comment: "取消预约会议",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.delConf(recvEDataJsonMsg);
			}
		},
        "549": {
			interfaces: "queryCasConfInfos",
			comment: "查询级联会议结构信息",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryCasConfInfos(recvEDataJsonMsg);
			}
		},
        "550": {
			interfaces: "queryOrgTree",
			comment: "查询组织树",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryOrgTree(recvEDataJsonMsg);
			}
		},
		"551": {
			interfaces: "queryAreaOrgInfo",
			comment: "查询SMC信息",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryAreaOrgInfo(recvEDataJsonMsg);
			}
		},
		"552": {
			interfaces: "queryConfURL",
			comment: "查询会议入会链接",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.queryConfURL(recvEDataJsonMsg);
			}
		},
		"553": {
			interfaces: "setTextTips",
			comment: "设置会议字幕横幅",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.conf.setTextTips(recvEDataJsonMsg);
			}
		},
	},
	sms: {
		"601": {
			interfaces: "sendDispSMS",
			comment: "发送短信",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.sms.sendDispSMS(recvEDataJsonMsg.value);
			}
		},
		"602": {
			interfaces: "sendDispMMS",
			comment: "发送彩信",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.sms.sendDispMMS(recvEDataJsonMsg.value);
			}
		},
	},
	gis: {
		"701": {
			interfaces: "subscribeGIS",
			comment: "GIS订阅",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.gis.subscribeGIS(recvEDataJsonMsg);
			}
		},
		"702": {
			interfaces: "unsubscribeGIS",
			comment: "GIS取消订阅",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.gis.unsubscribeGIS(recvEDataJsonMsg);
			}
		},
		"703": {
			interfaces: "queryGISSubList",
			comment: "GIS订阅终端列表查询",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.gis.queryGISSubList(recvEDataJsonMsg);
			}
		},
	},
	query: {
		"801": {
			interfaces: "queryTalkingGroup",
			comment: "查询群组（过时）",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryTalkingGroup(recvEDataJsonMsg);
			}
		},
		"802": {
			interfaces: "queryTalkingGroupV1",
			comment: "查询群组",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryTalkingGroupV1(recvEDataJsonMsg);
			}
		},
		"803": {
			interfaces: "queryTalkingGroupMembers",
			comment: "查询群组成员（过时）",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryTalkingGroupMembers(recvEDataJsonMsg);
			}
		},
		"804": {
			interfaces: "queryTalkingGroupMembersV1",
			comment: "查询群组成员",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryTalkingGroupMembersV1(recvEDataJsonMsg);
			}
		},
		"805": {
			interfaces: "queryDynamicGroupMembers",
			comment: "查询动态组成员",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryDynamicGroupMembers(recvEDataJsonMsg);
			}
		},
		"806": {
			interfaces: "queryStaticGroup",
			comment: "查询静态组（过时）",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryStaticGroup(recvEDataJsonMsg);
			}
		},
		"807": {
			interfaces: "queryStaticGroupV1",
			comment: "查询静态组",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryStaticGroupV1(recvEDataJsonMsg);
			}
		},
		"808": {
			interfaces: "queryUserList",
			comment: "查询用户（过时）",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryUserList(recvEDataJsonMsg);
			}
		},
		"809": {
			interfaces: "queryUserListV1",
			comment: "查询用户",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryUserListV1(recvEDataJsonMsg);
			}
		},
		"810": {
			interfaces: "queryCameras",
			comment: "查询摄像头",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryCameras(recvEDataJsonMsg);
			}
		},
		"811": {
			interfaces: "querySpecifiedLevelCamera",
			comment: "查询指定层级的摄像头",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.querySpecifiedLevelCamera(recvEDataJsonMsg);
			}
		},
		"812": {
			interfaces: "queryDecoder",
			comment: "查询解码器",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryDecoder(recvEDataJsonMsg);
			}
		},
		"813": {
			interfaces: "queryDepartmentList",
			comment: "查询部门",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryDepartmentList(recvEDataJsonMsg);
			}
		},
		"814": {
			interfaces: "queryCameraLevel",
			comment: "查询摄像头层级",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryCameraLevel(recvEDataJsonMsg);
			}
		},
		"815": {
			interfaces: "queryCameraLevelPermission",
			comment: "查询摄像头层级权限",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryCameraLevelPermission(recvEDataJsonMsg);
			}
		},
		"816": {
			interfaces: "queryTalkingGroupInfo",
			comment: "查询群组属性",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryTalkingGroupInfo(recvEDataJsonMsg);
			}
		},
		"817": {
			interfaces: "queryPatchGroupInfo",
			comment: "查询派接组属性",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryPatchGroupInfo(recvEDataJsonMsg);
			}
		},
		"818": {
			interfaces: "queryTalkingGroupPatchedInfo",
			comment: "查询群组被派接属性",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryTalkingGroupPatchedInfo(recvEDataJsonMsg);
			}
		},
		"819": {
			interfaces: "queryUserInfo",
			comment: "查询用户属性",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryUserInfo(recvEDataJsonMsg);
			}
		},
		"820": {
			interfaces: "queryCameraInfo",
			comment: "查询摄像头属性",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryCameraInfo(recvEDataJsonMsg);
			}
		},
		"821": {
			interfaces: "queryCameraPermissionInfo",
			comment: "查询摄像头权限开关",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryCameraPermissionInfo(recvEDataJsonMsg);
			}
		},
		"822": {
			interfaces: "queryCallInfo",
			comment: "查询视频呼叫信息",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryCallInfo(recvEDataJsonMsg);
			}
		},
		"823": {
			interfaces: "queryDCInfo",
			comment: "查询调度员属性",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryDCInfo(recvEDataJsonMsg);
			}
		},
		"824": {
			interfaces: "queryUEInfo",
			comment: "查询终端属性",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryUEInfo(recvEDataJsonMsg);
			}
		},
		"825": {
			interfaces: "queryUEGisInfo",
			comment: "查询终端GIS配置",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryUEGisInfo(recvEDataJsonMsg);
			}
		},
		"826": {
			interfaces: "queryRecord",
			comment: "查询录音录像文件信息",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryRecord(recvEDataJsonMsg);
			}
		},
		"827": {
			interfaces: "queryCameraGPSInfo",
			comment: "获取所有摄像头GPS信息",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryCameraGPSInfo(recvEDataJsonMsg);
			}
		},
		"828": {
			interfaces: "queryMRS",
			comment: "查询录音录像服务器",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryMRS(recvEDataJsonMsg);
			}
		},
		"829": {
			interfaces: "queryDeviceIDRelationship",
			comment: "查询GBID和ISDN对应关系",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryDeviceIDRelationship(recvEDataJsonMsg);
			}
		},
		"830": {
			interfaces: "queryDynamicGroup",
			comment: "查询动态组",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryDynamicGroup(recvEDataJsonMsg);
			}
		},
		"831": {
			interfaces: "queryPatchGroup",
			comment: "查询派接组",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryPatchGroup(recvEDataJsonMsg);
			}
		},
		"832": {
			interfaces: "queryPatchGroupMembers",
			comment: "查询派接组成员",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryPatchGroupMembers(recvEDataJsonMsg);
			}
		},
		"833": {
			interfaces: "queryCameras",
			comment: "查询摄像头",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryCamerasV1(recvEDataJsonMsg);
			}
		},
		"834": {
			interfaces: "queryGISTrack",
			comment: "查询历史轨迹",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.query.queryGISTrack(recvEDataJsonMsg);
			}
		},
	},
	device: {
		"901": {
			interfaces: "getSoundDevice",
			comment: "查询扬声器和麦克风列表",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.getSoundDevice(recvEDataJsonMsg);
			}
		},
		"902": {
			interfaces: "getVolume",
			comment: "查询扬声器音量",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.getVolume(recvEDataJsonMsg);
			}
		},
		"903": {
			interfaces: "setVolume",
			comment: "设置扬声器音量",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.setVolume(recvEDataJsonMsg);
			}
		},
		"904": {
			interfaces: "muteSpeaker",
			comment: "静音扬声器",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.muteSpeaker(recvEDataJsonMsg);
			}
		},
		"905": {
			interfaces: "unmuteSpeaker",
			comment: "取消静音扬声器",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.unmuteSpeaker(recvEDataJsonMsg);
			}
		},
		"906": {
			interfaces: "muteMic",
			comment: "静音麦克风",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.muteMic(recvEDataJsonMsg);
			}
		},
		"907": {
			interfaces: "unmuteMic",
			comment: "取消静音麦克风",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.unmuteMic(recvEDataJsonMsg);
			}
		},
		"908": {
			interfaces: "querySoundDevice",
			comment: "查询声卡设备",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.querySoundDevice(recvEDataJsonMsg);
			}
		},
		"909": {
			interfaces: "assignSoundDevice",
			comment: "切换声卡设备",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.assignSoundDevice(recvEDataJsonMsg);
			}
		},
		"910": {
			interfaces: "sendDTMF",
			comment: "二次拨号",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.sendDTMF(recvEDataJsonMsg);
			}
		},
		"911": {
			interfaces: "queryCameraAbility",
			comment: "查询本地摄像头能力",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.queryCameraAbility(recvEDataJsonMsg);
			}
		},
		"912": {
			interfaces: "stopPlayTone",
			comment: "停止播放放音",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.stopPlayTone(recvEDataJsonMsg);
			}
		},
		"913": {
			interfaces: "getVersion",
			comment: "版本号查询",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.getVersion(recvEDataJsonMsg);
			}
		},
		"914": {
			interfaces: "initMRS",
			comment: "初始化录音录像服务器",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.initMRS(recvEDataJsonMsg);
			}
		},
		"915": {
			interfaces: "forceInitMSP",
			comment: "强制初始化MSP",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.forceInitMSP(recvEDataJsonMsg);
			}
		},
		"916": {
			interfaces: "hideVideoWindow",
			comment: "窗口隐藏",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.hideVideoWindow(recvEDataJsonMsg);
			}
		},
		"917": {
			interfaces: "windowOnTop",
			comment: "窗口置顶",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.windowOnTop(recvEDataJsonMsg);
			}
		},
		"918": {
			interfaces: "windowDragEnable",
			comment: "窗口可拖动",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.windowDragEnable(recvEDataJsonMsg);
			}
		},
		"919": {
			interfaces: "showWindowBorder",
			comment: "窗口显示标题栏",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.showWindowBorder(recvEDataJsonMsg);
			}
		},
		"920": {
			interfaces: "setWindowInfo",
			comment: "修改窗口位置和大小",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.setWindowInfo(recvEDataJsonMsg);
			}
		},
		"921": {
			interfaces: "getResolution",
			comment: "获取屏幕分辨率",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.getResolution(recvEDataJsonMsg);
			}
		},
		"922": {
			interfaces: "snapshot",
			comment: "截图",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.snapshot(recvEDataJsonMsg);
			}
		},
		"923": {
			interfaces: "startLocalRecord",
			comment: "开始本地录音录像",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.startLocalRecord(recvEDataJsonMsg);
			}
		},
		"924": {
			interfaces: "stopLocalRecord",
			comment: "关闭本地录音录像",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.stopLocalRecord(recvEDataJsonMsg);
			}
		},
		"925": {
			interfaces: "playAudioShortMsg",
			comment: "语音短消息生成amr文件",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.playAudioShortMsg(recvEDataJsonMsg);
			}
		},
		"926": {
			interfaces: "createWindow",
			comment: "创建用户视频窗口",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.createWindow(recvEDataJsonMsg);
			}
		},
		"927": {
			interfaces: "destroyWindow",
			comment: "释放用户视频窗口",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.destroyWindow(recvEDataJsonMsg);
			}
		},
		"928": {
			interfaces: "ringType",
			comment: "振铃开关",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.ringType(recvEDataJsonMsg);
			}
		},
		"929": {
			interfaces: "setLinkedPhone",
			comment: "绑定硬话机",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.setLinkedPhone(recvEDataJsonMsg);
			}
		},
		"930": {
			interfaces: "setRingVolume",
			comment: "设置振铃音量",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.setRingVolume(recvEDataJsonMsg);
			}
		},
		"931": {
			interfaces: "muteGroup",
			comment: "静音群组",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.muteGroup(recvEDataJsonMsg);
			}
		},
		"932": {
			interfaces: "unmuteGroup",
			comment: "取消静音群组",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.unmuteGroup(recvEDataJsonMsg);
			}
		},
		"933": {
			interfaces: "setGroupVolume",
			comment: "设置群组扬声器音量",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.setGroupVolume(recvEDataJsonMsg);
			}
		},
		"934": {
			interfaces: "getMspVersion",
			comment: "获取MSP版本号",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.getMspVersion(recvEDataJsonMsg);
			}
		},
		"935": {
			interfaces: "checkMSPIsInstall",
			comment: "获取MSP版本号",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.checkMSPIsInstall(recvEDataJsonMsg);
			}
		},
		"936": {
			interfaces: "setMediaMode",
			comment: "设置媒体透传开关",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.setMediaMode(recvEDataJsonMsg);
			}
		},
		"937": {
			interfaces: "UpgradeMspVersion",
			comment: "MSP版本升级或回退",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.UpgradeMspVersion(recvEDataJsonMsg);
			}
		},
		"938": {
			interfaces: "setLocalRecordParam",
			comment: "设置本地录音录像位置",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.setLocalRecordParam(recvEDataJsonMsg);
			}
		},
		"939": {
			interfaces: "startWindowShare",
			comment: "开始共享桌面",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.startWindowShare(recvEDataJsonMsg);
			}
		},
		"940": {
			interfaces: "stopWindowShare",
			comment: "停止共享桌面",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.device.stopWindowShare(recvEDataJsonMsg);
			}
		}
	},
	event: {
		"1001": {
			interfaces: "register",
			comment: "事件注册",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.event.register(recvEDataJsonMsg);
			}
		}
	},
	websocket: {
		"1101": {
			interfaces: "ModifyMdcIp",
			comment: "修改或设置MDC的IP地址",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.webSocket.ModifyMdcIp(recvEDataJsonMsg);
			}
		},
		"1102": {
			interfaces: "getLocalIp",
			comment: "获取本地IP地址",
			action: function(recvEDataJsonMsg) {
				cloudICP.dispatch.webSocket.getLocalIp(recvEDataJsonMsg);
			}
		},
	},
	util: {
		"1201": {
			interfaces: "closeWssflowWindow",
			comment: "关闭内嵌模式窗口",
			action: function(recvEDataJsonMsg) {
				cloudICP.util.closeWssflowWindow(recvEDataJsonMsg);
			}
		},
	},
}
