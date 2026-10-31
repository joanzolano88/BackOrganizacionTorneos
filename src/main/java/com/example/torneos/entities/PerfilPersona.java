package com.example.torneos.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@MappedSuperclass
public abstract class PerfilPersona {
    @Id
    @GeneratedValue
    private long id;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "persona_id")
    private Persona persona = new Persona();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @JsonIgnore
    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    @Transient
    public String getNombre() {
        return persona == null ? null : persona.getNombre();
    }

    public void setNombre(String nombre) {
        persona().setNombre(nombre);
    }

    @Transient
    public String getIdentificacion() {
        return persona == null ? null : persona.getIdentificacion();
    }

    public void setIdentificacion(String identificacion) {
        persona().setIdentificacion(identificacion);
    }

    @Transient
    public byte[] getFoto() {
        return persona == null ? null : persona.getFoto();
    }

    public void setFoto(byte[] foto) {
        persona().setFoto(foto);
    }

    @Transient
    public String getNumeroCelular() {
        return persona == null ? null : persona.getNumeroCelular();
    }

    public void setNumeroCelular(String numeroCelular) {
        persona().setNumeroCelular(numeroCelular);
    }

    @Transient
    public String getNumeroTelefono() {
        return persona == null ? null : persona.getNumeroTelefono();
    }

    public void setNumeroTelefono(String numeroTelefono) {
        persona().setNumeroTelefono(numeroTelefono);
    }

    @Transient
    public boolean isWhatsappActivo() {
        return persona != null && persona.isWhatsappActivo();
    }

    public void setWhatsappActivo(boolean whatsappActivo) {
        persona().setWhatsappActivo(whatsappActivo);
    }

    @Transient
    public String getCorreoElectronico() {
        return persona == null ? null : persona.getCorreoElectronico();
    }

    public void setCorreoElectronico(String correoElectronico) {
        persona().setCorreoElectronico(correoElectronico);
    }

    private Persona persona() {
        if (persona == null) persona = new Persona();
        return persona;
    }
}
