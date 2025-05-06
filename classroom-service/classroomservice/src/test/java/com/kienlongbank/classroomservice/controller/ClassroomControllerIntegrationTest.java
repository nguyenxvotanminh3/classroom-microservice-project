package com.kienlongbank.classroomservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kienlongbank.classroomservice.dto.ClassroomRequest;
import com.kienlongbank.classroomservice.dto.ClassroomResponse;
import com.kienlongbank.classroomservice.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@Import(TestSecurityConfig.class)
public class ClassroomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.kienlongbank.classroomservice.handler.ClassroomHandler classroomHandler;

    @Test
    public void testGetAllClassrooms() throws Exception {
        ClassroomResponse response = new ClassroomResponse();
        response.setId(1L);
        response.setName("Math");
        when(classroomHandler.handleGetAllClassrooms(any())).thenReturn(
                (ResponseEntity) ResponseEntity.ok(List.of(response))
        );
        mockMvc.perform(get("/classrooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Math"));
    }

    @Test
    public void testGetClassroomById() throws Exception {
        ClassroomResponse response = new ClassroomResponse();
        response.setId(2L);
        response.setName("Physics");
        when(classroomHandler.handleGetClassroomById(eq(2L), any())).thenReturn(
                (ResponseEntity) ResponseEntity.ok(response)
        );
        mockMvc.perform(get("/classrooms/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Physics"));
    }

    @Test
    public void testCreateClassroom() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Chemistry");
        request.setCode("CHEM101");
        request.setTeacherId(1L);
        request.setCapacity(30);
        ClassroomResponse response = new ClassroomResponse();
        response.setId(3L);
        response.setName("Chemistry");
        when(classroomHandler.handleCreateClassroom(any(), any(), any())).thenReturn(
                (ResponseEntity) ResponseEntity.status(201).body(response)
        );
        mockMvc.perform(post("/classrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.name").value("Chemistry"));
    }

    @Test
    public void testUpdateClassroom() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Biology");
        request.setCode("BIO101");
        request.setTeacherId(2L);
        request.setCapacity(30);
        ClassroomResponse response = new ClassroomResponse();
        response.setId(4L);
        response.setName("Biology");
        when(classroomHandler.handleUpdateClassroom(eq(4L), any(), any(), any())).thenReturn(
                (ResponseEntity) ResponseEntity.ok(response)
        );
        mockMvc.perform(put("/classrooms/4")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4L))
                .andExpect(jsonPath("$.name").value("Biology"));
    }

    @Test
    public void testDeleteClassroom() throws Exception {
        when(classroomHandler.handleDeleteClassroom(eq(5L), any(), any())).thenReturn(
                (ResponseEntity) ResponseEntity.ok().build()
        );
        mockMvc.perform(delete("/classrooms/5"))
                .andExpect(status().isOk());
    }

    @Test
    public void testCreateClassroomValidationFail() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Chemistry");
        request.setCapacity(30);
        mockMvc.perform(post("/classrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Classroom code is required"))
                .andExpect(jsonPath("$.teacherId").value("Teacher ID is required"));
    }

    @Test
    public void testUpdateClassroomValidationFail() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Biology");
        request.setCapacity(30);
        mockMvc.perform(put("/classrooms/4")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Classroom code is required"))
                .andExpect(jsonPath("$.teacherId").value("Teacher ID is required"));
    }

    @Test
    public void testCreateClassroomMissingName() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setCode("CHEM101");
        request.setTeacherId(1L);
        request.setCapacity(30);
        mockMvc.perform(post("/classrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    public void testCreateClassroomNegativeCapacity() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Chemistry");
        request.setCode("CHEM101");
        request.setTeacherId(1L);
        request.setCapacity(-5);
        mockMvc.perform(post("/classrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.capacity").exists());
    }

    @Test
    public void testCreateClassroomZeroCapacity() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Chemistry");
        request.setCode("CHEM101");
        request.setTeacherId(1L);
        request.setCapacity(0);
        mockMvc.perform(post("/classrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.capacity").exists());
    }

    @Test
    public void testCreateClassroomShortCode() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Chemistry");
        request.setCode("C"); // quá ngắn nếu có @Size(min=2)
        request.setTeacherId(1L);
        request.setCapacity(30);
        mockMvc.perform(post("/classrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    public void testCreateClassroomNegativeTeacherId() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Chemistry");
        request.setCode("CHEM101");
        request.setTeacherId(-1L);
        request.setCapacity(30);
        mockMvc.perform(post("/classrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.teacherId").exists());
    }

    @Test
    public void testCreateClassroomNameTooLong() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setName("A".repeat(300)); // quá dài nếu có @Size(max=255)
        request.setCode("CHEM101");
        request.setTeacherId(1L);
        request.setCapacity(30);
        mockMvc.perform(post("/classrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }
} 