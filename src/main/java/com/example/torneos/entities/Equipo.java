package com.example.torneos.entities;

import com.example.torneos.enums.FaseActual;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Equipo {
    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
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
    private Ciudad ciudad;
    @JoinColumn
    @ManyToOne
    private Torneo torneo;
    @Transient
    private List<Jugador> listaJugadoresActivos;
    @Transient
    private List<Jugador> listaJugadoresInactivos;
    @Transient
    private ParticipacionEquipoTorneo participacionTorneo;

    public Torneo getTorneo() {
        return participacionTorneo == null ? torneo : participacionTorneo.getTorneo();
    }

    public FaseActual getFaseActual() {
        return participacionTorneo == null ? faseActual : participacionTorneo.getFaseActual();
    }

    public int getGrupo() {
        return participacionTorneo == null ? grupo : participacionTorneo.getGrupo();
    }

    public int getPuntos() {
        return participacionTorneo == null ? puntos : participacionTorneo.getPuntos();
    }

    public int getPartidosJugados() {
        return participacionTorneo == null ? partidosJugados : participacionTorneo.getPartidosJugados();
    }

    public int getPartidosGanados() {
        return participacionTorneo == null ? partidosGanados : participacionTorneo.getPartidosGanados();
    }

    public int getPartidosPerdidos() {
        return participacionTorneo == null ? partidosPerdidos : participacionTorneo.getPartidosPerdidos();
    }

    public int getPartidosEmpatados() {
        return participacionTorneo == null ? partidosEmpatados : participacionTorneo.getPartidosEmpatados();
    }

    public int getAnotacionesAFavor() {
        return participacionTorneo == null ? anotacionesAFavor : participacionTorneo.getGolesFavor();
    }

    public int getAnotacionesEnContra() {
        return participacionTorneo == null ? anotacionesEnContra : participacionTorneo.getGolesContra();
    }
}
