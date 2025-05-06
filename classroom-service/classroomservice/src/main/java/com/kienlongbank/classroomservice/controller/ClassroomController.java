package com.kienlongbank.classroomservice.controller;

import com.kienlongbank.classroomservice.dto.ClassroomRequest;
import com.kienlongbank.classroomservice.dto.ClassroomResponse;
import com.kienlongbank.classroomservice.service.ClassroomService;
import com.kienlongbank.classroomservice.handler.ClassroomJwtHandler;
import com.kienlongbank.classroomservice.handler.ClassroomHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.MessageSource;
import java.util.Locale;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/classrooms")
@Slf4j
public class ClassroomController {

    private final ClassroomService classroomService;
    private final ClassroomJwtHandler classroomJwtHandler;
    private final MessageSource messageSource;
    private final ClassroomHandler classroomHandler;

    @Autowired
    public ClassroomController(ClassroomService classroomService, ClassroomJwtHandler classroomJwtHandler, MessageSource messageSource, ClassroomHandler classroomHandler) {
        this.classroomService = classroomService;
        this.classroomJwtHandler = classroomJwtHandler;
        this.messageSource = messageSource;
        this.classroomHandler = classroomHandler;
    }

    @PostMapping
    public ResponseEntity<?> createClassroom(@Valid @RequestBody ClassroomRequest classroomRequest, HttpServletRequest request, Locale locale) {
        return classroomHandler.handleCreateClassroom(classroomRequest, request, locale);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getClassroomById(@PathVariable Long id, Locale locale) {
        return classroomHandler.handleGetClassroomById(id, locale);
    }

    @GetMapping
    public ResponseEntity<?> getAllClassrooms(Locale locale) {
        return classroomHandler.handleGetAllClassrooms(locale);
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<?> getClassroomsByTeacherId(@PathVariable Long teacherId, Locale locale) {
        return classroomHandler.handleGetClassroomsByTeacherId(teacherId, locale);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateClassroom(@PathVariable Long id, @Valid @RequestBody ClassroomRequest classroomRequest, HttpServletRequest request, Locale locale) {
        return classroomHandler.handleUpdateClassroom(id, classroomRequest, request, locale);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClassroom(@PathVariable Long id, HttpServletRequest request, Locale locale) {
        return classroomHandler.handleDeleteClassroom(id, request, locale);
    }
} 