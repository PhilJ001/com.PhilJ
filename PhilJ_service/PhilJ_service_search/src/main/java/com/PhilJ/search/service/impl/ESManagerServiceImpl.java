package com.PhilJ.search.service.impl;

import com.alibaba.fastjson.JSON;


import com.PhilJ.goods.feign.SkuFeign;
import com.PhilJ.goods.pojo.Sku;
import com.PhilJ.search.dao.ESManagerMapper;
import com.PhilJ.search.pojo.SkuInfo;
import com.PhilJ.search.service.ESManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ESManagerServiceImpl implements ESManagerService {

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private ESManagerMapper esManagerMapper;

    /**
     * 创建索引库结构
     */
    @Override
    public void createMappingAndIndex() {
        //创建索引
        elasticsearchTemplate.createIndex(SkuInfo.class);
        //创建映射
        elasticsearchTemplate.putMapping(SkuInfo.class);
    }

    /**
     * 导入所有数据进es
     */
    @Override
    public void importAll() {
//        //查询sku集合
//        List<Sku> skuList = skuFeign.findSkuListBySpuId("all");
//        if (skuList == null || skuList.size()<=0){
//            throw new RuntimeException("当前没有数据被查询到,无法导入索引库");
//        }
//
//        //skulist转换为json
//        String jsonSkuList = JSON.toJSONString(skuList);
//        //将json转换为skuinfo
//        List<SkuInfo> skuInfoList = JSON.parseArray(jsonSkuList, SkuInfo.class);
//
//        for (SkuInfo skuInfo : skuInfoList) {
//            //将规格信息转换为map
//            Map specMap = JSON.parseObject(skuInfo.getSpec(), Map.class);
//            skuInfo.setSpecMap(specMap);
//        }
//
//        //导入索引库
//        esManagerMapper.saveAll(skuInfoList);

        //查询sku集合
        List<Sku> skuList = skuFeign.findSkuListBySpuId("all");
        if (skuList == null || skuList.size() <= 0){
            throw new RuntimeException("当前没有数据被查询到,无法导入索引库");
        }
        //skuList转换为Json
        String jsonSkuList = JSON.toJSONString(skuList);
        //将json转换为skuList<skuInfo>集合
        List<SkuInfo> skuInfoList = JSON.parseArray(jsonSkuList, SkuInfo.class);
        for (SkuInfo skuInfo : skuInfoList) {
            Map specMap = JSON.parseObject(skuInfo.getSpec(), Map.class);
            skuInfo.setSpecMap(specMap);
        }

        //导入索引库
        esManagerMapper.saveAll(skuInfoList);
    }

    /**
     * 根据spuid查询skuList,再导入es
     */
    @Override
    public void importDataBySpuId(String spuId) {
//        List<Sku> skuList = skuFeign.findSkuListBySpuId(spuId);
//        if (skuList == null || skuList.size()<=0){
//            throw new RuntimeException("当前没有数据被查询到,无法导入索引库");
//        }
//        //将集合转换为json
//        String jsonSkuList = JSON.toJSONString(skuList);
//        List<SkuInfo> skuInfoList = JSON.parseArray(jsonSkuList, SkuInfo.class);
//
//        for (SkuInfo skuInfo : skuInfoList) {
//            //将规格信息进行转换
//            Map specMap = JSON.parseObject(skuInfo.getSpec(), Map.class);
//            skuInfo.setSpecMap(specMap);
//        }
//
//        //添加索引库
//        esManagerMapper.saveAll(skuInfoList);

        //查询数据库
        List<Sku> skuList = skuFeign.findSkuListBySpuId(spuId);
        if (skuList == null && skuList.size() <= 0){
            throw new RuntimeException("当前没有数据被查询到,无法导入索引库");
        }

        //将集合skuList转换为json
        String jsonSkuList = JSON.toJSONString(skuList);
        //将json转换为skuInfoList<skuInfo>集合
        List<SkuInfo> skuInfoList = JSON.parseArray(jsonSkuList, SkuInfo.class);
        for (SkuInfo skuInfo : skuInfoList) {
            //将规格信息进行转换
            Map specMap = JSON.parseObject(skuInfo.getSpec(), Map.class);
            skuInfo.setSpecMap(specMap);
        }

        //添加索引库
        esManagerMapper.saveAll(skuInfoList);
    }

    /**
     * 根据spuId删除索引库中sku数据
     * @param spuId
     */
    @Override
    public void delDataBySpuId(String spuId) {
        List<Sku> skuList = skuFeign.findSkuListBySpuId(spuId);
        if (skuList == null && skuList.size() <= 0){
            throw new RuntimeException("当前没有数据被查询到,无法导入索引库");
        }
        for (Sku sku : skuList) {
            //获取skuId，根据skuId删除对应数据
            long skuId = Long.parseLong(sku.getId());
            esManagerMapper.deleteById(skuId);
        }
    }
}
