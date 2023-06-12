// ==UserScript==
// @name         哔哩发评反诈
// @version      1.0
// @description  评论发送后自动检测状态，避免被发送成功的谎言所欺骗！
// @author       freedom-introvert & ChatGPT
// @match        https://*.bilibili.com/*
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    var originalSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function() {
        this.addEventListener('load', function() {
            if (this.readyState === 4 && this.status === 200 && this.responseURL.includes('https://api.bilibili.com/x/v2/reply/add')) {
              console.log('Bilibili reply add response:', this.response);
                var data = JSON.parse(this.response).data;
                var rpid = data.rpid;
                var oid = data.reply.oid;
                var type = data.reply.type;
                setTimeout(function() {
                  //抹除cookie获取最新评论列表第一页，再查找有没有该rpid
                  GM_xmlhttpRequest({
                    method: 'GET',
                    url: 'https://api.bilibili.com/x/v2/reply/main?next=0&type='+type+'&oid='+oid+'&mode=2',
                    responseType: 'json',
                    anonymous: true,
                    onload: function(response) {
                      console.log('Bilibili reply get response:', response.response);
                        var replies = response.response.data.replies;
                        var found = false;
                        for (var i = 0; i < replies.length; i++) {
                            if (replies[i].rpid === rpid) {
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            alert('🥳评论正常显示');
                        } else {
                            //带cookie获取评论的回复列表，成功就是仅自己可见，已经被删除了就是被系统秒删
                            GM_xmlhttpRequest({
                               method: 'GET',
                               url: 'https://api.bilibili.com/x/v2/reply/reply?oid='+oid+'&pn=1&ps=10&root='+rpid+'&type=1',
                               responseType: 'json',
                               onload: function(response) {
                                  var respJson = response.response;
                                  console.log('Bilibili comment reply get response:', respJson);
                                  if(respJson.code == 0){
                                    alert('🤥评论被ShadowBan');
                                  } else if (respJson.code == 12022){
                                    alert('🚫评论被系统秒删');
                                  }
                               }
                            });

                        }
                    }
                });
              }, 3000)
            }
        });
        originalSend.apply(this, arguments);
    };
})();
