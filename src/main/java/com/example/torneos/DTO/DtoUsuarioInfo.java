package com.example.torneos.DTO;

import com.example.torneos.enums.TipoUsuario;
import lombok.Data;

@Data
public class DtoUsuarioInfo {
    private long id;
    private TipoUsuario tipoUsuario;
}
