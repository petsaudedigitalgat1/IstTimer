package io.github.petsaudedigitalgat1.isttimer.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.app.PendingIntent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import io.github.petsaudedigitalgat1.isttimer.R;
import io.github.petsaudedigitalgat1.isttimer.model.ActiveTimer;
import io.github.petsaudedigitalgat1.isttimer.util.TimerAlarmScheduler;
import io.github.petsaudedigitalgat1.isttimer.util.TimerPersistence;

import java.util.List;

public class TimerFinishedReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "timer_finished_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        /*
         * O sistema so garante a CPU acordada durante a execucao deste onReceive().
         * Como o toque do alarme (AlarmPlayer) e assincrono e continua depois que
         * este metodo retorna, sem um wake lock o aparelho pode voltar a dormir com
         * a tela desligada antes do som comecar/terminar de tocar.
         */
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, "isttimer:alarm_finished_wake_lock");
            wakeLock.acquire(15_000L);
        }

        String timerId = intent.getStringExtra(TimerAlarmScheduler.EXTRA_TIMER_ID);
        String patientName = intent.getStringExtra(TimerAlarmScheduler.EXTRA_PATIENT_NAME);
        String testName = intent.getStringExtra(TimerAlarmScheduler.EXTRA_TEST_NAME);

        if (timerId == null || timerId.isEmpty()) {
            return;
        }

        List<ActiveTimer> timers = TimerPersistence.loadActiveTimers(context.getApplicationContext());
        ActiveTimer target = null;
        long now = System.currentTimeMillis();

        for (ActiveTimer timer : timers) {
            if (timerId.equals(timer.timerId)) {
                target = timer;
                break;
            }
        }

        if (target == null) {
            return;
        }

        if (!target.isRunning && target.timeLeftInMillis == 0) {
            return;
        }

        if (target.endTimeInMillis > now + 1500) {
            return;
        }

        target.isRunning = false;
        target.timeLeftInMillis = 0;
        target.endTimeInMillis = 0;
        TimerPersistence.saveActiveTimers(context.getApplicationContext(), timers);

        playDefaultAlarmTone(context.getApplicationContext(), timerId);

        createNotificationChannel(context);

        String title = context.getString(R.string.status_time_expired);
        String content = patientName + " - " + testName;

        // Intent para ação de parada do alarme
        Intent stopIntent = new Intent(context, StopAlarmReceiver.class);
        stopIntent.putExtra(TimerAlarmScheduler.EXTRA_TIMER_ID, timerId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent stopPending = PendingIntent.getBroadcast(context, timerId.hashCode() ^ 0x7fffffff, stopIntent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE);

        // Adiciona ação de parar
        builder.addAction(R.drawable.ic_launcher, "Parar", stopPending);

        NotificationManagerCompat.from(context).notify(timerId.hashCode(), builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Timers finalizados",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notificações quando os timers acabam");
        channel.enableVibration(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void playDefaultAlarmTone(Context context, String timerId) {
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        if (alarmUri == null) return;

        // Delega a reprodução ao AlarmPlayer para controle externo (parar via ação)
        io.github.petsaudedigitalgat1.isttimer.util.AlarmPlayer.play(context, timerId, alarmUri);
    }
}
