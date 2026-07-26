package com.criollo.omr.procesamiento;

import com.criollo.omr.config.ConfiguracionExamen;
import org.opencv.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OrganizadorPreguntas {

    private static final Logger log = LoggerFactory.getLogger(OrganizadorPreguntas.class);
    private final ConfiguracionExamen config;

    public OrganizadorPreguntas() { this.config = ConfiguracionExamen.getInstancia(); }

    public Map<Integer, Character> organizar(List<DetectorBurbujas.Burbuja> burbujas,
            Mat imagenBinaria, Mat imagenGrises, int anchoImagen, int altoImagen) {

        int opc = config.getOpcionesPorPregunta();
        char[] letras = config.getOpcionesLetra();
        int nCols = burbujas != null && burbujas.size() > 700 ? 4 : 2;
        final int ROWS = 25;

        log.info("=== GRID SIMPLE ({} cols, {} rows) ===", nCols, ROWS);

        // Zona de examen
        double yTop = altoImagen * 0.10;
        double yBot = altoImagen * 0.97;

        // Calibrar posiciones desde burbujas detectadas
        // Agrupar X por columna y encontrar centro de cada opción
        double[][] optX = new double[nCols][opc];
        double colW = anchoImagen / (double)nCols;

        // Recolectar X de burbujas por columna
        List<List<Double>> colXs = new ArrayList<>();
        for (int c = 0; c < nCols; c++) colXs.add(new ArrayList<>());
        for (var b : burbujas) {
            if (b.centro().y > yTop && b.centro().y < yBot) {
                int ci = (int)(b.centro().x / colW);
                if (ci >= 0 && ci < nCols) colXs.get(ci).add(b.centro().x);
            }
        }

        // Para cada columna, calibrar 5 posiciones A-E desde P10-P90
        for (int ci = 0; ci < nCols; ci++) {
            var xs = colXs.get(ci);
            if (xs.size() < 10) {
                // Fallback: división uniforme
                double cx = anchoImagen * (2.0*ci+1) / (2.0*nCols);
                for (int o = 0; o < opc; o++) optX[ci][o] = cx + (o-2)*12;
                continue;
            }
            Collections.sort(xs);
            int p10 = xs.size()/10, p90 = xs.size()*9/10;
            double minX = xs.get(p10), maxX = xs.get(p90);
            double sp = (maxX - minX) / (opc - 1);
            for (int o = 0; o < opc; o++) optX[ci][o] = minX + o * sp;
        }

        // Rows
        double rowH = (yBot - yTop) / ROWS;

        int sr = 7;
        Map<Integer, Character> resp = new TreeMap<>();

        for (int ci = 0; ci < nCols; ci++) {
            for (int ri = 0; ri < ROWS; ri++) {
                int qNum = ci * ROWS + ri + 1;
                double ry = yTop + ri * rowH + rowH/2;
                int bestO = -1, bestW = Integer.MAX_VALUE; // MENOR = más negro = relleno

                for (int oi = 0; oi < opc; oi++) {
                    int cx = (int)optX[ci][oi];
                    int cy = (int)ry;
                    int x1 = Math.max(0, cx-sr), y1 = Math.max(0, cy-sr);
                    int x2 = Math.min(imagenBinaria.cols(), cx+sr);
                    int y2 = Math.min(imagenBinaria.rows(), cy+sr);
                    if (x1 >= x2 || y1 >= y2) continue;
                    int w = Core.countNonZero(new Mat(imagenBinaria, new Rect(x1,y1,x2-x1,y2-y1)));
                    if (w < bestW) { bestW = w; bestO = oi; }
                }
                if (bestO >= 0 && bestO < letras.length) resp.put(qNum, letras[bestO]);
            }
        }

        log.info("Preguntas: {}", resp.size());
        return resp;
    }
}
