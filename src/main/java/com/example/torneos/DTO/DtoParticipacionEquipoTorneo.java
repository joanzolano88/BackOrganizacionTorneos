package com.example.torneos.DTO;

import lombok.Data;

@Data
public class DtoParticipacionEquipoTorneo {
    private Long id;
    private String estado;
    private TorneoInfo torneo;

    public DtoParticipacionEquipoTorneo(Long id, String estado, Long torneoId, String torneoNombre) {
        this.id = id;
        this.estado = estado;
        this.torneo = new TorneoInfo(torneoId, torneoNombre);
    }

    @Data
    public static class TorneoInfo {
        private Long id;
        private String nombre;

        public TorneoInfo(Long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }
}
