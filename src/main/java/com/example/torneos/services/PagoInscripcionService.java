package com.example.torneos.services;

import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.PagoInscripcionDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.PagoInscripcion;
import com.example.torneos.entities.ParticipacionEquipoTorneo;
import com.example.torneos.entities.Torneo;
import com.example.torneos.dao.ParticipacionEquipoTorneoDao;
import com.example.torneos.enums.EstadoPago;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PagoInscripcionService {
    @Autowired
    private PagoInscripcionDao pagoDao;
    @Autowired
    private EquipoDao equipoDao;
    @Autowired
    private UsuarioDao usuarioDao;
    @Autowired
    private ParticipacionEquipoTorneoDao participacionEquipoDao;
    @Autowired
    private TorneoDao torneoDao;

    public List<PagoInscripcion> listarPorEquipo(long idEquipo, long idTorneo) {
        Equipo equipo = equipoDao.findById(idEquipo).orElse(null);
        if (equipo == null) {
            throw new IllegalArgumentException("El equipo no existe");
        }
        Torneo torneo = torneo(idTorneo);
        validarParticipacionAceptada(equipo, torneo);
        return pagoDao.findByEquipoAndTorneoOrderByFechaPagoDesc(equipo, torneo);
    }

    public PagoInscripcion registrar(long idEquipo, long idTorneo, PagoInscripcion pago) {
        Equipo equipo = equipoDao.findById(idEquipo).orElse(null);
        if (equipo == null) {
            throw new IllegalArgumentException("El equipo no existe");
        }
        Torneo torneo = torneo(idTorneo);
        validarParticipacionAceptada(equipo, torneo);
        if (pago == null || pago.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto del pago no es válido");
        }
        if (pago.getUsuarioId() == null || torneo.getEncargadoTorneo() == null ||
            torneo.getEncargadoTorneo().getId() != pago.getUsuarioId() ||
                !usuarioDao.existsById(pago.getUsuarioId())) {
            throw new IllegalArgumentException("Solo el organizador del torneo puede registrar pagos");
        }
        long totalInscripcion = torneo.getValorInscripcion();
        long totalPagado = pagoDao.findByEquipoAndTorneoOrderByFechaPagoDesc(equipo, torneo).stream()
                .filter(pagoRegistrado -> pagoRegistrado.getEstado() != EstadoPago.PENDIENTE)
                .mapToLong(PagoInscripcion::getMonto)
                .sum();
        long nuevoTotalPagado = totalPagado + pago.getMonto();
        if (nuevoTotalPagado > totalInscripcion) {
            throw new IllegalArgumentException("El pago supera el valor total de la inscripción");
        }
        pago.setEstado(nuevoTotalPagado == totalInscripcion ? EstadoPago.PAGADO : EstadoPago.PARCIAL);
        if (pago.getFechaPago() == null) {
            pago.setFechaPago(LocalDate.now());
        }
        pago.setEquipo(equipo);
        pago.setTorneo(torneo);
        PagoInscripcion guardado = pagoDao.save(pago);
        completarResumen(guardado, totalInscripcion, nuevoTotalPagado);
        return guardado;
    }

    private Torneo torneo(long idTorneo) {
        return torneoDao.findById(idTorneo)
            .orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
    }

    private void validarParticipacionAceptada(Equipo equipo, Torneo torneo) {
        ParticipacionEquipoTorneo participacion = participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)
                .orElseThrow(() -> new IllegalArgumentException("El equipo no participa en este torneo"));
        if (!"ACEPTADO".equals(participacion.getEstado())) {
            throw new IllegalArgumentException("La participación del equipo aún no fue aceptada");
        }
    }

    private void completarResumen(PagoInscripcion pago, long totalInscripcion, long totalPagado) {
        pago.setTotalInscripcion(totalInscripcion);
        pago.setTotalPagado(totalPagado);
        pago.setSaldoPendiente(Math.max(totalInscripcion - totalPagado, 0));
    }
}