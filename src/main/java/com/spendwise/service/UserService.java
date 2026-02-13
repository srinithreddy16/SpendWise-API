package com.spendwise.service;

import com.spendwise.dto.response.UserResponse;
import com.spendwise.exception.ResourceNotFoundException;
import com.spendwise.mapper.UserMapper;
import com.spendwise.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper; //just like autowiring

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Retrieves the current user by email and returns a UserResponse DTO.
     * Read-only operation; does not modify any data.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
