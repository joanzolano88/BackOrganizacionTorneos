package com.example.torneos.DTO;

import lombok.Data;

@Data
public class DtoDelegadoPartido {
    private String numeroCelular;

    public DtoDelegadoPartido(String numeroCelular) {
        this.numeroCelular = numeroCelular;
    }
}