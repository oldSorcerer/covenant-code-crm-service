package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.dto.student.StudentUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentService {

    StudentResponse getById(Long id);
    List<StudentResponse> getAll();

    StudentResponse create(StudentCreateRequest studentCreateRequest);

    Page<StudentResponse> getAll(String search, Pageable pageable);

    StudentResponse update(Long id, StudentUpdateRequest request);
}
