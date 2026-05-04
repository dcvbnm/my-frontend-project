// ---------- 买卖双方私聊核心 (买家 & 卖家) ----------
let tradeMessages = [];
let ws = null;
let pendingMessages = new Map();
let nextMessageId = -1;
let idMapping = new Map();
let reverseMapping = new Map();
let messageId = 0;
let timer = null;
let reconnectTimer = null;

// DOM 元素
const messagesContainer = document.getElementById('tradeMessagesArea');
const messageInput = document.getElementById('tradeMessageInput');
const sendBtn = document.getElementById('sendTradeBtn');
const typingArea = document.getElementById('tradeTypingArea');
const quickBtns = document.querySelectorAll('.quick-btn');

// 状态机
let isOtherTyping = false;
let otherTypingTimeout = null;
let pendingReplyTimeout = null;
let currentUserId = null;
let conversationId = null;

// 辅助函数
function getCurrentTime() {
    const now = new Date();
    return `${now.getHours().toString().padStart(2,'0')}:${now.getMinutes().toString().padStart(2,'0')}`;
}

function scrollToBottom() {
    if (messagesContainer) {
        messagesContainer.scrollTo({
            top: messagesContainer.scrollHeight,
            behavior: 'smooth'
        });
    }
}

function isAtBottom() {
    if (!messagesContainer) return true;
    const { scrollTop, scrollHeight, clientHeight } = messagesContainer;
    return scrollHeight - scrollTop - clientHeight <= 12;
}

function generateMessageId() {
    // Use negative, int-safe temporary IDs so Java backend int parsing won't overflow.
    return nextMessageId--;
}

// 检查 WebSocket 是否可用
function isWebSocketOpen() {
    return ws && ws.readyState === WebSocket.OPEN;
}

// 开始超时检查
function startTimeoutCheck(msgId) {
    setTimeout(() => {
        const message = pendingMessages.get(msgId);
        if (message && message.status === 'sending') {
            message.status = 'failed';
            renderTradeMessages();
            console.log(`消息 ${msgId} 发送超时`);
        }
    }, 8000);
}

// 重发消息
function resendMessage(msgId) {
    const message = pendingMessages.get(msgId);
    if (message && message.content && isWebSocketOpen()) {
        message.status = 'sending';
        message.retryCount = (message.retryCount || 0) + 1;
        pendingMessages.set(msgId, message);
        ws.send(JSON.stringify({
            type: 4,
            conversationId: parseInt(conversationId),
            messageId: msgId,
            content: message.content,
            timeStr: message.timeStr
        }));
        startTimeoutCheck(msgId);
        renderTradeMessages();
    }
}

function handleAck(tempId, realId, isread = false) {
    idMapping.set(tempId, realId);
    reverseMapping.set(realId, tempId);
    updateMessageStatusById(tempId, 'sent', isread);
}

function renderTradeMessages() {
    if (!messagesContainer) return;
    const wasAtBottom = isAtBottom();
    messagesContainer.innerHTML = '';

    tradeMessages.forEach(msg => {
        const rowDiv = document.createElement('div');
        rowDiv.setAttribute('data-message-id', msg.id);
        const isMine = typeof msg.isMine === 'boolean'
            ? msg.isMine
            : parseInt(msg.sender) === parseInt(currentUserId);
        if (isMine) {
            rowDiv.className = 'message-row my';
        } else {
            rowDiv.className = 'message-row author';
        }
        const bubbleDiv = document.createElement('div');
        bubbleDiv.className = 'bubble';
        bubbleDiv.innerHTML = (msg.content || '').replace(/\n/g, '<br>');
        const metaDiv = document.createElement('div');
        metaDiv.className = 'message-meta';
        const timeSpan = document.createElement('span');
        timeSpan.innerText = msg.timeStr || getCurrentTime();
        const statusSpan = document.createElement('span');
        if (msg.status === 'failed') {
            statusSpan.innerText = '发送失败（点此重发）';
            statusSpan.style.color = '#F44336';
            statusSpan.style.cursor = 'pointer';
            statusSpan.onclick = () => resendMessage(msg.id);
        } else if (msg.status === 'sending') {
            statusSpan.innerText = '发送中...';
            statusSpan.style.color = '#999';
        } else {
            statusSpan.innerText = msg.isread ? '已读' : '已送达';
            statusSpan.style.color = '#4CAF50';
        }
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

function updateMessageStatusById(msgId, newStatus, isread = false) {
    const messageElement = messagesContainer.querySelector(`[data-message-id="${msgId}"]`);
    if (!messageElement) return false;
    
    const statusSpan = messageElement.querySelector('.message-meta span:last-child');
    if (!statusSpan) return false;
    
    const msg = tradeMessages.find(m => m.id === msgId);
    if (msg) {
        msg.status = newStatus;
        if (isread !== undefined) msg.isread = isread;
    }
    
    if (newStatus === 'sending') {
        statusSpan.innerText = '发送中...';
        statusSpan.style.color = '#999';
    } else if (newStatus === 'sent') {
        statusSpan.innerText = isread ? '已读' : '已送达';
        statusSpan.style.color = '#4CAF50';
        if (pendingMessages && pendingMessages.has(msgId)) {
            pendingMessages.delete(msgId);
        }
    } else if (newStatus === 'failed') {
        statusSpan.innerText = '发送失败（点此重发）';
        statusSpan.style.color = '#F44336';
        statusSpan.style.cursor = 'pointer';
        statusSpan.onclick = () => resendMessage(msgId);
    }
    return true;
}

function sendMessage(newMessage) {
    tradeMessages.push(newMessage);
    pendingMessages.set(newMessage.id, newMessage);
    renderTradeMessages();
    
    if (isWebSocketOpen()) {
        ws.send(JSON.stringify({
            type: 4,
            conversationId: parseInt(conversationId),
            messageId: newMessage.id,
            content: newMessage.content,
            timeStr: newMessage.timeStr
        }));
        startTimeoutCheck(newMessage.id);
    } else {
        console.warn('WebSocket 未连接，消息未发送');
        updateMessageStatusById(newMessage.id, 'failed');
    }
}

function receiveAndRenderNewMessage(data) {
    const senderId = data.sender ?? data.senderId;
    tradeMessages.push({
        id: data.realId || data.messageId,
        sender: senderId,
        // new_message is pushed by server to the peer side, so it should render as the other party.
        isMine: false,
        content: data.content,
        timeStr: data.timeStr,
        status: 'sent',
        isread: true
    });
    renderTradeMessages();
}

function showTyping(show) {
    if (!typingArea) return;
    if (show && !isOtherTyping) {
        isOtherTyping = true;
        const typingDiv = document.createElement('div');
        typingDiv.className = 'peer-typing';
        typingDiv.id = 'tradeTypingWidget';
        typingDiv.innerHTML = `<span>对方正在输入...</span><div class="dots"><span></span><span></span><span></span></div>`;
        typingArea.innerHTML = '';
        typingArea.appendChild(typingDiv);
        scrollToBottom();
    } else if (!show && isOtherTyping) {
        isOtherTyping = false;
        typingArea.innerHTML = '';
        if (otherTypingTimeout) clearTimeout(otherTypingTimeout);
    }
}

// 重新连接 WebSocket
function reconnectWebSocket() {
    if (reconnectTimer) clearTimeout(reconnectTimer);
    reconnectTimer = setTimeout(() => {
        console.log('尝试重新连接 WebSocket...');
        connectWebSocket();
    }, 3000);
}

// WebSocket 连接
function connectWebSocket() {
    const token = localStorage.getItem('accessToken');
    if (!token || !currentUserId) {
        console.log('未登录，无法建立 WebSocket 连接');
        return;
    }
    
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
        console.log('WebSocket 已连接或正在连接');
        return;
    }
    
    const wsUrl = `ws://localhost:8081/ws/chat?UserId=${currentUserId}&token=${token}`;
    console.log('连接 WebSocket:', wsUrl);
    
    try {
        ws = new WebSocket(wsUrl);
        
        ws.onopen = () => {
            console.log('WebSocket 连接成功');
            if (reconnectTimer) clearTimeout(reconnectTimer);
            
            // 根据进入方式发送消息
            const urlParams = new URLSearchParams(window.location.search);
            const sellerId = urlParams.get('sellerId');
            const convId = urlParams.get('conversationId');
            const userId = urlParams.get('userId');
            
            if (sellerId) {
                ws.send(JSON.stringify({type: 1, senderId: parseInt(sellerId), currentUserId: parseInt(currentUserId)}));
            } else if (convId) {
                conversationId = parseInt(convId);
                ws.send(JSON.stringify({type: 2, conversationId: conversationId, currentUserId: parseInt(currentUserId)}));
            } else if (userId) {
                ws.send(JSON.stringify({type: 3, currentUserId: parseInt(currentUserId), authorUserId: parseInt(userId)}));
            }
        };
        
        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                console.log('收到消息:', data);
                
                if (data.type === 'ack') {
                    handleAck(data.tempId, data.realId, data.isread);
                } else if (data.type === 'history') {
                    tradeMessages = data.messages.map(msg => ({
                        id: msg.id,
                        sender: msg.sender ?? msg.senderId,
                        isMine: parseInt(msg.sender ?? msg.senderId) === parseInt(currentUserId),
                        content: msg.content,
                        timeStr: msg.timeStr,
                        isread: msg.isread ?? msg.isRead,
                        status: 'sent'
                    }));
                    if (conversationId === null) {
                        conversationId = data.conversationId;
                    }
                    renderTradeMessages();
                } else if (data.type === 'new_message') {
                    receiveAndRenderNewMessage(data);
                    if (isWebSocketOpen()) {
                        ws.send(JSON.stringify({ type: 6, messageId: data.messageId, isread: true }));
                    }
                } else if (data.type === 'all_read') {
                    tradeMessages.forEach(msg => { msg.isread = true; });
                    renderTradeMessages();
                } else if (data.type === 'typing') {
                    showTyping(data.isTyping);
                }
            } catch (e) {
                console.error('解析消息失败:', e);
            }
        };
        
        ws.onclose = (event) => {
            console.log('WebSocket 连接已关闭:', event.code);
            ws = null;
            reconnectWebSocket();
        };
        
        ws.onerror = (error) => {
            console.error('WebSocket 错误:', error);
        };
    } catch (e) {
        console.error('WebSocket 连接失败:', e);
        reconnectWebSocket();
    }
}

// 发送商品卡片
function sendProductCardMessage(sellerId, productInfo) {
    if (isWebSocketOpen()) {
        ws.send(JSON.stringify({
            type: 7,
            sellerId: sellerId,
            productId: productInfo.productId,
            productName: productInfo.productName
        }));
    }
}

// 发送买家消息
function sendBuyerMessage(msgText) {
    if (!msgText) return;
    const msgId = generateMessageId();
    const newMessage = {
        id: msgId,
        sender: currentUserId,
        isMine: true,
        content: msgText,
        timeStr: getCurrentTime(),
        createTime: new Date(),
        isread: false,
        status: 'sending'
    };
    sendMessage(newMessage);
}

// 输入框事件
function autoResize() {
    if (!messageInput) return;
    messageInput.style.height = 'auto';
    messageInput.style.height = Math.min(messageInput.scrollHeight, 100) + 'px';
}

function onInput() {
    autoResize();
    if (timer) clearTimeout(timer);
    if (isWebSocketOpen()) {
        ws.send(JSON.stringify({ type: 5, isTyping: true, sender: currentUserId }));
    }
    timer = setTimeout(() => {
        if (isWebSocketOpen()) {
            ws.send(JSON.stringify({ type: 5, isTyping: false, sender: currentUserId }));
        }
    }, 1000);
}

function onKeydown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        const msg = messageInput.value.trim();
        if (msg) {
            sendBuyerMessage(msg);
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

function parseChatIdentity(urlParams, userInfo) {
    const chatId = urlParams.get('chatId');
    if (chatId !== null && chatId !== '') {
        const parsed = Number.parseInt(chatId, 10);
        return Number.isNaN(parsed) ? null : parsed;
    }
    const userId = userInfo && userInfo.userId;
    if (userId === null || userId === undefined || userId === '') {
        return null;
    }
    const parsed = Number.parseInt(userId, 10);
    return Number.isNaN(parsed) ? null : parsed;
}

// 页面初始化
document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
        sessionStorage.setItem('redirectAfterLogin', window.location.href);
        window.location.href = '/user/login';
        return;
    }
    
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    const urlParams = new URLSearchParams(window.location.search);
    currentUserId = parseChatIdentity(urlParams, userInfo);
    if (currentUserId === null) {
        sessionStorage.setItem('redirectAfterLogin', window.location.href);
        window.location.href = '/user/login';
        return;
    }
    
    // 解析 URL 参数
    conversationId = urlParams.get('conversationId');
    const sellerId = urlParams.get('sellerId');
    const userId = urlParams.get('userId');
    const goodsId = urlParams.get('goodsId');

    // 仅允许从商品详情页（sellerId）或已有会话（conversationId/userId）进入
    if (!conversationId && !sellerId && !userId) {
        alert('请从商品详情页点击聊天按钮发起会话');
        window.location.href = '/browse';
        return;
    }

    if (sellerId && Number(sellerId) === Number(currentUserId)) {
        alert('不能和自己发起聊天');
        if (goodsId) {
            window.location.href = `/buy/${encodeURIComponent(goodsId)}`;
        } else {
            window.location.href = '/browse';
        }
        return;
    }
    
    // 连接 WebSocket
    connectWebSocket();
    
    // 事件绑定
    if (sendBtn) {
        sendBtn.addEventListener('click', () => {
            const msg = messageInput.value.trim();
            if (msg) {
                sendBuyerMessage(msg);
                messageInput.value = '';
                autoResize();
            }
        });
    }
    
    quickBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            const preset = btn.getAttribute('data-msg');
            if (preset) sendBuyerMessage(preset);
        });
    });
    
    if (messageInput) {
        messageInput.addEventListener('input', onInput);
        messageInput.addEventListener('keydown', onKeydown);
    }
    
    initTradeChat();
    autoResize();
});