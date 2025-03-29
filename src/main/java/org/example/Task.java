package org.example;

import lombok.Data;

@Data
public class Task {
    private int id;
    private int requiredVCPUs;
    private int requiredRam;
    private int requiredDisk;
    private DependencyType dependencyType;
    private int[] dependsOnTaskIds;
}
