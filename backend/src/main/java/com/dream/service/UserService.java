package com.dream.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import com.dream.domain.User;
import com.dream.mapper.UserMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Cacheable(cacheNames = "users", key = "#userId")
    public User getById(Long userId) {
        return userMapper.selectById(userId);
    }

    public User getRequiredById(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public User findByOpenid(String openid) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getOpenid, openid));
    }

    @CacheEvict(cacheNames = "users", allEntries = true)
    public User upsertByOpenid(String openid) {
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "openid is required");
        }

        User user = findByOpenid(openid);
        if (user != null) {
            return user;
        }

        User created = new User();
        created.setOpenid(openid);
        created.setNickname("");
        created.setAvatar("");
        userMapper.insert(created);
        return created;
    }
}
