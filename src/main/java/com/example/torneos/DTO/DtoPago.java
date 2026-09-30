package com.example.torneos.DTO;

import com.example.torneos.enums.TipoPago;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DtoPago {
    private Long id;
    private long valor;
    private TipoPago tipo;
    private LocalDate fecha;
    private String concepto;
    private String observacion;
    private Long torneoId;
    private String torneoNombre;
    private Long equipoId;
    private String equipoNombre;
    private String delegadoNumeroCelular;
    private Long jugadorId;
    private String jugadorNombre;
    private String jugadorNumeroCelular;

    public DtoPago(Long id, long valor, TipoPago tipo, LocalDate fecha, String concepto, String observacion,
                   Long torneoId, String torneoNombre, Long equipoId, String equipoNombre,
                   String delegadoNumeroCelular, Long jugadorId, String jugadorNombre,
                   String jugadorNumeroCelular) {
        this.id = id;
        this.valor = valor;
        this.tipo = tipo;
        this.fecha = fecha;
        this.concepto = concepto;
        this.observacion = observacion;
        this.torneoId = torneoId;
        this.torneoNombre = torneoNombre;
        this.equipoId = equipoId;
        this.equipoNombre = equipoNombre;
        this.delegadoNumeroCelular = delegadoNumeroCelular;
        this.jugadorId = jugadorId;
        this.jugadorNombre = jugadorNombre;
        this.jugadorNumeroCelular = jugadorNumeroCelular;
    }
}