package com.criollo.omr.procesamiento;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Corrige la distorsión de perspectiva en fotos de exámenes.
 * 
 * FÓRMULA: M = getPerspectiveTransform(src, dst)
 *          Resultado = warpPerspective(imagen, M, tamañoDestino)
 * 
 * Usa transformación homográfica 4 puntos:
 *   TL(0,0) → TR(w,0) → BR(w,h) → BL(0,h)
 */
public class CorrectorPerspectiva {

    private static final Logger log = LoggerFactory.getLogger(CorrectorPerspectiva.class);

    /**
     * Aplica transformación de perspectiva para enderezar la hoja.
     * @param imagen imagen original BGR
     * @param esquinas 4 puntos: [TL, TR, BR, BL]
     * @return imagen enderezada (nueva Mat, el caller es responsable de liberarla)
     */
    public Mat corregirPerspectiva(Mat imagen, Point[] esquinas) {
        if (imagen == null || imagen.empty()) {
            throw new IllegalArgumentException("Imagen nula o vacía");
        }
        if (esquinas == null || esquinas.length != 4) {
            throw new IllegalArgumentException("Se requieren exactamente 4 esquinas");
        }

        Point tl = esquinas[0];
        Point tr = esquinas[1];
        Point br = esquinas[2];
        Point bl = esquinas[3];

        // Calcular dimensiones de la hoja enderezada
        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));
        int maxWidth = (int) Math.max(widthA, widthB);

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));
        int maxHeight = (int) Math.max(heightA, heightB);

        // Puntos origen (esquinas detectadas)
        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        srcMat.put(0, 0,
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        );

        // Puntos destino (rectángulo perfecto)
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);
        dstMat.put(0, 0,
            0.0, 0.0,
            maxWidth, 0.0,
            maxWidth, maxHeight,
            0.0, maxHeight
        );

        // Calcular matriz de transformación homográfica
        Mat M = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Mat resultado = new Mat();
        Imgproc.warpPerspective(imagen, resultado, M, new Size(maxWidth, maxHeight));

        // Liberar recursos
        srcMat.release();
        dstMat.release();
        M.release();

        log.info("Perspectiva corregida: {}x{}", maxWidth, maxHeight);
        return resultado;
    }
}
