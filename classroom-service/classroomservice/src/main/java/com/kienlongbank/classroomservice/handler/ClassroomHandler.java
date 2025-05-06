package com.kienlongbank.classroomservice.handler;

import com.kienlongbank.classroomservice.dto.ClassroomRequest;
import com.kienlongbank.classroomservice.dto.ClassroomResponse;
import com.kienlongbank.classroomservice.service.ClassroomService;
import com.kienlongbank.classroomservice.handler.ClassroomJwtHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassroomHandler {
    private final ClassroomService classroomService;
    private final ClassroomJwtHandler classroomJwtHandler;
    private final MessageSource messageSource;

    public ResponseEntity<?> handleCreateClassroom(ClassroomRequest classroomRequest, HttpServletRequest request, Locale locale) {
        log.info("API - POST /classrooms - Creating classroom with name: {}", classroomRequest.getName());
        try {
            classroomJwtHandler.validateAdminRole(request);
            ClassroomResponse createdClassroom = classroomService.createClassroom(classroomRequest);
            String msg = messageSource.getMessage("classroom.create.success", null, locale);
            log.info("API - POST /classrooms - Classroom created successfully with ID: {}", createdClassroom.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", msg, "data", createdClassroom));
        } catch (ResponseStatusException e) {
            String msg = messageSource.getMessage("classroom.access.denied", null, locale);
            log.error("API - POST /classrooms - Access denied: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", msg));
        } catch (Exception e) {
            String msg = messageSource.getMessage("classroom.create.fail", null, locale);
            log.error("API - POST /classrooms - Failed to create classroom: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
        }
    }

    public ResponseEntity<?> handleGetClassroomById(Long id, Locale locale) {
        log.info("API - GET /classrooms/{} - Retrieving classroom by ID", id);
        long startTime = System.currentTimeMillis();
        try {
            ClassroomResponse classroom = classroomService.getClassroomById(id);
            String msg = messageSource.getMessage("classroom.get.success", null, locale);
            long timeTaken = System.currentTimeMillis() - startTime;
            log.info("API - GET /classrooms/{} - Retrieved classroom in {}ms", id, timeTaken);
            return ResponseEntity.ok(Map.of("message", msg, "data", classroom));
        } catch (Exception e) {
            String msg = messageSource.getMessage("classroom.get.fail", null, locale);
            log.error("API - GET /classrooms/{} - Failed to retrieve classroom: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
        }
    }

    public ResponseEntity<?> handleGetAllClassrooms(Locale locale) {
        log.info("API - GET /classrooms - Retrieving all classrooms");
        long startTime = System.currentTimeMillis();
        try {
            List<ClassroomResponse> classrooms = classroomService.getAllClassrooms();
            String msg = messageSource.getMessage("classroom.list.success", null, locale);
            long timeTaken = System.currentTimeMillis() - startTime;
            log.info("API - GET /classrooms - Retrieved {} classrooms in {}ms", classrooms.size(), timeTaken);
            return ResponseEntity.ok(Map.of("message", msg, "data", classrooms));
        } catch (Exception e) {
            String msg = messageSource.getMessage("classroom.list.fail", null, locale);
            log.error("API - GET /classrooms - Failed to retrieve classrooms: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
        }
    }

    public ResponseEntity<?> handleGetClassroomsByTeacherId(Long teacherId, Locale locale) {
        log.info("API - GET /classrooms/teacher/{} - Retrieving classrooms for teacher", teacherId);
        long startTime = System.currentTimeMillis();
        try {
            List<ClassroomResponse> classrooms = classroomService.getClassroomsByTeacherId(teacherId);
            String msg = messageSource.getMessage("classroom.list.success", null, locale);
            long timeTaken = System.currentTimeMillis() - startTime;
            log.info("API - GET /classrooms/teacher/{} - Retrieved {} classrooms in {}ms", teacherId, classrooms.size(), timeTaken);
            return ResponseEntity.ok(Map.of("message", msg, "data", classrooms));
        } catch (Exception e) {
            String msg = messageSource.getMessage("classroom.list.fail", null, locale);
            log.error("API - GET /classrooms/teacher/{} - Failed to retrieve classrooms: {}", teacherId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
        }
    }

    public ResponseEntity<?> handleUpdateClassroom(Long id, ClassroomRequest classroomRequest, HttpServletRequest request, Locale locale) {
        log.info("API - PUT /classrooms/{} - Updating classroom", id);
        try {
            ClassroomResponse updatedClassroom = classroomService.updateClassroom(id, classroomRequest);
            String msg = messageSource.getMessage("classroom.update.success", null, locale);
            log.info("API - PUT /classrooms/{} - Classroom updated successfully", id);
            return ResponseEntity.ok(Map.of("message", msg, "data", updatedClassroom));
        } catch (ResponseStatusException e) {
            String msg = messageSource.getMessage("classroom.access.denied", null, locale);
            log.error("API - PUT /classrooms/{} - Access denied: {}", id, e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", msg));
        } catch (Exception e) {
            String msg = messageSource.getMessage("classroom.update.fail", null, locale);
            log.error("API - PUT /classrooms/{} - Failed to update classroom: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
        }
    }

    public ResponseEntity<?> handleDeleteClassroom(Long id, HttpServletRequest request, Locale locale) {
        log.info("API - DELETE /classrooms/{} - Deleting classroom", id);
        try {
            classroomJwtHandler.validateAdminRole(request);
            classroomService.deleteClassroom(id);
            String msg = messageSource.getMessage("classroom.delete.success", null, locale);
            log.info("API - DELETE /classrooms/{} - Classroom deleted successfully", id);
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (ResponseStatusException e) {
            String msg = messageSource.getMessage("classroom.access.denied", null, locale);
            log.error("API - DELETE /classrooms/{} - Access denied: {}", id, e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", msg));
        } catch (Exception e) {
            String msg = messageSource.getMessage("classroom.delete.fail", null, locale);
            log.error("API - DELETE /classrooms/{} - Failed to delete classroom: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
        }
    }
} 