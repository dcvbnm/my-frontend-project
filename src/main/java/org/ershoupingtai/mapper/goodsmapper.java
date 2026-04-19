package org.ershoupingtai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ershoupingtai.pojo.goods;

@Mapper
// 继承BaseMapper后可直接使用通用CRUD，无需手写XML
public interface goodsmapper extends BaseMapper<goods> {
}
