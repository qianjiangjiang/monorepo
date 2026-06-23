package com.dream.controller.dto;

import com.dream.domain.User;

public record UserResponse(Long id, String nickname, String avatar) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getNickname() == null ? "" : user.getNickname(),
                user.getAvatar() == null ? "" : user.getAvatar()
        );
    }
}
