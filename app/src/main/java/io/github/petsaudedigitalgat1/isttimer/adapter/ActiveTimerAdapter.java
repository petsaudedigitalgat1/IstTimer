package io.github.petsaudedigitalgat1.isttimer.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import io.github.petsaudedigitalgat1.isttimer.R;
import io.github.petsaudedigitalgat1.isttimer.model.ActiveTimer;
import io.github.petsaudedigitalgat1.isttimer.util.TimerAlarmScheduler;
import io.github.petsaudedigitalgat1.isttimer.util.TimerPersistence;
import com.google.android.material.button.MaterialButton;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActiveTimerAdapter extends RecyclerView.Adapter<ActiveTimerAdapter.ViewHolder> {

    public interface OnTimerCloseClickListener {
        void onTimerClose(ActiveTimer activeTimer, int position);
    }

    private List<ActiveTimer> activeTimers;
    private Context context;
    private Map<ActiveTimer, CountDownTimer> timerMap = new HashMap<>();
    private Map<ActiveTimer, MediaPlayer> mediaPlayerMap = new HashMap<>();
    private Map<ActiveTimer, Ringtone> ringtoneMap = new HashMap<>();
    private Map<ActiveTimer, ToneGenerator> toneGeneratorMap = new HashMap<>();
    private Map<ActiveTimer, Runnable> ringtoneStopTasks = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private OnTimerCloseClickListener onTimerCloseClickListener;

    public ActiveTimerAdapter(Context context, OnTimerCloseClickListener onTimerCloseClickListener) {
        this.context = context;
        this.onTimerCloseClickListener = onTimerCloseClickListener;
        this.activeTimers = new ArrayList<>();
    }

    public void setActiveTimers(List<ActiveTimer> activeTimers) {
        if (activeTimers != null) {
            this.activeTimers = activeTimers;
        } else {
            this.activeTimers = new ArrayList<>();
        }
        persistActiveTimers();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.timer_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActiveTimer activeTimer = activeTimers.get(position);
        holder.activeTimer = activeTimer;
        holder.patientName.setText(activeTimer.patientName);
        holder.testName.setText(translateTestName(activeTimer.testName));

        if (activeTimer.isRunning && activeTimer.endTimeInMillis > 0) {
            long remaining = Math.max(0, activeTimer.endTimeInMillis - System.currentTimeMillis());
            activeTimer.timeLeftInMillis = remaining;
            if (remaining == 0) {
                activeTimer.isRunning = false;
                activeTimer.endTimeInMillis = 0;
            }
        }

        updateCountdownText(holder, activeTimer.timeLeftInMillis);
        updateTotalTimeText(holder, activeTimer.durationInMillis);
        applyStatusStyles(holder, activeTimer);

        if (activeTimer.isRunning && activeTimer.timeLeftInMillis > 0 && !timerMap.containsKey(activeTimer)) {
            startTimer(activeTimer, holder);
        }

        holder.startStopButton.setOnClickListener(v -> {
            if (activeTimer.isRunning) {
                pauseTimer(activeTimer);
            } else {
                startTimer(activeTimer, holder);
            }
            applyStatusStyles(holder, activeTimer);
        });

        holder.deleteButton.setOnClickListener(v -> {
            cancelTimer(activeTimer);
            int adapterPosition = holder.getBindingAdapterPosition();
            if (onTimerCloseClickListener != null && adapterPosition != RecyclerView.NO_POSITION) {
                onTimerCloseClickListener.onTimerClose(activeTimer, adapterPosition);
            }
        });

    }

    private String translateTestName(String original) {
        if (original == null) return "";
        String lang = Locale.getDefault().getLanguage();

        String normalized = Normalizer.normalize(original, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .trim();

        boolean isHiv = normalized.contains("HIV");
        boolean isSifilis = normalized.contains("SIFILIS") || normalized.contains("SIF");
        boolean isHepB = normalized.contains("HEPATITE B") || normalized.contains("HEPATITEB") || normalized.contains("HEPATITIS B");
        boolean isHepC = normalized.contains("HEPATITE C") || normalized.contains("HEPATITEC") || normalized.contains("HEPATITIS C");

        if (lang != null && lang.startsWith("pt")) {
            if (isHiv) return "HIV";
            if (isSifilis) return "Sífilis";
            if (isHepB) return "Hepatite B";
            if (isHepC) return "Hepatite C";
            return original;
        } else if (lang != null && lang.startsWith("en")) {
            if (isHiv) return "HIV";
            if (isSifilis) return "Syphilis";
            if (isHepB) return "Hepatitis B";
            if (isHepC) return "Hepatitis C";
            return original;
        } else if (lang != null && lang.startsWith("es")) {
            if (isHiv) return "VIH";
            if (isSifilis) return "Sífilis";
            if (isHepB) return "Hepatitis B";
            if (isHepC) return "Hepatitis C";
            return original;
        }

        return original;
    }

    private void updateCountdownText(ViewHolder holder, long timeLeftInMillis) {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        holder.countdown.setText(timeLeftFormatted);
    }

    private void updateTotalTimeText(ViewHolder holder, long totalMillis) {
        int minutes = (int) (totalMillis / 1000) / 60;
        int seconds = (int) (totalMillis / 1000) % 60;
        String totalFormatted = String.format(Locale.getDefault(), "/ %02d:%02d", minutes, seconds);
        holder.totalTime.setText(totalFormatted);
    }

    private void applyStatusStyles(ViewHolder holder, ActiveTimer activeTimer) {
        int statusTextId;
        int testColor = resolveTestColor(activeTimer.testName);

        holder.background.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        holder.testName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        holder.status.setTextColor(ContextCompat.getColor(context, R.color.gray_dark));
        holder.countdown.setTextColor(testColor);
        holder.startStopButton.setBackgroundTintList(ColorStateList.valueOf(testColor));
        holder.startStopButton.setIconTintResource(android.R.color.white);
        if (activeTimer.timeLeftInMillis == 0) {
            statusTextId = R.string.status_time_expired;
            holder.startStopButton.setIconResource(android.R.drawable.ic_media_play);
            holder.startStopButton.setContentDescription(context.getString(R.string.button_start));
        } else if (activeTimer.isRunning) {
            statusTextId = R.string.status_running;
            holder.startStopButton.setIconResource(android.R.drawable.ic_media_pause);
            holder.startStopButton.setContentDescription(context.getString(R.string.button_pause));
        } else {
            statusTextId = R.string.status_ready;
            holder.startStopButton.setIconResource(android.R.drawable.ic_media_play);
            holder.startStopButton.setContentDescription(context.getString(R.string.button_start));
        }
        holder.status.setText(statusTextId);
    }

    private int resolveTestColor(String testName) {
        if (testName == null) {
            return ContextCompat.getColor(context, R.color.testHiv);
        }
        String normalized = Normalizer.normalize(testName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .trim();

        if (normalized.contains("HIV")) {
            return ContextCompat.getColor(context, R.color.testHiv);
        }
        if (normalized.contains("SIFILIS") || normalized.contains("SIF")) {
            return ContextCompat.getColor(context, R.color.testSifilis);
        }
        if (normalized.contains("HEPATITE B") || normalized.contains("HEPATITEB")) {
            return ContextCompat.getColor(context, R.color.testHepatiteB);
        }
        if (normalized.contains("HEPATITE C") || normalized.contains("HEPATITEC")) {
            return ContextCompat.getColor(context, R.color.testHepatiteC);
        }
        return ContextCompat.getColor(context, R.color.testHiv);
    }

    private void triggerTimerFinishedFeedback(ActiveTimer activeTimer) {
        triggerVibration();
        triggerAlarmSound(activeTimer);
    }

    private void triggerVibration() {
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager == null ? null : vibratorManager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator == null || !vibrator.hasVibrator()) {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(
                        new long[]{0, 250, 150, 250, 150, 350},
                        -1
                ));
            } else {
                vibrator.vibrate(new long[]{0, 250, 150, 250, 150, 350}, -1);
            }
        } catch (Exception e) {
            Log.e("ActiveTimerAdapter", "Erro ao vibrar: " + e.getMessage());
        }
    }

    private void triggerAlarmSound(ActiveTimer activeTimer) {
        stopTimerFeedback(activeTimer);

        // Permite que o botão "Parar" da notificação (StopAlarmReceiver -> AlarmPlayer.stop)
        // também silencie o som/vibração tocando aqui em primeiro plano.
        io.github.petsaudedigitalgat1.isttimer.util.AlarmPlayer.registerForegroundStopCallback(
                activeTimer.timerId, () -> stopTimerFeedback(activeTimer));

        if (playRawNotificationSound(activeTimer)) {
            return;
        }
        if (playSystemRingtone(activeTimer)) {
            return;
        }
        playFallbackTone(activeTimer);
    }

    private boolean playRawNotificationSound(ActiveTimer activeTimer) {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.notification_sound);
            if (mediaPlayer == null) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }
            // Play the raw notification in loop, but schedule automatic stop after 10s
            mediaPlayer.setLooping(true);
            mediaPlayerMap.put(activeTimer, mediaPlayer);
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                try { mp.release(); } catch (Exception ignored) {}
                mediaPlayerMap.remove(activeTimer);
                return true;
            });
            mediaPlayer.start();
            // agendar parada automática após 10 segundos
            Runnable stopTask = () -> {
                MediaPlayer mp = mediaPlayerMap.remove(activeTimer);
                if (mp != null) {
                    try { if (mp.isPlaying()) mp.stop(); } catch (Exception ignored) {}
                    try { mp.release(); } catch (Exception ignored) {}
                }
                // garantir que qualquer ringtone associado seja também parado
                stopRingtoneForTimer(activeTimer);
            };
            Runnable previous = ringtoneStopTasks.put(activeTimer, stopTask);
            if (previous != null) mainHandler.removeCallbacks(previous);
            mainHandler.postDelayed(stopTask, 10_000);
            return true;
        } catch (Exception e) {
            Log.e("ActiveTimerAdapter", "Erro no som interno: " + e.getMessage());
            return false;
        }
    }

    private boolean playSystemRingtone(ActiveTimer activeTimer) {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (soundUri == null) {
                return false;
            }
            Ringtone ringtone = RingtoneManager.getRingtone(context.getApplicationContext(), soundUri);
            if (ringtone == null) {
                return false;
            }
            // Play ringtone but schedule automatic stop after 10s
            ringtoneMap.put(activeTimer, ringtone);
            ringtone.play();
            Runnable stopTask = () -> {
                Ringtone r = ringtoneMap.remove(activeTimer);
                if (r != null && r.isPlaying()) {
                    try { r.stop(); } catch (Exception ignored) {}
                }
            };
            Runnable previous = ringtoneStopTasks.put(activeTimer, stopTask);
            if (previous != null) mainHandler.removeCallbacks(previous);
            mainHandler.postDelayed(stopTask, 10_000);
            return true;
        } catch (Exception e) {
            Log.e("ActiveTimerAdapter", "Erro no toque do sistema: " + e.getMessage());
            return false;
        }
    }

    private void playFallbackTone(ActiveTimer activeTimer) {
        try {
            ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneGeneratorMap.put(activeTimer, toneGenerator);
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500);
            mainHandler.postDelayed(() -> {
                ToneGenerator tg = toneGeneratorMap.remove(activeTimer);
                if (tg != null) {
                    tg.release();
                }
            }, 1700);
        } catch (Exception e) {
            Log.e("ActiveTimerAdapter", "Erro no tom fallback: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return activeTimers == null ? 0 : activeTimers.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        for (CountDownTimer timer : timerMap.values()) {
            timer.cancel();
        }
        timerMap.clear();
        stopAllFeedback();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.activeTimer != null) {
            CountDownTimer countDownTimer = timerMap.get(holder.activeTimer);
            if (countDownTimer != null) {
                countDownTimer.cancel();
                timerMap.remove(holder.activeTimer);
            }
        }
    }

    private void startTimer(ActiveTimer activeTimer, ViewHolder holder) {
        CountDownTimer existingTimer = timerMap.remove(activeTimer);
        if (existingTimer != null) {
            existingTimer.cancel();
        }

        if (!activeTimer.isRunning || activeTimer.endTimeInMillis <= 0) {
            activeTimer.endTimeInMillis = System.currentTimeMillis() + Math.max(0, activeTimer.timeLeftInMillis);
        }

        activeTimer.isRunning = true;
        long initialMillis = Math.max(0, activeTimer.endTimeInMillis - System.currentTimeMillis());
        activeTimer.timeLeftInMillis = initialMillis;
        TimerAlarmScheduler.scheduleTimerFinish(context.getApplicationContext(), activeTimer);
        persistActiveTimers();

        if (initialMillis == 0) {
            applyStatusStyles(holder, activeTimer);
            return;
        }

        CountDownTimer countDownTimer = new CountDownTimer(initialMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long remaining = Math.max(0, activeTimer.endTimeInMillis - System.currentTimeMillis());
                activeTimer.timeLeftInMillis = remaining;
                if (holder != null && isHolderBoundToTimer(holder, activeTimer)) {
                    updateCountdownText(holder, remaining);
                }
            }

            @Override
            public void onFinish() {
                activeTimer.timeLeftInMillis = 0;
                activeTimer.isRunning = false;
                activeTimer.endTimeInMillis = 0;
                timerMap.remove(activeTimer);
                TimerAlarmScheduler.cancelTimerFinish(context.getApplicationContext(), activeTimer);
                persistActiveTimers();

                if (holder != null && isHolderBoundToTimer(holder, activeTimer)) {
                    updateCountdownText(holder, 0);
                    applyStatusStyles(holder, activeTimer);
                }

                triggerTimerFinishedFeedback(activeTimer);
            }
        }.start();

        timerMap.put(activeTimer, countDownTimer);
    }

    private void pauseTimer(ActiveTimer activeTimer) {
        if (activeTimer.endTimeInMillis > 0) {
            activeTimer.timeLeftInMillis = Math.max(0, activeTimer.endTimeInMillis - System.currentTimeMillis());
        }
        activeTimer.isRunning = false;
        activeTimer.endTimeInMillis = 0;
        cancelTimer(activeTimer);
        TimerAlarmScheduler.cancelTimerFinish(context.getApplicationContext(), activeTimer);
        persistActiveTimers();
    }

    private void cancelTimer(ActiveTimer activeTimer) {
        CountDownTimer existingTimer = timerMap.remove(activeTimer);
        if (existingTimer != null) {
            existingTimer.cancel();
        }
        stopTimerFeedback(activeTimer);
    }

    private void stopTimerFeedback(ActiveTimer activeTimer) {
        io.github.petsaudedigitalgat1.isttimer.util.AlarmPlayer.unregisterForegroundStopCallback(activeTimer.timerId);

        MediaPlayer mediaPlayer = mediaPlayerMap.remove(activeTimer);
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception ignored) {
            }
            mediaPlayer.release();
        }

        stopRingtoneForTimer(activeTimer);

        ToneGenerator toneGenerator = toneGeneratorMap.remove(activeTimer);
        if (toneGenerator != null) {
            toneGenerator.release();
        }

        stopVibration();
    }

    private void stopRingtoneForTimer(ActiveTimer activeTimer) {
        Runnable stopTask = ringtoneStopTasks.remove(activeTimer);
        if (stopTask != null) {
            mainHandler.removeCallbacks(stopTask);
        }

        Ringtone ringtone = ringtoneMap.remove(activeTimer);
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void stopVibration() {
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager == null ? null : vibratorManager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception e) {
            Log.e("ActiveTimerAdapter", "Erro ao parar vibração: " + e.getMessage());
        }
    }

    private void stopAllFeedback() {
        List<ActiveTimer> timersWithFeedback = new ArrayList<>();
        timersWithFeedback.addAll(mediaPlayerMap.keySet());
        timersWithFeedback.addAll(ringtoneMap.keySet());
        timersWithFeedback.addAll(toneGeneratorMap.keySet());

        for (ActiveTimer timer : timersWithFeedback) {
            stopTimerFeedback(timer);
        }
    }

    private boolean isHolderBoundToTimer(ViewHolder holder, ActiveTimer activeTimer) {
        int position = holder.getBindingAdapterPosition();
        return position != RecyclerView.NO_POSITION
                && position < activeTimers.size()
                && activeTimers.get(position) == activeTimer;
    }

    private void persistActiveTimers() {
        TimerPersistence.saveActiveTimers(context.getApplicationContext(), activeTimers);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView patientName;
        TextView testName;
        TextView countdown;
        TextView totalTime;
        TextView status;
        MaterialButton startStopButton;
        ImageButton deleteButton;
        CardView background;
        ActiveTimer activeTimer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            patientName = itemView.findViewById(R.id.textView_patient_name);
            testName = itemView.findViewById(R.id.textView_test_name);
            countdown = itemView.findViewById(R.id.textView_countdown);
            totalTime = itemView.findViewById(R.id.textView_total_time);
            status = itemView.findViewById(R.id.textView_status);
            startStopButton = itemView.findViewById(R.id.button_start_stop);
            deleteButton = itemView.findViewById(R.id.button_delete_timer);
            background = itemView.findViewById(R.id.card_view_background);
        }
    }
}
