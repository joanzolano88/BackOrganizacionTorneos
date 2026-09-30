package com.example.torneos.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SancionJugadorTorneo {
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

    private String motivo;
    private int partidosPendientes;
    private boolean permanente;
    private boolean activa = true;
    private Long exencionPartidoId;
    private Long partidoOrigenId;
    private LocalDateTime creadaEn;
}