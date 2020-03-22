package com.PhilJ.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@Component //实现请求过滤器接口
public class FeignInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        //获取request
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null){
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            if (request != null){
                //获取请求头名称集合
                Enumeration<String> headerNames = request.getHeaderNames();
                //枚举各个请求头名称，从中获取到有关"authorization"头信息
                while (headerNames.hasMoreElements()){
                    String headerName = headerNames.nextElement();
                    if ("authorization".equals(headerName)){
                        String headerValue = request.getHeader(headerName);// Bearer jwt
                        //设置请求头信息
                        requestTemplate.header(headerName, headerValue);
                    }
                }
            }
        }
    }
}
