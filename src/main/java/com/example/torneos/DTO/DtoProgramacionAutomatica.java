package com.example.torneos.DTO;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class DtoProgramacionAutomatica {
    private LocalDate fechaInicio;
    private LocalTime horaInicio = LocalTime.of(18, 0);
    private int partidosPorDia = 1;
    private List<Integer> diasSemana = List.of(2, 3, 4, 5, 6, 7, 1);
}
