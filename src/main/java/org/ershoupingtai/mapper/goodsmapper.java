package org.ershoupingtai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.ershoupingtai.pojo.Goods;

@Mapper
// 继承BaseMapper后可直接使用通用CRUD，无需手写XML
public interface GoodsMapper extends BaseMapper<Goods> {

    @Select("SELECT g.*, u.UserName AS sellerStudentId FROM Goods g LEFT JOIN UserLogin u ON g.UserId = u.UserId WHERE g.GoodsId = #{id}")
    Goods selectByIdWithSellerStudentId(Long id);
}
