package com.example.torneos.dao;

import com.example.torneos.DTO.DtoPago;
import com.example.torneos.entities.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PagoDao extends JpaRepository<Pago, Long> {
    @Query("select new com.example.torneos.DTO.DtoPago(p.id, p.valor, p.tipo, p.fecha, p.concepto, p.observacion, " +
            "t.id, t.nombre, e.id, e.nombre, delegado.numeroCelular, j.id, j.nombre, j.numeroCelular) " +
            "from Pago p join p.torneo t left join p.equipo e left join e.delegado delegado left join p.jugador j " +
            "where t.id = :torneoId order by p.fecha desc")
    List<DtoPago> listarDtoPorTorneo(@Param("torneoId") long torneoId);

    @Query("select new com.example.torneos.DTO.DtoPago(p.id, p.valor, p.tipo, p.fecha, p.concepto, p.observacion, " +
            "t.id, t.nombre, e.id, e.nombre, delegado.numeroCelular, j.id, j.nombre, j.numeroCelular) " +
            "from Pago p join p.torneo t left join p.equipo e left join e.delegado delegado left join p.jugador j " +
            "where p.id = :pagoId")
    DtoPago buscarDto(@Param("pagoId") long pagoId);
}