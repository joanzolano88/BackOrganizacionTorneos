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
public class InvitacionPartido {
    @Id
    @GeneratedValue
    private Long id;
    private String token;
    private boolean usada;
    private LocalDateTime fechaExpiracion;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Partido partido;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Equipo equipo;
}
