package com.example.torneos.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Data;

@Data
@Entity
public class SolicitudJugadorEquipo {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Equipo equipo;
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Jugador jugador;
    private String estado = "PENDIENTE";
}