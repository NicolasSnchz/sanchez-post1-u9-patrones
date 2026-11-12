package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.channel.gateway.NotificadorSmsGateway;
import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsReminderChannelTest {

    @Mock
    private NotificadorSmsGateway gateway;

    @InjectMocks
    private SmsReminderChannel canal;

    @Test
    void enviar_citaConTelefono_delegaEnElGatewayConElNumeroYElMensaje() {
        Cita cita = new Cita(4L, "Pedro Rojas", "3005556677",
            LocalDateTime.of(2026, 8, 10, 11, 0), CanalNotificacion.SMS, false);

        canal.enviar(cita, "Recordatorio de prueba");

        verify(gateway, times(1)).enviarSms("3005556677", "Recordatorio de prueba");
    }
}
