package com.example.torneos.entities;

import com.example.torneos.enums.TipoUsuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Usuario extends InformacionPersona {
    private String nombreUsuario;
    private String contrasena;
    private TipoUsuario tipoUsuario;
    private String ubicacion;
    private LocalDate fechaNacimiento;
    @JoinColumn
    @OneToOne
    private Torneo torneo;
}
