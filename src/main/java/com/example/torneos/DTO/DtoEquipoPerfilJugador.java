package com.example.torneos.DTO;

import lombok.Data;

@Data
public class DtoEquipoPerfilJugador {
    private long id;
    private String nombre;
    private byte[] escudo;
    private DtoTorneoPerfilJugador torneo;

    public DtoEquipoPerfilJugador(long id, String nombre, byte[] escudo, DtoTorneoPerfilJugador torneo) {
        this.id = id;
        this.nombre = nombre;
        this.escudo = escudo;
        this.torneo = torneo;
    }
}