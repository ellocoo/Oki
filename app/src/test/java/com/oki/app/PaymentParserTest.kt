package com.oki.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentParserTest {

    @Test
    fun yapeRecibido_conNombre() {
        val p = PaymentParser.parse("Confirmación de Pago", "Pepito Pérez te envió un pago por S/ 1")
        assertNotNull(p)
        assertEquals(100L, p!!.amountCents)
        assertEquals("Pepito Pérez", p.sender)
    }

    @Test
    fun yapeConPrefijo() {
        val p = PaymentParser.parse("Yape", "¡Yape! Maria Lopez te yapeó S/ 10.50")
        assertEquals(1050L, p!!.amountCents)
        assertEquals("Maria Lopez", p.sender)
    }

    @Test
    fun recibisteDe() {
        val p = PaymentParser.parse("Plin", "Recibiste S/ 25.00 de Juan Perez")
        assertEquals(2500L, p!!.amountCents)
        assertEquals("Juan Perez", p.sender)
    }

    @Test
    fun montoConMiles() {
        val p = PaymentParser.parse("Pago", "Ana Ruiz te envió un pago por S/ 1,250.50")
        assertEquals(125050L, p!!.amountCents)
    }

    @Test
    fun montoConComaDecimal() {
        val p = PaymentParser.parse("Pago", "Ana Ruiz te envió un pago por S/ 10,50")
        assertEquals(1050L, p!!.amountCents)
    }

    @Test
    fun pagoEnviado_seIgnora() {
        assertNull(PaymentParser.parse("Yape", "Yapeaste S/ 5 a Carlos"))
    }

    @Test
    fun notificacionSinMonto_seIgnora() {
        assertNull(PaymentParser.parse("Yape", "Pepito te envió un mensaje"))
    }

    @Test
    fun vozMonto() {
        assertEquals("1 sol", PaymentParser.spokenAmount(100))
        assertEquals("5 soles", PaymentParser.spokenAmount(500))
        assertEquals("10 soles con 50 céntimos", PaymentParser.spokenAmount(1050))
        assertEquals("50 céntimos", PaymentParser.spokenAmount(50))
    }

    @Test
    fun vozFrase() {
        assertEquals(
            "Has recibido 1 sol de Pepito",
            PaymentParser.toSpeech(Payment(100, "Pepito"), true),
        )
        assertEquals(
            "Has recibido 1 sol",
            PaymentParser.toSpeech(Payment(100, "Pepito"), false),
        )
    }
}
