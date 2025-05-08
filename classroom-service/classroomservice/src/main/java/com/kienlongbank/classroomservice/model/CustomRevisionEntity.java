package com.kienlongbank.classroomservice.model;

import com.kienlongbank.classroomservice.handler.CustomRevisionListener;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

@Entity
@RevisionEntity(CustomRevisionListener.class)
@Table(name = "revinfo")
@Getter
@Setter
public class CustomRevisionEntity extends DefaultRevisionEntity {
    
    private String username;
    
} 