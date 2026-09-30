package com.example.torneos.entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
public class Pago {
    @Id
    @GeneratedValue
    private long id;
    private long valor;
    @Enumerated(EnumType.STRING)
    private com.example.torneos.enums.TipoPago tipo;
    private LocalDate fecha;
    private String concepto;
    private String observacion;
    @Transient
    private Long usuarioId;
    @ManyToOne
    @JoinColumn
    @JsonIgnore
    private Usuario registradoPor;
    @JoinColumn
    @ManyToOne
    private Jugador jugador;
    @JoinColumn
    @ManyToOne
    private Equipo equipo;
    @JoinColumn
    @ManyToOne
    private Torneo torneo;

}
