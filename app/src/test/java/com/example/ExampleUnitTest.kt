package com.example

import com.example.network.NearbyUser
import com.example.network.TransportType
import com.example.network.WireMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun testWireMessageSerialization() {
        val original = WireMessage(
            id = "test-msg-123",
            type = WireMessage.TYPE_CHAT,
            sender = "@cyber_runner",
            text = "Hello from Nearby Chat!",
            target = "@neon_pulse",
            timestamp = 1700000000000L
        )

        val json = original.toJson()
        assertTrue(json.contains("@cyber_runner"))
        assertTrue(json.contains("Hello from Nearby Chat!"))

        val parsed = WireMessage.fromJson(json)
        assertNotNull(parsed)
        assertEquals(original.id, parsed?.id)
        assertEquals(original.type, parsed?.type)
        assertEquals(original.sender, parsed?.sender)
        assertEquals(original.text, parsed?.text)
        assertEquals(original.target, parsed?.target)
        assertEquals(original.timestamp, parsed?.timestamp)
    }

    @Test
    fun testBuzzMessageSerialization() {
        val buzz = WireMessage(
            id = "buzz-456",
            type = WireMessage.TYPE_BUZZ,
            sender = "@aura_seeker",
            text = "⚡ Incoming Buzz!",
            target = ""
        )
        val json = buzz.toJson()
        val parsed = WireMessage.fromJson(json)
        assertNotNull(parsed)
        assertEquals(WireMessage.TYPE_BUZZ, parsed?.type)
        assertEquals("@aura_seeker", parsed?.sender)
    }

    @Test
    fun testTransportTypes() {
        assertEquals("Bluetooth", TransportType.BLUETOOTH.displayName)
        assertEquals("Wi-Fi Direct", TransportType.WIFI_DIRECT.displayName)
        assertEquals("Hotspot / Wi-Fi", TransportType.HOTSPOT.displayName)
        assertTrue(TransportType.MOBILE_DATA.displayName.contains("Mobile Data"))
    }

    @Test
    fun testNearbyUserModel() {
        val user = NearbyUser(
            username = "@stella_sky",
            displayName = "Stella Sky",
            transport = TransportType.MOBILE_DATA,
            address = "cell:stella",
            proximityDescription = "Long-Range (Mobile Data)",
            signalStrength = 95,
            isOnline = true
        )
        assertEquals("@stella_sky", user.username)
        assertEquals(TransportType.MOBILE_DATA, user.transport)
        assertTrue(user.isOnline)
        assertEquals(95, user.signalStrength)
    }
}
