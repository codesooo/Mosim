package com.example.mosim.data.mock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockPassengerProfilesTest {
    @Test
    fun registrationPreservesChoicesAndSeparatesSameNames() {
        val first = MockPassengerProfiles.createProfile(" 친구 ", "지인", " 뒷좌석 ", false, true, false)
        val second = MockPassengerProfiles.createProfile("친구", "가족", "조수석", true, false, true)
        assertEquals("친구", first.name)
        assertEquals("뒷좌석", first.seat)
        assertEquals("지인", first.relationship)
        assertEquals("Default", first.personalization)
        assertFalse(first.savePreferences)
        assertTrue(first.consent)
        assertFalse(first.adjustDuringTrip)
        assertNotEquals(first.id, second.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankNameCannotBeRegistered() {
        MockPassengerProfiles.createProfile("  ", "가족", "조수석", true, true, true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankSeatCannotBeRegistered() {
        MockPassengerProfiles.createProfile("친구", "지인", "  ", true, true, true)
    }
}
