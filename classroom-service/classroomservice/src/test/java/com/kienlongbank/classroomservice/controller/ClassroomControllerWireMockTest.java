package com.kienlongbank.classroomservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.kienlongbank.classroomservice.config.WireMockConfig;
import com.kienlongbank.classroomservice.config.TestSecurityConfig;
import com.kienlongbank.classroomservice.dto.ClassroomRequest;
import com.kienlongbank.classroomservice.dto.ClassroomResponse;
import com.kienlongbank.classroomservice.service.ClassroomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"wiremock.server.port=9561"})
@Import({WireMockConfig.class, TestSecurityConfig.class})
@DirtiesContext
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ClassroomControllerWireMockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClassroomService classroomService;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();
        
        // Giả lập API bên ngoài: User API
        wireMockServer.stubFor(get(urlEqualTo("/api/users/1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": 1, \"username\": \"teacher1\", \"fullName\": \"Teacher One\", \"email\": \"teacher1@example.com\", \"active\": true}")));
        
        wireMockServer.stubFor(get(urlEqualTo("/api/users/999"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"User not found\"}")));
        
        // Giả lập API bên ngoài: đối với các yêu cầu thất bại
        wireMockServer.stubFor(get(urlMatching("/api/users/error.*"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"Internal server error\"}")));
                
        // Giả lập API bên ngoài: đối với độ trễ cao
        wireMockServer.stubFor(get(urlMatching("/api/users/delay.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": 2, \"username\": \"teacher2\", \"fullName\": \"Teacher Two\", \"email\": \"teacher2@example.com\", \"active\": true}")
                .withFixedDelay(3000))); // Độ trễ 3 giây
    }

    @Test
    public void testGetClassroomById() throws Exception {
        Long classroomId = 1L;
        ClassroomResponse response = new ClassroomResponse();
        response.setId(classroomId);
        response.setName("Test Classroom");
        response.setCode("TCR001");
        response.setDescription("Test description");
        response.setTeacherId(1L);
        response.setTeacherName("Teacher One");
        response.setCapacity(30);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());

        when(classroomService.getClassroomById(classroomId)).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/classrooms/{id}", classroomId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", is("Test Classroom")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code", is("TCR001")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.teacherId", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.teacherName", is("Teacher One")));
        
        // Verify WireMock: Mặc dù API bên ngoài đã được stubbed, 
        // nhưng không được gọi trực tiếp trong controller test
        // Vì chúng ta đã mock classroomService
    }

    @Test
    public void testCreateClassroom() throws Exception {
        ClassroomRequest request = new ClassroomRequest();
        request.setName("New Classroom");
        request.setCode("NCR001");
        request.setDescription("New classroom description");
        request.setTeacherId(1L);
        request.setCapacity(25);

        ClassroomResponse response = new ClassroomResponse();
        response.setId(3L);
        response.setName(request.getName());
        response.setCode(request.getCode());
        response.setDescription(request.getDescription());
        response.setTeacherId(request.getTeacherId());
        response.setTeacherName("Teacher One");
        response.setCapacity(request.getCapacity());
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());

        when(classroomService.createClassroom(any(ClassroomRequest.class))).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/classrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", is("New Classroom")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code", is("NCR001")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.teacherId", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.teacherName", is("Teacher One")));
    }

    @Test
    public void testGetAllClassrooms() throws Exception {
        ClassroomResponse response1 = new ClassroomResponse();
        response1.setId(1L);
        response1.setName("Classroom 1");
        response1.setCode("CR001");
        response1.setDescription("Description 1");
        response1.setTeacherId(1L);
        response1.setTeacherName("Teacher One");
        response1.setCapacity(30);
        response1.setCreatedAt(LocalDateTime.now());
        response1.setUpdatedAt(LocalDateTime.now());

        ClassroomResponse response2 = new ClassroomResponse();
        response2.setId(2L);
        response2.setName("Classroom 2");
        response2.setCode("CR002");
        response2.setDescription("Description 2");
        response2.setTeacherId(2L);
        response2.setTeacherName("Teacher Two");
        response2.setCapacity(25);
        response2.setCreatedAt(LocalDateTime.now());
        response2.setUpdatedAt(LocalDateTime.now());

        List<ClassroomResponse> responses = Arrays.asList(response1, response2);

        when(classroomService.getAllClassrooms()).thenReturn(responses);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/classrooms"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name", is("Classroom 1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].teacherName", is("Teacher One")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].name", is("Classroom 2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].teacherName", is("Teacher Two")));
    }

    @Test
    public void testUpdateClassroom() throws Exception {
        Long classroomId = 1L;
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Updated Classroom");
        request.setCode("UCR001");
        request.setDescription("Updated description");
        request.setTeacherId(1L);
        request.setCapacity(35);

        ClassroomResponse response = new ClassroomResponse();
        response.setId(classroomId);
        response.setName(request.getName());
        response.setCode(request.getCode());
        response.setDescription(request.getDescription());
        response.setTeacherId(request.getTeacherId());
        response.setTeacherName("Teacher One");
        response.setCapacity(request.getCapacity());
        response.setCreatedAt(LocalDateTime.now().minusDays(1));
        response.setUpdatedAt(LocalDateTime.now());

        when(classroomService.updateClassroom(eq(classroomId), any(ClassroomRequest.class))).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/classrooms/{id}", classroomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", is("Updated Classroom")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code", is("UCR001")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.teacherId", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.teacherName", is("Teacher One")));
    }

    @Test
    public void testDeleteClassroom() throws Exception {
        Long classroomId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/classrooms/{id}", classroomId))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }
} 