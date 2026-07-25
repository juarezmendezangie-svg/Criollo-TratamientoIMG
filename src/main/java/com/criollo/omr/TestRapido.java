package com.criollo.omr;

import com.criollo.omr.config.ConfiguracionExamen;
import com.criollo.omr.modelo.PlantillaMaestra;
import com.criollo.omr.procesamiento.*;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.Map;

/**
 * Test rápido - solo procesamiento, sin UI.
 * Resultado: imprime respuestas detectadas y aciertos.
 */
public class TestRapido {

    public static void main(String[] args) {
        OpenCV.loadLocally();

        String plantillaPath = "img/RESPUESTAS-100PR.jpeg";
        String alumnoPath = "img/ALUMNO1-100PR.jpeg";

        FiltrosMatematicos filtros = new FiltrosMatematicos();
        ExtractorDeRespuestas extractor = new ExtractorDeRespuestas();
        DetectorHoja detectorHoja = new DetectorHoja();
        CorrectorPerspectiva corrector = new CorrectorPerspectiva();

        // Procesar plantilla
        System.out.println("=== PROCESANDO PLANTILLA ===");
        Map<Integer, Character> respPlantilla = procesar(plantillaPath, filtros, extractor, detectorHoja, corrector);
        System.out.println("Plantilla detectada: " + respPlantilla.size() + " preguntas");

        // Procesar alumno
        System.out.println("\n=== PROCESANDO ALUMNO ===");
        Map<Integer, Character> respAlumno = procesar(alumnoPath, filtros, extractor, detectorHoja, corrector);
        System.out.println("Alumno detectado: " + respAlumno.size() + " respuestas");

        // Comparar
        System.out.println("\n=== COMPARACIÓN ===");
        int aciertos = 0;
        int total = respPlantilla.size();
        for (int i = 1; i <= Math.min(total, 5); i++) {
            char p = respPlantilla.getOrDefault(i, '?');
            char a = respAlumno.getOrDefault(i, '?');
            boolean ok = p == a && p != '?';
            if (ok) aciertos++;
            System.out.printf("P%02d: Plant=%c Alum=%c %s%n", i, p, a, ok ? "OK" : "X");
        }
        System.out.println("...");
        for (int i = Math.max(6, total - 4); i <= total; i++) {
            char p = respPlantilla.getOrDefault(i, '?');
            char a = respAlumno.getOrDefault(i, '?');
            boolean ok = p == a && p != '?';
            if (ok) aciertos++;
            System.out.printf("P%02d: Plant=%c Alum=%c %s%n", i, p, a, ok ? "OK" : "X");
        }

        // Contar todos
        aciertos = 0;
        for (int i = 1; i <= total; i++) {
            char p = respPlantilla.getOrDefault(i, '?');
            char a = respAlumno.getOrDefault(i, '?');
            if (p != '?' && a != '?' && p == a) aciertos++;
        }
        System.out.printf("%nAciertos: %d/%d, Nota: %d/20%n", aciertos, total,
            (int)Math.round((double)aciertos/total*20));
    }

    private static Map<Integer, Character> procesar(String path,
            FiltrosMatematicos filtros, ExtractorDeRespuestas extractor,
            DetectorHoja detectorHoja, CorrectorPerspectiva corrector) {

        Mat img = Imgcodecs.imread(path);
        if (img.empty()) {
            System.err.println("ERROR: No se pudo cargar " + path);
            return Map.of();
        }

        Point[] esquinas = detectorHoja.detectarHoja(img);
        Mat enderezada;
        if (esquinas != null) {
            double w = Math.max(
                Math.sqrt(Math.pow(esquinas[1].x-esquinas[0].x,2)+Math.pow(esquinas[1].y-esquinas[0].y,2)),
                Math.sqrt(Math.pow(esquinas[2].x-esquinas[3].x,2)+Math.pow(esquinas[2].y-esquinas[3].y,2)));
            double h = Math.max(
                Math.sqrt(Math.pow(esquinas[2].x-esquinas[1].x,2)+Math.pow(esquinas[2].y-esquinas[1].y,2)),
                Math.sqrt(Math.pow(esquinas[3].x-esquinas[0].x,2)+Math.pow(esquinas[3].y-esquinas[0].y,2)));
            if (w < 200 || h < 200) {
                enderezada = new Mat();
                img.copyTo(enderezada);
            } else {
                enderezada = corrector.corregirPerspectiva(img, esquinas);
            }
        } else {
            enderezada = new Mat();
            img.copyTo(enderezada);
        }

        try {
            Mat[] resultados = filtros.procesarExamen(enderezada);
            Mat procesada = resultados[0];
            Mat grises = resultados[1];
            try {
                return extractor.extraerRespuestas(procesada, grises);
            } finally {
                procesada.release();
                grises.release();
            }
        } finally {
            enderezada.release();
            img.release();
        }
    }
}
