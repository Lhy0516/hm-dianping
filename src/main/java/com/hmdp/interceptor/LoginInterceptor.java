package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.UserHolder;
import jdk.net.SocketFlow;
import org.aopalliance.intercept.Interceptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperatorExtensionsKt;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @author 李海洋
 * @version 1.0
 */
//是new()对象来注册拦截器的，不要加注解
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 在到达控制层之前处理
     * @param request
     * @param response
     * @param handler
     * @return true 表示可以交由控制器处理 否则拦截
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //如果登陆过，RefreshTokenInterceptor拦截器已将信息存入ThreadLocal，token也刷新了
        if (UserHolder.getUser() == null){
            //没登陆过，就拦截让其去登录
            response.setStatus(401);
            return false;
        }
        //放行
        return true;
    }

}
