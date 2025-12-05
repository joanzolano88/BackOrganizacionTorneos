package com.example.torneos.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Cancha {
    @Id
    @GeneratedValue
    private long id;
    private double largo;
    private double ancho;
    private String nombre;
    private double latitud;
    private double longitud;
    private String direccion;
    @JoinColumn
    @ManyToOne
    private Torneo torneo;
}
