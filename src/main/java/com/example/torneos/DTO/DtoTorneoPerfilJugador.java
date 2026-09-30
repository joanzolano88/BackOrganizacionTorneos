package com.example.torneos.DTO;

import lombok.Data;

@Data
public class DtoTorneoPerfilJugador {
    private long id;
    private String nombre;
    private String ubicacion;

    public DtoTorneoPerfilJugador(long id, String nombre, String ubicacion) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
    }
}