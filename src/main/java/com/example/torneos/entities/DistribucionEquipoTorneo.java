package com.example.torneos.entities;

import com.example.torneos.enums.FaseActual;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class DistribucionEquipoTorneo {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    @JoinColumn(nullable = false)
    private Torneo torneo;
    @ManyToOne
    @JoinColumn(nullable = false)
    private Equipo equipo;
    @Enumerated(EnumType.STRING)
    private FaseActual fase;
    private int grupo;
}