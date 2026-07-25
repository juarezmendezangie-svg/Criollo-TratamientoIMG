package com.mycompany.mavenproject1.procesamiento;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

/**
 * ============================================================
 * CLASE: FiltrosMatematicos
 * ============================================================
 * Implementa EXACTAMENTE las fórmulas del Ingeniero del curso.
 * Cada método referencia directamente el apunte correspondiente.
 *
 * PRINCIPIO DE RESPONSABILIDAD ÚNICA (SRP - Estándar POO):
 *   Esta clase SOLO se encarga de transformar píxeles.
 *   No toma decisiones ni maneja la interfaz.
 * ============================================================
 */
public class FiltrosMatematicos {

    // ============================================================
    // FÓRMULA 1: ESCALA DE GRISES
    // Apunte del Ingeniero → Operaciones Orientadas al Punto:
    //   Z_nueva = (0.3 * R) + (0.59 * G) + (0.11 * B)
    //
    // FUNDAMENTO: El ojo humano NO percibe igual los 3 canales.
    //   Verde aporta 59% de la luminosidad percibida.
    //   Rojo aporta 30%, Azul solo 11%.
    // ============================================================
    public Mat convertirAGrises(Mat imagenBGR) {
        int filas = imagenBGR.rows();
        int cols  = imagenBGR.cols();
        Mat resultado = new Mat(filas, cols, CvType.CV_8UC1);

        // Recorrido EXPLÍCITO pixel a pixel (igual al apunte)
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                double[] pixel = imagenBGR.get(i, j); // [B, G, R] en OpenCV
                double B = pixel[0];
                double G = pixel[1];
                double R = pixel[2];

                // FÓRMULA EXACTA DEL INGENIERO:
                double z_nueva = (0.3 * R) + (0.59 * G) + (0.11 * B);

                // Validar rango [0, 255] antes de asignar
                z_nueva = Math.min(255, Math.max(0, z_nueva));
                resultado.put(i, j, (int) z_nueva);
            }
        }
        return resultado;
    }

    // ============================================================
    // FÓRMULA 2: ECUALIZACIÓN DEL HISTOGRAMA
    // Apunte del Ingeniero → FUNCIÓN EcualizarImagen(X):
    //   h   ← CalcularHistograma(X)
    //   FP  ← Probabilidad Acumulada de cada intensidad k
    //   Z'  ← EnteroAbajo(255 * FP[Z])
    //
    // FUNDAMENTO: Mejora el contraste redistribuyendo las
    //   intensidades para que haya más diferencia entre
    //   el papel blanco y el trazo del lápiz.
    // ============================================================
    public Mat ecualizarHistograma(Mat imagenGris) {
        int filas = imagenGris.rows();
        int cols  = imagenGris.cols();
        int totalPixeles = filas * cols;
        Mat resultado = new Mat(filas, cols, CvType.CV_8UC1);

        // PASO 1: Calcular Histograma (apunte: h[color] += 1)
        int[] h = new int[256];
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                int color = (int) imagenGris.get(i, j)[0];
                h[color]++;
            }
        }

        // PASO 2: Calcular Probabilidad Acumulada (apunte: FP[k])
        double[] FP = new double[256];
        double sumaAcumulada = 0;
        for (int k = 0; k < 256; k++) {
            double probabilidad = (double) h[k] / totalPixeles;
            sumaAcumulada += probabilidad;
            FP[k] = sumaAcumulada;
        }

        // PASO 3: Mapear cada píxel (apunte: Z' = EnteroAbajo(255 * FP[Z]))
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                int colorOriginal = (int) imagenGris.get(i, j)[0];
                int nuevoColor    = (int) Math.floor(255.0 * FP[colorOriginal]);
                resultado.put(i, j, nuevoColor);
            }
        }
        return resultado;
    }

    // ============================================================
    // FÓRMULA 3: BINARIZACIÓN (Umbralización)
    // Apunte del Ingeniero → Operaciones Orientadas al Punto:
    //   Si Z < u  →  Z_nueva = 255  (el lápiz oscuro se vuelve BLANCO)
    //   Sino      →  Z_nueva = 0    (el papel claro se vuelve NEGRO)
    //
    // NOTA: Invertida porque queremos detectar el trazo del lápiz.
    // ============================================================
    public Mat binarizar(Mat imagenGris) {
        int filas = imagenGris.rows();
        int cols  = imagenGris.cols();
        Mat resultado = new Mat(filas, cols, CvType.CV_8UC1);
        int u = 128; // Umbral en punto medio del rango 0-255

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                int z = (int) imagenGris.get(i, j)[0];

                // FÓRMULA DEL INGENIERO (invertida para detectar lápiz):
                int z_nueva;
                if (z < u) {
                    z_nueva = 255; // Píxel oscuro = lápiz → blanco
                } else {
                    z_nueva = 0;   // Píxel claro = papel → negro
                }
                resultado.put(i, j, z_nueva);
            }
        }
        return resultado;
    }

    // ============================================================
    // FÓRMULA 4: EROSIÓN MORFOLÓGICA
    // Apunte del Ingeniero → FUNCIÓN Erosion(Img, k):
    //   Para i, Para j:
    //     es_blanco ← Verdadero
    //     Para dy, Para dx:
    //       Si Img[i+dy, j+dx] == 0: es_blanco ← Falso
    //     Si es_blanco: Y[i,j]=255  Sino: Y[i,j]=0
    //
    // EFECTO: Encoge los objetos blancos. Elimina puntos pequeños.
    // ============================================================
    public Mat erosion(Mat imagen, int k) {
        int filas = imagen.rows();
        int cols  = imagen.cols();
        Mat resultado = new Mat(filas, cols, CvType.CV_8UC1, new Scalar(0));
        int mitad = k / 2;

        for (int i = mitad; i < filas - mitad; i++) {
            for (int j = mitad; j < cols - mitad; j++) {
                boolean esBlanco = true;

                // Recorrer vecinos (ventana k x k)
                for (int dy = -mitad; dy <= mitad; dy++) {
                    for (int dx = -mitad; dx <= mitad; dx++) {
                        int pixel = (int) imagen.get(i + dy, j + dx)[0];
                        if (pixel == 0) {
                            esBlanco = false;
                            break;
                        }
                    }
                    if (!esBlanco) break;
                }

                resultado.put(i, j, esBlanco ? 255 : 0);
            }
        }
        return resultado;
    }

    // ============================================================
    // FÓRMULA 5: DILATACIÓN MORFOLÓGICA
    // Apunte del Ingeniero → FUNCIÓN Dilatacion(Img, k):
    //   Para i, Para j:
    //     es_blanco ← Falso
    //     Para dy, Para dx:
    //       Si Img[i+dy, j+dx] == 255: es_blanco ← Verdadero
    //     Si es_blanco: Y[i,j]=255  Sino: Y[i,j]=0
    //
    // EFECTO: Expande los objetos blancos. Rellena huecos.
    // ============================================================
    public Mat dilatacion(Mat imagen, int k) {
        int filas = imagen.rows();
        int cols  = imagen.cols();
        Mat resultado = new Mat(filas, cols, CvType.CV_8UC1, new Scalar(0));
        int mitad = k / 2;

        for (int i = mitad; i < filas - mitad; i++) {
            for (int j = mitad; j < cols - mitad; j++) {
                boolean esBlanco = false;

                // Recorrer vecinos (ventana k x k)
                for (int dy = -mitad; dy <= mitad; dy++) {
                    for (int dx = -mitad; dx <= mitad; dx++) {
                        int pixel = (int) imagen.get(i + dy, j + dx)[0];
                        if (pixel == 255) {
                            esBlanco = true;
                            break;
                        }
                    }
                    if (esBlanco) break;
                }

                resultado.put(i, j, esBlanco ? 255 : 0);
            }
        }
        return resultado;
    }

    // ============================================================
    // FÓRMULA 6: APERTURA MORFOLÓGICA (Operación Compuesta)
    // Apunte del Ingeniero:
    //   Apertura(X) = Dilatacion( Erosion(X) )
    //   Limpieza(X) = Apertura(X)
    //
    // EFECTO: Elimina el ruido "sal y pimienta" del papel
    //   (borrones, marcas de borrador, suciedad).
    // ============================================================
    public Mat apertura(Mat imagen, int k) {
        Mat erosionada  = erosion(imagen, k);   // Paso 1: Erosión
        Mat dilatada    = dilatacion(erosionada, k); // Paso 2: Dilatación
        return dilatada;
    }

    // ============================================================
    // FÓRMULA 7: CIERRE MORFOLÓGICO (Operación Compuesta)
    // Apunte del Ingeniero:
    //   Cierre(X) = Erosion( Dilatacion(X) )
    //
    // EFECTO: Rellena los huecos internos de las burbujas
    //   (útil cuando el lápiz no es uniforme).
    // ============================================================
    public Mat cierre(Mat imagen, int k) {
        Mat dilatada    = dilatacion(imagen, k);    // Paso 1: Dilatación
        Mat erosionada  = erosion(dilatada, k);     // Paso 2: Erosión
        return erosionada;
    }

    // ============================================================
    // PIPELINE COMPLETO PARA EL EXAMEN OMR
    // Aplica todas las fórmulas en el orden correcto
    // ============================================================
    public Mat procesarExamen(Mat imagenOriginal) {
        // Paso 1: Escala de Grises (Fórmula luminosidad ponderada)
        Mat gris = convertirAGrises(imagenOriginal);
        
        // Paso 2: Ecualización (Mejora contraste con histograma)
        Mat ecualizada = ecualizarHistograma(gris);
        
        // Paso 3: Binarización (Separa lápiz del papel)
        Mat binaria = binarizar(ecualizada);
        
        // Paso 4: Apertura Morfológica (Limpia ruido pequeño)
        Mat limpia = apertura(binaria, 3);
        
        // Paso 5: Cierre Morfológico (Rellena huecos en burbujas)
        Mat rellena = cierre(limpia, 5);
        
        return rellena;
    }
}
