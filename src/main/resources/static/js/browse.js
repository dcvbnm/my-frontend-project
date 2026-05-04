const goodsGrid = document.getElementById("goodsGrid");
const recommendGrid = document.getElementById("recommendGrid");
const countText = document.getElementById("countText");
const keywordInput = document.getElementById("keyword");
const goodsTypeInput = document.getElementById("goodsType");
const minPriceInput = document.getElementById("minPrice");
const maxPriceInput = document.getElementById("maxPrice");
const searchBtn = document.getElementById("searchBtn");
const filterBtn = document.getElementById("filterBtn");
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

// 轮播相关
let currentSlide = 0;
const slides = document.querySelectorAll('.carousel-slide');
const indicators = document.querySelectorAll('.indicator');

function showSlide(index) {
    slides.forEach((slide, i) => {
        slide.classList.toggle('active', i === index);
    });
    indicators.forEach((indicator, i) => {
        indicator.classList.toggle('active', i === index);
    });
}

function nextSlide() {
    currentSlide = (currentSlide + 1) % slides.length;
    showSlide(currentSlide);
}

// 自动轮播
setInterval(nextSlide, 5000);

// 指示器点击
indicators.forEach((indicator, index) => {
    indicator.addEventListener('click', () => {
        currentSlide = index;
        showSlide(currentSlide);
    });
});

// 分类点击
document.querySelectorAll('.category-item').forEach(item => {
    item.addEventListener('click', (e) => {
        e.preventDefault();
        const type = item.dataset.type;
        goodsTypeInput.value = type;
        loadGoods();
        // 滚动到全部商品区域
        document.querySelector('.all-goods-section').scrollIntoView({ behavior: 'smooth' });
    });
});

function buildQuery() {
    const params = new URLSearchParams();
    params.append("status", "上架");
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

function renderGoods(list, gridElement, limit = 0) {
    const displayList = limit > 0 ? list.slice(0, limit) : list;
    
    if (gridElement === goodsGrid) {
        countText.textContent = `${list.length} 件`;
    }

    if (!displayList.length) {
        gridElement.innerHTML = '<div class="empty-box">没有筛选到商品，换个价格区间或种类再试试。</div>';
        return;
    }

    gridElement.innerHTML = displayList.map(item => {
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
        renderGoods(list, goodsGrid);
    } catch (err) {
        goodsGrid.innerHTML = `<div class="empty-box">加载失败：${err.message}</div>`;
    }
}

async function loadRecommend() {
    try {
        // 加载推荐商品（前8个）
        const list = await api('/api/goods?status=上架');
        renderGoods(list, recommendGrid, 8);
    } catch (err) {
        recommendGrid.innerHTML = `<div class="empty-box">加载失败：${err.message}</div>`;
    }
}

function resetFilters() {
    keywordInput.value = "";
    goodsTypeInput.value = "";
    minPriceInput.value = "";
    maxPriceInput.value = "";
    loadGoods();
}

// 初始化
loadRecommend();
loadGoods();

searchBtn.addEventListener("click", loadGoods);
filterBtn.addEventListener("click", loadGoods);
resetBtn.addEventListener("click", resetFilters);

// 退出登录
logoutBtn.addEventListener("click", () => {
    if (confirm("确定要退出登录吗？")) {
        // 清除cookie或session
        document.cookie = "auth_cookie=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        window.location.href = "/user/login";
    }
});

if (logoutBtn) {
    logoutBtn.addEventListener("click", async () => {
        try {
            await fetch("/api/auth/logout", { method: "POST" });
        } finally {
            window.location.href = "/user/login";
        }
    });
}

loadGoods();
