package com.PhilJ.search.controller;


import com.PhilJ.entity.Page;
import com.PhilJ.search.pojo.SkuInfo;
import com.PhilJ.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/list")
    public String list(@RequestParam Map<String,String> searchMap, Model model){
        //特殊符号处理
        this.handleSearchMap(searchMap);
        //获取查询结果
        Map resultMap = searchService.search(searchMap);
        model.addAttribute("result", resultMap);
        model.addAttribute("searchMap", searchMap);

        //封装分页数据并返回
        //1.总记录数
        //2.当前页
        //3.每页显示多少条
        System.out.println("====================================");
        System.out.println(resultMap.get("pageNum"));
        System.out.println("====================================");

        Page<SkuInfo> page = new Page<SkuInfo>(
                Long.parseLong(String.valueOf(resultMap.get("total"))),
                Integer.parseInt(String.valueOf(resultMap.get("pageNum"))),
                Page.pageSize
        );
        model.addAttribute("page",page);

        //拼接url
        StringBuilder url = new StringBuilder("/search/list");
        if (searchMap != null && searchMap.size() > 0){
            //有查询条件
            url.append("?");
            for (String key : searchMap.keySet()) {
                if (!"sortRule".equals(key) && !"sortField".equals(key) && !"pageNum".equals(key)){
                    url.append(key).append("=").append(searchMap.get(key)).append("&");
                }
            }
            //http://localhost:9009/search/list?keywords=手机&spec_网络制式=4G&
            String urlString = url.toString();
            //去除路径上的最后一个&
            urlString = urlString.substring(0,urlString.length()-1);
            model.addAttribute("url",urlString);
        } else {
            model.addAttribute("url",url);
        }
        //跳转页面
        return "search";
    }

    @GetMapping
    @ResponseBody
    public Map search(@RequestParam Map<String,String> searchMap){
        //特殊符号处理
        this.handleSearchMap(searchMap);

        Map resultSearch = searchService.search(searchMap);
        return resultSearch;

    }

    /**
     * 特殊符号处理
     * @param searchMap
     */
    private void handleSearchMap(Map<String, String> searchMap) {
        Set<Map.Entry<String, String>> entries = searchMap.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey().startsWith("spec_")) {
                searchMap.put(entry.getKey(),entry.getValue().replace("+","%2B"));
            }
        }
    }
}
