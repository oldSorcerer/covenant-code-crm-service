package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.user.UserResponse;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.ForbiddenException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.UserMapper;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Page<UserResponse> getAll(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(userMapper::toResponse);
    }

    @Override
    public UserResponse getUserById(Long targetId, Long currentUserId) {

        User targetUser = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        boolean isOwner = currentUserId.equals(targetId);
        boolean isAdmin = currentUser.getRole() != null
                && currentUser.getRole().getName() == RoleName.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Доступ запрещён");
        }

        return userMapper.toResponse(targetUser);
    }
}
