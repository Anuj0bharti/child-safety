package com.childsafety.watch

import com.childsafety.watch.sensors.FallDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FallDetectorTest {

    @Test
    fun testRestingGravityMagnitude() {
        // At rest on table: ax=0, ay=0, az=9.8
        val mag = FallDetector.computeMagnitude(0f, 0f, 9.8f)
        assertEquals(9.8f, mag, 0.01f)
    }

    @Test
    fun testFreeFallDropMagnitude() {
        // In free fall drop: ax=0.2, ay=0.3, az=0.1 -> magnitude < 3.0 m/s^2
        val mag = FallDetector.computeMagnitude(0.2f, 0.3f, 0.1f)
        assertTrue("Free fall magnitude should be below 3.0 m/s^2", mag < 3.0f)
    }

    @Test
    fun testImpactSpikeMagnitude() {
        // Violent ground impact: ax=15.0, ay=20.0, az=10.0 -> sqrt(225 + 400 + 100) = sqrt(725) ~ 26.9 m/s^2
        val mag = FallDetector.computeMagnitude(15.0f, 20.0f, 10.0f)
        assertTrue("Impact spike magnitude should exceed 25.0 m/s^2", mag > 25.0f)
    }

    @Test
    fun testFallPatternSequence() {
        val freeFallMag = 1.2f
        val impactMag = 28.5f
        val timeDiffMs = 400L // 400ms after free fall

        val isFall = FallDetector.isFallPattern(freeFallMag, impactMag, timeDiffMs)
        assertTrue("Should identify drop followed by sharp impact spike as fall pattern", isFall)
    }

    @Test
    fun testNormalRunningIsNotFall() {
        val runningSpike = 14.0f // Jogging step spike
        val isFall = FallDetector.isFallPattern(9.5f, runningSpike, 200L)
        assertFalse("Running step should not trigger fall pattern", isFall)
    }
}

