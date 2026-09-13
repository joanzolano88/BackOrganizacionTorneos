package com.example.torneos.entities;

import com.example.torneos.enums.EstadoPago;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class PagoInscripcion {
    @Id
    @GeneratedValue
    private Long id;
    private long monto;
    @Enumerated(EnumType.STRING)
    private EstadoPago estado;
    private LocalDate fechaPago;
    private String observacion;
    @jakarta.persistence.Transient
    private Long usuarioId;
    @jakarta.persistence.Transient
    private long totalInscripcion;
    @jakarta.persistence.Transient
    private long totalPagado;
    @jakarta.persistence.Transient
    private long saldoPendiente;
    @ManyToOne
    @JoinColumn(nullable = false)
    @JsonIgnore
    private Equipo equipo;
}