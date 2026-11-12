package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.channel.gateway.NotificadorSmsGateway;
import com.clinica.recordatorios.domain.CanalNotificacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReminderChannelFactoryImplTest {

    private final EmailReminderChannel email = new EmailReminderChannel();
    private final SmsReminderChannel sms = new SmsReminderChannel(mock(NotificadorSmsGateway.class));
    private final ReminderChannelFactoryImpl factory = new ReminderChannelFactoryImpl(email, sms);

    @Test
    void obtenerCanal_email_retornaElCanalDeCorreo() {
        assertSame(email, factory.obtenerCanal(CanalNotificacion.EMAIL));
    }

    @Test
    void obtenerCanal_sms_retornaElCanalDeSms() {
        assertSame(sms, factory.obtenerCanal(CanalNotificacion.SMS));
    }

    @ParameterizedTest
    @EnumSource(CanalNotificacion.class)
    void obtenerCanal_cualquierCanalDelEnum_retornaUnaEstrategia(CanalNotificacion canal) {
        assertNotNull(factory.obtenerCanal(canal));
    }
}
