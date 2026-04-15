import java.util.ArrayList;
import java.util.Random;

public class GenDatav2 {
    static Random rand = new Random();
    static ArrayList<Integer> listX = new ArrayList<>();
    static ArrayList<Integer> listY = new ArrayList<>();

    public static void main(String[] args) {
        int width = 100;
        int height = 100;
        int numberOfCustomers = 100;
        int numberOfChargingStations = 20;

        double[][] adjacencyMatrix = createAdjacencyMatrix(width, height, numberOfCustomers, numberOfChargingStations);

        if (adjacencyMatrix != null) {
            saveToCSV(adjacencyMatrix, numberOfCustomers, numberOfChargingStations);
        }
    }

    static double[][] createAdjacencyMatrix(int width, int height, int numberOfCustomers,
            int numberOfChargingStations) {

        if (1 + numberOfCustomers + numberOfChargingStations <= width * height) {

            int id = 0;
            listX.clear();
            listY.clear();

            // Depot
            int x = rand.nextInt(width + 1);
            int y = rand.nextInt(height + 1);
            listX.add(x);
            listY.add(y);
            id++;

            // Customer
            while (id <= numberOfCustomers) {
                x = rand.nextInt(width + 1);
                y = rand.nextInt(height + 1);
                boolean isDuplicate = false;
                for (int i = 0; i < listX.size(); i++) {
                    if (x == listX.get(i) && y == listY.get(i)) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    listX.add(x);
                    listY.add(y);
                    id++;
                }
            }

            // Charging Station
            while (id <= (numberOfCustomers + numberOfChargingStations)) {
                x = rand.nextInt(width + 1);
                y = rand.nextInt(height + 1);
                boolean isDuplicate = false;
                for (int i = 0; i < listX.size(); i++) {
                    if (x == listX.get(i) && y == listY.get(i)) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    listX.add(x);
                    listY.add(y);
                    id++;
                }
            }

            // Adjacency Matrix
            double[][] adjacencyMatrix = new double[listX.size()][listY.size()];
            for (int i = 0; i < adjacencyMatrix.length; i++) {
                for (int j = i + 1; j < adjacencyMatrix[i].length; j++) {
                    double dx = listX.get(i) - listX.get(j);
                    double dy = listY.get(i) - listY.get(j);
                    double jarak = Math.sqrt(dx * dx + dy * dy);
                    adjacencyMatrix[i][j] = jarak;
                    adjacencyMatrix[j][i] = jarak;
                }
            }

            return adjacencyMatrix;

        } else {
            System.out.println("Parameter pembangkitan data tidak valid!!!");
            return null;
        }
    }

    static void saveToCSV(double[][] adjacencyMatrix, int numberOfCustomers, int numberOfChargingStations) {
        try {
            // File 1: adjacency_matrix.csv (format sama seperti sebelumnya)
            java.io.PrintWriter writer = new java.io.PrintWriter("Matrix100Customer.csv");

            writer.println(numberOfCustomers + "," + numberOfChargingStations);

            StringBuilder header = new StringBuilder();
            for (int i = 0; i < adjacencyMatrix.length; i++) {
                if (i > 0) header.append(",");
                header.append("V" + i);
            }
            writer.println("," + header.toString());

            for (int i = 0; i < adjacencyMatrix.length; i++) {
                StringBuilder row = new StringBuilder();
                row.append("V" + i);
                for (int j = 0; j < adjacencyMatrix[i].length; j++) {
                    row.append(",");
                    row.append(String.format("%.2f", adjacencyMatrix[i][j]));
                }
                writer.println(row.toString());
            }
            writer.close();
            System.out.println("adjacency_matrix.csv berhasil disimpan!");

            // File 2: coordinates.csv (untuk GUI)
            java.io.PrintWriter coordWriter = new java.io.PrintWriter("coordinates100Customer.csv");
            coordWriter.println(numberOfCustomers + "," + numberOfChargingStations);
            coordWriter.println("id,x,y");
            for (int i = 0; i < listX.size(); i++) {
                coordWriter.println(i + "," + listX.get(i) + "," + listY.get(i));
            }
            coordWriter.close();
            System.out.println("coordinates100Customer.csv berhasil disimpan!");

        } catch (Exception e) {
            System.out.println("Gagal menyimpan CSV: " + e.getMessage());
        }
    }
}
