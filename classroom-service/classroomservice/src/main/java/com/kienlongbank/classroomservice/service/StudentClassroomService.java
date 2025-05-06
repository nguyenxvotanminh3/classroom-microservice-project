package com.kienlongbank.classroomservice.service;


import com.kienlongbank.classroomservice.dto.GradeRequest;
import com.kienlongbank.classroomservice.dto.StudentClassroomRequest;
import com.kienlongbank.classroomservice.dto.StudentClassroomResponse;

import java.util.List;

public interface StudentClassroomService {
    StudentClassroomResponse enrollStudentToClassroom(StudentClassroomRequest request);
    StudentClassroomResponse updateStudentGrade(Long studentId, Long classroomId, GradeRequest gradeRequest);
    List<StudentClassroomResponse> getStudentsByClassroomId(Long classroomId);
    List<StudentClassroomResponse> getClassroomsByStudentId(Long studentId);
    void unenrollStudent(Long studentId, Long classroomId);
    List<StudentClassroomResponse> getAllStudentClassrooms();
} 