package com.dream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dream.domain.User;
import com.dream.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void returnsConcurrentUserWhenOpenidInsertRaces() {
        User existing = new User();
        existing.setId(12L);
        existing.setOpenid("openid-1");
        when(userMapper.selectOne(any())).thenReturn(null, existing);
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("duplicate openid"));

        User result = userService.upsertByOpenid("openid-1");

        assertThat(result).isSameAs(existing);
        verify(userMapper).insert(any(User.class));
    }
}
