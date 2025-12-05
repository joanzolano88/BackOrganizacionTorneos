package com.example.torneos.entities;

import com.example.torneos.enums.EstadoTorneo;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadFase;
import com.example.torneos.enums.ModalidadTorneo;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Torneo {
    @Id
    @GeneratedValue
    private Long id;
    private String nombre;
    private int cantidadGrupos;
    private int cantidadEquipos;
    private int cantidadGruposEliminatoriaGrupos;
    private int cantidadEquiposEliminatoriaGrupos;
    private long valorInscripcion;
    private FaseActual faseTorneo;
    private EstadoTorneo estadoTorneo;
    private ModalidadFase modalidadGrupos;
    private ModalidadTorneo modalidadTorneo;
    private FaseActual faseInicioEliminatorias;
    private ModalidadFase modalidadEliminatorias;
    private ModalidadFase modalidadEliminatoriasGrupos;
    @JoinColumn
    @ManyToOne
    private Usuario encargadoTorneo;
}
