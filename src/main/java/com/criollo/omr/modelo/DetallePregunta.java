package com.criollo.omr.modelo;

/**
 * Detalle de una pregunta en la calificación.
 */
public class DetallePregunta {
    private final int numeroPregunta;
    private final char respuestaCorrecta;
    private final char respuestaAlumno;
    private final boolean esCorrecta;

    public DetallePregunta(int numeroPregunta, char respuestaCorrecta, char respuestaAlumno) {
        this.numeroPregunta = numeroPregunta;
        this.respuestaCorrecta = respuestaCorrecta;
        this.respuestaAlumno = respuestaAlumno;
        this.esCorrecta = (respuestaCorrecta == respuestaAlumno);
    }

    public int getNumeroPregunta() { return numeroPregunta; }
    public char getRespuestaCorrecta() { return respuestaCorrecta; }
    public char getRespuestaAlumno() { return respuestaAlumno; }
    public boolean isEsCorrecta() { return esCorrecta; }
}
