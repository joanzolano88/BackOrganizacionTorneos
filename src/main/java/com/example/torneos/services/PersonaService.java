package com.example.torneos.services;

import com.example.torneos.dao.PersonaDao;
import com.example.torneos.entities.PerfilPersona;
import com.example.torneos.entities.Persona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Service
public class PersonaService {
    @Autowired
    private PersonaDao personaDao;

    @Transactional
    public Persona vincular(PerfilPersona perfil) {
        Persona datos = perfil.getPersona();
        if (datos == null) datos = new Persona();
        Persona persona = datos.getIdentificacion() == null || datos.getIdentificacion().isBlank()
                ? null
                : personaDao.findByIdentificacion(datos.getIdentificacion()).orElse(null);
        if (persona == null) {
            persona = datos;
        } else if (persona != datos) {
            copiarDatos(datos, persona);
        }
        perfil.setPersona(personaDao.save(persona));
        return perfil.getPersona();
    }

    private void copiarDatos(Persona origen, Persona destino) {
        if (origen.getNombre() != null) destino.setNombre(origen.getNombre());
        if (origen.getIdentificacion() != null) destino.setIdentificacion(origen.getIdentificacion());
        if (origen.getFoto() != null) destino.setFoto(origen.getFoto());
        if (origen.getNumeroCelular() != null) destino.setNumeroCelular(origen.getNumeroCelular());
        if (origen.getNumeroTelefono() != null) destino.setNumeroTelefono(origen.getNumeroTelefono());
        if (origen.getCorreoElectronico() != null) destino.setCorreoElectronico(origen.getCorreoElectronico());
        if (origen.getFechaNacimiento() != null) destino.setFechaNacimiento(origen.getFechaNacimiento());
        destino.setWhatsappActivo(origen.isWhatsappActivo());
    }

    public Persona save(Persona persona) {
        return personaDao.save(persona);
    }
    public List<Persona> getAll() {
        return personaDao.findAll();
    }
    public Persona getById(Long id) {
        Persona persona = personaDao.findById(id).orElse(null);
        if (persona.equals(null)) {
            throw  new IllegalArgumentException("La Persona no existe");
        }
        return persona;
    }
    public Persona update(Persona cancha) {
        Optional<Persona> optPersona = personaDao.findById(cancha.getId());
        if (!optPersona.isPresent()) {
            throw  new IllegalArgumentException("La Persona no existe");
        }
        Persona pesonaDB = optPersona.get();
        pesonaDB.setNombre(cancha.getNombre());
        return personaDao.save(pesonaDB);
    }
    public void delete(long id) {
        Optional<Persona> optCancha = personaDao.findById(id);
        if (optCancha.isPresent()) {
            personaDao.delete(optCancha.get());
        }
    }
}
