# 🐋 Whale Optimization Algorithm for Drone Routing Problem

Implementasi Whale Optimization Algorithm (WOA) untuk menyelesaikan Drone Routing Problem (DRP) dengan kendala kapasitas baterai dan penyisipan charging station secara adaptif.

> **Skripsi** — Program Studi Informatika, Fakultas Teknik, Universitas Sulawesi Barat

---

## 📋 Deskripsi

Drone Routing Problem (DRP) adalah permasalahan optimasi untuk menentukan rute terbang yang paling efisien bagi drone dalam melayani sejumlah titik customer, dengan mempertimbangkan keterbatasan kapasitas baterai. DRP termasuk dalam kategori permasalahan **NP-Hard**.

Penelitian ini mengimplementasikan **Whale Optimization Algorithm (WOA)** — algoritma metaheuristik berbasis populasi yang terinspirasi dari strategi perburuan paus bungkuk melalui teknik _bubble-net feeding_ — untuk menyelesaikan DRP dengan tujuan **meminimalkan total konsumsi energi** drone.

### Fitur Utama

- Pembangkitan dataset sintetis (depot, customer, charging station)
- Implementasi WOA dengan adaptasi domain diskrit (operasi swap pada permutasi)
- Penyisipan charging station secara adaptif berdasarkan kondisi baterai
- Pengecekan proaktif baterai sebelum terbang ke node berikutnya
- Mekanisme hop antar charging station untuk jarak yang melebihi range drone
- GUI visualisasi dengan animasi drone menggunakan JavaFX

---

## 🏗️ Arsitektur Program

```
┌─────────────────────┐     ┌──────────────┐     ┌───────────────────────┐
│  CreateDataset.java  │────▶│  CSV Files   │────▶│  DroneRoutingGUI.java │
│  (Generate Data)     │     │              │     │  (Visualization)      │
└─────────────────────┘     │  adjacency_  │     └───────────────────────┘
                            │  matrix.csv  │                ▲
                            │              │                │
                            │  coordinates │     ┌──────────┴──────────┐
                            │  .csv        │────▶│     WOA.java        │
                            │              │     │  (Optimization)     │
                            │  solution    │◀────│                     │
                            │  .csv        │     └─────────────────────┘
                            └──────────────┘
```

| File                   | Fungsi                                                       |
| ---------------------- | ------------------------------------------------------------ |
| `CreateDataset.java`   | Membangkitkan koordinat acak dan menghitung adjacency matrix |
| `WOA.java`             | Algoritma Whale Optimization untuk mencari rute optimal      |
| `DroneRoutingGUI.java` | Visualisasi peta dan animasi drone menggunakan JavaFX        |

---

## ⚙️ Parameter

### Parameter DRP

| Parameter          | Nilai    | Keterangan                     |
| ------------------ | -------- | ------------------------------ |
| `BATTERY_CAPACITY` | 4000 Wh   | Kapasitas baterai drone        |
| `ALPHA`            | 20 Wh/km | Konsumsi energi per kilometer  |
| Range              | 33.33 km | Jarak maksimum per full charge |

### Parameter WOA

| Parameter            | Nilai | Keterangan                     |
| -------------------- | ----- | ------------------------------ |
| `populationSize` (N) | 30    | Jumlah individu dalam populasi |
| `maxIterations` (T)  | 1000  | Jumlah iterasi maksimum        |
| `b`                  | 1.0   | Konstanta spiral (bubble-net)  |

---

## 🔄 Alur Algoritma WOA

```
1. Inisialisasi populasi (N solusi acak berupa permutasi customer)
2. Evaluasi fitness setiap solusi → cari X* (solusi terbaik)
3. Loop iterasi (t = 1 sampai T):
   │
   ├── Untuk setiap individu:
   │   ├── Hitung parameter: a = 2 - 2t/T, A = 2a·r1 - a, p = random[0,1]
   │   │
   │   ├── if p < 0.5 dan |A| < 1:
   │   │   └── Encircling Prey (swap terarah mendekati X*)
   │   │
   │   ├── if p < 0.5 dan |A| ≥ 1:
   │   │   └── Search for Prey (swap terarah mendekati X_rand)
   │   │
   │   └── if p ≥ 0.5:
   │       └── Bubble-Net Attacking (swap acak berdasarkan koefisien spiral)
   │
   ├── Evaluasi fitness solusi baru (termasuk penyisipan CS)
   ├── Terima solusi baru jika lebih baik
   └── Update X* jika ada solusi lebih baik

4. Return X* sebagai solusi akhir
```

### Mekanisme Penyisipan Charging Station

```
Sebelum terbang ke customer berikutnya:
1. Cek bisaLanjutAman? (baterai cukup + cadangan ke CS terdekat)
2. Tidak aman → findReachableCS (cari CS terbaik menuju tujuan)
3. Hop ke CS → isi ulang baterai → cek lagi
4. Aman → terbang ke customer
```

---

## 🚀 Cara Menjalankan

### Prasyarat

- **Java 21** atau lebih baru
- **JavaFX SDK 21** ([Download di sini](https://gluonhq.com/products/javafx/))

### Langkah 1: Generate Dataset

```bash
javac CreateDataset.java
java CreateDataset
```

Output: `adjacency_matrix.csv` dan `coordinates.csv`

### Langkah 2: Jalankan Optimasi WOA

```bash
javac WOA.java
java WOA
```

Output: `solution.csv` dan hasil optimasi di terminal

### Langkah 3: Jalankan GUI Visualisasi

```bash
javac --module-path "C:/javafx-sdk-21/lib" --add-modules javafx.controls WOA.java DroneRoutingGUI.java
java --module-path "C:/javafx-sdk-21/lib" --add-modules javafx.controls DroneRoutingGUI
```

> Ganti `C:/javafx-sdk-21/lib` dengan path JavaFX SDK di komputer Anda.

### VS Code

Tambahkan di `launch.json`:

```json
{
  "type": "java",
  "name": "DroneRoutingGUI",
  "request": "launch",
  "mainClass": "DroneRoutingGUI",
  "vmArgs": "--module-path \"C:/javafx-sdk-21/lib\" --add-modules javafx.controls"
}
```

---

## 🖥️ Screenshot GUI

### Tampilan Peta

- **Kotak Orange** — Depot (titik awal dan akhir)
- **Lingkaran Biru** — Customer (titik yang dikunjungi)
- **Diamond Hijau** — Charging Station (titik pengisian daya)
- **Garis Tipis** — Graf koneksi yang bisa dijangkau drone
- **Garis Cyan** — Rute drone hasil optimasi
- **Ikon Merah** — Drone (dengan animasi propeller berputar)

### Fitur GUI

- Animasi drone mengikuti rute dengan garis tergambar bertahap
- Battery bar berubah warna (hijau → kuning → merah) sesuai sisa baterai
- Tombol Play/Pause dan Reset
- Slider kecepatan animasi
- Log aktivitas drone (charging, tiba di node, dll)
- Panel informasi (fitness, total energi, total jarak)

---

## 📁 Struktur File

```
├── CreateDataset.java          # Pembangkitan dataset
├── WOA.java                    # Algoritma WOA
├── DroneRoutingGUI.java        # GUI visualisasi
├── adjacency_matrix.csv        # Matriks jarak (generated)
├── coordinates.csv             # Koordinat node (generated)
├── solution.csv                # Solusi terbaik (generated)
└── README.md
```

### Format CSV

**adjacency_matrix.csv:**

```
20,3                          ← jumlah customer, jumlah CS
,V0,V1,V2,...                 ← header
V0,0.00,3.16,5.83,...         ← data jarak
V1,3.16,0.00,2.83,...
...
```

**coordinates.csv:**

```
20,3                          ← jumlah customer, jumlah CS
id,x,y                        ← header
0,12,5                         ← depot
1,3,18                         ← customer
...
21,7,14                        ← charging station
```

**solution.csv:**

```
5,18,12,13,9,19,8,1,2,...     ← urutan kunjungan customer
```

---

## 📊 Skenario Pengujian

| Skenario | Customer | CS  | Area  | Tujuan             |
| -------- | -------- | --- | ----- | ------------------ |
| Kecil    | 20       | 3   | 20×20 | Validasi algoritma |
| Sedang   | 50       | 5   | 25×25 | Pengujian utama    |
| Besar    | 100      | 8   | 30×30 | Uji skalabilitas   |

Variasi parameter yang diuji:

- **Jumlah customer**: 20, 50, 100
- **Ukuran populasi (N)**: 10, 30, 50
- **Jumlah iterasi (T)**: 100, 500, 1000

---

## 📚 Referensi

- Mirjalili, S., & Lewis, A. (2016). The Whale Optimization Algorithm. _Advances in Engineering Software_, 95, 51–67.
- Yu, N. K., et al. (2021). Learning Whale Optimization Algorithm for Open Vehicle Routing Problem with Loading Constraints. _Discrete Dynamics in Nature and Society_.
- Zhang, S., & Gu, X. (2023). A discrete whale optimization algorithm for the no-wait flow shop scheduling problem. _Measurement and Control_.
- Cheng, C., et al. (2020). Drone routing with energy function: Formulation and exact algorithm. _Transportation Research Part B_.
- Dorling, K., et al. (2017). Vehicle Routing Problems for Drone Delivery. _IEEE Transactions on Systems, Man, and Cybernetics_.

---

## 👤 Penulis

**Achmad Ali Akbar** — D0222320  
Program Studi Informatika, Fakultas Teknik  
Universitas Sulawesi Barat

### Pembimbing

- Ir. Sugiarto Cokrowibowo, S.Si., M.T. (Pembimbing I)
- A. Amirul Asnan Cirua, S.T., M.Kom. (Pembimbing II)
