package com.example.torneos.DTO;

public record DtoConvocatoriaPartido(
        Long id,
        DtoJugadorConvocatoria jugador,
        DtoEquipoConvocatoria equipo,
        boolean titular,
        Integer numeroUniforme,
        Integer posicionAlineacion) {

    public record DtoJugadorConvocatoria(
            Long id,
            String nombre,
            String identificacion,
            int numeroCamiseta) {}

    public record DtoEquipoConvocatoria(Long id, String nombre) {}
}
