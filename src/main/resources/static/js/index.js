const goodsTbody = document.getElementById("goodsTbody");
const countText = document.getElementById("countText");
const goodsModal = document.getElementById("goodsModal");
const modalTitle = document.getElementById("modalTitle");
const goodsForm = document.getElementById("goodsForm");
const totalCount = document.getElementById("totalCount");
const saleCount = document.getElementById("saleCount");

const openCreateBtn = document.getElementById("openCreateBtn");
const closeModalBtn = document.getElementById("closeModalBtn");
const cancelBtn = document.getElementById("cancelBtn");
const searchBtn = document.getElementById("searchBtn");
const resetBtn = document.getElementById("resetBtn");

const keywordInput = document.getElementById("keyword");
const statusInput = document.getElementById("status");
const searchTypeInput = document.getElementById("searchType");
const minPriceInput = document.getElementById("minPrice");
const maxPriceInput = document.getElementById("maxPrice");

const goodsId = document.getElementById("goodsId");
const goodsImage = document.getElementById("goodsImage");
const goodsImageInput = document.getElementById("goodsImageInput");
const uploadImageBtn = document.getElementById("uploadImageBtn");
const uploadImageName = document.getElementById("uploadImageName");
const uploadPreview = document.getElementById("uploadPreview");
const uploadPreviewImg = document.getElementById("uploadPreviewImg");
const uploadPreviewText = document.getElementById("uploadPreviewText");
const uploadError = document.getElementById("uploadError");
const goodsName = document.getElementById("goodsName");
const goodsType = document.getElementById("goodsType");
const goodsDesc = document.getElementById("goodsDesc");
const goodsPrice = document.getElementById("goodsPrice");
const goodsQuantity = document.getElementById("goodsQuantity");
const goodsDate = document.getElementById("goodsDate");
const shelflife = document.getElementById("shelflife");
const noShelflife = document.getElementById("noShelflife");
const goodsLocation = document.getElementById("goodsLocation");
const userId = document.getElementById("userId");
const goodsStatus = document.getElementById("goodsStatus");

let selectedImageFile = null;

noShelflife.addEventListener("change", () => {
	shelflife.disabled = noShelflife.checked;
	if (noShelflife.checked) {
		shelflife.value = "";
		shelflife.removeAttribute("required");
	} else {
		shelflife.setAttribute("required", "required");
	}
});

function buildQuery() {
	// 将筛选条件拼接为查询参数，空值不�?
	const params = new URLSearchParams();
	if (keywordInput.value.trim()) params.append("keyword", keywordInput.value.trim());
	if (statusInput.value) params.append("status", statusInput.value);
	if (searchTypeInput.value !== "") params.append("goodsType", searchTypeInput.value);
	if (minPriceInput.value) params.append("minPrice", minPriceInput.value);
	if (maxPriceInput.value) params.append("maxPrice", maxPriceInput.value);
	const qs = params.toString();
	return qs ? `?${qs}` : "";
}

async function api(url, options = {}) {
	// 统一接口调用与错误处理，减少重复代码
	const headers = options.body instanceof FormData ? {} : {
		"Content-Type": "application/json"
	};
	const response = await fetch(url, {
		headers,
		...options
	});
	let data;
	try {
		data = await response.json();
	} catch (err) {
		throw new Error("内部服务器错�?500) �?返回格式异常");
	}
	if (data.code !== 200) {
		throw new Error(data.msg || "请求失败");
	}
	return data.data;
}

async function uploadImageFile(file) {
	const formData = new FormData();
	formData.append("file", file);
	return api("/api/upload/image", {
		method: "POST",
		body: formData
	});
}

function renderLocalPreview(file) {
	const reader = new FileReader();
	reader.onload = () => {
		uploadPreviewImg.src = reader.result;
		uploadPreviewImg.classList.remove("hidden");
		uploadPreviewText.classList.add("hidden");
	};
	reader.readAsDataURL(file);
}

function setUploadError(message) {
	if (uploadError) {
		uploadError.textContent = message || "上传失败，请重试";
		uploadError.classList.remove("hidden");
	}
}

function clearUploadError() {
	if (uploadError) {
		uploadError.textContent = "";
		uploadError.classList.add("hidden");
	}
}

function statusClass(status) {
	if (status === "上架") return "status-pill status-sale";
	return "status-pill status-off";
}

function renderRows(list) {
	countText.textContent = `${list.length} 条`;
	totalCount.textContent = list.length;
	saleCount.textContent = list.filter(item => item.stock).length;
	if (!list.length) {
		goodsTbody.innerHTML = `<tr><td colspan="13">暂无数据，先新增一条商品吧�?/td></tr>`;
		return;
	}

	goodsTbody.innerHTML = list.map(item => `
		<tr>
			<td>${item.goodsId ?? "-"}</td>
			<td>${item.goodsImage ? `<span class="thumb-box has-image" style="background-image:url('${item.goodsImage}')">�?/span>` : `<span class="thumb-box">无图</span>`}</td>
			<td>${item.goodsName ?? "-"}</td>
			<td>${{1:"电子数码", 2:"书籍教材", 3:"生活用品", 4:"美妆服饰", 5:"运动器材", 6:"交通代步", 7:"二次元/手办", 8:"票务卡券", 9:"虚拟物品", 10:"食品零食", 0:"其他"}[item.goodsType] ?? "未分类"}</td>
			<td>${item.goodsDesc ?? "-"}</td>
			<td>¥${item.goodsPrice ?? 0}</td>
			<td>${item.goodsQuantity ?? 0}</td>
			<td>${item.goodsDate ?? "-"}</td>
			<td>${item.shelflife == null ? "无保质期" : item.shelflife + "天"}</td>
			<td>${item.goodsLocation ?? "-"}</td>
			<td>${item.userId ?? "-"}</td>
			<td><span class="${statusClass(item.stock ? "上架" : "下架")}">${item.stock ? "上架" : "下架"}</span></td>
			<td>
				<div class="ops">
					<button class="small-btn" data-action="edit" data-id="${item.goodsId}">编辑</button>
					<button class="small-btn" data-action="toggle" data-id="${item.goodsId}" data-status="${item.stock ? "上架" : "下架"}">
						${item.stock ? "下架" : "上架"}
					</button>
					<button class="danger-btn" data-action="delete" data-id="${item.goodsId}">删除</button>
				</div>
			</td>
		</tr>
	`).join("");
}

async function loadGoods() {
	// 刷新列表入口：初始化、查询后、增删改后都复用这里
	try {
		const list = await api(`/api/goods${buildQuery()}`);
		renderRows(list);
	} catch (e) {
		alert(e.message);
	}
}

function openModal(isEdit = false) {
	modalTitle.textContent = isEdit ? "编辑商品" : "新增商品";
	goodsModal.classList.remove("hidden");
}

function closeModal() {
	goodsModal.classList.add("hidden");
	goodsForm.reset();
	goodsId.value = "";
	goodsStatus.value = "上架";
	shelflife.disabled = false;
	shelflife.setAttribute("required", "required");
	clearUploadField();
}

function fillForm(item) {
	goodsId.value = item.goodsId || "";
	goodsImage.value = item.goodsImage || "";
	refreshUploadPreview(item.goodsImage || "", item.goodsImage ? "已选择图片" : "未选择图片");
	goodsName.value = item.goodsName || "";
	goodsType.value = item.goodsType ?? "0";
	goodsDesc.value = item.goodsDesc || "";
	goodsPrice.value = item.goodsPrice || "";
	goodsQuantity.value = item.goodsQuantity || "";
	goodsDate.value = item.goodsDate || "";
	if (item.shelflife == null) {
		noShelflife.checked = true;
		shelflife.value = "";
		shelflife.disabled = true;
		shelflife.removeAttribute("required");
	} else {
		noShelflife.checked = false;
		shelflife.value = item.shelflife;
		shelflife.disabled = false;
		shelflife.setAttribute("required", "required");
	}
	goodsLocation.value = item.goodsLocation || "";
	userId.value = item.userId || "";
	goodsStatus.value = item.stock ? "上架" : "下架";
}

function refreshUploadPreview(imageUrl, nameText) {
	uploadImageName.textContent = nameText || "未选择图片";
	if (imageUrl) {
		uploadPreviewImg.src = imageUrl;
		uploadPreviewImg.classList.remove("hidden");
		uploadPreviewText.classList.add("hidden");
	} else {
		uploadPreviewImg.removeAttribute("src");
		uploadPreviewImg.classList.add("hidden");
		uploadPreviewText.classList.remove("hidden");
	}
}

function clearUploadField() {
	goodsImage.value = "";
	goodsImageInput.value = "";
	selectedImageFile = null;
	refreshUploadPreview("", "未选择图片");
	clearUploadError();
}

function getPayload() {
	// 收集表单字段并与后端goods对象保持同名
	return {
		goodsImage: goodsImage.value.trim(),
		goodsName: goodsName.value.trim(),
		goodsType: goodsType.value ? Number(goodsType.value) : null,
		goodsDesc: goodsDesc.value.trim(),
		goodsPrice: Number(goodsPrice.value),
		goodsQuantity: Number(goodsQuantity.value),
		goodsDate: goodsDate.value,
		shelflife: noShelflife.checked ? null : (shelflife.value === "" ? null : Number(shelflife.value)),
		goodsLocation: goodsLocation.value.trim(),
		userId: Number(userId.value),
		stock: goodsStatus.value === "上架"
	};
}

async function createOrUpdate(e) {
	e.preventDefault();

	if (selectedImageFile) {
		try {
			uploadImageName.textContent = `上传�?..`;
			const imageUrl = await uploadImageFile(selectedImageFile);
			goodsImage.value = imageUrl;
			selectedImageFile = null;
		} catch (error) {
			setUploadError("图片上传失败：" + error.message);
			uploadImageName.textContent = `已选择�?{selectedImageFile.name}`;
			return; // 终止保存动作
		}
	}

	const payload = getPayload();
        const id = goodsId.value;

	try {
		// 有id走编辑，无id走新�?
		if (id) {
			await api(`/api/goods/${id}`, {
				method: "PUT",
				body: JSON.stringify(payload)
			});
		} else {
			await api("/api/goods", {
				method: "POST",
				body: JSON.stringify(payload)
			});
		}
		closeModal();
		await loadGoods();
	} catch (e2) {
		alert(e2.message);
	}
}

async function handleTableClick(e) {
	// 使用事件委托统一处理行内按钮（编�?上下�?删除�?
	const target = e.target;
	const id = target.getAttribute("data-id");
	const action = target.getAttribute("data-action");
	if (!id || !action) return;

	try {
		if (action === "edit") {
			const item = await api(`/api/goods/${id}`);
			fillForm(item);
			openModal(true);
			return;
		}

		if (action === "delete") {
			if (!confirm("确认删除该商品吗?")) return;
			await api(`/api/goods/${id}`, { method: "DELETE" });
			await loadGoods();
			return;
		}

		if (action === "toggle") {
			const oldStatus = target.getAttribute("data-status");
			const nextStatus = oldStatus === "下架" ? "上架" : "下架";
			await api(`/api/goods/${id}/status?status=${encodeURIComponent(nextStatus)}`, {
				method: "PATCH"
			});
			await loadGoods();
		}
	} catch (err) {
		alert(err.message);
	}
}

function resetSearch() {
	keywordInput.value = "";
	statusInput.value = "";
	searchTypeInput.value = "";
	minPriceInput.value = "";
	maxPriceInput.value = "";
	loadGoods();
}

openCreateBtn.addEventListener("click", () => {
	closeModal();
	openModal(false);
});
closeModalBtn.addEventListener("click", closeModal);
cancelBtn.addEventListener("click", closeModal);
searchBtn.addEventListener("click", loadGoods);
resetBtn.addEventListener("click", resetSearch);
goodsForm.addEventListener("submit", createOrUpdate);
goodsTbody.addEventListener("click", handleTableClick);

goodsImageInput.addEventListener("change", async () => {
	const file = goodsImageInput.files && goodsImageInput.files[0];
	if (!file) {
		return;
	}

	clearUploadError();
	selectedImageFile = file;
	renderLocalPreview(file);
	uploadImageName.textContent = `已选择�?{file.name}`;
});

loadGoods();
