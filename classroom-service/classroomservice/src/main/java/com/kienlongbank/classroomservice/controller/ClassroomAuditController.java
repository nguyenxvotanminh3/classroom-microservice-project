package com.kienlongbank.classroomservice.controller;

import com.kienlongbank.classroomservice.model.Classroom;
import com.kienlongbank.classroomservice.service.ClassroomAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/classrooms")
public class ClassroomAuditController {
    @Autowired
    private ClassroomAuditService classroomAuditService;

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<Classroom>> getClassroomAudit(@PathVariable Long id) {
        List<Classroom> revisions = classroomAuditService.getRevisions(id);
        return ResponseEntity.ok(revisions);
    }
} 