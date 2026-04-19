package org.ershoupingtai.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.ershoupingtai.admin.entity.Goods;

import java.util.List;
import java.util.Map;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    @Select("SELECT g.*, u.UserName AS sellerName FROM Goods g LEFT JOIN UserLogin u ON g.UserId = u.UserId WHERE g.IsDeleted = 0 ORDER BY g.GoodsId DESC")
    List<Goods> findAllWithSeller();

    @Select("SELECT TOP 10 COALESCE(gc.CategoryName, '分类' + CONVERT(VARCHAR, g.GoodsType)) AS categoryName, COUNT(*) AS goodsCount " +
            "FROM Goods g LEFT JOIN GoodsCategory gc ON g.GoodsType = gc.CategoryId " +
            "WHERE g.IsDeleted = 0 GROUP BY COALESCE(gc.CategoryName, '分类' + CONVERT(VARCHAR, g.GoodsType)) " +
            "ORDER BY goodsCount DESC")
    List<Map<String, Object>> findTopCategoryStats();

    @Update("UPDATE Goods SET Stock = #{stock} WHERE GoodsId = #{goodsId}")
    int updateStock(@Param("goodsId") Integer goodsId, @Param("stock") Boolean stock);

    @Update("UPDATE Goods SET IsDeleted = 1 WHERE GoodsId = #{goodsId}")
    int softDelete(@Param("goodsId") Integer goodsId);
}
