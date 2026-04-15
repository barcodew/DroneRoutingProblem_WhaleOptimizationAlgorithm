import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class DroneRoutingGUI extends Application {

    // Canvas
    Canvas mapCanvas;
    GraphicsContext gc;
    double canvasWidth = 700;
    double canvasHeight = 700;
    double padding = 50;

    // Data node
    int numberOfCustomers;
    int numberOfChargingStations;
    int totalNodes;
    double[] nodeX;
    double[] nodeY;
    double maxCoord = 20;

    // Solusi
    int[] solution;
    ArrayList<Integer> fullRoute;

    // Animasi
    int currentSegment = 0;
    double droneX, droneY;
    double targetX, targetY;
    double animProgress = 0;
    boolean isAnimating = false;
    double animSpeed = 0.015;
    double currentBattery;
    double segmentStartBattery;
    double segmentEndBattery;
    double totalEnergyUsed = 0;
    double totalDistanceTraveled = 0;
    AnimationTimer animTimer;
    double propellerAngle = 0;

    // UI
    Label lblStatus, lblBattery, lblCurrentNode, lblEnergy, lblDistance, lblFitness, lblStep;
    ProgressBar batteryBar;
    Button btnAnimate, btnReset;
    Slider speedSlider;
    TextArea logArea;

    @Override
    public void start(Stage stage) {
        loadCoordinates("coordinates50Customer.csv");
        WOA.loadDistanceMatrix("Matrix50Customer.csv");
        loadSolution("solution50Customer.csv");

        fullRoute = buildFullRoute(solution);

        double fitness = WOA.evaluateFitness(solution);
        double totalEnergy = (fitness > 0) ? (1.0 / fitness) - 1 : 0;
        double totalDistance = totalEnergy / WOA.ALPHA;

        // === ROOT ===
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0F1923;");

        // === TOP BAR ===
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(12, 20, 12, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: linear-gradient(to right, #1B2A4A, #2C3E6B);");

        Label title = new Label("DRONE ROUTING PROBLEM");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label subtitle = new Label("Whale Optimization Algorithm");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        subtitle.setTextFill(Color.web("#7B9FCC"));

        topBar.getChildren().addAll(title, spacer, subtitle);
        root.setTop(topBar);

        // === MAP ===
        mapCanvas = new Canvas(canvasWidth, canvasHeight);
        gc = mapCanvas.getGraphicsContext2D();

        StackPane mapContainer = new StackPane(mapCanvas);
        mapContainer.setPadding(new Insets(10));
        root.setCenter(mapContainer);

        // === RIGHT PANEL ===
        VBox rightPanel = new VBox(8);
        rightPanel.setPadding(new Insets(15));
        rightPanel.setPrefWidth(300);
        rightPanel.setStyle("-fx-background-color: #152238;");

        Label lblInfo = sectionLabel("HASIL OPTIMASI");
        lblFitness = infoLabel("Fitness: " + String.format("%.6f", fitness));
        lblEnergy = infoLabel("Total Energi: " + String.format("%.2f", totalEnergy) + " Wh");
        lblDistance = infoLabel("Total Jarak: " + String.format("%.2f", totalDistance) + " km");
        lblStep = infoLabel("Node dalam rute: " + fullRoute.size());

        Separator sep1 = new Separator();

        Label lblAnim = sectionLabel("ANIMASI DRONE");
        lblCurrentNode = infoLabel("Posisi: V0 (Depot)");
        lblBattery = infoLabel("Baterai: " + String.format("%.0f", WOA.BATTERY_CAPACITY) + " Wh");

        batteryBar = new ProgressBar(1.0);
        batteryBar.setPrefWidth(270);
        batteryBar.setPrefHeight(18);
        batteryBar.setStyle("-fx-accent: #00E676;");

        Separator sep2 = new Separator();

        Label lblCtrl = sectionLabel("KONTROL");

        btnAnimate = new Button("▶  PLAY ANIMASI");
        styleButton(btnAnimate, "#27AE60");
        btnAnimate.setOnAction(e -> {
            if (isAnimating) {
                pauseAnimation();
            } else {
                startAnimation();
            }
        });

        btnReset = new Button("↺  RESET");
        styleButton(btnReset, "#E74C3C");
        btnReset.setOnAction(e -> resetAnimation());

        Label lblSpeed = infoLabel("Kecepatan:");
        speedSlider = new Slider(0.003, 0.06, 0.015);
        speedSlider.setPrefWidth(270);
        speedSlider.valueProperty().addListener((o, ov, nv) -> animSpeed = nv.doubleValue());

        Separator sep3 = new Separator();

        Label lblLeg = sectionLabel("LEGENDA");
        HBox leg1 = legendItem("#E67E22", "■", "Depot");
        HBox leg2 = legendItem("#3498DB", "●", "Customer");
        HBox leg3 = legendItem("#2ECC71", "◆", "Charging Station");
        HBox leg4 = legendItem("#1E3A5F", "┈", "Graf (jangkauan)");
        HBox leg5 = legendItem("#00E5FF", "─", "Rute Drone");
        HBox leg6 = legendItem("#FF5722", "✦", "Drone");

        Separator sep4 = new Separator();

        lblStatus = infoLabel("Status: Siap");
        lblStatus.setTextFill(Color.web("#00E676"));

        rightPanel.getChildren().addAll(
                lblInfo, lblFitness, lblEnergy, lblDistance, lblStep,
                sep1, lblAnim, lblCurrentNode, lblBattery, batteryBar,
                sep2, lblCtrl, btnAnimate, btnReset, lblSpeed, speedSlider,
                sep3, lblLeg, leg1, leg2, leg3, leg4, leg5, leg6,
                sep4, lblStatus
        );

        ScrollPane scrollRight = new ScrollPane(rightPanel);
        scrollRight.setFitToWidth(true);
        scrollRight.setStyle("-fx-background: #152238; -fx-border-color: transparent;");
        root.setRight(scrollRight);

        // === LOG AREA ===
        logArea = new TextArea();
        logArea.setPrefHeight(100);
        logArea.setEditable(false);
        logArea.setStyle("-fx-control-inner-background: #0A1628; -fx-text-fill: #00E676; "
                + "-fx-font-family: 'Consolas'; -fx-font-size: 11;");

        logArea.setText(">> Data dimuat: " + numberOfCustomers + " customers, "
                + numberOfChargingStations + " CS\n");
        logArea.appendText(">> Solusi dimuat dari solution.csv\n");
        logArea.appendText(">> Rute lengkap: ");
        for (int i = 0; i < fullRoute.size(); i++) {
            int n = fullRoute.get(i);
            if (i > 0) logArea.appendText(" -> ");
            logArea.appendText(getNodeLabel(n));
        }
        logArea.appendText("\n>> Tekan PLAY untuk mulai animasi\n");

        VBox bottomBox = new VBox();
        bottomBox.setPadding(new Insets(5, 10, 10, 10));
        bottomBox.getChildren().add(logArea);
        root.setBottom(bottomBox);

        // Draw initial map
        double depotSX = toScreenX(nodeX[0]);
        double depotSY = toScreenY(nodeY[0]);
        drawMap(-1, depotSX, depotSY);

        // Setup animation timer
        animTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isAnimating) return;

                animProgress += animSpeed;
                propellerAngle += 15;

                if (animProgress >= 1.0) {
                    int arrivedNode = fullRoute.get(currentSegment + 1);
                    onSegmentComplete(arrivedNode);

                    animProgress = 0;
                    currentSegment++;

                    if (currentSegment >= fullRoute.size() - 1) {
                        isAnimating = false;
                        lblStatus.setText("Status: Animasi selesai!\nMade with love by Barcodew!");
                        lblStatus.setTextFill(Color.web("#00E676"));
                        btnAnimate.setText("▶  PLAY ANIMASI");
                        logArea.appendText(">> Animasi selesai! Drone kembali ke depot.\n");

                        double dx = toScreenX(nodeX[0]);
                        double dy = toScreenY(nodeY[0]);
                        drawMap(fullRoute.size() - 1, dx, dy);
                        drawDrone(dx, dy);
                        updateBatteryDisplay(1.0);
                        return;
                    }

                    int fromNode = fullRoute.get(currentSegment);
                    int toNode = fullRoute.get(currentSegment + 1);

                    updateDroneInfo(fromNode, toNode);

                    droneX = toScreenX(nodeX[fromNode]);
                    droneY = toScreenY(nodeY[fromNode]);
                    targetX = toScreenX(nodeX[toNode]);
                    targetY = toScreenY(nodeY[toNode]);
                }

                double cx = droneX + (targetX - droneX) * animProgress;
                double cy = droneY + (targetY - droneY) * animProgress;

                updateBatteryDisplay(animProgress);

                drawMap(currentSegment, cx, cy);
                drawDrone(cx, cy);
            }
        };

        Scene scene = new Scene(root, 1080, 870);
        stage.setTitle("Drone Routing - WOA Visualization");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    // ===========================
    // DATA LOADING
    // ===========================

    void loadCoordinates(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));

            String paramLine = br.readLine();
            String[] params = paramLine.split(",");
            numberOfCustomers = Integer.parseInt(params[0]);
            numberOfChargingStations = Integer.parseInt(params[1]);
            totalNodes = 1 + numberOfCustomers + numberOfChargingStations;

            br.readLine();

            nodeX = new double[totalNodes];
            nodeY = new double[totalNodes];
            maxCoord = 0;

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                nodeX[id] = Double.parseDouble(parts[1]);
                nodeY[id] = Double.parseDouble(parts[2]);

                if (nodeX[id] > maxCoord) maxCoord = nodeX[id];
                if (nodeY[id] > maxCoord) maxCoord = nodeY[id];
            }
            maxCoord += 2;
            br.close();
        } catch (Exception e) {
            System.out.println("Gagal baca coordinates.csv: " + e.getMessage());
        }
    }

    void loadSolution(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line = br.readLine();
            String[] parts = line.split(",");

            solution = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                solution[i] = Integer.parseInt(parts[i].trim());
            }
            br.close();
        } catch (Exception e) {
            System.out.println("Gagal baca solution.csv: " + e.getMessage());
        }
    }

    ArrayList<Integer> buildFullRoute(int[] solution) {
        ArrayList<Integer> route = new ArrayList<>();
        route.add(0);

        double battery = WOA.BATTERY_CAPACITY;
        int cur = 0;

        for (int i = 0; i < solution.length; i++) {
            int next = solution[i];
            ArrayList<Integer> visited = new ArrayList<>();

            while (!WOA.bisaLanjutAman(cur, next, battery)) {
                int cs = WOA.findReachableCS(cur, next, battery, visited);
                if (cs == -1) return route;

                route.add(cs);
                battery = WOA.BATTERY_CAPACITY;
                cur = cs;
                visited.add(cs);
            }

            battery -= WOA.ALPHA * WOA.distanceMatrix[cur][next];
            cur = next;
            route.add(next);
        }

        ArrayList<Integer> visited = new ArrayList<>();
        while (WOA.ALPHA * WOA.distanceMatrix[cur][0] > battery) {
            int cs = WOA.findReachableCS(cur, 0, battery, visited);
            if (cs == -1) return route;

            route.add(cs);
            battery = WOA.BATTERY_CAPACITY;
            cur = cs;
            visited.add(cs);
        }

        route.add(0);
        return route;
    }

    // ===========================
    // ANIMASI
    // ===========================

    void startAnimation() {
        if (currentSegment >= fullRoute.size() - 1) {
            resetAnimation();
        }

        isAnimating = true;
        btnAnimate.setText("⏸  PAUSE");
        lblStatus.setText("Status: Animasi berjalan...");
        lblStatus.setTextFill(Color.web("#FFD700"));

        int fromNode = fullRoute.get(currentSegment);
        int toNode = fullRoute.get(currentSegment + 1);

        droneX = toScreenX(nodeX[fromNode]);
        droneY = toScreenY(nodeY[fromNode]);
        targetX = toScreenX(nodeX[toNode]);
        targetY = toScreenY(nodeY[toNode]);

        if (currentSegment == 0) {
            currentBattery = WOA.BATTERY_CAPACITY;
            totalEnergyUsed = 0;
            totalDistanceTraveled = 0;
            logArea.appendText(">> Drone lepas landas dari depot!\n");
        }

        updateDroneInfo(fromNode, toNode);
        animTimer.start();
    }

    void pauseAnimation() {
        isAnimating = false;
        btnAnimate.setText("▶  PLAY ANIMASI");
        lblStatus.setText("Status: Dijeda");
        lblStatus.setTextFill(Color.web("#FFD700"));
    }

    void resetAnimation() {
        isAnimating = false;
        animTimer.stop();
        currentSegment = 0;
        animProgress = 0;
        currentBattery = WOA.BATTERY_CAPACITY;
        segmentStartBattery = WOA.BATTERY_CAPACITY;
        segmentEndBattery = WOA.BATTERY_CAPACITY;
        totalEnergyUsed = 0;
        totalDistanceTraveled = 0;

        lblCurrentNode.setText("Posisi: V0 (Depot)");
        lblBattery.setText("Baterai: " + String.format("%.0f", WOA.BATTERY_CAPACITY) + " Wh");
        batteryBar.setProgress(1.0);
        batteryBar.setStyle("-fx-accent: #00E676;");
        btnAnimate.setText("▶  PLAY ANIMASI");
        lblStatus.setText("Status: Siap");
        lblStatus.setTextFill(Color.web("#00E676"));
        logArea.appendText(">> Reset animasi.\n");

        double depotSX = toScreenX(nodeX[0]);
        double depotSY = toScreenY(nodeY[0]);
        drawMap(-1, depotSX, depotSY);
    }

    void updateDroneInfo(int fromNode, int toNode) {
        double jarak = WOA.distanceMatrix[fromNode][toNode];
        double energi = WOA.ALPHA * jarak;

        segmentStartBattery = currentBattery;
        segmentEndBattery = currentBattery - energi;

        lblCurrentNode.setText("Posisi: " + getNodeLabel(fromNode) + " -> " + getNodeLabel(toNode));

        totalEnergyUsed += energi;
        totalDistanceTraveled += jarak;
    }

    void onSegmentComplete(int arrivedNode) {
        currentBattery = segmentEndBattery;
        if (currentBattery < 0) currentBattery = 0;

        if (isChargingStation(arrivedNode)) {
            currentBattery = WOA.BATTERY_CAPACITY;
            logArea.appendText(">> ⚡ Charging di " + getNodeLabel(arrivedNode) + " | Baterai penuh!\n");
        } else if (arrivedNode == 0) {
            logArea.appendText(">> Drone tiba di depot.\n");
        } else {
            logArea.appendText(">> Tiba di " + getNodeLabel(arrivedNode)
                    + " | Sisa: " + String.format("%.1f", currentBattery) + " Wh\n");
        }
    }

    void updateBatteryDisplay(double progress) {
        double displayBattery = segmentStartBattery + (segmentEndBattery - segmentStartBattery) * progress;
        if (displayBattery < 0) displayBattery = 0;

        lblBattery.setText("Baterai: " + String.format("%.1f", displayBattery) + " / "
                + String.format("%.0f", WOA.BATTERY_CAPACITY) + " Wh");

        double pct = displayBattery / WOA.BATTERY_CAPACITY;
        batteryBar.setProgress(pct);
        if (pct > 0.5) batteryBar.setStyle("-fx-accent: #00E676;");
        else if (pct > 0.25) batteryBar.setStyle("-fx-accent: #FFD700;");
        else batteryBar.setStyle("-fx-accent: #E74C3C;");
    }

    // ===========================
    // DRAWING
    // ===========================

    double toScreenX(double x) {
        return padding + (x / maxCoord) * (canvasWidth - 2 * padding);
    }

    double toScreenY(double y) {
        return canvasHeight - padding - (y / maxCoord) * (canvasHeight - 2 * padding);
    }

    void drawMap(int completedSegment, double droneCurrentX, double droneCurrentY) {
        // Background
        gc.setFill(Color.web("#0A1628"));
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        // Grid
        gc.setStroke(Color.web("#1A2744"));
        gc.setLineWidth(0.5);
        gc.setFill(Color.web("#3A4F6F"));
        gc.setFont(Font.font("Arial", 9));

        for (int i = 0; i <= maxCoord; i += 2) {
            double sx = toScreenX(i);
            double sy = toScreenY(i);
            gc.strokeLine(sx, padding, sx, canvasHeight - padding);
            gc.strokeLine(padding, sy, canvasWidth - padding, sy);
            gc.fillText(String.valueOf(i), sx - 5, canvasHeight - padding + 15);
            gc.fillText(String.valueOf(i), padding - 20, sy + 4);
        }

        // Graph edges - bayangan koneksi yang bisa dijangkau drone
        double maxRange = WOA.BATTERY_CAPACITY / WOA.ALPHA;
        gc.setStroke(Color.web("#fcfcfc"));
        gc.setLineWidth(0.3);
        gc.setGlobalAlpha(0.2);

        for (int i = 0; i < totalNodes; i++) {
            for (int j = i + 1; j < totalNodes; j++) {
                if (WOA.distanceMatrix[i][j] <= maxRange) {
                    double x1 = toScreenX(nodeX[i]);
                    double y1 = toScreenY(nodeY[i]);
                    double x2 = toScreenX(nodeX[j]);
                    double y2 = toScreenY(nodeY[j]);
                    gc.strokeLine(x1, y1, x2, y2);
                }
            }
        }

        gc.setGlobalAlpha(1.0);

        // Route lines - HANYA yang sudah dilalui
        if (fullRoute != null && fullRoute.size() > 1) {

            // Segmen yang sudah selesai
            for (int i = 0; i < completedSegment; i++) {
                int from = fullRoute.get(i);
                int to = fullRoute.get(i + 1);
                double x1 = toScreenX(nodeX[from]);
                double y1 = toScreenY(nodeY[from]);
                double x2 = toScreenX(nodeX[to]);
                double y2 = toScreenY(nodeY[to]);

                gc.setStroke(Color.web("#00E5FF"));
                gc.setLineWidth(2.5);
                gc.setGlobalAlpha(0.9);
                gc.strokeLine(x1, y1, x2, y2);
                gc.setGlobalAlpha(1.0);

                drawArrow(x1, y1, x2, y2, true);
            }

            // Segmen yang sedang dilalui (partial - sampai posisi drone)
            if (completedSegment >= 0 && completedSegment < fullRoute.size() - 1) {
                int from = fullRoute.get(completedSegment);
                double x1 = toScreenX(nodeX[from]);
                double y1 = toScreenY(nodeY[from]);

                gc.setStroke(Color.web("#00E5FF"));
                gc.setLineWidth(2.5);
                gc.setGlobalAlpha(0.9);
                gc.strokeLine(x1, y1, droneCurrentX, droneCurrentY);
                gc.setGlobalAlpha(1.0);
            }
        }

        // Nodes
        for (int i = 0; i < totalNodes; i++) {
            double sx = toScreenX(nodeX[i]);
            double sy = toScreenY(nodeY[i]);

            if (i == 0) {
                drawDepot(sx, sy);
            } else if (isChargingStation(i)) {
                drawChargingStation(sx, sy, i);
            } else {
                drawCustomer(sx, sy, i);
            }
        }
    }

    void drawDepot(double x, double y) {
        double size = 20;

        gc.setFill(Color.web("#E67E22", 0.2));
        gc.fillOval(x - 15, y - 15, 30, 30);

        gc.setFill(Color.web("#E67E22"));
        gc.fillRect(x - size / 2, y - size / 2, size, size);
        gc.setStroke(Color.web("#F39C12"));
        gc.setLineWidth(2);
        gc.strokeRect(x - size / 2, y - size / 2, size, size);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        gc.fillText("D", x - 4, y + 4);
    }

    void drawCustomer(double x, double y, int id) {
        double r = 7;

        gc.setFill(Color.web("#3498DB"));
        gc.fillOval(x - r, y - r, r * 2, r * 2);
        gc.setStroke(Color.web("#2980B9"));
        gc.setLineWidth(1.5);
        gc.strokeOval(x - r, y - r, r * 2, r * 2);

        gc.setFill(Color.web("#AED6F1"));
        gc.setFont(Font.font("Arial", 8));
        gc.fillText("" + id, x + 9, y + 3);
    }

    void drawChargingStation(double x, double y, int id) {
        double size = 11;

        gc.setFill(Color.web("#2ECC71", 0.15));
        gc.fillOval(x - 14, y - 14, 28, 28);

        gc.save();
        gc.translate(x, y);
        gc.rotate(45);
        gc.setFill(Color.web("#2ECC71"));
        gc.fillRect(-size / 2, -size / 2, size, size);
        gc.setStroke(Color.web("#27AE60"));
        gc.setLineWidth(2);
        gc.strokeRect(-size / 2, -size / 2, size, size);
        gc.restore();

        gc.setFill(Color.web("#F1C40F"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        gc.fillText("⚡", x - 7, y - 11);

        gc.setFill(Color.web("#2ECC71"));
        gc.setFont(Font.font("Arial", 8));
        gc.fillText("CS" + (id - numberOfCustomers), x + 10, y + 4);
    }

    void drawArrow(double x1, double y1, double x2, double y2, boolean bright) {
        double mx = (x1 + x2) / 2;
        double my = (y1 + y2) / 2;
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double len = 6;

        gc.setFill(bright ? Color.web("#00E5FF", 0.7) : Color.web("#00E5FF", 0.2));

        double ax1 = mx - len * Math.cos(angle - Math.PI / 6);
        double ay1 = my - len * Math.sin(angle - Math.PI / 6);
        double ax2 = mx - len * Math.cos(angle + Math.PI / 6);
        double ay2 = my - len * Math.sin(angle + Math.PI / 6);

        gc.fillPolygon(new double[]{mx, ax1, ax2}, new double[]{my, ay1, ay2}, 3);
    }

    void drawDrone(double x, double y) {
        double armLen = 14;

        gc.save();
        gc.translate(x, y);

        gc.setFill(Color.web("#FF5722", 0.15));
        gc.fillOval(-22, -22, 44, 44);

        gc.setStroke(Color.web("#BDBDBD"));
        gc.setLineWidth(2);
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(45 + i * 90);
            double ex = Math.cos(a) * armLen;
            double ey = Math.sin(a) * armLen;

            gc.strokeLine(0, 0, ex, ey);

            gc.save();
            gc.translate(ex, ey);
            gc.rotate(propellerAngle + i * 90);
            gc.setStroke(Color.web("#FF5722", 0.7));
            gc.setLineWidth(2.5);
            gc.strokeLine(-7, 0, 7, 0);
            gc.restore();

            gc.setFill(Color.web("#424242"));
            gc.fillOval(ex - 3, ey - 3, 6, 6);
        }

        gc.setFill(Color.web("#FF5722"));
        gc.fillOval(-5, -5, 10, 10);
        gc.setStroke(Color.web("#E64A19"));
        gc.setLineWidth(1.5);
        gc.strokeOval(-5, -5, 10, 10);

        gc.setFill(Color.WHITE);
        gc.fillOval(-2, -2, 4, 4);

        gc.restore();
    }

    // ===========================
    // HELPERS
    // ===========================

    boolean isChargingStation(int id) {
        return id > numberOfCustomers && id != 0;
    }

    String getNodeLabel(int id) {
        if (id == 0) return "V0(Depot)";
        if (id > numberOfCustomers) return "V" + id + "(CS)";
        return "V" + id;
    }

    Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web("#2980B9"));
        lbl.setPadding(new Insets(5, 0, 3, 0));
        return lbl;
    }

    Label infoLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Consolas", 11));
        lbl.setTextFill(Color.web("#B0BEC5"));
        return lbl;
    }

    void styleButton(Button btn, String color) {
        btn.setPrefWidth(270);
        btn.setPrefHeight(38);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
                + "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
    }

    HBox legendItem(String color, String symbol, String text) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Label sym = new Label(symbol);
        sym.setFont(Font.font("Arial", 14));
        sym.setTextFill(Color.web(color));

        Label lbl = new Label(text);
        lbl.setFont(Font.font("Arial", 11));
        lbl.setTextFill(Color.web("#8899AA"));

        box.getChildren().addAll(sym, lbl);
        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
}