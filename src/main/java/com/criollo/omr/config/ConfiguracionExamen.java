package com.criollo.omr.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuración centralizada del sistema OMR.
 * Lee parámetros de config.properties con valores por defecto.
 */
public class ConfiguracionExamen {

    private static ConfiguracionExamen instancia;
    private final Properties props;

    private ConfiguracionExamen() {
        props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
                System.out.println("[Config] config.properties CARGADO correctamente");
            } else {
                System.err.println("[Config] config.properties NO ENCONTRADO - usando defaults de Java");
            }
        } catch (IOException e) {
            System.err.println("[Config] Error leyendo config.properties: " + e.getMessage());
        }
        // Debug: mostrar valores cargados
        System.out.println("[Config] area.min=" + getAreaMinima());
        System.out.println("[Config] area.max=" + getAreaMaxima());
        System.out.println("[Config] circularidad.min=" + getCircularidadMinima());
        System.out.println("[Config] relleno.min=" + getRellenoMinimo());
        System.out.println("[Config] opciones.por.pregunta=" + getOpcionesPorPregunta());
        System.out.println("[Config] max.preguntas=" + getMaxPreguntas());
    }

    public static synchronized ConfiguracionExamen getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracionExamen();
        }
        return instancia;
    }

    // --- DB ---
    public String getDbUrl() { return props.getProperty("db.url", "jdbc:h2:./data/omr_examenes;AUTO_SERVER=TRUE"); }
    public String getDbUser() { return props.getProperty("db.user", "sa"); }
    public String getDbPassword() { return props.getProperty("db.password", ""); }

    // --- Detector Hoja ---
    public int getCannyThreshold1() { return Integer.parseInt(props.getProperty("detector.hoja.canny.threshold1", "75")); }
    public int getCannyThreshold2() { return Integer.parseInt(props.getProperty("detector.hoja.canny.threshold2", "200")); }
    public double getAproxEpsilon() { return Double.parseDouble(props.getProperty("detector.hoja.aprox.epsilon", "0.02")); }

    // --- Detector Burbujas ---
    public double getCircularidadMinima() { return Double.parseDouble(props.getProperty("detector.burbujas.circularidad.min", "0.7")); }
    public int getAreaMinima() { return Integer.parseInt(props.getProperty("detector.burbujas.area.min", "100")); }
    public int getAreaMaxima() { return Integer.parseInt(props.getProperty("detector.burbujas.area.max", "50000")); }
    public double getRellenoMinimo() { return Double.parseDouble(props.getProperty("detector.burbujas.relleno.min", "0.30")); }

    // --- Filtros ---
    public int getKernelApertura() { return Integer.parseInt(props.getProperty("filtros.kernel.apertura", "3")); }
    public int getKernelCierre() { return Integer.parseInt(props.getProperty("filtros.kernel.cierre", "5")); }
    public boolean isOtsuHabilitado() { return Boolean.parseBoolean(props.getProperty("filtros.umbral.otsu", "true")); }

    // --- Examen ---
    public int getMaxPreguntas() { return Integer.parseInt(props.getProperty("examen.max.preguntas", "100")); }
    public int getOpcionesPorPregunta() { return Integer.parseInt(props.getProperty("examen.opciones.por.pregunta", "4")); }

    // --- Calificación ---
    public int getNotaMaxima() { return Integer.parseInt(props.getProperty("calificacion.nota.maxima", "20")); }
    public int getNotaAprobacion() { return Integer.parseInt(props.getProperty("calificacion.nota.aprobacion", "11")); }
    
    public char[] getOpcionesLetra() {
        int n = getOpcionesPorPregunta();
        char[] opciones = new char[n];
        for (int i = 0; i < n; i++) {
            opciones[i] = (char) ('A' + i);
        }
        return opciones;
    }
}
