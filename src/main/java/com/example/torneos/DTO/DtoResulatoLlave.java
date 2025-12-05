package com.example.torneos.DTO;

import lombok.Data;

@Data
public class DtoResulatoLlave {
    private String resultadoGanador;
    private String resultadoPerdedor;
    private long idGanador;
    private long idPerdedor;
    private String nombreGanador;
    private String nombrePerdedor;
}
