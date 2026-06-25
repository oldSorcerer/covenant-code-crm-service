package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.dto.student.StudentResponse;

import java.util.List;

public interface StudentService {

    StudentResponse getById(Long id);
    List<StudentResponse> getAll();

    StudentResponse create(StudentCreateRequest studentCreateRequest);
}
