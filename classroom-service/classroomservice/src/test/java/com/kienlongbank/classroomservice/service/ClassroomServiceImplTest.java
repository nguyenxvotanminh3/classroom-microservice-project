package com.kienlongbank.classroomservice.service;

import com.kienlongbank.classroomservice.client.UserServiceClient;
import com.kienlongbank.classroomservice.dto.ClassroomRequest;
import com.kienlongbank.classroomservice.dto.ClassroomResponse;
import com.kienlongbank.classroomservice.exception.ClassroomException;
import com.kienlongbank.classroomservice.model.Classroom;
import com.kienlongbank.classroomservice.repository.ClassroomRepository;
import com.kienlongbank.classroomservice.service.impl.ClassroomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class ClassroomServiceImplTest {

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ClassroomServiceImpl classroomService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createClassroom_shouldSaveAndReturnClassroomResponse() {
        ClassroomRequest request = new ClassroomRequest("Math 101", "MTH101", "Basic Math", 1L, 30);
        when(classroomRepository.existsByCode("MTH101")).thenReturn(false);
        when(userServiceClient.userExists(1L)).thenReturn(true);

        Classroom saved = new Classroom();
        saved.setId(1L);
        saved.setName("Math 101");
        saved.setCode("MTH101");
        saved.setDescription("Basic Math");
        saved.setTeacherId(1L);
        saved.setCapacity(30);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());

        when(classroomRepository.save(any())).thenReturn(saved);
        UserServiceClient.UserDto userDto = new UserServiceClient.UserDto();
        userDto.setFullName("Mr. John");
        when(userServiceClient.getUserById(1L)).thenReturn(userDto);

        ClassroomResponse response = classroomService.createClassroom(request);

        assertThat(response.getName()).isEqualTo("Math 101");
        assertThat(response.getCode()).isEqualTo("MTH101");
        assertThat(response.getTeacherName()).isEqualTo("Mr. John");
    }

    @Test
    void getClassroomById_shouldReturnClassroomResponse() {
        Classroom classroom = new Classroom();
        classroom.setId(1L);
        classroom.setName("Physics");
        classroom.setCode("PHY101");
        classroom.setTeacherId(2L);
        classroom.setCreatedAt(LocalDateTime.now());
        classroom.setUpdatedAt(LocalDateTime.now());

        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        UserServiceClient.UserDto userDto = new UserServiceClient.UserDto();
        userDto.setFullName("Ms. Jane");
        when(userServiceClient.getUserById(2L)).thenReturn(userDto);

        ClassroomResponse response = classroomService.getClassroomById(1L);
        assertThat(response.getName()).isEqualTo("Physics");
        assertThat(response.getTeacherName()).isEqualTo("Ms. Jane");
    }

    @Test
    void getClassroomById_shouldThrowExceptionIfNotFound() {
        when(classroomRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classroomService.getClassroomById(1L))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("Classroom not found");
    }

    @Test
    void deleteClassroom_shouldDeleteClassroom() {
        when(classroomRepository.existsById(1L)).thenReturn(true);
        classroomService.deleteClassroom(1L);
        verify(classroomRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteClassroom_shouldThrowIfNotExist() {
        when(classroomRepository.existsById(1L)).thenReturn(false);
        assertThatThrownBy(() -> classroomService.deleteClassroom(1L))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("Classroom not found");
    }
}
