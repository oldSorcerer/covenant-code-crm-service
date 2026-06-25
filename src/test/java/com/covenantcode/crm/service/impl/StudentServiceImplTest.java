package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.StudentMapper;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    @DisplayName("Тест 1: Успешное создание студента без привязки к пользователю")
    void create_WithoutUserId_ShouldSucceed() {

        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .userId(null)
                .build();

        Student savedStudent = Student.builder().id(1L).firstName("Ivan").build();
        StudentResponse expectedResponse = new StudentResponse();
        expectedResponse.setId(1L);
        expectedResponse.setUserId(null);

        when(studentRepository.saveAndFlush(any(Student.class))).thenReturn(savedStudent);
        when(studentMapper.toResponse(savedStudent)).thenReturn(expectedResponse);

        StudentResponse actualResponse = studentService.create(request);

        assertNotNull(actualResponse);
        assertEquals(1L, actualResponse.getId());
        assertNull(actualResponse.getUserId());

        verify(userRepository, never()).findById(any());
        verify(studentRepository).saveAndFlush(any(Student.class));
    }

    @Test
    @DisplayName("Тест 2: Успешное создание студента с валидным userId")
    void create_WithValidUserId_ShouldSucceed() {

        Long userId = 5L;
        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Petr")
                .userId(userId)
                .build();

        User user = new User();
        user.setId(userId);

        Student savedStudent = Student.builder().id(10L).user(user).build();
        StudentResponse expectedResponse = new StudentResponse();
        expectedResponse.setId(10L);
        expectedResponse.setUserId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentRepository.existsByUser_Id(userId)).thenReturn(false);
        when(studentRepository.saveAndFlush(any(Student.class))).thenReturn(savedStudent);
        when(studentMapper.toResponse(savedStudent)).thenReturn(expectedResponse);

        StudentResponse actualResponse = studentService.create(request);

        assertEquals(userId, actualResponse.getUserId());
        verify(userRepository).findById(userId);
        verify(studentRepository).existsByUser_Id(userId);
    }

    @Test
    @DisplayName("Тест 3: Ошибка, если указанный userId не существует")
    void create_WithNonExistentUserId_ShouldThrowNotFound() {

        Long userId = 99L;
        StudentCreateRequest request = StudentCreateRequest.builder().userId(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.create(request));

        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тест 4: Ошибка, если пользователь уже привязан к другому студенту")
    void create_WithAlreadyTakenUserId_ShouldThrowConflict() {

        Long userId = 5L;
        StudentCreateRequest request = StudentCreateRequest.builder().userId(userId).build();

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentRepository.existsByUser_Id(userId)).thenReturn(true);


        assertThrows(ConflictException.class, () -> studentService.create(request));

        verify(studentRepository, never()).save(any());
    }
}
