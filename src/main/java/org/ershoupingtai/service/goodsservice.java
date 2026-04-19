package org.ershoupingtai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ershoupingtai.mapper.goodsmapper;
import org.ershoupingtai.pojo.goods;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class goodsservice {

	private final goodsmapper goodsMapper;

	public goodsservice(goodsmapper goodsMapper) {
		this.goodsMapper = goodsMapper;
	}

	public List<goods> listGoods(String keyword, String status, Integer goodsType, BigDecimal minPrice, BigDecimal maxPrice) {
		// 使用LambdaQueryWrapper拼接动态查询条件
		LambdaQueryWrapper<goods> wrapper = new LambdaQueryWrapper<>();
		// 只查询未删除商品；兼容老数据中 IsDeleted 为空的情况
		wrapper.and(w -> w.eq(goods::getIsDeleted, false).or().isNull(goods::getIsDeleted))
				.orderByDesc(goods::getGoodsId);

		if (StringUtils.hasText(keyword)) {
			wrapper.and(w -> w.like(goods::getGoodsName, keyword)
					.or()
					.like(goods::getGoodsDesc, keyword)
					.or()
					.like(goods::getGoodsLocation, keyword));
		}
		if (goodsType != null) { wrapper.eq(goods::getGoodsType, goodsType); }
			if (StringUtils.hasText(status)) {
			if ("上架".equals(status)) {
				wrapper.eq(goods::getStock, true);
			} else if ("下架".equals(status)) {
				wrapper.eq(goods::getStock, false);
			}
		}
		if (minPrice != null) {
			wrapper.ge(goods::getGoodsPrice, minPrice);
		}
		if (maxPrice != null) {
			wrapper.le(goods::getGoodsPrice, maxPrice);
		}

		return goodsMapper.selectList(wrapper);
	}

	public goods getById(Long id) {
		goods item = goodsMapper.selectById(id);
		if (item != null && Boolean.TRUE.equals(item.getIsDeleted())) {
			return null;
		}
		return item;
	}

	public boolean create(goods item) {
		// 统一补充数据库必填字段和默认值
		if (item.getGoodsDate() == null) {
			item.setGoodsDate(LocalDate.now());
		}
		if (item.getViews() == null) {
			item.setViews(0);
		}
		if (item.getStock() == null) {
			item.setStock(true);
		}
		if (item.getIsDeleted() == null) {
			item.setIsDeleted(false);
		}
		return goodsMapper.insert(item) > 0;
	}

	public boolean update(Long id, goods item) {
		item.setGoodsId(id.intValue());
		return goodsMapper.updateById(item) > 0;
	}

	public boolean delete(Long id) {
		goods item = new goods();
		item.setGoodsId(id.intValue());
		item.setIsDeleted(true);
		return goodsMapper.updateById(item) > 0;
	}

	public boolean updateStatus(Long id, String status) {
		goods item = new goods();
		item.setGoodsId(id.intValue());
		item.setStock("上架".equals(status));
		return goodsMapper.updateById(item) > 0;
	}
}
