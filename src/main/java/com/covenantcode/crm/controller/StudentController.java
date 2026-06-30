package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.dto.student.StudentUpdateRequest;
import com.covenantcode.crm.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Управление студентами")
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Создать нового студента", description = "Доступно ролям ADMIN, MANAGER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Студент успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
            @ApiResponse(responseCode = "404", description = "Указанный userId не найден"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже привязан к другому студенту")
    })
    public StudentResponse create(@Valid @RequestBody StudentCreateRequest request) {
        return studentService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Получить список студентов с пагинацией и поиском",
            description = "Возвращает страницу студентов. Доступно ролям ADMIN, MANAGER"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список студентов найден",
                    content =
                    @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    public Page<StudentResponse> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return studentService.getAll(search, pageable);
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Обновить данные студента", description = "Обновляет все поля существующего студента (кроме привязки к учётной записи)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные успешно обновлены"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос (ошибки валидации)"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (недостаточно прав)"),
            @ApiResponse(responseCode = "404", description = "Студент с указанным ID не найден")
    })
    public ResponseEntity<StudentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request) {
        StudentResponse response = studentService.update(id, request);
        return ResponseEntity.ok(response);
    }
}
