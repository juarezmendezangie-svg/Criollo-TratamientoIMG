package com.criollo.omr;

import nu.pattern.OpenCV;
import com.criollo.omr.modelo.ResultadoCalificacion;
import com.criollo.omr.controlador.ControladorOMR;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Compara las respuestas detectadas por el sistema contra el GROUND TRUTH
 * definido en los archivos .md de la carpeta img/.
 */
public class TestGroundTruth {

    public static void main(String[] args) throws Exception {
        OpenCV.loadLocally();
        ControladorOMR ctrl = new ControladorOMR();

        test("img/RESPUESTAS-100PR.png", "img/ALUMNO-100PR.png",
             "img/RESPUESTAS-100PR.md", "100PR", ctrl);
        System.out.println();
        test("img/RESPUESTAS-50PR.png", "img/ALUMNO-50PR.png",
             "img/RESPUESTAS-50PR..md", "50PR", ctrl);
    }

    private static void test(String plantillaPath, String alumnoPath,
            String truthPath, String label, ControladorOMR ctrl) throws Exception {

        System.out.println("========== " + label + " vs GROUND TRUTH ==========");

        // Cargar ground truth
        Map<Integer, Character> truth = new HashMap<>();
        List<String> lines = Files.readAllLines(Path.of(truthPath));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty() && line.length() == 1) {
                truth.put(i + 1, line.charAt(0));
            }
        }
        System.out.println("Ground truth: " + truth.size() + " respuestas");

        // Procesar imágenes
        ResultadoCalificacion r = ctrl.calificar(plantillaPath, alumnoPath, "TEST-" + label);

        // Comparar sistema vs ground truth
        Map<Integer, Character> sysPlantilla = r.getRespuestasCorrectas();
        Map<Integer, Character> sysAlumno = r.getRespuestasDetectadas();

        System.out.println("Sistema detectó: " + sysPlantilla.size() + " preguntas en plantilla");
        System.out.println("Sistema detectó: " + sysAlumno.size() + " respuestas en alumno");

        // ¿Cuántas preguntas del ground truth detectó el sistema?
        int detectadas = 0;
        int correctasPlantilla = 0;
        for (int i = 1; i <= truth.size(); i++) {
            if (sysPlantilla.containsKey(i)) {
                detectadas++;
                if (sysPlantilla.get(i) == truth.get(i)) {
                    correctasPlantilla++;
                }
            }
        }

        System.out.printf("Plantilla: %d/%d preguntas detectadas, %d/%d correctas vs ground truth (%.0f%%)%n",
            detectadas, truth.size(), correctasPlantilla, detectadas,
            detectadas > 0 ? 100.0 * correctasPlantilla / detectadas : 0);

        // Mostrar primeras 10 discrepancias
        System.out.println("\nPrimeras discrepancias (Sistema vs Ground Truth):");
        int shown = 0;
        for (int i = 1; i <= truth.size() && shown < 10; i++) {
            char s = sysPlantilla.getOrDefault(i, '?');
            char t = truth.get(i);
            if (s != t) {
                System.out.printf("  P%02d: Sistema=%c, GroundTruth=%c %s%n",
                    i, s, t, s == '?' ? "(NO DETECTADO)" : "");
                shown++;
            }
        }

        // También comparar alumno vs ground truth (del alumno no tenemos .md, así que comparamos
        // las respuestas del alumno contra la plantilla del sistema)
        int aciertos = 0;
        int respondidas = 0;
        for (int i = 1; i <= truth.size(); i++) {
            char a = sysAlumno.getOrDefault(i, 'X');
            if (a != 'X') respondidas++;
            if (sysPlantilla.containsKey(i) && a == sysPlantilla.get(i)) aciertos++;
        }
        System.out.printf("%nAlumno: %d/%d respondidas, %d/%d aciertos (%.0f%%)%n",
            respondidas, truth.size(), aciertos, truth.size(),
            100.0 * aciertos / truth.size());
    }
}
