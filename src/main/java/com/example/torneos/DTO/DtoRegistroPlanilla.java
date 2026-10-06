package com.example.torneos.DTO;

import lombok.Data;

import java.util.List;

@Data
public class DtoRegistroPlanilla {
    private String nombreEquipo;
    private Perfil delegado;
    private List<Perfil> jugadores;

    @Data
    public static class Perfil {
        private String nombre;
        private String identificacion;
    }
}