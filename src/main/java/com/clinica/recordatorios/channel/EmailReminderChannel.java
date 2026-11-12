package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.domain.Cita;
import org.springframework.stereotype.Component;

@Component
public class EmailReminderChannel implements ReminderChannel {

    @Override
    public void enviar(Cita cita, String mensaje) {
        // En produccion: cliente SMTP real. Las pruebas no verifican el transporte,
        // sino que el servicio selecciono y uso el canal correcto.
        System.out.printf("[EMAIL a %s] %s%n", cita.getPacienteContacto(), mensaje);
    }
}
