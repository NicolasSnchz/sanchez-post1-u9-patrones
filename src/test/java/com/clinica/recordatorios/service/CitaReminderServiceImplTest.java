package com.clinica.recordatorios.service;

import com.clinica.recordatorios.channel.ReminderChannel;
import com.clinica.recordatorios.channel.ReminderChannelFactory;
import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import com.clinica.recordatorios.repository.CitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaReminderServiceImplTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ReminderChannelFactory reminderChannelFactory;

    @Mock
    private ReminderChannel reminderChannelMock;

    @Mock
    private ReminderChannel canalSmsMock;

    @Mock
    private ReminderChannel canalUrgenteMock;

    // Fake: deliberadamente NO se mockea con Mockito. Clock.fixed(...) es una implementacion
    // real y completa de java.time.Clock, solo que con el instante congelado.
    private final Clock clockFijo =
        Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"));

    private CitaReminderServiceImpl citaReminderService;

    @BeforeEach
    void setUp() {
        citaReminderService = new CitaReminderServiceImpl(
            citaRepository, reminderChannelFactory, canalUrgenteMock, clockFijo);
    }

    @Test
    void enviarRecordatoriosPendientes_citaEnVentana_usaElCanalPreferido() {
        Cita cita = new Cita(1L, "Ana Gomez", "ana@correo.com",
            LocalDateTime.of(2026, 8, 10, 15, 0), CanalNotificacion.EMAIL, false);

        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
            .thenReturn(List.of(cita));
        when(reminderChannelFactory.obtenerCanal(CanalNotificacion.EMAIL))
            .thenReturn(reminderChannelMock);

        int enviados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(1, enviados);
        verify(reminderChannelMock, times(1)).enviar(eq(cita), anyString());
        verify(citaRepository, times(1)).save(cita);
        assertTrue(cita.isRecordatorioEnviado());
    }

    @Test
    void enviarRecordatoriosPendientes_dosCitasEnVentana_retornaCuantasNotificoYLasMarca() {
        // Aqui el repositorio y la fabrica son Stubs: solo entregan datos y la prueba revisa
        // el resultado (el conteo y el estado de las citas), no como se llamo a nadie.
        Cita primera = new Cita(7L, "Elena Mora", "elena@correo.com",
            LocalDateTime.of(2026, 8, 10, 11, 0), CanalNotificacion.EMAIL, false);
        Cita segunda = new Cita(8L, "Tomas Vega", "tomas@correo.com",
            LocalDateTime.of(2026, 8, 10, 17, 30), CanalNotificacion.EMAIL, false);
        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
            .thenReturn(List.of(primera, segunda));
        when(reminderChannelFactory.obtenerCanal(CanalNotificacion.EMAIL)).thenReturn(reminderChannelMock);

        int enviados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(2, enviados);
        assertTrue(primera.isRecordatorioEnviado());
        assertTrue(segunda.isRecordatorioEnviado());
    }

    @Test
    void enviarRecordatoriosPendientes_citasConCanalesDistintos_pideACadaUnaSuCanal() {
        Cita porCorreo = new Cita(2L, "Jorge Silva", "jorge@correo.com",
            LocalDateTime.of(2026, 8, 10, 16, 0), CanalNotificacion.EMAIL, false);
        Cita porSms = new Cita(3L, "Sara Lopez", "3001112233",
            LocalDateTime.of(2026, 8, 11, 8, 30), CanalNotificacion.SMS, false);

        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
            .thenReturn(List.of(porCorreo, porSms));
        when(reminderChannelFactory.obtenerCanal(CanalNotificacion.EMAIL)).thenReturn(reminderChannelMock);
        when(reminderChannelFactory.obtenerCanal(CanalNotificacion.SMS)).thenReturn(canalSmsMock);

        int enviados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(2, enviados);
        verify(reminderChannelFactory).obtenerCanal(CanalNotificacion.EMAIL);
        verify(reminderChannelFactory).obtenerCanal(CanalNotificacion.SMS);
        verify(reminderChannelMock).enviar(porCorreo, "Recordatorio: tiene una cita el 2026-08-10T16:00");
        verify(canalSmsMock).enviar(porSms, "Recordatorio: tiene una cita el 2026-08-11T08:30");
        verify(citaRepository, times(2)).save(any(Cita.class));
    }

    @Test
    void enviarRecordatoriosPendientes_ventanaDeterministaSegunClockFijo_consultaLosLimitesExactos() {
        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
            .thenReturn(List.of());

        citaReminderService.enviarRecordatoriosPendientes();

        ArgumentCaptor<LocalDateTime> desdeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hastaCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(citaRepository).findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(
            desdeCaptor.capture(), hastaCaptor.capture());

        // Con Clock.systemDefaultZone() esta asercion cambiaria en cada ejecucion;
        // con el Fake el resultado es exacto en cualquier maquina y a cualquier hora.
        assertEquals(LocalDateTime.of(2026, 8, 10, 9, 0), desdeCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 8, 11, 9, 0), hastaCaptor.getValue());
    }

    @Test
    void enviarRecordatoriosPendientes_sinCitasPendientes_noConsultaLaFactory() {
        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
            .thenReturn(List.of());

        int enviados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(0, enviados);
        verifyNoInteractions(reminderChannelFactory);
        verify(citaRepository, never()).save(any());
    }
}
