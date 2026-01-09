package com.mlengine.model.enums;

/**
 * Deployment status - tracks lifecycle of model deployments.
 */
public enum DeploymentStatus {
    PENDING,      // Deployment initiated
    DEPLOYING,    // In progress
    ACTIVE,       // Currently serving predictions (only one per project)
    INACTIVE,     // Was active, now replaced by newer version
    FAILED,       // Deployment failed
    ROLLED_BACK   // Manually rolled back
}
