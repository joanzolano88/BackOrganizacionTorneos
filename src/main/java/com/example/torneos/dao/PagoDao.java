package com.example.torneos.dao;

import com.example.torneos.entities.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoDao extends JpaRepository<Pago, Long> {
    List<Pago> findByTorneo_IdOrderByFechaDesc(long torneoId);
}