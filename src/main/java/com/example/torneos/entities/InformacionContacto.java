package com.example.torneos.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class InformacionContacto {
    @Id
    @GeneratedValue
    private long id;
    @Column(unique = true)
    private String numeroCelular;
    @Column(unique = true)
    private String cedula;
    private String numeroTelefono;
    private boolean whatsappActivo;
    private String correoElectronico;
}
