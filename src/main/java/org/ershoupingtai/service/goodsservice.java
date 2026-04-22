package org.ershoupingtai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ershoupingtai.mapper.GoodsMapper;
import org.ershoupingtai.pojo.Goods;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class GoodsService {

	private final GoodsMapper goodsMapper;

	public GoodsService(GoodsMapper goodsMapper) {
		this.goodsMapper = goodsMapper;
	}

	public List<Goods> listGoods(String keyword, String status, Integer goodsType, BigDecimal minPrice, BigDecimal maxPrice) {
		// 使用LambdaQueryWrapper拼接动态查询条件
		LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
		// 只查询未删除商品；兼容老数据中 IsDeleted 为空的情况
		wrapper.and(w -> w.eq(Goods::getIsDeleted, false).or().isNull(Goods::getIsDeleted))
				.orderByDesc(Goods::getGoodsId);

		if (StringUtils.hasText(keyword)) {
			wrapper.and(w -> w.like(Goods::getGoodsName, keyword)
					.or()
					.like(Goods::getGoodsDesc, keyword)
					.or()
					.like(Goods::getGoodsLocation, keyword));
		}
		if (goodsType != null) { wrapper.eq(Goods::getGoodsType, goodsType); }
			if (StringUtils.hasText(status)) {
			if ("上架".equals(status)) {
				wrapper.eq(Goods::getStock, true);
			} else if ("下架".equals(status)) {
				wrapper.eq(Goods::getStock, false);
			}
		}
		if (minPrice != null) {
			wrapper.ge(Goods::getGoodsPrice, minPrice);
		}
		if (maxPrice != null) {
			wrapper.le(Goods::getGoodsPrice, maxPrice);
		}

		return goodsMapper.selectList(wrapper);
	}

	public Goods getById(Long id) {
		Goods item = goodsMapper.selectById(id);
		if (item != null && Boolean.TRUE.equals(item.getIsDeleted())) {
			return null;
		}
		return item;
	}

	public boolean create(Goods item) {
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

	public boolean update(Long id, Goods item) {
		item.setGoodsId(id.intValue());
		return goodsMapper.updateById(item) > 0;
	}

	public boolean delete(Long id) {
		Goods item = new Goods();
		item.setGoodsId(id.intValue());
		item.setIsDeleted(true);
		return goodsMapper.updateById(item) > 0;
	}

	public boolean updateStatus(Long id, String status) {
		Goods item = new Goods();
		item.setGoodsId(id.intValue());
		item.setStock("上架".equals(status));
		return goodsMapper.updateById(item) > 0;
	}
}
