package com.criollo.omr.procesamiento;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FiltrosMatematicos {

    private static final Logger log = LoggerFactory.getLogger(FiltrosMatematicos.class);

    public Mat convertirAGrises(Mat imagenBGR) {
        if (imagenBGR == null || imagenBGR.empty())
            throw new IllegalArgumentException("Imagen nula o vacía");
        Mat resultado = new Mat();
        if (imagenBGR.channels() == 3)
            Imgproc.cvtColor(imagenBGR, resultado, Imgproc.COLOR_BGR2GRAY);
        else
            imagenBGR.copyTo(resultado);
        return resultado;
    }

    public Mat ecualizarHistograma(Mat imagenGris) {
        Mat resultado = new Mat();
        // Reemplazado equalizeHist por GaussianBlur leve para evitar ruido granulado y sesgo horizontal
        Imgproc.GaussianBlur(imagenGris, resultado, new Size(5, 5), 0);
        return resultado;
    }

    public Mat binarizar(Mat imagenGris) {
        Mat resultado = new Mat();
        Imgproc.threshold(imagenGris, resultado, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        return resultado;
    }

    public Mat erosion(Mat imagen, int k) {
        Mat resultado = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(k, k));
        Imgproc.erode(imagen, resultado, kernel);
        kernel.release();
        return resultado;
    }

    public Mat dilatacion(Mat imagen, int k) {
        Mat resultado = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(k, k));
        Imgproc.dilate(imagen, resultado, kernel);
        kernel.release();
        return resultado;
    }

    public Mat apertura(Mat imagen, int k) {
        Mat e = erosion(imagen, k);
        Mat r = dilatacion(e, k);
        e.release();
        return r;
    }

    public Mat cierre(Mat imagen, int k) {
        Mat d = dilatacion(imagen, k);
        Mat r = erosion(d, k);
        d.release();
        return r;
    }

    // PIPELINE DUAL:
    // [0] EqualizeHist+Otsu → contornos (100% grid detection)
    // [1] Adaptive BINARY_INV → inner mask fill (sin sesgo)
    // [2] Grises original
    public Mat[] procesarExamen(Mat imagenOriginal) {
        Mat gris = null, eq = null, otsu = null, adaptive = null;
        try {
            gris = convertirAGrises(imagenOriginal);
            
            // Otsu para contornos
            eq = ecualizarHistograma(gris);
            otsu = new Mat();
            Imgproc.threshold(eq, otsu, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            
            // Adaptive para relleno (BINARY_INV: filled=WHITE)
            adaptive = new Mat();
            Imgproc.adaptiveThreshold(gris, adaptive, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 15, 5);
            
            Mat r1 = new Mat(); otsu.copyTo(r1);
            Mat r2 = new Mat(); adaptive.copyTo(r2);
            Mat r3 = new Mat(); gris.copyTo(r3);
            return new Mat[]{r1, r2, r3};
        } finally {
            if (gris != null) gris.release();
            if (eq != null) eq.release();
            if (otsu != null) otsu.release();
            if (adaptive != null) adaptive.release();
        }
    }
}
