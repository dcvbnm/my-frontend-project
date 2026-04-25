package org.ershoupingtai.controller;

import org.ershoupingtai.common.Result;
import org.ershoupingtai.common.ResultCode;
import org.ershoupingtai.pojo.Goods;
import org.ershoupingtai.service.GoodsService;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Controller
public class goodscontroller {
	private static final String UPLOAD_DIR_NAME = "uploads";

	private final GoodsService goodsService;

	public goodscontroller(GoodsService goodsService) {
		this.goodsService = goodsService;
	}

	@ResponseBody
	@ExceptionHandler(Exception.class)
	public Result<Void> handleException(Exception e) {
		String msg = e.toString();
		Throwable cause = e.getCause();
		while(cause != null) {
			msg += " " + cause.toString();
			cause = cause.getCause();
		}
		if (msg.contains("REFERENCE constraint") || msg.contains("FOREIGN KEY") || msg.contains("外键约束")) {
			return Result.fail("数据保存失败，因用户ID等外键不存在！");
		}
		if (msg.contains("Failed to obtain JDBC Connection") || msg.contains("登录失败") || msg.contains("CannotGetJdbcConnectionException")) {
			return Result.fail("数据库连接失败，请检查数据库配置、密码状态！");
		}
		e.printStackTrace();
		return Result.fail("系统异常保存失败，请检查填写内容或系统设置");
	}

	@GetMapping({"/", "/goods"})
	public String goodsPage() {
		// 商品管理页入口（模板：index.html）
		return "index";
	}

	@GetMapping("/browse")
	public String browsePage() {
		// 商品浏览页入口（模板：browse.html）
		return "browse";
	}

	@GetMapping("/buy/{id}")
	public String buyPage(@PathVariable Long id) {
		// 商品购买详情页入口（模板：buy.html）
		return "buy";
	}

	@GetMapping("/communication")
	public String communicationPage() {
		// 买卖沟通页入口（模板：communication.html）
		return "communication";
	}

	@ResponseBody
	@GetMapping("/api/goods")
	public Result<List<Goods>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Integer goodsType,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice
	) {
		// 支持按关键词、状态、分类、价格区间筛选
		return Result.success(goodsService.listGoods(keyword, status, goodsType, minPrice, maxPrice));
	}

	@ResponseBody
	@GetMapping("/api/goods/{id}")
	public Result<Goods> getOne(@PathVariable Long id) {
		Goods item = goodsService.getById(id);
		if (item == null) {
			return Result.fail(ResultCode.DATA_NOT_FOUND);
		}
		return Result.success(item);
	}

	@ResponseBody
	@PostMapping("/api/goods")
	public Result<Void> create(@RequestBody Goods item) {
		// Goods表是典型的强约束表，新增时必须校验完整必填字段
		Result<Void> checkResult = validateGoods(item, false);
		if (checkResult != null) {
			return checkResult;
		}
		return goodsService.create(item) ? Result.success() : Result.fail(ResultCode.SYSTEM_ERROR);
	}

	@ResponseBody
	@PutMapping("/api/goods/{id}")
	public Result<Void> update(@PathVariable Long id, @RequestBody Goods item) {
		// 编辑时允许部分字段更新，但传入字段必须合法
		Result<Void> checkResult = validateGoods(item, true);
		if (checkResult != null) {
			return checkResult;
		}
		return goodsService.update(id, item) ? Result.success() : Result.fail(ResultCode.SYSTEM_ERROR);
	}

	@ResponseBody
	@DeleteMapping("/api/goods/{id}")
	public Result<Void> delete(@PathVariable Long id) {
		return goodsService.delete(id) ? Result.success() : Result.fail(ResultCode.DATA_NOT_FOUND);
	}

	@ResponseBody
	@PatchMapping("/api/goods/{id}/status")
	public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
		// 上下架直接映射到 Stock 字段，避免前端手动改整条记录
		if (!StringUtils.hasText(status)) {
			return Result.fail("状态不能为空");
		}
		return goodsService.updateStatus(id, status) ? Result.success() : Result.fail(ResultCode.SYSTEM_ERROR);
	}

	@ResponseBody
	@PostMapping("/api/upload/image")
	public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return Result.fail("请选择要上传的图片");
		}

		String originalName = file.getOriginalFilename();
		if (!StringUtils.hasText(originalName)) {
			return Result.fail("图片文件名不能为空");
		}

		String lowerName = originalName.toLowerCase();
		if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg") && !lowerName.endsWith(".png")
				&& !lowerName.endsWith(".gif") && !lowerName.endsWith(".webp") && !lowerName.endsWith(".bmp")) {
			return Result.fail("仅支持 jpg、jpeg、png、gif、webp、bmp 格式");
		}

		try {
			Path uploadDir = Paths.get(System.getProperty("user.dir"), UPLOAD_DIR_NAME);
			if (!Files.exists(uploadDir)) {
				Files.createDirectories(uploadDir);
			}

			String suffix = originalName.substring(originalName.lastIndexOf('.'));
			String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
			Path targetFile = uploadDir.resolve(fileName);
			file.transferTo(targetFile.toFile());

			return Result.success("/uploads/" + fileName);
		} catch (IOException e) {
			return Result.fail("图片上传失败");
		}
	}

	private Result<Void> validateGoods(Goods item, boolean allowPartialUpdate) {
		if (item == null) {
			return Result.fail("请求体不能为空");
		}

		// allowPartialUpdate=true 时，仅校验本次请求里出现的字段
		if (!allowPartialUpdate || item.getGoodsName() != null) {
			if (!StringUtils.hasText(item.getGoodsName())) { 
				return Result.fail("商品名称不能为空");
			}
		}
		if (!allowPartialUpdate || item.getGoodsPrice() != null) {
			if (item.getGoodsPrice() == null || item.getGoodsPrice().compareTo(BigDecimal.ZERO) < 0) {
				return Result.fail("商品价格必须大于等于0");
			}
		}
		if (!allowPartialUpdate || item.getGoodsImage() != null) {
			if (!StringUtils.hasText(item.getGoodsImage())) {
				return Result.fail("商品图片不能为空");
			}
		}
		if (!allowPartialUpdate || item.getGoodsDesc() != null) {
			if (!StringUtils.hasText(item.getGoodsDesc())) {
				return Result.fail("商品描述不能为空");
			}
		}
		if (!allowPartialUpdate || item.getGoodsQuantity() != null) {
			if (item.getGoodsQuantity() == null || item.getGoodsQuantity() < 0) {
				return Result.fail("商品数量必须大于等于0");
			}
		}
		if (item.getShelflife() != null && item.getShelflife() < 0) {
			return Result.fail("保质期必须大于等于0");
		}
		if (!allowPartialUpdate || item.getGoodsLocation() != null) {
			if (!StringUtils.hasText(item.getGoodsLocation())) {
				return Result.fail("商品所在地不能为空");
			}
		}
		// 创建商品时必须验证用户ID，编辑时如果提供了userId也要验证
		if (!allowPartialUpdate || item.getUserId() != null) {
			if (item.getUserId() == null || item.getUserId() <= 0) {
				return Result.fail("用户ID不能为空");
			}
		}
		return null;
	}
}
