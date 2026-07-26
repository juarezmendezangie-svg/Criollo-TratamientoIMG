package com.criollo.omr;

import nu.pattern.OpenCV;
import com.criollo.omr.procesamiento.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.*;

/**
 * Test de diagnóstico mejorado:
 * 1. Detecta burbujas con filtros estrictos
 * 2. Clusteriza posiciones X → centros de opción A-E
 * 3. Clusteriza posiciones Y → centros de fila
 * 4. Muestra valores muestreados en cada celda del grid
 */
public class TestDebug {

    public static void main(String[] args) {
        OpenCV.loadLocally();

        String path = "img/RESPUESTAS-100PR.png";
        System.out.println("=== DIAGNÓSTICO MEJORADO: " + path + " ===\n");

        Mat img = Imgcodecs.imread(path);
        if (img.empty()) { System.err.println("ERROR: no se pudo cargar " + path); return; }
        System.out.println("Dimensiones: " + img.cols() + "x" + img.rows());

        // Pipeline dual
        Mat gray = new Mat();
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
        Mat eq = new Mat();
        Imgproc.equalizeHist(gray, eq);
        Mat binary = new Mat();
        Imgproc.threshold(eq, binary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        int w = binary.cols(), h = binary.rows();

        // Detectar burbujas
        var detector = new DetectorBurbujas();
        var burbujas = detector.detectarBurbujas(binary);
        System.out.println("\nBurbujas detectadas: " + burbujas.size());

        if (burbujas.isEmpty()) {
            System.err.println("No se detectaron burbujas. Revisa config.");
            return;
        }

        // Zona de examen
        double yTop = h * 0.08;
        double yBot = h * 0.97;

        // Filtrar en zona
        List<DetectorBurbujas.Burbuja> enZona = new ArrayList<>();
        for (var b : burbujas) {
            if (b.centro().y > yTop && b.centro().y < yBot) enZona.add(b);
        }
        System.out.println("En zona de examen: " + enZona.size());

        // Separar en 4 columnas
        double colW = w / 4.0;
        List<DetectorBurbujas.Burbuja> col0 = new ArrayList<>(), col1 = new ArrayList<>(), col2 = new ArrayList<>(), col3 = new ArrayList<>();
        for (var b : enZona) {
            int ci = (int)(b.centro().x / colW);
            if (ci == 0) col0.add(b);
            else if (ci == 1) col1.add(b);
            else if (ci == 2) col2.add(b);
            else if (ci == 3) col3.add(b);
        }
        System.out.println("Columna 0 (P01-P25): " + col0.size() + " burbujas");
        System.out.println("Columna 1 (P26-P50): " + col1.size() + " burbujas");
        System.out.println("Columna 2 (P51-P75): " + col2.size() + " burbujas");
        System.out.println("Columna 3 (P76-P100): " + col3.size() + " burbujas");

        // Cluster X en columna 0 → 5 opciones
        System.out.println("\n--- CLUSTER X (Opciones A-E) ---");
        clusterInfo(col0, "Col0", true);

        // Cluster Y en columna 0 → 25 filas
        System.out.println("\n--- CLUSTER Y (Filas 1-25) ---");
        clusterInfo(col0, "Col0", false);

        // Muestrear P01-P10 con posiciones clusteradas
        System.out.println("\n--- MUESTREO P01-P10 (con clustering) ---");
        List<Double> xs0 = new ArrayList<>();
        for (var b : col0) xs0.add(b.centro().x);
        List<Double> ys0 = new ArrayList<>();
        for (var b : col0) ys0.add(b.centro().y);

        double[] optX = clusterCentros(xs0, 5, colW * 0.3);
        double[] rowY = clusterCentros(ys0, 25, (yBot - yTop) / 25 * 0.4);

        char[] letras = {'A','B','C','D','E'};
        int sr = 7;

        for (int ri = 0; ri < 10; ri++) {
            int qNum = ri + 1;
            int bestO = -1, bestW = Integer.MAX_VALUE;
            int[] whites = new int[5];

            System.out.printf("P%02d (Y=%d): ", qNum, (int) rowY[ri]);

            for (int oi = 0; oi < 5; oi++) {
                int cx = (int) optX[oi];
                int cy = (int) rowY[ri];
                int x1 = Math.max(0, cx - sr), y1 = Math.max(0, cy - sr);
                int x2 = Math.min(w, cx + sr), y2 = Math.min(h, cy + sr);
                if (x1 >= x2 || y1 >= y2) continue;

                int white = Core.countNonZero(new Mat(binary, new Rect(x1, y1, x2 - x1, y2 - y1)));
                whites[oi] = white;
                if (white < bestW) { bestW = white; bestO = oi; }
            }

            for (int oi = 0; oi < 5; oi++) {
                String marker = (whites[oi] == bestW) ? "*" : " ";
                System.out.printf("%c=%d%s  ", letras[oi], whites[oi], marker);
            }
            System.out.printf(" → %c (min=%d)%n", bestO >= 0 ? letras[bestO] : '?', bestW);
        }

        // También comparar con posiciones fijas (el approach anterior)
        System.out.println("\n--- COMPARACIÓN: Fijo vs Cluster ---");
        double fixedColX = w / 4.0; // Centro de columna 0
        double fixedOptSpan = 48;
        System.out.printf("Fijo: colCenter=%d, optSpan=%d%n", (int) fixedColX, (int) fixedOptSpan);
        System.out.printf("Cluster: optX=%s%n", Arrays.toString(optX));

        // Cleanup
        gray.release(); eq.release(); binary.release(); img.release();
    }

    private static void clusterInfo(List<DetectorBurbujas.Burbuja> burbujas, String label, boolean isX) {
        List<Double> vals = new ArrayList<>();
        for (var b : burbujas) vals.add(isX ? b.centro().x : b.centro().y);
        Collections.sort(vals);

        System.out.println(label + " " + (isX ? "X" : "Y") + " rango: [" +
            String.format("%.1f", vals.get(0)) + ", " +
            String.format("%.1f", vals.get(vals.size() - 1)) + "]");

        // Mostrar centroides
        int nClusters = isX ? 5 : 25;
        double maxDist = isX ? 100 : 50;
        double[] centroids = clusterCentros(vals, nClusters, maxDist);
        System.out.println("  Centroides (" + centroids.length + "): ");
        for (int i = 0; i < centroids.length; i++) {
            System.out.printf("    [%d] = %.1f%n", i, centroids[i]);
        }
    }

    private static double[] clusterCentros(List<Double> valores, int nClusters, double maxDist) {
        if (valores.size() <= nClusters) {
            double[] result = new double[valores.size()];
            for (int i = 0; i < result.length; i++) result[i] = valores.get(i);
            return result;
        }

        Collections.sort(valores);
        List<List<Double>> grupos = new ArrayList<>();
        List<Double> grupoActual = new ArrayList<>();
        grupoActual.add(valores.get(0));

        for (int i = 1; i < valores.size(); i++) {
            if (valores.get(i) - valores.get(i - 1) <= maxDist) {
                grupoActual.add(valores.get(i));
            } else {
                grupos.add(grupoActual);
                grupoActual = new ArrayList<>();
                grupoActual.add(valores.get(i));
            }
        }
        grupos.add(grupoActual);

        while (grupos.size() > nClusters) {
            int bestI = 0;
            double bestGap = Double.MAX_VALUE;
            for (int i = 0; i < grupos.size() - 1; i++) {
                double gap = mean(grupos.get(i + 1)) - mean(grupos.get(i));
                if (gap < bestGap) { bestGap = gap; bestI = i; }
            }
            grupos.get(bestI).addAll(grupos.get(bestI + 1));
            grupos.remove(bestI + 1);
        }

        while (grupos.size() < nClusters) {
            int biggestI = 0;
            for (int i = 1; i < grupos.size(); i++) {
                if (grupos.get(i).size() > grupos.get(biggestI).size()) biggestI = i;
            }
            List<Double> biggest = grupos.get(biggestI);
            int mid = biggest.size() / 2;
            grupos.set(biggestI, new ArrayList<>(biggest.subList(0, mid)));
            grupos.add(biggestI + 1, new ArrayList<>(biggest.subList(mid, biggest.size())));
        }

        double[] centroides = new double[nClusters];
        for (int i = 0; i < nClusters; i++) centroides[i] = mean(grupos.get(i));
        return centroides;
    }

    private static double mean(List<Double> vals) {
        double sum = 0;
        for (double v : vals) sum += v;
        return sum / vals.size();
    }
}
