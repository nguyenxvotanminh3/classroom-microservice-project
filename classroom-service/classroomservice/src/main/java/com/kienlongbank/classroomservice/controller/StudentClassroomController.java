package com.kienlongbank.classroomservice.controller;


import com.kienlongbank.classroomservice.dto.GradeRequest;
import com.kienlongbank.classroomservice.dto.StudentClassroomRequest;
import com.kienlongbank.classroomservice.dto.StudentClassroomResponse;
import com.kienlongbank.classroomservice.service.StudentClassroomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/student-classrooms")
@Slf4j
public class StudentClassroomController {

    private final StudentClassroomService studentClassroomService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public StudentClassroomController(StudentClassroomService studentClassroomService) {
        this.studentClassroomService = studentClassroomService;
    }

    @GetMapping
    public ResponseEntity<List<StudentClassroomResponse>> getAllStudentClassrooms() {
        log.info("API - GET /student-classrooms - Retrieving all student-classroom enrollments");
        try {
            List<StudentClassroomResponse> enrollments = studentClassroomService.getAllStudentClassrooms();
            log.info("API - GET /student-classrooms - Retrieved {} enrollments", enrollments.size());
            return ResponseEntity.ok(enrollments);
        } catch (Exception e) {
            log.error("API - GET /student-classrooms - Error retrieving enrollments: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<StudentClassroomResponse> enrollStudent(@Valid @RequestBody StudentClassroomRequest request) {
        StudentClassroomResponse enrollmentResponse = studentClassroomService.enrollStudentToClassroom(request);
        return new ResponseEntity<>(enrollmentResponse, HttpStatus.CREATED);
    }

    @PutMapping("/{studentId}/classrooms/{classroomId}/grade")
    public ResponseEntity<StudentClassroomResponse> updateGrade(
            @PathVariable Long studentId,
            @PathVariable Long classroomId,
            @Valid @RequestBody GradeRequest gradeRequest) {
        StudentClassroomResponse updatedEnrollment = studentClassroomService.updateStudentGrade(
                studentId, classroomId, gradeRequest);
        return ResponseEntity.ok(updatedEnrollment);
    }

    @GetMapping("/classrooms/{classroomId}/students")
    public ResponseEntity<List<StudentClassroomResponse>> getStudentsByClassroom(@PathVariable Long classroomId) {
        List<StudentClassroomResponse> students = studentClassroomService.getStudentsByClassroomId(classroomId);
        return ResponseEntity.ok(students);
    }

    @GetMapping("/students/{studentId}/classrooms")
    public ResponseEntity<List<StudentClassroomResponse>> getClassroomsByStudent(@PathVariable Long studentId) {
        List<StudentClassroomResponse> classrooms = studentClassroomService.getClassroomsByStudentId(studentId);
        return ResponseEntity.ok(classrooms);
    }

    @DeleteMapping("/{studentId}/classrooms/{classroomId}")
    public ResponseEntity<Void> unenrollStudent(
            @PathVariable Long studentId,
            @PathVariable Long classroomId) {
        studentClassroomService.unenrollStudent(studentId, classroomId);
        return ResponseEntity.noContent().build();
    }
} 