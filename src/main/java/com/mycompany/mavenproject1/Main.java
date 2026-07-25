package com.mycompany.mavenproject1;

import com.mycompany.mavenproject1.vista.VentanaPrincipal;
import nu.pattern.OpenCV;
import javax.swing.*;

/**
 * ============================================================
 * CLASE: Main — Punto de Entrada del Sistema OMR
 * ============================================================
 * Responsabilidad:
 *   1. Cargar la librería nativa de OpenCV (C++ bajo el capó)
 *   2. Aplicar el Look & Feel del sistema operativo (Windows)
 *   3. Lanzar la Interfaz Gráfica en el hilo de eventos de Swing
 * ============================================================
 */
public class Main {

    public static void main(String[] args) {
        
        // 1. Cargar OpenCV (nu.pattern lo hace automáticamente
        //    sin configurar rutas de .dll manualmente)
        OpenCV.loadLocally();
        
        // 2. Aplicar la apariencia nativa de Windows
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("No se pudo aplicar el Look&Feel: " + e.getMessage());
        }

        // 3. Lanzar la ventana en el hilo correcto de Swing (EDT)
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
        });
    }
}
