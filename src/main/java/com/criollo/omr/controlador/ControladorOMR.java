package com.criollo.omr.controlador;

import com.criollo.omr.config.ConfiguracionExamen;
import com.criollo.omr.modelo.DetallePregunta;
import com.criollo.omr.modelo.PlantillaMaestra;
import com.criollo.omr.modelo.ResultadoCalificacion;
import com.criollo.omr.persistencia.CalificacionDAO;
import com.criollo.omr.procesamiento.*;
import com.criollo.omr.exportacion.ExportadorPDF;
import com.criollo.omr.exportacion.ExportadorCSV;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Controlador central del sistema OMR.
 * Coordina: detección → procesamiento → calificación → persistencia/exportación.
 */
public class ControladorOMR {

    private static final Logger log = LoggerFactory.getLogger(ControladorOMR.class);

    private final FiltrosMatematicos filtros;
    private final ExtractorDeRespuestas extractor;
    private final DetectorHoja detectorHoja;
    private final CorrectorPerspectiva correctorPerspectiva;
    private final PlantillaMaestra plantilla;
    private final CalificacionDAO calificacionDAO;
    private final ConfiguracionExamen config;

    public ControladorOMR() {
        this.filtros = new FiltrosMatematicos();
        this.extractor = new ExtractorDeRespuestas();
        this.detectorHoja = new DetectorHoja();
        this.correctorPerspectiva = new CorrectorPerspectiva();
        this.plantilla = new PlantillaMaestra();
        this.calificacionDAO = new CalificacionDAO();
        this.config = ConfiguracionExamen.getInstancia();
    }

    /**
     * Procesa la imagen del examen completo: detecta hoja, corrige perspectiva,
     * aplica filtros matemáticos y extrae respuestas.
     */
    public ResultadoCalificacion calificar(String rutaPlantilla, String rutaAlumno, String nombreAlumno) {
        ResultadoCalificacion resultado = new ResultadoCalificacion();
        resultado.setNombreAlumno(nombreAlumno);
        resultado.setRutaImagenPlantilla(rutaPlantilla);
        resultado.setRutaImagenAlumno(rutaAlumno);

        // 1. Procesar plantilla del profesor
        Mat imgPlantilla = Imgcodecs.imread(rutaPlantilla);
        if (imgPlantilla.empty()) {
            throw new RuntimeException("No se pudo cargar la imagen de la plantilla: " + rutaPlantilla);
        }

        try {
            Mat[] resultados = procesarImagenCompleta(imgPlantilla);
            Mat plantillaOtsu = resultados[0];       // Equalize+Otsu → contornos
            Mat plantillaCLAHE = resultados[1];      // CLAHE+Otsu → relleno (sin sesgo)
            Map<Integer, Character> respuestasPlantilla = extractor.extraerRespuestas(plantillaOtsu, plantillaCLAHE);
            plantillaOtsu.release();
            plantillaCLAHE.release();

            // Registrar respuestas correctas
            plantilla.setPlantillaCargada(false);
            plantilla.getRespuestasCorrectas().clear();
            respuestasPlantilla.forEach(plantilla::registrarRespuesta);
            plantilla.setPlantillaCargada(true);
            plantilla.setRutaImagenPlantilla(rutaPlantilla);

            resultado.setRespuestasCorrectas(new HashMap<>(plantilla.getRespuestasCorrectas()));
            resultado.setTotalPreguntas(plantilla.getTotalPreguntas());
        } finally {
            imgPlantilla.release();
        }

        // 2. Procesar examen del alumno
        Mat imgAlumno = Imgcodecs.imread(rutaAlumno);
        if (imgAlumno.empty()) {
            throw new RuntimeException("No se pudo cargar la imagen del alumno: " + rutaAlumno);
        }

        try {
            Mat[] resultados = procesarImagenCompleta(imgAlumno);
            Mat alumnoOtsu = resultados[0];
            Mat alumnoCLAHE = resultados[1];
            Map<Integer, Character> respuestasAlumno = extractor.extraerRespuestas(alumnoOtsu, alumnoCLAHE);
            alumnoOtsu.release();
            alumnoCLAHE.release();

            resultado.setRespuestasDetectadas(respuestasAlumno);

            // 3. Calificar
            int aciertos = plantilla.calcularAciertos(respuestasAlumno);
            int nota = plantilla.calcularNota(respuestasAlumno, config.getNotaMaxima());

            resultado.setAciertos(aciertos);
            resultado.setNotaFinal(nota);

            // 4. Generar detalle por pregunta
            List<DetallePregunta> detalle = new ArrayList<>();
            for (int i = 1; i <= plantilla.getTotalPreguntas(); i++) {
                char correcta = plantilla.getRespuestaCorrecta(i).orElse('X');
                char alumno = respuestasAlumno.getOrDefault(i, 'X');
                detalle.add(new DetallePregunta(i, correcta, alumno));
            }
            resultado.setDetalle(detalle);

            log.info("Calificación completada: {}/{} aciertos, nota {}/20",
                aciertos, plantilla.getTotalPreguntas(), nota);

        } finally {
            imgAlumno.release();
        }

        return resultado;
    }

    /**
     * Procesa una imagen completa y devuelve [binaria, grises].
     */
    private Mat[] procesarImagenCompleta(Mat imagenOriginal) {
        int wOrig = imagenOriginal.cols();
        int hOrig = imagenOriginal.rows();
        log.info("Imagen original: {}x{}px", wOrig, hOrig);

        // Intentar detectar y corregir perspectiva
        Point[] esquinas = detectorHoja.detectarHoja(imagenOriginal);

        if (esquinas != null) {
            // Calcular dimensiones que tendría la imagen corregida
            double w1 = Math.sqrt(Math.pow(esquinas[1].x-esquinas[0].x,2)+Math.pow(esquinas[1].y-esquinas[0].y,2));
            double w2 = Math.sqrt(Math.pow(esquinas[2].x-esquinas[3].x,2)+Math.pow(esquinas[2].y-esquinas[3].y,2));
            double h1 = Math.sqrt(Math.pow(esquinas[2].x-esquinas[1].x,2)+Math.pow(esquinas[2].y-esquinas[1].y,2));
            double h2 = Math.sqrt(Math.pow(esquinas[3].x-esquinas[0].x,2)+Math.pow(esquinas[3].y-esquinas[0].y,2));
            int estWidth = (int) Math.max(w1, w2);
            int estHeight = (int) Math.max(h1, h2);

            // Si el área corregida es <40% del área original, usar original
            double ratio = (estWidth * estHeight) / (double)(wOrig * hOrig);
            if (estWidth < 200 || estHeight < 200 || ratio < 0.40) {
                log.warn("Corrección inválida ({}x{}px, ratio={}). Usando imagen original ({}x{}px).",
                    estWidth, estHeight, String.format("%.2f", ratio), wOrig, hOrig);
                Mat copia = new Mat();
                imagenOriginal.copyTo(copia);
                return filtros.procesarExamen(copia);
            }

            log.info("Hoja detectada, corrigiendo perspectiva ({}x{}px → {}x{}px)",
                wOrig, hOrig, estWidth, estHeight);
            Mat imagenEnderezada = correctorPerspectiva.corregirPerspectiva(imagenOriginal, esquinas);
            try {
                return filtros.procesarExamen(imagenEnderezada);
            } finally {
                imagenEnderezada.release();
            }
        } else {
            log.info("No se detectó hoja, usando imagen original ({}x{}px)", wOrig, hOrig);
        }

        Mat copia = new Mat();
        imagenOriginal.copyTo(copia);
        return filtros.procesarExamen(copia);
    }

    /**
     * Guarda el resultado en la base de datos.
     */
    public void guardarEnHistorial(ResultadoCalificacion resultado) {
        calificacionDAO.guardar(resultado);
    }

    /**
     * Lista todas las calificaciones del historial.
     */
    public List<ResultadoCalificacion> obtenerHistorial() {
        return calificacionDAO.listarTodas();
    }

    /**
     * Busca calificaciones por nombre de alumno.
     */
    public List<ResultadoCalificacion> buscarPorAlumno(String nombre) {
        return calificacionDAO.buscarPorAlumno(nombre);
    }

    /**
     * Elimina una calificación del historial.
     */
    public void eliminarDelHistorial(int id) {
        calificacionDAO.eliminar(id);
    }

    /**
     * Exporta el resultado a PDF.
     */
    public void exportarPDF(ResultadoCalificacion resultado, File destino) throws IOException {
        new ExportadorPDF().exportar(resultado, destino);
    }

    /**
     * Exporta el resultado a CSV.
     */
    public void exportarCSV(ResultadoCalificacion resultado, File destino) throws IOException {
        new ExportadorCSV().exportar(resultado, destino);
    }

    public PlantillaMaestra getPlantilla() { return plantilla; }
    public boolean isPlantillaCargada() { return plantilla.isPlantillaCargada(); }
}
