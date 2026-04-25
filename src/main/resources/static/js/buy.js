const buyContainer = document.getElementById("buyContainer");

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

async function api(url) {
    const response = await fetch(url);
    const data = await response.json();
    if (data.code !== 200) {
        throw new Error(data.msg || "请求失败");
    }
    return data.data;
}

function renderGoods(item) {
    const statusText = item.stock ? "上架" : "下架";
    const imageStyle = item.goodsImage ? `style="background-image:url('${item.goodsImage}')"` : "";
    const shelflifeText = item.shelflife == null ? "无保质期" : `${item.shelflife} 天`;

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
                    <button id="buyBtn" class="buy-btn" type="button">立即购买</button>
                    <span class="buy-tip">购买后流程由其他模块处理</span>
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
    `;

    const chatBtn = document.getElementById("chatBtn");
    if (chatBtn) {
        chatBtn.addEventListener("click", () => {
            window.location.href = "/communication";
        });
    }
    const buyBtn = document.getElementById("buyBtn");
    if (buyBtn) {
        buyBtn.addEventListener("click", () => {
            alert("已收到购买请求，后续流程由交易模块处理。");
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
