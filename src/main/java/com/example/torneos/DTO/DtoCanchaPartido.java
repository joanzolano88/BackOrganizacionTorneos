package com.example.torneos.DTO;

import lombok.Data;

@Data
public class DtoCanchaPartido {
    private long id;
    private String nombre;
    private String direccion;

    public DtoCanchaPartido(long id, String nombre, String direccion) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
    }
}