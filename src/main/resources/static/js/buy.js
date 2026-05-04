const buyContainer = document.getElementById("buyContainer");

let currentGoods = null;

const typeMap = {
    1: "电子数码",
    2: "书籍教材",
    3: "生活用品",
    4: "美妆服饰",
    5: "运动器材",
    6: "交通代步",
    7: "二次元/手办",
    8: "票务卡券",
    9: "虚拟物品",
    10: "食品零食",
    0: "其他"
};

function getGoodsIdFromPath() {
    const parts = window.location.pathname.split("/").filter(Boolean);
    return parts.length ? parts[parts.length - 1] : "";
}

function getAccessToken() {
    return localStorage.getItem("accessToken") || "";
}

async function api(url, options = {}) {
    const headers = options.headers ? { ...options.headers } : {};
    const token = getAccessToken();
    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }
    if (options.body && !(options.body instanceof FormData) && !headers["Content-Type"]) {
        headers["Content-Type"] = "application/json";
    }

    const response = await fetch(url, {
        ...options,
        headers
    });
    const data = await response.json();
    if (data.code !== 200) {
        throw new Error(data.msg || "请求失败");
    }
    return data.data;
}

function openPurchaseModal() {
    if (!currentGoods) {
        return;
    }

    const token = getAccessToken();
    if (!token) {
        sessionStorage.setItem("redirectAfterLogin", window.location.href);
        window.location.href = "/user/login";
        return;
    }

    const modal = document.getElementById("purchaseModal");
    const quantityInput = document.getElementById("purchaseQuantity");
    const stock = Number(currentGoods.goodsQuantity || 0);
    if (!modal || !quantityInput) {
        return;
    }

    quantityInput.min = "1";
    quantityInput.max = String(stock);
    quantityInput.value = stock > 0 ? "1" : "0";
    modal.classList.remove("hidden");
}

function closePurchaseModal() {
    const modal = document.getElementById("purchaseModal");
    if (modal) {
        modal.classList.add("hidden");
    }
}

function renderPurchaseModal(canBuy, stock) {
    return `
        <div id="purchaseModal" class="purchase-modal hidden">
            <div class="purchase-mask" id="purchaseMask"></div>
            <div class="purchase-dialog" role="dialog" aria-modal="true" aria-labelledby="purchaseTitle">
                <div class="purchase-head">
                    <h3 id="purchaseTitle">确认支付</h3>
                    <button type="button" class="purchase-close" id="purchaseCloseBtn">×</button>
                </div>
                <div class="purchase-body">
                    <p class="purchase-summary">${currentGoods ? `购买商品《${currentGoods.goodsName || "未命名商品"}》` : ""}</p>
                    <label class="purchase-field">
                        <span>购买数量</span>
                        <input id="purchaseQuantity" type="number" min="1" max="${stock}" step="1" value="1" ${canBuy ? "" : "disabled"}>
                    </label>
                    <div class="purchase-hint">可选数量范围：1 - ${stock}</div>
                    <div id="purchaseError" class="purchase-error hidden"></div>
                </div>
                <div class="purchase-actions">
                    <button type="button" class="purchase-cancel" id="purchaseCancelBtn">取消</button>
                    <button type="button" class="purchase-confirm" id="purchaseConfirmBtn" ${canBuy ? "" : "disabled"}>确认支付</button>
                </div>
            </div>
        </div>
    `;
}

function setPurchaseError(message) {
    const errorBox = document.getElementById("purchaseError");
    if (!errorBox) {
        return;
    }
    if (!message) {
        errorBox.textContent = "";
        errorBox.classList.add("hidden");
        return;
    }
    errorBox.textContent = message;
    errorBox.classList.remove("hidden");
}

async function submitPurchase() {
    const quantityInput = document.getElementById("purchaseQuantity");
    const confirmBtn = document.getElementById("purchaseConfirmBtn");
    if (!currentGoods || !quantityInput || !confirmBtn) {
        return;
    }

    const quantity = Number.parseInt(quantityInput.value, 10);
    const stock = Number(currentGoods.goodsQuantity || 0);
    if (!Number.isInteger(quantity) || quantity < 1) {
        setPurchaseError("购买数量必须大于0");
        return;
    }
    if (quantity > stock) {
        setPurchaseError("购买数量不能大于库存数");
        return;
    }

    setPurchaseError("");
    confirmBtn.disabled = true;
    confirmBtn.textContent = "处理中...";

    try {
        await api(`/api/goods/${currentGoods.goodsId}/purchase`, {
            method: "POST",
            body: JSON.stringify({ quantity })
        });
        closePurchaseModal();
        alert("支付成功，订单已生成");
        await loadGoodsDetail();
    } catch (err) {
        setPurchaseError(err.message || "支付失败");
    } finally {
        confirmBtn.disabled = false;
        confirmBtn.textContent = "确认支付";
    }
}

function renderGoods(item) {
    currentGoods = item;
    const statusText = item.stock ? "上架" : "下架";
    const imageStyle = item.goodsImage ? `style="background-image:url('${item.goodsImage}')"` : "";
    const shelflifeText = item.shelflife == null ? "无保质期" : `${item.shelflife} 天`;
    const stock = Number(item.goodsQuantity || 0);
    const canBuy = Boolean(item.stock) && stock > 0;

    buyContainer.innerHTML = `
        <div class="buy-grid">
            <div>
                <div class="main-image" ${imageStyle}></div>
            </div>
            <div>
                <h1 class="buy-title">${item.goodsName || "未命名商品"}</h1>
                <div class="buy-price">¥${item.goodsPrice ?? 0}</div>
                <p class="buy-desc">${item.goodsDesc || "暂无商品描述"}</p>

                <ul class="info-list">
                    <li class="info-item"><span class="info-label">商品类型</span><span>${typeMap[item.goodsType] || "未分类"}</span></li>
                    <li class="info-item"><span class="info-label">库存数量</span><span>${item.goodsQuantity ?? 0}</span></li>
                    <li class="info-item"><span class="info-label">发布日期</span><span>${item.goodsDate || "未知"}</span></li>
                    <li class="info-item"><span class="info-label">保质期</span><span>${shelflifeText}</span></li>
                    <li class="info-item"><span class="info-label">商品位置</span><span>${item.goodsLocation || "未知"}</span></li>
                    <li class="info-item"><span class="info-label">卖家学号</span><span>${item.sellerStudentId || item.userId || "未知"}</span></li>
                    <li class="info-item"><span class="info-label">当前状态</span><span>${statusText}</span></li>
                    <li class="info-item"><span class="info-label">浏览次数</span><span>${item.views ?? 0}</span></li>
                </ul>

                <div class="buy-actions">
                    <button id="chatBtn" class="chat-btn" type="button">聊天</button>
                    <button id="buyBtn" class="buy-btn" type="button" ${canBuy ? "" : "disabled"}>${canBuy ? "立即购买" : "已下架"}</button>
                    <span class="buy-tip">${canBuy ? "点击后可选择购买数量" : "商品已下架或库存不足"}</span>
                </div>
                <section class="comment-section">
                    <div class="comment-header">
                        <h2>用户评论</h2>
                        <span>暂无真实评论时显示占位信息</span>
                    </div>
                    <div id="commentList" class="comment-list">
                        <div class="comment-empty">暂无评论，成为首个评论者吧。</div>
                    </div>
                </section>
            </div>
        </div>
        ${renderPurchaseModal(canBuy, stock)}
    `;

    const chatBtn = document.getElementById("chatBtn");
    if (chatBtn) {
        chatBtn.addEventListener("click", () => {
            const sellerId = item.userId;
            const userInfo = JSON.parse(localStorage.getItem("userInfo") || "{}");
            const currentUserId = userInfo.userId;
            if (!sellerId) {
                alert("卖家信息缺失，无法发起聊天");
                return;
            }
            if (currentUserId && Number(currentUserId) === Number(sellerId)) {
                alert("不能和自己发起聊天");
                return;
            }
            const goodsId = item.goodsId || getGoodsIdFromPath();
            window.location.href = `/communication?sellerId=${encodeURIComponent(sellerId)}&goodsId=${encodeURIComponent(goodsId)}`;
        });
    }
    const buyBtn = document.getElementById("buyBtn");
    if (buyBtn) {
        buyBtn.addEventListener("click", () => {
            openPurchaseModal();
        });
    }

    const purchaseModal = document.getElementById("purchaseModal");
    const purchaseMask = document.getElementById("purchaseMask");
    const purchaseCloseBtn = document.getElementById("purchaseCloseBtn");
    const purchaseCancelBtn = document.getElementById("purchaseCancelBtn");
    const purchaseConfirmBtn = document.getElementById("purchaseConfirmBtn");
    const purchaseQuantity = document.getElementById("purchaseQuantity");

    if (purchaseMask) {
        purchaseMask.addEventListener("click", closePurchaseModal);
    }
    if (purchaseCloseBtn) {
        purchaseCloseBtn.addEventListener("click", closePurchaseModal);
    }
    if (purchaseCancelBtn) {
        purchaseCancelBtn.addEventListener("click", closePurchaseModal);
    }
    if (purchaseConfirmBtn) {
        purchaseConfirmBtn.addEventListener("click", submitPurchase);
    }
    if (purchaseQuantity) {
        purchaseQuantity.addEventListener("input", () => setPurchaseError(""));
        purchaseQuantity.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                submitPurchase();
            }
        });
    }

    if (purchaseModal) {
        purchaseModal.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closePurchaseModal();
            }
        });
    }
}

async function loadGoodsDetail() {
    const id = getGoodsIdFromPath();
    if (!id) {
        buyContainer.innerHTML = '<div class="error-box">商品ID无效</div>';
        return;
    }

    try {
        const item = await api(`/api/goods/${id}`);
        renderGoods(item);
    } catch (err) {
        buyContainer.innerHTML = `<div class="error-box">加载失败：${err.message}</div>`;
    }
}

loadGoodsDetail();
