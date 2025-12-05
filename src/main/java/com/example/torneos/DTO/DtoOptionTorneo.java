package com.example.torneos.DTO;

import lombok.Data;

@Data
public class DtoOptionTorneo {
    private String label;
    private long value;

    public DtoOptionTorneo(String label, long value) {
        this.label = label;
        this.value = value;
    }
}
