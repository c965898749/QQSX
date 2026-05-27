package com.sy.interceptor;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求包装过滤器
 * 在请求到达Controller之前，将HttpServletRequest包装成RequestWrapper
 * 以便在Aspect中可以重复读取请求体
 */
@Component
@Order(1) // 确保在其他过滤器之前执行
public class RequestWrapperFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // 只处理HTTP请求
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            // 包装请求
            RequestWrapper wrappedRequest = new RequestWrapper(httpRequest);
            // 继续执行过滤器链
            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}