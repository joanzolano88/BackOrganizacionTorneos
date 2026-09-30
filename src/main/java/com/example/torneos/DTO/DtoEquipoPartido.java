package com.example.torneos.DTO;

import lombok.Data;

@Data
public class DtoEquipoPartido {
    private long id;
    private String nombre;
    private byte[] escudo;
    private DtoDelegadoPartido delegado;

    public DtoEquipoPartido(long id, String nombre, byte[] escudo, DtoDelegadoPartido delegado) {
        this.id = id;
        this.nombre = nombre;
        this.escudo = escudo;
        this.delegado = delegado;
    }
}