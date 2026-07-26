package com.criollo.omr;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

/**
 * Guarda imágenes de cada etapa del procesamiento con el grid dibujado.
 * Así puedes ver visualmente dónde está muestreando el sistema.
 */
public class GuardarEtapas {

    public static void main(String[] args) {
        OpenCV.loadLocally();
        
        String path = "img/ALUMNO-50PR.png";
        Mat img = Imgcodecs.imread(path);
        if (img.empty()) { System.err.println("ERROR: " + path); return; }
        
        int w = img.cols(), h = img.rows();
        int nCols = 2, nRows = 25, nOpts = 5;
        
        // 1. Imagen original con grid
        Mat origGrid = img.clone();
        dibujarGrid(origGrid, w, h, nCols, nRows, nOpts, new Scalar(0, 255, 0));
        Imgcodecs.imwrite("debug/01_original_grid.png", origGrid);
        System.out.println("Guardado: debug/01_original_grid.png");
        
        // 2. Escala de grises
        Mat gray = new Mat();
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
        Imgcodecs.imwrite("debug/02_grayscale.png", gray);
        System.out.println("Guardado: debug/02_grayscale.png");
        
        // 3. GaussianBlur (antes EqualizeHist)
        Mat eq = new Mat();
        Imgproc.GaussianBlur(gray, eq, new Size(5, 5), 0);
        Imgcodecs.imwrite("debug/03_equalized.png", eq);
        System.out.println("Guardado: debug/03_equalized.png (ahora con GaussianBlur sin ruido)");
        
        // 4. Otsu binary
        Mat binary = new Mat();
        Imgproc.threshold(eq, binary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        Imgcodecs.imwrite("debug/04_otsu_binary.png", binary);
        System.out.println("Guardado: debug/04_otsu_binary.png");
        
        // 5. Binary con grid y rectángulos de muestreo
        Mat binGrid = binary.clone();
        Imgproc.cvtColor(binGrid, binGrid, Imgproc.COLOR_GRAY2BGR);
        dibujarGrid(binGrid, w, h, nCols, nRows, nOpts, new Scalar(0, 0, 255));
        
        // Dibujar rectángulos de muestreo para P01-P10
        double yTop = h * 0.2605;
        double yBot = h * 0.9393;
        double rowH = (yBot - yTop) / nRows;
        double[] colX = new double[]{w * 0.2832, w * 0.7554}; // Centros exactos (Opción C en 290 y 773.5)
        double optSpan = w * 0.2539; // Ancho entre opción A y E (~260px, 65px por opción)
        int sr = 7;
        
        for (int ri = 0; ri < 10; ri++) {
            double ry = yTop + ri * rowH + rowH/2;
            for (int oi = 0; oi < nOpts; oi++) {
                int cx = (int)(colX[0] + (oi - 2) * optSpan/(nOpts-1));
                int cy = (int)ry;
                Imgproc.rectangle(binGrid, 
                    new Point(cx-sr, cy-sr), new Point(cx+sr, cy+sr),
                    new Scalar(0, 255, 255), 1);
            }
        }
        Imgcodecs.imwrite("debug/05_binary_sampling_rects.png", binGrid);
        System.out.println("Guardado: debug/05_binary_sampling_rects.png");
        
        // 6. ZOOM: P01-P05 en la imagen original con rectángulos
        Mat zoom = img.clone();
        for (int ri = 0; ri < 5; ri++) {
            double ry = yTop + ri * rowH + rowH/2;
            for (int oi = 0; oi < nOpts; oi++) {
                int cx = (int)(colX[0] + (oi - 2) * optSpan/(nOpts-1));
                int cy = (int)ry;
                Imgproc.rectangle(zoom, 
                    new Point(cx-sr, cy-sr), new Point(cx+sr, cy+sr),
                    new Scalar(0, 255, 0), 2);
                // Etiqueta A,B,C,D,E
                Imgproc.putText(zoom, "" + (char)('A'+oi),
                    new Point(cx-4, cy-10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.4, new Scalar(255, 0, 0), 1);
            }
        }
        // Recortar zona de P01-P05 ampliando el margen para no cortar A y E
        int cropY1 = (int)(yTop + 0*rowH) - 20;
        int cropY2 = (int)(yTop + 5*rowH) + 20;
        int cropX1 = Math.max(0, (int)colX[0] - 155);
        int cropX2 = Math.min(w, (int)colX[0] + 155);
        Mat zoomCrop = new Mat(zoom, new Rect(cropX1, cropY1, cropX2-cropX1, cropY2-cropY1));
        Imgcodecs.imwrite("debug/06_zoom_p01_p05.png", zoomCrop);
        System.out.println("Guardado: debug/06_zoom_p01_p05.png");
        
        // 7. Burbujas detectadas por contornos
        var detector = new com.criollo.omr.procesamiento.DetectorBurbujas();
        var burbujas = detector.detectarBurbujas(binary);
        Mat contours = img.clone();
        for (var b : burbujas) {
            Imgproc.circle(contours, b.centro(), b.radio(), new Scalar(0, 255, 0), 2);
        }
        Imgcodecs.imwrite("debug/07_detected_bubbles.png", contours);
        System.out.println("Guardado: debug/07_detected_bubbles.png (" + burbujas.size() + " burbujas)");
        
        long numQ1A = burbujas.stream().filter(b -> Math.abs(b.centro().x - 160.5) < 10 && Math.abs(b.centro().y - 421.0) < 10).count();
        System.out.println("Burbujas detectadas en Q1 Opción A (X~160.5, Y~421.0): " + numQ1A);
        
        // Cleanup
        img.release(); gray.release(); eq.release(); binary.release();
        origGrid.release(); binGrid.release(); zoom.release(); contours.release();
        zoomCrop.release();
        
        System.out.println("\nRevisa la carpeta debug/ para ver las imágenes.");
        System.out.println("Fíjate en 06_zoom_p01_p05.png: ¿los rectángulos amarillos");
        System.out.println("caen SOBRE las burbujas o están desplazados?");
    }
    
    private static void dibujarGrid(Mat img, int w, int h, int nCols, int nRows, int nOpts, Scalar color) {
        double yTop = h * 0.2605;
        double yBot = h * 0.9393;
        double rowH = (yBot - yTop) / nRows;
        double[] colX = new double[]{w * 0.2832, w * 0.7554};
        double optSpan = w * 0.2539;
        
        // Líneas horizontales (filas)
        for (int ri = 0; ri <= nRows; ri++) {
            int y = (int)(yTop + ri * rowH);
            Imgproc.line(img, new Point(0, y), new Point(w, y), color, 1);
        }
        
        // Líneas verticales (límites de columnas A-E)
        for (int ci = 0; ci < nCols; ci++) {
            int xLeft = (int)(colX[ci] - optSpan/2.0 - 12);
            Imgproc.line(img, new Point(xLeft, 0), new Point(xLeft, h), color, 1);
            int xRight = (int)(colX[ci] + optSpan/2.0 + 12);
            Imgproc.line(img, new Point(xRight, 0), new Point(xRight, h), color, 1);
        }
    }
}
