package com.example.torneos.DTO;

import lombok.Data;

@Data
public class DtoAyudanteTorneo {
    private long id;
    private long usuarioId;
    private String nombre;
    private String identificacion;

    public DtoAyudanteTorneo(long id, long usuarioId, String nombre, String identificacion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.identificacion = identificacion;
    }
}