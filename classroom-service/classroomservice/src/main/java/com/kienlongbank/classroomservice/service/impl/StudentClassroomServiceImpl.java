package com.kienlongbank.classroomservice.service.impl;


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
import com.kienlongbank.classroomservice.service.ClassroomService;
import com.kienlongbank.classroomservice.service.StudentClassroomService;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
public class StudentClassroomServiceImpl implements StudentClassroomService {

    private final StudentClassroomRepository studentClassroomRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomService classroomService;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public StudentClassroomServiceImpl(
            StudentClassroomRepository studentClassroomRepository,
            ClassroomRepository classroomRepository,
            ClassroomService classroomService,
            UserServiceClient userServiceClient) {
        this.studentClassroomRepository = studentClassroomRepository;
        this.classroomRepository = classroomRepository;
        this.classroomService = classroomService;
        this.userServiceClient = userServiceClient;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "studentsByClassroom", key = "#request.classroomId"),
        @CacheEvict(value = "classroomsByStudent", key = "#request.studentId")
    })
    public StudentClassroomResponse enrollStudentToClassroom(StudentClassroomRequest request) {
        log.info("📝 ENROLL: Đăng ký học sinh ID {} vào lớp học ID {} và xóa cache liên quan", 
                request.getStudentId(), request.getClassroomId());
        
        try {
            int capacity = 0;
            // Verify classroom exists
            Classroom classroom = classroomRepository.findById(request.getClassroomId())
                    .orElseThrow(() -> new ClassroomException("Classroom not found with id: " + request.getClassroomId()));
            capacity = classroom.getCapacity();
            // Check if student is already enrolled in the classroom
            if (studentClassroomRepository.existsByStudentIdAndClassroomId(
                    request.getStudentId(), request.getClassroomId())) {
                throw new ClassroomException("Student is already enrolled in this classroom");
            }

            // Create a new enrollment
            if(capacity <= 0 ){throw new ClassroomException("No capacity avaiable !");}
            StudentClassroom studentClassroom = new StudentClassroom();
            studentClassroom.setStudentId(request.getStudentId());
            studentClassroom.setClassroomId(request.getClassroomId());
            studentClassroom.setEnrolledAt(LocalDateTime.now());

            StudentClassroom savedEnrollment = studentClassroomRepository.save(studentClassroom);
            //update capacity
            classroom.setCapacity(capacity-1);
            //save classroom
            classroomRepository.save(classroom);
            log.info("📝 ENROLL: Đăng ký học sinh ID {} vào lớp học ID {} thành công", 
                    request.getStudentId(), request.getClassroomId());
                    
            // Try to get student details from User Service
            UserServiceClient.UserDto studentDetails = null;
            try {
                studentDetails = userServiceClient.getUserById(request.getStudentId());
            } catch (Exception e) {
                // Log the error but continue - the enrollment was successful
                log.error("❌ Không thể lấy thông tin học sinh: {}", e.getMessage());
            }

            // Xử lý dữ liệu từ classroomService phòng trường hợp cache trả về LinkedHashMap
            ClassroomResponse classroomResponse = safeGetClassroomResponse(request.getClassroomId());
            
            return convertToResponse(savedEnrollment, studentDetails, classroomResponse);
        } catch (Exception e) {
            log.error("Lỗi khi đăng ký học sinh ID {} vào lớp học ID {}: {}", 
                request.getStudentId(), request.getClassroomId(), e.getMessage(), e);
            
            // Nếu lỗi liên quan đến LinkedHashMap, thử lại với phương thức không dùng cache
            if (e.getMessage() != null && e.getMessage().contains("LinkedHashMap cannot be cast")) {
                log.info("🔄 RETRY: Thử đăng ký lại không qua cache classroom");
                return enrollStudentToClassroomWithoutCache(request);
            }
            throw e;
        }
    }
    
    // Phương thức bổ sung để đăng ký học sinh vào lớp học không qua cache
    private StudentClassroomResponse enrollStudentToClassroomWithoutCache(StudentClassroomRequest request) {
        try {
            int capacity = 0;
            // Verify classroom exists
            Classroom classroom = classroomRepository.findById(request.getClassroomId())
                    .orElseThrow(() -> new ClassroomException("Classroom not found with id: " + request.getClassroomId()));
            capacity = classroom.getCapacity();
            
            // Check if student is already enrolled
            if (studentClassroomRepository.existsByStudentIdAndClassroomId(
                    request.getStudentId(), request.getClassroomId())) {
                throw new ClassroomException("Student is already enrolled in this classroom");
            }

            // Check capacity
            if (capacity <= 0) {
                throw new ClassroomException("No capacity available!");
            }

            // Create enrollment
            StudentClassroom studentClassroom = new StudentClassroom();
            studentClassroom.setStudentId(request.getStudentId());
            studentClassroom.setClassroomId(request.getClassroomId());
            studentClassroom.setEnrolledAt(LocalDateTime.now());

            // Save enrollment
            StudentClassroom savedEnrollment = studentClassroomRepository.save(studentClassroom);
            
            // Update capacity
            classroom.setCapacity(capacity - 1);
            classroomRepository.save(classroom);
            
            log.info("📝 DIRECT ENROLL: Đăng ký học sinh ID {} vào lớp học ID {} thành công (không qua cache)", 
                    request.getStudentId(), request.getClassroomId());
            
            // Get student details
            UserServiceClient.UserDto studentDetails = null;
            try {
                studentDetails = userServiceClient.getUserById(request.getStudentId());
            } catch (Exception e) {
                log.error("❌ Không thể lấy thông tin học sinh: {}", e.getMessage());
            }
            
            // Convert classroom to response directly
            ClassroomResponse classroomResponse = safeGetClassroomResponse(request.getClassroomId());
            
            return convertToResponse(savedEnrollment, studentDetails, classroomResponse);
        } catch (Exception e) {
            log.error("Lỗi khi đăng ký học sinh trực tiếp vào lớp học: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    @Caching(
        put = {
            @CachePut(value = "studentClassroom", key = "#studentId + '-' + #classroomId")
        },
        evict = {
            @CacheEvict(value = "studentsByClassroom", key = "#classroomId"),
            @CacheEvict(value = "classroomsByStudent", key = "#studentId")
        }
    )
    public StudentClassroomResponse updateStudentGrade(Long studentId, Long classroomId, GradeRequest gradeRequest) {
        log.info("📊 UPDATE GRADE: Cập nhật điểm cho học sinh ID {} trong lớp học ID {} và cập nhật cache", 
                studentId, classroomId);
                
        StudentClassroom enrollment = studentClassroomRepository.findByStudentIdAndClassroomId(studentId, classroomId)
                .orElseThrow(() -> new ClassroomException("Enrollment not found for student " + studentId + 
                        " in classroom " + classroomId));
        
        enrollment.setGrade(gradeRequest.getGrade());
        enrollment.setFeedback(gradeRequest.getFeedback());
        
        StudentClassroom updatedEnrollment = studentClassroomRepository.save(enrollment);
        log.info("📊 UPDATE GRADE: Cập nhật điểm cho học sinh ID {} trong lớp học ID {} thành công", studentId, classroomId);
        
        // Fetch related data for the response
        UserServiceClient.UserDto studentDetails = null;
        try {
            studentDetails = userServiceClient.getUserById(studentId);
        } catch (Exception e) {
            log.error("❌ Không thể lấy thông tin học sinh: {}", e.getMessage());
        }
        
        ClassroomResponse classroomResponse = safeGetClassroomResponse(classroomId);
        return convertToResponse(updatedEnrollment, studentDetails, classroomResponse);
    }

    @Override
    @Cacheable(value = "studentsByClassroom", key = "#classroomId")
    public List<StudentClassroomResponse> getStudentsByClassroomId(Long classroomId) {
        log.info("🔍 DATABASE: Lấy danh sách học sinh cho lớp học ID {} từ database", classroomId);
        
        try {
            // Verify classroom exists
            if (!classroomRepository.existsById(classroomId)) {
                throw new ClassroomException("Classroom not found with id: " + classroomId);
            }
            
            List<StudentClassroom> enrollments = studentClassroomRepository.findByClassroomId(classroomId);
            ClassroomResponse classroomResponse = safeGetClassroomResponse(classroomId);
            
            List<StudentClassroomResponse> responses = enrollments.stream()
                    .map(enrollment -> {
                        UserServiceClient.UserDto studentDetails = null;
                        try {
                            studentDetails = userServiceClient.getUserById(enrollment.getStudentId());
                        } catch (Exception e) {
                            log.error("❌ Không thể lấy thông tin học sinh: {}", e.getMessage());
                        }
                        return convertToResponse(enrollment, studentDetails, classroomResponse);
                    })
                    .collect(Collectors.toList());
                    
            log.info("🔍 DATABASE: Tìm thấy {} học sinh cho lớp học ID {} trong database", responses.size(), classroomId);
            return responses;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách học sinh cho lớp học ID {}: {}", classroomId, e.getMessage(), e);
            // Nếu lỗi, thử lấy trực tiếp từ database, không dùng cache
            if (e.getMessage() != null && e.getMessage().contains("LinkedHashMap cannot be cast")) {
                log.info("🔄 RETRY: Thử lấy lại dữ liệu từ database không qua cache");
                return getStudentsFromDatabaseWithoutCache(classroomId);
            }
            throw e;
        }
    }

    @Override
    @Cacheable(value = "classroomsByStudent", key = "#studentId")
    public List<StudentClassroomResponse> getClassroomsByStudentId(Long studentId) {
        log.info("🔍 DATABASE: Lấy danh sách lớp học cho học sinh ID {} từ database", studentId);

        List<StudentClassroom> enrollments = studentClassroomRepository.findByStudentId(studentId);

        List<StudentClassroomResponse> responses = enrollments.stream()
                .map(enrollment -> {
                    UserServiceClient.UserDto studentDetails = null;
                    try {
                        studentDetails = userServiceClient.getUserById(enrollment.getStudentId());
                    } catch (Exception e) {
                        log.error("❌ Không thể lấy thông tin học sinh: {}", e.getMessage());
                    }

                    ClassroomResponse classroomResponse = safeGetClassroomResponse(enrollment.getClassroomId());
                    return convertToResponse(enrollment, studentDetails, classroomResponse);
                })
                        .collect(Collectors.toList());

        log.info("🔍 DATABASE: Tìm thấy {} lớp học cho học sinh ID {} trong database", responses.size(), studentId);
        return responses;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "studentClassroom", key = "#studentId + '-' + #classroomId"),
        @CacheEvict(value = "studentsByClassroom", key = "#classroomId"),
        @CacheEvict(value = "classroomsByStudent", key = "#studentId")
    })
    public void unenrollStudent(Long studentId, Long classroomId) {
        log.info("🗑️ UNENROLL: Hủy đăng ký học sinh ID {} khỏi lớp học ID {} và xóa khỏi cache", 
                studentId, classroomId);
                
        if (!studentClassroomRepository.existsByStudentIdAndClassroomId(studentId, classroomId)) {
            throw new ClassroomException("Student " + studentId + " is not enrolled in classroom " + classroomId);
        }
        
        studentClassroomRepository.deleteByStudentIdAndClassroomId(studentId, classroomId);
        log.info("🗑️ UNENROLL: Hủy đăng ký học sinh ID {} khỏi lớp học ID {} thành công", 
                studentId, classroomId);
    }
    
    private StudentClassroomResponse convertToResponse(
            StudentClassroom enrollment, 
            UserServiceClient.UserDto studentDetails,
            ClassroomResponse classroomResponse) {
        
        return new StudentClassroomResponse(
                enrollment.getId(),
                enrollment.getStudentId(),
                studentDetails,
                enrollment.getClassroomId(),
                classroomResponse,
                enrollment.getGrade(),
                enrollment.getFeedback(),
                enrollment.getEnrolledAt()
        );
    }

    // Phương thức bổ sung để lấy dữ liệu trực tiếp từ database mà không qua cache
    private List<StudentClassroomResponse> getStudentsFromDatabaseWithoutCache(Long classroomId) {
        try {
            // Verify classroom exists
            if (!classroomRepository.existsById(classroomId)) {
                throw new ClassroomException("Classroom not found with id: " + classroomId);
            }
            
            // Lấy thông tin lớp học trực tiếp từ repository, không qua service để tránh cache
            Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ClassroomException("Classroom not found with id: " + classroomId));
            
            // Convert sang ClassroomResponse thủ công
            ClassroomResponse classroomResponse = safeGetClassroomResponse(classroomId);
            
            // Lấy danh sách học sinh
            List<StudentClassroom> enrollments = studentClassroomRepository.findByClassroomId(classroomId);
            
            // Convert sang response
            List<StudentClassroomResponse> responses = enrollments.stream()
                    .map(enrollment -> {
                        UserServiceClient.UserDto studentDetails = null;
                        try {
                            studentDetails = userServiceClient.getUserById(enrollment.getStudentId());
                        } catch (Exception e) {
                            log.error("❌ Không thể lấy thông tin học sinh: {}", e.getMessage());
                        }
                        return convertToResponse(enrollment, studentDetails, classroomResponse);
                    })
                    .collect(Collectors.toList());
                    
            log.info("🔍 DATABASE DIRECT: Tìm thấy {} học sinh cho lớp học ID {} trong database", responses.size(), classroomId);
            return responses;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách học sinh trực tiếp từ database cho lớp học ID {}: {}", classroomId, e.getMessage(), e);
            throw e;
        }
    }
    
    // Phương thức bổ sung để convert Classroom thành ClassroomResponse
    private ClassroomResponse safeGetClassroomResponse(Long classroomId) {
        Object classroomObj = classroomService.getClassroomById(classroomId);
        if (classroomObj instanceof ClassroomResponse) {
            return (ClassroomResponse) classroomObj;
        } else if (classroomObj instanceof java.util.LinkedHashMap) {
            return objectMapper.convertValue(classroomObj, ClassroomResponse.class);
        } else if (classroomObj != null) {
            throw new ClassroomException("Cannot convert classroom object to ClassroomResponse: " + classroomObj.getClass().getName());
        } else {
            throw new ClassroomException("Cannot convert classroom object to ClassroomResponse: null");
        }
    }

    @Override
    public List<StudentClassroomResponse> getAllStudentClassrooms() {
        log.info("🔍 DATABASE: Lấy tất cả enrollments từ database");
        List<StudentClassroom> enrollments = studentClassroomRepository.findAll();
        return enrollments.stream().map(enrollment -> {
            UserServiceClient.UserDto studentDetails = null;
            try {
                studentDetails = userServiceClient.getUserById(enrollment.getStudentId());
            } catch (Exception e) {
                log.error("❌ Không thể lấy thông tin học sinh: {}", e.getMessage());
            }
            ClassroomResponse classroomResponse = null;
            try {
                classroomResponse = safeGetClassroomResponse(enrollment.getClassroomId());
            } catch (Exception e) {
                log.error("❌ Không thể lấy thông tin lớp học: {}", e.getMessage());
            }
            return convertToResponse(enrollment, studentDetails, classroomResponse);
        }).collect(Collectors.toList());
    }
} 