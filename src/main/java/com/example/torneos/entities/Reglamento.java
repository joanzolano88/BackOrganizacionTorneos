package com.example.torneos.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Reglamento {
    @Id
    @GeneratedValue
    private Long id;
    @Lob
    private byte[] reglamento;
    @JoinColumn
    @OneToOne
    private Torneo torneo;
}
