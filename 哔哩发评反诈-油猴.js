// ==UserScript==
// @name         哔哩发评反诈
// @namespace    http://tampermonkey.net/
// @version      3.5
// @description  评论发送后自动检测状态，避免被发送成功的谎言所欺骗！
// @author       freedom-introvert & ChatGPT
// @match        https://*.bilibili.com/*
// @run-at       document-idle
// @grant        GM.xmlHttpRequest
// @license      GPL
// @downloadURL https://update.greasyfork.org/scripts/496537/%E5%93%94%E5%93%A9%E5%8F%91%E8%AF%84%E5%8F%8D%E8%AF%88.user.js
// @updateURL https://update.greasyfork.org/scripts/496537/%E5%93%94%E5%93%A9%E5%8F%91%E8%AF%84%E5%8F%8D%E8%AF%88.meta.js
// ==/UserScript==

const waitTime = 5000;//评论发送后的等待时间，单位毫秒，可修改此项，不建议低于5秒

const sortByTime = 0;
const SORT_MODE_TIME = 2;

const originalFetch = unsafeWindow.fetch;//注意是unsafeWindow，不是window，使用 GM.xmlHttpRequest 换掉window里的fecth将不起作用

// Replace the fetch function with a custom one
unsafeWindow.fetch = async function (...args) {
    // Call the original fetch function and wait for the response
    var response = await originalFetch.apply(this, args);

    // Clone the response to read its content without altering the original response
    var clonedResponse = response.clone();

    // Read the response content as text
    clonedResponse.text().then(content => {
        // Log the URL of the fetch request to the console
        var url = args[0];
        //console.log('Fetch request URL:', url);
        // Log the response content to the console
        //console.log('Fetch response content:', content);
        if (url.startsWith("//api.bilibili.com/x/v2/reply/add")) {
            handleAddCommentResponse(url, JSON.parse(content));
        }
    });

    // Return the original response so that the fetch call continues to work as normal
    return response;
};

//动态shadowBan检测
window.onload = function () {
    const currentURL = window.location.href;
    const hostname = window.location.hostname;
    let id = null;

    if (hostname === 't.bilibili.com') {
        // 提取 t.bilibili.com URL 中的数字部分
        const urlPath = window.location.pathname;
        id = urlPath.split('/')[1];
    } else if (hostname === 'www.bilibili.com') {
        // 提取 www.bilibili.com/opus URL 中的数字部分
        const urlPath = window.location.pathname;
        const pathParts = urlPath.split('/');
        if (pathParts[1] === 'opus') {
            id = pathParts[2];
        }
    }

    if (id) {
        console.log('Dynamic ID:', id);
        handleCheckDynamic(id);
    }

}

console.log(window.fetch)
console.log("反诈脚本已加载")

//
var dialogHTML = `
        <style>
        #progress-overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0, 0, 0, 0.5);
            z-index: 999;
        }

        #progress-title{
            display: block;
            font-size: 19px;
            margin-block-start: 0.5em;
            margin-block-end: 1em;
            margin-inline-start: 0px;
            margin-inline-end: 0px;
            font-weight: bold;
            unicode-bidi: isolate;
        }

        #progress-message{
            display: block;
            font-size: 16px;
            margin-block-start: 1em;
            margin-block-end: 1em;
            margin-inline-start: 0px;
            margin-inline-end: 0px;
            unicode-bidi: isolate;
        }

        #progress-dialog {
            display: none;
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 60%;
            padding: 20px;
            background-color: #fff;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
            border-radius: 3px;
            z-index: 1000;
        }

        #progress-bar-container {
            width: 100%;
            height: 5px;
            margin-top: 30px;
            background-color: #ddd;
            overflow: hidden;
            position: relative;
            margin-bottom: 20px;
        }

        .progress-bar {
            width: 0;
            height: 20px;
            background-color: #FB7299;
            text-align: center;
            color: white;
            line-height: 20px;
            
        }


        /* 不确定进度的线性进度条 摘抄自mdui*/
        .progress-bar-indeterminate {
            background-color: #FB7299;

            &::before {
                position: absolute;
                top: 0;
                bottom: 0;
                left: 0;
                background-color: inherit;
                animation: mdui-progress-indeterminate 2s linear infinite;
                content: ' ';
                will-change: left, width;
            }

            &::after {
                position: absolute;
                top: 0;
                bottom: 0;
                left: 0;
                background-color: inherit;
                animation: mdui-progress-indeterminate-short 2s linear infinite;
                content: ' ';
                will-change: left, width;
            }
        }

        @keyframes mdui-progress-indeterminate {
            0% {
                left: 0;
                width: 0;
            }

            50% {
                left: 30%;
                width: 70%;
            }

            75% {
                left: 100%;
                width: 0;
            }
        }

        @keyframes mdui-progress-indeterminate-short {
            0% {
                left: 0;
                width: 0;
            }

            50% {
                left: 0;
                width: 0;
            }

            75% {
                left: 0;
                width: 25%;
            }

            100% {
                left: 100%;
                width: 0;
            }
        }

        #close-button {
            display: inline-block;
            margin-top: 20px;
            padding: 10px 15px;
            background-color: #fff;
            color: #FB7299;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-align: center;
            font-size: 14px;
            font-weight: bold;
            float: right;
            transition: all 0.2s;
        }

        #close-button:hover {
            background-color: #F0F0F0;
        }

        .shadowban-scanner-message {
            --message-background-color: rgb(255, 0, 0, 0.2);
            color: var(--md-sys-color-on-primary);
            padding: 1em;
            border-radius: 0.5em;
            background: var(--message-background-color);
            margin: 1em 0px 0px;
        }

        </style>
        <div id="progress-overlay"></div>
        <div id="progress-dialog">
            <h3 id="progress-title">Progress</h3>
            <p id="progress-message"></p>
            <div id="progress-bar-container">
                <div id="progressBar" class="progress-bar"></div>
            </div>
            <button id="close-button">关闭</button>
        </div>
        `
document.body.insertAdjacentHTML('beforeend', dialogHTML);

const ProgressDialog = {
    show: function () {
        document.getElementById('progress-overlay').style.display = 'block';
        document.getElementById('progress-dialog').style.display = 'block';
    },
    hide: function () {
        document.getElementById('progress-overlay').style.display = 'none';
        document.getElementById('progress-dialog').style.display = 'none';
    },
    setTitle: function (title) {
        document.getElementById('progress-title').textContent = title;
    },
    setMessage: function (message) {
        document.getElementById('progress-message').innerText = message;
    },
    setProgress: function (progress) {
        const progressBar = document.getElementById('progressBar');
        progressBar.style.width = progress + '%';
    },
    setIndeterminate: function (indeterminate) {
        var progressBar = document.getElementById('progressBar');
        if (indeterminate) {
            progressBar.className = "progress-bar-indeterminate";
            progressBar.style.width = "30%"
        } else {
            progressBar.className = "progress-bar";
            progressBar.style.width = "0"
        }
    }
};

document.getElementById('close-button').addEventListener('click', function () {
    ProgressDialog.hide();
});

function sleep(time) {
    return new Promise((resolve) => setTimeout(resolve, time));
}

async function handleAddCommentResponse(url, responseJson) {
    console.log(url);
    console.log(responseJson);
    console.log(responseJson.code);
    if (responseJson.code == 0) {
        var data = responseJson.data;
        var reply = data.reply;

        var oid = reply.oid;
        var type = reply.type;
        var rpid = reply.rpid;
        var root = reply.root;

        console.log(`${data.success_toast}，准备检查评论`);
        ProgressDialog.show();
        await sleepAndShowInDialog(waitTime);
        ProgressDialog.setIndeterminate(true);
        ProgressDialog.setTitle("检查中……");
        //如果root==0，这是在评论区的根评论，否则是一个对某评论的回复评论
        if (root == 0) {
            ProgressDialog.setMessage("查找无账号评论区时间排序第一页");
            var resp = await getMainCommentList(oid, type,0,SORT_MODE_TIME,false);
            if(resp.code != 0){
                showErrorResult("获取评论主列表时发生错误，响应数据：" + resp);
                return;
            }
            console.log(resp);
            var replies = resp.data.replies;
            var found = findReplies(replies, rpid);
            if (found) {
                showOkResult(reply);
            } else {
                //有账号获取评论回复页
                ProgressDialog.setMessage("有账号获取此评论的回复列表");
                resp = await fetchBilibiliCommentReplies(oid, type, rpid, 0, sortByTime, true);
                //“已经被删除了”状态码
                if (resp.code == 12022) {
                    //自己都显示被删除了那就真删除了（ps，按照流程图还要多个cookie检查，但是浏览器环境没这问题）
                    showQuickDeleteResult(reply)
                } else if (resp.code == 0) {
                    //继续无账号获取来检查，看看是否是可疑的？
                    ProgressDialog.setMessage("无账号获取此评论的回复列表");
                    resp = await fetchBilibiliCommentReplies(oid, type, rpid, 0, sortByTime, false);
                    if (resp.code == 12022) {
                        showShadowBanResult(reply);
                    } else if (resp.code == 0) {
                        showSusResult(reply);
                    } else {
                        console.log(resp);
                        showErrorResult("获取评论回复列表时发生错误，响应数据：" + resp);
                    }
                } else {
                    console.log(resp);
                    showErrorResult("有账号获取评论回复列表时发生错误，响应数据：" + resp);
                }
            }
        } else {
            ProgressDialog.setMessage("无账号查找评论回复页……");

            for (i = 0; true; i++) {
                ProgressDialog.setMessage(`无账号查找评论回复第${i}页……`);
                var resp = await fetchBilibiliCommentReplies(oid, type, root, i, sortByTime, false);
                var replies = resp.data.replies;
                console.log(resp);
                if (replies === null || replies.length === 0) {
                    console.log("已翻遍无账号下的评论回复页");
                    break;
                }

                if (findReplies(replies, rpid)) {
                    showOkResult(reply);
                    return;
                }
            }

            for (i = 0; true; i++) {
                ProgressDialog.setMessage(`有账号查找评论回复第${i}页……`);
                var resp = await fetchBilibiliCommentReplies(oid, type, root, i, sortByTime, true);
                var replies = resp.data.replies;
                console.log(resp);
                if (replies === null || replies.length === 0) {
                    console.log("已翻遍有账号下的评论回复页");
                    break;
                }

                if (findReplies(replies, rpid)) {
                    showShadowBanResult(reply);
                    return;
                }
            }

            //若两个条件都没找到评论则是秒删
            showQuickDeleteResult(reply);
        }
    }
}

async function handleCheckDynamic(id) {
    var resp = await fetchDynamic(id, false);
    console.log(resp);
    if (resp.code == -352) {
        addDynamicShadowBannedHint("检测到此动态被shadowBan，仅自己可见！（也可能是误判了，你可以在无痕模式去验证一下）");
    } else if (resp.code == 4101131) {
        console.log("检测到动态被shadowBan！");
        addDynamicShadowBannedHint("检测到此动态被shadowBan，仅自己可见！（可能你转发到动态的评论被ShadowBan）");
    } else if (resp.code == 500) {
        console.log("检测到动态被shadowBan！");
        addDynamicShadowBannedHint("检测到此动态被shadowBan，仅自己可见!（可能你转发到动态的评论疑似审核中）");
    } else if (resp.code == 0) {
        console.log("检查到此动态正常，没被shadowBan");
    } else {
        console.log("动态检查出错：未知的响应码", resp);
    }
}

function findReplies(replies, rpid) {
    for (var i in replies) {
        var reply = replies[i];
        console.log(reply);
        if (reply.rpid == rpid) {
            return reply;
        }
    }
    return null;
}

function findReplyInReplies(replies, rpid) {
    for (var i in replies) {
        var reply = replies[i];
        console.log(reply);
        var subReplies = reply.replies;
        console.log(subReplies)
        for (var j in subReplies) {
            var subReply = subReplies[j];
            console.log(subReply);
            if (subReply.rpid == rpid) {
                return subReply;
            }
        }
    }
    return null;
}

async function sleepAndShowInDialog(sleepTime) {
    ProgressDialog.setTitle("等待检查中");
    var sleepCount = sleepTime / 10;
    for (var i = 0; i <= sleepCount; i++) {
        await sleep(10);
        ProgressDialog.setMessage(`等待 ${i * 10}/${sleepTime}ms 后检查评论`)
        ProgressDialog.setProgress(100 / sleepCount * i);
    }
    ProgressDialog.setProgress(100);
}

/**
 * 
 * @param {*} oid 
 * @param {*} type 
 * @param {*} next 
 * @param {*} mode 评论排序模式 2为按时间
 * @param {*} isLogin 是否携带cookie
 * @param {*} seek_rpid 定位rpid
 * @returns 
 */

async function getMainCommentList(oid, type, next, mode,isLogin,seek_rpid) {  
    let url = `https://api.bilibili.com/x/v2/reply/main?oid=${oid}&type=${type}&next=${next}&mode=${mode}` + (seek_rpid ? `&seek_rpid=${seek_rpid}` : "")
    const req = {
        url,
        anonymous: !isLogin
    }

    req.headers={ "cookie": getBuvid3Cookie() };
    let response = (await GM.xmlHttpRequest(req).catch(e => console.error(e))).response;
    let resp = JSON.parse(response);
    console.log("获取主评论列表，携带cookie："+isLogin,url,resp)
    return resp;
}

/**
 * 获取某评论的回复列表
 * @param {*} oid 
 * @param {*} type 
 * @param {*} root 
 * @param {*} pn 
 * @param {*} sort 
 * @param {*} hasCookie 
 * @returns 
 */
async function fetchBilibiliCommentReplies(oid, type, root, pn, sort, hasCookie) {
    const url = new URL('https://api.bilibili.com/x/v2/reply/reply');
    const params = { oid, type, root, pn, sort };
    url.search = new URLSearchParams(params).toString();

    try {
        const response = await originalFetch(url, hasCookie ? { credentials: 'include' } : { credentials: 'omit' });
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return await response.json(); // Return JSON object
    } catch (error) {
        throw error; // Rethrow the error
    }
}

/**
 * 由于需要残次cookie，浏览器js无法自定义cookie，此方法废弃，需要翻全页
 * 
 * 使用Main api 结合 seek_rpid 参数定位评论
 * 如果seek_rpid 的评论id是一个回复别人的评论，
 * 那么它会出现在某个根评论的预览评论列表里
 * @param {*} oid 
 * @param {*} type 
 * @param {*} seek_rpid 要查看的rpid
 * @param {*} next 页码（从零开始）
 * @param {*} mode 排序模式
 * @param {*} hasCookie 
 * @returns 
 */
async function fetchBilibiliCommentsByMainApiUseSeekRpid(oid, type, seek_rpid, next, mode, hasCookie) {
    const url = new URL('https://api.bilibili.com/x/v2/reply/main');
    const params = { oid, type, seek_rpid, next, mode };
    url.search = new URLSearchParams(params).toString();

    try {
        const response = await originalFetch(url, hasCookie ? { credentials: 'include' } : { credentials: 'omit' });
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return await response.json(); // Return JSON object
    } catch (error) {
        throw error; // Rethrow the error
    }
}


async function fetchDynamic(id, hasCookie) {
    const url = new URL('https://api.bilibili.com/x/polymer/web-dynamic/v1/detail');
    const params = { id };
    url.search = new URLSearchParams(params).toString();

    try {
        const response = await originalFetch(url, hasCookie ? { credentials: 'include' } : { credentials: 'omit' });
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return await response.json(); // Return JSON object
    } catch (error) {
        throw error; // Rethrow the error
    }
}


function showOkResult(reply) {
    showResult("恭喜，无账号状态下找到了你的评论，你的评论正常！\n\n你的评论：" + reply.content.message);
}

function showShadowBanResult(reply) {
    showResult("你被骗了，此评论被shadow ban（仅自己可见）！\n\n你的评论：" + reply.content.message);
}

function showQuickDeleteResult(reply) {
    showResult("你评论没了，此评论已被系统秒删！刷新评论区也许就不见了，复制留个档吧。\n\n你的评论：" + reply.content.message);
}

function showSusResult(reply) {
    showResult(`
                你评论状态有点可疑，虽然无账号翻找评论区获取不到你的评论，但是无账号可通过
                https://api.bilibili.com/x/v2/reply/reply?oid=${reply.oid}&pn=1&ps=20&root=${reply.rpid}&type=${reply.type}&sort=0
                获取你的评论，疑似评论区被戒严或者这是你的视频。

                你的评论：${reply.content.message}
            `);
}

function showResult(message) {
    ProgressDialog.setIndeterminate(false);
    ProgressDialog.setProgress(100);
    ProgressDialog.setTitle("检查完毕");
    ProgressDialog.setMessage(message);
}

function showErrorResult(message) {
    ProgressDialog.setIndeterminate(false);
    ProgressDialog.setProgress(0);
    ProgressDialog.setTitle("发生错误");
    ProgressDialog.setMessage(message);
}

//样式抄自X（Twitter）的shadowBan检查器，插件可在Chrome商店搜索
function addDynamicShadowBannedHint(message) {
    const biliDynContent = document.querySelector('.bili-dyn-content');

    if (biliDynContent) {
        const shadowbanMessage = document.createElement('div');
        shadowbanMessage.className = 'shadowban-scanner-message';
        shadowbanMessage.style.setProperty('--md-sys-color-on-primary', 'rgb(15, 20, 25)');

        const messageSpan = document.createElement('span');
        messageSpan.textContent = message;

        shadowbanMessage.appendChild(messageSpan);
        biliDynContent.appendChild(shadowbanMessage);
    }
}

function getBuvid3Cookie() {
    var cookies = document.cookie.split(';');
    for (var i = 0; i < cookies.length; i++) {
        var cookie = cookies[i].trim();
        if (cookie.startsWith('buvid3=')) {
            return cookie;
        }
    }
    return null;
}
