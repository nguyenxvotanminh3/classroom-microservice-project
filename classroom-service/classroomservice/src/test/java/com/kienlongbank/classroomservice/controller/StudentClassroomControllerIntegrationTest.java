package com.kienlongbank.classroomservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kienlongbank.classroomservice.dto.GradeRequest;
import com.kienlongbank.classroomservice.dto.StudentClassroomRequest;
import com.kienlongbank.classroomservice.dto.StudentClassroomResponse;
import com.kienlongbank.classroomservice.model.Classroom;
import com.kienlongbank.classroomservice.model.StudentClassroom;
import com.kienlongbank.classroomservice.repository.ClassroomRepository;
import com.kienlongbank.classroomservice.repository.StudentClassroomRepository;
import com.kienlongbank.classroomservice.client.UserServiceClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
//import com.kienlongbank.classroomservice.config.TestConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)

public class StudentClassroomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentClassroomRepository studentClassroomRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @MockBean
    private UserServiceClient userServiceClient;
    
    @Autowired
    private CacheManager cacheManager;

    private Classroom createAndSaveClassroom() {
        Classroom classroom = new Classroom();
        classroom.setName("Test Class");
        classroom.setCode("TEST_" + UUID.randomUUID());
        classroom.setTeacherId(100L);
        classroom.setCapacity(30);
        classroom = classroomRepository.save(classroom);
        classroomRepository.flush();
        return classroom;
    }

    private StudentClassroom createAndSaveEnrollment(Long studentId, Long classroomId) {
        StudentClassroom enrollment = new StudentClassroom();
        enrollment.setStudentId(studentId);
        enrollment.setClassroomId(classroomId);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment = studentClassroomRepository.save(enrollment);
        studentClassroomRepository.flush();
        return enrollment;
    }

    @BeforeEach
    void setupMocks() {
        // Clear all caches before each test
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
        
        // Setup mock user
        UserServiceClient.UserDto dummyUser = new UserServiceClient.UserDto();
        dummyUser.setId(1L);
        dummyUser.setUsername("testuser");
        dummyUser.setFullName("Test User");
        dummyUser.setEmail("test@example.com");
        dummyUser.setActive(true);
        when(userServiceClient.getUserById(anyLong())).thenReturn(dummyUser);
        when(userServiceClient.userExists(anyLong())).thenReturn(true);
    }

    @Test
    public void testGetAllStudentClassrooms() throws Exception {
        Classroom classroom = createAndSaveClassroom();
        createAndSaveEnrollment(1L, classroom.getId());

        mockMvc.perform(get("/student-classrooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.studentId == 1 && @.classroomId == " + classroom.getId() + ")]").exists());
    }

    @Test
    public void testEnrollStudent_Integration() throws Exception {
        Classroom classroom = createAndSaveClassroom();
        StudentClassroomRequest request = new StudentClassroomRequest();
        request.setStudentId(1L);
        request.setClassroomId(classroom.getId());

        mockMvc.perform(post("/student-classrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(1L))
                .andExpect(jsonPath("$.classroomId").value(classroom.getId()));
    }

    @Test
    public void testUpdateGrade() throws Exception {
        Classroom classroom = createAndSaveClassroom();
        createAndSaveEnrollment(1L, classroom.getId());

        GradeRequest gradeRequest = new GradeRequest();
        gradeRequest.setGrade(9.5);
        mockMvc.perform(put("/student-classrooms/1/classrooms/" + classroom.getId() + "/grade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gradeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1L))
                .andExpect(jsonPath("$.classroomId").value(classroom.getId()))
                .andExpect(jsonPath("$.grade").value(9.5));
    }

    @Test
    public void testGetStudentsByClassroom() throws Exception {
        Classroom classroom = createAndSaveClassroom();
        createAndSaveEnrollment(1L, classroom.getId());

        mockMvc.perform(get("/student-classrooms/classrooms/" + classroom.getId() + "/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(1L))
                .andExpect(jsonPath("$[0].classroomId").value(classroom.getId()))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    public void testGetClassroomsByStudent() throws Exception {
        Classroom classroom = createAndSaveClassroom();
        createAndSaveEnrollment(1L, classroom.getId());

        mockMvc.perform(get("/student-classrooms/students/1/classrooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(1L))
                .andExpect(jsonPath("$[0].classroomId").value(classroom.getId()))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    public void testUnenrollStudent() throws Exception {
        Classroom classroom = createAndSaveClassroom();
        createAndSaveEnrollment(1L, classroom.getId());

        mockMvc.perform(delete("/student-classrooms/1/classrooms/" + classroom.getId()))
                .andExpect(status().isNoContent());
    }
} 