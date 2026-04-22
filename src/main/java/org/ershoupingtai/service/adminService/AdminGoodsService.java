package org.ershoupingtai.service.adminService;

import org.ershoupingtai.pojo.Goods;
import org.ershoupingtai.mapper.adminMapper.GoodsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminGoodsService {

    private final GoodsMapper goodsMapper;

    public AdminGoodsService(GoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    public List<Goods> getGoodsList() {
        return goodsMapper.findAllWithSeller();
    }

    public void updateGoodsStock(Integer id, boolean stock) {
        goodsMapper.updateStock(id, stock);
    }

    public void softDeleteGoods(Integer id) {
        goodsMapper.softDelete(id);
    }
}