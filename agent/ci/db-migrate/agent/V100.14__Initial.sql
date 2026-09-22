alter table ai_agent_config
    add column param_script longtext null default null;
alter table ai_agent_config
    add column resp_script longtext null default null;

update ai_agent_config
set category_ids = 1979064194893023233,
    `desc`       = '输入人员信息进行查询',
    avatar       = '/images/default-avatar.png',
    priority     = 0
where id = 1;

INSERT INTO ai_agent_config (id, name, url, token, priority, avatar, creator, updater, create_time, update_time,
                             deleted, `desc`, type, category_ids, is_restricted, param_script, resp_script)
VALUES (2, '法治', 'http://20.216.1.131:80/v1/chat-messages', 'app-qqWFzYkXJXuGpNFDavCGJPT0', 2,
        '/images/default-avatar.png', null, null, now(), now(), 0, '法治', 1,
        '1979064194893023233', 0, 'Result = function(p,c){
                    return {
                        "inputs": {},
                        "query": p.trim(),
                        "response_mode": "streaming",
                        "conversation_id": "",
                        "user": "鼎桥"
                    }
                }(Params,Config)', 'Result = function(p,c){
                if(!p.startsWith("data:")){
                    console.log("not started with data:")
                    return null
                }
                p=p.substring(5)
                var pj=JSON.parse(p)
                var conf=JSON.parse(c)
                if(pj.event=="message"){
                    return {
                        "event": "message",
                        "answer": pj.answer
                    }
                }else if(pj.event=="workflow_finished"){
                    return {
                        "event": "message_end",
                        "status": pj.data.status,
                        "error": pj.data.error
                    }
                }else{
                    console.log("unknown data node "+pj.event)
                    return null;
                }
            }(Params,Config)');
INSERT INTO ai_agent_config (id, name, url, token, priority, avatar, creator, updater, create_time, update_time,
                             deleted, `desc`, type, category_ids, is_restricted, param_script, resp_script)
VALUES (3, '智能问答', 'http://20.216.1.131:80/v1/chat-messages', 'app-0Ayk2OSzOYMJ1licNl43o0Ny', 2,
        '/images/default-avatar.png', null, null, now(), now(), 0, '智能问答', 0,
        '1979064194893023233', 0, 'Result = function(p,c){
                    return {
                        "inputs": {},
                        "query": p.trim(),
                        "response_mode": "streaming",
                        "conversation_id": "",
                        "user": "鼎桥"
                    }
                }(Params,Config)', 'Result = function(p,c){
                if(!p.startsWith("data:")){
                    console.log("not started with data:")
                    return null
                }
                p=p.substring(5)
                var pj=JSON.parse(p)
                var conf=JSON.parse(c)
                if(pj.event=="message"){
                    return {
                        "event": "message",
                        "answer": pj.answer
                    }
                }else if(pj.event=="workflow_finished"){
                    return {
                        "event": "message_end",
                        "status": pj.data.status,
                        "error": pj.data.error
                    }
                }else{
                    console.log("unknown data node "+pj.event)
                    return null;
                }
            }(Params,Config)');
INSERT INTO ai_agent_config (id, name, url, token, priority, avatar, creator, updater, create_time, update_time,
                             deleted, `desc`, type, category_ids, is_restricted, param_script, resp_script)
VALUES (4, '查询人员', 'http://20.216.1.131:80/v1/chat-messages', 'app-9c3dxmRFFXZtBFHlxca54MRe', 2,
        '/images/default-avatar.png', null, null, now(), now(), 0, '查询人员', 1,
        '1979064194893023233', 0, 'Result = function(p,c){
                    var format = function(n) {
                        var year = n.getFullYear();
                        var month = ("0" + (n.getMonth() + 1)).slice(-2);
                        var date = ("0" + n.getDate()).slice(-2);
                        var hours = ("0" + n.getHours()).slice(-2);
                        var minutes = ("0" + n.getMinutes()).slice(-2);
                        var seconds = ("0" + n.getSeconds()).slice(-2);
                        return year + "-" + month + "-" + date + " " + hours + ":" + minutes + ":" + seconds;
                    }
                    var startDate=new Date(Date.now()-7*24*60*60*1000)
                    return {
                        "inputs": {
                            "id_card": p.trim(),
                            "start_date": format(startDate),
                            "end_date": format(new Date())
                        },
                        "response_mode": "streaming",
                        "user": "dingqiao"
                    }
                }(Params,Config)', 'Result = function(p,c){
                    if(!p.trim().startsWith("data:")){
                        console.log("not started with data:")
                        return null
                    }
                    p=p.substring(5)
                    var pj=JSON.parse(p.trim())
                    if(pj.event=="workflow_finished"){
                        return {
                            "event": "message",
                            "answer": pj.data.outputs.body+"[end]"
                        }
                    }else{
                        console.log("unknown data node "+pj.event)
                        return null;
                    }
                }(Params,Config)');
