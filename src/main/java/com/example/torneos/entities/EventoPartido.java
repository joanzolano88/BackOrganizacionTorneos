package com.example.torneos.entities;

import com.example.torneos.enums.TipoEventoPartido;
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
public class EventoPartido {
    @Id
    @GeneratedValue
    private Long id;
    private TipoEventoPartido tipo;
    private int minuto;
    private LocalDateTime registradoEn;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Partido partido;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Jugador jugador;
}
