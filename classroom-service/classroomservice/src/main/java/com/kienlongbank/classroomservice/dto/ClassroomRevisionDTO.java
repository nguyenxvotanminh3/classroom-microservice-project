package com.kienlongbank.classroomservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomRevisionDTO {
    private ClassroomDetailDTO classroom;
    private Integer revisionNumber;
    private String revisionType;
    private String username;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date revisionDate;
} 