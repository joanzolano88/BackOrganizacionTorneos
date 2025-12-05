package com.example.torneos.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Imagen {
    @Id
    private Long id;
    @Lob
    private byte[] imagen;
}
