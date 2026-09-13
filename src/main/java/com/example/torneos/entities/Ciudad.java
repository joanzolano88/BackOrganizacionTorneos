package com.example.torneos.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Ciudad {
    @Id
    @GeneratedValue
    private long id;

    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Departamento departamento;
}
