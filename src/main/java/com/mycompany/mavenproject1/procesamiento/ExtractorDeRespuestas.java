package com.mycompany.mavenproject1.procesamiento;

import com.mycompany.mavenproject1.util.GeneradorExamen;
import org.opencv.core.*;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * CLASE: ExtractorDeRespuestas
 * ============================================================
 * Detecta qué burbuja marcó el alumno usando:
 *
 * FÓRMULA DEL INGENIERO → Histograma / Conteo de Píxeles:
 *   Para i desde y hasta y+alto:
 *     Para j desde x hasta x+ancho:
 *       Si Img[i,j] == 255: contador += 1
 *
 * DECISIÓN → Función MAX (Apunte: MAX elige el más claro):
 *   La burbuja con más píxeles blancos = la que marcó el alumno.
 *
 * PRINCIPIO SRP: Esta clase SOLO extrae respuestas.
 *   No filtra ni decide quién pasó o reprobó.
 * ============================================================
 */
public class ExtractorDeRespuestas {

    // Umbral mínimo: si hay menos de este número de píxeles
    // blancos en una burbuja, se considera que NO fue marcada.
    private static final int UMBRAL_MINIMO_PIXELES = 30;

    /**
     * Extrae las respuestas de toda la hoja procesada.
     * Recorre cada pregunta y detecta cuál opción tiene más píxeles.
     *
     * @param imagenProcesada Imagen binarizada y limpia por FiltrosMatematicos
     * @return Mapa [Número de Pregunta -> Letra detectada (A/B/C/D)]
     */
    public Map<Integer, Character> extraerRespuestas(Mat imagenProcesada) {
        Map<Integer, Character> respuestasDetectadas = new HashMap<>();

        for (int p = 1; p <= GeneradorExamen.NUM_PREGUNTAS; p++) {
            char opcionDetectada = detectarOpcionMarcada(imagenProcesada, p);
            respuestasDetectadas.put(p, opcionDetectada);
        }

        return respuestasDetectadas;
    }

    /**
     * Detecta cuál de las 4 opciones (A, B, C, D) fue marcada
     * para una pregunta específica.
     *
     * APLICA: MAX(A, B, C, D) del apunte del Ingeniero.
     */
    private char detectarOpcionMarcada(Mat imagen, int numPregunta) {
        int maximoPixeles = 0;
        char mejorOpcion  = '?';

        for (int i = 0; i < GeneradorExamen.OPCIONES.length; i++) {
            // Obtener las coordenadas exactas de esta burbuja
            Rect coordBurbuja = GeneradorExamen.obtenerCoordBurbuja(numPregunta, i);

            // Validar que las coordenadas estén dentro de la imagen
            if (!coordenadaValida(coordBurbuja, imagen)) continue;

            // Extraer la sub-matriz de esa burbuja (ROI = Region of Interest)
            Mat roi = new Mat(imagen, coordBurbuja);

            // FÓRMULA DEL INGENIERO: Conteo explícito de píxeles blancos
            int sumaPixeles = contarPixelesBlancos(roi);

            // DECISIÓN MAX: Comparar con el actual máximo
            if (sumaPixeles > maximoPixeles) {
                maximoPixeles = sumaPixeles;
                mejorOpcion   = GeneradorExamen.OPCIONES[i];
            }
        }

        // Si ninguna burbuja superó el umbral mínimo => no marcó nada
        if (maximoPixeles < UMBRAL_MINIMO_PIXELES) {
            return '?';
        }

        return mejorOpcion;
    }

    /**
     * IMPLEMENTACIÓN DIRECTA DE LA FÓRMULA DEL INGENIERO:
     *
     * FUNCIÓN CalcularHistograma Local (solo cuenta el 255):
     *   contador ← 0
     *   Para i desde 0 hasta filas-1:
     *     Para j desde 0 hasta cols-1:
     *       Si Img[i,j] == 255: contador += 1
     *   Retornar contador
     */
    private int contarPixelesBlancos(Mat roi) {
        int contador = 0;

        for (int i = 0; i < roi.rows(); i++) {
            for (int j = 0; j < roi.cols(); j++) {
                double valor = roi.get(i, j)[0];
                if (valor == 255.0) {
                    contador++;
                }
            }
        }

        return contador;
    }

    /**
     * Valida que un rectángulo no salga de los bordes de la imagen.
     * Evita excepciones al extraer sub-matrices (ROI).
     */
    private boolean coordenadaValida(Rect rect, Mat imagen) {
        return rect.x >= 0
            && rect.y >= 0
            && rect.x + rect.width  <= imagen.cols()
            && rect.y + rect.height <= imagen.rows();
    }
}
