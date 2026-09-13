package com.example.torneos.dao;

import com.example.torneos.entities.InvitacionPartido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitacionPartidoDao extends JpaRepository<InvitacionPartido, Long> {
    Optional<InvitacionPartido> findByToken(String token);
}
