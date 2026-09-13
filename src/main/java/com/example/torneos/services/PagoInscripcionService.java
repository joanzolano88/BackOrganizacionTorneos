package com.example.torneos.services;

import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.PagoInscripcionDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.PagoInscripcion;
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

    public List<PagoInscripcion> listarPorEquipo(long idEquipo) {
        Equipo equipo = equipoDao.findById(idEquipo).orElse(null);
        if (equipo == null) {
            throw new IllegalArgumentException("El equipo no existe");
        }
        return pagoDao.findByEquipoOrderByFechaPagoDesc(equipo);
    }

    public PagoInscripcion registrar(long idEquipo, PagoInscripcion pago) {
        Equipo equipo = equipoDao.findById(idEquipo).orElse(null);
        if (equipo == null || equipo.getTorneo() == null) {
            throw new IllegalArgumentException("El equipo no tiene torneo asociado");
        }
        if (pago == null || pago.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto del pago no es válido");
        }
        if (pago.getUsuarioId() == null || equipo.getTorneo().getEncargadoTorneo() == null ||
                equipo.getTorneo().getEncargadoTorneo().getId() != pago.getUsuarioId() ||
                !usuarioDao.existsById(pago.getUsuarioId())) {
            throw new IllegalArgumentException("Solo el organizador del torneo puede registrar pagos");
        }
        long totalInscripcion = equipo.getTorneo().getValorInscripcion();
        long totalPagado = pagoDao.findByEquipoOrderByFechaPagoDesc(equipo).stream()
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
        PagoInscripcion guardado = pagoDao.save(pago);
        completarResumen(guardado, totalInscripcion, nuevoTotalPagado);
        return guardado;
    }

    private void completarResumen(PagoInscripcion pago, long totalInscripcion, long totalPagado) {
        pago.setTotalInscripcion(totalInscripcion);
        pago.setTotalPagado(totalPagado);
        pago.setSaldoPendiente(Math.max(totalInscripcion - totalPagado, 0));
    }
}