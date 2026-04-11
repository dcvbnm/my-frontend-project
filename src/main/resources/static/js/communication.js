    // ---------- 买卖双方私聊核心 (买家 & 卖家) ----------
    // 角色定义: 'buyer' (买家，消息左侧) , 'seller' (卖家，消息右侧)
    let tradeMessages = [];
    let ws = new WebSocket(`ws://${window.location.host}/api/ws`);  // 连接后端 WebSocket 服务器
    
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
    let lastSender = null;              // 最后一条消息的发送者，用于模拟不同角色回复
    
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

    // 渲染所有消息
    function renderTradeMessages() {
        if (!messagesContainer) return;
        const wasAtBottom = isAtBottom();
        // 清除现有消息行(保留未感染)
        messagesContainer.innerHTML = '';
        
        tradeMessages.forEach(msg => {
            const rowDiv = document.createElement('div');
            rowDiv.className = `message-row ${msg.sender}`;  // 'buyer' 或 'seller'
            const bubbleDiv = document.createElement('div');
            bubbleDiv.className = 'bubble';
            bubbleDiv.innerHTML = msg.content.replace(/\n/g, '<br>');
            const metaDiv = document.createElement('div');
            metaDiv.className = 'message-meta';
            const timeSpan = document.createElement('span');
            timeSpan.innerText = msg.timeStr;
            const statusSpan = document.createElement('span');
            // 已读回执风格：如果是卖家发给买家，默认已读；买家发给卖家且未读则显示已发送，稍后标记已读
            if (msg.sender === 'seller') {
                statusSpan.innerText = msg.read ? '已读' : '已发送';
            } else {
                statusSpan.innerText = msg.read ? '已读' : '已发送';
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
    
    // 添加消息 (sender: 'buyer' 或 'seller')
    function addTradeMessage(sender, content, markReadAfter = true) {
        const nowTime = getCurrentTime();
        const newMsg = {
            sender: sender,
            content: content.trim(),
            timestamp: new Date(),
            timeStr: nowTime,
            read: (sender === 'seller') ? true : false   // 卖家发的我立即已读，买家发的稍后模拟已读
        };
        tradeMessages.push(newMsg);
        renderTradeMessages();
        
        // 如果是买家发送的消息，延迟一秒后标记为“已读”（表示卖家已看）
        if (sender === 'buyer') {
            setTimeout(() => {
                for (let i = tradeMessages.length-1; i >= 0; i--) {
                    if (tradeMessages[i].sender === 'buyer' && !tradeMessages[i].read) {
                        tradeMessages[i].read = true;
                        renderTradeMessages();
                        break;
                    }
                }
            }, 800 + Math.random() * 700);
        }
        return newMsg;
    }
    
    // 显示对方 (卖家) 正在输入
    function showTyping(show) {
        if (show && !isOtherTyping) {
            isOtherTyping = true;
            const typingDiv = document.createElement('div');
            typingDiv.className = 'peer-typing';
            typingDiv.id = 'tradeTypingWidget';
            typingDiv.innerHTML = `
                <span>卖家正在输入...</span>
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
    
    // 模拟卖家回复 (延迟并显示正在输入)
    function simulateSellerReply(userMessage) {
        if (pendingReplyTimeout) clearTimeout(pendingReplyTimeout);
        // 显示卖家正在输入
        showTyping(true);
        if (otherTypingTimeout) clearTimeout(otherTypingTimeout);
        otherTypingTimeout = setTimeout(() => {
            showTyping(false);
        }, 3500);
        
        const delay = 1000 + Math.random() * 1500;
        pendingReplyTimeout = setTimeout(() => {
            showTyping(false);
            if (otherTypingTimeout) clearTimeout(otherTypingTimeout);
            const replyContent = getSellerReply(userMessage);
            addTradeMessage('seller', replyContent);
            pendingReplyTimeout = null;
        }, delay);
    }
    
    // 买家主动发送消息 (当前用户)
    function sendBuyerMessage(text) {
        if (!text.trim()) return;
        // 清空之前等待的卖家回复（避免堆积）
        if (pendingReplyTimeout) {
            clearTimeout(pendingReplyTimeout);
            pendingReplyTimeout = null;
        }
        // 关闭正在输入提示
        showTyping(false);
        if (otherTypingTimeout) clearTimeout(otherTypingTimeout);
        
        // 添加买家消息
        addTradeMessage('buyer', text);
        // 触发卖家模拟回复
        simulateSellerReply(text);
    }
    
    // 清空历史 (可选功能不暴露,但可以保留无清空但增加重置)
    // 买卖场景增加一个清爽功能？为了体验，不主动加清空避免误操作，但可通过控制台；同时已够用。
    
    

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
    renderTradeMessages();
    }
    
    // 事件绑定
    sendBtn.addEventListener('click', () => {
        const msg = messageInput.value.trim();
        if (msg) {
            sendBuyerMessage(msg);
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