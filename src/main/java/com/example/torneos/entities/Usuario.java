package com.example.torneos.entities;

import com.example.torneos.enums.TipoUsuario;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Data
@Entity
public class Usuario extends InformacionPersona {
    private String nombreUsuario;
    private String contrasena;
    private TipoUsuario tipoUsuario;
    @JoinColumn
    @OneToOne
    private Torneo torneo;
}
