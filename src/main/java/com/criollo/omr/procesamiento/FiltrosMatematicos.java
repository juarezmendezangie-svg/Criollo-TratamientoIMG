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
        Imgproc.equalizeHist(imagenGris, resultado);
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

    // PIPELINE: Gris → EqualizeHist → Otsu
    public Mat[] procesarExamen(Mat imagenOriginal) {
        Mat gris = null, eq = null, otsu = null;
        try {
            gris = convertirAGrises(imagenOriginal);
            eq = ecualizarHistograma(gris);
            otsu = binarizar(eq);
            Mat r1 = new Mat(); otsu.copyTo(r1);
            Mat r2 = new Mat(); gris.copyTo(r2);
            return new Mat[]{r1, r2};
        } finally {
            if (gris != null) gris.release();
            if (eq != null) eq.release();
            if (otsu != null) otsu.release();
        }
    }
}
