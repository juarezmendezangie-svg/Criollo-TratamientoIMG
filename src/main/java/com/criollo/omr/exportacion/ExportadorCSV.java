package com.criollo.omr.exportacion;

import com.criollo.omr.modelo.DetallePregunta;
import com.criollo.omr.modelo.ResultadoCalificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Exporta resultados de calificación a CSV (compatible con Excel).
 */
public class ExportadorCSV {

    private static final Logger log = LoggerFactory.getLogger(ExportadorCSV.class);

    public void exportar(ResultadoCalificacion resultado, File destino) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(destino), StandardCharsets.UTF_8))) {

            // BOM para Excel
            writer.write('\uFEFF');

            // Headers
            writer.write("Pregunta,Correcta,Alumno,Estado");
            writer.newLine();

            // Datos
            for (DetallePregunta det : resultado.getDetalle()) {
                writer.write(String.format("%d,%c,%c,%s",
                    det.getNumeroPregunta(),
                    det.getRespuestaCorrecta(),
                    det.getRespuestaAlumno(),
                    det.isEsCorrecta() ? "Correcto" : "Incorrecto"));
                writer.newLine();
            }

            // Resumen
            writer.newLine();
            writer.write(String.format("Total Preguntas,%d", resultado.getTotalPreguntas()));
            writer.newLine();
            writer.write(String.format("Aciertos,%d", resultado.getAciertos()));
            writer.newLine();
            writer.write(String.format("Nota Final,%d/20", resultado.getNotaFinal()));
            writer.newLine();
            writer.write(String.format("Fecha,%s", resultado.getFechaCalificacion()));
            writer.newLine();

            log.info("CSV exportado: {}", destino.getAbsolutePath());
        }
    }
}
