package com.example.torneos.DTO;

public record DtoConvocatoriaPartido(
        Long id,
        DtoJugadorConvocatoria jugador,
        DtoEquipoConvocatoria equipo,
        boolean titular,
        boolean fueTitular,
        boolean expulsado,
        int cambiosRealizados,
        Integer numeroUniforme,
        Integer posicionAlineacion) {

    public record DtoJugadorConvocatoria(
            Long id,
            String nombre,
            String identificacion,
            int numeroCamiseta,
            DtoEquipoConvocatoria equipo) {}

    public record DtoEquipoConvocatoria(Long id, String nombre) {}
}
