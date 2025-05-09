package com.kienlongbank.classroomservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kienlongbank.classroomservice.model.Classroom;
import com.kienlongbank.classroomservice.dto.ClassroomDetailDTO;
import com.kienlongbank.classroomservice.dto.ClassroomRevisionDTO;
import com.kienlongbank.classroomservice.model.CustomRevisionEntity;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.RevisionType;

@Service
@Slf4j
public class ClassroomAuditService {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<ClassroomRevisionDTO> getRevisions(Long classroomId) {
        log.info("Fetching audit history for classroom with ID: {}", classroomId);
        try {
            AuditReader auditReader = AuditReaderFactory.get(entityManager);
            List<Object[]> resultList = auditReader.createQuery()
                    .forRevisionsOfEntity(Classroom.class, false, true)
                    .add(AuditEntity.id().eq(classroomId))
                    .getResultList();

            List<ClassroomRevisionDTO> revisions = new ArrayList<>();
            for (Object[] row : resultList) {
                if (row.length >= 3) {
                    try {
                        Classroom classroom = (Classroom) row[0];
                        CustomRevisionEntity revisionEntity = (CustomRevisionEntity) row[1];
                        RevisionType revisionType = (RevisionType) row[2];
                        
                        // Chuyển đổi từ Classroom entity sang ClassroomDetailDTO bằng ModelMapper
                        ClassroomDetailDTO classroomDTO = convertToDTO(classroom);
                        
                        ClassroomRevisionDTO dto = new ClassroomRevisionDTO();
                        dto.setClassroom(classroomDTO);
                        dto.setRevisionNumber(revisionEntity.getId());
                        dto.setRevisionType(revisionType.name());
                        dto.setRevisionDate(revisionEntity.getRevisionDate());
                        dto.setUsername(revisionEntity.getUsername());
                        revisions.add(dto);
                    } catch (Exception e) {
                        log.error("Error processing revision record: {}", e.getMessage());
                    }
                }
            }
            
            log.info("Found {} revisions for classroom ID: {}", revisions.size(), classroomId);
            return revisions;
        } catch (Exception e) {
            log.error("Error retrieving audit history for classroom ID: {}", classroomId, e);
            throw e;
        }
    }
    
    private ClassroomDetailDTO convertToDTO(Classroom classroom) {
        try {
            return modelMapper.map(classroom, ClassroomDetailDTO.class);
        } catch (Exception e) {
            log.error("Error mapping Classroom to DTO: {}", e.getMessage());
            // Fallback to manual mapping
            ClassroomDetailDTO dto = new ClassroomDetailDTO();
            dto.setId(classroom.getId());
            dto.setName(classroom.getName());
            dto.setCode(classroom.getCode());
            dto.setDescription(classroom.getDescription());
            dto.setTeacherId(classroom.getTeacherId());
            dto.setCapacity(classroom.getCapacity());
            dto.setCreatedAt(classroom.getCreatedAt());
            dto.setUpdatedAt(classroom.getUpdatedAt());
            return dto;
        }
    }
} 