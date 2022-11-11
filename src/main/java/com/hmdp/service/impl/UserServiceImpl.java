package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式不正确");
        }
        //生成验证码 6位
        String code = RandomUtil.randomNumbers(6);
        //将验证码发送到用户手机-模拟
        log.debug("已向用户发送验证码{}", code);
        //将验证码保存到redis,验证码有效期两分钟
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY,code,2, TimeUnit.MINUTES);
        //返回给用户
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //再次校验手机号格式
        //前端会限制传来的dto不能为空，get时不会NPE
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式不正确");
        }
        //取出session的验证码和dto里的比较是否一样
        String redisCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY);
        if (!code.equals(redisCode)) {
            return Result.fail("验证码不正确");
        }
        //判断用户是否存在
        User user = query().eq("phone", phone).one();
        if (user == null) {
            //添加新用户
            user = new User();
            user.setPhone(phone);
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            this.save(user);
        }
        //直接返回user信息太多了，返回dto
        //随机生成token，作为登陆凭证
        String token = UUID.randomUUID().toString(true);
        //将userDto存入redis
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //字段编辑器，因id为Long类型，调用其toString()将其转为String
        Map<String, Object> userDtoMap = BeanUtil.beanToMap(userDTO,new HashMap<>(3),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue)-> fieldValue.toString()));
        //只能存Map
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token,userDtoMap);
        //session的有效期就是30分钟，这里仿照
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, Duration.ofMinutes(LOGIN_USER_TTL));
        //返回token
        return Result.ok(token);
    }
}
