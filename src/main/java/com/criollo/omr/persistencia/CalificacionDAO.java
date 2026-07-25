package com.criollo.omr.persistencia;

import com.criollo.omr.modelo.ResultadoCalificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de calificaciones en H2.
 */
public class CalificacionDAO {

    private static final Logger log = LoggerFactory.getLogger(CalificacionDAO.class);

    public void guardar(ResultadoCalificacion resultado) {
        String sql = "INSERT INTO calificaciones (nombre_alumno, ruta_imagen_plantilla, ruta_imagen_alumno, " +
            "aciertos, total_preguntas, nota_final, respuestas_detectadas, fecha_calificacion) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBaseDatos.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, resultado.getNombreAlumno());
            ps.setString(2, resultado.getRutaImagenPlantilla());
            ps.setString(3, resultado.getRutaImagenAlumno());
            ps.setInt(4, resultado.getAciertos());
            ps.setInt(5, resultado.getTotalPreguntas());
            ps.setInt(6, resultado.getNotaFinal());
            ps.setString(7, resultado.getRespuestasDetectadasJSON());
            ps.setTimestamp(8, Timestamp.valueOf(resultado.getFechaCalificacion()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    resultado.setId(rs.getInt(1));
                }
            }
            log.info("Calificación guardada: {} - Nota: {}/20",
                resultado.getNombreAlumno(), resultado.getNotaFinal());

        } catch (SQLException e) {
            log.error("Error guardando calificación: {}", e.getMessage());
        }
    }

    public List<ResultadoCalificacion> listarTodas() {
        List<ResultadoCalificacion> lista = new ArrayList<>();
        String sql = "SELECT * FROM calificaciones ORDER BY fecha_calificacion DESC";

        try (Connection conn = ConexionBaseDatos.getInstancia().getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapearResultado(rs));
            }
        } catch (SQLException e) {
            log.error("Error listando calificaciones: {}", e.getMessage());
        }
        return lista;
    }

    public List<ResultadoCalificacion> buscarPorAlumno(String nombre) {
        List<ResultadoCalificacion> lista = new ArrayList<>();
        String sql = "SELECT * FROM calificaciones WHERE nombre_alumno LIKE ? ORDER BY fecha_calificacion DESC";

        try (Connection conn = ConexionBaseDatos.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearResultado(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error buscando calificaciones: {}", e.getMessage());
        }
        return lista;
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM calificaciones WHERE id = ?";
        try (Connection conn = ConexionBaseDatos.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            log.info("Calificación eliminada: id={}", id);
        } catch (SQLException e) {
            log.error("Error eliminando calificación: {}", e.getMessage());
        }
    }

    private ResultadoCalificacion mapearResultado(ResultSet rs) throws SQLException {
        ResultadoCalificacion r = new ResultadoCalificacion();
        r.setId(rs.getInt("id"));
        r.setNombreAlumno(rs.getString("nombre_alumno"));
        r.setRutaImagenPlantilla(rs.getString("ruta_imagen_plantilla"));
        r.setRutaImagenAlumno(rs.getString("ruta_imagen_alumno"));
        r.setAciertos(rs.getInt("aciertos"));
        r.setTotalPreguntas(rs.getInt("total_preguntas"));
        r.setNotaFinal(rs.getInt("nota_final"));

        Timestamp ts = rs.getTimestamp("fecha_calificacion");
        if (ts != null) r.setFechaCalificacion(ts.toLocalDateTime());

        return r;
    }
}
