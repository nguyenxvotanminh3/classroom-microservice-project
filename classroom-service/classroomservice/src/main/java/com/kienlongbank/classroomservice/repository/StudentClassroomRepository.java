package com.kienlongbank.classroomservice.repository;


import com.kienlongbank.classroomservice.model.StudentClassroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentClassroomRepository extends JpaRepository<StudentClassroom, Long> {
    List<StudentClassroom> findByClassroomId(Long classroomId);
    List<StudentClassroom> findByStudentId(Long studentId);
    Optional<StudentClassroom> findByStudentIdAndClassroomId(Long studentId, Long classroomId);
    boolean existsByStudentIdAndClassroomId(Long studentId, Long classroomId);
    void deleteByStudentIdAndClassroomId(Long studentId, Long classroomId);
} 