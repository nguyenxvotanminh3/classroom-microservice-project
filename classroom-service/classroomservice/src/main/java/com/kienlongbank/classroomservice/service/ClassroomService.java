package com.kienlongbank.classroomservice.service;



import com.kienlongbank.classroomservice.dto.ClassroomRequest;
import com.kienlongbank.classroomservice.dto.ClassroomResponse;

import java.util.List;

public interface ClassroomService {
    ClassroomResponse createClassroom(ClassroomRequest classroomRequest);
    ClassroomResponse getClassroomById(Long id);
    List<ClassroomResponse> getAllClassrooms();
    List<ClassroomResponse> getClassroomsByTeacherId(Long teacherId);
    ClassroomResponse updateClassroom(Long id, ClassroomRequest classroomRequest);
    void deleteClassroom(Long id);
} 