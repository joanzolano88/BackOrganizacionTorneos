package com.example.torneos.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"torneo_id", "jugador_id"}))
public class ParticipacionJugadorTorneo {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Torneo torneo;
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Jugador jugador;
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Equipo equipo;
    private boolean participando;
    private boolean jugoPartido;
    private boolean cambioEquipo;
    @ManyToOne
    private Equipo equipoAnterior;
}