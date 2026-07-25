package com.criollo.omr.persistencia;

import com.criollo.omr.config.ConfiguracionExamen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestiona la conexión a la base de datos H2 embebida.
 * H2 es una BD Java pura que NO requiere instalación.
 * Los datos se guardan en un archivo .mv.db en la carpeta ./data/
 */
public class ConexionBaseDatos {

    private static final Logger log = LoggerFactory.getLogger(ConexionBaseDatos.class);
    private static ConexionBaseDatos instancia;
    private final ConfiguracionExamen config;

    private ConexionBaseDatos() {
        this.config = ConfiguracionExamen.getInstancia();
        inicializar();
    }

    public static synchronized ConexionBaseDatos getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBaseDatos();
        }
        return instancia;
    }

    private void inicializar() {
        // Crear carpeta data si no existe
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        try (Connection conn = getConexion(); Statement stmt = conn.createStatement()) {
            // Crear tabla de calificaciones
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS calificaciones (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nombre_alumno VARCHAR(100) NOT NULL,
                    ruta_imagen_plantilla VARCHAR(500),
                    ruta_imagen_alumno VARCHAR(500),
                    aciertos INT NOT NULL,
                    total_preguntas INT NOT NULL,
                    nota_final INT NOT NULL,
                    respuestas_detectadas TEXT,
                    fecha_calificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            log.info("Base de datos H2 inicializada correctamente en ./data/omr_examenes");
        } catch (SQLException e) {
            log.error("Error inicializando base de datos: {}", e.getMessage());
        }
    }

    public Connection getConexion() throws SQLException {
        return DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword());
    }
}
