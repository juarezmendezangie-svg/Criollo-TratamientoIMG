package com.criollo.omr.procesamiento;

import com.criollo.omr.config.ConfiguracionExamen;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Detecta la hoja del examen en una fotografía.
 * 
 * Pipeline:
 * 1. GaussianBlur → suavizar ruido
 * 2. Canny → detectar bordes
 * 3. Dilatar → ensanchar bordes
 * 4. findContours → encontrar contornos
 * 5. approxPolyDP → aproximar a polígono de 4 lados
 * 6. Ordenar esquinas: TL, TR, BR, BL
 * 
 * FÓRMULA: ε = 0.02 × arcLength (Douglas-Peucker)
 */
public class DetectorHoja {

    private static final Logger log = LoggerFactory.getLogger(DetectorHoja.class);
    private final ConfiguracionExamen config;

    public DetectorHoja() {
        this.config = ConfiguracionExamen.getInstancia();
    }

    /**
     * Detecta las 4 esquinas de la hoja del examen en la imagen.
     * @param imagenOriginal imagen BGR completa
     * @return array de 4 Point [TL, TR, BR, BL] o null si no detecta hoja
     */
    public Point[] detectarHoja(Mat imagenOriginal) {
        if (imagenOriginal == null || imagenOriginal.empty()) {
            log.warn("Imagen nula o vacía, no se puede detectar hoja");
            return null;
        }

        Mat gris = null, blur = null, canny = null, dilatada = null;
        try {
            // 1. Escala de grises
            gris = new Mat();
            Imgproc.cvtColor(imagenOriginal, gris, Imgproc.COLOR_BGR2GRAY);

            // 2. GaussianBlur para reducir ruido
            blur = new Mat();
            Imgproc.GaussianBlur(gris, blur, new Size(5, 5), 0);

            // 3. Detección de bordes Canny
            canny = new Mat();
            Imgproc.Canny(blur, canny, config.getCannyThreshold1(), config.getCannyThreshold2());

            // 4. Dilatar para ensanchar bordes (cierra gaps)
            dilatada = new Mat();
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
            Imgproc.dilate(canny, dilatada, kernel);
            kernel.release();

            // 5. Encontrar contornos
            List<MatOfPoint> contornos = new ArrayList<>();
            Mat jerarquia = new Mat();
            Imgproc.findContours(dilatada, contornos, jerarquia, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            jerarquia.release();

            // 6. Ordenar por área descendente (procesar los más grandes primero)
            Collections.sort(contornos, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint c1, MatOfPoint c2) {
                    return Double.compare(Imgproc.contourArea(c2), Imgproc.contourArea(c1));
                }
            });

            // 7. Buscar el contorno de 4 lados más grande
            double epsilon = config.getAproxEpsilon();
            for (int i = 0; i < Math.min(contornos.size(), 10); i++) {
                MatOfPoint contour = contornos.get(i);
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                double perimetro = Imgproc.arcLength(contour2f, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour2f, approx, epsilon * perimetro, true);

                Point[] puntos = approx.toArray();
                if (puntos.length == 4) {
                    log.info("Hoja detectada: {} esquinas en contorno #{}", puntos.length, i);
                    Point[] ordenados = ordenarEsquinas(puntos);
                    int w = (int)Math.max(
                        Math.sqrt(Math.pow(ordenados[1].x-ordenados[0].x,2)+Math.pow(ordenados[1].y-ordenados[0].y,2)),
                        Math.sqrt(Math.pow(ordenados[2].x-ordenados[3].x,2)+Math.pow(ordenados[2].y-ordenados[3].y,2)));
                    int h = (int)Math.max(
                        Math.sqrt(Math.pow(ordenados[2].x-ordenados[1].x,2)+Math.pow(ordenados[2].y-ordenados[1].y,2)),
                        Math.sqrt(Math.pow(ordenados[3].x-ordenados[0].x,2)+Math.pow(ordenados[3].y-ordenados[0].y,2)));
                    log.info("  Esquinas TL/TR/BL/BR, tamaño estimado={}x{}px", w, h);
                    contour2f.release();
                    approx.release();
                    return ordenados;
                }
                contour2f.release();
                approx.release();
            }

            log.warn("No se detectó hoja con 4 esquinas en la imagen");
            return null;

        } catch (Exception e) {
            log.error("Error detectando hoja: {}", e.getMessage());
            return null;
        } finally {
            if (gris != null) gris.release();
            if (blur != null) blur.release();
            if (canny != null) canny.release();
            if (dilatada != null) dilatada.release();
        }
    }

    /**
     * Ordena 4 puntos esquina en: Top-Left, Top-Right, Bottom-Right, Bottom-Left.
     * Basado en la suma y diferencia de coordenadas.
     */
    private Point[] ordenarEsquinas(Point[] puntos) {
        List<Point> pts = new ArrayList<>();
        for (Point p : puntos) pts.add(p);

        // TL tiene menor suma (x+y), BR tiene mayor suma
        // TR tiene menor diferencia (y-x), BL tiene mayor diferencia (y-x)
        Point tl = Collections.min(pts, Comparator.comparingDouble(p -> p.x + p.y));
        Point br = Collections.max(pts, Comparator.comparingDouble(p -> p.x + p.y));
        Point tr = Collections.min(pts, Comparator.comparingDouble(p -> p.y - p.x));
        Point bl = Collections.max(pts, Comparator.comparingDouble(p -> p.y - p.x));

        return new Point[]{tl, tr, br, bl};
    }
}
