package com.PhilJ.search.service;

public interface ESManagerService {

    /**
     * 创建索引库结构
     */
    void createMappingAndIndex();

    /**
     * 导入所有数据进es
     */
    void importAll();

    /**
     * 根据spuid查询skuList,再导入es
     */
    void importDataBySpuId(String spuId);

    /**
     * 根据spuid删除es索引库中相关的sku数据
     */
    void delDataBySpuId(String spuId);
}
