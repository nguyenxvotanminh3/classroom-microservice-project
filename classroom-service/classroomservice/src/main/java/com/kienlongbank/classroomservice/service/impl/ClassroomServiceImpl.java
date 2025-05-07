package com.kienlongbank.classroomservice.service.impl;


import com.kienlongbank.api.UserService;
import com.kienlongbank.classroomservice.client.UserServiceClient;
import com.kienlongbank.classroomservice.dto.ClassroomRequest;
import com.kienlongbank.classroomservice.dto.ClassroomResponse;
import com.kienlongbank.classroomservice.exception.ClassroomException;
import com.kienlongbank.classroomservice.model.Classroom;
import com.kienlongbank.classroomservice.repository.ClassroomRepository;
import com.kienlongbank.classroomservice.service.ClassroomService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper;


    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "classrooms", allEntries = true),
        @CacheEvict(value = "classroomsByTeacher", allEntries = true)
    })
    public ClassroomResponse createClassroom(ClassroomRequest classroomRequest) {
        log.info("💾 CREATE: Saving new classroom to database and evicting cache");
        if (classroomRepository.existsByCode(classroomRequest.getCode())) {
            throw new ClassroomException("A classroom with the code " + classroomRequest.getCode() + " already exists");
        }
        
        // Check if teacher exists
        if (!userServiceClient.userExists(classroomRequest.getTeacherId())) {
            throw new ClassroomException("Teacher with ID " + classroomRequest.getTeacherId() + " does not exist");
        }
        
        Classroom classroom = new Classroom();
        classroom.setName(classroomRequest.getName());
        classroom.setCode(classroomRequest.getCode());
        classroom.setDescription(classroomRequest.getDescription());
        classroom.setTeacherId(classroomRequest.getTeacherId());
        classroom.setCapacity(classroomRequest.getCapacity());
        classroom.setCreatedAt(LocalDateTime.now());
        classroom.setUpdatedAt(LocalDateTime.now());

        Classroom savedClassroom = classroomRepository.save(classroom);
        
        return convertToClassroomResponse(savedClassroom);
    }

    @Override
    @Cacheable(value = "classrooms", key = "#id")
    @Transactional(readOnly = true)
    public ClassroomResponse getClassroomById(Long id) {
        log.info("🔍 DATABASE: Fetching classroom with ID {} from database", id);
        Object classroomObj = classroomRepository.findById(id)
                .orElseThrow(() -> new ClassroomException("Classroom not found with id: " + id));
        if (classroomObj instanceof ClassroomResponse) {
            return (ClassroomResponse) classroomObj;
        } else if (classroomObj instanceof java.util.LinkedHashMap) {
            return objectMapper.convertValue(classroomObj, ClassroomResponse.class);
        } else if (classroomObj instanceof Classroom) {
            return convertToClassroomResponse((Classroom) classroomObj);
        } else {
            throw new ClassroomException("Cannot convert classroom object to ClassroomResponse: " + (classroomObj != null ? classroomObj.getClass().getName() : "null"));
        }
    }

    @Override
    @Cacheable(value = "classrooms", key = "'all'")
    @Transactional(readOnly = true)
    public List<ClassroomResponse> getAllClassrooms() {
        log.info("🔍 DATABASE: Fetching all classrooms from database");
        try {
            List<ClassroomResponse> classrooms = classroomRepository.findAll().stream()
                    .map(this::convertToClassroomResponse)
                    .collect(Collectors.toList());
            log.info("🔍 DATABASE: Found {} classrooms in database", classrooms.size());
            return classrooms;
        } catch (Exception e) {
            log.error("❌ ERROR: Failed to fetch classrooms: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "classroomsByTeacher", key = "#teacherId")
    @Transactional(readOnly = true)
    public List<ClassroomResponse> getClassroomsByTeacherId(Long teacherId) {
        log.info("🔍 DATABASE: Fetching classrooms for teacher ID {} from database", teacherId);
        List<ClassroomResponse> classrooms = classroomRepository.findByTeacherId(teacherId).stream()
                .map(this::convertToClassroomResponse)
                .collect(Collectors.toList());
        log.info("🔍 DATABASE: Found {} classrooms for teacher ID {} in database", classrooms.size(), teacherId);
        return classrooms;
    }

    @Override
    @Transactional
    @Caching(
        put = {
            @CachePut(value = "classrooms", key = "#id")
        },
        evict = {
            @CacheEvict(value = "classrooms", key = "'all'"),
            @CacheEvict(value = "classroomsByTeacher", key = "#result.teacherId")
        }
    )
    public ClassroomResponse updateClassroom(Long id, ClassroomRequest classroomRequest) {
        log.info("💾 UPDATE: Updating classroom with ID {} in database and updating cache", id);
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new ClassroomException("Classroom not found with id: " + id));

        // Check if code is being changed and if it already exists
        if (!classroom.getCode().equals(classroomRequest.getCode()) && 
                classroomRepository.existsByCode(classroomRequest.getCode())) {
            throw new ClassroomException("A classroom with the code " + classroomRequest.getCode() + " already exists");
        }

        classroom.setName(classroomRequest.getName());
        classroom.setCode(classroomRequest.getCode());
        classroom.setDescription(classroomRequest.getDescription());
        classroom.setTeacherId(classroomRequest.getTeacherId());
        classroom.setCapacity(classroomRequest.getCapacity());
        classroom.setUpdatedAt(LocalDateTime.now());

        Classroom updatedClassroom = classroomRepository.save(classroom);
        log.info("💾 UPDATE: Classroom with ID {} updated successfully in database", id);
        return convertToClassroomResponse(updatedClassroom);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "classrooms", key = "#id"),
        @CacheEvict(value = "classrooms", key = "'all'"),
        @CacheEvict(value = "classroomsByTeacher", allEntries = true)
    })
    public void deleteClassroom(Long id) {
        log.info("🗑️ DELETE: Removing classroom with ID {} from database and evicting from cache", id);
        if (!classroomRepository.existsById(id)) {
            throw new ClassroomException("Classroom not found with id: " + id);
        }
        classroomRepository.deleteById(id);
        log.info("🗑️ DELETE: Classroom with ID {} successfully removed from database", id);
    }

    private ClassroomResponse convertToClassroomResponse(Classroom classroom) {
        ClassroomResponse response = new ClassroomResponse();
        response.setId(classroom.getId());
        response.setName(classroom.getName());
        response.setCode(classroom.getCode());
        response.setDescription(classroom.getDescription());
        response.setTeacherId(classroom.getTeacherId());
        
        // Get teacher details
        try {
            UserServiceClient.UserDto teacherDetails = userServiceClient.getUserById(classroom.getTeacherId());
            response.setTeacherName(teacherDetails != null ? teacherDetails.getFullName() : "Unknown");
        } catch (Exception e) {
            response.setTeacherName("Unknown (Error fetching teacher details)");
        }
        
        response.setCapacity(classroom.getCapacity());
        response.setCreatedAt(classroom.getCreatedAt());
        response.setUpdatedAt(classroom.getUpdatedAt());
        return response;
    }
} 