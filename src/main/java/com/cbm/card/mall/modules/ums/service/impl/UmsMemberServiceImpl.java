package com.cbm.card.mall.modules.ums.service.impl;

/**
 * @Author: dxh
 * @Date: 2026/01/27/10:16
 */
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.cbm.card.mall.common.exception.Asserts;
import com.cbm.card.mall.common.service.RedisService;
import com.cbm.card.mall.modules.ums.dto.UmsMemberParam;
import com.cbm.card.mall.modules.ums.mapper.UmsMemberMapper;
import com.cbm.card.mall.modules.ums.model.UmsMember;
import com.cbm.card.mall.modules.ums.service.UmsMemberService;
import com.cbm.card.mall.security.util.JwtTokenUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UmsMemberServiceImpl extends ServiceImpl<UmsMemberMapper, UmsMember> implements UmsMemberService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private RedisService redisService;

    private static final String MEMBER_TOKEN_KEY = "mall:member:token:";

    @Override
    public UmsMember getByUsername(String username) {
        QueryWrapper<UmsMember> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsMember::getUsername, username);
        List<UmsMember> list = list(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public UmsMember register(UmsMemberParam param) {
        UmsMember member = new UmsMember();
        BeanUtils.copyProperties(param, member);
        member.setCreateTime(new Date());
        member.setStatus(1);

        if (getByUsername(member.getUsername()) != null) {
            Asserts.fail("用户名已存在");
        }

        member.setPassword(passwordEncoder.encode(param.getPassword()));
        save(member);
        return member;
    }

    @Override
    public String login(String username, String password) {
        UmsMember member = getByUsername(username);
        if (member == null) {
            Asserts.fail("用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, member.getPassword())) {
            Asserts.fail("用户名或密码错误");
        }
        if (member.getStatus() == 0) {
            Asserts.fail("账号已被禁用");
        }

        String token = jwtTokenUtil.generateToken(username);
        redisService.set(MEMBER_TOKEN_KEY + username, token, jwtTokenUtil.getExpiration());
        return token;
    }

    @Override
    public String refreshToken(String oldToken) {
        return jwtTokenUtil.refreshHeadToken(oldToken);
    }

    @Override
    public void logout(String username) {
        redisService.del(MEMBER_TOKEN_KEY + username);
    }
}

