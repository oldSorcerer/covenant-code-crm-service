package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;


public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByUser_Id(Long userId);
}
