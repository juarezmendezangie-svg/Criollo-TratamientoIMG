package com.mycompany.mavenproject1.modelo;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * CLASE: PlantillaMaestra (Capa Modelo - Patrón MVC)
 * ============================================================
 * Responsabilidad: Almacena y gestiona las respuestas correctas
 * extraídas de la foto original del profesor.
 * 
 * Esto es el "Método B" que el Ingeniero verá:
 * No hay respuestas escritas en el código (hardcodeadas).
 * El programa las "aprende" leyendo la foto de la plantilla.
 * ============================================================
 */
public class PlantillaMaestra {

    // Mapa de [Número de Pregunta -> Respuesta Correcta (A/B/C/D)]
    private final Map<Integer, Character> respuestasCorrectas;
    
    // Nombre del archivo de imagen de la plantilla
    private String rutaImagenPlantilla;
    
    // Estado: ¿Ya fue procesada la plantilla?
    private boolean plantillaCargada;

    // ==================== CONSTRUCTOR ====================
    public PlantillaMaestra() {
        this.respuestasCorrectas = new HashMap<>();
        this.plantillaCargada = false;
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    /**
     * Registra una respuesta correcta detectada de la plantilla.
     * @param numeroPregunta - Número de la pregunta (1, 2, 3...)
     * @param opcion - Letra detectada (A, B, C o D)
     */
    public void registrarRespuesta(int numeroPregunta, char opcion) {
        respuestasCorrectas.put(numeroPregunta, opcion);
    }

    /**
     * Compara las respuestas del alumno contra las correctas
     * y calcula la nota sobre 20.
     * 
     * @param respuestasAlumno - Mapa con respuestas detectadas del alumno
     * @return Nota calculada sobre 20
     */
    public int calcularNota(Map<Integer, Character> respuestasAlumno) {
        int aciertos = 0;
        int totalPreguntas = respuestasCorrectas.size();

        for (Integer numPregunta : respuestasCorrectas.keySet()) {
            char correcta = respuestasCorrectas.get(numPregunta);
            char delAlumno = respuestasAlumno.getOrDefault(numPregunta, 'X');

            if (correcta == delAlumno) {
                aciertos++;
            }
        }

        // Fórmula de nota: (aciertos / totalPreguntas) * 20
        if (totalPreguntas == 0) return 0;
        return (int) Math.round(((double) aciertos / totalPreguntas) * 20);
    }

    /**
     * Retorna un resumen detallado para mostrar en la interfaz.
     * @param respuestasAlumno - Las respuestas del alumno a evaluar
     * @return Texto completo con el detalle de calificación
     */
    public String generarReporteDetallado(Map<Integer, Character> respuestasAlumno) {
        StringBuilder reporte = new StringBuilder();
        reporte.append("===== REPORTE DE CALIFICACIÓN =====\n\n");
        reporte.append(String.format("%-12s %-10s %-10s %-10s%n",
                "Pregunta", "Correcta", "Alumno", "Estado"));
        reporte.append("-".repeat(45)).append("\n");

        int aciertos = 0;
        for (Map.Entry<Integer, Character> entrada : respuestasCorrectas.entrySet()) {
            int numPregunta = entrada.getKey();
            char correcta = entrada.getValue();
            char alumno = respuestasAlumno.getOrDefault(numPregunta, '?');
            boolean esCorrecta = (correcta == alumno);
            if (esCorrecta) aciertos++;

            reporte.append(String.format("  P%02d         %c           %c           %s%n",
                    numPregunta, correcta, alumno, esCorrecta ? "✓" : "✗"));
        }

        int nota = calcularNota(respuestasAlumno);
        reporte.append("-".repeat(45)).append("\n");
        reporte.append(String.format("Aciertos: %d / %d%n", aciertos, respuestasCorrectas.size()));
        reporte.append(String.format("NOTA FINAL: %d / 20%n", nota));
        reporte.append("===================================");
        return reporte.toString();
    }

    // ==================== GETTERS Y SETTERS ====================
    public boolean isPlantillaCargada() { return plantillaCargada; }
    public void setPlantillaCargada(boolean cargada) { this.plantillaCargada = cargada; }
    public String getRutaImagenPlantilla() { return rutaImagenPlantilla; }
    public void setRutaImagenPlantilla(String ruta) { this.rutaImagenPlantilla = ruta; }
    public Map<Integer, Character> getRespuestasCorrectas() { return respuestasCorrectas; }
    public int getTotalPreguntas() { return respuestasCorrectas.size(); }
}
