package com.example.torneos.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Persona {
    @Id
    @SequenceGenerator(name = "persona_id_generator", sequenceName = "persona_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "persona_id_generator")
    private long id;
    private String nombre;
    @Basic(fetch = FetchType.LAZY)
    @Lob
    private byte[] foto;
    @Column(unique = true)
    private String identificacion;
    private String numeroCelular;
    private String numeroTelefono;
    private boolean whatsappActivo;
    private String correoElectronico;
    private LocalDate fechaNacimiento;
}
