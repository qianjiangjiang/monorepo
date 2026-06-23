package com.dream.controller;

import com.dream.common.ApiResponse;
import com.dream.common.auth.CurrentUserContext;
import com.dream.common.auth.UserPrincipal;
import com.dream.controller.dto.UserResponse;
import com.dream.domain.User;
import com.dream.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        UserPrincipal principal = CurrentUserContext.require();
        User user = userService.getRequiredById(principal.userId());
        return ApiResponse.ok(UserResponse.from(user));
    }
}
