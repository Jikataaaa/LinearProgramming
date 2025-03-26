package org.example;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Scanner;

public class TaskDistribution {
    public static void main(String[] args) {
        Loader.loadNativeLibraries();

        Scanner scanner = new Scanner(System.in);

        System.out.print("Въведете броя на сървърите: ");
        int numServers = scanner.nextInt();

        // Дефиниране на сървърите
        int[] serverCPUs = new int[numServers];
        int[] serverRAM = new int[numServers];
        int[] serverDisk = new int[numServers];

        for (int i = 0; i < numServers; i++) {
            System.out.println("Сървър " + (i + 1));
            System.out.print("Въведете броя на CPU ядрата: ");
            serverCPUs[i] = scanner.nextInt();
            System.out.print("Въведете капацитета на RAM (GB): ");
            serverRAM[i] = scanner.nextInt();
            System.out.print("Въведете капацитета на диска (GB): ");
            serverDisk[i] = scanner.nextInt();
        }

        System.out.print("Въведете броя на задачите: ");
        int numTasks = scanner.nextInt();

        // Дефиниране на задачите
        int[] taskCPUs = new int[numTasks];
        int[] taskRAM = new int[numTasks];
        int[] taskDisk = new int[numTasks];
        int[] taskDependencies = new int[numTasks];

        for (int i = 0; i < numTasks; i++) {
            System.out.println("Задача " + (i + 1));
            System.out.print("Въведете изисквания CPU ядра: ");
            taskCPUs[i] = scanner.nextInt();
            System.out.print("Въведете изисквания RAM (GB): ");
            taskRAM[i] = scanner.nextInt();
            System.out.print("Въведете изисквания диск пространство (GB): ");
            taskDisk[i] = scanner.nextInt();
            System.out.print("Въведете номера на зависимата задача (0 ако няма): ");
            taskDependencies[i] = scanner.nextInt();
        }

        // Създаване на солвера
        MPSolver solver = MPSolver.createSolver("CBC");
        if (solver == null) {
            System.out.println("Could not create solver GLOP");
            return;
        }

        // Дефиниране на променливите
        MPVariable[][] taskOnServer = new MPVariable[numTasks][numServers];
        for (int i = 0; i < numTasks; i++) {
            for (int j = 0; j < numServers; j++) {
                taskOnServer[i][j] = solver.makeIntVar(0, 1, "Task" + (i + 1) + "OnServer" + (j + 1));
            }
        }

        // Дефиниране на ограниченията
        for (int i = 0; i < numServers; i++) {
            MPConstraint cpuConstraint = solver.makeConstraint(0, serverCPUs[i], "CPU_Server" + (i + 1));
            MPConstraint ramConstraint = solver.makeConstraint(0, serverRAM[i], "RAM_Server" + (i + 1));
            MPConstraint diskConstraint = solver.makeConstraint(0, serverDisk[i], "Disk_Server" + (i + 1));

            for (int j = 0; j < numTasks; j++) {
                cpuConstraint.setCoefficient(taskOnServer[j][i], taskCPUs[j]);
                ramConstraint.setCoefficient(taskOnServer[j][i], taskRAM[j]);
                diskConstraint.setCoefficient(taskOnServer[j][i], taskDisk[j]);
            }
        }

        // Ограничения за зависимостта между задачите
        for (int i = 0; i < numTasks; i++) {
            if (taskDependencies[i] > 0) {
                MPConstraint dependencyConstraint = solver.makeConstraint(0, 0, "Dependency" + (i + 1));
                for (int j = 0; j < numServers; j++) {
                    dependencyConstraint.setCoefficient(taskOnServer[taskDependencies[i] - 1][j], 1);
                    dependencyConstraint.setCoefficient(taskOnServer[i][j], -1);
                }
            }
        }

        // Ограничения, че всяка задача трябва да бъде разпределена точно на един сървър
        for (int i = 0; i < numTasks; i++) {
            MPConstraint taskConstraint = solver.makeConstraint(1, 1, "Task" + (i + 1));
            for (int j = 0; j < numServers; j++) {
                taskConstraint.setCoefficient(taskOnServer[i][j], 1);
            }
        }

        // Целева функция: Минимизиране на общото време за изпълнение на задачите
        MPObjective objective = solver.objective();
        for (int i = 0; i < numTasks; i++) {
            for (int j = 0; j < numServers; j++) {
                objective.setCoefficient(taskOnServer[i][j], 1);
            }
        }

        // Решаване на задачата
        solver.solve();

        // Отпечатване на резултатите
        for (int i = 0; i < numTasks; i++) {
            for (int j = 0; j < numServers; j++) {
                if (taskOnServer[i][j].solutionValue() == 1) {
                    System.out.println("Задача " + (i + 1) + " е разпределена на Сървър " + (j + 1));
                }
            }
        }
    }
}
