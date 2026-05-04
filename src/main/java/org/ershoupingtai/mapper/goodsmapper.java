package org.ershoupingtai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.ershoupingtai.pojo.Goods;

@Mapper
// 继承BaseMapper后可直接使用通用CRUD，无需手写XML
public interface GoodsMapper extends BaseMapper<Goods> {

    @Select("SELECT g.GoodsId AS goodsId, " +
            "g.GoodsImage AS goodsImage, " +
            "g.GoodsName AS goodsName, " +
            "g.GoodsType AS goodsType, " +
            "g.GoodsDescription AS goodsDesc, " +
            "g.Price AS goodsPrice, " +
            "g.GoodsQuantity AS goodsQuantity, " +
            "g.GoodsDate AS goodsDate, " +
            "g.Shelflife AS shelflife, " +
            "g.Views AS views, " +
            "g.GoodsLocation AS goodsLocation, " +
            "g.UserId AS userId, " +
            "g.Stock AS stock, " +
            "g.IsDeleted AS isDeleted, " +
            "u.UserName AS sellerStudentId " +
            "FROM dbo.Goods g LEFT JOIN dbo.UserLogin u ON g.UserId = u.UserId " +
            "WHERE g.GoodsId = #{id}")
    Goods selectByIdWithSellerStudentId(Long id);
}
