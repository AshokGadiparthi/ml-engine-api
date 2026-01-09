package com.mlengine.model.enums;

/**
 * Training job status - matches React UI badges.
 */
public enum JobStatus {
    QUEUED,      // Waiting to start
    STARTING,    // Initializing
    TRAINING,    // Currently training
    VALIDATING,  // Running validation
    COMPLETED,   // Successfully finished
    FAILED,      // Error occurred
    STOPPED,     // Manually stopped
    PAUSED       // Manually paused
}
