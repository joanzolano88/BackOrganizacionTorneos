package com.example.torneos.dao;

import com.example.torneos.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioDao extends JpaRepository<Usuario, Long> {
    Usuario findByNumeroCelular(String celular);
}
