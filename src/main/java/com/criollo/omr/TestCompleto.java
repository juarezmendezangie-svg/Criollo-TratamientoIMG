package com.criollo.omr;

import nu.pattern.OpenCV;
import com.criollo.omr.controlador.ControladorOMR;
import com.criollo.omr.modelo.ResultadoCalificacion;

import java.io.File;

/**
 * Prueba completa con los 6 PNGs generados.
 * Procesa 100PR y 50PR y muestra precisión.
 */
public class TestCompleto {

    public static void main(String[] args) {
        OpenCV.loadLocally();
        ControladorOMR ctrl = new ControladorOMR();

        test("img/RESPUESTAS-100PR.png", "img/ALUMNO-100PR.png", "100PR", ctrl);
        test("img/RESPUESTAS-50PR.png", "img/ALUMNO-50PR.png", "50PR", ctrl);
    }

    private static void test(String plantillaPath, String alumnoPath, String label, ControladorOMR ctrl) {
        System.out.println("\n========== TEST " + label + " ==========");

        File fp = new File(plantillaPath);
        File fa = new File(alumnoPath);
        if (!fp.exists()) { System.out.println("ERROR: " + plantillaPath + " no existe"); return; }
        if (!fa.exists()) { System.out.println("ERROR: " + alumnoPath + " no existe"); return; }

        try {
            ResultadoCalificacion r = ctrl.calificar(plantillaPath, alumnoPath, "TEST-" + label);

            int total = r.getTotalPreguntas();
            int aciertos = r.getAciertos();
            int nota = r.getNotaFinal();

            System.out.printf("Preguntas detectadas en plantilla: %d%n", total);
            System.out.printf("Respuestas detectadas en alumno: %d%n", r.getRespuestasDetectadas().size());
            System.out.printf("Aciertos: %d/%d (%.0f%%)%n", aciertos, total, 100.0*aciertos/total);
            System.out.printf("Nota: %d/20%n", nota);

            // Mostrar primeras 5
            System.out.print("P01-P05: ");
            var detalles = r.getDetalle();
            for (int i = 0; i < Math.min(5, detalles.size()); i++) {
                var d = detalles.get(i);
                System.out.printf("P%d=%c/%c ", d.getNumeroPregunta(), d.getRespuestaCorrecta(), d.getRespuestaAlumno());
            }
            System.out.println();

            // Mostrar últimas 5
            if (detalles.size() > 5) {
                System.out.print("Últimas:  ");
                int start = detalles.size() - 5;
                for (int i = start; i < detalles.size(); i++) {
                    var d = detalles.get(i);
                    System.out.printf("P%d=%c/%c ", d.getNumeroPregunta(), d.getRespuestaCorrecta(), d.getRespuestaAlumno());
                }
                System.out.println();
            }

            // De las que el alumno respondió, ¿cuántas acertó?
            int respondidas = 0;
            int acertadas = 0;
            for (var d : detalles) {
                if (d.getRespuestaAlumno() != 'X') {
                    respondidas++;
                    if (d.isEsCorrecta()) acertadas++;
                }
            }
            if (respondidas > 0) {
                System.out.printf("Alumno respondió %d/%d, acertó %d/%d (%.0f%%)%n",
                    respondidas, total, acertadas, respondidas, 100.0*acertadas/respondidas);
            }

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
