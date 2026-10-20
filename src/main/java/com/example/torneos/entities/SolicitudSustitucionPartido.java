package com.example.torneos.entities;

import com.example.torneos.enums.EstadoSolicitudSustitucion;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class SolicitudSustitucionPartido {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Partido partido;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Equipo equipo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Usuario solicitante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private ConvocatoriaPartido titular;

    @ManyToOne(fetch = FetchType.LAZY)
    private ConvocatoriaPartido suplente;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Jugador jugadorEntrante;

    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario resueltaPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitudSustitucion estado = EstadoSolicitudSustitucion.PENDIENTE;

    @Column(nullable = false)
    private int numeroUniforme;

    private Integer minuto;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    private LocalDateTime resueltaEn;
}
