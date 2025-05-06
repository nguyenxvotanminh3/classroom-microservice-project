package com.kienlongbank.temporal.workflow;

import com.kienlongbank.temporal.dto.ClassroomResponse;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ClassroomActivities {
    
    /**
     * Activity to call the classroom service API to get available classrooms
     * 
     * @param token The authentication token obtained from login
     * @param locale The locale for the request
     * @return ClassroomResponse with the list of available classrooms
     */
    @ActivityMethod
    ClassroomResponse getClassrooms(String token, String locale);
} 