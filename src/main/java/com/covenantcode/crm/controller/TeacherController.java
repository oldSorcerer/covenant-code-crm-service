package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.teacher.TeacherResponse;
import com.covenantcode.crm.service.TeacherService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "teachers", description = "Управление учителями")
public class TeacherController {

    private final TeacherService teacherService;


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    @Operation(summary = "Получить список преподавателей")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список преподавателей"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    public ResponseEntity<Page<TeacherResponse>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable){
        return ResponseEntity.ok(teacherService.getAll(search, pageable));
    }
}
