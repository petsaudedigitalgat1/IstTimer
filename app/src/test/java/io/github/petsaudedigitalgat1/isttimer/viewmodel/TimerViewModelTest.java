package io.github.petsaudedigitalgat1.isttimer.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import io.github.petsaudedigitalgat1.isttimer.model.ActiveTimer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TimerViewModelTest {

    private TimerViewModel viewModel;

    @Before
    public void setUp() {
        Application application = ApplicationProvider.getApplicationContext();
        viewModel = new TimerViewModel(application);
    }

    @Test
    public void newViewModel_startsWithNoActiveTimers() {
        assertEquals(0, viewModel.getActiveTimers().getValue().size());
    }

    @Test
    public void startNewTimer_addsAStoppedTimerWithFullDuration() {
        viewModel.startNewTimer("Ana", "HIV", 15);

        List<ActiveTimer> timers = viewModel.getActiveTimers().getValue();
        assertEquals(1, timers.size());
        ActiveTimer timer = timers.get(0);
        assertEquals("Ana", timer.patientName);
        assertEquals("HIV", timer.testName);
        assertEquals(15 * 60 * 1000L, timer.durationInMillis);
        assertEquals(15 * 60 * 1000L, timer.timeLeftInMillis);
        assertTrue("um timer recem-criado nao deveria comecar rodando sozinho", !timer.isRunning);
    }

    @Test
    public void startNewTimer_appendsWithoutRemovingExistingTimers() {
        viewModel.startNewTimer("Ana", "HIV", 15);
        viewModel.startNewTimer("Bruno", "Sifilis", 20);

        List<ActiveTimer> timers = viewModel.getActiveTimers().getValue();
        assertEquals(2, timers.size());
        assertEquals("Ana", timers.get(0).patientName);
        assertEquals("Bruno", timers.get(1).patientName);
    }

    @Test
    public void removeActiveTimer_byValidPosition_removesOnlyThatTimer() {
        viewModel.startNewTimer("Ana", "HIV", 15);
        viewModel.startNewTimer("Bruno", "Sifilis", 20);
        ActiveTimer toRemove = viewModel.getActiveTimers().getValue().get(0);

        viewModel.removeActiveTimer(toRemove, 0);

        List<ActiveTimer> timers = viewModel.getActiveTimers().getValue();
        assertEquals(1, timers.size());
        assertEquals("Bruno", timers.get(0).patientName);
    }

    @Test
    public void removeActiveTimer_withOutOfRangePosition_fallsBackToRemovingByReference() {
        viewModel.startNewTimer("Ana", "HIV", 15);
        viewModel.startNewTimer("Bruno", "Sifilis", 20);
        ActiveTimer toRemove = viewModel.getActiveTimers().getValue().get(1);

        viewModel.removeActiveTimer(toRemove, 99);

        List<ActiveTimer> timers = viewModel.getActiveTimers().getValue();
        assertEquals(1, timers.size());
        assertEquals("Ana", timers.get(0).patientName);
    }

    @Test
    public void removeActiveTimer_onEmptyList_doesNotThrow() {
        viewModel.removeActiveTimer(new ActiveTimer("Ana", "HIV", 60_000L), 0);

        assertEquals(0, viewModel.getActiveTimers().getValue().size());
    }

    @Test
    public void moveActiveTimerUp_swapsWithPreviousPosition() {
        viewModel.startNewTimer("Ana", "HIV", 15);
        viewModel.startNewTimer("Bruno", "Sifilis", 20);
        ActiveTimer bruno = viewModel.getActiveTimers().getValue().get(1);

        viewModel.moveActiveTimerUp(1);

        List<ActiveTimer> timers = viewModel.getActiveTimers().getValue();
        assertSame(bruno, timers.get(0));
        assertEquals("Bruno", timers.get(0).patientName);
        assertEquals("Ana", timers.get(1).patientName);
    }

    @Test
    public void moveActiveTimerUp_onFirstPosition_isANoOp() {
        viewModel.startNewTimer("Ana", "HIV", 15);
        viewModel.startNewTimer("Bruno", "Sifilis", 20);

        viewModel.moveActiveTimerUp(0);

        List<ActiveTimer> timers = viewModel.getActiveTimers().getValue();
        assertEquals("Ana", timers.get(0).patientName);
        assertEquals("Bruno", timers.get(1).patientName);
    }

    @Test
    public void moveActiveTimerDown_swapsWithNextPosition() {
        viewModel.startNewTimer("Ana", "HIV", 15);
        viewModel.startNewTimer("Bruno", "Sifilis", 20);

        viewModel.moveActiveTimerDown(0);

        List<ActiveTimer> timers = viewModel.getActiveTimers().getValue();
        assertEquals("Bruno", timers.get(0).patientName);
        assertEquals("Ana", timers.get(1).patientName);
    }

    @Test
    public void moveActiveTimerToStart_movesLastTimerToTheFront() {
        viewModel.startNewTimer("Ana", "HIV", 15);
        viewModel.startNewTimer("Bruno", "Sifilis", 20);
        viewModel.startNewTimer("Carla", "Hepatite C", 10);

        viewModel.moveActiveTimerToStart(2);

        List<ActiveTimer> timers = viewModel.getActiveTimers().getValue();
        assertEquals("Carla", timers.get(0).patientName);
        assertEquals("Ana", timers.get(1).patientName);
        assertEquals("Bruno", timers.get(2).patientName);
    }

    @Test
    public void moveActiveTimerToEnd_movesFirstTimerToTheBack() {
        viewModel.startNewTimer("Ana", "HIV", 15);
        viewModel.startNewTimer("Bruno", "Sifilis", 20);
        viewModel.startNewTimer("Carla", "Hepatite C", 10);

        viewModel.moveActiveTimerToEnd(0);

        List<ActiveTimer> timers = viewModel.getActiveTimers().getValue();
        assertEquals("Bruno", timers.get(0).patientName);
        assertEquals("Carla", timers.get(1).patientName);
        assertEquals("Ana", timers.get(2).patientName);
    }

    @Test
    public void moveActiveTimer_onEmptyList_doesNotThrow() {
        viewModel.moveActiveTimer(0, 2);

        assertEquals(0, viewModel.getActiveTimers().getValue().size());
    }

    @Test
    public void moveActiveTimer_withInvalidFromPosition_isIgnored() {
        viewModel.startNewTimer("Ana", "HIV", 15);

        viewModel.moveActiveTimer(5, 0);

        List<ActiveTimer> timers = viewModel.getActiveTimers().getValue();
        assertEquals(1, timers.size());
        assertEquals("Ana", timers.get(0).patientName);
    }
}
