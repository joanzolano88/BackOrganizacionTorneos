package com.example.torneos.DTO;

public record DtoSancionJugadorPartido(
        Long id,
        DtoJugador jugador,
        boolean activa,
        int partidosPendientes,
        boolean permanente,
        Long exencionPartidoId) {

    public record DtoJugador(Long id, String nombre) {}
}
