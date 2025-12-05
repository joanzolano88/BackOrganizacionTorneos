package com.example.torneos.entities;

import com.example.torneos.enums.TipoAnotacion;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class AnotacionPartido {
    @Id
    @GeneratedValue
    private long id;
    private int minuto;
    private TipoAnotacion tipoAnotacion;
    @JoinColumn
    @ManyToOne
    private Jugador jugador;
    @JoinColumn
    @ManyToOne
    private Partido partido;
}
