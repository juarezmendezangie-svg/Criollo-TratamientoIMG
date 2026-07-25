# Fórmulas del Curso de Tratamiento de Imágenes

## Tabla de Contenidos
1. [Transformaciones Geométricas](#1-transformaciones-geométricas)
2. [Operaciones Aritméticas](#2-operaciones-aritméticas)
3. [Operaciones Lógicas](#3-operaciones-lógicas)
4. [Histograma y Ecualización](#4-histograma-y-ecualización)
5. [Operaciones Orientadas al Punto](#5-operaciones-orientadas-al-punto)
6. [Morfología](#6-morfología)
7. [Operaciones Morfológicas Compuestas](#7-operaciones-morfológicas-compuestas)
8. [Transformaciones Iterativas](#8-transformaciones-iterativas)
9. [Digitalización y Memoria](#9-digitalización-y-memoria)
10. [Teoría del Color](#10-teoría-del-color)
11. [Tratamiento Digital y Almacenamiento](#11-tratamiento-digital-y-almacenamiento)
12. [Preguntas Resueltas](#12-preguntas-resueltas)
13. [Aplicación al Sistema OMR](#13-aplicación-al-sistema-omr)

---

## 1. Transformaciones Geométricas

```
FUNCIÓN: geo_transformation(X, A)
N ← Alto(X)
M ← Ancho(X)
Y ← CrearImagen(N, M) con ceros

// COEFICIENTES DE LA MATRIZ
a11←A[0,0]  a12←A[0,1]  a13←A[0,2]
a21←A[1,0]  a22←A[1,1]  a23←A[1,2]

// RECORRIDO DEL LIENZO DESTINO (Mapeo Inverso)
Para i desde 0 hasta N-1:
  Para j desde 0 hasta M-1:
    ie ← a11*i + a12*j + a13
    je ← a21*i + a22*j + a23
    i0 ← EnteroAbajo(ie)   j0 ← EnteroAbajo(je)
    di ← ie - i0           dj ← je - j0

    // INTERPOLACIÓN BILINEAL
    Si (i0>=0) Y (i0<N-1) Y (j0>=0) Y (j0<M-1):
      c00←X[i0,j0]   c10←X[i0+1,j0]
      c01←X[i0,j0+1] c11←X[i0+1,j0+1]
      v1 ← c00*(1-dj) + c01*dj
      v2 ← c10*(1-dj) + c11*dj
      Y[i,j] ← v1*(1-di) + v2*di
    Sino Si (i0>=0) Y (i0<N) Y (j0>=0) Y (j0<M):
      Y[i,j] ← X[i0, j0]
    FinSi
  FinPara
FinPara
Retornar Y
```

### Matrices de Transformación

| Transformación | Matriz |
|---------------|--------|
| Traslación (tx, ty) | [1,0,-ty] [0,1,-tx] |
| Escalado (s) | [si,0,0] [0,si,0] donde si=1/s |
| Rotación (θ°) | [cos(θ),sin(θ),0] [-sin(θ),cos(θ),0] |
| Reflejo Horizontal | [1,0,0] [0,-1,M-1] |
| Reflejo Vertical | [-1,0,N-1] [0,1,0] |
| Rotación 180° | [-1,0,N-1] [0,-1,M-1] |
| Rotación 90° Horario | [0,-1,N-1] [1,0,0] |
| Rotación 90° Antihorario | [0,1,0] [-1,0,M-1] |

---

## 2. Operaciones Aritméticas

| Operación | Fórmula | Uso |
|-----------|---------|-----|
| Suma Promedio | Z = (A+B)/2 | Fusionar luz suavemente |
| Suma Saturada | Z = A+B (clip 255) | — |
| Suma Ponderada | Z = α·A + β·B (α+β=1) | Fusionar con pesos |
| Resta Absoluta | Z = \|A−B\| | Detectar movimiento/cambios |
| Resta Suelo | Z = A−B (≥0) | Zonas donde A > B |
| Resta Desplazada | Z = 127 + (A−B)/2 | Fondo gris neutro |
| Multiplicación | Z = (A×B)/255 | Recortar preservando sombras |
| MIN | Z = min(A,B) | Elegir más oscuro |
| MAX | Z = max(A,B) | Elegir más claro |

---

## 3. Operaciones Lógicas

| Operación | Uso |
|-----------|-----|
| AND | Recortes estrictos B/N (intersección) |
| OR | Pegar dos siluetas B/N (unión) |
| NOT | Negación (inversión) |

---

## 4. Histograma y Ecualización

```
FUNCIÓN: CalcularHistograma(X)
N←Alto(X)  M←Ancho(X)
h ← Arreglo(256) con valor 0
Para i desde 0 hasta N-1:
  Para j desde 0 hasta M-1:
    color ← X[i,j]
    h[color] ← h[color] + 1
  FinPara
FinPara
Retornar h
```

```
FUNCIÓN: EcualizarImagen(X)
N←Alto(X)  M←Ancho(X)  TotalPixeles←N*M
Y ← CrearImagen(N, M) con ceros

// PASO 1: Histograma
h ← CalcularHistograma(X)

// PASO 2: Probabilidad Acumulada (FP)
FP ← Arreglo(256) con valor 0
SumaAcumulada ← 0
Para k desde 0 hasta 255:
  Probabilidad ← h[k] / TotalPixeles
  SumaAcumulada ← SumaAcumulada + Probabilidad
  FP[k] ← SumaAcumulada
FinPara

// PASO 3: Mapeo Z' = 255 × FP[Z]
Para i desde 0 hasta N-1:
  Para j desde 0 hasta M-1:
    color_original ← X[i,j]
    nuevo_color ← EnteroAbajo(255 * FP[color_original])
    Y[i,j] ← nuevo_color
  FinPara
FinPara
Retornar Y
```

---

## 5. Operaciones Orientadas al Punto

| Operación | Fórmula |
|-----------|---------|
| Identidad | z' ← z |
| Negativo | z' ← 255 - z |
| Brillo | z' ← z + beta |
| Contraste | z' ← (z-a)×tan(α) + a |
| Corrección Gamma | z' ← L × (z/L)^γ |
| Posterización | z' ← 255 × floor(N×z/255) / (N-1) |
| Binarización | z < u → 0, else 255 |
| Escala de Grises | z' ← 0.3R + 0.59G + 0.11B |
| Estiramiento (Stretch) | z' ← 255 × (z-a)/(b-a) |

---

## 6. Morfología

```
FUNCIÓN: Dilatacion(Img, k)   [basta un blanco]
// Si ALGÚN vecino en ventana k×k es 255 → 255
```

```
FUNCIÓN: Erosion(Img, k)   [todos deben ser blancos]
// Si TODOS los vecinos en ventana k×k son 255 → 255
```

---

## 7. Operaciones Morfológicas Compuestas

```
Apertura(X)  ← Dilatacion(Erosion(X))    // Quita ruido sal/pimienta
Cierre(X)    ← Erosion(Dilatacion(X))    // Rellena huecos
Perimetro(X) ← Resta(X, Erosion(X))      // Extrae bordes
Limpieza(X)  ← Apertura(X)               // Limpieza estándar
```

---

## 8. Transformaciones Iterativas

```
FUNCIÓN: Relleno(B, S)   [necesita bordes B y semilla S]
Y ← S
Repetir:
  Y_ant ← Y
  Y ← Interseccion(Dilatacion(Y), Negacion(B))
Mientras (Y ≠ Y_ant)
Retornar Union(Y, B)
```

```
FUNCIÓN: Esqueletizacion(X)   [pela capa por capa]
Esqueleto ← 0
Mientras (X no esté vacía):
  Paso ← Resta(X, Apertura(X))
  Esqueleto ← Union(Esqueleto, Paso)
  X ← Erosion(X)
Retornar Esqueleto
```

---

## 9. Digitalización y Memoria

```
FUNCIÓN: CalcularTamano(N, M, k)
// N: Ancho, M: Alto, k: bits por píxel
TamanoBits ← N × M × k
Retornar TamanoBits
```

- Imágenes vectoriales: NO pierden calidad al escalar
- Mapas de bits: SÍ pierden calidad al escalar
- Tamaño en memoria depende de: ancho, alto, bits por píxel (k)

---

## 10. Teoría del Color

- **RGB**: Aditivo (pantallas). Suma de luz → blanco
- **CMYK**: Sustractivo (impresión). Pigmentos que absorben luz
- **HSV**: Perceptual (Matiz, Saturación, Brillo)

```
CONVERSIÓN RGB A HSV
Cmax ← Max(R,G,B), Cmin ← Min(R,G,B)
D ← Cmax - Cmin
V ← Cmax / 255
S ← D / Cmax (si Cmax=0 → S=0)
H ← 60×((G-B)/D) si Cmax=R
H ← 60×((B-R)/D)+120 si Cmax=G
H ← 60×((R-G)/D)+240 si Cmax=B
```

---

## 11. Tratamiento Digital y Almacenamiento

- **TDI**: 3 niveles — Bajo (mejora), Medio (segmenta), Alto (IA)
- **Histograma**: X = intensidad (0-255), Y = cantidad de píxeles
- **Formatos**: Sin pérdida (RAW, PNG), Con pérdida (JPEG)

---

## 12. Preguntas Resueltas

### Dibujar rectángulo negro
```pseudocode
Para y desde b hasta b+H-1:
  Para x desde a hasta a+W-1:
    Si x<N Y y<M: Img[x,y] ← 0
```

### Aislar un color (Filtro Condicional)
```pseudocode
Si C = c2: Img2[x,y] ← c2
Sino: Img2[x,y] ← c4
```

### Reducción de imagen (submuestreo)
```pseudocode
Para y: Para x:
  C ← Img1[x*5, y*5]
  Img2[x,y] ← C
```

### Espejo a -45° (Optimizado)
```pseudocode
Para y: Para x desde y:
  C1←Img1[x,y], C2←Img1[y,x]
  Img2[x,y]←C2, Img2[y,x]←C1
```

### Amplificación 1.73x (Mapeo Inverso)
```pseudocode
XX ← EnteroAbajo(x/1.73), YY ← EnteroAbajo(y/1.73)
Img2[x,y] ← Img1[XX,YY]
```

### OR lógico para incrustar
```pseudocode
C2_Negativo ← 255 - Img2[x,y]
Z ← C1 OR C2_Negativo
Img3[x,y] ← Z
```

---

## 13. Aplicación al Sistema OMR

### Algoritmo Inteligente de Detección de Cuadrícula

**Concepto**: Aplicar análisis de histograma a las POSICIONES espaciales de las burbujas (no solo a intensidades).

```
FUNCIÓN: DetectarCuadricula(burbujas, anchoImagen, altoImagen)
// PASO 1: Histograma de gaps en X
coordenadasX ← Ordenar(burbujas.map(.x))
gapsX ← []
Para i desde 1 hasta tamaño(coordenadasX)-1:
  gap ← coordenadasX[i] - coordenadasX[i-1]
  Si gap > 0: gapsX.agregar(gap)

// PASO 2: Clasificar gaps (pequeños=opciones, grandes=columnas)
// Usar umbral dinámico (percentil 75 o Otsu en 1D)
umbralX ← Mediana(gapsX) * 2.5

// PASO 3: Agrupar por columnas
columnas ← []
columnaActual ← [burbujas[0]]
Para i desde 1 hasta tamaño(burbujas)-1:
  Si |burbujas[i].x - columnaActual.promedioX| > umbralX:
    columnas.agregar(columnaActual)
    columnaActual ← [burbujas[i]]
  Sino:
    columnaActual.agregar(burbujas[i])

// PASO 4: Repetir con Y para filas
// (mismo algoritmo sobre coordenadas Y dentro de cada columna)

// PASO 5: Asignar preguntas
// Cada combinación (columna, fila) = una pregunta
// Opción más oscura (mayor relleno) = respuesta seleccionada
```

### Pipeline de Procesamiento (usando fórmulas del curso)

```
ImagenOriginal
  → Escala de Grises: z' = 0.3R + 0.59G + 0.11B
  → Ecualización: Z' = 255 × FP[Z] (usando histograma)
  → Binarización: z < u → 0, else 255 (umbral Otsu automático)
  → [Opcional] Apertura = Dilatacion(Erosion(X)) para quitar ruido
  → findContours → contornos
  → Filtrar por circularidad y área (usando histograma de áreas
    para calcular umbrales dinámicos en lugar de valores fijos)
  → DetectarCuadricula (histograma de gaps espaciales)
  → Extraer respuestas
```
