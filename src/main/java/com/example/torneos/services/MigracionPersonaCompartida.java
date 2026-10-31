package com.example.torneos.services;

import com.example.torneos.dao.PersonaDao;
import com.example.torneos.entities.Persona;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Component
public class MigracionPersonaCompartida implements ApplicationRunner {
    private static final List<String> DATOS_PERSONALES = List.of(
            "nombre", "foto", "identificacion", "numero_celular",
            "numero_telefono", "whatsapp_activo", "correo_electronico");
    private static final List<String> COLUMNAS_PERSONALES_LEGADAS = List.of(
            "nombre", "foto", "identificacion", "numero_celular",
            "numero_telefono", "whatsapp_activo", "correo_electronico", "fecha_nacimiento");

    private final JdbcTemplate jdbcTemplate;
    private final PersonaDao personaDao;

    public MigracionPersonaCompartida(JdbcTemplate jdbcTemplate, PersonaDao personaDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.personaDao = personaDao;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!columnExists("usuario", "persona_id") || !columnExists("jugador", "persona_id")) return;
        sincronizarSecuenciaPersona();
        eliminarUnicidadPersona("usuario");
        eliminarUnicidadPersona("jugador");
        migrarTabla("usuario", true, true);
        migrarTabla("jugador", false, false);
        permitirColumnasPersonalesLegadasNulas("usuario");
        permitirColumnasPersonalesLegadasNulas("jugador");
    }

    private void sincronizarSecuenciaPersona() {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS persona_id_seq START WITH 1 INCREMENT BY 1");
        jdbcTemplate.queryForObject(
                "SELECT setval('persona_id_seq', COALESCE((SELECT MAX(id) FROM persona), 0) + 1, false)",
                Long.class);
    }

    private void migrarTabla(String tabla, boolean usuario, boolean priorizarDatos) {
        String fechaNacimiento = usuario && columnExists(tabla, "fecha_nacimiento")
                ? "fecha_nacimiento" : "NULL AS fecha_nacimiento";
        String consulta = "SELECT id, persona_id, " + seleccionarColumnas(tabla) + ", " + fechaNacimiento +
                " FROM " + tabla + " WHERE persona_id IS NULL";
        List<Map<String, Object>> perfiles = jdbcTemplate.queryForList(consulta);
        for (Map<String, Object> perfil : perfiles) {
            Persona persona = obtenerPersona(perfil);
            completarPersona(perfil, persona, priorizarDatos);
            persona = personaDao.saveAndFlush(persona);
            jdbcTemplate.update("UPDATE " + tabla + " SET persona_id = ? WHERE id = ?",
                    persona.getId(), perfil.get("id"));
        }
    }

    private String seleccionarColumnas(String tabla) {
        return DATOS_PERSONALES.stream()
                .map(columna -> columnExists(tabla, columna) ? columna : "NULL AS " + columna)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private Persona obtenerPersona(Map<String, Object> perfil) {
        Number personaId = (Number) perfil.get("persona_id");
        if (personaId != null) {
            Persona relacionada = personaDao.findById(personaId.longValue()).orElse(null);
            if (relacionada != null) return relacionada;
        }
        String identificacion = (String) perfil.get("identificacion");
        if (identificacion != null && !identificacion.isBlank()) {
            Persona existente = personaDao.findByIdentificacion(identificacion).orElse(null);
            if (existente != null) return existente;
        }
        return new Persona();
    }

    private void completarPersona(Map<String, Object> datos, Persona persona, boolean priorizarDatos) {
        String nombre = (String) datos.get("nombre");
        String identificacion = (String) datos.get("identificacion");
        String numeroCelular = (String) datos.get("numero_celular");
        String numeroTelefono = (String) datos.get("numero_telefono");
        String correo = (String) datos.get("correo_electronico");
        Object foto = datos.get("foto");
        Object whatsapp = datos.get("whatsapp_activo");
        Object fecha = datos.get("fecha_nacimiento");

        if ((priorizarDatos || persona.getNombre() == null) && nombre != null) persona.setNombre(nombre);
        if ((priorizarDatos || persona.getIdentificacion() == null) && identificacion != null) persona.setIdentificacion(identificacion);
        if ((priorizarDatos || persona.getNumeroCelular() == null) && numeroCelular != null) persona.setNumeroCelular(numeroCelular);
        if ((priorizarDatos || persona.getNumeroTelefono() == null) && numeroTelefono != null) persona.setNumeroTelefono(numeroTelefono);
        if ((priorizarDatos || persona.getCorreoElectronico() == null) && correo != null) persona.setCorreoElectronico(correo);
        if ((priorizarDatos || persona.getFoto() == null) && foto instanceof byte[] imagen) persona.setFoto(imagen);
        if (priorizarDatos && whatsapp instanceof Boolean activo) persona.setWhatsappActivo(activo);
        if (fecha instanceof Date fechaNacimiento && (priorizarDatos || persona.getFechaNacimiento() == null)) {
            persona.setFechaNacimiento(fechaNacimiento.toLocalDate());
        }
    }

    private boolean columnExists(String tabla, String columna) {
        Integer cantidad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
                Integer.class, tabla, columna);
        return cantidad != null && cantidad > 0;
    }

    private void eliminarUnicidadPersona(String tabla) {
        List<String> restricciones = jdbcTemplate.queryForList(
                "SELECT tc.constraint_name FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "ON tc.constraint_name = kcu.constraint_name AND tc.constraint_schema = kcu.constraint_schema " +
                        "WHERE tc.table_schema = current_schema() AND tc.table_name = ? " +
                        "AND tc.constraint_type = 'UNIQUE' AND kcu.column_name = 'persona_id'",
                String.class, tabla);
        for (String restriccion : restricciones) {
            if (!restriccion.matches("[A-Za-z0-9_]+")) {
                throw new IllegalStateException("Nombre de restricción no válido durante la migración de personas");
            }
            jdbcTemplate.execute("ALTER TABLE " + tabla + " DROP CONSTRAINT " + restriccion);
        }
    }

    private void permitirColumnasPersonalesLegadasNulas(String tabla) {
        for (String columna : COLUMNAS_PERSONALES_LEGADAS) {
            if (columnExists(tabla, columna)) {
                jdbcTemplate.execute("ALTER TABLE " + tabla + " ALTER COLUMN " + columna + " DROP NOT NULL");
            }
        }
    }
}
