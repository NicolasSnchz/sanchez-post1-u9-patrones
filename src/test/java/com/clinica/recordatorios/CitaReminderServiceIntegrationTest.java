package com.clinica.recordatorios;

import com.clinica.recordatorios.channel.gateway.NotificadorSmsGateway;
import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import com.clinica.recordatorios.repository.CitaRepository;
import com.clinica.recordatorios.service.CitaReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class CitaReminderServiceIntegrationTest {

    @TestConfiguration
    static class ClockDeterministaConfig {
        // Se llama clockFijo y no clock: con el mismo nombre que el bean de ClockConfig,
        // Spring Boot rechaza la segunda definicion en lugar de reemplazarla.
        @Bean
        @Primary
        public Clock clockFijo() {
            return Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Autowired
    private CitaReminderService citaReminderService;

    @Autowired
    private CitaRepository citaRepository;

    @MockBean
    private NotificadorSmsGateway notificadorSmsGateway;

    @BeforeEach
    void limpiarBase() {
        citaRepository.deleteAll();
    }

    @Test
    void enviarRecordatoriosPendientes_contextoCompletoDeSpring_persisteYNotificaCitasEnVentana() {
        citaRepository.save(new Cita(null, "Carla Rios", "3011112222",
            LocalDateTime.of(2026, 8, 10, 18, 0), CanalNotificacion.SMS, false));

        int enviados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(1, enviados);
        verify(notificadorSmsGateway, times(1)).enviarSms(anyString(), anyString());
        assertTrue(citaRepository.findAll().get(0).isRecordatorioEnviado());
    }

    @Test
    void enviarRecordatoriosPendientes_citaPorCorreo_usaElCanalDeCorreoSinTocarElGatewayDeSms() {
        citaRepository.save(new Cita(null, "Diana Castro", "diana@correo.com",
            LocalDateTime.of(2026, 8, 10, 20, 0), CanalNotificacion.EMAIL, false));

        int enviados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(1, enviados);
        verifyNoInteractions(notificadorSmsGateway);
        assertTrue(citaRepository.findAll().get(0).isRecordatorioEnviado());
    }

    @Test
    void enviarRecordatorioUrgente_contextoCompletoDeSpring_enviaPorElCanalUrgenteDeLaConfiguracion() {
        Cita cita = citaRepository.save(new Cita(null, "Hugo Diaz", "3024445566",
            LocalDateTime.of(2026, 8, 10, 10, 30), CanalNotificacion.SMS, false));

        citaReminderService.enviarRecordatorioUrgente(cita.getId());

        verify(notificadorSmsGateway, times(1)).enviarSms(eq("3024445566"), contains("URGENTE"));
        assertTrue(citaRepository.findById(cita.getId()).orElseThrow().isRecordatorioEnviado());
    }
}
