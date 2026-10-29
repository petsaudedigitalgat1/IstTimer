package io.github.petsaudedigitalgat1.isttimer.util;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ConcurrentHashMap;

public final class AlarmPlayer {

    private static final ConcurrentHashMap<Integer, Ringtone> PLAYERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Runnable> STOP_TASKS = new ConcurrentHashMap<>();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Callback registrado pelo ActiveTimerAdapter enquanto o app está em primeiro plano,
     * para que o botão "Parar" da notificação também consiga silenciar o
     * som/vibração tocando dentro do app (e não só o alarme em segundo plano).
     */
    private static final ConcurrentHashMap<String, Runnable> FOREGROUND_STOP_CALLBACKS = new ConcurrentHashMap<>();

    private AlarmPlayer() {}

    public static void registerForegroundStopCallback(String timerId, Runnable callback) {
        if (timerId == null || callback == null) return;
        FOREGROUND_STOP_CALLBACKS.put(timerId, callback);
    }

    public static void unregisterForegroundStopCallback(String timerId) {
        if (timerId == null) return;
        FOREGROUND_STOP_CALLBACKS.remove(timerId);
    }

    public static void play(Context context, String timerId, Uri uri) {
        if (context == null || uri == null) return;
        int key = timerId != null ? timerId.hashCode() : 0;
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
            if (ringtone == null) return;
            // start playing
            ringtone.play();
            PLAYERS.put(key, ringtone);
            // agendar parada automática após 10 segundos
            Runnable stopTask = () -> {
                Ringtone r = PLAYERS.remove(key);
                if (r != null) {
                    try { r.stop(); } catch (Exception ignored) {}
                }
                Runnable t = STOP_TASKS.remove(key);
                if (t != null) MAIN_HANDLER.removeCallbacks(t);
            };
            Runnable prev = STOP_TASKS.put(key, stopTask);
            if (prev != null) MAIN_HANDLER.removeCallbacks(prev);
            MAIN_HANDLER.postDelayed(stopTask, 10_000);
        } catch (Exception ignored) {
        }
    }

    public static void stop(String timerId) {
        int key = timerId != null ? timerId.hashCode() : 0;
        Runnable task = STOP_TASKS.remove(key);
        if (task != null) MAIN_HANDLER.removeCallbacks(task);
        Ringtone r = PLAYERS.remove(key);
        if (r != null) {
            try {
                r.stop();
            } catch (Exception ignored) {
            }
        }

        Runnable foregroundStop = FOREGROUND_STOP_CALLBACKS.remove(timerId);
        if (foregroundStop != null) {
            foregroundStop.run();
        }
    }

    public static void stopAll() {
        for (Integer k : PLAYERS.keySet()) {
            Runnable task = STOP_TASKS.remove(k);
            if (task != null) MAIN_HANDLER.removeCallbacks(task);
            Ringtone r = PLAYERS.remove(k);
            if (r != null) {
                try { r.stop(); } catch (Exception ignored) {}
            }
        }
    }
}
