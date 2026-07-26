# Sistema OMR - Corrector de Exámenes con OpenCV y Java

## Objetivo del Proyecto

Sistema de corrección automática de exámenes tipo OMR (Optical Mark Recognition) usando OpenCV y Java 21. Procesa hojas de respuestas escaneadas, detecta burbujas marcadas y califica automáticamente comparando contra una plantilla del profesor.

## Stack Tecnológico

- Java 21 con Maven
- OpenCV 4.7.0 (`org.openpnp:opencv:4.7.0-0`)
- H2 Database embebida
- Apache PDFBox 3.0.3
- SLF4J para logging
- Swing para UI

## Estructura del Proyecto

```
com.criollo.omr/
├── Main.java                          # Punto de entrada
├── DiagnosticoSesgo.java              # Diagnóstico de sesgo de iluminación
├── TestGroundTruth.java               # Test vs respuestas conocidas (.md)
├── TestRapido.java                    # Test sin UI
├── TestCompleto.java                  # Test batch
├── config/
│   └── ConfiguracionExamen.java       # Singleton de configuración
├── controlador/
│   └── ControladorOMR.java            # Orquestador central
├── modelo/
│   ├── PlantillaMaestra.java          # Almacena respuestas correctas
│   ├── ResultadoCalificacion.java     # Modelo de resultado
│   └── DetallePregunta.java           # Detalle pregunta por pregunta
├── procesamiento/
│   ├── FiltrosMatematicos.java        # Pipeline: Gris → EqualizeHist → Otsu
│   ├── DetectorBurbujas.java          # findContours + filtros circularidad/área
│   ├── OrganizadorPreguntas.java      # Grid + detección de respuestas
│   ├── ExtractorDeRespuestas.java     # Coordina detección + organización
│   ├── DetectorHoja.java              # Canny + approxPolyDP para esquinas
│   ├── CorrectorPerspectiva.java      # getPerspectiveTransform + warpPerspective
│   └── FormulaDelCurso.java           # Todas las fórmulas del curso
├── persistencia/
│   ├── ConexionBaseDatos.java         # H2 embebida
│   └── CalificacionDAO.java           # CRUD calificaciones
├── exportacion/
│   ├── ExportadorPDF.java             # PDFBox 3.x
│   └── ExportadorCSV.java             # CSV UTF-8
└── vista/
    └── VentanaPrincipal.java          # Swing UI
```

## Pipeline de Procesamiento de Imagen

### Pipeline Principal (FiltrosMatematicos.procesarExamen)

```
Imagen BGR (1024×1536px)
    ↓
convertirAGrises() → COLOR_BGR2GRAY (Z = 0.3R + 0.59G + 0.11B)
    ↓
ecualizarHistograma() → Imgproc.equalizeHist (FP[k] = Σ(h[i]/N), Z' = 255 × FP[Z])
    ↓
binarizar() → Imgproc.threshold(THRESH_BINARY | THRESH_OTSU)
    ↓
Mat binaria → DetectorBurbujas (contornos)
Mat grises → OrganizadorPreguntas (intensidad)
```

### Pipeline de Detección (ControladorOMR)

```
Imgcodecs.imread(ruta)
    ↓
DetectorHoja.detectarHoja() → Point[4] esquinas
    ↓
[Si dimensiones < 40% del original] → usar imagen original
    ↓
FiltrosMatematicos.procesarExamen() → [binaria, grises]
    ↓
DetectorBurbujas.detectarBurbujas(binaria) → List<Burbuja>
    ↓
OrganizadorPreguntas.organizar(burbujas, grises) → Map<Pregunta, Letra>
    ↓
PlantillaMaestra.calcularAciertos() / calcularNota()
    ↓
ResultadoCalificacion
```

## Algoritmo de Detección de Burbujas

### DetectorBurbujas.java

```java
// 1. Encontrar contornos
Imgproc.findContours(imagenBinaria, contornos, jerarquia, RETR_TREE, CHAIN_APPROX_SIMPLE);

// 2. Filtrar por área y circularidad
for (MatOfPoint c : contornos) {
    double area = Math.abs(Imgproc.contourArea(c));           // área del contorno
    double perim = Imgproc.arcLength(contour2f, true);       // perímetro
    double circ = (4.0 * Math.PI * area) / (perim * perim);  // circularidad C = 4πA/P²
    
    if (area >= 20 && area <= 50000 && circ >= 0.4) {
        Rect bbox = Imgproc.boundingRect(c);
        double relleno = 1.0 - countNonZero(bbox) / (w*h);   // % píxeles negros
        burbujas.add(new Burbuja(centro, radio, relleno, bbox));
    }
}

// 3. Fusionar burbujas cercanas (misma posición detectada 2 veces)
// Umbral: distancia < min(radio1, radio2) × 2
```

### Parámetros (config.properties)

```properties
detector.burbujas.circularidad.min=0.4
detector.burbujas.area.min=20
detector.burbujas.area.max=50000
detector.burbujas.relleno.min=0.10
examen.max.preguntas=100
examen.opciones.por.pregunta=5
```

## Algoritmo de Organización de Preguntas

### OrganizadorPreguntas.java

```java
// PASO 1: Columnas por división del ancho
// Si >700 burbujas → 4 columnas, sino → 2 columnas
int nCols = ex.size() > 700 ? 4 : 2;
double fw = anchoImagen / nCols;
// Cada burbuja asignada a la columna según su X

// PASO 2: Filas por gaps en Y
// Agrupar burbujas cuyos centros Y están dentro de un umbral
// El umbral se calcula como: mediana(gaps) × 2.5 (mínimo 15px)
// Filtrar: solo filas con 1 a 6 burbujas
// Tomar máximo 25 filas por columna (ordenadas por Y)

// PASO 3: Asignación de opciones
// Ordenar burbujas en la fila por X (izquierda a derecha)
// Opción A = índice 0, B = índice 1, ..., E = índice 4
// Elegir la burbuja con mayor porcentajeRelleno() como respuesta
for (Burbuja b : fila) {
    if (b.porcentajeRelleno() > bestR) {
        bestR = b.porcentajeRelleno();
        bestI = bi;
    }
}
respuestas.put(numPregunta, opcionesLetra[bestI]);
```

## El Problema Actual

### Comportamiento

El sistema detecta 100/100 preguntas (detección de grid perfecta). Pero la precisión al identificar **cuál burbuja está rellena** es solo 20-35% (apenas arriba del 20% aleatorio para 5 opciones).

### Síntomas

1. **Sesgo a la izquierda**: el sistema favorece las opciones A y B (lado izquierdo) como respuesta
2. **Respuestas aleatorias**: las respuestas detectadas no coinciden con las reales en la imagen
3. **Consistente entre métodos**: todos los enfoques dan 20-35% de precisión

### Datos del Diagnóstico

El `DiagnosticoSesgo.java` reveló que **EqualizeHist CREA el sesgo**:

```
=== GRISES ORIGINALES (sin ecualizar) ===
Columna      A        B        C        D        E       
Col 1:       235      248      247      245      244    ← UNIFORME
Col 2:       232      234      215      219      223    ← UNIFORME

=== DESPUÉS DE EQUALIZEHIST ===
Columna      A        B        C        D        E       
Col 1:       158      179      170      166      165    
Col 2:       154      138      121      118      125    ← A 29 puntos MÁS CLARO que E!

=== DESPUÉS DE OTSU (countNonZero) ===
Columna      A        B        C        D        E       
Col 2:       107      84       71       67       75     ← A tiene 50% más blancos
```

**Conclusión**: EqualizeHist distorsiona los valores de grises de forma desigual entre columnas. La opción A (izquierda) queda sistemáticamente más clara/blanca que E, falseando la detección.

### Lo que se ha probado (todos dan 20-35%)

| Técnica | Resultado |
|---------|-----------|
| `porcentajeRelleno()` (fill%) | 19-25% |
| Área de bounding box | 29% |
| relleno × área | 33% |
| Pixel density (countNonZero binaria) | 24-32% |
| Intensidad media en grises (Core.mean) | 17-21% |
| HoughCircles | 15-25% |
| Snap-to-grid en columna (P10-P90) | 18-24% |
| Circular mask CLA (70% radio interno) | 14-30% |
| Template-based sampling | 23% |
| Gap Analysis estadístico | 15-20% |
| THRESH_BINARY_INV | 16-24% |
| Adaptive Threshold (blockSize 11-41) | 7-42 preguntas |
| CLAHE (clipLimit=2.5, tiles=8×8) | 7 preguntas |
| Dual pipeline (Equalize+Otsu contornos + CLAHE+Otsu relleno) | 20-24% |
| Decisión diferencial (Δ mejor vs segunda mejor) | 14% |
| MORPH_CLOSE / MORPH_OPEN | variable |
| resize a 512×768 | 50 preguntas |

### Hipótesis no resueltas

1. ¿EqualizeHist + Otsu es la causa raíz o solo un síntoma?
2. ¿Sauvola binarization (no disponible en Java OpenCV) resolvería el problema?
3. ¿El muestreo en posición fija (grid) vs posición de burbuja real marca diferencia?
4. ¿Las burbujas de 12-15px son demasiado pequeñas para cualquier técnica de OpenCV?

### Especificaciones de las Imágenes

```
- Formato: PNG, 1024×1536px
- Fondo: blanco puro #FFFFFF
- Texto/líneas: negro #000000
- Burbujas: círculos de 12-15px diámetro, outline 1-2px negro
- Burbuja llena: círculo negro sólido #000000
- Burbuja vacía: solo el outline, interior blanco
- 100 preguntas: 4 columnas × 25 filas × 5 opciones (A-E)
- 50 preguntas: 2 columnas × 25 filas × 5 opciones (A-E)
- Sin rotación, sin perspectiva, sin sombras (imagen digital pura)
```

## Lo que se necesita investigar

1. **Técnica exacta para detectar burbujas llenas vs vacías** en imágenes binarias Otsu a 1024×1536px con burbujas de 12-15px
2. **Si existe un proyecto OMR en GitHub/Gitee que funcione** con burbujas de ese tamaño
3. **Alternativa a EqualizeHist** que no distorsione los grises entre columnas (¿CLAHE? ¿nada?)
4. **Métrica de relleno** más robusta que `porcentajeRelleno` para burbujas pequeñas
5. **Código Java o Python** que implemente la solución
6. **Parámetros exactos** de binarización, filtros y thresholds

## Código de las Clases Principales

### FiltrosMatematicos.java (Pipeline)

```java
public Mat[] procesarExamen(Mat imagenOriginal) {
    Mat gris = convertirAGrises(imagenOriginal);   // BGR → Gray
    Mat eq = ecualizarHistograma(gris);             // EqualizeHist
    Mat otsu = binarizar(eq);                       // Otsu THRESH_BINARY
    return new Mat[]{otsu, gris};
}

public Mat binarizar(Mat imagenGris) {
    Mat resultado = new Mat();
    Imgproc.threshold(imagenGris, resultado, 0, 255, 
        Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
    return resultado;
}
```

### DetectorBurbujas.java (Detección de Contornos)

```java
public List<Burbuja> detectarBurbujas(Mat imagenBinaria) {
    Imgproc.findContours(imagenBinaria, contornos, jerarquia, 
        Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
    
    for (MatOfPoint c : contornos) {
        double area = Math.abs(Imgproc.contourArea(c));
        double perim = Imgproc.arcLength(c2f, true);
        double circ = (4.0 * Math.PI * area) / (perim * perim);
        if (area >= aMin && area <= aMax && circ >= cMin) {
            Rect bbox = Imgproc.boundingRect(c);
            double relleno = 1.0 - Core.countNonZero(roi) / (w*h);
            todas.add(new Burbuja(centro, radio, relleno, bbox, i));
        }
    }
    return fusionar(todas); // Fusionar burbujas cercanas
}

public record Burbuja(Point centro, int radio, 
    double porcentajeRelleno, Rect boundingBox, int indiceContour) {}
```

### OrganizadorPreguntas.java (Grid + Asignación)

```java
public Map<Integer, Character> organizar(List<Burbuja> burbujas,
        Mat imagenBinaria, Mat imagenGrises, int anchoImagen, int altoImagen) {
    
    // Zona examen: excluir 10% superior, 3% bordes
    double hY = altoImagen * 0.10, bY = altoImagen * 0.97;
    
    // Filtrar burbujas en zona examen
    List<Burbuja> ex = burbujas.stream()
        .filter(b -> b.centro().y > hY && b.centro().y < bY)
        .collect(Collectors.toList());
    
    // Columnas: 4 si >700 burbujas, 2 si no
    int nCols = ex.size() > 700 ? 4 : 2;
    
    // Filas por gaps en Y (25 máx por columna)
    // ...
    
    // Asignar: ordenar por X, elegir mayor porcentajeRelleno
    row.sort(Comparator.comparingDouble(b -> b.centro().x));
    for (Burbuja b : row) {
        if (b.porcentajeRelleno() > bestR) {
            bestR = b.porcentajeRelleno();
            bestI = bi;
        }
    }
    resp.put(qNum, letras[bestI]);
}
```

## Archivos de Ground Truth

Los archivos `.md` en `img/` contienen las respuestas correctas reales:

- `img/RESPUESTAS-100PR.md`: 100 líneas con letras A-E
- `img/RESPUESTAS-50PR..md`: 50 líneas con letras A-E

## Tests Automatizados

```bash
# Test vs ground truth
mvn compile exec:java -Dexec.mainClass=com.criollo.omr.TestGroundTruth

# Diagnóstico de sesgo de iluminación
mvn compile exec:java -Dexec.mainClass=com.criollo.omr.DiagnosticoSesgo

# Test rápido sin UI
mvn compile exec:java -Dexec.mainClass=com.criollo.omr.TestRapido

# Ejecutar la UI
mvn compile exec:java -Dexec.mainClass=com.criollo.omr.Main
```

VM args necesarios: `--enable-native-access=ALL-UNNAMED`

## Repositorio

https://github.com/juarezmendezangie-svg/Criollo-TratamientoIMG (master)
