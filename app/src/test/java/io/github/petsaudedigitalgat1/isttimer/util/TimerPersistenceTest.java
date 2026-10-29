package io.github.petsaudedigitalgat1.isttimer.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import io.github.petsaudedigitalgat1.isttimer.model.ActiveTimer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TimerPersistenceTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void loadActiveTimers_withNothingSavedYet_returnsEmptyList() {
        List<ActiveTimer> result = TimerPersistence.loadActiveTimers(context);

        assertEquals(0, result.size());
    }

    @Test
    public void saveAndLoad_roundTripsAllFieldsOfASingleTimer() {
        ActiveTimer original = new ActiveTimer(
                "id-1", "Ana", "Hepatite B", 20 * 60 * 1000L, true, 7 * 60 * 1000L, 999_888_777L);

        TimerPersistence.saveActiveTimers(context, java.util.Collections.singletonList(original));
        List<ActiveTimer> loaded = TimerPersistence.loadActiveTimers(context);

        assertEquals(1, loaded.size());
        ActiveTimer restored = loaded.get(0);
        assertEquals(original.timerId, restored.timerId);
        assertEquals(original.patientName, restored.patientName);
        assertEquals(original.testName, restored.testName);
        assertEquals(original.durationInMillis, restored.durationInMillis);
        assertEquals(original.isRunning, restored.isRunning);
        assertEquals(original.timeLeftInMillis, restored.timeLeftInMillis);
        assertEquals(original.endTimeInMillis, restored.endTimeInMillis);
    }

    @Test
    public void saveAndLoad_preservesOrderOfMultipleTimers() {
        ActiveTimer first = new ActiveTimer("Ana", "HIV", 60_000L);
        ActiveTimer second = new ActiveTimer("Bruno", "Sifilis", 120_000L);
        ActiveTimer third = new ActiveTimer("Carla", "Hepatite C", 180_000L);
        List<ActiveTimer> original = new ArrayList<>();
        original.add(first);
        original.add(second);
        original.add(third);

        TimerPersistence.saveActiveTimers(context, original);
        List<ActiveTimer> loaded = TimerPersistence.loadActiveTimers(context);

        assertEquals(3, loaded.size());
        assertEquals("Ana", loaded.get(0).patientName);
        assertEquals("Bruno", loaded.get(1).patientName);
        assertEquals("Carla", loaded.get(2).patientName);
    }

    @Test
    public void saveActiveTimers_withEmptyList_clearsPreviouslySavedTimers() {
        TimerPersistence.saveActiveTimers(context,
                java.util.Collections.singletonList(new ActiveTimer("Ana", "HIV", 60_000L)));
        assertEquals(1, TimerPersistence.loadActiveTimers(context).size());

        TimerPersistence.saveActiveTimers(context, new ArrayList<>());

        assertEquals(0, TimerPersistence.loadActiveTimers(context).size());
    }

    @Test
    public void saveActiveTimers_withNullContext_doesNotThrow() {
        TimerPersistence.saveActiveTimers(null, new ArrayList<>());
    }

    @Test
    public void loadActiveTimers_withNullContext_returnsEmptyListInsteadOfThrowing() {
        List<ActiveTimer> result = TimerPersistence.loadActiveTimers(null);

        assertEquals(0, result.size());
    }

    @Test
    public void loadActiveTimers_withCorruptedJson_returnsEmptyListInsteadOfCrashing() {
        SharedPreferences prefs = context.getSharedPreferences("isttimer_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("active_timers_json", "{not valid json[[[").apply();

        List<ActiveTimer> result = TimerPersistence.loadActiveTimers(context);

        assertEquals(0, result.size());
    }

    @Test
    public void saveActiveTimers_skipsNullEntriesInTheList() {
        List<ActiveTimer> withNullEntry = new ArrayList<>();
        withNullEntry.add(new ActiveTimer("Ana", "HIV", 60_000L));
        withNullEntry.add(null);

        TimerPersistence.saveActiveTimers(context, withNullEntry);
        List<ActiveTimer> loaded = TimerPersistence.loadActiveTimers(context);

        assertEquals(1, loaded.size());
        assertTrue(loaded.get(0).patientName.equals("Ana"));
    }
}
