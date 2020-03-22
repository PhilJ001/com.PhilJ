package com.PhilJ.goods.feign;

import com.PhilJ.entity.Result;
import com.PhilJ.goods.pojo.Category;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "goods")
public interface CategoryFeign {

    @GetMapping("/category/{id}")
    public Result<Category> findById(@PathVariable("id") Integer id);
}
