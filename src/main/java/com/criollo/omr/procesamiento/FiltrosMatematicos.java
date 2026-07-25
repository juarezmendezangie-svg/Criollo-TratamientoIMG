package com.criollo.omr.procesamiento;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementa fórmulas matemáticas de procesamiento de imágenes.
 * 
 * Fórmulas del curso:
 * 1. Escala de grises: Z = 0.3R + 0.59G + 0.11B
 * 2. Ecualización: FP[k] = Σ(h[i]/N), Z' = floor(255 × FP[Z])
 * 3. Binarización: Z < u → 255, Z ≥ u → 0 (mejorado con Otsu)
 * 4. Erosión: Y[i,j] = AND(ventana k×k)
 * 5. Dilatación: Y[i,j] = OR(ventana k×k)
 * 6. Apertura: Dilatación(Erosión(X))
 * 7. Cierre: Erosión(Dilatación(X))
 */
public class FiltrosMatematicos {

    private static final Logger log = LoggerFactory.getLogger(FiltrosMatematicos.class);

    // ==================== ESCALA DE GRISES ====================
    // FÓRMULA: Z_nueva = (0.3 × R) + (0.59 × G) + (0.11 × B)
    // Modo profesional: usa Imgproc.cvtColor con COLOR_BGR2GRAY
    public Mat convertirAGrises(Mat imagenBGR) {
        if (imagenBGR == null || imagenBGR.empty()) {
            throw new IllegalArgumentException("Imagen de entrada nula o vacía");
        }
        Mat resultado = new Mat();
        // Modo profesional: OpenCV optimizado
        if (imagenBGR.channels() == 3) {
            Imgproc.cvtColor(imagenBGR, resultado, Imgproc.COLOR_BGR2GRAY);
        } else {
            imagenBGR.copyTo(resultado);
        }
        return resultado;
    }

    // Modo didáctico: recorre pixel a pixel (para demostración en clase)
    public Mat convertirAGrisesDidactico(Mat imagenBGR) {
        if (imagenBGR == null || imagenBGR.empty()) {
            throw new IllegalArgumentException("Imagen de entrada nula o vacía");
        }
        int filas = imagenBGR.rows();
        int cols = imagenBGR.cols();
        Mat resultado = new Mat(filas, cols, CvType.CV_8UC1);

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                double[] pixel = imagenBGR.get(i, j);
                double B = pixel[0], G = pixel[1], R = pixel[2];
                double z = (0.3 * R) + (0.59 * G) + (0.11 * B);
                z = Math.min(255, Math.max(0, z));
                resultado.put(i, j, (int) z);
            }
        }
        return resultado;
    }

    // ==================== ECUALIZACIÓN DEL HISTOGRAMA ====================
    // FÓRMULA: FP[k] = Σ(h[i]/N), Z' = floor(255 × FP[Z])
    // Modo profesional: Imgproc.equalizeHist
    public Mat ecualizarHistograma(Mat imagenGris) {
        if (imagenGris == null || imagenGris.empty()) {
            throw new IllegalArgumentException("Imagen de entrada nula o vacía");
        }
        Mat resultado = new Mat();
        Imgproc.equalizeHist(imagenGris, resultado);
        return resultado;
    }

    // Modo didáctico: cálculo manual del histograma
    public Mat ecualizarHistogramaDidactico(Mat imagenGris) {
        if (imagenGris == null || imagenGris.empty()) {
            throw new IllegalArgumentException("Imagen de entrada nula o vacía");
        }
        int filas = imagenGris.rows();
        int cols = imagenGris.cols();
        int totalPixeles = filas * cols;
        Mat resultado = new Mat(filas, cols, CvType.CV_8UC1);

        int[] h = new int[256];
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                h[(int) imagenGris.get(i, j)[0]]++;
            }
        }

        double[] FP = new double[256];
        double suma = 0;
        for (int k = 0; k < 256; k++) {
            suma += (double) h[k] / totalPixeles;
            FP[k] = suma;
        }

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                int z = (int) imagenGris.get(i, j)[0];
                resultado.put(i, j, (int) Math.floor(255.0 * FP[z]));
            }
        }
        return resultado;
    }

    // ==================== BINARIZACIÓN ====================
    // Umbralización ADAPTATIVA GAUSSIANA (Sección 5.2 del informe)
    // blockSize=31, C=10: adapta umbral localmente
    // BINARY_INV: marcas=BLANCO(255), fondo=NEGRO(0)
    // Supera a Otsu en iluminación no uniforme y marcas tenues
    public Mat binarizar(Mat imagenGris) {
        if (imagenGris == null || imagenGris.empty()) {
            throw new IllegalArgumentException("Imagen de entrada nula o vacía");
        }
        Mat resultado = new Mat();
        Imgproc.adaptiveThreshold(imagenGris, resultado, 255,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV, 31, 10);
        log.debug("Adaptive Threshold INV (b=31, C=10)");
        return resultado;
    }

    // Binarización adaptativa: mejor para documentos (maneja iluminación variable)
    public Mat binarizarAdaptativo(Mat imagenGris) {
        if (imagenGris == null || imagenGris.empty()) {
            throw new IllegalArgumentException("Imagen de entrada nula o vacía");
        }
        Mat resultado = new Mat();
        Imgproc.adaptiveThreshold(imagenGris, resultado, 255,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY, 15, 2);
        log.debug("Binarización adaptativa aplicada");
        return resultado;
    }

    // Binarización con umbral fijo (para comparación)
    public Mat binarizarUmbralFijo(Mat imagenGris, int umbral) {
        if (imagenGris == null || imagenGris.empty()) {
            throw new IllegalArgumentException("Imagen de entrada nula o vacía");
        }
        Mat resultado = new Mat();
        Imgproc.threshold(imagenGris, resultado, umbral, 255, Imgproc.THRESH_BINARY);
        return resultado;
    }

    // Modo didáctico: binarización manual
    public Mat binarizarDidactico(Mat imagenGris) {
        if (imagenGris == null || imagenGris.empty()) {
            throw new IllegalArgumentException("Imagen de entrada nula o vacía");
        }
        int filas = imagenGris.rows();
        int cols = imagenGris.cols();
        Mat resultado = new Mat(filas, cols, CvType.CV_8UC1);
        int u = 128;

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                int z = (int) imagenGris.get(i, j)[0];
                resultado.put(i, j, z < u ? 255 : 0);
            }
        }
        return resultado;
    }

    // ==================== EROSIÓN MORFOLÓGICA ====================
    // FÓRMULA: Y[i,j] = 255 si TODOS los vecinos en ventana k×k son 255
    public Mat erosion(Mat imagen, int k) {
        if (imagen == null || imagen.empty()) {
            throw new IllegalArgumentException("Imagen de entrada nula o vacía");
        }
        Mat resultado = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(k, k));
        try {
            Imgproc.erode(imagen, resultado, kernel);
        } finally {
            kernel.release();
        }
        return resultado;
    }

    // ==================== DILATACIÓN MORFOLÓGICA ====================
    // FÓRMULA: Y[i,j] = 255 si ALGÚN vecino en ventana k×k es 255
    public Mat dilatacion(Mat imagen, int k) {
        if (imagen == null || imagen.empty()) {
            throw new IllegalArgumentException("Imagen de entrada nula o vacía");
        }
        Mat resultado = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(k, k));
        try {
            Imgproc.dilate(imagen, resultado, kernel);
        } finally {
            kernel.release();
        }
        return resultado;
    }

    // ==================== APERTURA (Dilatación de Erosión) ====================
    // FÓRMULA: Apertura(X) = Dilatación(Erosión(X))
    // EFECTO: Elimina ruido pequeño (sal y pimienta)
    public Mat apertura(Mat imagen, int k) {
        Mat erosionada = erosion(imagen, k);
        try {
            return dilatacion(erosionada, k);
        } finally {
            erosionada.release();
        }
    }

    // ==================== CIERRE (Erosión de Dilatación) ====================
    // FÓRMULA: Cierre(X) = Erosión(Dilatación(X))
    // EFECTO: Rellena huecos internos
    public Mat cierre(Mat imagen, int k) {
        Mat dilatada = dilatacion(imagen, k);
        try {
            return erosion(dilatada, k);
        } finally {
            dilatada.release();
        }
    }

    // ==================== PIPELINE COMPLETO ====================
    // Dual: EqualizeHist+Otsu (contornos) + CLAHE+Otsu (relleno sin sesgo)
    public Mat[] procesarExamen(Mat imagenOriginal) {
        if (imagenOriginal == null || imagenOriginal.empty()) {
            throw new IllegalArgumentException("Imagen original nula o vacía");
        }

        Mat gris = null, eq = null, claheMat = null, otsuCont = null, otsuFill = null;
        try {
            gris = convertirAGrises(imagenOriginal);

            // 1. EqualizeHist + Otsu → contornos (100% detección)
            eq = new Mat();
            Imgproc.equalizeHist(gris, eq);
            otsuCont = new Mat();
            Imgproc.threshold(eq, otsuCont, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

            // 2. CLAHE + Otsu INV → relleno (sin sesgo, BINARY_INV: filled=WHITE)
            org.opencv.imgproc.CLAHE clahe = Imgproc.createCLAHE();
            clahe.setClipLimit(2.5);
            clahe.setTilesGridSize(new Size(8, 8));
            claheMat = new Mat();
            clahe.apply(gris, claheMat);
            otsuFill = new Mat();
            Imgproc.threshold(claheMat, otsuFill, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);

            Mat r1 = new Mat(); otsuCont.copyTo(r1);
            Mat r2 = new Mat(); otsuFill.copyTo(r2);
            Mat r3 = new Mat(); gris.copyTo(r3);
            return new Mat[]{r1, r2, r3};
        } finally {
            if (gris != null) gris.release();
            if (eq != null) eq.release();
            if (claheMat != null) claheMat.release();
            if (otsuCont != null) otsuCont.release();
            if (otsuFill != null) otsuFill.release();
        }
    }
}
