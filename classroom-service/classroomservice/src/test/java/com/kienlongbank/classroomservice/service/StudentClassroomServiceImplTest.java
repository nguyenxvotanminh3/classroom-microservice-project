package com.kienlongbank.classroomservice.service;

import com.kienlongbank.classroomservice.client.UserServiceClient;
import com.kienlongbank.classroomservice.dto.ClassroomResponse;
import com.kienlongbank.classroomservice.dto.GradeRequest;
import com.kienlongbank.classroomservice.dto.StudentClassroomRequest;
import com.kienlongbank.classroomservice.dto.StudentClassroomResponse;
import com.kienlongbank.classroomservice.exception.ClassroomException;
import com.kienlongbank.classroomservice.model.Classroom;
import com.kienlongbank.classroomservice.model.StudentClassroom;
import com.kienlongbank.classroomservice.repository.ClassroomRepository;
import com.kienlongbank.classroomservice.repository.StudentClassroomRepository;
import com.kienlongbank.classroomservice.service.impl.StudentClassroomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class StudentClassroomServiceImplTest {

    @Mock
    private StudentClassroomRepository studentClassroomRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private ClassroomService classroomService;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private StudentClassroomServiceImpl studentClassroomService;

    private Classroom classroom;
    private StudentClassroom studentClassroom;
    private UserServiceClient.UserDto studentDto;
    private ClassroomResponse classroomResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test classroom
        classroom = new Classroom();
        classroom.setId(1L);
        classroom.setName("Math 101");
        classroom.setCode("MTH101");
        classroom.setDescription("Basic Math");
        classroom.setTeacherId(2L);
        classroom.setCapacity(30);
        classroom.setCreatedAt(LocalDateTime.now());
        classroom.setUpdatedAt(LocalDateTime.now());

        // Setup test studentClassroom
        studentClassroom = new StudentClassroom();
        studentClassroom.setId(1L);
        studentClassroom.setStudentId(3L);
        studentClassroom.setClassroomId(1L);
        studentClassroom.setEnrolledAt(LocalDateTime.now());
        studentClassroom.setGrade(null);
        studentClassroom.setFeedback(null);

        // Setup test student
        studentDto = new UserServiceClient.UserDto();
        studentDto.setId(3L);
        studentDto.setUsername("student1");
        studentDto.setFullName("John Student");
        studentDto.setEmail("john@example.com");

        // Setup test classroom response
        classroomResponse = new ClassroomResponse();
        classroomResponse.setId(1L);
        classroomResponse.setName("Math 101");
        classroomResponse.setCode("MTH101");
        classroomResponse.setTeacherId(2L);
        classroomResponse.setTeacherName("Teacher Name");
    }

    @Test
    void enrollStudentToClassroom_shouldEnrollStudentSuccessfully() {
        // Arrange
        StudentClassroomRequest request = new StudentClassroomRequest();
        request.setStudentId(3L);
        request.setClassroomId(1L);

        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(studentClassroomRepository.existsByStudentIdAndClassroomId(3L, 1L)).thenReturn(false);
        when(studentClassroomRepository.save(any(StudentClassroom.class))).thenReturn(studentClassroom);
        when(userServiceClient.getUserById(3L)).thenReturn(studentDto);
        when(classroomService.getClassroomById(1L)).thenReturn(classroomResponse);

        // Act
        StudentClassroomResponse response = studentClassroomService.enrollStudentToClassroom(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(3L);
        assertThat(response.getClassroomId()).isEqualTo(1L);
        assertThat(response.getStudentDetails().getFullName()).isEqualTo("John Student");
        assertThat(response.getClassroom().getName()).isEqualTo("Math 101");
        
        verify(classroomRepository).save(any(Classroom.class)); // Verify capacity updated
        verify(studentClassroomRepository).save(any(StudentClassroom.class));
    }

    @Test
    void enrollStudentToClassroom_shouldThrowExceptionWhenClassroomNotFound() {
        // Arrange
        StudentClassroomRequest request = new StudentClassroomRequest();
        request.setStudentId(3L);
        request.setClassroomId(1L);

        when(classroomRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> studentClassroomService.enrollStudentToClassroom(request))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("Classroom not found");
    }

    @Test
    void enrollStudentToClassroom_shouldThrowExceptionWhenAlreadyEnrolled() {
        // Arrange
        StudentClassroomRequest request = new StudentClassroomRequest();
        request.setStudentId(3L);
        request.setClassroomId(1L);

        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(studentClassroomRepository.existsByStudentIdAndClassroomId(3L, 1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> studentClassroomService.enrollStudentToClassroom(request))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("already enrolled");
    }

    @Test
    void enrollStudentToClassroom_shouldThrowExceptionWhenNoCapacity() {
        // Arrange
        StudentClassroomRequest request = new StudentClassroomRequest();
        request.setStudentId(3L);
        request.setClassroomId(1L);

        classroom.setCapacity(0); // No capacity
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(studentClassroomRepository.existsByStudentIdAndClassroomId(3L, 1L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> studentClassroomService.enrollStudentToClassroom(request))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("No capacity");
    }

    @Test
    void updateStudentGrade_shouldUpdateGradeSuccessfully() {
        // Arrange
        Long studentId = 3L;
        Long classroomId = 1L;
        GradeRequest gradeRequest = new GradeRequest();
        gradeRequest.setGrade(85.5);
        gradeRequest.setFeedback("Good job!");

        when(studentClassroomRepository.findByStudentIdAndClassroomId(studentId, classroomId))
                .thenReturn(Optional.of(studentClassroom));
        when(studentClassroomRepository.save(any(StudentClassroom.class)))
                .thenAnswer(invocation -> {
                    StudentClassroom saved = invocation.getArgument(0);
                    saved.setId(1L); // Ensure ID is set
                    return saved;
                });
        when(userServiceClient.getUserById(studentId)).thenReturn(studentDto);
        when(classroomService.getClassroomById(classroomId)).thenReturn(classroomResponse);

        // Act
        StudentClassroomResponse response = studentClassroomService.updateStudentGrade(studentId, classroomId, gradeRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getGrade()).isEqualTo(85.5);
        assertThat(response.getFeedback()).isEqualTo("Good job!");
        verify(studentClassroomRepository).save(any(StudentClassroom.class));
    }

    @Test
    void updateStudentGrade_shouldThrowExceptionWhenEnrollmentNotFound() {
        // Arrange
        Long studentId = 3L;
        Long classroomId = 1L;
        GradeRequest gradeRequest = new GradeRequest();
        gradeRequest.setGrade(85.5);
        gradeRequest.setFeedback("Good job!");

        when(studentClassroomRepository.findByStudentIdAndClassroomId(studentId, classroomId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> studentClassroomService.updateStudentGrade(studentId, classroomId, gradeRequest))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("Enrollment not found");
    }

    @Test
    void getStudentsByClassroomId_shouldReturnStudentsList() {
        // Arrange
        Long classroomId = 1L;
        List<StudentClassroom> enrollments = Arrays.asList(studentClassroom);
        
        // Sửa mock: existsById thay vì findById để phù hợp với implementation
        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(studentClassroomRepository.findByClassroomId(classroomId)).thenReturn(enrollments);
        when(userServiceClient.getUserById(3L)).thenReturn(studentDto);
        when(classroomService.getClassroomById(classroomId)).thenReturn(classroomResponse);

        // Act
        List<StudentClassroomResponse> responses = studentClassroomService.getStudentsByClassroomId(classroomId);

        // Assert
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStudentId()).isEqualTo(3L);
        assertThat(responses.get(0).getStudentDetails().getFullName()).isEqualTo("John Student");
    }

    @Test
    void getStudentsByClassroomId_shouldThrowExceptionWhenClassroomNotFound() {
        // Arrange
        Long classroomId = 1L;
        
        // Sửa mock: Classroom không tồn tại
        when(classroomRepository.existsById(classroomId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> studentClassroomService.getStudentsByClassroomId(classroomId))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("Classroom not found with id");
    }

    @Test
    void getClassroomsByStudentId_shouldReturnClassroomsList() {
        // Arrange
        Long studentId = 3L;
        List<StudentClassroom> enrollments = Arrays.asList(studentClassroom);
        
        when(studentClassroomRepository.findByStudentId(studentId)).thenReturn(enrollments);
        when(userServiceClient.getUserById(studentId)).thenReturn(studentDto);
        when(classroomService.getClassroomById(1L)).thenReturn(classroomResponse);

        // Act
        List<StudentClassroomResponse> responses = studentClassroomService.getClassroomsByStudentId(studentId);

        // Assert
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getClassroomId()).isEqualTo(1L);
        assertThat(responses.get(0).getClassroom().getName()).isEqualTo("Math 101");
    }

    @Test
    void unenrollStudent_shouldUnenrollSuccessfully() {
        // Arrange
        Long studentId = 3L;
        Long classroomId = 1L;
        
        // Sửa: Sử dụng existsByStudentIdAndClassroomId thay vì findByStudentIdAndClassroomId 
        // theo implementation thực tế
        when(studentClassroomRepository.existsByStudentIdAndClassroomId(studentId, classroomId))
                .thenReturn(true);
        
        // Mock xóa enrollment - Note: không cần mockito.doNothing() vì void method đã được mock by default
        doNothing().when(studentClassroomRepository).deleteByStudentIdAndClassroomId(studentId, classroomId);

        // Act
        studentClassroomService.unenrollStudent(studentId, classroomId);

        // Assert - Verify the repository methods were called with correct arguments
        verify(studentClassroomRepository).existsByStudentIdAndClassroomId(studentId, classroomId);
        verify(studentClassroomRepository).deleteByStudentIdAndClassroomId(studentId, classroomId);
    }

    @Test
    void unenrollStudent_shouldThrowExceptionWhenEnrollmentNotFound() {
        // Arrange
        Long studentId = 3L;
        Long classroomId = 1L;
        
        // Sửa: Mock existsByStudentIdAndClassroomId trả về false
        when(studentClassroomRepository.existsByStudentIdAndClassroomId(studentId, classroomId))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> studentClassroomService.unenrollStudent(studentId, classroomId))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("Student " + studentId + " is not enrolled in classroom " + classroomId);
                
        // Verify repository methods were called
        verify(studentClassroomRepository).existsByStudentIdAndClassroomId(studentId, classroomId);
        // Verify delete method was NOT called
        verify(studentClassroomRepository, never()).deleteByStudentIdAndClassroomId(anyLong(), anyLong());
    }

    @Test
    void getAllStudentClassrooms_shouldReturnAllEnrollments() {
        // Arrange
        List<StudentClassroom> allEnrollments = Arrays.asList(studentClassroom);
        
        when(studentClassroomRepository.findAll()).thenReturn(allEnrollments);
        when(userServiceClient.getUserById(anyLong())).thenReturn(studentDto);
        when(classroomService.getClassroomById(anyLong())).thenReturn(classroomResponse);

        // Act
        List<StudentClassroomResponse> responses = studentClassroomService.getAllStudentClassrooms();

        // Assert
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStudentId()).isEqualTo(3L);
        assertThat(responses.get(0).getClassroomId()).isEqualTo(1L);
    }
} 