package com.criollo.omr.procesamiento;

import com.criollo.omr.config.ConfiguracionExamen;
import com.criollo.omr.procesamiento.DetectorBurbujas.Burbuja;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class OrganizadorPreguntas {

    private static final Logger log = LoggerFactory.getLogger(OrganizadorPreguntas.class);
    private final ConfiguracionExamen config;

    public OrganizadorPreguntas() { this.config = ConfiguracionExamen.getInstancia(); }

    public Map<Integer, Character> organizar(List<Burbuja> burbujas,
            Mat imagenBinaria, Mat imagenGrises, int anchoImagen, int altoImagen) {

        if (burbujas == null || burbujas.isEmpty()) return new HashMap<>();
        int opc = config.getOpcionesPorPregunta();
        char[] letras = config.getOpcionesLetra();
        final int ROWS = 25;

        log.info("=== ORGANIZADOR ===");

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

        // Filas por gaps (limitadas a 25)
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

        // Asignar preguntas: sort X, mayor relleno
        Map<Integer, Character> resp = new TreeMap<>();
        for (int ci = 0; ci < cols.size(); ci++) {
            var rows = allRows.get(ci);
            int maxR = Math.min(rows.size(), ROWS);
            for (int ri = 0; ri < maxR; ri++) {
                var row = rows.get(ri);
                int qNum = ci * ROWS + ri + 1;
                row.sort(Comparator.comparingDouble(b -> b.centro().x));
                int bestI = -1;
                double bestR = 0;
                for (int bi = 0; bi < row.size(); bi++) {
                    if (row.get(bi).porcentajeRelleno() > bestR) {
                        bestR = row.get(bi).porcentajeRelleno();
                        bestI = bi;
                    }
                }
                if (bestI >= 0 && bestI < letras.length) {
                    resp.put(qNum, letras[bestI]);
                }
            }
        }

        log.info("Preguntas: {}", resp.size());
        return resp;
    }
}
