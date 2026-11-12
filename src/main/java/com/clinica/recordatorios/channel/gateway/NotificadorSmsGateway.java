package com.clinica.recordatorios.channel.gateway;

/**
 * Abstraccion del proveedor externo de SMS, para que el canal no dependa de un proveedor concreto.
 */
public interface NotificadorSmsGateway {
    void enviarSms(String numeroDestino, String texto);
}
