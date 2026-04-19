    // ---------- 买卖双方私聊核心 (买家 & 卖家) ----------
    // 角色定义: 'buyer' (买家，消息左侧) , 'seller' (卖家，消息右侧)
    let tradeMessages = [];
    let ws = null;
    let pendingMessages = new Map();  // 存储待确认的消息
    let nextMessageId = 1;            // 消息ID生成器
    let idMapping = new Map();         // ID映射 tempId -> realId
    let reverseMapping = new Map();    // ID映射 realId -> tempId

    
    // DOM 元素
    const messagesContainer = document.getElementById('tradeMessagesArea');
    const messageInput = document.getElementById('tradeMessageInput');
    const sendBtn = document.getElementById('sendTradeBtn');
    const typingArea = document.getElementById('tradeTypingArea');
    const quickBtns = document.querySelectorAll('.quick-btn');

    // 状态机: 模拟对方正在输入 & 回复逻辑 (卖家与买家智能响应)
    let isOtherTyping = false;         // 对方是否正在输入
    let otherTypingTimeout = null;
    let pendingReplyTimeout = null;    // 模拟对方回复的定时器
    let currentUserId = null;           // 当前用户 ID (用于区分买卖双方，实际场景中需要根据登录状态获取)
    let conversationId = null;          // 当前会话 ID (用于发送消息时关联会话)

    // 当前登录视角: 假定当前用户是「买家」 (但是用户也可以发消息，聊天双方均可主动)
    // 但是为了真实买卖场景: 买家可以发消息，卖家自动模拟回复; 卖家也可以主动发消息，买家模拟回复?
    // 更符合买卖双方：当前用户既是买家也是卖家？实际页面中用户身份是买家（发消息的是我），对方是卖家。
    // 但为了双向交流，我们定义：用户发送消息的角色始终为“买家”(buyer)；
    // 而模拟回复方为“卖家”(seller) 当用户发言后，卖家会智能回复；同时，卖家也可以主动发起消息（通过快捷回复/模拟行为？）
    // 为了沉浸，当用户使用快捷按钮时，也是以买家身份发送，卖家会回复。此外可额外增加卖家主动问好? 在初始化已包含
    // 为了双向，我们可以增加卖家偶尔主动推送提示，但为了简洁可靠，让所有买家发出的消息都触发卖家回复，且卖家偶尔也会在对话停顿后主动介绍。
    
    // 辅助函数: 获取当前时间 HH:MM
    function getCurrentTime() {
        const now = new Date();
        return `${now.getHours().toString().padStart(2,'0')}:${now.getMinutes().toString().padStart(2,'0')}`;
    }

    // 滚动到底部
    function scrollToBottom() {
        if (messagesContainer) {
            messagesContainer.scrollTo({
                top: messagesContainer.scrollHeight,
                behavior: 'smooth'
            });
        }
    }

    // 判断是否在底部 (用于渲染保持)
    function isAtBottom() {
        if (!messagesContainer) return true;
        const { scrollTop, scrollHeight, clientHeight } = messagesContainer;
        return scrollHeight - scrollTop - clientHeight <= 12;
    } 
  
    // 生成唯一ID
    function generateMessageId() {
        return Date.now() * 1000 + nextMessageId++;
    }

    // 处理消息确认超时
    function setTimeoutCheck(messageId) {
        setTimeout(() => {
            const message = pendingMessages.get(messageId);
            if (message && message.status === 'sending') {
                if (message.retryCount < 3) {
                    // 重试
                    message.retryCount++;
                    console.log(`消息 ${messageId} 超时，重试第 ${message.retryCount} 次`);
                    resendMessage(messageId);
                } else {
                    // 超过重试次数，标记失败
                    message.status = 'failed';
                    renderTradeMessages();
                    console.log(`消息 ${messageId} 发送失败，已重试 ${message.retryCount} 次`);
                }
            }
        }, 3000);
    }

    // 重发消息方法
    function resendMessage(messageId) {
        const message = pendingMessages.get(messageId);
        if (message && message.content) {
            // 重新发送到服务器
            ws.send(JSON.stringify({
                type: 'new_message',
                content: content,
                sender: currentUserId,
                timestamp: newMsg.timestamp.getTime(),
                timeStr: newMsg.timeStr
            }));
            
            // 重新启动超时检查
            setTimeoutCheck(messageId);
        }
    }

    // 收到服务器确认
    function handleAck(tempId, realId, isread = false) {
        idMapping.set(tempId, realId);
        reverseMapping.set(realId, tempId);
        updateMessageStatusById(tempId, 'sent', isread);
    }

    // 渲染所有消息
    function renderTradeMessages() {
        if (!messagesContainer) return;
        const wasAtBottom = isAtBottom();
        // 清除现有消息行(保留未感染)
        messagesContainer.innerHTML = '';

        tradeMessages.forEach(msg => {
        const rowDiv = document.createElement('div');
        rowDiv.setAttribute('data-message-id', msg.id);
        if(parseInt(msg.sender) === parseInt(currentUserId)) {
            rowDiv.className = `message-row my`;  // 'my' 或 'author'
        } else {
            rowDiv.className = `message-row author`;
        }
        const bubbleDiv = document.createElement('div');
        bubbleDiv.className = 'bubble';
        bubbleDiv.innerHTML = msg.content.replace(/\n/g, '<br>');
        const metaDiv = document.createElement('div');
        metaDiv.className = 'message-meta';
        const timeSpan = document.createElement('span');
        timeSpan.innerText = msg.timeStr;
        const statusSpan = document.createElement('span');
        statusSpan.innerText = msg.isread ? '已读' : '已送达';
        statusSpan.style.color = '#4CAF50';
        metaDiv.appendChild(timeSpan);
        metaDiv.appendChild(statusSpan);
        bubbleDiv.appendChild(metaDiv);
        rowDiv.appendChild(bubbleDiv);
        messagesContainer.appendChild(rowDiv);
        });
        
        if (wasAtBottom || tradeMessages.length > 0) {
            scrollToBottom();
        }
    }

    // 通过 messageId 更新单条消息的状态
    function updateMessageStatusById(messageId, newStatus, isread = false) {
        // 通过 data-message-id 属性直接找到对应的消息元素
        const messageElement = messagesContainer.querySelector(`[data-message-id="${messageId}"]`);
        
        if (!messageElement) {
            console.warn(`未找到 messageId 为 ${messageId} 的消息元素`);
            return false;
        }
        
        // 找到状态span（消息元数据的最后一个span）
        const statusSpan = messageElement.querySelector('.message-meta span:last-child');
        if (!statusSpan) return false;
        
        // 更新内存中的数据
        const msg = tradeMessages.find(m => m.id === messageId);
        if (msg) {
            msg.status = newStatus;
            if (isread !== undefined) msg.isread = isread;
        }
        
        // 根据新状态更新UI
        if (newStatus === 'sending') {
            statusSpan.innerText = '发送中...';
            statusSpan.style.color = '#999';
            statusSpan.style.cursor = 'default';
            statusSpan.onclick = null;
        } else if (newStatus === 'sent') {
            statusSpan.innerText = isread ? '已读' : '已送达';
            statusSpan.style.color = '#4CAF50';
            statusSpan.style.cursor = 'default';
            statusSpan.onclick = null;
            // 从待确认列表中移除
            if (pendingMessages && pendingMessages.has(messageId)) {
                pendingMessages.delete(messageId);
                console.log(`消息 ${messageId} 已从待确认列表移除`);
            }
        } else if (newStatus === 'failed') {
            statusSpan.innerText = '发送失败';
            statusSpan.style.color = '#F44336';
            statusSpan.style.cursor = 'pointer';
            // 添加重试功能
            statusSpan.onclick = () => resendMessage(messageId);
        }
    
        return true;
    }
    
    // 发送消息
    function sendMessage(newMessage) { 
        // 添加到数据数组
        tradeMessages.push(newMessage);
        
        // 直接创建并添加DOM元素（复用你的渲染逻辑）
        const rowDiv = document.createElement('div');
        rowDiv.setAttribute('data-message-id', messageId);  // ✅ 设置ID
        
        if(parseInt(newMessage.sender) === parseInt(currentUserId)) {
            rowDiv.className = `message-row my`;
        } else {
            rowDiv.className = `message-row author`;
        }
        
        const bubbleDiv = document.createElement('div');
        bubbleDiv.className = 'bubble';
        bubbleDiv.innerHTML = newMessage.content.replace(/\n/g, '<br>');
        
        const metaDiv = document.createElement('div');
        metaDiv.className = 'message-meta';
        
        const timeSpan = document.createElement('span');
        timeSpan.innerText = newMessage.timeStr;
        
        const statusSpan = document.createElement('span');
        statusSpan.innerText = '发送中...';
        statusSpan.style.color = '#999';
        
        metaDiv.appendChild(timeSpan);
        metaDiv.appendChild(statusSpan);
        bubbleDiv.appendChild(metaDiv);
        rowDiv.appendChild(bubbleDiv);
        
        messagesContainer.appendChild(rowDiv);
        scrollToBottom();
        
        // 发送到服务器
        ws.send(JSON.stringify({
            type: 4,
            conversationId: parseInt(conversationId),
            messageId: newMessage.messageId,
            content: newMessage.content,
            timeStr: newMessage.timeStr
        }));
        
        // 启动超时检查
        startTimeoutCheck(messageId);
    }

    // 收到新消息后渲染（默认状态：已发送）
    function receiveAndRenderNewMessage(data) {
        tradeMessages.push({
            messageId: data.realId,
            sender: data.sender,
            content: data.content,
            timeStr: data.timeStr,
            status: 'sent',      // 默认状态：已发送
            isread: true        // 默认已读
        });
        
        // 创建DOM元素
        const rowDiv = document.createElement('div');
        rowDiv.setAttribute('data-message-id', data.messageId);
        
        // 判断发送者，设置样式
        if(parseInt(data.sender) === parseInt(currentUserId)) {
            rowDiv.className = `message-row my`;
        } else {
            rowDiv.className = `message-row author`;
        }
        
        // 创建气泡
        const bubbleDiv = document.createElement('div');
        bubbleDiv.className = 'bubble';
        bubbleDiv.innerHTML = data.content.replace(/\n/g, '<br>');
        
        // 创建消息元数据区域
        const metaDiv = document.createElement('div');
        metaDiv.className = 'message-meta';
        
        // 时间
        const timeSpan = document.createElement('span');
        timeSpan.innerText = data.timeStr;
        
        // 状态（默认已发送）
        const statusSpan = document.createElement('span');
        statusSpan.innerText = '已发送';
        statusSpan.style.color = '#4CAF50';
        statusSpan.style.cursor = 'default';
        statusSpan.onclick = null;
        
        // 组装DOM
        metaDiv.appendChild(timeSpan);
        metaDiv.appendChild(statusSpan);
        bubbleDiv.appendChild(metaDiv);
        rowDiv.appendChild(bubbleDiv);
        
        // 添加到容器并滚动到底部
        messagesContainer.appendChild(rowDiv);
        scrollToBottom();
    }
    
    // 显示对方正在输入
    function showTyping(show) {
        if (show && !isOtherTyping) {
            isOtherTyping = true;
            const typingDiv = document.createElement('div');
            typingDiv.className = 'peer-typing';
            typingDiv.id = 'tradeTypingWidget';
            typingDiv.innerHTML = `
                <span>对方正在输入...</span>
                <div class="dots">
                    <span></span><span></span><span></span>
                </div>
            `;
            typingArea.innerHTML = '';
            typingArea.appendChild(typingDiv);
            scrollToBottom();
        } else if (!show && isOtherTyping) {
            isOtherTyping = false;
            typingArea.innerHTML = '';
            if (otherTypingTimeout) clearTimeout(otherTypingTimeout);
        }
    }

    // 输入事件监听，发送正在输入状态
    messageInput.addEventListener('input', () => {
        clearTimeout(timer);
        
        // 节流：500ms内只发送一次
        if (!messageInput.hasAttribute('data-typing-sent')) {
            messageInput.setAttribute('data-typing-sent', 'true');
            ws.send(JSON.stringify({ type: 'typing', isTyping: true, sender: currentUserId }));
            setTimeout(() => messageInput.removeAttribute('data-typing-sent'), 500);
        }
        
        // 停止输入1秒后发送false
        timer = setTimeout(() => {
            ws.send(JSON.stringify({ type: 5, isTyping: false, sender: currentUserId }));
        }, 1000);
    });
    
    // 智能回复库 (针对买卖场景，角色是卖家回复买家)
    function getSellerReply(buyerMsg) {
        const lower = buyerMsg.toLowerCase();
        if (lower.includes('视频') || lower.includes('实物')) {
            return "没问题！我马上拍一段高清视频发给您，稍等～ 📹 加我微信也可以发原图哦。";
        } else if (lower.includes('包邮') || lower.includes('发货')) {
            return "全场包邮的哈，今天下午4点前下单都能当天发货！发顺丰✈️。";
        } else if (lower.includes('优惠') || lower.includes('便宜') || lower.includes('议价') || lower.includes('价格')) {
            return "亲，这款性价比很高了，可以送您一个精美小礼品，最低179给您，您看可以吗？🎁";
        } else if (lower.includes('保修') || lower.includes('退换') || lower.includes('售后')) {
            return "支持7天无理由退换，一年保修，放心购买～ 有任何问题随时找我💪";
        } else if (lower.includes('正品') || lower.includes('真假')) {
            return "绝对正品渠道，支持专柜鉴定，假一赔十！这是我们的承诺✨";
        } else if (lower.includes('你好') || lower.includes('在吗')) {
            return "在的！有什么可以帮您？这款包包最近很受欢迎，库存不多啦~";
        } else if (lower.includes('下单') || lower.includes('怎么买')) {
            return "点击商品页直接下单就可以，或者我给您发链接～ 现在下单还送挂件哦！🛍️";
        } else if (lower.length > 18) {
            return "收到啦，我仔细看看您的问题～ 别急哈，我会一一解答的。这款包包性价比很高，很多买家反馈很好😊";
        } else {
            const replies = [
                "好滴～ 我明白了，您还需要了解其他细节吗？",
                "谢谢您的咨询！这款是今天热卖款，很多姐妹都在买~",
                "嗯嗯，是的呢。需要帮您预留一件吗？",
                "感谢支持！有任何需要随时喊我 😄"
            ];
            return replies[Math.floor(Math.random() * replies.length)];
        }
    }

    // websocket.js - WebSocket 连接文件
    function connectWebSocket() {
        // 连接时携带 token
        ws = new WebSocket(`ws://localhost:8080/ws/chat?UserId=${currentUserId}&token=${token}`);
        
        ws.onopen = () => {
            console.log('WebSocket 连接成功');
        };
    }

    // 进入聊天界面
    document.addEventListener('DOMContentLoaded', async () => {
        const token = localStorage.getItem('token');
        if (!token) {
            sessionStorage.setItem('redirectAfterLogin', window.location.href);
            window.location.href = '/login.html';
            return;
        }
        
        // 获取当前用户
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
        currentUserId = userInfo.userId;
        
        // 解析进入方式
        const urlParams = new URLSearchParams(window.location.search);
        const sellerId = urlParams.get('sellerId');      // 场景1：从商品详情页进入
        conversationId = urlParams.get('conversationId'); // 场景2：从消息列表进入
        const userId = urlParams.get('userId');          // 场景3：从用户主页进入
        
        // 连接 WebSocket
        let ws = connectWebSocket();
        
        // 根据参数决定显示什么
        if (sellerId) {
            // 场景1：直接与卖家聊天
            ws.send(JSON.stringify({type: 1, userId: parseInt(currentUserId), sellerId: parseInt(sellerId)}));
            
            // 发送商品卡片
            const productName = urlParams.get('productName');
            if (productName) {
                sendProductCardMessage(parseInt(sellerId), {
                    productId: urlParams.get('productId'),
                    productName: productName
                });
            }
            
        } else if (conversationId) {
            // 场景2：从消息列表进入，打开指定会话
            ws.send(JSON.stringify({type: 2, conversationId: parseInt(conversationId)}));
            
            
        } else if (userId) {
            // 场景3：与指定用户聊天
            ws.send(JSON.stringify({type: 3, userId: currentUserId, authorUserId: parseInt(userId)}));
            
        }

        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            
            // 判断消息类型
            if (data.type === 'ack') {
                // 这是确认消息，交给确认管理器处理
                handleAck(data.tempId, data.realId, data.isread);
            } 
            else if (data.type === 'history') {
                // 历史消息，交给UI处理
                tradeMessages = data.messages.map(msg => ({
                sender: msg.sender,
                content: msg.content,
                timeStr: msg.timeStr,
                isread: msg.isread,
                status: 'sending',  // 添加状态字段: sending发送中, sent发送成功, failed发送失败
                }));
                if (conversationId === null) {
                    conversationId = data.conversationId; // 场景1或3进入时，服务器返回会话ID
                }
                renderTradeMessages();
            }
            else if (data.type === 'new_message') {
                // 新消息，显示并发送已读回执
                receiveAndRenderNewMessage(data);
                // 发送已读回执
                ws.send(JSON.stringify({ type: 6, messageId: data.messageId, isread: true }));
            }
            else if (data.type === 'typing') {
                // 对方正在输入
                showTyping(data.isTyping);
            }
        }
    });

    // 快捷按钮处理
    function handleQuickMsg(msgText) {
        if (!msgText) return;
        sendBuyerMessage(msgText);
    }
    
    // 输入框事件
    function autoResize() {
        const ta = messageInput;
        ta.style.height = 'auto';
        ta.style.height = Math.min(ta.scrollHeight, 100) + 'px';
    }
    
    function onInput() {
        autoResize();
    }
    
    function onKeydown(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            const msg = messageInput.value.trim();
            if (msg) {
                sendMessage(msg);
                messageInput.value = '';
                autoResize();
            }
        } else if (e.key === 'Enter' && e.shiftKey) {
            setTimeout(autoResize, 0);
        }
    }
    
    function initTradeChat() {
    tradeMessages = [];
    }
    
    // 事件绑定
    sendBtn.addEventListener('click', () => {
        const msg = messageInput.value.trim();
        if (msg) {
            const messageId = generateMessageId();
        
            // 创建新消息对象
            const newMessage = {
                id: messageId,
                sender: currentUserId,
                content: msg,
                timeStr: new Date().toLocaleTimeString(),
                createTime: new Date(),
                isread: false,
                status: 'sending'
            };
            sendMessage(newMessage);
            messageInput.value = '';
            autoResize();
        }
    });
    
    quickBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            const preset = btn.getAttribute('data-msg');
            if (preset) handleQuickMsg(preset);
        });
    });
    
    messageInput.addEventListener('input', onInput);
    messageInput.addEventListener('keydown', onKeydown);
    
    // 启动交易聊天
    initTradeChat();
    autoResize();



/*function openConversation(conversationId, otherUserId, otherUserName) {
    window.location.href = `/chat.html?conversationId=${conversationId}&userId=${otherUserId}&userName=${encodeURIComponent(otherUserName)}`;
}*/