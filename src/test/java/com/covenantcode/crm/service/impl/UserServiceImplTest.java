package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.user.UserResponse;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.ForbiddenException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.UserMapper;
import com.covenantcode.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private Pageable pageable;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);
        now = OffsetDateTime.now();
    }

    @Test
    public void getAll_success() {
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(RoleName.ADMIN);

        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleName.MANAGER);

        User user1 = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("System")
                .email("admin@covenantcode.ru")
                .phone(null)
                .role(adminRole)
                .enabled(true)
                .createdAt(now)
                .build();

        User user2 = User.builder()
                .id(2L)
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@company.ru")
                .phone("+79161234567")
                .role(managerRole)
                .enabled(true)
                .createdAt(now)
                .build();

        List<User> users = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        UserResponse response1 = UserResponse.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("System")
                .email("admin@covenantcode.ru")
                .phone(null)
                .role("ADMIN")
                .enabled(true)
                .createdAt(now.toString())
                .build();

        UserResponse response2 = UserResponse.builder()
                .id(2L)
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@company.ru")
                .phone("+79161234567")
                .role("MANAGER")
                .enabled(true)
                .createdAt(now.toString())
                .build();

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user1)).thenReturn(response1);
        when(userMapper.toResponse(user2)).thenReturn(response2);

        Page<UserResponse> result = userService.getAll(pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        UserResponse firstResponse = result.getContent().get(0);
        assertEquals("ADMIN", firstResponse.getRole());

        UserResponse secondResponse = result.getContent().get(1);
        assertEquals("MANAGER", secondResponse.getRole());

        verify(userRepository).findAll(pageable);
        verify(userMapper).toResponse(user1);
        verify(userMapper).toResponse(user2);
    }

    @Test
    public void getAll_emptyList() {
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<UserResponse> result = userService.getAll(pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(userRepository).findAll(pageable);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void getUserById_whenAdminRequestsAnotherUser_thenSuccess() {

        Long adminId = 1L;
        Long targetId = 2L;
        User adminUser = buildUser(adminId, RoleName.ADMIN);
        User targetUser = buildUser(targetId, RoleName.MANAGER);
        UserResponse expectedResponse = new UserResponse();

        when(userRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userMapper.toResponse(targetUser)).thenReturn(expectedResponse);

        UserResponse result = userService.getUserById(targetId, adminId);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(userRepository).findById(targetId);
        verify(userRepository).findById(adminId);
        verify(userMapper).toResponse(targetUser);
    }

    @Test
    void getUserById_whenUserRequestsOwnProfile_thenSuccess() {

        Long userId = 5L;
        User user = buildUser(userId, RoleName.MANAGER);
        UserResponse expectedResponse = new UserResponse();


        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse result = userService.getUserById(userId, userId);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(userRepository, times(2)).findById(userId);
    }

    @Test
    void getUserById_whenManagerRequestsAnotherUser_thenForbiddenException() {

        Long managerId = 5L;
        Long otherUserId = 7L;

        User managerUser = buildUser(managerId, RoleName.MANAGER);
        User otherUser = buildUser(otherUserId, RoleName.MANAGER);

        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(userRepository.findById(managerId)).thenReturn(Optional.of(managerUser));

        assertThrows(ForbiddenException.class, () ->
                userService.getUserById(otherUserId, managerId)
        );

        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void getUserById_whenTargetUserNotFound_thenEntityNotFoundException() {

        Long targetId = 1L;
        Long currentUserId = 1L;
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(targetId, currentUserId));

        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void getUserById_whenCurrentUserNotFound_thenEntityNotFoundException() {

        Long targetId = 1L;
        Long currentUserId = 2L;

        User targetUser = new User();
        targetUser.setId(targetId);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(targetUser));

        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(targetId, currentUserId);
        });

        verify(userRepository).findById(targetId);
        verify(userRepository).findById(currentUserId);
    }

    private User buildUser(Long id, RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setEmail("user_" + id + "@test.com");
        user.setFirstName("User");
        user.setLastName(String.valueOf(id));
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}