package com.example.torneos.services;

import com.example.torneos.dao.ImagenDao;
import com.example.torneos.entities.Imagen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ImagenService {

    @Autowired
    private ImagenDao imagenDao;

    public Imagen save(Imagen imagen) {
        return imagenDao.save(imagen);
    }
    public Imagen getByIdTabla(Long id) {
        Optional<Imagen> optImagen = imagenDao.findById(id);
        if (optImagen.isEmpty()) {
            throw  new IllegalArgumentException("La Imagen no existe");
        }
        return optImagen.get();
    }
    public Imagen update(Imagen imagen) {
        Optional<Imagen> optImagen = imagenDao.findById(imagen.getId());
        if (optImagen.isEmpty()) {
            throw new IllegalArgumentException("La Imagen no existe");
        }
        optImagen.get().setImagen(imagen.getImagen());
        return imagenDao.save(optImagen.get());
    }
    public void delete(Long id) {
        Optional<Imagen> optImagen = imagenDao.findById(id);
        if (optImagen.isPresent()) {
            imagenDao.delete(optImagen.get());
        }
    }
}
