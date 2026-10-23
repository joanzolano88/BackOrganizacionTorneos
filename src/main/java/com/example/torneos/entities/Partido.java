package com.example.torneos.entities;

import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModoRegistroEventoPartido;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Date;
import java.util.List;
@Data
@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Partido {
    @Id
    @GeneratedValue
    private long id;
    private int grupo;
    private long arbitrajeLocal;
    private long arbitrajeVisitante;
    private FaseActual faseEncuentro;
    private LocalDateTime fechaPartido;
    private int anotacionesEquipoLocal;
    private Date horaFinalPrimerTiempo;
    private Date horaInicioPrimerTiempo;
    private Date horaFinalSegundoTiempo;
    private EstadoPartido estadoPartido;
    private int tiempoExtraPrimerTiempo;
    private Date horaInicioSegundoTiempo;
    private int tiempoExtraSegundoTiempo;
    private int anotacionesEquipoVisitante;
    private int penaltisEquipoVisitante;
    private int penaltisEquipoLocal;
    private int llaveEliminatoria;
    @Enumerated(EnumType.STRING)
    private ModoRegistroEventoPartido modoRegistroEventos = ModoRegistroEventoPartido.SIMPLE;
    @JoinColumn
    @ManyToOne
    private Cancha cancha;
    @JoinColumn
    @ManyToOne
    private Equipo equipoLocal;
    @JoinColumn
    @ManyToOne
    private Equipo equipoVisitante;
    @JoinColumn
    @ManyToOne
    private Torneo torneo;
    @OneToMany(mappedBy = "partido", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("partido")
    private List<ConvocatoriaPartido> convocatorias;
    @Transient
    private List<Arbitro>listaArbitros;
}
