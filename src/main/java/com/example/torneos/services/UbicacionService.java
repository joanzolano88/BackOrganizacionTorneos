package com.example.torneos.services;

import com.example.torneos.dao.CiudadDao;
import com.example.torneos.dao.DepartamentoDao;
import com.example.torneos.dao.PaisDao;
import com.example.torneos.entities.Ciudad;
import com.example.torneos.entities.Departamento;
import com.example.torneos.entities.Pais;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UbicacionService {
    @Autowired
    private PaisDao paisDao;
    @Autowired
    private DepartamentoDao departamentoDao;
    @Autowired
    private CiudadDao ciudadDao;

    @PostConstruct
    public void inicializarUbicaciones() {
        if (paisDao.count() > 0) {
            return;
        }

        Pais colombia = new Pais();
        colombia.setNombre("Colombia");
        colombia = paisDao.save(colombia);

        Map<String, List<String>> departamentos = Map.of(
                "Antioquia", List.of("Medellín", "Envigado", "Itagüí"),
                "Valle del Cauca", List.of("Cali", "Palmira", "Tuluá"),
                "Cauca", List.of("Popayán", "Santander de Quilichao", "Puerto Tejada"),
                "Nariño", List.of("Pasto", "Ipiales", "Tumaco"),
                "Bogotá D.C.", List.of("Bogotá"),
                "Santander", List.of("Bucaramanga", "Floridablanca", "Girón")
        );

        for (Map.Entry<String, List<String>> entry : departamentos.entrySet()) {
            Departamento departamento = new Departamento();
            departamento.setNombre(entry.getKey());
            departamento.setPais(colombia);
            departamento = departamentoDao.save(departamento);

            for (String ciudadNombre : entry.getValue()) {
                Ciudad ciudad = new Ciudad();
                ciudad.setNombre(ciudadNombre);
                ciudad.setDepartamento(departamento);
                ciudadDao.save(ciudad);
            }
        }
    }

    public List<Pais> listarPaises() {
        return paisDao.findAll();
    }

    public List<Departamento> listarDepartamentosPorPais(Long paisId) {
        return departamentoDao.findByPaisId(paisId);
    }

    public List<Ciudad> listarCiudadesPorDepartamento(Long departamentoId) {
        return ciudadDao.findByDepartamentoId(departamentoId);
    }
}
