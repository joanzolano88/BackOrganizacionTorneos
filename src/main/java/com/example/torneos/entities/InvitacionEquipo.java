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
public class InvitacionEquipo {
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
    private LocalDateTime creadaEn;
}