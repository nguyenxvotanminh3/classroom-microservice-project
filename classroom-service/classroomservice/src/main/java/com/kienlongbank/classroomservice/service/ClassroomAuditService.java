package com.kienlongbank.classroomservice.service;

import com.kienlongbank.classroomservice.model.Classroom;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Service
public class ClassroomAuditService {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List getRevisions(Long classroomId) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        return auditReader.createQuery()
                .forRevisionsOfEntity(Classroom.class, false, true)
                .add(AuditEntity.id().eq(classroomId))
                .getResultList();
    }
} 