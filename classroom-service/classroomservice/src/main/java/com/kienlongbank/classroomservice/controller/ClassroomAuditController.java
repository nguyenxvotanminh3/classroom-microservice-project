package com.kienlongbank.classroomservice.controller;

import com.kienlongbank.classroomservice.dto.ClassroomRevisionDTO;
import com.kienlongbank.classroomservice.service.ClassroomAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/classrooms")
@Slf4j
public class ClassroomAuditController {
    @Autowired
    private ClassroomAuditService classroomAuditService;

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<ClassroomRevisionDTO>> getClassroomAudit(@PathVariable Long id) {
        log.info("Received request to get audit history for classroom ID: {}", id);
        List<ClassroomRevisionDTO> revisions = classroomAuditService.getRevisions(id);
        log.info("Returning {} revisions for classroom ID: {}", revisions.size(), id);
        return ResponseEntity.ok(revisions);
    }
} 