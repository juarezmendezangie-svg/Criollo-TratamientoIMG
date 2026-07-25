package com.criollo.omr.vista;

import com.criollo.omr.controlador.ControladorOMR;
import com.criollo.omr.config.ConfiguracionExamen;
import com.criollo.omr.modelo.ResultadoCalificacion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/**
 * Interfaz grafica principal del Sistema OMR.
 * Diseno profesional con 3 paneles: Norte (titulo), Centro (imagenes + reporte), Sur (acciones).
 */
public class VentanaPrincipal extends JFrame {

    // =================== COLORES ===================
    private static final Color C_AZUL    = new Color(41, 128, 185);
    private static final Color C_VERDE   = new Color(39, 174, 96);
    private static final Color C_ROJO    = new Color(231, 76, 60);
    private static final Color C_NARANJA = new Color(230, 126, 34);
    private static final Color C_FONDO   = new Color(245, 247, 250);
    private static final Color C_BLANCO  = Color.WHITE;
    private static final Color C_TEXTO   = new Color(44, 62, 80);
    private static final Color C_GRIS    = new Color(140, 140, 140);

    // =================== FUENTES ===================
    private static final Font F_TITULO    = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font F_SUBTITULO = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_BOTON     = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font F_ESTADO    = new Font("Segoe UI", Font.ITALIC, 11);
    private static final Font F_CONSOLA   = new Font("Consolas", Font.PLAIN, 12);
    private static final Font F_NOTA      = new Font("Segoe UI", Font.BOLD, 48);

    // =================== COMPONENTES ===================
    private JLabel lblImgPlantilla, lblImgAlumno;
    private JLabel lblEstadoPlantilla, lblEstadoAlumno;
    private JButton btnCargarPlantilla, btnCargarAlumno;
    private JButton btnCalificar, btnLimpiar;
    private JButton btnExportarPDF, btnExportarCSV, btnHistorial;
    private JTextArea txtReporte;
    private JLabel lblNota, lblNotaTitulo;
    private JProgressBar barNota;
    private JTextField txtNombreAlumno;

    // =================== LOGICA ===================
    private final ControladorOMR controlador;
    private ResultadoCalificacion ultimoResultado;
    private String rutaPlantilla = null;
    private String rutaAlumno = null;

    // =================== CONSTRUCTOR ===================
    public VentanaPrincipal() {
        this.controlador = new ControladorOMR();
        configurarVentana();
        add(panelNorte(), BorderLayout.NORTH);
        add(panelCentro(), BorderLayout.CENTER);
        add(panelSur(), BorderLayout.SOUTH);
        configurarEventos();
    }

    private void configurarVentana() {
        setTitle("Sistema OMR - Corrector de Examenes con Tratamiento de Imagenes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 780);
        setMinimumSize(new Dimension(900, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_FONDO);
        setLayout(new BorderLayout(0, 0));
    }

    // =================== PANEL NORTE ===================
    private JPanel panelNorte() {
        JPanel panel = new JPanel();
        panel.setBackground(C_AZUL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(16, 25, 16, 25));

        JLabel titulo = new JLabel("Sistema OMR - Corrector Automatico de Examenes");
        titulo.setFont(F_TITULO);
        titulo.setForeground(C_BLANCO);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel(
            "Binarizacion  |  Ecualizacion de Histograma  |  Erosion  |  Dilatacion  |  Apertura  |  Cierre  |  Otsu");
        sub.setFont(F_SUBTITULO);
        sub.setForeground(new Color(200, 225, 255));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titulo);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(sub);
        return panel;
    }

    // =================== PANEL CENTRO ===================
    private JPanel panelCentro() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(C_FONDO);
        panel.setBorder(new EmptyBorder(10, 15, 6, 15));

        // Tarjetas de imagen
        JPanel tarjetas = new JPanel(new GridLayout(1, 2, 12, 0));
        tarjetas.setBackground(C_FONDO);
        tarjetas.add(tarjetaImagen("1. Plantilla del Profesor", true));
        tarjetas.add(tarjetaImagen("2. Examen del Alumno", false));
        panel.add(tarjetas, BorderLayout.NORTH);

        // Area de reporte
        txtReporte = new JTextArea(10, 50);
        txtReporte.setFont(F_CONSOLA);
        txtReporte.setEditable(false);
        txtReporte.setBackground(new Color(22, 27, 34));
        txtReporte.setForeground(new Color(0, 220, 100));
        txtReporte.setText(getTextoInicial());

        JScrollPane scroll = new JScrollPane(txtReporte);
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 70)),
            "Reporte de Calificacion",
            TitledBorder.LEFT, TitledBorder.TOP, F_BOTON, C_TEXTO));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // Guardar rutas para redimensionar al cambiar tamaño de ventana
    private String rutaMiniaturaPlantilla = null;
    private String rutaMiniaturaAlumno = null;

    private JPanel tarjetaImagen(String titulo, boolean esPlantilla) {
        JPanel tarjeta = new JPanel(new BorderLayout(6, 6));
        tarjeta.setBackground(C_BLANCO);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(C_AZUL, 2), titulo,
                TitledBorder.LEFT, TitledBorder.TOP, F_BOTON, C_AZUL),
            new EmptyBorder(8, 8, 8, 8)));

        JLabel lblImagen = new JLabel("Sin imagen cargada", SwingConstants.CENTER);
        lblImagen.setPreferredSize(new Dimension(310, 200));
        lblImagen.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblImagen.setForeground(C_GRIS);
        lblImagen.setBackground(new Color(250, 251, 253));
        lblImagen.setOpaque(true);
        lblImagen.setBorder(BorderFactory.createDashedBorder(Color.LIGHT_GRAY, 3, 3));

        JLabel lblEstado = new JLabel("Estado: sin procesar", SwingConstants.CENTER);
        lblEstado.setFont(F_ESTADO);
        lblEstado.setForeground(C_GRIS);

        JButton btnCargar = boton(
            esPlantilla ? "Cargar Plantilla" : "Cargar Examen",
            esPlantilla ? C_AZUL : C_VERDE);

        tarjeta.add(btnCargar, BorderLayout.NORTH);
        tarjeta.add(lblImagen, BorderLayout.CENTER);
        tarjeta.add(lblEstado, BorderLayout.SOUTH);

        // ComponentListener: redimensionar imagen al cambiar tamaño del panel
        final boolean isPlantilla = esPlantilla;
        lblImagen.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                String ruta = isPlantilla ? rutaMiniaturaPlantilla : rutaMiniaturaAlumno;
                if (ruta != null && lblImagen.getWidth() > 0 && lblImagen.getHeight() > 0) {
                    ImageIcon icon = new ImageIcon(ruta);
                    int w = lblImagen.getWidth();
                    int h = lblImagen.getHeight();
                    Image scaled = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
                    lblImagen.setIcon(new ImageIcon(scaled));
                }
            }
        });

        if (esPlantilla) {
            lblImgPlantilla = lblImagen;
            lblEstadoPlantilla = lblEstado;
            btnCargarPlantilla = btnCargar;
        } else {
            lblImgAlumno = lblImagen;
            lblEstadoAlumno = lblEstado;
            btnCargarAlumno = btnCargar;
        }
        return tarjeta;
    }

    // =================== PANEL SUR ===================
    private JPanel panelSur() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(C_FONDO);
        panel.setBorder(new EmptyBorder(6, 15, 15, 15));

        // Panel izquierdo: nombre del alumno + nota
        JPanel izquierdo = new JPanel();
        izquierdo.setLayout(new BoxLayout(izquierdo, BoxLayout.Y_AXIS));
        izquierdo.setBackground(C_FONDO);

        // Nombre del alumno
        JPanel panelNombre = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panelNombre.setBackground(C_FONDO);
        panelNombre.add(new JLabel("Alumno:"));
        txtNombreAlumno = new JTextField(20);
        txtNombreAlumno.setFont(F_BOTON);
        panelNombre.add(txtNombreAlumno);
        izquierdo.add(panelNombre);
        izquierdo.add(Box.createRigidArea(new Dimension(0, 6)));

        // Nota
        lblNotaTitulo = etiqueta("NOTA FINAL", F_SUBTITULO, C_GRIS);
        lblNotaTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblNota = etiqueta("-- / 20", F_NOTA, C_AZUL);
        lblNota.setAlignmentX(Component.CENTER_ALIGNMENT);
        barNota = new JProgressBar(0, 20);
        barNota.setForeground(C_VERDE);
        barNota.setStringPainted(true);
        barNota.setMaximumSize(new Dimension(200, 18));

        izquierdo.add(lblNotaTitulo);
        izquierdo.add(Box.createRigidArea(new Dimension(0, 2)));
        izquierdo.add(lblNota);
        izquierdo.add(Box.createRigidArea(new Dimension(0, 6)));
        izquierdo.add(barNota);

        panel.add(izquierdo, BorderLayout.WEST);

        // Panel derecho: botones de accion
        JPanel derecho = new JPanel();
        derecho.setLayout(new BoxLayout(derecho, BoxLayout.Y_AXIS));
        derecho.setBackground(C_FONDO);
        derecho.setBorder(new EmptyBorder(0, 12, 0, 0));

        btnCalificar  = boton("Calificar Examen",     C_VERDE);
        btnExportarPDF = boton("Exportar PDF",        C_AZUL);
        btnExportarCSV = boton("Exportar CSV",        new Color(46, 134, 193));
        btnHistorial  = boton("Ver Historial",       new Color(142, 68, 173));
        btnLimpiar    = boton("Limpiar Todo",         C_ROJO);

        btnCalificar.setEnabled(false);
        btnExportarPDF.setEnabled(false);
        btnExportarCSV.setEnabled(false);

        for (JButton b : new JButton[]{btnCalificar, btnExportarPDF,
                btnExportarCSV, btnHistorial, btnLimpiar}) {
            b.setMaximumSize(new Dimension(200, 36));
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        derecho.add(btnCalificar);
        derecho.add(Box.createRigidArea(new Dimension(0, 6)));
        derecho.add(btnExportarPDF);
        derecho.add(Box.createRigidArea(new Dimension(0, 6)));
        derecho.add(btnExportarCSV);
        derecho.add(Box.createRigidArea(new Dimension(0, 6)));
        derecho.add(btnHistorial);
        derecho.add(Box.createRigidArea(new Dimension(0, 6)));
        derecho.add(btnLimpiar);

        panel.add(derecho, BorderLayout.EAST);

        return panel;
    }

    // =================== EVENTOS ===================
    private void configurarEventos() {

        // CARGAR PLANTILLA
        btnCargarPlantilla.addActionListener(e -> {
            String ruta = dialogoSeleccionarImagen();
            if (ruta != null) {
                rutaPlantilla = ruta;
                rutaMiniaturaPlantilla = ruta;
                mostrarMiniatura(ruta, lblImgPlantilla);
                procesarPlantillaEnHilo(ruta);
            }
        });

        // CARGAR EXAMEN DEL ALUMNO
        btnCargarAlumno.addActionListener(e -> {
            String ruta = dialogoSeleccionarImagen();
            if (ruta != null) {
                rutaAlumno = ruta;
                rutaMiniaturaAlumno = ruta;
                mostrarMiniatura(ruta, lblImgAlumno);
                lblEstadoAlumno.setText("Estado: imagen lista");
                lblEstadoAlumno.setForeground(C_VERDE);
                verificarHabilitarCalificar();
            }
        });

        // CALIFICAR EXAMEN
        btnCalificar.addActionListener(e -> {
            String nombreRaw = txtNombreAlumno.getText().trim();
            final String nombre = nombreRaw.isEmpty() ? "Alumno Sin Nombre" : nombreRaw;

            btnCalificar.setEnabled(false);
            txtReporte.setText("Procesando imagen con algoritmos matematicos...\n");

            final String rutaP = rutaPlantilla;
            final String rutaA = rutaAlumno;

            new SwingWorker<ResultadoCalificacion, Void>() {
                @Override protected ResultadoCalificacion doInBackground() {
                    return controlador.calificar(rutaP, rutaA, nombre);
                }
                @Override protected void done() {
                    try {
                        ultimoResultado = get();
                        txtReporte.setText(ultimoResultado.getDetalle() != null ?
                            generarReporte(ultimoResultado) :
                            "No se detectaron respuestas");

                        int nota = ultimoResultado.getNotaFinal();
                        lblNota.setText(nota + " / 20");
                        lblNota.setForeground(nota >= ConfiguracionExamen.getInstancia().getNotaAprobacion() ? C_VERDE : C_ROJO);
                        barNota.setValue(nota);

                        btnExportarPDF.setEnabled(true);
                        btnExportarCSV.setEnabled(true);

                        // Guardar en historial
                        controlador.guardarEnHistorial(ultimoResultado);
                    } catch (Exception ex) {
                        txtReporte.setText("Error: " + ex.getMessage());
                    }
                    btnCalificar.setEnabled(rutaPlantilla != null && rutaAlumno != null);
                }
            }.execute();
        });

        // EXPORTAR PDF
        btnExportarPDF.addActionListener(e -> {
            if (ultimoResultado == null) return;
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Guardar reporte PDF");
            fc.setSelectedFile(new File("reporte_calificacion.pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    controlador.exportarPDF(ultimoResultado, fc.getSelectedFile());
                    JOptionPane.showMessageDialog(this, "PDF exportado correctamente",
                        "Exportar PDF", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error exportando PDF: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // EXPORTAR CSV
        btnExportarCSV.addActionListener(e -> {
            if (ultimoResultado == null) return;
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Guardar reporte CSV");
            fc.setSelectedFile(new File("reporte_calificacion.csv"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    controlador.exportarCSV(ultimoResultado, fc.getSelectedFile());
                    JOptionPane.showMessageDialog(this, "CSV exportado correctamente",
                        "Exportar CSV", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error exportando CSV: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // VER HISTORIAL
        btnHistorial.addActionListener(e -> mostrarHistorial());

        // LIMPIAR TODO
        btnLimpiar.addActionListener(e -> limpiar());
    }

    // =================== UTILIDADES ===================
    private void procesarPlantillaEnHilo(String ruta) {
        lblEstadoPlantilla.setText("Estado: procesando...");
        lblEstadoPlantilla.setForeground(C_NARANJA);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                controlador.calificar(ruta, ruta, "Plantilla");
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    lblEstadoPlantilla.setText("Estado: plantilla memorizada (" +
                        controlador.getPlantilla().getTotalPreguntas() + " preguntas)");
                    lblEstadoPlantilla.setForeground(C_VERDE);
                } catch (Exception ex) {
                    lblEstadoPlantilla.setText("Estado: error al procesar");
                    lblEstadoPlantilla.setForeground(C_ROJO);
                }
                verificarHabilitarCalificar();
            }
        }.execute();
    }

    private void verificarHabilitarCalificar() {
        btnCalificar.setEnabled(rutaPlantilla != null && rutaAlumno != null && controlador.isPlantillaCargada());
    }

    private void mostrarMiniatura(String ruta, JLabel label) {
        label.setIcon(null);
        label.setText("Cargando...");
        label.repaint();

        // Usar SwingWorker para cargar imagen sin bloquear UI
        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() {
                ImageIcon icon = new ImageIcon(ruta);
                return icon;
            }
            @Override protected void done() {
                try {
                    ImageIcon icon = get();
                    int w = label.getWidth() > 0 ? label.getWidth() : 310;
                    int h = label.getHeight() > 0 ? label.getHeight() : 200;
                    Image scaled = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(scaled));
                    label.setText("");
                } catch (Exception ex) {
                    label.setText("Error al cargar imagen");
                }
            }
        }.execute();
    }

    private String dialogoSeleccionarImagen() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar imagen del examen");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imagenes (JPG, PNG, BMP)", "jpg", "jpeg", "png", "bmp"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
            ? fc.getSelectedFile().getAbsolutePath() : null;
    }

    private void limpiar() {
        rutaPlantilla = null;
        rutaAlumno = null;
        rutaMiniaturaPlantilla = null;
        rutaMiniaturaAlumno = null;
        ultimoResultado = null;
        lblImgPlantilla.setIcon(null);
        lblImgPlantilla.setText("Sin imagen cargada");
        lblImgAlumno.setIcon(null);
        lblImgAlumno.setText("Sin imagen cargada");
        lblEstadoPlantilla.setText("Estado: sin procesar");
        lblEstadoPlantilla.setForeground(C_GRIS);
        lblEstadoAlumno.setText("Estado: sin procesar");
        lblEstadoAlumno.setForeground(C_GRIS);
        lblNota.setText("-- / 20");
        lblNota.setForeground(C_AZUL);
        barNota.setValue(0);
        btnCalificar.setEnabled(false);
        btnExportarPDF.setEnabled(false);
        btnExportarCSV.setEnabled(false);
        txtNombreAlumno.setText("");
        txtReporte.setText(getTextoInicial());
    }

    private void mostrarHistorial() {
        JDialog dialogo = new JDialog(this, "Historial de Calificaciones", true);
        dialogo.setSize(700, 400);
        dialogo.setLocationRelativeTo(this);

        List<ResultadoCalificacion> historial = controlador.obtenerHistorial();
        String[] columnas = {"ID", "Alumno", "Aciertos", "Total", "Nota", "Fecha"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (ResultadoCalificacion r : historial) {
            modelo.addRow(new Object[]{
                r.getId(), r.getNombreAlumno(), r.getAciertos(),
                r.getTotalPreguntas(), r.getNotaFinal() + "/20",
                r.getFechaCalificacion() != null ? r.getFechaCalificacion().toString().substring(0, 16) : ""
            });
        }

        JTable tabla = new JTable(modelo);
        tabla.setFont(F_CONSOLA);
        tabla.setRowHeight(25);
        dialogo.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JButton btnCerrar = boton("Cerrar", C_GRIS);
        btnCerrar.addActionListener(ev -> dialogo.dispose());
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBoton.add(btnCerrar);
        dialogo.add(panelBoton, BorderLayout.SOUTH);

        dialogo.setVisible(true);
    }

    private String generarReporte(ResultadoCalificacion r) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== REPORTE DE CALIFICACION =====\n\n");
        sb.append("Alumno: ").append(r.getNombreAlumno()).append("\n");
        sb.append(String.format("Fecha: %s\n\n", r.getFechaCalificacion()));
        sb.append(String.format("%-12s %-10s %-10s %-10s%n", "Pregunta", "Correcta", "Alumno", "Estado"));
        sb.append("-".repeat(45)).append("\n");

        for (var det : r.getDetalle()) {
            sb.append(String.format("  P%02d         %c           %c           %s%n",
                det.getNumeroPregunta(), det.getRespuestaCorrecta(),
                det.getRespuestaAlumno(), det.isEsCorrecta() ? "OK" : "X"));
        }

        sb.append("-".repeat(45)).append("\n");
        sb.append(String.format("Aciertos: %d / %d%n", r.getAciertos(), r.getTotalPreguntas()));
        sb.append(String.format("NOTA FINAL: %d / 20%n", r.getNotaFinal()));
        return sb.toString();
    }

    private String getTextoInicial() {
        return "  Reporte del sistema:\n" +
            "  ---------------------------------------------\n" +
            "  [1] Ingrese nombre del alumno\n" +
            "  [2] Cargue la plantilla del profesor\n" +
            "  [3] Cargue el examen del alumno\n" +
            "  [4] Presione 'Calificar Examen'\n";
    }

    // =================== HELPERS ===================
    private JButton boton(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(F_BOTON);
        btn.setBackground(color);
        btn.setForeground(C_BLANCO);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(color.darker()); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(color); }
        });
        return btn;
    }

    private JLabel etiqueta(String texto, Font fuente, Color color) {
        JLabel lbl = new JLabel(texto, SwingConstants.CENTER);
        lbl.setFont(fuente);
        lbl.setForeground(color);
        return lbl;
    }
}
