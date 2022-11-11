package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

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

/**
 * 如果没登录，放行至第二哥拦截器，去过他访问的是需要登的页面，第二个拦截器会拦截，如果不是，第二个拦截器不会拦截，比如访问【首页】
 * 如果登录过，访问不需要登陆的页面时也帮其刷新token又凶啊其，防止此情况下再去访问需要登录的页面时出现登录超时的情况
 * 此页面不管怎样都放行，目的是刷新token有效期，
 *  ==========注意，能根据token从Redis中获取到value才叫token有效==========
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {


    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

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
        //获取token
        String token = request.getHeader("authorization");

        if (StrUtil.isBlank(token)){
            //没传token
            return true;
        }
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);

        if (userMap.isEmpty()){
            //token无效
            return true;
        }

        //说明是登录过，然后去访问非登录页面了
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(),false);

        //放入ThreadLocal
        UserHolder.saveUser(userDTO);

        //刷新token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL, TimeUnit.MINUTES);

        //放行
        return true;
    }


    /**
     * 在请求处理完成(请求处理完成的标志是视图处理完成)之后执行
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    //移除用户
        UserHolder.removeUser();
    }
}
