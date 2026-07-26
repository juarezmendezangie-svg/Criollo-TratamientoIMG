package com.criollo.omr.procesamiento;

import com.criollo.omr.config.ConfiguracionExamen;
import org.opencv.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Organizador por GRID SPLITTING directo (técnica del video de YouTube).
 * NO detecta burbujas individuales — divide la imagen binarizada en una
 * cuadrícula regular y cuenta píxeles blancos en cada celda.
 * Simple, robusto, funciona con cualquier tamaño de burbuja.
 */
public class OrganizadorPreguntas {

    private static final Logger log = LoggerFactory.getLogger(OrganizadorPreguntas.class);
    private final ConfiguracionExamen config;

    public OrganizadorPreguntas() { this.config = ConfiguracionExamen.getInstancia(); }

    /**
     * Grid splitting: ignora la lista de burbujas, divide la imagen
     * binarizada directamente en una cuadrícula.
     */
    public Map<Integer, Character> organizar(List<DetectorBurbujas.Burbuja> burbujas,
            Mat imagenBinaria, Mat imagenGrises, int anchoImagen, int altoImagen) {

        int opc = config.getOpcionesPorPregunta();
        char[] letras = config.getOpcionesLetra();
        int nCols = burbujas != null && burbujas.size() > 700 ? 4 : 2;
        final int ROWS = 25;

        log.info("=== GRID SPLITTING ({} cols × {} rows) ===", nCols, ROWS);

        // Zona examen: excluir 10% superior, 3% bordes
        int yStart = (int)(altoImagen * 0.10);
        int yEnd = (int)(altoImagen * 0.97);
        int xStart = (int)(anchoImagen * 0.03);
        int xEnd = (int)(anchoImagen * 0.97);

        int examH = yEnd - yStart;
        int examW = xEnd - xStart;
        double cellH = (double)examH / ROWS;
        double cellW = (double)examW / (nCols * opc);

        log.info("Grid: cell={}x{}px, exam zone={}x{}px",
            String.format("%.1f",cellW), String.format("%.1f",cellH), examW, examH);

        Map<Integer, Character> resp = new TreeMap<>();

        for (int ci = 0; ci < nCols; ci++) {
            int colStartX = xStart + (int)(ci * examW / nCols);
            
            for (int ri = 0; ri < ROWS; ri++) {
                int rowY = yStart + (int)(ri * cellH);
                int qNum = ci * ROWS + ri + 1;

                // Muestrear las 5 opciones A-E en esta fila+columna
                int bestO = -1;
                int bestWhite = -1;

                for (int oi = 0; oi < opc; oi++) {
                    int optX = colStartX + (int)(oi * cellW);
                    int x1 = Math.max(0, optX + 2);
                    int y1 = Math.max(0, rowY + 2);
                    int x2 = Math.min(imagenBinaria.cols(), optX + (int)cellW - 2);
                    int y2 = Math.min(imagenBinaria.rows(), rowY + (int)cellH - 2);
                    
                    if (x1 >= x2 || y1 >= y2) continue;

                    Rect roi = new Rect(x1, y1, x2 - x1, y2 - y1);
                    Mat cell = new Mat(imagenBinaria, roi);
                    int white = Core.countNonZero(cell);
                    
                    if (white > bestWhite) {
                        bestWhite = white;
                        bestO = oi;
                    }
                }

                if (bestO >= 0 && bestO < letras.length) {
                    resp.put(qNum, letras[bestO]);
                }
            }
        }

        log.info("Preguntas detectadas: {}", resp.size());
        return resp;
    }
}
