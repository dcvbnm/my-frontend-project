const chatListEl = document.getElementById("chatList");
const chatCountEl = document.getElementById("chatCount");
let refreshTimer = null;
let chatWs = null;

function getAccessToken() {
    return localStorage.getItem("accessToken");
}

function getCurrentUserId() {
    const userInfo = localStorage.getItem("userInfo");
    if (!userInfo) return null;
    try {
        const parsed = JSON.parse(userInfo);
        return parsed.userId;
    } catch (e) {
        console.error("解析用户信息失败:", e);
        return null;
    }
}

function formatTime(input) {
    if (!input) return "";
    const date = new Date(input);
    if (Number.isNaN(date.getTime())) return "";

    const now = new Date();
    const sameDay = date.toDateString() === now.toDateString();
    if (sameDay) {
        return `${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}`;
    }
    return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}`;
}

async function fetchConversations(userId) {
    const res = await fetch(`/api/chat/conversations?userId=${encodeURIComponent(userId)}`);
    const json = await res.json();
    if (json.code !== 200) {
        throw new Error(json.msg || "加载聊天列表失败");
    }
    return Array.isArray(json.data) ? json.data : [];
}

function connectChatSocket(userId) {
    const token = getAccessToken();
    if (!token || !userId) {
        return;
    }
    if (chatWs && (chatWs.readyState === WebSocket.OPEN || chatWs.readyState === WebSocket.CONNECTING)) {
        return;
    }

    const wsUrl = `ws://localhost:8081/ws/chat?UserId=${encodeURIComponent(userId)}&token=${encodeURIComponent(token)}`;
    chatWs = new WebSocket(wsUrl);

    chatWs.onopen = () => {
        console.log("聊天列表 WebSocket 已连接");
    };

    chatWs.onmessage = async (event) => {
        try {
            const data = JSON.parse(event.data);
            if (data.type === "new_message" || data.type === "all_read" || data.type === "ack") {
                const latest = await fetchConversations(userId);
                renderList(latest);
            }
        } catch (e) {
            console.warn("聊天列表消息解析失败:", e);
        }
    };

    chatWs.onclose = () => {
        chatWs = null;
        setTimeout(() => connectChatSocket(userId), 3000);
    };

    chatWs.onerror = () => {
        // 交给 onclose 后的重连处理
    };
}

function renderList(items) {
    chatCountEl.textContent = `${items.length} 个会话`;

    if (!items.length) {
        chatListEl.innerHTML = '<div class="empty-box">暂无聊天记录，去商品详情页发起聊天吧</div>';
        return;
    }

    chatListEl.innerHTML = items.map(item => {
        const unread = Number(item.unreadCount || 0);
        return `
            <button class="chat-item" type="button" data-conversation-id="${item.conversationId}" data-other-user-id="${item.otherUserId}">
                <div class="chat-item-top">
                    <span class="chat-user">${item.otherUserName || `用户${item.otherUserId}`}</span>
                    <span class="chat-time">${formatTime(item.lastMessageTime)}</span>
                </div>
                <p class="chat-message">${item.lastMessage || "[暂无消息]"}</p>
                <div class="chat-meta">
                    <span>会话ID: ${item.conversationId}</span>
                    ${unread > 0 ? `<span class="unread-pill">${unread}</span>` : "<span>已读</span>"}
                </div>
            </button>
        `;
    }).join("");
}

function bindEvents() {
    chatListEl.addEventListener("click", (event) => {
        const target = event.target.closest(".chat-item");
        if (!target) return;
        const conversationId = target.getAttribute("data-conversation-id");
        const otherUserId = target.getAttribute("data-other-user-id");
        window.location.href = `/communication?conversationId=${encodeURIComponent(conversationId)}&userId=${encodeURIComponent(otherUserId)}`;
    });
}

async function init() {
    const userId = getCurrentUserId();
    if (!userId) {
        alert("请先登录后查看聊天列表");
        window.location.href = "/user/login";
        return;
    }

    bindEvents();
    connectChatSocket(userId);
    try {
        const list = await fetchConversations(userId);
        renderList(list);
    } catch (e) {
        chatListEl.innerHTML = `<div class="empty-box">${e.message}</div>`;
    }

    if (refreshTimer) {
        clearInterval(refreshTimer);
    }
    refreshTimer = setInterval(async () => {
        if (document.hidden) {
            return;
        }
        try {
            const latest = await fetchConversations(userId);
            renderList(latest);
        } catch (e) {
            console.warn("刷新聊天列表失败:", e);
        }
    }, 5000);
}

window.addEventListener("beforeunload", () => {
    if (refreshTimer) {
        clearInterval(refreshTimer);
        refreshTimer = null;
    }
});

init();
