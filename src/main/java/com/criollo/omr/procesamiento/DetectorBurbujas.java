package com.criollo.omr.procesamiento;

import com.criollo.omr.config.ConfiguracionExamen;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DetectorBurbujas {

    private static final Logger log = LoggerFactory.getLogger(DetectorBurbujas.class);
    private final ConfiguracionExamen config;

    public DetectorBurbujas() {
        this.config = ConfiguracionExamen.getInstancia();
    }

    public record Burbuja(Point centro, int radio, double porcentajeRelleno,
                          Rect boundingBox, int indiceContour) {
        public String toString() {
            return String.format("Burbuja[(%.0f,%.0f) r=%d relleno=%.1f%%]",
                centro.x, centro.y, radio, porcentajeRelleno * 100);
        }
    }

    public List<Burbuja> detectarBurbujas(Mat imagenBinaria) {
        if (imagenBinaria == null || imagenBinaria.empty()) return new ArrayList<>();

        List<MatOfPoint> contornos = new ArrayList<>();
        Mat jerarquia = new Mat();
        Imgproc.findContours(imagenBinaria, contornos, jerarquia,
            Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        jerarquia.release();

        int areaMin = config.getAreaMinima();
        int areaMax = config.getAreaMaxima();
        double circMin = config.getCircularidadMinima();

        log.info("=== DETECTOR BURBUJAS ===");
        log.info("Contornos: {}, area=[{},{}], circ>={}", contornos.size(), areaMin, areaMax, circMin);

        List<Burbuja> todas = new ArrayList<>();
        for (int i = 0; i < contornos.size(); i++) {
            MatOfPoint c = contornos.get(i);
            double area = Math.abs(Imgproc.contourArea(c));
            if (area < areaMin || area > areaMax) continue;
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double perim = Imgproc.arcLength(c2f, true);
            c2f.release();
            if (perim <= 0) continue;
            double circ = (4.0 * Math.PI * area) / (perim * perim);
            if (circ < circMin) continue;

            Rect bbox = Imgproc.boundingRect(c);
            Point centro = new Point(bbox.x + bbox.width/2.0, bbox.y + bbox.height/2.0);
            int radio = Math.max(bbox.width, bbox.height) / 2;
            double relleno = calcularRellenoNegro(imagenBinaria, bbox);
            todas.add(new Burbuja(centro, radio, relleno, bbox, i));
        }

        log.info("Burbujas válidas: {}", todas.size());

        List<Burbuja> burbujas = fusionarCercanas(todas);
        log.info("Post-fusión: {}", burbujas.size());
        return burbujas;
    }

    private List<Burbuja> fusionarCercanas(List<Burbuja> burbujas) {
        if (burbujas.size() <= 1) return new ArrayList<>(burbujas);
        List<Burbuja> resultado = new ArrayList<>();
        boolean[] fusionado = new boolean[burbujas.size()];
        for (int i = 0; i < burbujas.size(); i++) {
            if (fusionado[i]) continue;
            Burbuja actual = burbujas.get(i);
            Burbuja mejor = actual;
            double mejorArea = actual.boundingBox().width * actual.boundingBox().height;
            for (int j = i + 1; j < burbujas.size(); j++) {
                if (fusionado[j]) continue;
                Burbuja otra = burbujas.get(j);
                double dx = actual.centro().x - otra.centro().x;
                double dy = actual.centro().y - otra.centro().y;
                double dist = Math.sqrt(dx*dx + dy*dy);
                double radioMenor = Math.min(actual.radio(), otra.radio());
                if (dist < Math.max(radioMenor * 2.0, 10)) {
                    fusionado[j] = true;
                    double ao = otra.boundingBox().width * otra.boundingBox().height;
                    if (ao > mejorArea) { mejor = otra; mejorArea = ao; }
                }
            }
            resultado.add(mejor);
        }
        return resultado;
    }

    private double calcularRellenoNegro(Mat imagenBinaria, Rect bbox) {
        int x = Math.max(0, bbox.x), y = Math.max(0, bbox.y);
        int w = Math.min(bbox.width, imagenBinaria.cols() - x);
        int h = Math.min(bbox.height, imagenBinaria.rows() - y);
        if (w <= 0 || h <= 0) return 0;
        Mat roi = new Mat(imagenBinaria, new Rect(x, y, w, h));
        int blancos = Core.countNonZero(roi);
        return 1.0 - (double)blancos / (w * h);
    }
}
