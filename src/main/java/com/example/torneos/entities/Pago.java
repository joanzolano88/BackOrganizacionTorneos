package com.example.torneos.entities;

import com.example.torneos.enums.TipoAmonestacion;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
public class Pago {
    @Id
    @GeneratedValue
    private long id;
    private long valor;
    private Date fecha;
    private TipoAmonestacion tipoAmonestacion;
    @JoinColumn
    @ManyToOne
    private Jugador jugador;
    @JoinColumn
    @ManyToOne
    private Equipo equipo;
    @JoinColumn
    @ManyToOne
    private Torneo torneo;

}
