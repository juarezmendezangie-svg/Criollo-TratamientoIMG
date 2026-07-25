package com.criollo.omr;

import com.criollo.omr.vista.VentanaPrincipal;
import nu.pattern.OpenCV;
import javax.swing.*;

/**
 * Punto de entrada del Sistema OMR - Corrector de Exámenes.
 * 
 * 1. Carga la librería nativa de OpenCV
 * 2. Aplica Look & Feel del sistema
 * 3. Lanza la interfaz gráfica en el EDT
 */
public class Main {

    public static void main(String[] args) {
        // 1. Cargar OpenCV
        OpenCV.loadLocally();

        // 2. Look & Feel nativo
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("No se pudo aplicar Look&Feel: " + e.getMessage());
        }

        // 3. Lanzar ventana en el hilo correcto de Swing
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
        });
    }
}
