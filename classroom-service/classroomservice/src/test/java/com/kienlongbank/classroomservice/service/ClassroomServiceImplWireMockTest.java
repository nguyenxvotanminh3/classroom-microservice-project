package com.kienlongbank.classroomservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.kienlongbank.classroomservice.config.WireMockConfig;
import com.kienlongbank.classroomservice.dto.ClassroomRequest;
import com.kienlongbank.classroomservice.dto.ClassroomResponse;
import com.kienlongbank.classroomservice.exception.ClassroomException;
import com.kienlongbank.classroomservice.model.Classroom;
import com.kienlongbank.classroomservice.repository.ClassroomRepository;
import com.kienlongbank.classroomservice.service.impl.ClassroomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"wiremock.server.port=9561"})
@Import(WireMockConfig.class)
@DirtiesContext
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class ClassroomServiceImplWireMockTest {

    @Autowired
    private WireMockServer wireMockServer;

    @Mock
    private ClassroomRepository classroomRepository;

    @Autowired
    private UserServiceClientMock userServiceClientMock;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ClassroomServiceImpl classroomService;

    @BeforeEach
    void setup() {
        classroomService = new ClassroomServiceImpl(classroomRepository, userServiceClientMock, objectMapper);
        
        // Reset WireMock
        wireMockServer.resetAll();
    }

    @Test
    public void testCreateClassroom_withValidTeacher_shouldCreateClassroom() {
        // Given
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Test Classroom");
        request.setCode("TCR001");
        request.setDescription("Test description");
        request.setTeacherId(1L); // Sử dụng teacher ID trong mock
        request.setCapacity(30);

        Classroom savedClassroom = new Classroom();
        savedClassroom.setId(1L);
        savedClassroom.setName(request.getName());
        savedClassroom.setCode(request.getCode());
        savedClassroom.setDescription(request.getDescription());
        savedClassroom.setTeacherId(request.getTeacherId());
        savedClassroom.setCapacity(request.getCapacity());
        savedClassroom.setCreatedAt(LocalDateTime.now());
        savedClassroom.setUpdatedAt(LocalDateTime.now());

        // When
        when(classroomRepository.existsByCode(request.getCode())).thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenReturn(savedClassroom);

        // Chú ý: Không cần stubbing cho userServiceClientMock vì đã implement sẵn

        // Then
        ClassroomResponse response = classroomService.createClassroom(request);

        // Verify
        assertNotNull(response);
        assertEquals(savedClassroom.getId(), response.getId());
        assertEquals(savedClassroom.getName(), response.getName());
        assertEquals(savedClassroom.getCode(), response.getCode());
        assertEquals(savedClassroom.getDescription(), response.getDescription());
        assertEquals(savedClassroom.getTeacherId(), response.getTeacherId());
        assertEquals("Teacher One", response.getTeacherName()); // Từ mock data
        assertEquals(savedClassroom.getCapacity(), response.getCapacity());

        verify(classroomRepository).existsByCode(request.getCode());
        verify(classroomRepository).save(any(Classroom.class));
    }

    @Test
    public void testCreateClassroom_withNonExistingTeacher_shouldThrowException() {
        // Given
        ClassroomRequest request = new ClassroomRequest();
        request.setName("Test Classroom");
        request.setCode("TCR002");
        request.setDescription("Test description");
        request.setTeacherId(999L); // Teacher không tồn tại
        request.setCapacity(30);

        // When
        when(classroomRepository.existsByCode(request.getCode())).thenReturn(false);
        
        // Then
        ClassroomException exception = assertThrows(ClassroomException.class, () -> {
            classroomService.createClassroom(request);
        });

        // Verify
        assertTrue(exception.getMessage().contains("Teacher with ID 999 does not exist"));
        verify(classroomRepository).existsByCode(request.getCode());
        verify(classroomRepository, never()).save(any(Classroom.class));
    }

    @Test
    public void testGetClassroomById_withExistingId_shouldReturnClassroom() {
        // Given
        Long classroomId = 1L;
        Classroom classroom = new Classroom();
        classroom.setId(classroomId);
        classroom.setName("Test Classroom");
        classroom.setCode("TCR001");
        classroom.setDescription("Test description");
        classroom.setTeacherId(1L);
        classroom.setCapacity(30);
        classroom.setCreatedAt(LocalDateTime.now());
        classroom.setUpdatedAt(LocalDateTime.now());

        // When
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        // Then
        ClassroomResponse response = classroomService.getClassroomById(classroomId);

        // Verify
        assertNotNull(response);
        assertEquals(classroom.getId(), response.getId());
        assertEquals(classroom.getName(), response.getName());
        assertEquals(classroom.getCode(), response.getCode());
        assertEquals(classroom.getDescription(), response.getDescription());
        assertEquals(classroom.getTeacherId(), response.getTeacherId());
        assertEquals("Teacher One", response.getTeacherName()); // Từ mock data
        assertEquals(classroom.getCapacity(), response.getCapacity());

        verify(classroomRepository).findById(classroomId);
    }

    @Test
    public void testGetClassroomById_withNonExistingId_shouldThrowException() {
        // Given
        Long classroomId = 999L;

        // When
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.empty());

        // Then
        ClassroomException exception = assertThrows(ClassroomException.class, () -> {
            classroomService.getClassroomById(classroomId);
        });

        // Verify
        assertTrue(exception.getMessage().contains("Classroom not found with id: 999"));
        verify(classroomRepository).findById(classroomId);
    }

    @Test
    public void testGetAllClassrooms_shouldReturnAllClassrooms() {
        // Given
        Classroom classroom1 = new Classroom();
        classroom1.setId(1L);
        classroom1.setName("Test Classroom 1");
        classroom1.setCode("TCR001");
        classroom1.setDescription("Test description 1");
        classroom1.setTeacherId(1L);
        classroom1.setCapacity(30);
        classroom1.setCreatedAt(LocalDateTime.now());
        classroom1.setUpdatedAt(LocalDateTime.now());

        Classroom classroom2 = new Classroom();
        classroom2.setId(2L);
        classroom2.setName("Test Classroom 2");
        classroom2.setCode("TCR002");
        classroom2.setDescription("Test description 2");
        classroom2.setTeacherId(2L);
        classroom2.setCapacity(25);
        classroom2.setCreatedAt(LocalDateTime.now());
        classroom2.setUpdatedAt(LocalDateTime.now());

        List<Classroom> classrooms = Arrays.asList(classroom1, classroom2);

        // When
        when(classroomRepository.findAll()).thenReturn(classrooms);

        // Then
        List<ClassroomResponse> responses = classroomService.getAllClassrooms();

        // Verify
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(classroom1.getId(), responses.get(0).getId());
        assertEquals(classroom1.getName(), responses.get(0).getName());
        assertEquals("Teacher One", responses.get(0).getTeacherName()); // Từ mock data
        assertEquals(classroom2.getId(), responses.get(1).getId());
        assertEquals(classroom2.getName(), responses.get(1).getName());
        assertEquals("Teacher Two", responses.get(1).getTeacherName()); // Từ mock data

        verify(classroomRepository).findAll();
    }
} 