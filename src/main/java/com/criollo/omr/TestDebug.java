package com.criollo.omr;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

/**
 * Test de diagnóstico: muestra las 10 primeras preguntas detectadas
 * y los valores de píxeles en CADA opción A-E.
 * Así sabemos si el grid está bien posicionado.
 */
public class TestDebug {

    public static void main(String[] args) {
        OpenCV.loadLocally();

        // Elegir qué imagen probar (cambiar según necesites)
        String path = "img/ALUMNO-100PR.png";
        // String path = "img/RESPUESTAS-100PR.png";
        // String path = "img/RESPUESTAS-50PR.png";
        // String path = "img/ALUMNO-50PR.png";

        System.out.println("=== DIAGNÓSTICO: " + path + " ===");

        Mat img = Imgcodecs.imread(path);
        if (img.empty()) { System.err.println("ERROR: no se pudo cargar " + path); return; }

        System.out.println("Dimensiones: " + img.cols() + "x" + img.rows());

        // Grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);

        // Binarizar (EqualizeHist + Otsu como en el pipeline)
        Mat eq = new Mat();
        Imgproc.equalizeHist(gray, eq);
        Mat binary = new Mat();
        Imgproc.threshold(eq, binary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        int w = binary.cols(), h = binary.rows();
        int nCols = 4; // o 2 para 50 preguntas
        int nRows = 25;
        int nOpts = 5;
        char[] letras = {'A','B','C','D','E'};

        // Column centers: 1/8, 3/8, 5/8, 7/8 del ancho
        double[] colX = new double[nCols];
        for (int c = 0; c < nCols; c++)
            colX[c] = w * (2.0*c + 1) / (2.0 * nCols);

        double yTop = h * 0.10;
        double yBot = h * 0.97;
        double rowH = (yBot - yTop) / nRows;
        double optSpan = 48; // 48px de span para 5 opciones
        int sr = 7;

        System.out.println("\nColumn centers: C0=" + (int)colX[0] + " C1=" + (int)colX[1] +
            " C2=" + (int)colX[2] + " C3=" + (int)colX[3]);
        System.out.println("Row height: " + String.format("%.1f", rowH) + "px");
        System.out.println("Option span: " + (int)optSpan + "px, sample radius: " + sr + "px\n");

        // Mostrar las 10 primeras preguntas (P01-P10)
        for (int ri = 0; ri < 10; ri++) {
            int qNum = ri + 1;
            double ry = yTop + ri * rowH + rowH/2;
            int ci = 0; // Columna 0 = preguntas 1-25

            System.out.printf("P%02d (Y=%d): ", qNum, (int)ry);

            int bestO = -1;
            int bestW = Integer.MAX_VALUE; // MENOR = más negro = relleno
            int[] whites = new int[nOpts];

            for (int oi = 0; oi < nOpts; oi++) {
                int cx = (int)(colX[ci] + (oi - 2) * optSpan/(nOpts-1));
                int cy = (int)ry;
                int x1 = Math.max(0, cx-sr), y1 = Math.max(0, cy-sr);
                int x2 = Math.min(w, cx+sr), y2 = Math.min(h, cy+sr);
                if (x1 >= x2 || y1 >= y2) continue;
                int white = Core.countNonZero(new Mat(binary, new Rect(x1,y1,x2-x1,y2-y1)));
                whites[oi] = white;
                if (white < bestW) { bestW = white; bestO = oi; }
            }

            // Mostrar valores por opción
            for (int oi = 0; oi < nOpts; oi++) {
                String marker = (whites[oi] == bestW) ? "*" : " ";
                System.out.printf("%c=%d%s  ", letras[oi], whites[oi], marker);
            }
            System.out.printf(" → Detectada: %c (white=%d)%n",
                bestO >= 0 ? letras[bestO] : '?', bestW);
        }

        // También mostrar posiciones X exactas de muestreo para P01
        System.out.println("\n--- Posiciones X de muestreo para P01 ---");
        double ry = yTop + rowH/2;
        for (int oi = 0; oi < nOpts; oi++) {
            int cx = (int)(colX[0] + (oi - 2) * optSpan/(nOpts-1));
            System.out.printf("Opción %c: X=%d, Y=%d%n", letras[oi], cx, (int)ry);
        }

        // Mostrar valores de gris en P01 (sin binarizar)
        System.out.println("\n--- Intensidad GRIS en P01 (sin binarizar) ---");
        for (int oi = 0; oi < nOpts; oi++) {
            int cx = (int)(colX[0] + (oi - 2) * optSpan/(nOpts-1));
            int cy = (int)ry;
            int x1 = Math.max(0, cx-sr), y1 = Math.max(0, cy-sr);
            int x2 = Math.min(w, cx+sr), y2 = Math.min(h, cy+sr);
            if (x1 >= x2 || y1 >= y2) continue;
            Scalar m = Core.mean(new Mat(gray, new Rect(x1,y1,x2-x1,y2-y1)));
            System.out.printf("Opción %c: gray_mean=%.0f%n", letras[oi], m.val[0]);
        }

        // Limpiar
        gray.release(); eq.release(); binary.release(); img.release();
    }
}
