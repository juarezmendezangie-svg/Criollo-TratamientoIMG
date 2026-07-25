package com.criollo.omr.procesamiento;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación de TODAS las fórmulas del curso de Tratamiento de Imágenes.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *  1. TRANSFORMACIONES GEOMÉTRICAS
 * ═══════════════════════════════════════════════════════════════════════════════
 *  Traslación:     x' = x + tx,  y' = y + ty
 *  Escalado:       x' = Sx × x,  y' = Sy × y
 *  Rotación:       x' = x·cos(θ) − y·sin(θ),  y' = x·sin(θ) + y·cos(θ)
 *  Reflejo H:      x' = width − x − 1
 *  Reflejo V:      y' = height − y − 1
 *  Perspectiva:    [x']   [m00 m01 m02] [x ]
 *                  [y'] = [m10 m11 m12] [y ]
 *                  [w']   [m20 m21  1 ] [1 ]
 *  Interpolación bilineal:
 *      f(x,y) ≈ (1−α)(1−β)·f(i,j) + α(1−β)·f(i+1,j)
 *                + (1−α)β·f(i,j+1) + αβ·f(i+1,j+1)
 *      donde α = x − i,  β = y − j
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *  2. OPERACIONES ARITMÉTICAS
 * ═══════════════════════════════════════════════════════════════════════════════
 *  Suma promedio:      Z = (A + B) / 2
 *  Suma saturada:      Z = min(A + B, 255)
 *  Suma ponderada:     Z = α·A + (1−α)·B
 *  Resta suelo:        Z = max(A − B, 0)
 *  Resta absoluta:     Z = |A − B|
 *  Resta desplazada:   Z = (A − B) + 128
 *  Multiplicación máscara: Z = A × B / 255
 *  Mínimo:             Z = min(A, B)
 *  Máximo:             Z = max(A, B)
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *  3. OPERACIONES LÓGICAS
 * ═══════════════════════════════════════════════════════════════════════════════
 *  AND:  Z = A & B
 *  OR:   Z = A | B
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *  4. HISTOGRAMA Y ECUALIZACIÓN
 * ═══════════════════════════════════════════════════════════════════════════════
 *  Histograma:        h[k] = n_k   (conteo de píxeles con nivel k)
 *  PDF:               p[k] = h[k] / N
 *  CDF:               FP[k] = Σ p[i], i=0..k
 *  Ecualización:      Z' = floor(255 × FP[Z])
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *  5. OPERACIONES PUNTO A PUNTO
 * ═══════════════════════════════════════════════════════════════════════════════
 *  Negativo:          Z' = 255 − Z
 *  Brillo:            Z' = Z + b
 *  Contraste:         Z' = α × (Z − 128) + 128
 *  Gamma:             Z' = 255 × (Z/255)^γ
 *  Posterización:     Z' = floor(Z / (256/L)) × (256/L)
 *  Estiramiento:      Z' = (Z − Zmin) × 255 / (Zmax − Zmin)
 *  Binarización:      Z' = Z < u ? 255 : 0
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *  6. MORFOLOGÍA
 * ═══════════════════════════════════════════════════════════════════════════════
 *  Erosión:           Y[i,j] = AND{k×k}
 *  Dilatación:        Y[i,j] = OR{k×k}
 *  Apertura:          Dilatación(Erosión(X))
 *  Cierre:            Erosión(Dilatación(X))
 *  Esqueleto:         S = ∪ (Erosiónn(X) − Apertura(Erosiónn(X)))
 *  Relleno:           Flood fill desde semilla
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *  7. ESPACIOS DE COLOR
 * ═══════════════════════════════════════════════════════════════════════════════
 *  RGB → Grises:      Z = 0.3R + 0.59G + 0.11B
 *  RGB → HSV:
 *      H = acos(  0.5×((R−G)+(R−B)) / sqrt((R−G)²+(R−B)(G−B)) )  si B≤G
 *      H = 360° − H                                                  si B>G
 *      S = (max−min) / max
 *      V = max / 255
 *  RGB → CMYK:
 *      K = 1 − max(R,G,B)/255
 *      C = (1−R/255−K) / (1−K)
 *      M = (1−G/255−K) / (1−K)
 *      Y = (1−B/255−K) / (1−K)
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *  8. CÁLCULO DE TAMAÑO DE ARCHIVO
 * ═══════════════════════════════════════════════════════════════════════════════
 *  Imagen cruda:       Tamaño = filas × cols × canales × bitsPorPixel / 8
 *  Ejemplo 8-bit:      640×480×1 = 307,200 bytes ≈ 300 KB
 *  Ejemplo 24-bit:     640×480×3 = 921,600 bytes ≈ 900 KB
 */
public class FormulaDelCurso {

    private static final Logger log = LoggerFactory.getLogger(FormulaDelCurso.class);

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║  1. TRANSFORMACIONES GEOMÉTRICAS                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Traslación: x' = x + tx,  y' = y + ty
     * @param imagen imagen de entrada
     * @param tx traslación en X (píxeles)
     * @param ty traslación en Y (píxeles)
     */
    public Mat traslacion(Mat imagen, int tx, int ty) {
        Mat M = Mat.eye(2, 3, CvType.CV_64FC1);
        M.put(0, 2, tx);
        M.put(1, 2, ty);
        Mat resultado = new Mat();
        Imgproc.warpAffine(imagen, resultado, M, imagen.size());
        M.release();
        log.debug("Traslación aplicada: tx={}, ty={}", tx, ty);
        return resultado;
    }

    /**
     * Escalado: x' = Sx × x,  y' = Sy × y
     * @param imagen imagen de entrada
     * @param sx factor de escala en X
     * @param sy factor de escala en Y
     */
    public Mat escalado(Mat imagen, double sx, double sy) {
        Mat resultado = new Mat();
        Imgproc.resize(imagen, resultado, new Size(0, 0), sx, sy, Imgproc.INTER_LINEAR);
        log.debug("Escalado aplicado: sx={}, sy={}", sx, sy);
        return resultado;
    }

    /**
     * Escalado a tamaño fijo.
     */
    public Mat escaladoATamano(Mat imagen, int ancho, int alto) {
        Mat resultado = new Mat();
        Imgproc.resize(imagen, resultado, new Size(ancho, alto), 0, 0, Imgproc.INTER_LINEAR);
        log.debug("Escalado a tamaño: {}x{}", ancho, alto);
        return resultado;
    }

    /**
     * Rotación con mapeo inverso (backward mapping):
     *   x' = x·cos(θ) − y·sin(θ)
     *   y' = x·sin(θ) + y·cos(θ)
     * @param imagen imagen de entrada
     * @param anguloGrados ángulo de rotación en grados (sentido antihorario)
     */
    public Mat rotacion(Mat imagen, double anguloGrados) {
        Point centro = new Point(imagen.cols() / 2.0, imagen.rows() / 2.0);
        Mat M = Imgproc.getRotationMatrix2D(centro, anguloGrados, 1.0);
        Mat resultado = new Mat();
        Imgproc.warpAffine(imagen, resultado, M, imagen.size(),
            Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE);
        M.release();
        log.debug("Rotación aplicada: {}°", anguloGrados);
        return resultado;
    }

    /**
     * Reflejo horizontal: x' = width − x − 1
     */
    public Mat reflejoHorizontal(Mat imagen) {
        Mat resultado = new Mat();
        Core.flip(imagen, resultado, 1);
        log.debug("Reflejo horizontal aplicado");
        return resultado;
    }

    /**
     * Reflejo vertical: y' = height − y − 1
     */
    public Mat reflejoVertical(Mat imagen) {
        Mat resultado = new Mat();
        Core.flip(imagen, resultado, 0);
        log.debug("Reflejo vertical aplicado");
        return resultado;
    }

    /**
     * Reflejo ambos ejes (rotación 180°).
     */
    public Mat reflejoAmbos(Mat imagen) {
        Mat resultado = new Mat();
        Core.flip(imagen, resultado, -1);
        log.debug("Reflejo ambos ejes aplicado");
        return resultado;
    }

    /**
     * Transformación de perspectiva con 4 puntos usando interpolación bilineal.
     *
     * FÓRMULA bilineal:
     *   f(x,y) = (1−α)(1−β)·f(i,j) + α(1−β)·f(i+1,j)
     *           + (1−α)β·f(i,j+1) + αβ·f(i+1,j+1)
     *   donde α = x−i, β = y−j
     *
     * @param imagen imagen de entrada
     * @param src puntos origen (4 esquinas detectadas)
     * @param dst puntos destino (rectángulo ideal)
     * @param anchoDestino ancho de la imagen resultado
     * @param altoDestino alto de la imagen resultado
     */
    public Mat transformacionPerspectiva(Mat imagen, Point[] src, Point[] dst,
                                          int anchoDestino, int altoDestino) {
        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);
        srcMat.put(0, 0,
            src[0].x, src[0].y, src[1].x, src[1].y,
            src[2].x, src[2].y, src[3].x, src[3].y);
        dstMat.put(0, 0,
            dst[0].x, dst[0].y, dst[1].x, dst[1].y,
            dst[2].x, dst[2].y, dst[3].x, dst[3].y);

        Mat M = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Mat resultado = new Mat();
        // INTER_LINEAR = interpolación bilineal
        Imgproc.warpPerspective(imagen, resultado, M,
            new Size(anchoDestino, altoDestino), Imgproc.INTER_LINEAR);

        srcMat.release();
        dstMat.release();
        M.release();
        log.debug("Transformación de perspectiva aplicada (bilineal)");
        return resultado;
    }

    /**
     * Interpolación bilineal manual (didáctica).
     *
     * f(x,y) = (1−α)(1−β)·f(i,j) + α(1−β)·f(i+1,j)
     *         + (1−α)β·f(i,j+1)   + αβ·f(i+1,j+1)
     */
    public double interpolarBilineal(double[][] imagen, double x, double y) {
        int i = (int) x;
        int j = (int) y;
        int alto = imagen.length;
        int ancho = imagen[0].length;

        // Clamp a límites
        if (i < 0) i = 0;
        if (j < 0) j = 0;
        if (i >= ancho - 1) i = ancho - 2;
        if (j >= alto - 1) j = alto - 2;

        double alpha = x - i;
        double beta = y - j;

        double fij = imagen[j][i];
        double fi1j = imagen[j][i + 1];
        double fij1 = imagen[j + 1][i];
        double fi1j1 = imagen[j + 1][i + 1];

        return (1 - alpha) * (1 - beta) * fij
             + alpha * (1 - beta) * fi1j
             + (1 - alpha) * beta * fij1
             + alpha * beta * fi1j1;
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║  2. OPERACIONES ARITMÉTICAS                                         ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Suma promedio: Z = (A + B) / 2
     */
    public Mat sumaPromedio(Mat a, Mat b) {
        Mat resultado = new Mat();
        Core.addWeighted(a, 0.5, b, 0.5, 0, resultado);
        return resultado;
    }

    /**
     * Suma saturada: Z = min(A + B, 255)
     */
    public Mat sumaSaturada(Mat a, Mat b) {
        Mat resultado = new Mat();
        Core.add(a, b, resultado);
        return resultado;
    }

    /**
     * Suma ponderada: Z = α·A + (1−α)·B
     */
    public Mat sumaPonderada(Mat a, Mat b, double alpha) {
        Mat resultado = new Mat();
        Core.addWeighted(a, alpha, b, 1.0 - alpha, 0, resultado);
        return resultado;
    }

    /**
     * Resta con suelo: Z = max(A − B, 0)
     */
    public Mat restaSuelo(Mat a, Mat b) {
        Mat resultado = new Mat();
        Core.subtract(a, b, resultado);
        return resultado;
    }

    /**
     * Resta absoluta: Z = |A − B|
     */
    public Mat restaAbsoluta(Mat a, Mat b) {
        Mat diff = new Mat();
        Mat absDiff = new Mat();
        Core.subtract(a, b, diff);
        Core.absdiff(a, b, absDiff);
        diff.release();
        return absDiff;
    }

    /**
     * Resta desplazada: Z = (A − B) + 128
     * Útil para ver diferencias con nivel medio de gris.
     */
    public Mat restaDesplazada(Mat a, Mat b) {
        Mat resultado = new Mat();
        Mat aux = new Mat();
        Core.subtract(a, b, aux);
        Core.add(aux, new Scalar(128), resultado);
        aux.release();
        return resultado;
    }

    /**
     * Multiplicación máscara: Z = A × B / 255
     * Útil para enmascarar regiones de interés.
     */
    public Mat multiplicacionMascara(Mat a, Mat b) {
        Mat aFloat = new Mat();
        Mat bFloat = new Mat();
        Mat resultado = new Mat();
        a.convertTo(aFloat, CvType.CV_32F);
        b.convertTo(bFloat, CvType.CV_32F);
        Core.multiply(aFloat, bFloat, resultado, 1.0 / 255.0);
        Mat resultado8u = new Mat();
        resultado.convertTo(resultado8u, CvType.CV_8U);
        aFloat.release();
        bFloat.release();
        resultado.release();
        return resultado8u;
    }

    /**
     * Operación MÍNIMO pixel a pixel: Z = min(A, B)
     */
    public Mat operacionMinimo(Mat a, Mat b) {
        Mat resultado = new Mat();
        Core.min(a, b, resultado);
        return resultado;
    }

    /**
     * Operación MÁXIMO pixel a pixel: Z = max(A, B)
     */
    public Mat operacionMaximo(Mat a, Mat b) {
        Mat resultado = new Mat();
        Core.max(a, b, resultado);
        return resultado;
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║  3. OPERACIONES LÓGICAS                                             ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * AND bit a bit: Z = A & B
     */
    public Mat operacionAND(Mat a, Mat b) {
        Mat resultado = new Mat();
        Core.bitwise_and(a, b, resultado);
        return resultado;
    }

    /**
     * OR bit a bit: Z = A | B
     */
    public Mat operacionOR(Mat a, Mat b) {
        Mat resultado = new Mat();
        Core.bitwise_or(a, b, resultado);
        return resultado;
    }

    /**
     * XOR bit a bit: Z = A ^ B
     */
    public Mat operacionXOR(Mat a, Mat b) {
        Mat resultado = new Mat();
        Core.bitwise_xor(a, b, resultado);
        return resultado;
    }

    /**
     * NOT bit a bit: Z = ~A
     */
    public Mat operacionNOT(Mat imagen) {
        Mat resultado = new Mat();
        Core.bitwise_not(imagen, resultado);
        return resultado;
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║  4. HISTOGRAMA Y ECUALIZACIÓN                                       ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Calcula el histograma de una imagen grayscale.
     * FÓRMULA: h[k] = cantidad de píxeles con nivel de gris k
     * @return Mat de 1×256 con los conteos
     */
    public int[] calcularHistograma(Mat imagenGris) {
        int[] histograma = new int[256];
        for (int i = 0; i < imagenGris.rows(); i++) {
            for (int j = 0; j < imagenGris.cols(); j++) {
                int valor = (int) imagenGris.get(i, j)[0];
                histograma[valor]++;
            }
        }
        return histograma;
    }

    /**
     * Calcula la función de distribución acumulada (CDF).
     * FÓRMULA: FP[k] = Σ(h[i]/N), i=0..k
     */
    public double[] calcularCDF(int[] histograma, int totalPixeles) {
        double[] cdf = new double[256];
        double acumulador = 0;
        for (int k = 0; k < 256; k++) {
            acumulador += (double) histograma[k] / totalPixeles;
            cdf[k] = acumulador;
        }
        return cdf;
    }

    /**
     * Ecualización del histograma (modo didáctico manual).
     * FÓRMULA: Z' = floor(255 × FP[Z])
     */
    public Mat ecualizarHistogramaManual(Mat imagenGris) {
        int filas = imagenGris.rows();
        int cols = imagenGris.cols();
        int totalPixeles = filas * cols;

        int[] h = calcularHistograma(imagenGris);
        double[] cdf = calcularCDF(h, totalPixeles);

        Mat resultado = new Mat(filas, cols, CvType.CV_8UC1);
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                int z = (int) imagenGris.get(i, j)[0];
                resultado.put(i, j, (int) Math.floor(255.0 * cdf[z]));
            }
        }
        return resultado;
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║  5. OPERACIONES PUNTO A PUNTO                                        ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Negativo (inverso): Z' = 255 − Z
     */
    public Mat negativo(Mat imagen) {
        Mat resultado = new Mat();
        Core.bitwise_not(imagen, resultado);
        return resultado;
    }

    /**
     * Brillo: Z' = Z + b
     * @param brillo valor a sumar (-255 a 255)
     */
    public Mat brillo(Mat imagen, int brillo) {
        Mat resultado = new Mat();
        Core.add(imagen, new Scalar(brillo), resultado);
        return resultado;
    }

    /**
     * Contraste: Z' = α × (Z − 128) + 128
     * α > 1 incrementa contraste, α < 1 lo reduce
     */
    public Mat contraste(Mat imagen, double alpha) {
        Mat resultado = new Mat();
        imagen.convertTo(resultado, -1, alpha, 128 * (1 - alpha));
        return resultado;
    }

    /**
     * Corrección Gamma: Z' = 255 × (Z / 255)^γ
     * γ < 1 aclara, γ > 1 oscurece
     */
    public Mat gamma(Mat imagen, double gamma) {
        Mat floatImg = new Mat();
        Mat resultado = new Mat();
        imagen.convertTo(floatImg, CvType.CV_64F, 1.0 / 255.0);
        Core.pow(floatImg, gamma, resultado);
        Mat resultado8u = new Mat();
        resultado.convertTo(resultado8u, CvType.CV_8U, 255.0);
        floatImg.release();
        resultado.release();
        return resultado8u;
    }

    /**
     * Posterización: Z' = floor(Z / (256/L)) × (256/L)
     * Reduce niveles de gris a L niveles.
     */
    public Mat posterizacion(Mat imagen, int niveles) {
        int factor = 256 / niveles;
        Mat resultado = new Mat();
        imagen.convertTo(resultado, CvType.CV_8U, 1.0 / factor);
        resultado.convertTo(resultado, CvType.CV_8U, factor);
        return resultado;
    }

    /**
     * Estiramiento de contraste (stretch):
     * Z' = (Z − Zmin) × 255 / (Zmax − Zmin)
     */
    public Mat estiramiento(Mat imagen) {
        Core.MinMaxLocResult mmr = Core.minMaxLoc(imagen);
        double zMin = mmr.minVal;
        double zMax = mmr.maxVal;
        double rango = zMax - zMin;
        if (rango == 0) rango = 1;

        Mat resultado = new Mat();
        imagen.convertTo(resultado, CvType.CV_64F);
        Core.subtract(resultado, new Scalar(zMin), resultado);
        Core.multiply(resultado, new Scalar(255.0 / rango), resultado);
        Mat resultado8u = new Mat();
        resultado.convertTo(resultado8u, CvType.CV_8U);
        resultado.release();
        return resultado8u;
    }

    /**
     * Binarización con umbral fijo: Z < u → 255, Z ≥ u → 0
     */
    public Mat binarizar(Mat imagenGris, int umbral) {
        Mat resultado = new Mat();
        Imgproc.threshold(imagenGris, resultado, umbral, 255, Imgproc.THRESH_BINARY);
        return resultado;
    }

    /**
     * Binarización con umbral Otsu automático.
     */
    public Mat binarizarOtsu(Mat imagenGris) {
        Mat resultado = new Mat();
        Imgproc.threshold(imagenGris, resultado, 0, 255,
            Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        return resultado;
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║  6. MORFOLOGÍA (erosión, dilatación, apertura, cierre, esqueleto)   ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Erosión: Y[i,j] = AND de todos los vecinos en ventana k×k
     */
    public Mat erosion(Mat imagen, int k) {
        Mat resultado = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(k, k));
        Imgproc.erode(imagen, resultado, kernel);
        kernel.release();
        return resultado;
    }

    /**
     * Dilatación: Y[i,j] = OR de todos los vecinos en ventana k×k
     */
    public Mat dilatacion(Mat imagen, int k) {
        Mat resultado = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(k, k));
        Imgproc.dilate(imagen, resultado, kernel);
        kernel.release();
        return resultado;
    }

    /**
     * Apertura: Dilatación(Erosión(X))
     * Elimina ruido pequeño.
     */
    public Mat apertura(Mat imagen, int k) {
        Mat erosionada = erosion(imagen, k);
        Mat resultado = dilatacion(erosionada, k);
        erosionada.release();
        return resultado;
    }

    /**
     * Cierre: Erosión(Dilatación(X))
     * Rellena huecos internos.
     */
    public Mat cierre(Mat imagen, int k) {
        Mat dilatada = dilatacion(imagen, k);
        Mat resultado = erosion(dilatada, k);
        dilatada.release();
        return resultado;
    }

    /**
     * Esqueleto morfológico:
     * S = ∪_{n=0}^{N} [ Erosiónn(X) − Apertura(Erosiónn(X) ) ]
     *
     * Algoritmo iterativo:
     *   1. erosionar imagen
     *   2. abrir imagen erosionada
     *   3. restar: erosionada − abierta
     *   4. OR con esqueleto acumulado
     *   5. erosionar imagen original para siguiente iteración
     *   6. repetir hasta que la imagen sea vacía
     */
    public Mat esqueleto(Mat imagen) {
        Mat esqueleto = Mat.zeros(imagen.size(), CvType.CV_8UC1);
        Mat img = new Mat();
        imagen.copyTo(img);
        Mat temp = new Mat();
        Mat abierta = new Mat();

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3));
        boolean done = false;

        while (!done) {
            Imgproc.erode(img, temp, kernel);
            Imgproc.morphologyEx(temp, abierta, Imgproc.MORPH_OPEN, kernel);

            Mat diff = new Mat();
            Core.subtract(temp, abierta, diff);
            Core.bitwise_or(esqueleto, diff, esqueleto);

            img.release();
            img = new Mat();
            temp.copyTo(img);

            done = Core.countNonZero(img) == 0;
            diff.release();
        }

        kernel.release();
        temp.release();
        abierta.release();
        img.release();
        log.debug("Esqueleto morfológico extraído");
        return esqueleto;
    }

    /**
     * Relleno de región (flood fill) desde un punto semilla.
     * Rellena la región conectada del mismo color que el punto semilla.
     *
     * @param imagen imagen de entrada (se modifica in-place)
     * @param semilla punto semilla (x, y)
     * @param nuevoColor color de relleno (Scalar para 1 canal o 3 canales)
     */
    public void rellenarRegion(Mat imagen, Point semilla, Scalar nuevoColor) {
        Mat mascara = new Mat(imagen.rows() + 2, imagen.cols() + 2, CvType.CV_8UC1, Scalar.all(0));
        Scalar loDiff = new Scalar(20);
        Scalar upDiff = new Scalar(20);
        int flags = 8 | (255 << 8);

        int area = Imgproc.floodFill(imagen, mascara, semilla, nuevoColor,
            null, loDiff, upDiff, flags);

        mascara.release();
        log.debug("Flood fill: {} píxeles rellenados desde ({},{})",
            area, (int) semilla.x, (int) semilla.y);
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║  7. ESPACIOS DE COLOR                                                ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Conversión RGB → Grises:
     * FÓRMULA: Z = 0.3R + 0.59G + 0.11B
     */
    public Mat rgbAGrises(Mat imagenBGR) {
        Mat resultado = new Mat();
        if (imagenBGR.channels() == 3) {
            Imgproc.cvtColor(imagenBGR, resultado, Imgproc.COLOR_BGR2GRAY);
        } else {
            imagenBGR.copyTo(resultado);
        }
        return resultado;
    }

    /**
     * Conversión BGR → HSV:
     * H = ángulo en [0, 360)
     * S = (max − min) / max
     * V = max / 255
     */
    public Mat bgrAHSV(Mat imagenBGR) {
        Mat hsv = new Mat();
        if (imagenBGR.channels() == 3) {
            Imgproc.cvtColor(imagenBGR, hsv, Imgproc.COLOR_BGR2HSV);
        } else {
            imagenBGR.copyTo(hsv);
        }
        return hsv;
    }

    /**
     * Conversión RGB → CMYK:
     * K = 1 − max(R,G,B)/255
     * C = (1 − R/255 − K) / (1 − K)
     * M = (1 − G/255 − K) / (1 − K)
     * Y = (1 − B/255 − K) / (1 − K)
     *
     * Retorna Mat de 4 canales (C, M, Y, K).
     */
    public Mat rgbACMYK(Mat imagenBGR) {
        int filas = imagenBGR.rows();
        int cols = imagenBGR.cols();
        Mat cmyk = new Mat(filas, cols, CvType.CV_8UC4);

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                double[] pixel = imagenBGR.get(i, j);
                double b = pixel[0] / 255.0;
                double g = pixel[1] / 255.0;
                double r = pixel[2] / 255.0;

                double k = 1.0 - Math.max(r, Math.max(g, b));
                double c, m, y;
                if (k >= 1.0) {
                    c = 0; m = 0; y = 0;
                } else {
                    c = (1.0 - r - k) / (1.0 - k);
                    m = (1.0 - g - k) / (1.0 - k);
                    y = (1.0 - b - k) / (1.0 - k);
                }

                cmyk.put(i, j, c * 255, m * 255, y * 255, k * 255);
            }
        }
        return cmyk;
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║  8. CÁLCULO DE TAMAÑO DE ARCHIVO                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Calcula el tamaño crudo de una imagen en bytes.
     * FÓRMULA: Tamaño = filas × cols × canales × (bitsPorPixel / 8)
     */
    public long calcularTamanoBytes(int filas, int cols, int canales, int bitsPorPixel) {
        return (long) filas * cols * canales * (bitsPorPixel / 8);
    }

    /**
     * Formatea tamaño en bytes a formato legible.
     */
    public String formatearTamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
