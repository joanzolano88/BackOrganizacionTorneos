package com.example.torneos.services;

import com.example.torneos.dao.CanchaDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.entities.Cancha;
import com.example.torneos.entities.Torneo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CanchaService {
    @Autowired
    private CanchaDao canchaDao;
    @Autowired
    private TorneoDao torneoDao;

    public Cancha save(Cancha cancha) {
        if (cancha.getTorneo().equals(null)) {
            throw  new IllegalArgumentException("Torneo nulo");
        }
        return canchaDao.save(cancha);
    }
    public List<Cancha> getAll() {
        return canchaDao.findAll();
    }
    public Cancha getById(Long id) {
        Cancha cancha = canchaDao.findById(id).orElse(null);
        if (cancha.equals(null)) {
            throw  new IllegalArgumentException("La Cancha no existe");
        }
        return cancha;
    }
    public List<Cancha> getByTorneo(long id) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo.equals(null)) {
            throw  new IllegalArgumentException("El Torneo no existe");
        }
        List<Cancha> canchaList = canchaDao.findByTorneo(torneo);
        return canchaList;
    }
    public Cancha update(Cancha cancha) {
        Optional<Cancha> optCancha = canchaDao.findById(cancha.getId());
        if (!optCancha.isPresent()) {
            throw  new IllegalArgumentException("La Cancha no existe");
        }
        Cancha canchaDB = optCancha.get();
        canchaDB.setAncho(cancha.getAncho());
        canchaDB.setLargo(cancha.getLargo());
        canchaDB.setNombre(cancha.getNombre());
        canchaDB.setLatitud(cancha.getLatitud());
        canchaDB.setLongitud(cancha.getLongitud());
        canchaDB.setDireccion(cancha.getDireccion());
        return canchaDao.save(canchaDB);
    }
    public void delete(long id) {
        Optional<Cancha> optCancha = canchaDao.findById(id);
        if (optCancha.isPresent()) {
            canchaDao.delete(optCancha.get());
        }
    }
}
