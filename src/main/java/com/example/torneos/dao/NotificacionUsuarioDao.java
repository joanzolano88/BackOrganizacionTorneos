package com.example.torneos.dao;

import com.example.torneos.entities.NotificacionUsuario;
import com.example.torneos.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface NotificacionUsuarioDao extends JpaRepository<NotificacionUsuario, Long> {
    List<NotificacionUsuario> findByUsuarioOrderByCreadaEnDesc(Usuario usuario);
    List<NotificacionUsuario> findByUsuarioAndLeidaFalseOrderByCreadaEnDesc(Usuario usuario);
    Optional<NotificacionUsuario> findByIdAndUsuario(Long id, Usuario usuario);
    long countByUsuarioAndLeidaFalse(Usuario usuario);
    @Transactional
    void deleteByIdAndUsuario(Long id, Usuario usuario);
    @Transactional
    void deleteByUsuarioAndReferenciaTipoAndReferenciaId(Usuario usuario, String referenciaTipo, Long referenciaId);
    @Transactional
    void deleteByReferenciaTipoAndReferenciaId(String referenciaTipo, Long referenciaId);
}