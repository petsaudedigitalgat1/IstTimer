package io.github.petsaudedigitalgat1.isttimer.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import io.github.petsaudedigitalgat1.isttimer.model.ActiveTimer;
import io.github.petsaudedigitalgat1.isttimer.receiver.TimerFinishedReceiver;
import io.github.petsaudedigitalgat1.isttimer.ui.MainActivity;

public final class TimerAlarmScheduler {

    public static final String EXTRA_TIMER_ID = "extra_timer_id";
    public static final String EXTRA_PATIENT_NAME = "extra_patient_name";
    public static final String EXTRA_TEST_NAME = "extra_test_name";

    /**
     * O alarme em background é só um plano B, para quando o app não está aberto para
     * rodar o CountDownTimer em primeiro plano. Esse atraso dá tempo do
     * CountDownTimer.onFinish() cancelar este alarme antes que ele dispare, evitando
     * som/vibração duplicados quando o app está em primeiro plano.
     */
    private static final long BACKGROUND_ALARM_BUFFER_MS = 3000L;

    private TimerAlarmScheduler() {
    }

    public static void scheduleTimerFinish(Context context, ActiveTimer timer) {
        if (context == null || timer == null || timer.timerId == null || timer.timerId.isEmpty()) {
            return;
        }

        long triggerAt = timer.endTimeInMillis + BACKGROUND_ALARM_BUFFER_MS;
        if (triggerAt <= System.currentTimeMillis()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(context, timer, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent showIntent = createShowIntent(context);

        /*
         * setAlarmClock() faz o sistema tratar isto como um despertador de verdade:
         * dispara no horario exato mesmo em Doze/App Standby (tela desligada) e nao
         * exige a permissao especial SCHEDULE_EXACT_ALARM do Android 12+.
         */
        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(triggerAt, showIntent);
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
    }

    public static void cancelTimerFinish(Context context, ActiveTimer timer) {
        if (context == null || timer == null || timer.timerId == null || timer.timerId.isEmpty()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(context, timer, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static PendingIntent createPendingIntent(Context context, ActiveTimer timer, int baseFlag) {
        Intent intent = new Intent(context, TimerFinishedReceiver.class);
        intent.putExtra(EXTRA_TIMER_ID, timer.timerId);
        intent.putExtra(EXTRA_PATIENT_NAME, timer.patientName);
        intent.putExtra(EXTRA_TEST_NAME, timer.testName);

        int flags = baseFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, timer.timerId.hashCode(), intent, flags);
    }

    private static PendingIntent createShowIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(context, 0, intent, flags);
    }
}
