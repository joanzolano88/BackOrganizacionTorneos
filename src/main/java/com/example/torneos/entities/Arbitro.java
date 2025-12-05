package com.example.torneos.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Arbitro extends InformacionPersona {
    @JoinColumn
    @ManyToOne
    private EscuelaArbitros escuelaArbitros;
}
