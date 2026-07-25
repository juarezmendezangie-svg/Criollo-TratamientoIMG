package com.criollo.omr;

import com.criollo.omr.controlador.ControladorOMR;
import com.criollo.omr.modelo.ResultadoCalificacion;
import nu.pattern.OpenCV;

import java.io.File;

/**
 * Prueba automatizada del sistema OMR con imágenes reales.
 * Procesa las imágenes en /img/ y muestra resultados detallados.
 *
 * Uso: Ejecutar esta clase directamente (no requiere UI).
 * Imprime un reporte completo de detección en consola.
 */
public class TestOMR {

    private static final String IMG_DIR = "img/";

    public static void main(String[] args) {
        OpenCV.loadLocally();
        ControladorOMR controlador = new ControladorOMR();

        String[] pares = {
            "RESPUESTAS-100PR.jpeg,ALUMNO1-100PR.jpeg",
        };

        for (String par : pares) {
            String[] nombres = par.split(",");
            String plantilla = IMG_DIR + nombres[0];
            String alumno = IMG_DIR + nombres[1];

            File fPlantilla = new File(plantilla);
            File fAlumno = new File(alumno);

            if (!fPlantilla.exists()) {
                System.err.println("NO EXISTE: " + plantilla);
                continue;
            }
            if (!fAlumno.exists()) {
                System.err.println("NO EXISTE: " + alumno);
                continue;
            }

            System.out.println("\n============================================");
            System.out.println("TEST: " + nombres[0] + " vs " + nombres[1]);
            System.out.println("============================================");

            try {
                // Procesar plantilla (solo para cargar respuestas correctas)
                System.out.println("\n--- PROCESANDO PLANTILLA ---");
                ResultadoCalificacion r = controlador.calificar(plantilla, alumno, "TEST");

                System.out.println("\n--- RESULTADOS ---");
                System.out.println("Plantilla: " + r.getTotalPreguntas() + " preguntas detectadas");
                System.out.println("Alumno: " + r.getRespuestasDetectadas().size() + " respuestas detectadas");
                System.out.println("Aciertos: " + r.getAciertos() + "/" + r.getTotalPreguntas());
                System.out.println("Nota: " + r.getNotaFinal() + "/20");

                // Mostrar primeras y últimas 10 preguntas para ver alineamiento
                System.out.println("\n--- PRIMERAS 10 PREGUNTAS (alineamiento) ---");
                var det = r.getDetalle();
                int max = Math.min(10, det.size());
                for (int i = 0; i < max; i++) {
                    var d = det.get(i);
                    System.out.printf("  P%02d  Plant=%c  Alum=%c  %s%n",
                        d.getNumeroPregunta(), d.getRespuestaCorrecta(),
                        d.getRespuestaAlumno(), d.isEsCorrecta() ? "OK" : "X");
                }

                System.out.println("\n--- ÚLTIMAS 10 PREGUNTAS ---");
                int start = Math.max(0, det.size() - 10);
                for (int i = start; i < det.size(); i++) {
                    var d = det.get(i);
                    System.out.printf("  P%02d  Plant=%c  Alum=%c  %s%n",
                        d.getNumeroPregunta(), d.getRespuestaCorrecta(),
                        d.getRespuestaAlumno(), d.isEsCorrecta() ? "OK" : "X");
                }

                // Mostrar respuestas de la plantilla vs alumno completas
                System.out.println("\n--- MAPA COMPLETO (Plantilla vs Alumno) ---");
                var respPlantilla = r.getRespuestasCorrectas();
                var respAlumno = r.getRespuestasDetectadas();
                int tp = r.getTotalPreguntas();

                System.out.print("Plantilla: ");
                for (int i = 1; i <= tp; i++) {
                    System.out.print(String.format("P%02d=%c ", i, respPlantilla.getOrDefault(i, '?')));
                    if (i % 10 == 0) System.out.println("          ");
                }
                System.out.println();

                System.out.print("Alumno:    ");
                for (int i = 1; i <= tp; i++) {
                    System.out.print(String.format("P%02d=%c ", i, respAlumno.getOrDefault(i, '?')));
                    if (i % 10 == 0) System.out.println("          ");
                }
                System.out.println();

                // Contar aciertos manualmente para verificar
                System.out.println("\n--- VERIFICACIÓN MANUAL DE ACIERTOS ---");
                int aciertos = 0;
                int totalComparables = 0;
                for (int i = 1; i <= tp; i++) {
                    char plant = respPlantilla.getOrDefault(i, '?');
                    char alum = respAlumno.getOrDefault(i, '?');
                    if (plant != '?' && alum != '?') {
                        totalComparables++;
                        if (plant == alum) aciertos++;
                    }
                }
                System.out.println("Comparables: " + totalComparables + "/" + tp);
                System.out.println("Aciertos manuales: " + aciertos + "/" + totalComparables);
                System.out.println("Nota manual: " + Math.round((double)aciertos/tp*20) + "/20");

            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
