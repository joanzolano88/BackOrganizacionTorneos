package com.example.torneos.DTO;

import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.FaseActual;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DtoPartidoResumen {
    private long id;
    private int grupo;
    private FaseActual faseEncuentro;
    private LocalDateTime fechaPartido;
    private EstadoPartido estadoPartido;
    private int anotacionesEquipoLocal;
    private int anotacionesEquipoVisitante;
    private int penaltisEquipoLocal;
    private int penaltisEquipoVisitante;
    private DtoEquipoPartido equipoLocal;
    private DtoEquipoPartido equipoVisitante;
    private DtoCanchaPartido cancha;

    public DtoPartidoResumen(long id, int grupo, FaseActual faseEncuentro, LocalDateTime fechaPartido,
                             EstadoPartido estadoPartido, int anotacionesEquipoLocal,
                             int anotacionesEquipoVisitante, int penaltisEquipoLocal,
                             int penaltisEquipoVisitante, DtoEquipoPartido equipoLocal,
                             DtoEquipoPartido equipoVisitante, DtoCanchaPartido cancha) {
        this.id = id;
        this.grupo = grupo;
        this.faseEncuentro = faseEncuentro;
        this.fechaPartido = fechaPartido;
        this.estadoPartido = estadoPartido;
        this.anotacionesEquipoLocal = anotacionesEquipoLocal;
        this.anotacionesEquipoVisitante = anotacionesEquipoVisitante;
        this.penaltisEquipoLocal = penaltisEquipoLocal;
        this.penaltisEquipoVisitante = penaltisEquipoVisitante;
        this.equipoLocal = equipoLocal;
        this.equipoVisitante = equipoVisitante;
        this.cancha = cancha;
    }
}