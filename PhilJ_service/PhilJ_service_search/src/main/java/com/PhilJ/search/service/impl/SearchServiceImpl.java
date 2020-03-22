package com.PhilJ.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.PhilJ.search.pojo.SkuInfo;
import com.PhilJ.search.service.SearchService;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    /**
     * 按照查询条件进行数据查询
     * @param searchMap 查询条件
     * @return
     */
    @Override
    public Map search(Map<String, String> searchMap) {
        //创建要返回的Map集合
        Map<String,Object> resultMap = new HashMap<>();

        //构建查询
        if (searchMap != null){
            //组合查询条件对象
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery(); //boolQuery可以用来封装多个查询条件
            /**
             * 根据-关键字-查询
             */
            if (!StringUtils.isEmpty(searchMap.get("keywords"))){
                //组合查询条件对象
                //如搜索条件为“小米手机”这里must会分词为“小米”和“手机”这两个词是or关系
                //加operator(Operator.AND)可以把or改为and关系
                boolQuery.must(QueryBuilders.matchQuery("name",searchMap.get("keywords")).operator(Operator.AND));//.operator(Operator.AND)条件拼接
            }

            /**
             * 根据-品牌-过滤查询
             */
            if (!StringUtils.isEmpty(searchMap.get("brand"))){
                //构建查询条件
                boolQuery.filter(QueryBuilders.termQuery("brandName",searchMap.get("brand")));
            }

            /**
             * 根据-规格-进行过滤查询
             */
            for (String key : searchMap.keySet()) {
                //判断是否为规格参数传递
                if (key.startsWith("spec_")){
                    String value = searchMap.get(key).replace("%2B","+");
                    //specMap.规格名称.keyword
                    boolQuery.filter(QueryBuilders.termQuery("specMap."+key.substring(5)+".keyword", value));
                }
            }

            /**
             * 根据-价格-进行区间过滤查询
             */
            if (!StringUtils.isEmpty(searchMap.get("price"))){
                String[] prices = searchMap.get("price").split("-");
                if (prices.length == 2){
                    //说明这是是个价格区间
                    boolQuery.filter(QueryBuilders.rangeQuery("price").lte(prices[1]));//.rangeQuery()区间过滤
                }
                boolQuery.filter(QueryBuilders.rangeQuery("price").gte(prices[0]));
            }

            /**
             * 开启-分页-查询
             */
            String pageNum = searchMap.get("pageNum"); //当前页
            String pageSize = searchMap.get("pageSize"); //每页显示多少条
            if (pageNum == null){
                pageNum = "1";
            }
            if (pageSize == null){
                pageSize = "30";
            }

            /**
             * 设置-关键字高亮-查询
             */
            //设置高亮域以及高亮的样式 ---> HighlightBuilder.Field获得高亮域对象
            HighlightBuilder.Field field = new HighlightBuilder.Field("name")//设置需要高亮显示所在的区域
                    .preTags("<span style='color:red'>")//高亮样式的前缀
                    .postTags("</span>");//高亮样式的后缀

            //定义聚合查询的字段列名
            String skuBrand="skuBrand";
            String skuSpec="skuSpec";

            //原生搜索实现类 ---> nativeSearchQueryBuilder：构建原生查询对象
            NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
            nativeSearchQueryBuilder
                    .withQuery(boolQuery)
                    .addAggregation(AggregationBuilders.terms(skuBrand).field("brandName"))//按照品牌进行分组(聚合)查询：1.聚合查询后的字段列。2.聚合查询的区域
                    .addAggregation(AggregationBuilders.terms(skuSpec).field("spec.keyword"))//按照规格进行聚合查询
                    .withPageable(PageRequest.of(Integer.parseInt(pageNum)-1,Integer.parseInt(pageSize)))//设置分页: 参数1:当前页 是从0开始;参数2:每页显示多少条
                    .withHighlightFields(field);//设置高亮区域

            /**
             * 按照相关字段进行排序查询
             * 1.当前域
             * 2.当前的排序操作(升序ASC,降序DESC)
             */
            if (StringUtils.isNotEmpty(searchMap.get("sortField")) && StringUtils.isNotEmpty(searchMap.get("sortRule"))){
                if ("ASC".equals(searchMap.get("sortRule"))){
                    //升序
                    nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort((searchMap.get("sortField"))).order(SortOrder.ASC));
                }else{
                    //降序
                    nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort((searchMap.get("sortField"))).order(SortOrder.DESC));
                }
            }

            //构建最终的查询对象
            NativeSearchQuery query = nativeSearchQueryBuilder.build();

            //开启查询
            /**
             * 第一个参数: 条件构建对象
             * 第二个参数: 查询操作实体类
             * 第三个参数: 查询结果操作对象
             */
            AggregatedPage<SkuInfo> resultInfo = elasticsearchTemplate.queryForPage(query, SkuInfo.class, new SearchResultMapper() {
                @Override
                public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                    //查询结果操作：封装查询结果
                    List<T> list = new ArrayList<>();

                    //获取查询命中结果数据--->每一个hit就是一个商品
                    SearchHits hits = searchResponse.getHits();
                    if (hits != null) {
                        for (SearchHit hit : hits) {
                            //获取当前商品数据的json字符串格式数据
                            String sourceAsString = hit.getSourceAsString();
                            //将json转为SkuInfo格式
                            SkuInfo skuInfo = JSON.parseObject(sourceAsString, SkuInfo.class);

                            //获取高亮域
                            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                            System.out.println("高亮域：" + highlightFields);
                            if (highlightFields != null && highlightFields.size()>0){
                                //替换数据
                                skuInfo.setName(highlightFields.get("name").getFragments()[0].toString());
                            }

                            list.add((T) skuInfo);
                        }
                    }
                    /**
                     * 参数1：当前封装好的数据集合
                     * 参数2：分页对象
                     * 参数3：获取到的总数据个数
                     * 参数4：searchResponse.getAggregations()
                     */
                    return new AggregatedPageImpl<T>(list, pageable, hits.getTotalHits(), searchResponse.getAggregations());
                }
            });
            //封装返回结果
            //封装查询得到的总条数
            resultMap.put("total",resultInfo.getTotalElements());
            //封装查询到的总页数
            resultMap.put("totalPages", resultInfo.getTotalPages());
            //封装查询到的数据集合
            resultMap.put("rows", resultInfo.getContent());
            //封装品牌的分组（聚合）结果
            StringTerms brandTerms = (StringTerms) resultInfo.getAggregation(skuBrand);//参数skuBrand：分组列名
            System.out.println(brandTerms);
            List<String> brandList = brandTerms.getBuckets().stream().map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());
            resultMap.put("brandList", brandList);
            //封装规格的分组（聚合）结果
            StringTerms specTerms = (StringTerms) resultInfo.getAggregation(skuSpec);
            System.out.println(specTerms);
            List<String> specList = specTerms.getBuckets().stream().map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());
            resultMap.put("specList",this.formartSpec(specList));

            //当前页
            resultMap.put("pageNum",pageNum);
            return resultMap;
        }

        return null;
    }

    /**
     * 原有数据
     *  [
     *         "{'颜色': '黑色', '尺码': '平光防蓝光-无度数电脑手机护目镜'}",
     *         "{'颜色': '红色', '尺码': '150度'}",
     *         "{'颜色': '黑色', '尺码': '150度'}",
     *         "{'颜色': '黑色'}",
     *         "{'颜色': '红色', '尺码': '100度'}",
     *         "{'颜色': '红色', '尺码': '250度'}",
     *         "{'颜色': '红色', '尺码': '350度'}",
     *         "{'颜色': '黑色', '尺码': '200度'}",
     *         "{'颜色': '黑色', '尺码': '250度'}"
     *     ]
     *
     * 需要的数据格式
     *    {
     *        颜色:[黑色,红色],
     *        尺码:[100度,150度]
     *    }
     */
    public Map<String, Set<String>> formartSpec(List<String> specList){
        Map<String,Set<String>> resultMap = new HashMap<>();
        if (specList != null && specList.size() > 0){
            for (String specJsonString : specList) {
                //{'颜色': '黑色', '尺码': '平光防蓝光-无度数电脑手机护目镜'} ---> Map格式: '颜色': '黑色'; '尺码': '平光防蓝光-无度数电脑手机护目镜'
                Map<String,String> specMap = JSON.parseObject(specJsonString, Map.class);
                for (String specKey : specMap.keySet()) {
                    //specKey: 颜色，尺码
                    Set<String> specSet = resultMap.get(specKey);
                    if (specSet == null){
                        specSet = new HashSet<String>();
                    }
                    //将规格的值放入set中
                    specSet.add(specMap.get(specKey));
                    //将set放入map中
                    resultMap.put(specKey, specSet);
                }
            }
        }
        return resultMap;
    }
}
