const goodsGrid = document.getElementById("goodsGrid");
const countText = document.getElementById("countText");
const keywordInput = document.getElementById("keyword");
const goodsTypeInput = document.getElementById("goodsType");
const minPriceInput = document.getElementById("minPrice");
const maxPriceInput = document.getElementById("maxPrice");
const searchBtn = document.getElementById("searchBtn");
const resetBtn = document.getElementById("resetBtn");
const logoutBtn = document.getElementById("logoutBtn");

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

function buildQuery() {
    const params = new URLSearchParams();
    if (keywordInput.value.trim()) {
        params.append("keyword", keywordInput.value.trim());
    }
    if (goodsTypeInput.value !== "") {
        params.append("goodsType", goodsTypeInput.value);
    }
    if (minPriceInput.value) {
        params.append("minPrice", minPriceInput.value);
    }
    if (maxPriceInput.value) {
        params.append("maxPrice", maxPriceInput.value);
    }
    const query = params.toString();
    return query ? `?${query}` : "";
}

async function api(url) {
    const response = await fetch(url);
    const data = await response.json();
    if (data.code !== 200) {
        throw new Error(data.msg || "请求失败");
    }
    return data.data;
}

function renderGoods(list) {
    countText.textContent = `${list.length} 件`;

    if (!list.length) {
        goodsGrid.innerHTML = '<div class="empty-box">没有筛选到商品，换个价格区间或种类再试试。</div>';
        return;
    }

    goodsGrid.innerHTML = list.map(item => {
        const image = item.goodsImage ? `style="background-image:url('${item.goodsImage}')"` : "";
        const statusText = item.stock ? "上架" : "下架";
        const statusClass = item.stock ? "status-tag" : "status-tag off";
        return `
            <article class="goods-card">
                <a class="goods-image-link" href="/buy/${item.goodsId}">
                    <div class="goods-image" ${image}></div>
                </a>
                <div class="goods-info">
                    <h3 class="goods-title">${item.goodsName || "未命名商品"}</h3>
                    <div class="goods-meta">${typeMap[item.goodsType] || "未分类"} · ${item.goodsLocation || "未知地点"}</div>
                    <div class="goods-meta">${item.goodsDesc || "暂无描述"}</div>
                    <div class="goods-bottom">
                        <div class="goods-price">¥${item.goodsPrice ?? 0}</div>
                        <span class="${statusClass}">${statusText}</span>
                    </div>
                </div>
            </article>
        `;
    }).join("");
}

async function loadGoods() {
    try {
        const list = await api(`/api/goods${buildQuery()}`);
        renderGoods(list);
    } catch (err) {
        goodsGrid.innerHTML = `<div class="empty-box">加载失败：${err.message}</div>`;
    }
}

function resetFilters() {
    keywordInput.value = "";
    goodsTypeInput.value = "";
    minPriceInput.value = "";
    maxPriceInput.value = "";
    loadGoods();
}

searchBtn.addEventListener("click", loadGoods);
resetBtn.addEventListener("click", resetFilters);

if (logoutBtn) {
    logoutBtn.addEventListener("click", async () => {
        try {
            await fetch("/api/auth/logout", { method: "POST" });
        } finally {
            window.location.href = "/login";
        }
    });
}

loadGoods();
