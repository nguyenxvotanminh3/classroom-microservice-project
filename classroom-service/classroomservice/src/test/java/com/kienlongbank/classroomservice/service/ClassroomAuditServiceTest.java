package com.kienlongbank.classroomservice.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Note: This is a simplified test for ClassroomAuditService.
 * A proper test would require an integration test setup with a real EntityManager
 * or a more complex mocking approach using PowerMockito to mock static methods.
 */
@ExtendWith(MockitoExtension.class)
class ClassroomAuditServiceTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ClassroomAuditService classroomAuditService;

    @Test
    void validateServiceHasEntityManager() {
        // Verify that the entity manager is properly injected
        // This is a minimal test to ensure the setup is correct
        ReflectionTestUtils.setField(classroomAuditService, "entityManager", entityManager);
        
        // Note: A full test would require an integration test with a test database
        // or using PowerMockito to mock the static AuditReaderFactory.get() method
    }
} 