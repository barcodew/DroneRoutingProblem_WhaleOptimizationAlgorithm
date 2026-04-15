package konvergensiFolder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

public class WOA_v2 {

    // Parameter DRP
    static int numberOfCustomers;
    static int numberOfChargingStations;
    static int totalNodes;
    static double BATTERY_CAPACITY = 500.0;
    static double ALPHA = 15.0;

    // Data
    static double[][] distanceMatrix;

    // Parameter WOA
    static int populationSize = 30;
    static int maxIterations = 200;
    static double b = 1.0;

    static Random rand = new Random();

    public static void main(String[] args) {
        loadDistanceMatrix("adjacency_matrix_kecil.csv");
        System.out.println("Data berhasil dimuat! Total nodes: " + totalNodes);
        System.out.println("Range drone: " + String.format("%.2f", BATTERY_CAPACITY / ALPHA) + " km");
        System.out.println();

        int numberOfRuns = 5;
        double[] bestEnergyPerRun = new double[numberOfRuns];
        double[] timePerRun = new double[numberOfRuns];
        int[] bestSolutionOverall = null;
        double bestFitnessOverall = 0;

        for (int run = 0; run < numberOfRuns; run++) {
            System.out.println("========== RUN " + (run + 1) + " ==========");

            long startTime = System.currentTimeMillis();

            // Inisialisasi populasi (pastikan semua feasible)
            int[][] population = initializePopulation();
            double[] fitnessValues = new double[populationSize];
            int bestIndex = findBestSolution(population, fitnessValues);

            // Simpan solusi terbaik global
            int[] bestSolution = population[bestIndex].clone();
            double bestFitness = fitnessValues[bestIndex];

            // Array untuk simpan konvergensi
            double[] konvergensi = new double[maxIterations];

            if (bestFitness > 0) {
                double energy = (1.0 / bestFitness) - 1;
                System.out.println("Iterasi 0 (awal): Energi = " + String.format("%.2f", energy) + " Wh");
            }

            // Loop utama WOA
            for (int t = 0; t < maxIterations; t++) {

                // Update posisi setiap individu
                for (int i = 0; i < populationSize; i++) {
                    int[] newSolution = updatePosition(population[i], bestSolution, population, t);

                    double newFitness = evaluateFitness(newSolution);

                    // Terima solusi baru kalau lebih baik
                    if (newFitness > fitnessValues[i]) {
                        population[i] = newSolution;
                        fitnessValues[i] = newFitness;
                    }
                }

                // Update solusi terbaik global
                for (int i = 0; i < populationSize; i++) {
                    if (fitnessValues[i] > bestFitness) {
                        bestSolution = population[i].clone();
                        bestFitness = fitnessValues[i];
                    }
                }

                // Simpan konvergensi
                if (bestFitness > 0) {
                    konvergensi[t] = (1.0 / bestFitness) - 1;
                } else {
                    konvergensi[t] = Double.MAX_VALUE;
                }

                // Cetak progress setiap 20 iterasi
                if ((t + 1) % 20 == 0 && bestFitness > 0) {
                    double energy = (1.0 / bestFitness) - 1;
                    System.out.println("Iterasi " + (t + 1) + ": Energi = "
                            + String.format("%.2f", energy) + " Wh");
                }
            }

            long endTime = System.currentTimeMillis();
            long waktu = endTime - startTime;

            // Simpan hasil run
            if (bestFitness > 0) {
                bestEnergyPerRun[run] = (1.0 / bestFitness) - 1;
            } else {
                bestEnergyPerRun[run] = Double.MAX_VALUE;
            }
            timePerRun[run] = waktu;

            // Update solusi terbaik keseluruhan
            if (bestFitness > bestFitnessOverall) {
                bestFitnessOverall = bestFitness;
                bestSolutionOverall = bestSolution.clone();
            }

            System.out.println("Waktu komputasi: " + waktu + " ms");

            // Simpan konvergensi ke CSV
            saveKonvergensi(konvergensi, run + 1);

            System.out.println();
        }

        // Statistik semua run
        System.out.println("========== STATISTIK SEMUA RUN ==========");
        double totalEnergy = 0;
        double minEnergy = Double.MAX_VALUE;
        double maxEnergy = 0;
        double totalTime = 0;
        int feasibleRuns = 0;

        for (int i = 0; i < numberOfRuns; i++) {
            System.out.println("Run " + (i + 1) + ": Energi = "
                    + String.format("%.2f", bestEnergyPerRun[i]) + " Wh | Waktu = "
                    + String.format("%.0f", timePerRun[i]) + " ms");

            if (bestEnergyPerRun[i] != Double.MAX_VALUE) {
                totalEnergy += bestEnergyPerRun[i];
                if (bestEnergyPerRun[i] < minEnergy) minEnergy = bestEnergyPerRun[i];
                if (bestEnergyPerRun[i] > maxEnergy) maxEnergy = bestEnergyPerRun[i];
                feasibleRuns++;
            }
            totalTime += timePerRun[i];
        }

        if (feasibleRuns > 0) {
            double avgEnergy = totalEnergy / feasibleRuns;
            double avgTime = totalTime / numberOfRuns;

            // Hitung standar deviasi
            double sumSquared = 0;
            for (int i = 0; i < numberOfRuns; i++) {
                if (bestEnergyPerRun[i] != Double.MAX_VALUE) {
                    sumSquared += (bestEnergyPerRun[i] - avgEnergy) * (bestEnergyPerRun[i] - avgEnergy);
                }
            }
            double stdDev = Math.sqrt(sumSquared / feasibleRuns);

            System.out.println();
            System.out.println("Feasible runs: " + feasibleRuns + "/" + numberOfRuns);
            System.out.println("Energi terbaik: " + String.format("%.2f", minEnergy) + " Wh");
            System.out.println("Energi terburuk: " + String.format("%.2f", maxEnergy) + " Wh");
            System.out.println("Energi rata-rata: " + String.format("%.2f", avgEnergy) + " Wh");
            System.out.println("Standar deviasi: " + String.format("%.2f", stdDev) + " Wh");
            System.out.println("Waktu rata-rata: " + String.format("%.0f", avgTime) + " ms");
        }

        // Trace solusi terbaik keseluruhan
        if (bestSolutionOverall != null && bestFitnessOverall > 0) {
            System.out.println();
            System.out.println("========== SOLUSI TERBAIK KESELURUHAN ==========");
            printTrace(bestSolutionOverall);
        } else {
            System.out.println("Tidak ada solusi feasible!");
        }
    }

    // =======================================================
    // LOAD DATA
    // =======================================================
    static void loadDistanceMatrix(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));

            String paramLine = br.readLine();
            String[] params = paramLine.split(",");
            numberOfCustomers = Integer.parseInt(params[0]);
            numberOfChargingStations = Integer.parseInt(params[1]);
            totalNodes = 1 + numberOfCustomers + numberOfChargingStations;

            br.readLine(); // skip header

            distanceMatrix = new double[totalNodes][totalNodes];
            int row = 0;
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                for (int col = 0; col < totalNodes; col++) {
                    distanceMatrix[row][col] = Double.parseDouble(parts[col + 1]);
                }
                row++;
            }
            br.close();

            System.out.println("adjacency_matrix.csv berhasil dibaca!");
            System.out.println("Customers: " + numberOfCustomers);
            System.out.println("Charging Stations: " + numberOfChargingStations);

        } catch (Exception e) {
            System.out.println("Gagal membaca CSV: " + e.getMessage());
        }
    }

    // =======================================================
    // REPRESENTASI SOLUSI
    // =======================================================
    static int[] createRandomSolution() {
        int[] solution = new int[numberOfCustomers];
        for (int i = 0; i < numberOfCustomers; i++) {
            solution[i] = i + 1;
        }

        for (int i = solution.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = solution[i];
            solution[i] = solution[j];
            solution[j] = temp;
        }

        return solution;
    }

    // =======================================================
    // INISIALISASI POPULASI (pastikan semua feasible)
    // =======================================================
    static int[][] initializePopulation() {
        int[][] population = new int[populationSize][numberOfCustomers];

        for (int i = 0; i < populationSize; i++) {
            int attempt = 0;
            do {
                population[i] = createRandomSolution();
                attempt++;
            } while (evaluateFitness(population[i]) == 0 && attempt < 100);
        }

        return population;
    }

    static int findBestSolution(int[][] population, double[] fitnessValues) {
        int bestIndex = 0;

        for (int i = 0; i < populationSize; i++) {
            fitnessValues[i] = evaluateFitness(population[i]);

            if (fitnessValues[i] > fitnessValues[bestIndex]) {
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    // =======================================================
    // EVALUASI FITNESS
    // =======================================================
    static int findReachableCS(int fromNode, int targetNode, double currentBattery,
            ArrayList<Integer> visitedCS) {
        int bestCS = -1;
        double minDistToTarget = Double.MAX_VALUE;

        // Prioritas 1: CS yang bisa dijangkau DAN paling dekat ke tujuan
        for (int i = numberOfCustomers + 1; i < totalNodes; i++) {
            if (visitedCS.contains(i)) continue;

            double energiKeCS = ALPHA * distanceMatrix[fromNode][i];
            if (energiKeCS <= currentBattery) {
                double distCSToTarget = distanceMatrix[i][targetNode];
                if (distCSToTarget < minDistToTarget) {
                    minDistToTarget = distCSToTarget;
                    bestCS = i;
                }
            }
        }

        // Prioritas 2: kalau tidak ada, cari CS terdekat manapun
        if (bestCS == -1) {
            double minDist = Double.MAX_VALUE;
            for (int i = numberOfCustomers + 1; i < totalNodes; i++) {
                if (visitedCS.contains(i)) continue;

                double energiKeCS = ALPHA * distanceMatrix[fromNode][i];
                if (energiKeCS <= currentBattery && distanceMatrix[fromNode][i] < minDist) {
                    minDist = distanceMatrix[fromNode][i];
                    bestCS = i;
                }
            }
        }

        return bestCS;
    }

    static boolean bisaLanjutAman(int fromNode, int nextNode, double currentBattery) {
        double energiKeNext = ALPHA * distanceMatrix[fromNode][nextNode];
        double sisaBaterai = currentBattery - energiKeNext;

        if (sisaBaterai < 0) return false;

        double minEnergiKeCS = Double.MAX_VALUE;
        for (int i = numberOfCustomers + 1; i < totalNodes; i++) {
            double energiKeCS = ALPHA * distanceMatrix[nextNode][i];
            if (energiKeCS < minEnergiKeCS) {
                minEnergiKeCS = energiKeCS;
            }
        }

        return sisaBaterai >= minEnergiKeCS;
    }

    static double evaluateFitness(int[] solution) {
        double totalDistance = 0;
        double currentBattery = BATTERY_CAPACITY;
        int currentNode = 0;

        for (int i = 0; i < solution.length; i++) {
            int nextNode = solution[i];
            ArrayList<Integer> visitedCS = new ArrayList<>();

            while (!bisaLanjutAman(currentNode, nextNode, currentBattery)) {
                int cs = findReachableCS(currentNode, nextNode, currentBattery, visitedCS);

                if (cs == -1) {
                    return 0;
                }

                totalDistance += distanceMatrix[currentNode][cs];
                currentBattery = BATTERY_CAPACITY;
                currentNode = cs;
                visitedCS.add(cs);
            }

            double jarak = distanceMatrix[currentNode][nextNode];
            totalDistance += jarak;
            currentBattery -= ALPHA * jarak;
            currentNode = nextNode;
        }

        // Kembali ke depot
        ArrayList<Integer> visitedCS = new ArrayList<>();
        while (ALPHA * distanceMatrix[currentNode][0] > currentBattery) {
            int cs = findReachableCS(currentNode, 0, currentBattery, visitedCS);

            if (cs == -1) {
                return 0;
            }

            totalDistance += distanceMatrix[currentNode][cs];
            currentBattery = BATTERY_CAPACITY;
            currentNode = cs;
            visitedCS.add(cs);
        }

        totalDistance += distanceMatrix[currentNode][0];

        double totalEnergy = ALPHA * totalDistance;
        double fitness = 1.0 / (totalEnergy + 1);
        return fitness;
    }

    // =======================================================
    // MEKANISME WOA
    // =======================================================
    static int jarakPermutasi(int[] a, int[] b) {
        int beda = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) beda++;
        }
        return beda;
    }

    static int[] swapTerarah(int[] solution, int[] reference, int nSwap) {
        int[] result = solution.clone();

        int swapDone = 0;
        for (int i = 0; i < result.length && swapDone < nSwap; i++) {
            if (result[i] != reference[i]) {
                int targetPos = -1;
                for (int j = i + 1; j < result.length; j++) {
                    if (result[j] == reference[i]) {
                        targetPos = j;
                        break;
                    }
                }

                if (targetPos != -1) {
                    int temp = result[i];
                    result[i] = result[targetPos];
                    result[targetPos] = temp;
                    swapDone++;
                }
            }
        }

        return result;
    }

    static int[] swapAcak(int[] solution, int nSwap) {
        int[] result = solution.clone();

        for (int i = 0; i < nSwap; i++) {
            int pos1 = rand.nextInt(result.length);
            int pos2 = rand.nextInt(result.length);
            while (pos2 == pos1) {
                pos2 = rand.nextInt(result.length);
            }

            int temp = result[pos1];
            result[pos1] = result[pos2];
            result[pos2] = temp;
        }

        return result;
    }

    static int[] updatePosition(int[] solution, int[] bestSolution, int[][] population,
            int currentIteration) {
        double a = 2.0 - 2.0 * currentIteration / maxIterations;

        double r1 = rand.nextDouble();
        double p = rand.nextDouble();

        double A = 2.0 * a * r1 - a;

        int[] newSolution;

        if (p < 0.5) {
            if (Math.abs(A) < 1) {
                // Encircling Prey
                int jarak = jarakPermutasi(solution, bestSolution);
                int nSwap = (int) Math.ceil(Math.abs(A) * jarak);
                if (nSwap == 0) nSwap = 1;

                newSolution = swapTerarah(solution, bestSolution, nSwap);

            } else {
                // Search for Prey
                int randIndex = rand.nextInt(population.length);
                int[] xRand = population[randIndex];

                int jarak = jarakPermutasi(solution, xRand);
                int nSwap = (int) Math.ceil(Math.abs(A) * jarak);
                if (nSwap > numberOfCustomers) nSwap = numberOfCustomers;
                if (nSwap == 0) nSwap = 1;

                newSolution = swapTerarah(solution, xRand, nSwap);
            }
        } else {
            // Bubble-Net Attacking
            double l = -1.0 + rand.nextDouble() * 2.0;
            double koefisien = Math.abs(Math.exp(b * l) * Math.cos(2 * Math.PI * l));

            int nSwap = (int) Math.ceil(koefisien * numberOfCustomers);
            if (nSwap > numberOfCustomers) nSwap = numberOfCustomers;
            if (nSwap == 0) nSwap = 1;

            newSolution = swapAcak(solution, nSwap);
        }

        return newSolution;
    }

    // =======================================================
    // OUTPUT
    // =======================================================
    static void printTrace(int[] solution) {
        double totalDistance = 0;
        double currentBattery = BATTERY_CAPACITY;
        int currentNode = 0;
        ArrayList<String> rute = new ArrayList<>();
        rute.add("V0(Depot)");

        System.out.println("=== TRACE RUTE ===");
        System.out.println("Start: V0 (Depot) | Baterai: " + String.format("%.2f", currentBattery) + " Wh");
        System.out.println();

        // Cetak solusi (urutan customer)
        System.out.print("Solusi: [");
        for (int i = 0; i < solution.length; i++) {
            if (i > 0) System.out.print(", ");
            System.out.print("V" + solution[i]);
        }
        System.out.println("]");
        System.out.println();

        for (int i = 0; i < solution.length; i++) {
            int nextNode = solution[i];
            ArrayList<Integer> visitedCS = new ArrayList<>();

            while (!bisaLanjutAman(currentNode, nextNode, currentBattery)) {
                int cs = findReachableCS(currentNode, nextNode, currentBattery, visitedCS);

                if (cs == -1) {
                    System.out.println("GAGAL! Tidak ada CS yang bisa dijangkau");
                    return;
                }

                double jarakKeCS = distanceMatrix[currentNode][cs];
                double energiKeCS = ALPHA * jarakKeCS;

                System.out.println("  [!] Baterai tidak aman ke V" + nextNode
                        + " (butuh " + String.format("%.2f", ALPHA * distanceMatrix[currentNode][nextNode])
                        + " Wh + cadangan ke CS)");
                System.out.println("  --> Hop ke CS V" + cs
                        + " | Jarak: " + String.format("%.2f", jarakKeCS)
                        + " km | Energi: " + String.format("%.2f", energiKeCS) + " Wh");

                totalDistance += jarakKeCS;
                currentBattery = BATTERY_CAPACITY;
                currentNode = cs;
                visitedCS.add(cs);
                rute.add("V" + cs + "(CS)");

                System.out.println("  --> Baterai diisi ulang: " + String.format("%.2f", currentBattery) + " Wh");
                System.out.println();
            }

            double jarak = distanceMatrix[currentNode][nextNode];
            double energi = ALPHA * jarak;
            totalDistance += jarak;
            currentBattery -= energi;

            // Cetak SEBELUM update currentNode
            System.out.println("V" + currentNode + " -> V" + nextNode
                    + " (Customer) | Jarak: " + String.format("%.2f", jarak)
                    + " km | Energi: " + String.format("%.2f", energi)
                    + " Wh | Sisa baterai: " + String.format("%.2f", currentBattery) + " Wh");

            currentNode = nextNode;
            rute.add("V" + nextNode);
        }

        // Kembali ke depot
        System.out.println();
        ArrayList<Integer> visitedCS = new ArrayList<>();
        while (ALPHA * distanceMatrix[currentNode][0] > currentBattery) {
            int cs = findReachableCS(currentNode, 0, currentBattery, visitedCS);

            if (cs == -1) {
                System.out.println("GAGAL! Tidak bisa kembali ke depot");
                return;
            }

            double jarakKeCS = distanceMatrix[currentNode][cs];
            double energiKeCS = ALPHA * jarakKeCS;

            System.out.println("  [!] Baterai tidak cukup ke Depot"
                    + " (butuh " + String.format("%.2f", ALPHA * distanceMatrix[currentNode][0]) + " Wh)");
            System.out.println("  --> Hop ke CS V" + cs
                    + " | Jarak: " + String.format("%.2f", jarakKeCS)
                    + " km | Energi: " + String.format("%.2f", energiKeCS) + " Wh");

            totalDistance += jarakKeCS;
            currentBattery = BATTERY_CAPACITY;
            currentNode = cs;
            visitedCS.add(cs);
            rute.add("V" + cs + "(CS)");

            System.out.println("  --> Baterai diisi ulang: " + String.format("%.2f", currentBattery) + " Wh");
            System.out.println();
        }

        double jarakDepot = distanceMatrix[currentNode][0];
        double energiDepot = ALPHA * jarakDepot;
        totalDistance += jarakDepot;
        currentBattery -= energiDepot;
        rute.add("V0(Depot)");

        System.out.println("V" + currentNode + " -> V0 (Depot) | Jarak: " + String.format("%.2f", jarakDepot)
                + " km | Energi: " + String.format("%.2f", energiDepot)
                + " Wh | Sisa baterai: " + String.format("%.2f", currentBattery) + " Wh");

        double totalEnergy = ALPHA * totalDistance;
        double fitness = 1.0 / (totalEnergy + 1);

        // Cetak rute lengkap
        System.out.println();
        System.out.println("=== RUTE LENGKAP ===");
        System.out.print(rute.get(0));
        for (int i = 1; i < rute.size(); i++) {
            System.out.print(" -> " + rute.get(i));
        }
        System.out.println();

        System.out.println();
        System.out.println("=== RINGKASAN ===");
        System.out.println("Total jarak: " + String.format("%.2f", totalDistance) + " km");
        System.out.println("Total energi: " + String.format("%.2f", totalEnergy) + " Wh");
        System.out.println("Fitness: " + String.format("%.6f", fitness));
    }

    static void saveKonvergensi(double[] konvergensi, int runNumber) {
        try {
            PrintWriter writer = new PrintWriter("E:\\Skripsian\\WOA\\WOA_DRP\\DroneRoutingProblem\\src\\konvergensiFolder\\konvergensi_run" + runNumber + ".csv");
            writer.println("iterasi,energi");
            for (int i = 0; i < konvergensi.length; i++) {
                if (konvergensi[i] != Double.MAX_VALUE) {
                    writer.println((i + 1) + "," + String.format("%.2f", konvergensi[i]));
                }
            }
            writer.close();
            System.out.println("konvergensi_run" + runNumber + ".csv berhasil disimpan!");
        } catch (Exception e) {
            System.out.println("Gagal menyimpan konvergensi: " + e.getMessage());
        }
    }
}