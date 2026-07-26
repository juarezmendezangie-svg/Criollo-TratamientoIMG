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

        // Column centers: 1/8, 3/8, 5/8, 7/8 del ancho
        double[] colX = new double[nCols];
        for (int c = 0; c < nCols; c++)
            colX[c] = anchoImagen * (2*c + 1) / (2.0 * nCols);

        // Option spacing: ~12px per option (A-E span ~48px)
        double optSpan = 48;
        double optStart = -optSpan/2;

        // Rows
        double yTop = altoImagen * 0.10;
        double yBot = altoImagen * 0.97;
        double rowH = (yBot - yTop) / ROWS;

        int sr = 7;
        Map<Integer, Character> resp = new TreeMap<>();

        for (int ci = 0; ci < nCols; ci++) {
            for (int ri = 0; ri < ROWS; ri++) {
                int qNum = ci * ROWS + ri + 1;
                double ry = yTop + ri * rowH + rowH/2;
                int bestO = -1, bestW = -1;

                for (int oi = 0; oi < opc; oi++) {
                    int cx = (int)(colX[ci] + optStart + oi * optSpan/(opc-1));
                    int cy = (int)ry;
                    int x1 = Math.max(0, cx-sr), y1 = Math.max(0, cy-sr);
                    int x2 = Math.min(imagenBinaria.cols(), cx+sr);
                    int y2 = Math.min(imagenBinaria.rows(), cy+sr);
                    if (x1 >= x2 || y1 >= y2) continue;
                    int w = Core.countNonZero(new Mat(imagenBinaria, new Rect(x1,y1,x2-x1,y2-y1)));
                    if (w > bestW) { bestW = w; bestO = oi; }
                }
                if (bestO >= 0 && bestO < letras.length) resp.put(qNum, letras[bestO]);
            }
        }

        log.info("Preguntas: {}", resp.size());
        return resp;
    }
}
