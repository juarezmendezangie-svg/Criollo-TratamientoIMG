package com.criollo.omr;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

/**
 * Diagnóstico: mide el sesgo de iluminación por columna A-E.
 * Sin cambiar el pipeline, solo mide para decidir qué arreglar.
 */
public class DiagnosticoSesgo {

    public static void main(String[] args) {
        OpenCV.loadLocally();
        
        String path = "img/RESPUESTAS-100PR.png";
        Mat img = Imgcodecs.imread(path);
        if (img.empty()) { System.err.println("ERROR: " + path); return; }

        // Pipeline actual: Gray → Equalize
        Mat gris = new Mat();
        Imgproc.cvtColor(img, gris, Imgproc.COLOR_BGR2GRAY);
        img.release();

        Mat ecualizada = new Mat();
        Imgproc.equalizeHist(gris, ecualizada);

        // Medir valores ANTES de ecualización
        System.out.println("=== GRISES ORIGINALES (sin ecualizar) ===");
        medirColumnas(gris);
        
        System.out.println("\n=== DESPUÉS DE EQUALIZEHIST ===");
        medirColumnas(ecualizada);
        
        // Medir CLAHE
        Mat claheMat = new Mat();
        org.opencv.imgproc.CLAHE clahe = Imgproc.createCLAHE();
        clahe.setClipLimit(2.5);
        clahe.setTilesGridSize(new Size(8, 8));
        clahe.apply(gris, claheMat);
        System.out.println("\n=== DESPUÉS DE CLAHE ===");
        medirColumnas(claheMat);
        
        // Medir Otsu
        Mat otsu = new Mat();
        Imgproc.threshold(ecualizada, otsu, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        System.out.println("\n=== DESPUÉS DE OTSU (countNonZero por ROI 7×7) ===");
        medirColumnasBinaria(otsu);
        
        gris.release();
        ecualizada.release();
        claheMat.release();
        otsu.release();
    }
    
    private static void medirColumnas(Mat imagen) {
        int h = imagen.rows();
        int w = imagen.cols();
        double colW = w / 4.0;
        double rowH = h / 25.0;
        int sr = 7;
        
        // Para cada columna A-E, acumular intensidades de TODAS las filas
        double[][] sums = new double[5][5]; // [col][opcion]
        int[][] counts = new int[5][5];
        
        for (int ci = 0; ci < 4; ci++) {
            double colCenter = (ci + 0.5) * colW;
            double optSpacing = colW / 8.0; // 5 opciones en el ancho de columna
            
            for (int ri = 0; ri < 25; ri++) {
                double rowY = ri * rowH + rowH / 2;
                for (int oi = 0; oi < 5; oi++) {
                    double optX = colCenter + (oi - 2) * optSpacing;
                    int cx = (int)optX, cy = (int)rowY;
                    int x1 = Math.max(0, cx - sr), y1 = Math.max(0, cy - sr);
                    int x2 = Math.min(w, cx + sr), y2 = Math.min(h, cy + sr);
                    if (x1 < x2 && y1 < y2) {
                        Mat roi = new Mat(imagen, new Rect(x1, y1, x2-x1, y2-y1));
                        Scalar m = Core.mean(roi);
                        sums[ci][oi] += m.val[0];
                        counts[ci][oi]++;
                    }
                }
            }
        }
        
        System.out.printf("%-12s %-8s %-8s %-8s %-8s %-8s%n", "Columna", "A", "B", "C", "D", "E");
        for (int ci = 0; ci < 4; ci++) {
            System.out.printf("Col %d (P%02d-%02d): ", ci+1, ci*25+1, ci*25+25);
            for (int oi = 0; oi < 5; oi++) {
                if (counts[ci][oi] > 0)
                    System.out.printf("%-8.0f", sums[ci][oi] / counts[ci][oi]);
                else
                    System.out.printf("%-8s", "-");
            }
            System.out.println();
        }
    }
    
    private static void medirColumnasBinaria(Mat imagen) {
        int h = imagen.rows();
        int w = imagen.cols();
        double colW = w / 4.0;
        double rowH = h / 25.0;
        int sr = 7;
        
        System.out.printf("%-12s %-8s %-8s %-8s %-8s %-8s%n", "Columna", "A", "B", "C", "D", "E");
        for (int ci = 0; ci < 4; ci++) {
            double colCenter = (ci + 0.5) * colW;
            double optSpacing = colW / 8.0;
            
            System.out.printf("Col %d (P%02d-%02d): ", ci+1, ci*25+1, ci*25+25);
            for (int oi = 0; oi < 5; oi++) {
                int sum = 0, cnt = 0;
                for (int ri = 0; ri < 25; ri++) {
                    double rowY = ri * rowH + rowH / 2;
                    double optX = colCenter + (oi - 2) * optSpacing;
                    int cx = (int)optX, cy = (int)rowY;
                    int x1 = Math.max(0, cx - sr), y1 = Math.max(0, cy - sr);
                    int x2 = Math.min(w, cx + sr), y2 = Math.min(h, cy + sr);
                    if (x1 < x2 && y1 < y2) {
                        Mat roi = new Mat(imagen, new Rect(x1, y1, x2-x1, y2-y1));
                        sum += Core.countNonZero(roi);
                        cnt++;
                    }
                }
                if (cnt > 0)
                    System.out.printf("%-8d", sum / cnt);
                else
                    System.out.printf("%-8s", "-");
            }
            System.out.println();
        }
    }
}
