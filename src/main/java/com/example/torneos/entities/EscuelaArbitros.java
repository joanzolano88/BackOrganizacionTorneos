package com.example.torneos.entities;

import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class EscuelaArbitros extends InformacionContacto{
    private String nombre;
    private String direccion;
}
