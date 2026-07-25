package com.mycompany.mavenproject1.util;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.util.Map;
import java.util.HashMap;

/**
 * ============================================================
 * CLASE UTILITARIA: GeneradorExamen
 * ============================================================
 * PROPÓSITO: Genera imágenes de examen OMR de prueba usando
 * transformaciones geométricas y operaciones de dibujo.
 *
 * ESTO RESUELVE el problema de no tener fotos reales:
 * El sistema genera el examen Y sabe dónde están las burbujas
 * porque las dibujó él mismo en coordenadas conocidas.
 *
 * ADEMÁS demuestra dominio de:
 *   - Operaciones Geométricas (líneas, rectángulos, círculos)
 *   - Escritura de texto sobre matrices de píxeles
 *   - Creación de imágenes desde cero (matriz de ceros)
 * ============================================================
 */
public class GeneradorExamen {

    // Dimensiones estándar del examen (en píxeles)
    public static final int ANCHO_EXAMEN    = 600;
    public static final int ALTO_EXAMEN     = 700;

    // Margen de la hoja
    public static final int MARGEN_LEFT     = 80;
    public static final int MARGEN_TOP      = 150;

    // Espaciado entre preguntas y opciones
    public static final int SALTO_VERTICAL  = 60;
    public static final int SALTO_HORIZONTAL = 55;

    // Tamaño de cada burbuja circular
    public static final int RADIO_BURBUJA   = 12;

    // Número de preguntas y opciones
    public static final int NUM_PREGUNTAS   = 5;
    public static final char[] OPCIONES     = {'A', 'B', 'C', 'D'};

    /**
     * Genera un examen como plantilla (respuestas correctas marcadas).
     * @param respuestasCorrectas Mapa [pregunta -> opcion correcta]
     * @param rutaDestino Ruta donde guardar la imagen generada
     */
    public static void generarPlantilla(Map<Integer, Character> respuestasCorrectas,
                                         String rutaDestino) {
        Mat examen = crearHojaBase("PLANTILLA MAESTRA - PROFESOR");
        marcarRespuestas(examen, respuestasCorrectas, true);
        Imgcodecs.imwrite(rutaDestino, examen);
        System.out.println("Plantilla generada en: " + rutaDestino);
    }

    /**
     * Genera el examen de un alumno con sus respuestas marcadas.
     * @param respuestasAlumno Mapa [pregunta -> opcion marcada]
     * @param rutaDestino Ruta donde guardar la imagen generada
     */
    public static void generarExamenAlumno(Map<Integer, Character> respuestasAlumno,
                                            String rutaDestino) {
        Mat examen = crearHojaBase("EXAMEN DEL ALUMNO");
        marcarRespuestas(examen, respuestasAlumno, false);
        Imgcodecs.imwrite(rutaDestino, examen);
        System.out.println("Examen del alumno generado en: " + rutaDestino);
    }

    /**
     * Crea la hoja base del examen (fondo blanco, líneas, encabezado).
     * Esto aplica:
     *   - CrearImagen(N, M) con ceros -> luego llenar de blanco (255)
     *   - Dibujo de líneas y texto sobre la matriz
     */
    private static Mat crearHojaBase(String encabezado) {
        // CrearImagen(N, M) con fondo BLANCO (255) — apunte: CrearImagen con ceros, luego invertimos
        Mat hoja = new Mat(ALTO_EXAMEN, ANCHO_EXAMEN, CvType.CV_8UC1, new Scalar(255));

        // Borde de la hoja
        Imgproc.rectangle(hoja,
            new Point(20, 20),
            new Point(ANCHO_EXAMEN - 20, ALTO_EXAMEN - 20),
            new Scalar(0), 2);

        // Encabezado del examen
        Imgproc.putText(hoja, "Sistema OMR - Tratamiento de Imagenes",
            new Point(40, 55), Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0), 2);

        Imgproc.putText(hoja, encabezado,
            new Point(40, 90), Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(0), 1);

        // Línea separadora bajo el encabezado
        Imgproc.line(hoja,
            new Point(30, 110),
            new Point(ANCHO_EXAMEN - 30, 110),
            new Scalar(0), 1);

        // Encabezado de columnas (A, B, C, D)
        Imgproc.putText(hoja, "Preg", new Point(MARGEN_LEFT - 60, MARGEN_TOP - 20),
            Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0), 1);

        for (int i = 0; i < OPCIONES.length; i++) {
            int x = MARGEN_LEFT + (i * SALTO_HORIZONTAL);
            Imgproc.putText(hoja, String.valueOf(OPCIONES[i]),
                new Point(x + 5, MARGEN_TOP - 20),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(0), 2);
        }

        // Dibujar preguntas con sus 4 burbujas vacías
        for (int p = 1; p <= NUM_PREGUNTAS; p++) {
            int y = MARGEN_TOP + ((p - 1) * SALTO_VERTICAL);

            // Número de pregunta
            Imgproc.putText(hoja, "P" + String.format("%02d", p),
                new Point(MARGEN_LEFT - 60, y + 5),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0), 1);

            // 4 burbujas vacías (círculos)
            for (int i = 0; i < OPCIONES.length; i++) {
                int x = MARGEN_LEFT + (i * SALTO_HORIZONTAL) + RADIO_BURBUJA;
                Imgproc.circle(hoja, new Point(x, y), RADIO_BURBUJA, new Scalar(0), 1);
            }
        }

        return hoja;
    }

    /**
     * Rellena las burbujas correspondientes a las respuestas del mapa.
     * Una burbuja rellena = círculo negro sólido en la imagen.
     */
    private static void marcarRespuestas(Mat hoja,
                                          Map<Integer, Character> respuestas,
                                          boolean esPlantilla) {
        for (Map.Entry<Integer, Character> entrada : respuestas.entrySet()) {
            int pregunta = entrada.getKey();
            char opcion  = entrada.getValue();

            // Encontrar índice de la opción (A=0, B=1, C=2, D=3)
            int indice = -1;
            for (int i = 0; i < OPCIONES.length; i++) {
                if (OPCIONES[i] == opcion) { indice = i; break; }
            }
            if (indice < 0) continue;

            // Calcular coordenadas exactas del centro de la burbuja
            int x = MARGEN_LEFT + (indice * SALTO_HORIZONTAL) + RADIO_BURBUJA;
            int y = MARGEN_TOP  + ((pregunta - 1) * SALTO_VERTICAL);

            // Rellenar la burbuja con negro (trazo de lápiz simulado)
            int grosor = esPlantilla ? -1 : -1; // -1 = relleno sólido
            Imgproc.circle(hoja, new Point(x, y), RADIO_BURBUJA - 2, new Scalar(0), grosor);
        }
    }

    /**
     * Retorna las coordenadas (Rect) de una burbuja específica.
     * Usado por ExtractorDeRespuestas para saber EXACTAMENTE dónde mirar.
     * @param pregunta Número de pregunta (1..NUM_PREGUNTAS)
     * @param indiceOpcion Índice de la opción (0=A, 1=B, 2=C, 3=D)
     */
    public static Rect obtenerCoordBurbuja(int pregunta, int indiceOpcion) {
        int cx = MARGEN_LEFT + (indiceOpcion * SALTO_HORIZONTAL) + RADIO_BURBUJA;
        int cy = MARGEN_TOP  + ((pregunta - 1) * SALTO_VERTICAL);

        // El Rect abarca el cuadrado que rodea al círculo
        int x = cx - RADIO_BURBUJA;
        int y = cy - RADIO_BURBUJA;
        int lado = RADIO_BURBUJA * 2;

        return new Rect(x, y, lado, lado);
    }

    /**
     * Genera un set de prueba completo (plantilla + examen de alumno).
     * Útil para demostración sin necesitar fotos reales.
     */
    public static String[] generarSetDePrueba(String carpetaDestino) {
        // Respuestas correctas del profesor
        Map<Integer, Character> respuestasProfesor = new HashMap<>();
        respuestasProfesor.put(1, 'A');
        respuestasProfesor.put(2, 'C');
        respuestasProfesor.put(3, 'B');
        respuestasProfesor.put(4, 'D');
        respuestasProfesor.put(5, 'A');

        // Respuestas del alumno (tiene 3 correctas de 5)
        Map<Integer, Character> respuestasAlumno = new HashMap<>();
        respuestasAlumno.put(1, 'A'); // ✓
        respuestasAlumno.put(2, 'B'); // ✗
        respuestasAlumno.put(3, 'B'); // ✓
        respuestasAlumno.put(4, 'D'); // ✓
        respuestasAlumno.put(5, 'C'); // ✗

        String rutaPlantilla = carpetaDestino + "/plantilla_profesor.png";
        String rutaAlumno    = carpetaDestino + "/examen_alumno.png";

        generarPlantilla(respuestasProfesor, rutaPlantilla);
        generarExamenAlumno(respuestasAlumno, rutaAlumno);

        return new String[]{rutaPlantilla, rutaAlumno};
    }
}
