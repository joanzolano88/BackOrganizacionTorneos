package com.example.torneos.services;

import com.example.torneos.dao.PersonaDao;
import com.example.torneos.entities.Persona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class PersonaService {
    @Autowired
    private PersonaDao personaDao;

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
