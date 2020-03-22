package com.PhilJ.goods.dao;

import com.PhilJ.goods.pojo.Brand;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface BrandMapper extends Mapper<Brand> {

    /**
     * 根据分类名称查询品牌名称
     * @param categoryName
     * @return List
     */
    @Select("SELECT name,image FROM tb_brand where id in(SELECT brand_id FROM tb_category_brand WHERE category_id in(SELECT id FROM tb_category where name=#{categoryName}))")
    public List<Map> findBrandListByCategoryName(@Param("categoryName") String categoryName);
}
