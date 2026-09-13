package com.example.torneos.entities;

import com.example.torneos.enums.EstadoTorneo;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadFase;
import com.example.torneos.enums.ModalidadTorneo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Torneo {
    @Id
    @GeneratedValue
    private Long id;
    private String nombre;
    private String ubicacion;
    private int cantidadGrupos;
    private int cantidadEquipos;
    private int cantidadGruposEliminatoriaGrupos;
    private int cantidadEquiposEliminatoriaGrupos;
    private int duracionMinutos;
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
    @JoinColumn
    @ManyToOne
    private Ciudad ciudad;
    @JoinColumn
    @ManyToOne
    private Deporte deporte;
}
