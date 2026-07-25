# Auditoría Visual del Sistema OMR — Análisis de Imágenes Reales

## Imágenes de entrada (CORRECTAS)

### Hoja 1 — Plantilla (Answer Key / Respuestas Correctas)
- Formato: "ANSWER SHEET MULTIPLE CHOICE EXAMINATION"
- Header: Student Name, Date, Group/Subject
- Instructions: EXAMPLE con correct/incorrect
- **4 columnas** × **25 filas** = **100 preguntas**
- **5 opciones por pregunta**: A, B, C, D, E
- Burbujas: **CÍRCULOS PERFECTOS** (rellenos negros = marcados, vacíos = sin marcar)
- Footer: "DO NOT WRITE IN THIS AREA" + esquinas negras (marcadores de alineación)
- **Total burbujas**: 100 × 5 = 500 burbujas

### Hoja 2 — Examen del Alumno (Respuestas del estudiante)
- Mismo formato exacto
- Diferentes burbujas marcadas (respuestas del alumno)
- Misma resolución y layout

---

## DIAGNÓSTICO: Por qué solo detecta 1 pregunta

### Las burbujas SÍ son circulares — el problema es OTRO

Revisando el código, los problemas son:

### Problema 1: Filtro de zona demasiado agresivo
```java
// OrganizadorPreguntas.java línea 47-48 (ANTES del fix)
.filter(b -> b.centro().y > 130 && b.centro().y < 1400 - 50)
.filter(b -> b.centro().x > 50 && b.centro().x < 900 - 20)
```
**Problema**: Si la imagen es más grande o más pequeña que 900×1400, las burbujas se descartan.
**Fix ya aplicado**: Ahora usa porcentuales dinámicos.

### Problema 2: El header descarta burbujas del encabezado
El EXAMPLE box tiene burbujas de ejemplo (A B C D E) que el detector detecta como contornos válidos. Estas burbujas están en la zona del header y se filtran correctamente.

### Problema 3: Las esquinas negras del footer se detectan como burbujas
Las 2 esquinas negras del footer ("DO NOT WRITE IN THIS AREA") son cuadrados que el detector podría confundir con burbujas.

### Problema 4 (CRÍTICO): Configuración default vs real
```properties
# config.properties actual
detector.burbujas.area.min=20
detector.burbujas.area.max=50000
detector.burbujas.circularidad.min=0.4
detector.burbujas.relleno.min=0.10
examen.opciones.por.pregunta=5
```

**Pero ConfiguracionExamen.java tiene defaults diferentes:**
```java
// Línea 46: default es 100, NO 20
public int getAreaMinima() { return Integer.parseInt(props.getProperty("detector.burbujas.area.min", "100")); }
// Línea 57: default es 4, NO 5
public int getOpcionesPorPregunta() { return Integer.parseInt(props.getProperty("examen.opciones.por.pregunta", "4")); }
```

**SI el config.properties no se carga** (path incorrecto), los defaults de Java se usan:
- `area.min = 100` (demasiado alto para burbujas pequeñas)
- `opciones.por.pregunta = 4` (debería ser 5)

### Problema 5 (CRÍTICO): La imagen se procesa 2 veces
```java
// ControladorOMR.java - procesarPlantillaEnHilo()
controlador.calificar(ruta, ruta, "Plantilla");  // Procesa la imagen 2 veces (plantilla + "alumno")
```
Esto significa que la imagen de la plantilla se binariza y procesa DOS veces, lo cual es redundante y podría causar problemas de memoria.

---

## ANÁLISIS DE FLUJO COMPLETO

```
Imagen PNG/JPG
    ↓
Imgcodecs.imread() → Mat BGR
    ↓
DetectorHoja.detectarHoja() → Point[4] esquinas (o null)
    ↓
CorrectorPerspectiva.corregirPerspectiva() → Mat enderezada
    ↓
FiltrosMatematicos.procesarExamen():
    1. convertirAGrises() → Mat gris (COLOR_BGR2GRAY)
    2. ecualizarHistograma() → Mat ecualizada
    3. binarizar() → Mat binaria (Otsu: THRESH_BINARY | THRESH_OTSU)
    ↓
ExtractorDeRespuestas.extraerRespuestas():
    1. DetectorBurbujas.detectarBurbujas():
       - findContours(RETR_TREE) → contornos
       - Filtrar por area [min, max] y circularidad >= 0.4
       - fusionarCercanas() → burbujas únicas
    2. OrganizadorPreguntas.organizar():
       - Filtrar zona de examen (excluir header/bordes)
       - Agrupar por columnas (X con tolerancia)
       - Agrupar por filas (Y con tolerancia)
       - Filtrar filas con 4-5 burbujas
       - Encontrar burbuja con mayor relleno por fila
    ↓
Map<Integer, Character> respuestas
    ↓
PlantillaMaestra / Calificación
```

---

## VERIFICACIÓN NECESARIA (para el siguiente modelo)

### 1. Ejecutar con logging verbose
Agregar al inicio de `main()`:
```java
System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
```

### 2. Verificar que config.properties se carga
```java
// En ConfiguracionExamen constructor
System.out.println("area.min=" + getAreaMinima());
System.out.println("opciones.por.pregunta=" + getOpcionesPorPregunta());
```

### 3. Verificar dimensiones de imagen cargada
```java
// En procesarImagenCompleta()
System.out.println("Imagen: " + imagenOriginal.cols() + "x" + imagenOriginal.rows());
```

### 4. Verificar contornos detectados
```java
// En DetectorBurbujas.detectarBurbujas()
System.out.println("Contornos: " + contornos.size());
System.out.println("Burbujas válidas: " + todasBurbujas.size());
```

---

## PARÁMETROS ÓPTIMOS PARA ESTAS IMÁGENES

Basado en el análisis visual de las hojas:

```properties
# Burbujas circulares de ~12-15px diámetro
detector.burbujas.area.min=80
detector.burbujas.area.max=500
detector.burbujas.circularidad.min=0.5
detector.burbujas.relleno.min=0.15

# Imagen típica: ~1000-1200px ancho
examen.max.preguntas=100
examen.opciones.por.pregunta=5

# Filtros
filtros.kernel.apertura=3
filtros.kernel.cierre=3
filtros.umbral.otsu=true
```

---

## ESTRUCTURA DE LA HOJA (para debugging)

```
[0px - 130px]    HEADER: Título, nombre, fecha, instrucciones, EXAMPLE
[130px - ~1050px] ZONA DE EXAMEN: 4 columnas × 25 filas × 5 burbujas
[~1050px - fin]  FOOTER: "DO NOT WRITE IN THIS AREA" + esquinas negras
```

### Columna 1: Preguntas 1-25 (x: ~50-250px)
### Columna 2: Preguntas 26-50 (x: ~270-470px)
### Columna 3: Preguntas 51-75 (x: ~490-690px)
### Columna 4: Preguntas 76-100 (x: ~710-910px)

*(Valores aproximados para imagen de ~1000px de ancho)*

---

## CAMBIOS APLICADOS EN ESTA SESIÓN

1. ✅ Eliminado `GeneradorExamen.java` y lógica de prueba
2. ✅ Fix `mostrarMiniatura()` con ComponentListener para redimensionamiento
3. ✅ Fix `OrganizadorPreguntas` con dimensiones dinámicas
4. ✅ Fix `ExtractorDeRespuestas` pasa dimensiones de imagen
5. ⚠️ Falta: Verificar que config.properties se carga correctamente
6. ⚠️ Falta: Ajustar parámetros de detección para estas imágenes específicas
