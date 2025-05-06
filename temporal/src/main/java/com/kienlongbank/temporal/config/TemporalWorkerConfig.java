package com.kienlongbank.temporal.config;

import com.kienlongbank.temporal.workflow.LoginWorkflow;
import com.kienlongbank.temporal.workflow.impl.LoginWorkflowImpl;
import com.kienlongbank.temporal.workflow.LoginActivities;
import com.kienlongbank.temporal.workflow.ClassroomActivities;
import com.kienlongbank.temporal.workflow.EmailActivities;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkerConfig {

    @Value("${temporal.task-queue:LoginTaskQueue}")
    private String taskQueue;

    private final WorkerFactory workerFactory;
    private final LoginActivities loginActivities;
    private final ClassroomActivities classroomActivities;
    private final EmailActivities emailActivities;

    @Autowired
    public TemporalWorkerConfig(
            WorkerFactory workerFactory, 
            LoginActivities loginActivities,
            ClassroomActivities classroomActivities,
            EmailActivities emailActivities) {
        this.workerFactory = workerFactory;
        this.loginActivities = loginActivities;
        this.classroomActivities = classroomActivities;
        this.emailActivities = emailActivities;
    }

    @PostConstruct
    public void startWorker() {
        Worker worker = workerFactory.newWorker(taskQueue);
        
        // Register workflow implementation
        worker.registerWorkflowImplementationTypes(LoginWorkflowImpl.class);
        
        // Register activity implementations
        worker.registerActivitiesImplementations(loginActivities, classroomActivities, emailActivities);
        
        // Start worker factory (non-blocking)
        workerFactory.start();
    }
} 