package org.ershoupingtai.mapper.adminMapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.ershoupingtai.pojo.Goods;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminGoodsMapper extends BaseMapper<Goods> {

    @Select("SELECT COUNT(*) FROM dbo.Goods WHERE IsDeleted = 0")
    Long countOnSaleGoods();

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
            "u.UserName AS sellerStudentId, " +
            "g.Stock AS stock, " +
            "g.IsDeleted AS isDeleted " +
            "FROM dbo.Goods g LEFT JOIN dbo.UserLogin u ON g.UserId = u.UserId " +
            "WHERE g.IsDeleted = 0 ORDER BY g.GoodsId DESC")
    List<Goods> findAllWithSeller();

        @Select("<script>" +
            "SELECT g.GoodsId AS goodsId, " +
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
            "u.UserName AS sellerStudentId, " +
            "g.Stock AS stock, " +
            "g.IsDeleted AS isDeleted " +
            "FROM dbo.Goods g LEFT JOIN dbo.UserLogin u ON g.UserId = u.UserId " +
            "<where> g.IsDeleted = 0 " +
            "<if test='goodsId != null'> AND g.GoodsId = #{goodsId} </if>" +
            "<if test='goodsName != null and goodsName != &quot;&quot;'> AND g.GoodsName LIKE '%' + #{goodsName} + '%' </if>" +
            "<if test='sellerId != null'> AND g.UserId = #{sellerId} </if>" +
            "</where> ORDER BY g.GoodsId DESC" +
            "</script>")
        List<Goods> searchGoods(@Param("goodsId") Integer goodsId,
                    @Param("goodsName") String goodsName,
                    @Param("sellerId") Integer sellerId);


    @Update("UPDATE Goods SET Stock = #{stock} WHERE GoodsId = #{goodsId}")
    int updateStock(@Param("goodsId") Integer goodsId, @Param("stock") Boolean stock);

    @Update("UPDATE Goods SET IsDeleted = 1 WHERE GoodsId = #{goodsId}")
    int softDelete(@Param("goodsId") Integer goodsId);
}
