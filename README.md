# Oki

Oki lee en voz alta los pagos que recibes, por ejemplo: **"Has recibido 1 sol de Pepito"**, para que no tengas que sacar el teléfono a cada rato.

Solo funciona en **Android**. Todo se procesa dentro del teléfono: Oki no usa internet ni envía datos a ningún servidor.

## Cómo abrirlo

1. Instala [Android Studio](https://developer.android.com/studio).
2. Abre la carpeta `oki` con *File → Open* y espera a que termine el *Sync* de Gradle.
3. Conecta tu teléfono (con depuración USB activada) y pulsa **Run**.

## Cómo usarlo

1. Abre Oki y pulsa **Activar acceso a notificaciones**. Busca "Oki" en la lista y actívalo.
2. Pulsa **Evitar que Android cierre Oki** y permite que la app no se optimice. Sin esto, algunos teléfonos la cierran en segundo plano.
3. Pulsa **Probar voz** para confirmar que se escucha bien.
4. Listo: cuando llegue un pago de una app vigilada, Oki lo dirá en voz alta.

## Opciones

- Leer pagos en voz alta (activar/desactivar).
- Decir quién te pagó.
- Decir cuánto llevas en el día ("Llevas 85 soles hoy").

## Lo que debes verificar con notificaciones reales

Oki reconoce los pagos con patrones de texto (`PaymentParser.kt`). Están pensados para mensajes como "Pepito te envió un pago por S/ 1" o "Recibiste S/ 25.00 de Juan", pero **no se han probado con notificaciones reales de Yape o Plin**, y los bancos pueden cambiar el texto.

- En la pantalla principal, la sección **Últimas notificaciones** muestra los pagos detectados y las notificaciones "Ignoradas" de apps vigiladas, con su texto original. Si un pago real sale como "Ignorada", copia ese texto y ajusta los patrones en `PaymentParser.kt`.
- Los nombres de paquete de `MONITORED_PACKAGES` son: Yape (`com.bcp.innovacxion.yapeapp`) y los más probables para BCP, Interbank, BBVA y Scotiabank (Plin). Confírmalos en tu teléfono y agrega los de tu banco si faltan.

## Pruebas

`app/src/test/.../PaymentParserTest.kt` cubre el reconocimiento de montos, nombres y la frase en voz alta. Se ejecuta desde Android Studio (clic derecho → *Run*) o con `./gradlew test`.

## Ideas para la siguiente versión

- Varias billeteras con filtros por monto o por persona.
- Modo equipo: el dueño oye los pagos que recibe su empleado.
- Elegir voz y velocidad.
