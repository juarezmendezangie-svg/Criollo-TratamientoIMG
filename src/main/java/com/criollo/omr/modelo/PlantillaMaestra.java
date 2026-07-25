package com.criollo.omr.modelo;

import java.util.*;

/**
 * Almacena las respuestas correctas de la plantilla del profesor.
 */
public class PlantillaMaestra {

    private final Map<Integer, Character> respuestasCorrectas;
    private String rutaImagenPlantilla;
    private boolean plantillaCargada;

    public PlantillaMaestra() {
        this.respuestasCorrectas = new HashMap<>();
        this.plantillaCargada = false;
    }

    public void registrarRespuesta(int numeroPregunta, char opcion) {
        respuestasCorrectas.put(numeroPregunta, opcion);
    }

    public Optional<Character> getRespuestaCorrecta(int numeroPregunta) {
        return Optional.ofNullable(respuestasCorrectas.get(numeroPregunta));
    }

    public int calcularAciertos(Map<Integer, Character> respuestasAlumno) {
        int aciertos = 0;
        for (Map.Entry<Integer, Character> entry : respuestasCorrectas.entrySet()) {
            char delAlumno = respuestasAlumno.getOrDefault(entry.getKey(), 'X');
            if (entry.getValue() == delAlumno) {
                aciertos++;
            }
        }
        return aciertos;
    }

    public int calcularNota(Map<Integer, Character> respuestasAlumno, int notaMaxima) {
        int total = respuestasCorrectas.size();
        if (total == 0) return 0;
        int aciertos = calcularAciertos(respuestasAlumno);
        return (int) Math.round(((double) aciertos / total) * notaMaxima);
    }

    public String generarReporteDetallado(Map<Integer, Character> respuestasAlumno, int notaMaxima) {
        StringBuilder reporte = new StringBuilder();
        reporte.append("===== REPORTE DE CALIFICACIÓN =====\n\n");
        reporte.append(String.format("%-12s %-10s %-10s %-10s%n",
            "Pregunta", "Correcta", "Alumno", "Estado"));
        reporte.append("-".repeat(45)).append("\n");

        int aciertos = 0;
        List<Integer> orden = new ArrayList<>(respuestasCorrectas.keySet());
        Collections.sort(orden);

        for (int numPregunta : orden) {
            char correcta = respuestasCorrectas.get(numPregunta);
            char alumno = respuestasAlumno.getOrDefault(numPregunta, 'X');
            boolean esCorrecta = (correcta == alumno);
            if (esCorrecta) aciertos++;

            reporte.append(String.format("  P%02d         %c           %c           %s%n",
                numPregunta, correcta, alumno, esCorrecta ? "✓" : "✗"));
        }

        int nota = calcularNota(respuestasAlumno, notaMaxima);
        reporte.append("-".repeat(45)).append("\n");
        reporte.append(String.format("Aciertos: %d / %d%n", aciertos, respuestasCorrectas.size()));
        reporte.append(String.format("NOTA FINAL: %d / %d%n", nota, notaMaxima));
        return reporte.toString();
    }

    public boolean isPlantillaCargada() { return plantillaCargada; }
    public void setPlantillaCargada(boolean cargada) { this.plantillaCargada = cargada; }
    public String getRutaImagenPlantilla() { return rutaImagenPlantilla; }
    public void setRutaImagenPlantilla(String ruta) { this.rutaImagenPlantilla = ruta; }
    public Map<Integer, Character> getRespuestasCorrectas() { return respuestasCorrectas; }
    public int getTotalPreguntas() { return respuestasCorrectas.size(); }
}
