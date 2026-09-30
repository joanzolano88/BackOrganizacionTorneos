package com.example.torneos.entities;

import com.example.torneos.enums.EstadoJugador;
import com.example.torneos.enums.TipoAmonestacion;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Jugador extends InformacionPersona {
    @EqualsAndHashCode.Include
    private long identityId() {
        return getId();
    }

    private EstadoJugador estadoJugador;
    private int numeroCamiseta;
    private TipoAmonestacion amonestacionActual;
    private int cantidadTarjetasRojas;
    private int cantidadTarjetasAmarillas;
    @JoinColumn
    @ManyToOne
    private Equipo equipo;
    @ManyToMany
    @JoinTable(name = "jugador_equipo",
            joinColumns = @JoinColumn(name = "jugador_id"),
            inverseJoinColumns = @JoinColumn(name = "equipo_id"))
    @JsonIgnore
    private Set<Equipo> equipos = new HashSet<>();
}
