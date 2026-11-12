# Recordatorios Citas Service

Post-contenido de la unidad 9 de Patrones de Diseño de Software (UDES). Nicolás Andrés Sánchez Villamizar.

Microservicio en Spring Boot que busca las citas médicas de las próximas 24 horas que todavía no tienen recordatorio y se lo envía al paciente por su canal preferido, correo o SMS. El servicio nace con **Strategy** (`ReminderChannel`) y **Factory Method** (`ReminderChannelFactory`) inyectados por constructor junto con un `Clock`, así que ninguna dependencia se construye dentro de la lógica de negocio.

## Parte 1: pruebas unitarias con JUnit 5 y Mockito

`CitaReminderServiceImplTest` aísla `CitaReminderServiceImpl` de sus tres colaboradores. Antes de escribir la prueba decidí qué doble usar para cada uno:

| Colaborador | Doble | Por qué |
|---|---|---|
| `CitaRepository` | Mock | No basta con que la consulta devuelva citas: la prueba tiene que verificar que `save()` se llama una vez por cita y con la cita ya marcada como enviada. Cuando hay que controlar la respuesta y además verificar la interacción, es un mock y no un stub. |
| `ReminderChannelFactory` y `ReminderChannel` | Mock | Lo que se prueba es la selección de la estrategia: que el servicio pida el canal que prefiere la cita y que ese canal reciba el mensaje. Eso solo se comprueba verificando interacciones, no con un valor de retorno. |
| `Clock` | Fake | `Clock.fixed(...)` es una implementación real y completa que trae el JDK. Mockearlo obligaría a simular `instant()` y `getZone()` por separado y arriesgarse a que `LocalDateTime.now(clock)` no se comporte como en producción. |

A la pregunta del Paso 5 («¿existe una implementación real y completa en la librería estándar que se comporte igual que en producción?») la respuesta es **sí** para el reloj y **no** para los otros dos, y eso es justo lo que separa el fake de los mocks.

## Parte 2: diagnóstico antes del refactor

`enviarRecordatorioUrgente(Long citaId)` funciona, pero no se puede probar en aislamiento. El problema está en la línea 52 de [`CitaReminderServiceImpl`](src/main/java/com/clinica/recordatorios/service/CitaReminderServiceImpl.java#L52):

```java
ReminderChannel canalUrgente = new SmsReminderChannel(new TwilioNotificadorSmsGateway());
```

El canal y el gateway se construyen con `new` dentro del método, así que la prueba no tiene ningún punto donde entregar un doble. No hay forma de simular que el proveedor de SMS falla para comprobar que la cita no queda marcada como enviada, y cada ejecución de la prueba intentaría mandar un SMS de verdad. `enviarRecordatoriosPendientes()` sí se puede probar porque recibe la fábrica por constructor y el canal sale de ella, no de un `new`. Se podría interceptar ese `new` con `Mockito.mockConstruction()`, pero eso esconde el problema de diseño en vez de resolverlo: lo que falta es aplicar inversión de dependencias.

## Ejecutar las pruebas

```bash
mvn test
```
