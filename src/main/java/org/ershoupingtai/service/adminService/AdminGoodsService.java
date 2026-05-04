package org.ershoupingtai.service.adminService;

import org.ershoupingtai.pojo.Goods;
import org.ershoupingtai.mapper.adminMapper.AdminGoodsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminGoodsService {

    private final AdminGoodsMapper goodsMapper;

    public AdminGoodsService(AdminGoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    public List<Goods> getGoodsList() {
        return goodsMapper.searchGoods(null, null, null);
    }

    public List<Goods> searchGoods(Integer goodsId, String goodsName, Integer sellerId) {
        return goodsMapper.searchGoods(goodsId, goodsName, sellerId);
    }

    public void updateGoodsStock(Integer id, boolean stock) {
        goodsMapper.updateStock(id, stock);
    }

    public void softDeleteGoods(Integer id) {
        goodsMapper.softDelete(id);
    }
}