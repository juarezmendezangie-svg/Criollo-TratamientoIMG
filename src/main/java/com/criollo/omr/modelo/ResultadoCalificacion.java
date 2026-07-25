package com.criollo.omr.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de datos completo para un resultado de calificación.
 */
public class ResultadoCalificacion {

    private int id;
    private String nombreAlumno;
    private String rutaImagenPlantilla;
    private String rutaImagenAlumno;
    private Map<Integer, Character> respuestasDetectadas;
    private Map<Integer, Character> respuestasCorrectas;
    private int aciertos;
    private int totalPreguntas;
    private int notaFinal;
    private LocalDateTime fechaCalificacion;
    private List<DetallePregunta> detalle;

    public ResultadoCalificacion() {
        this.respuestasDetectadas = new HashMap<>();
        this.respuestasCorrectas = new HashMap<>();
        this.detalle = new ArrayList<>();
        this.fechaCalificacion = LocalDateTime.now();
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreAlumno() { return nombreAlumno; }
    public void setNombreAlumno(String nombreAlumno) { this.nombreAlumno = nombreAlumno; }

    public String getRutaImagenPlantilla() { return rutaImagenPlantilla; }
    public void setRutaImagenPlantilla(String ruta) { this.rutaImagenPlantilla = ruta; }

    public String getRutaImagenAlumno() { return rutaImagenAlumno; }
    public void setRutaImagenAlumno(String ruta) { this.rutaImagenAlumno = ruta; }

    public Map<Integer, Character> getRespuestasDetectadas() { return respuestasDetectadas; }
    public void setRespuestasDetectadas(Map<Integer, Character> resp) { this.respuestasDetectadas = resp; }

    public Map<Integer, Character> getRespuestasCorrectas() { return respuestasCorrectas; }
    public void setRespuestasCorrectas(Map<Integer, Character> resp) { this.respuestasCorrectas = resp; }

    public int getAciertos() { return aciertos; }
    public void setAciertos(int aciertos) { this.aciertos = aciertos; }

    public int getTotalPreguntas() { return totalPreguntas; }
    public void setTotalPreguntas(int total) { this.totalPreguntas = total; }

    public int getNotaFinal() { return notaFinal; }
    public void setNotaFinal(int nota) { this.notaFinal = nota; }

    public LocalDateTime getFechaCalificacion() { return fechaCalificacion; }
    public void setFechaCalificacion(LocalDateTime fecha) { this.fechaCalificacion = fecha; }

    public List<DetallePregunta> getDetalle() { return detalle; }
    public void setDetalle(List<DetallePregunta> detalle) { this.detalle = detalle; }

    public void agregarDetalle(DetallePregunta dp) { this.detalle.add(dp); }

    public String getRespuestasDetectadasJSON() {
        StringBuilder sb = new StringBuilder("{");
        boolean primero = true;
        for (Map.Entry<Integer, Character> e : respuestasDetectadas.entrySet()) {
            if (!primero) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            primero = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
