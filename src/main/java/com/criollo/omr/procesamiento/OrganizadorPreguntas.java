package com.criollo.omr.procesamiento;

import com.criollo.omr.config.ConfiguracionExamen;
import org.opencv.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Organiza burbujas detectadas en preguntas y opciones usando clustering
 * de posiciones X (opciones A-E) e Y (filas 1-25/50).
 *
 * ALGORITMO:
 * 1. Filtrar burbujas en zona de examen
 * 2. Separar en columnas por posición X
 * 3. Dentro de cada columna, agrupar X en 5 clusters → centros de opción (A-E)
 * 4. Dentro de cada columna, agrupar Y en N clusters → centros de fila
 * 5. Para cada celda del grid, muestrear imagen binaria y elegir MENOR countNonZero
 */
public class OrganizadorPreguntas {

    private static final Logger log = LoggerFactory.getLogger(OrganizadorPreguntas.class);
    private final ConfiguracionExamen config;

    public OrganizadorPreguntas() { this.config = ConfiguracionExamen.getInstancia(); }

    public Map<Integer, Character> organizar(List<DetectorBurbujas.Burbuja> burbujas,
            Mat imagenBinaria, Mat imagenGrises, int anchoImagen, int altoImagen) {
        return organizar(burbujas, imagenBinaria, imagenGrises, anchoImagen, altoImagen, false);
    }

    public Map<Integer, Character> organizar(List<DetectorBurbujas.Burbuja> burbujas,
            Mat imagenBinaria, Mat imagenGrises, int anchoImagen, int altoImagen, boolean esPlantilla) {

        int opc = config.getOpcionesPorPregunta();
        char[] letras = config.getOpcionesLetra();
        int totalPreguntas = config.getMaxPreguntas();

        if (burbujas == null || burbujas.isEmpty()) {
            log.warn("No hay burbujas para organizar");
            return Collections.emptyMap();
        }

        // 1. Zona de examen (empezando en 0.25 para incluir las 25 filas exactamente)
        double yTop = altoImagen * 0.25;
        double yBot = altoImagen * 0.945;

        // Determinar número de columnas dinámicamente según la geometría de las burbujas
        long burbujasIzq = burbujas.stream()
            .filter(b -> b.centro().y > yTop && b.centro().y < yBot && b.centro().x < anchoImagen * 0.24)
            .count();
        int nCols = (burbujasIzq < 80 || totalPreguntas <= 50) ? 2 : 4;
        if (nCols == 4) {
            opc = 4;
            letras = new char[]{'A', 'B', 'C', 'D'};
        }
        int preguntasPorCol = 25;

        log.info("=== CLUSTER GRID ({} cols, {} preguntas/col, {} opciones, plantilla={}) ===",
            nCols, preguntasPorCol, opc, esPlantilla);

        // 2. Filtrar burbujas en zona de examen
        List<DetectorBurbujas.Burbuja> enZona = new ArrayList<>();
        for (var b : burbujas) {
            double y = b.centro().y;
            if (y > yTop && y < yBot) enZona.add(b);
        }
        log.info("Burbujas en zona de examen: {} / {}", enZona.size(), burbujas.size());

        if (enZona.isEmpty()) {
            log.warn("No hay burbujas en zona de examen");
            return Collections.emptyMap();
        }

        // 3. Separar en columnas por posición X
        double colW = anchoImagen / (double) nCols;
        List<List<DetectorBurbujas.Burbuja>> porColumna = new ArrayList<>();
        for (int c = 0; c < nCols; c++) porColumna.add(new ArrayList<>());
        for (var b : enZona) {
            int ci = (int) (b.centro().x / colW);
            if (ci >= 0 && ci < nCols) porColumna.get(ci).add(b);
        }

        // 4. Mapear opciones X y filas Y para cada columna de forma independiente
        double[][] optX = new double[nCols][opc];
        double[][] rowY = new double[nCols][];

        for (int ci = 0; ci < nCols; ci++) {
            List<DetectorBurbujas.Burbuja> col = porColumna.get(ci);
            log.info("Columna {}: {} burbujas", ci, col.size());

            if (col.isEmpty()) {
                rowY[ci] = new double[preguntasPorCol];
                continue;
            }

            double maxRawX = col.stream().mapToDouble(b -> b.centro().x).max().orElse(0);
            double leftBound = maxRawX - (nCols == 2 ? colW * 0.60 : colW * 0.43);
            List<DetectorBurbujas.Burbuja> colLimpia = col.stream()
                .filter(b -> b.centro().x > leftBound)
                .collect(Collectors.toList());

            if (colLimpia.isEmpty()) {
                colLimpia = col;
            }

            // Obtener opciones A-E de forma equidistante (evita duplicar centroides si alguna letra no fue marcada)
            List<Double> allX = new ArrayList<>();
            for (var b : colLimpia) allX.add(b.centro().x);

            double[] clusX = clusterCentros(allX, Math.min(opc, allX.size()), colW * 0.03);
            double minX = clusX[0];
            double maxX = clusX[clusX.length - 1];
            double expectedSpan = nCols == 2 ? anchoImagen * 0.2539 : anchoImagen * 0.0976;
            if (maxX - minX < expectedSpan * 0.75) {
                double center = 0;
                for (double x : clusX) center += x;
                center /= clusX.length;
                minX = center - expectedSpan / 2.0;
                maxX = center + expectedSpan / 2.0;
            }
            for (int o = 0; o < opc; o++) {
                optX[ci][o] = minX + o * (maxX - minX) / (opc - 1);
            }

            // Cluster Y → preguntasPorCol filas
            List<Double> allY = new ArrayList<>();
            for (var b : colLimpia) allY.add(b.centro().y);
            rowY[ci] = clusterCentros(allY, preguntasPorCol, (yBot - yTop) / preguntasPorCol * 0.4);

            log.info("Columna {} - Opciones X: {}", ci,Arrays.toString(optX[ci]));
            log.info("Columna {} - Filas Y (primeras 5): {}...", ci,
                Arrays.toString(Arrays.copyOf(rowY[ci], Math.min(5, rowY[ci].length))));
        }

        // 5. Muestrear grid: MENOR countNonZero = relleno (THRESH_BINARY: negro=0)
        int sr = 7;
        Map<Integer, Character> resp = new TreeMap<>();

        for (int ci = 0; ci < nCols; ci++) {
            for (int ri = 0; ri < preguntasPorCol; ri++) {
                int qNum;
                int pos = ri % 5;
                if (nCols == 4 && esPlantilla) {
                    int block = ri / 5;
                    int riMapped = block * 5 + (pos == 0 ? 4 : pos - 1);
                    qNum = ci * preguntasPorCol + riMapped + 1;
                } else {
                    qNum = ci * preguntasPorCol + ri + 1;
                }
                if (qNum > totalPreguntas) break;

                int bestO = -1, bestW = Integer.MAX_VALUE;

                for (int oi = 0; oi < opc; oi++) {
                    int cx = (int) optX[ci][oi];
                    int cy = (int) rowY[ci][ri];
                    int x1 = Math.max(0, cx - sr), y1 = Math.max(0, cy - sr);
                    int x2 = Math.min(imagenBinaria.cols(), cx + sr);
                    int y2 = Math.min(imagenBinaria.rows(), cy + sr);
                    if (x1 >= x2 || y1 >= y2) continue;

                    int w = Core.countNonZero(new Mat(imagenBinaria, new Rect(x1, y1, x2 - x1, y2 - y1)));
                    if (w < bestW) { bestW = w; bestO = oi; }
                }

                if (bestO >= 0 && bestO < letras.length) {
                    if (bestW > 100) {
                        if (nCols == 4) {
                            resp.put(qNum, 'E');
                        }
                        // En exámenes de 2 columnas (50PR), si bestW > 100 la pregunta está sin responder (en blanco): no agregar a resp
                    } else {
                        resp.put(qNum, letras[bestO]);
                    }
                }
            }
        }

        log.info("Preguntas detectadas: {}", resp.size());
        return resp;
    }

    /**
     * Agrupa una lista de valores en N clusters y retorna los centroides.
     * Usa clustering por distancia mínima (greedy).
     */
    private double[] clusterCentros(List<Double> valores, int nClusters, double maxDist) {
        if (valores.size() <= nClusters) {
            // Menos valores que clusters → retorno los valores directamente
            double[] result = new double[valores.size()];
            Collections.sort(valores);
            for (int i = 0; i < result.length; i++) result[i] = valores.get(i);
            return result;
        }

        Collections.sort(valores);

        // Agrupar valores consecutivos que estén dentro de maxDist
        List<List<Double>> grupos = new ArrayList<>();
        List<Double> grupoActual = new ArrayList<>();
        grupoActual.add(valores.get(0));

        for (int i = 1; i < valores.size(); i++) {
            if (valores.get(i) - valores.get(i - 1) <= maxDist) {
                grupoActual.add(valores.get(i));
            } else {
                grupos.add(grupoActual);
                grupoActual = new ArrayList<>();
                grupoActual.add(valores.get(i));
            }
        }
        grupos.add(grupoActual);

        log.info("  Clustering: {} valores → {} grupos (maxDist={})",
            valores.size(), grupos.size(), String.format("%.1f", maxDist));

        // Si tenemos más grupos que clusters, fusionar los más cercanos
        while (grupos.size() > nClusters) {
            int bestI = 0;
            double bestGap = Double.MAX_VALUE;
            for (int i = 0; i < grupos.size() - 1; i++) {
                double gap = mean(grupos.get(i + 1)) - mean(grupos.get(i));
                if (gap < bestGap) { bestGap = gap; bestI = i; }
            }
            // Fusionar grupo[i] con grupo[i+1]
            grupos.get(bestI).addAll(grupos.get(bestI + 1));
            grupos.remove(bestI + 1);
        }

        // Si tenemos menos grupos que clusters, dividir los más grandes
        while (grupos.size() < nClusters) {
            int biggestI = 0;
            for (int i = 1; i < grupos.size(); i++) {
                if (grupos.get(i).size() > grupos.get(biggestI).size()) biggestI = i;
            }
            List<Double> biggest = grupos.get(biggestI);
            int mid = biggest.size() / 2;
            List<Double> first = new ArrayList<>(biggest.subList(0, mid));
            List<Double> second = new ArrayList<>(biggest.subList(mid, biggest.size()));
            grupos.set(biggestI, first);
            grupos.add(biggestI + 1, second);
        }

        // Calcular centroides
        double[] centroides = new double[nClusters];
        for (int i = 0; i < nClusters; i++) {
            centroides[i] = mean(grupos.get(i));
        }

        log.info("  Centroides: {}", Arrays.toString(centroides));
        return centroides;
    }

    private double mean(List<Double> vals) {
        double sum = 0;
        for (double v : vals) sum += v;
        return sum / vals.size();
    }
}
