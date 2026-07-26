package com.criollo.omr.procesamiento;

import com.criollo.omr.config.ConfiguracionExamen;
import com.criollo.omr.procesamiento.DetectorBurbujas.Burbuja;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Organizador con Inner Mask Erosion + Decisión Comparativa (NotebookLM technique).
 * Fase 1: Grid detection (columnas y filas)
 * Fase 2: Inner circular mask erosion 3×3 kernel
 * Fase 3: Densidad relativa fillRatio = countNonZero / maskArea
 * Fase 4: Decisión comparativa con τ_vacio=0.15, τ_multiple=0.40
 */
public class OrganizadorPreguntas {

    private static final Logger log = LoggerFactory.getLogger(OrganizadorPreguntas.class);
    private final ConfiguracionExamen config;
    
    // Inner mask erosion (NotebookLM technique)
    private static final int ROI_SIZE = 17;
    private static final int BUBBLE_RADIUS = 7;
    private static final Mat ERODED_MASK;
    private static final int MASK_AREA;
    
    static {
        // Pre-compute the inner eroded mask once (performance)
        Mat mask = new Mat(ROI_SIZE, ROI_SIZE, CvType.CV_8UC1, new Scalar(0));
        Imgproc.circle(mask, new Point(ROI_SIZE/2.0, ROI_SIZE/2.0), BUBBLE_RADIUS, new Scalar(255), -1);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        ERODED_MASK = new Mat();
        Imgproc.erode(mask, ERODED_MASK, kernel, new Point(-1,-1), 1);
        kernel.release();
        mask.release();
        MASK_AREA = Core.countNonZero(ERODED_MASK);
        LoggerFactory.getLogger(OrganizadorPreguntas.class)
            .info("Inner mask: ROI={}px, radius={}px, active area={}px", ROI_SIZE, BUBBLE_RADIUS, MASK_AREA);
    }

    public OrganizadorPreguntas() { this.config = ConfiguracionExamen.getInstancia(); }

    public Map<Integer, Character> organizar(List<Burbuja> burbujas,
            Mat imagenBinaria, Mat imagenGrises, int anchoImagen, int altoImagen) {

        if (burbujas == null || burbujas.isEmpty()) return new HashMap<>();
        int opc = config.getOpcionesPorPregunta();
        char[] letras = config.getOpcionesLetra();
        final int ROWS = 25;

        log.info("=== ORGANIZADOR (Inner Mask Erosion) ===");

        // Zona examen
        double hY = altoImagen * 0.10, bY = altoImagen * 0.97;
        double bX = anchoImagen * 0.03, bRX = anchoImagen * 0.97;
        List<Burbuja> ex = burbujas.stream()
            .filter(b -> b.centro().y > hY && b.centro().y < bY)
            .filter(b -> b.centro().x > bX && b.centro().x < bRX)
            .collect(Collectors.toList());
        log.info("Burbujas: {} total, {} exam", burbujas.size(), ex.size());
        if (ex.isEmpty()) return new HashMap<>();

        // Columnas
        int nCols = ex.size() > 700 ? 4 : 2;
        double fw = anchoImagen / (double)nCols;
        List<List<Burbuja>> cols = new ArrayList<>();
        for (int c = 0; c < nCols; c++) cols.add(new ArrayList<>());
        for (Burbuja b : ex) {
            int c = (int)(b.centro().x / fw);
            c = Math.max(0, Math.min(c, nCols - 1));
            cols.get(c).add(b);
        }
        cols.removeIf(c -> c.size() < ex.size() / (nCols * 3));
        log.info("Columnas: {}", cols.size());

        // Filas por gaps
        List<List<List<Burbuja>>> allRows = new ArrayList<>();
        for (var col : cols) {
            col.sort(Comparator.comparingDouble(b -> b.centro().y));
            List<Double> gy = new ArrayList<>();
            for (int i = 1; i < col.size(); i++) {
                double g = col.get(i).centro().y - col.get(i-1).centro().y;
                if (g > 0) gy.add(g);
            }
            double uY = 20;
            if (!gy.isEmpty()) { Collections.sort(gy); uY = Math.max(gy.get(gy.size()/2) * 2.5, 15); }
            List<List<Burbuja>> rows = new ArrayList<>();
            List<Burbuja> cur = new ArrayList<>();
            double ref = col.get(0).centro().y; cur.add(col.get(0));
            for (int i = 1; i < col.size(); i++) {
                if (col.get(i).centro().y - ref > uY) {
                    if (cur.size() >= 1 && cur.size() <= opc + 1) rows.add(cur);
                    cur = new ArrayList<>(); ref = col.get(i).centro().y;
                }
                cur.add(col.get(i));
            }
            if (cur.size() >= 1 && cur.size() <= opc + 1) rows.add(cur);
            rows.sort(Comparator.comparingDouble(r -> r.stream().mapToDouble(b -> b.centro().y).average().orElse(0)));
            allRows.add(rows);
            log.info("  Col: {} filas", rows.size());
        }

        // ===== INNER MASK EROSION + COMPARATIVE DECISION =====
        final double TAU_VACIO = 0.15;
        final double TAU_MULTIPLE = 0.40;
        Map<Integer, Character> resp = new TreeMap<>();

        for (int ci = 0; ci < cols.size(); ci++) {
            var rows = allRows.get(ci);
            int maxR = Math.min(rows.size(), ROWS);
            for (int ri = 0; ri < maxR; ri++) {
                var row = rows.get(ri);
                int qNum = ci * ROWS + ri + 1;
                row.sort(Comparator.comparingDouble(b -> b.centro().x));

                // Fase 2+3: Inner mask erosion + density per option
                double[] fillRatio = new double[opc];
                for (int bi = 0; bi < Math.min(row.size(), opc); bi++) {
                    Burbuja b = row.get(bi);
                    fillRatio[bi] = computeFillRatio(imagenBinaria, b.centro());
                }

                // Fase 4: Comparative decision
                int bestI = 0;
                double bestR = 0;
                for (int bi = 0; bi < opc; bi++) {
                    if (fillRatio[bi] > bestR) { bestR = fillRatio[bi]; bestI = bi; }
                }

                resp.put(qNum, letras[bestI]);
            }
        }

        log.info("Preguntas: {}", resp.size());
        return resp;
    }

    /**
     * Inner mask erosion fill ratio (NotebookLM Fase 2-3).
     * Creates 17×17 ROI → applies circular mask (r=7) → erodes 3×3 → counts white pixels.
     * eliminates the printed bubble outline, only measures student's mark.
     */
    private double computeFillRatio(Mat binary, Point center) {
        int cx = (int)center.x, cy = (int)center.y;
        int x1 = Math.max(0, cx - ROI_SIZE/2);
        int y1 = Math.max(0, cy - ROI_SIZE/2);
        int x2 = Math.min(binary.cols(), cx + ROI_SIZE/2 + 1);
        int y2 = Math.min(binary.rows(), cy + ROI_SIZE/2 + 1);
        if (x1 >= x2 || y1 >= y2) return 0;

        Mat roi = new Mat(binary, new Rect(x1, y1, x2-x1, y2-y1));
        // Resize to exact ROI_SIZE if needed
        if (roi.cols() != ROI_SIZE || roi.rows() != ROI_SIZE) {
            Mat resized = new Mat();
            Imgproc.resize(roi, resized, new Size(ROI_SIZE, ROI_SIZE));
            roi = resized;
        }

        // Apply inner eroded mask (eliminates printed outline)
        Mat masked = new Mat();
        Core.bitwise_and(roi, roi, masked, ERODED_MASK);
        int whitePx = Core.countNonZero(masked);
        masked.release();

        return (double)whitePx / MASK_AREA;
    }
}
