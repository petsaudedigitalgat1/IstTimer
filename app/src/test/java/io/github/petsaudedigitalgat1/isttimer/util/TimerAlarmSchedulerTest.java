package io.github.petsaudedigitalgat1.isttimer.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.AlarmManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import io.github.petsaudedigitalgat1.isttimer.model.ActiveTimer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TimerAlarmSchedulerTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    private ShadowAlarmManager shadowAlarmManager() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return Shadows.shadowOf(alarmManager);
    }

    @Test
    public void scheduleTimerFinish_withFutureEndTime_registersAlarm() {
        long endTime = System.currentTimeMillis() + 60_000L;
        ActiveTimer timer = new ActiveTimer("id-1", "Ana", "HIV", 60_000L, true, 60_000L, endTime);

        TimerAlarmScheduler.scheduleTimerFinish(context, timer);

        ShadowAlarmManager.ScheduledAlarm scheduled = shadowAlarmManager().getNextScheduledAlarm();
        assertNotNull("um alarme deveria ter sido agendado", scheduled);
    }

    @Test
    public void scheduleTimerFinish_addsBackgroundBufferOnTopOfEndTime() {
        long endTime = System.currentTimeMillis() + 60_000L;
        ActiveTimer timer = new ActiveTimer("id-1", "Ana", "HIV", 60_000L, true, 60_000L, endTime);

        TimerAlarmScheduler.scheduleTimerFinish(context, timer);

        ShadowAlarmManager.ScheduledAlarm scheduled = shadowAlarmManager().getNextScheduledAlarm();
        assertNotNull(scheduled);
        // O alarme de fundo precisa disparar DEPOIS do fim real do timer (item 1 do
        // relatorio: esse atraso e o que da tempo do CountDownTimer em primeiro plano
        // cancelar o alarme antes dele tocar, evitando som duplicado).
        assertEquals(endTime + 3000L, scheduled.triggerAtTime);
    }

    @Test
    public void scheduleTimerFinish_withEndTimeAlreadyInThePast_doesNotScheduleAnything() {
        long pastEndTime = System.currentTimeMillis() - 60_000L;
        ActiveTimer timer = new ActiveTimer("id-1", "Ana", "HIV", 60_000L, true, 0L, pastEndTime);

        TimerAlarmScheduler.scheduleTimerFinish(context, timer);

        assertNull("nao deveria agendar alarme para um horario que ja passou",
                shadowAlarmManager().getNextScheduledAlarm());
    }

    @Test
    public void scheduleTimerFinish_withoutTimerId_doesNotScheduleAnything() {
        ActiveTimer timer = new ActiveTimer(
                "", "Ana", "HIV", 60_000L, true, 60_000L, System.currentTimeMillis() + 60_000L);

        TimerAlarmScheduler.scheduleTimerFinish(context, timer);

        assertNull(shadowAlarmManager().getNextScheduledAlarm());
    }

    @Test
    public void scheduleTimerFinish_withNullContextOrTimer_doesNotThrow() {
        TimerAlarmScheduler.scheduleTimerFinish(null, new ActiveTimer("Ana", "HIV", 60_000L));
        TimerAlarmScheduler.scheduleTimerFinish(context, null);
    }

    @Test
    public void cancelTimerFinish_removesAPreviouslyScheduledAlarm() {
        long endTime = System.currentTimeMillis() + 60_000L;
        ActiveTimer timer = new ActiveTimer("id-1", "Ana", "HIV", 60_000L, true, 60_000L, endTime);
        TimerAlarmScheduler.scheduleTimerFinish(context, timer);
        assertNotNull(shadowAlarmManager().getNextScheduledAlarm());

        TimerAlarmScheduler.cancelTimerFinish(context, timer);

        assertNull("cancelar deveria remover o alarme agendado para esse timer",
                shadowAlarmManager().getNextScheduledAlarm());
    }

    @Test
    public void cancelTimerFinish_whenNothingWasScheduled_doesNotThrow() {
        ActiveTimer timer = new ActiveTimer("id-1", "Ana", "HIV", 60_000L, false, 60_000L, 0L);

        TimerAlarmScheduler.cancelTimerFinish(context, timer);
    }

    @Test
    public void cancelTimerFinish_withNullContextOrTimer_doesNotThrow() {
        TimerAlarmScheduler.cancelTimerFinish(null, new ActiveTimer("Ana", "HIV", 60_000L));
        TimerAlarmScheduler.cancelTimerFinish(context, null);
    }
}
