package com.criollo.omr.exportacion;

import com.criollo.omr.modelo.DetallePregunta;
import com.criollo.omr.modelo.ResultadoCalificacion;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Exporta resultados de calificación a PDF usando Apache PDFBox 3.x.
 */
public class ExportadorPDF {

    private static final Logger log = LoggerFactory.getLogger(ExportadorPDF.class);

    private static PDColor color(int r, int g, int b) {
        return new PDColor(new float[]{r / 255f, g / 255f, b / 255f}, PDDeviceRGB.INSTANCE);
    }

    public void exportar(ResultadoCalificacion resultado, File destino) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);

            PDPageContentStream cs = new PDPageContentStream(doc, pagina);
            PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fuenteNegrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float y = 750;
            float margenIzq = 50;

            // Título
            cs.beginText();
            cs.setFont(fuenteNegrita, 18);
            cs.newLineAtOffset(margenIzq, y);
            cs.showText("Reporte de Calificación OMR");
            cs.endText();
            y -= 30;

            // Info del alumno
            cs.beginText();
            cs.setFont(fuente, 12);
            cs.newLineAtOffset(margenIzq, y);
            cs.showText("Alumno: " + (resultado.getNombreAlumno() != null ? resultado.getNombreAlumno() : "N/A"));
            cs.endText();
            y -= 20;

            cs.beginText();
            cs.newLineAtOffset(margenIzq, y);
            cs.showText("Fecha: " + resultado.getFechaCalificacion());
            cs.endText();
            y -= 20;

            cs.beginText();
            cs.newLineAtOffset(margenIzq, y);
            cs.showText("Nota: " + resultado.getNotaFinal() + " / 20  |  Aciertos: " +
                resultado.getAciertos() + " / " + resultado.getTotalPreguntas());
            cs.endText();
            y -= 30;

            // Tabla de detalles
            float anchoTabla = 300;
            float altoFila = 18;
            float[] anchosColumnas = {60, 80, 80, 80};

            // Encabezado de tabla
            cs.setNonStrokingColor(color(41, 128, 185));
            cs.addRect(margenIzq, y - altoFila, anchoTabla, altoFila);
            cs.fill();

            cs.setNonStrokingColor(color(255, 255, 255));
            cs.beginText();
            cs.setFont(fuenteNegrita, 10);
            float x = margenIzq + 10;
            cs.newLineAtOffset(x, y - 13);
            cs.showText("Pregunta");
            cs.newLineAtOffset(anchosColumnas[0], 0);
            cs.showText("Correcta");
            cs.newLineAtOffset(anchosColumnas[1], 0);
            cs.showText("Alumno");
            cs.newLineAtOffset(anchosColumnas[2], 0);
            cs.showText("Estado");
            cs.endText();
            y -= altoFila;

            // Filas de datos
            for (DetallePregunta det : resultado.getDetalle()) {
                if (y < 50) {
                    cs.close();
                    pagina = new PDPage();
                    doc.addPage(pagina);
                    cs = new PDPageContentStream(doc, pagina);
                    y = 750;
                }

                // Fondo alternado
                if (det.getNumeroPregunta() % 2 == 0) {
                    cs.setNonStrokingColor(color(240, 240, 240));
                    cs.addRect(margenIzq, y - altoFila, anchoTabla, altoFila);
                    cs.fill();
                }

                cs.setNonStrokingColor(color(0, 0, 0));
                cs.beginText();
                cs.setFont(fuente, 10);
                cs.newLineAtOffset(margenIzq + 10, y - 13);
                cs.showText("P" + String.format("%02d", det.getNumeroPregunta()));
                cs.newLineAtOffset(anchosColumnas[0], 0);
                cs.showText(String.valueOf(det.getRespuestaCorrecta()));
                cs.newLineAtOffset(anchosColumnas[1], 0);
                cs.showText(String.valueOf(det.getRespuestaAlumno()));
                cs.newLineAtOffset(anchosColumnas[2], 0);
                cs.showText(det.isEsCorrecta() ? "Correcto" : "Incorrecto");
                cs.endText();
                y -= altoFila;
            }

            cs.close();
            doc.save(destino);
            log.info("PDF exportado: {}", destino.getAbsolutePath());
        }
    }
}
