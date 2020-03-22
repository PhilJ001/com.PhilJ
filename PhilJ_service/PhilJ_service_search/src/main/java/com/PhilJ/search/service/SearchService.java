package com.PhilJ.search.service;

import java.util.Map;

public interface SearchService {

    /**
     * 按照查询条件进行数据查询
     * @param searchMap 查询条件
     * @return
     */
    Map search(Map<String,String> searchMap);
}
