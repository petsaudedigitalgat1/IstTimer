package io.github.petsaudedigitalgat1.isttimer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ActiveTimerTest {

    @Test
    public void shortConstructor_startsStoppedWithFullTimeLeft() {
        ActiveTimer timer = new ActiveTimer("Maria", "HIV", 15 * 60 * 1000L);

        assertNotNull("timerId deveria ser gerado automaticamente", timer.timerId);
        assertFalse("timerId gerado nao deveria ser vazio", timer.timerId.isEmpty());
        assertEquals("Maria", timer.patientName);
        assertEquals("HIV", timer.testName);
        assertEquals(15 * 60 * 1000L, timer.durationInMillis);
        assertFalse("timer novo nao deveria comecar rodando", timer.isRunning);
        assertEquals(0L, timer.endTimeInMillis);
        assertEquals("tempo restante deveria comecar igual a duracao total",
                timer.durationInMillis, timer.timeLeftInMillis);
    }

    @Test
    public void shortConstructor_generatesDifferentIdsForDifferentInstances() {
        ActiveTimer first = new ActiveTimer("Maria", "HIV", 60_000L);
        ActiveTimer second = new ActiveTimer("Maria", "HIV", 60_000L);

        assertTrue("cada timer deveria receber um timerId unico",
                !first.timerId.equals(second.timerId));
    }

    @Test
    public void fullConstructor_preservesExactlyWhatWasPassed() {
        ActiveTimer timer = new ActiveTimer(
                "fixed-id-123",
                "Joao",
                "Sifilis",
                10 * 60 * 1000L,
                true,
                4 * 60 * 1000L,
                123456789L
        );

        assertEquals("fixed-id-123", timer.timerId);
        assertEquals("Joao", timer.patientName);
        assertEquals("Sifilis", timer.testName);
        assertEquals(10 * 60 * 1000L, timer.durationInMillis);
        assertTrue(timer.isRunning);
        assertEquals(4 * 60 * 1000L, timer.timeLeftInMillis);
        assertEquals(123456789L, timer.endTimeInMillis);
    }
}
