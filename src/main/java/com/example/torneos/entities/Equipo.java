package com.example.torneos.entities;

import com.example.torneos.enums.FaseActual;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Equipo {
    @Id
    @GeneratedValue
    private long id;
    private int grupo;
    private int puntos;
    private String nombre;
    @Lob
    private byte[] escudo;
    @Lob
    private byte[] bandera;
    private int partidosJugados;
    private int partidosGanados;
    private int partidosPerdidos;
    private int partidosEmpatados;
    private FaseActual faseActual;
    private int anotacionesAFavor;
    private int anotacionesEnContra;
    private int grupoEliminatoria;
    private int puntosEliminatoria;
    private int partidosJugadosEliminatoria;
    private int partidosGanadosEliminatoria;
    private int partidosPerdidosEliminatoria;
    private int partidosEmpatadosEliminatoria;
    private int anotacionesAFavorEliminatoria;
    private int anotacionesEnContraEliminatoria;
    @JoinColumn
    @ManyToOne
    private Persona delegado;
    @JoinColumn
    @ManyToOne
    private Persona entrenador;
    @JoinColumn
    @ManyToOne
    private Torneo torneo;
    @Transient
    private List<Jugador> listaJugadoresActivos;
    @Transient
    private List<Jugador> listaJugadoresInactivos;
}
