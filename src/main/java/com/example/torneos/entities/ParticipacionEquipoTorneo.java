package com.example.torneos.entities;

import com.example.torneos.enums.FaseActual;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"equipo_id", "torneo_id"}))
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class ParticipacionEquipoTorneo {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Equipo equipo;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Torneo torneo;

    private String estado = "PENDIENTE";
    private int grupo;
    private FaseActual faseActual;
    private int puntos;
    private int partidosJugados;
    private int partidosGanados;
    private int partidosPerdidos;
    private int partidosEmpatados;
    private int golesFavor;
    private int golesContra;
}