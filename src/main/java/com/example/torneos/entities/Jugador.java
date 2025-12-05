package com.example.torneos.entities;

import com.example.torneos.enums.EstadoJugador;
import com.example.torneos.enums.TipoAmonestacion;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Jugador extends InformacionPersona {
    private EstadoJugador estadoJugador;
    private int numeroCamiseta;
    private TipoAmonestacion amonestacionActual;
    private int cantidadTarjetasRojas;
    private int cantidadTarjetasAmarillas;
    @JoinColumn
    @ManyToOne
    private Equipo equipo;
}
