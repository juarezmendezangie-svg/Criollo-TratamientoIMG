package com.criollo.omr.procesamiento;

import com.criollo.omr.config.ConfiguracionExamen;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Extrae respuestas del examen usando detección dinámica de burbujas.
 * 
 * FLUJO:
 * 1. Recibe imagen procesada (binarizada y limpia)
 * 2. DetectorBurbujas → encuentra círculos automáticamente
 * 3. OrganizadorPreguntas → agrupa en preguntas y opciones
 * 4. Retorna Map<Integer, Character> con las respuestas detectadas
 * 
 * Ya NO depende de coordenadas fijas.
 * Funciona con imágenes reales de examenes.
 */
public class ExtractorDeRespuestas {

    private static final Logger log = LoggerFactory.getLogger(ExtractorDeRespuestas.class);
    private final DetectorBurbujas detectorBurbujas;
    private final OrganizadorPreguntas organizador;
    private final ConfiguracionExamen config;

    public ExtractorDeRespuestas() {
        this.detectorBurbujas = new DetectorBurbujas();
        this.organizador = new OrganizadorPreguntas();
        this.config = ConfiguracionExamen.getInstancia();
    }

    /**
     * Extrae las respuestas de la imagen procesada.
     * @param imagenBinaria imagen binarizada (para contornos)
     * @param imagenGrises imagen en escala de grises (para detectar relleno)
     * @return Map<NumeroPregunta, LetraDetectada>
     */
    public Map<Integer, Character> extraerRespuestas(Mat imagenBinaria, Mat imagenGrises) {
        return extraerRespuestas(imagenBinaria, imagenGrises, false);
    }

    public Map<Integer, Character> extraerRespuestas(Mat imagenBinaria, Mat imagenGrises, boolean esPlantilla) {
        if (imagenBinaria == null || imagenBinaria.empty()) {
            log.warn("Imagen procesada nula o vacía, no se pueden extraer respuestas");
            return java.util.Collections.emptyMap();
        }

        // Paso 1: Detectar burbujas dinámicamente (en imagen binaria)
        java.util.List<DetectorBurbujas.Burbuja> burbujas =
            detectorBurbujas.detectarBurbujas(imagenBinaria);

        log.info("Burbujas detectadas: {}", burbujas.size());

        // Paso 2: Organizar usando Otsu para grid + Adaptive para relleno + Grises
        Map<Integer, Character> respuestas = organizador.organizar(
            burbujas, imagenBinaria, imagenGrises, imagenBinaria.cols(), imagenBinaria.rows(), esPlantilla);

        log.info("Respuestas extraídas (esPlantilla={}): {}", esPlantilla, respuestas);
        return respuestas;
    }
}
