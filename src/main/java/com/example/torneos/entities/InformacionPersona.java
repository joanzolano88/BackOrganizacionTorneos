package com.example.torneos.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class InformacionPersona extends InformacionContacto{
    private String nombre;
    //@JoinColumn
    //@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Basic(fetch = FetchType.LAZY)
    @Lob
    private byte[]  foto;
    //@JoinColumn
    //@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    //@Basic(fetch = FetchType.LAZY)
    //@Lob
    @Column(unique = true)
    private String identificacion;
}
