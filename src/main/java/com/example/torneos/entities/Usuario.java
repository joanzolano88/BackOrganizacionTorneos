package com.example.torneos.entities;

import com.example.torneos.enums.TipoUsuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Usuario extends PerfilPersona {
    private String contrasena;
    private TipoUsuario tipoUsuario;
    private String ubicacion;
    @Transient
    public LocalDate getFechaNacimiento() {
        return getPersona() == null ? null : getPersona().getFechaNacimiento();
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        if (getPersona() == null) setPersona(new Persona());
        getPersona().setFechaNacimiento(fechaNacimiento);
    }

    @JoinColumn
    @ManyToOne
    private Torneo torneo;
}
