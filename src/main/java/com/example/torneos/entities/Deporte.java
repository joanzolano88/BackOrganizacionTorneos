package com.example.torneos.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;

@Data
@Entity
public class Deporte {
    @Id
    @GeneratedValue
    private Long id;
    private String nombre;
    private int minimoTitulares = 7;
    private int maximoTitulares = 11;
    private int maximoConvocados = 22;
    @Lob
    private String reglas;
}
