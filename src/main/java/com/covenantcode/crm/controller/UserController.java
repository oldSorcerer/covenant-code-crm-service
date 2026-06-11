package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.user.UserResponse;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Управление пользователями", description = "Эндпоинты для управления пользователями")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить всех пользователей",
            description = "Возвращает постраничный список всех пользователей. Доступно только для роли ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен"),
            @ApiResponse(responseCode = "401", description = "Не авторизован - отсутствует или невалидный JWT токен"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён - у пользователя нет роли ADMIN")
    })
    public Page<UserResponse> getAll(@PageableDefault(size = 20) Pageable pageable){
        return userService.getAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID", responses = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id, Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getUserById(id, currentUser.getId()));
    }
}
