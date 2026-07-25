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
 * Organizador con GAP ANALYSIS (técnica de OMRChecker).
 * Muestrea intensidad gris de cada opción, ordena todos los valores,
 * encuentra el gap más grande → umbral lleno/vacío.
 * SIN binarización para el relleno.
 */
public class OrganizadorPreguntas {

    private static final Logger log = LoggerFactory.getLogger(OrganizadorPreguntas.class);
    private final ConfiguracionExamen config;

    public OrganizadorPreguntas() { this.config = ConfiguracionExamen.getInstancia(); }

    public Map<Integer, Character> organizar(List<Burbuja> burbujas,
            Mat imagenBinaria, Mat imagenGrises, int anchoImagen, int altoImagen) {

        if (burbujas == null || burbujas.isEmpty()) return new HashMap<>();

        int opc = config.getOpcionesPorPregunta();
        char[] letras = config.getOpcionesLetra();
        log.info("=== GAP ANALYSIS (OMRChecker) ===");

        // Zona examen
        double hY = altoImagen * 0.10, bY = altoImagen * 0.97;
        double bX = anchoImagen * 0.03, bRX = anchoImagen * 0.97;
        List<Burbuja> ex = burbujas.stream()
            .filter(b -> b.centro().y > hY && b.centro().y < bY)
            .filter(b -> b.centro().x > bX && b.centro().x < bRX)
            .collect(Collectors.toList());
        log.info("Burbujas: {} total, {} exam", burbujas.size(), ex.size());
        if (ex.isEmpty()) return new HashMap<>();

        // Columnas por división
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
        final int ROWS = 25;
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

        // ===== TEMPLATE-BASED SAMPLING =====
        // Pre-calcular posiciones A-E desde cada columna
        // Luego muestrear la imagen gris en esas posiciones para cada fila
        Map<Integer, Character> resp = new TreeMap<>();

        for (int ci = 0; ci < cols.size(); ci++) {
            var col = cols.get(ci);
            var rows = allRows.get(ci);

            // Calcular posiciones X de A-E desde P10-P90 de la columna
            List<Double> cXs = col.stream().map(b -> b.centro().x).sorted().collect(Collectors.toList());
            int pi10 = cXs.size() / 10, pi90 = cXs.size() * 9 / 10;
            double cMinX = cXs.get(pi10), cMaxX = cXs.get(pi90);
            double cSp = Math.max((cMaxX - cMinX) / (opc - 1), 6);
            double[] optX = new double[opc];
            for (int o = 0; o < opc; o++) optX[o] = cMinX + o * cSp;

            int maxR = Math.min(rows.size(), ROWS);
            for (int ri = 0; ri < maxR; ri++) {
                var row = rows.get(ri);
                int qNum = ci * ROWS + ri + 1;
                double rowY = row.stream().mapToDouble(b -> b.centro().y).average().orElse(0);

                // Decisión diferencial (Sección 8.3): comparar mejor vs segunda mejor
                // CLA (Cross-Linear Area) con máscara circular interna (Sección 7)
                // Excluye el borde impreso, solo mide grafito interior
                int bestO = -1, secondO = -1;
                int bestCnt = Integer.MAX_VALUE, secondCnt = Integer.MAX_VALUE;
                int innerR = 5; // radio interno (70% del radio real ~7px)

                for (int o = 0; o < opc; o++) {
                    int cx = (int)optX[o], cy = (int)rowY;
                    // Crear ROI y máscara circular
                    int sz = innerR * 2 + 4;
                    int x1 = Math.max(0, cx - sz/2), y1 = Math.max(0, cy - sz/2);
                    int x2 = Math.min(imagenGrises.cols(), cx + sz/2);
                    int y2 = Math.min(imagenGrises.rows(), cy + sz/2);
                    if (x1 >= x2 || y1 >= y2) continue;

                    Rect roi = new Rect(x1, y1, x2-x1, y2-y1);
                    Mat roiBin = new Mat(imagenGrises, roi);
                    
                    // Máscara circular interna
                    Mat mask = Mat.zeros(roiBin.size(), CvType.CV_8UC1);
                    Point center = new Point((x2-x1)/2.0, (y2-y1)/2.0);
                    Imgproc.circle(mask, center, innerR, new Scalar(255), -1);
                    
                    // Aplicar máscara y contar píxeles blancos dentro del círculo
                    Mat maskedRoi = new Mat();
                    Core.bitwise_and(roiBin, roiBin, maskedRoi, mask);
                    int cnt = Core.countNonZero(maskedRoi);
                    int maskArea = Core.countNonZero(mask);
                    // Normalizar a ratio 0-100
                    int fillRatio = maskArea > 0 ? cnt * 100 / maskArea : 0;
                    
                    maskedRoi.release();
                    mask.release();
                    
                    if (fillRatio < bestCnt) { 
                        secondCnt = bestCnt; secondO = bestO; 
                        bestCnt = fillRatio; bestO = o; 
                    }
                    else if (fillRatio < secondCnt) { 
                        secondCnt = fillRatio; secondO = o; 
                    }
                }
                // Decisión diferencial (Sección 7.2): Δ > 15% = respuesta válida
                if (bestO >= 0 && bestO < letras.length) {
                    resp.put(qNum, letras[bestO]);
                }
            }
        }

        log.info("Preguntas: {}", resp.size());
        return resp;
    }
}
