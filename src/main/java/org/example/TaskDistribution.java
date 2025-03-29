package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.io.File;
import java.io.IOException;

public class TaskDistribution {
    public static void main(String[] args) throws IOException {
        Loader.loadNativeLibraries();

        ObjectMapper mapper = new ObjectMapper();
        Data data = mapper.readValue(new File("src/main/data2.json"), Data.class); // insert here the path to data file

        // Initialize a solver
        MPSolver solver = MPSolver.createSolver("CBC");
        if (solver == null) {
            System.out.println("Could not create solver GLOP");
            return;
        }
        int numTasks = data.getTasks().length;
        int numServers = data.getServers().length;

        // Define the vars
        MPVariable[][] taskOnServer = new MPVariable[numTasks][numServers];
        for (int i = 0; i < numTasks; i++) {
            for (int j = 0; j < numServers; j++) {
                taskOnServer[i][j] = solver.makeIntVar(0, 1, "Task" + (i + 1) + "OnServer" + (j + 1));
            }
        }

        // constraints
        for (int i = 0; i < numServers; i++) {
            Server server = data.getServers()[i];
            MPConstraint cpuConstraint = solver.makeConstraint(0, server.getVCPUs(), "CPU_Server" + (i + 1));
            MPConstraint ramConstraint = solver.makeConstraint(0, server.getRam(), "RAM_Server" + (i + 1));
            MPConstraint diskConstraint = solver.makeConstraint(0, server.getDisk(), "Disk_Server" + (i + 1));

            for (int j = 0; j < numTasks; j++) {
                Task task = data.getTasks()[j];
                cpuConstraint.setCoefficient(taskOnServer[j][i], task.getRequiredVCPUs());
                ramConstraint.setCoefficient(taskOnServer[j][i],task.getRequiredRam());
                diskConstraint.setCoefficient(taskOnServer[j][i], task.getRequiredDisk());
            }
        }

        // dependenices
        for (int i = 0; i < numTasks; i++) {
            Task task = data.getTasks()[i];
            if (task.getDependencyType() == DependencyType.SEQUENTIAL) {
                for (int dependencyId : task.getDependsOnTaskIds()) {
                    MPConstraint sequentialConstraint = solver.makeConstraint(0, 0, "Sequential" + (i + 1) + "_" + dependencyId);
                    for (int j = 0; j < numServers; j++) {
                        sequentialConstraint.setCoefficient(taskOnServer[dependencyId - 1][j], 1);
                        sequentialConstraint.setCoefficient(taskOnServer[i][j], -1);
                    }
                }
            }
        }


        // validate that every task is distributed to server
        for (int i = 0; i < numTasks; i++) {
            MPConstraint taskConstraint = solver.makeConstraint(1, 1, "Task" + (i + 1));
            for (int j = 0; j < numServers; j++) {
                taskConstraint.setCoefficient(taskOnServer[i][j], 1);
            }
        }

        // target function
        MPObjective objective = solver.objective();
        for (int i = 0; i < numTasks; i++) {
            for (int j = 0; j < numServers; j++) {
                objective.setCoefficient(taskOnServer[i][j], 1);
            }
        }

        // solve the distribution
        solver.solve();

        // print the results
        for (int i = 0; i < numTasks; i++) {
            for (int j = 0; j < numServers; j++) {
                if (taskOnServer[i][j].solutionValue() == 1) {
                    System.out.println("Задача " + (i + 1) + " е разпределена на Сървър " + (j + 1));
                }
            }
        }
    }
}
