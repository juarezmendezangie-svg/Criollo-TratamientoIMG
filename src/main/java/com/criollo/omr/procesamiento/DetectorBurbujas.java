package com.criollo.omr.procesamiento;

import com.criollo.omr.config.ConfiguracionExamen;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Detector de burbujas mejorado con filtros más estrictos.
 * - Filtro de área ajustado (80-3000 px para burbujas ~15-20px diámetro)
 * - Filtro de circularidad estricto (≥0.60)
 * - Filtro de aspecto (width/height entre 0.5 y 2.0)
 * - Fusión con distancia mínima de 8px
 */
public class DetectorBurbujas {

    private static final Logger log = LoggerFactory.getLogger(DetectorBurbujas.class);
    private final ConfiguracionExamen config;

    public DetectorBurbujas() { this.config = ConfiguracionExamen.getInstancia(); }

    public record Burbuja(Point centro, int radio, double porcentajeRelleno,
                          Rect boundingBox, int indiceContour) {}

    public List<Burbuja> detectarBurbujas(Mat imagenBinaria) {
        if (imagenBinaria == null || imagenBinaria.empty()) return new ArrayList<>();

        List<MatOfPoint> contornos = new ArrayList<>();
        Mat jerarquia = new Mat();
        Imgproc.findContours(imagenBinaria, contornos, jerarquia, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        jerarquia.release();

        // También buscar en imagen invertida para capturar burbujas completamente rellenadas (negras)
        Mat binInv = new Mat();
        Core.bitwise_not(imagenBinaria, binInv);
        Mat jerarquia2 = new Mat();
        Imgproc.findContours(binInv, contornos, jerarquia2, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        jerarquia2.release();
        binInv.release();

        int aMin = config.getAreaMinima(), aMax = config.getAreaMaxima();
        double cMin = config.getCircularidadMinima();

        log.info("Contornos totales: {}, filtros: area=[{},{}], circ≥{}", contornos.size(), aMin, aMax, cMin);

        List<Burbuja> todas = new ArrayList<>();
        int descartados = 0;
        for (int i = 0; i < contornos.size(); i++) {
            MatOfPoint c = contornos.get(i);
            double area = Math.abs(Imgproc.contourArea(c));
            if (area < aMin || area > aMax) { descartados++; continue; }

            Rect bbox = Imgproc.boundingRect(c);

            // Filtro de aspecto: width y height deben ser similares (círculo)
            double aspecto = (double) Math.max(bbox.width, bbox.height) / Math.max(1, Math.min(bbox.width, bbox.height));
            if (aspecto > 2.0) { descartados++; continue; }

            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double perim = Imgproc.arcLength(c2f, true);
            c2f.release();
            if (perim <= 0) { descartados++; continue; }

            double circ = (4.0 * Math.PI * area) / (perim * perim);
            if (circ < cMin) { descartados++; continue; }

            Point centro = new Point(bbox.x + bbox.width / 2.0, bbox.y + bbox.height / 2.0);
            int radio = Math.max(bbox.width, bbox.height) / 2;
            double relleno = calcRelleno(imagenBinaria, bbox);
            todas.add(new Burbuja(centro, radio, relleno, bbox, i));
        }

        log.info("Válidas post-filtros: {} (descartados: {})", todas.size(), descartados);
        List<Burbuja> resultado = fusionar(todas);
        log.info("Post-fusión: {}", resultado.size());
        return resultado;
    }

    private List<Burbuja> fusionar(List<Burbuja> burbujas) {
        if (burbujas.size() <= 1) return new ArrayList<>(burbujas);
        List<Burbuja> res = new ArrayList<>();
        boolean[] f = new boolean[burbujas.size()];
        for (int i = 0; i < burbujas.size(); i++) {
            if (f[i]) continue;
            Burbuja act = burbujas.get(i), best = act;
            double bestArea = act.boundingBox().width * act.boundingBox().height;
            for (int j = i + 1; j < burbujas.size(); j++) {
                if (f[j]) continue;
                Burbuja otr = burbujas.get(j);
                double dx = act.centro().x - otr.centro().x;
                double dy = act.centro().y - otr.centro().y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                // Fusionar solo si están muy cerca (8px o menos)
                if (dist < 8) {
                    f[j] = true;
                    double ao = otr.boundingBox().width * otr.boundingBox().height;
                    if (ao > bestArea) { best = otr; bestArea = ao; }
                }
            }
            res.add(best);
        }
        return res;
    }

    private double calcRelleno(Mat bin, Rect bb) {
        int x = Math.max(0, bb.x), y = Math.max(0, bb.y);
        int w = Math.min(bb.width, bin.cols() - x), h = Math.min(bb.height, bin.rows() - y);
        if (w <= 0 || h <= 0) return 0;
        Mat roi = new Mat(bin, new Rect(x, y, w, h));
        int blancos = Core.countNonZero(roi);
        return 1.0 - (double) blancos / (w * h);
    }
}
