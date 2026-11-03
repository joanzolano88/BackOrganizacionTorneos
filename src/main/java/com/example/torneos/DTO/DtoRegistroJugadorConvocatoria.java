package com.example.torneos.DTO;

public record DtoRegistroJugadorConvocatoria(
        String nombre,
        String identificacion,
        boolean confirmarExistente) {
    public DtoRegistroJugadorConvocatoria(String nombre, String identificacion) {
        this(nombre, identificacion, false);
    }
}
