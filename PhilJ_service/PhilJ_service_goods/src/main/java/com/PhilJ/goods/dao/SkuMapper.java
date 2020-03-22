package com.PhilJ.goods.dao;

import com.PhilJ.goods.pojo.Sku;
//import com.PhilJ.order.pojo.OrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

public interface SkuMapper extends Mapper<Sku> {

//    /**
//     * 库存减少，销量增加
//     * @param orderItem
//     * @return
//     */
//    @Update("update tb_sku set num=num-#{num},sale_num=sale_num+#{num} where id=#{skuId} and num>=#{num}")
//    int decrCount(OrderItem orderItem);

    /**
     * 库存增加，销量减少(回滚库存)
     * @param skuId
     * @param num
     * @return
     */
    @Update("update tb_sku set num=num+#{num} ,sale_num=sale_num-#{num} where id=#{skuId}")
    int resumeStockNum(@Param("skuId") String skuId, @Param("num") Integer num);

}
