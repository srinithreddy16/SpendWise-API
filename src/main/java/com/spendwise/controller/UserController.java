package com.spendwise.controller;

import com.spendwise.domain.entity.User;
import com.spendwise.dto.response.UserResponse;
import com.spendwise.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); //It tells Spring Security: This request is now authenticated as this user.”
        Object principal = authentication.getPrincipal();  //authentication.getPrincipal says user

        if (!(principal instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponse response = userMapper.toUserResponse(user);
        return ResponseEntity.ok(response);
    }
}
