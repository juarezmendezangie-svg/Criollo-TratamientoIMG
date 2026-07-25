package com.mycompany.mavenproject1.vista;

import com.mycompany.mavenproject1.modelo.PlantillaMaestra;
import com.mycompany.mavenproject1.procesamiento.ExtractorDeRespuestas;
import com.mycompany.mavenproject1.procesamiento.FiltrosMatematicos;
import com.mycompany.mavenproject1.util.GeneradorExamen;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;

import java.util.Map;

/**
 * ============================================================
 * CLASE: VentanaPrincipal (Capa Vista — Patrón MVC)
 * ============================================================
 * Interfaz 100% hecha a mano en Java Swing Puro.
 * SIN arrastrar y soltar del editor de paleta de NetBeans.
 * Esto demuestra dominio total de la POO en Java.
 *
 * DISEÑO DE 3 PANELES (BorderLayout):
 *   NORTE  → Barra de título azul
 *   CENTRO → Dos tarjetas (Plantilla | Alumno)
 *   SUR    → Área de reporte + Nota + Botones de acción
 * ============================================================
 */
public class VentanaPrincipal extends JFrame {

    // =================== PALETA DE COLORES ===================
    private static final Color C_AZUL      = new Color(41, 128, 185);
    private static final Color C_VERDE     = new Color(39, 174, 96);
    private static final Color C_ROJO      = new Color(231, 76, 60);
    private static final Color C_NARANJA   = new Color(230, 126, 34);
    private static final Color C_FONDO     = new Color(245, 247, 250);
    private static final Color C_BLANCO    = Color.WHITE;
    private static final Color C_TEXTO     = new Color(44, 62, 80);
    private static final Color C_GRIS      = new Color(140, 140, 140);

    // =================== FUENTES ===================
    private static final Font F_TITULO    = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font F_SUBTITULO = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_BOTON     = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font F_ESTADO    = new Font("Segoe UI", Font.ITALIC, 11);
    private static final Font F_CONSOLA   = new Font("Consolas", Font.PLAIN, 12);
    private static final Font F_NOTA      = new Font("Segoe UI", Font.BOLD, 48);

    // =================== COMPONENTES ===================
    private JLabel     lblImgPlantilla, lblImgAlumno;
    private JLabel     lblEstadoPlantilla, lblEstadoAlumno;
    private JButton    btnCargarPlantilla, btnCargarAlumno;
    private JButton    btnGenerar, btnCalificar, btnLimpiar;
    private JTextArea  txtReporte;
    private JLabel     lblNota, lblNotaTitulo;
    private JProgressBar barNota;

    // =================== LÓGICA ===================
    private final FiltrosMatematicos    filtros    = new FiltrosMatematicos();
    private final ExtractorDeRespuestas extractor  = new ExtractorDeRespuestas();
    private PlantillaMaestra            memoria    = new PlantillaMaestra();
    private String rutaPlantilla = null;
    private String rutaAlumno    = null;

    // =================== CONSTRUCTOR ===================
    public VentanaPrincipal() {
        configurarVentana();
        add(panelNorte(),  BorderLayout.NORTH);
        add(panelCentro(), BorderLayout.CENTER);
        add(panelSur(),    BorderLayout.SOUTH);
        configurarEventos();
    }

    // ============================================================
    // CONFIGURACIÓN BASE DE LA VENTANA
    // ============================================================
    private void configurarVentana() {
        setTitle("Sistema OMR — Corrector de Exámenes con Tratamiento de Imágenes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 720);
        setMinimumSize(new Dimension(800, 620));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_FONDO);
        setLayout(new BorderLayout(0, 0));
    }

    // ============================================================
    // PANEL NORTE — Encabezado azul con título del sistema
    // ============================================================
    private JPanel panelNorte() {
        JPanel panel = new JPanel();
        panel.setBackground(C_AZUL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(18, 25, 18, 25));

        JLabel titulo = new JLabel("Sistema OMR — Corrector Automático de Exámenes");
        titulo.setFont(F_TITULO);
        titulo.setForeground(C_BLANCO);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel(
            "Binarización  ·  Ecualización de Histograma  ·  Erosión  ·  Dilatación  ·  Apertura  ·  Cierre"
        );
        sub.setFont(F_SUBTITULO);
        sub.setForeground(new Color(200, 225, 255));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titulo);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(sub);
        return panel;
    }

    // ============================================================
    // PANEL CENTRO — Tarjetas de imagen (Plantilla | Alumno)
    // ============================================================
    private JPanel panelCentro() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.setBackground(C_FONDO);
        panel.setBorder(new EmptyBorder(12, 15, 6, 15));
        panel.add(tarjetaImagen("1.  Plantilla del Profesor", true));
        panel.add(tarjetaImagen("2.  Examen del Alumno",       false));
        return panel;
    }

    private JPanel tarjetaImagen(String titulo, boolean esPlantilla) {
        JPanel tarjeta = new JPanel(new BorderLayout(6, 6));
        tarjeta.setBackground(C_BLANCO);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(C_AZUL, 2), titulo,
                TitledBorder.LEFT, TitledBorder.TOP, F_BOTON, C_AZUL),
            new EmptyBorder(8, 8, 8, 8)
        ));

        // Placeholder de imagen
        JLabel lblImagen = new JLabel("Sin imagen cargada", SwingConstants.CENTER);
        lblImagen.setPreferredSize(new Dimension(310, 230));
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
            esPlantilla ? C_AZUL : C_VERDE
        );

        tarjeta.add(btnCargar,  BorderLayout.NORTH);
        tarjeta.add(lblImagen,  BorderLayout.CENTER);
        tarjeta.add(lblEstado,  BorderLayout.SOUTH);

        if (esPlantilla) {
            lblImgPlantilla    = lblImagen;
            lblEstadoPlantilla = lblEstado;
            btnCargarPlantilla = btnCargar;
        } else {
            lblImgAlumno    = lblImagen;
            lblEstadoAlumno = lblEstado;
            btnCargarAlumno = btnCargar;
        }
        return tarjeta;
    }

    // ============================================================
    // PANEL SUR — Reporte, Nota y Botones de Acción
    // ============================================================
    private JPanel panelSur() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(C_FONDO);
        panel.setBorder(new EmptyBorder(6, 15, 15, 15));

        // Área de texto — Reporte detallado
        txtReporte = new JTextArea(9, 50);
        txtReporte.setFont(F_CONSOLA);
        txtReporte.setEditable(false);
        txtReporte.setBackground(new Color(22, 27, 34));
        txtReporte.setForeground(new Color(0, 220, 100));
        txtReporte.setText(
            "  Reporte del sistema:\n" +
            "  ─────────────────────────────────────────────\n" +
            "  [1] Cargue la plantilla del profesor\n" +
            "      (o presione 'Generar Set de Prueba')\n" +
            "  [2] Cargue el examen del alumno\n" +
            "  [3] Presione 'Calificar Examen'\n"
        );
        JScrollPane scroll = new JScrollPane(txtReporte);
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 70)),
            "Reporte de Calificación",
            TitledBorder.LEFT, TitledBorder.TOP, F_BOTON, C_TEXTO
        ));
        panel.add(scroll, BorderLayout.CENTER);

        // Panel derecho — Nota + botones
        JPanel derecho = new JPanel();
        derecho.setLayout(new BoxLayout(derecho, BoxLayout.Y_AXIS));
        derecho.setBackground(C_FONDO);
        derecho.setBorder(new EmptyBorder(0, 12, 0, 0));

        lblNotaTitulo = etiqueta("NOTA FINAL", F_SUBTITULO, C_GRIS);
        lblNotaTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblNota = etiqueta("-- / 20", F_NOTA, C_AZUL);
        lblNota.setAlignmentX(Component.CENTER_ALIGNMENT);
        barNota = new JProgressBar(0, 20);
        barNota.setForeground(C_VERDE);
        barNota.setStringPainted(true);
        barNota.setMaximumSize(new Dimension(190, 18));

        btnGenerar  = boton("★  Generar Set de Prueba", C_NARANJA);
        btnCalificar = boton("▶  Calificar Examen",     C_VERDE);
        btnLimpiar   = boton("✖  Limpiar Todo",         C_ROJO);
        btnCalificar.setEnabled(false);
        for (JButton b : new JButton[]{btnGenerar, btnCalificar, btnLimpiar}) {
            b.setMaximumSize(new Dimension(200, 38));
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        derecho.add(lblNotaTitulo);
        derecho.add(Box.createRigidArea(new Dimension(0, 2)));
        derecho.add(lblNota);
        derecho.add(Box.createRigidArea(new Dimension(0, 6)));
        derecho.add(barNota);
        derecho.add(Box.createRigidArea(new Dimension(0, 14)));
        derecho.add(btnGenerar);
        derecho.add(Box.createRigidArea(new Dimension(0, 8)));
        derecho.add(btnCalificar);
        derecho.add(Box.createRigidArea(new Dimension(0, 8)));
        derecho.add(btnLimpiar);

        panel.add(derecho, BorderLayout.EAST);
        return panel;
    }

    // ============================================================
    // EVENTOS — Conecta la Vista con la Lógica de Procesamiento
    // ============================================================
    private void configurarEventos() {

        // CARGAR PLANTILLA
        btnCargarPlantilla.addActionListener(e -> {
            String ruta = dialogo();
            if (ruta != null) {
                rutaPlantilla = ruta;
                mostrarMiniatura(ruta, lblImgPlantilla);
                procesarPlantillaEnHilo(ruta);
            }
        });

        // CARGAR EXAMEN DEL ALUMNO
        btnCargarAlumno.addActionListener(e -> {
            String ruta = dialogo();
            if (ruta != null) {
                rutaAlumno = ruta;
                mostrarMiniatura(ruta, lblImgAlumno);
                lblEstadoAlumno.setText("Estado: ✓ imagen lista");
                lblEstadoAlumno.setForeground(C_VERDE);
                verificarHabilitarCalificar();
            }
        });

        // GENERAR SET DE PRUEBA (resuelve el problema de no tener fotos)
        btnGenerar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Seleccionar carpeta donde guardar el set de prueba");
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String carpeta = fc.getSelectedFile().getAbsolutePath();
                String[] rutas = GeneradorExamen.generarSetDePrueba(carpeta);

                rutaPlantilla = rutas[0];
                rutaAlumno    = rutas[1];
                mostrarMiniatura(rutaPlantilla, lblImgPlantilla);
                mostrarMiniatura(rutaAlumno,    lblImgAlumno);
                procesarPlantillaEnHilo(rutaPlantilla);

                lblEstadoAlumno.setText("Estado: ✓ generado automáticamente");
                lblEstadoAlumno.setForeground(C_VERDE);
                txtReporte.setText("Set de prueba generado en:\n  " + carpeta +
                    "\n\nPlantilla: " + rutaPlantilla +
                    "\nAlumno:    " + rutaAlumno +
                    "\n\nPresione '▶ Calificar Examen' para continuar.");
            }
        });

        // CALIFICAR EXAMEN
        btnCalificar.addActionListener(e -> {
            btnCalificar.setEnabled(false);
            txtReporte.setText("Procesando imagen con algoritmos matemáticos...\n");
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    Mat img = Imgcodecs.imread(rutaAlumno);
                    Mat procesada = filtros.procesarExamen(img);
                    Map<Integer, Character> respAlumno = extractor.extraerRespuestas(procesada);
                    return memoria.generarReporteDetallado(respAlumno);
                }
                @Override protected void done() {
                    try {
                        txtReporte.setText(get());
                        int nota = calcularNota();
                        lblNota.setText(nota + " / 20");
                        lblNota.setForeground(nota >= 11 ? C_VERDE : C_ROJO);
                        barNota.setValue(nota);
                    } catch (Exception ex) {
                        txtReporte.setText("Error: " + ex.getMessage());
                    }
                    btnCalificar.setEnabled(true);
                }
            }.execute();
        });

        // LIMPIAR TODO
        btnLimpiar.addActionListener(e -> limpiar());
    }

    // ============================================================
    // UTILIDADES INTERNAS
    // ============================================================
    private void procesarPlantillaEnHilo(String ruta) {
        lblEstadoPlantilla.setText("Estado: procesando...");
        lblEstadoPlantilla.setForeground(C_NARANJA);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                Mat img = Imgcodecs.imread(ruta);
                Mat procesada = filtros.procesarExamen(img);
                Map<Integer, Character> resp = extractor.extraerRespuestas(procesada);
                memoria = new PlantillaMaestra();
                memoria.setRutaImagenPlantilla(ruta);
                resp.forEach(memoria::registrarRespuesta);
                memoria.setPlantillaCargada(true);
                return null;
            }
            @Override protected void done() {
                lblEstadoPlantilla.setText("Estado: ✓ plantilla memorizada ("
                    + memoria.getTotalPreguntas() + " preguntas)");
                lblEstadoPlantilla.setForeground(C_VERDE);
                verificarHabilitarCalificar();
            }
        }.execute();
    }

    private int calcularNota() {
        Mat img = Imgcodecs.imread(rutaAlumno);
        Mat procesada = filtros.procesarExamen(img);
        Map<Integer, Character> resp = extractor.extraerRespuestas(procesada);
        return memoria.calcularNota(resp);
    }

    private void verificarHabilitarCalificar() {
        btnCalificar.setEnabled(
            rutaPlantilla != null && rutaAlumno != null && memoria.isPlantillaCargada()
        );
    }

    private void mostrarMiniatura(String ruta, JLabel label) {
        ImageIcon icon = new ImageIcon(ruta);
        int w = label.getWidth()  > 0 ? label.getWidth()  : 290;
        int h = label.getHeight() > 0 ? label.getHeight() : 220;
        label.setIcon(new ImageIcon(icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH)));
        label.setText("");
    }

    private String dialogo() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar imagen del examen");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes (JPG, PNG, BMP)", "jpg", "jpeg", "png", "bmp"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
            ? fc.getSelectedFile().getAbsolutePath() : null;
    }

    private void limpiar() {
        rutaPlantilla = null; rutaAlumno = null;
        memoria = new PlantillaMaestra();
        lblImgPlantilla.setIcon(null); lblImgPlantilla.setText("Sin imagen cargada");
        lblImgAlumno.setIcon(null);    lblImgAlumno.setText("Sin imagen cargada");
        lblEstadoPlantilla.setText("Estado: sin procesar"); lblEstadoPlantilla.setForeground(C_GRIS);
        lblEstadoAlumno.setText("Estado: sin procesar");   lblEstadoAlumno.setForeground(C_GRIS);
        lblNota.setText("-- / 20"); lblNota.setForeground(C_AZUL);
        barNota.setValue(0);
        btnCalificar.setEnabled(false);
        txtReporte.setText("Sistema limpiado. Cargue nuevas imágenes.");
    }

    // ============================================================
    // HELPERS PARA CONSTRUIR COMPONENTES (REUTILIZABLES)
    // ============================================================
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
