package io.github.petsaudedigitalgat1.isttimer.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.github.petsaudedigitalgat1.isttimer.model.ActiveTimer;
import io.github.petsaudedigitalgat1.isttimer.util.TimerAlarmScheduler;
import io.github.petsaudedigitalgat1.isttimer.util.TimerPersistence;

import java.util.ArrayList;
import java.util.List;

public class TimerViewModel extends AndroidViewModel {

    private static final String TAG = "TimerViewModel";

    private MutableLiveData<List<ActiveTimer>> activeTimers = new MutableLiveData<>();

    public TimerViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "Inicializando TimerViewModel");
        List<ActiveTimer> restoredTimers = TimerPersistence.loadActiveTimers(application);
        reconcileTimersWithClock(restoredTimers);
        activeTimers.setValue(restoredTimers);
        rescheduleRunningTimers(restoredTimers);
    }

    public LiveData<List<ActiveTimer>> getActiveTimers() {
        return activeTimers;
    }

    public void startNewTimer(String patientName, String testName, int durationInMinutes) {
        try {
            Log.d(TAG, "Adicionando timer parado para: " + patientName + " - " + testName);
            List<ActiveTimer> currentTimers = activeTimers.getValue();
            if (currentTimers == null) {
                currentTimers = new ArrayList<>();
            }

            List<ActiveTimer> updatedTimers = new ArrayList<>(currentTimers);
            ActiveTimer timer = new ActiveTimer(patientName, testName, durationInMinutes * 60 * 1000L);
            timer.isRunning = false;
            timer.endTimeInMillis = 0;
            timer.timeLeftInMillis = timer.durationInMillis;

            updatedTimers.add(timer);
            activeTimers.setValue(updatedTimers);
            TimerPersistence.saveActiveTimers(getApplication(), updatedTimers);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar timer: " + e.getMessage(), e);
        }
    }

    public void removeActiveTimer(ActiveTimer activeTimer, int position) {
        try {
            List<ActiveTimer> currentTimers = activeTimers.getValue();
            if (currentTimers == null || currentTimers.isEmpty()) {
                return;
            }

            List<ActiveTimer> updatedTimers = new ArrayList<>(currentTimers);
            if (position >= 0 && position < updatedTimers.size()) {
                ActiveTimer removed = updatedTimers.remove(position);
                TimerAlarmScheduler.cancelTimerFinish(getApplication(), removed);
            } else {
                TimerAlarmScheduler.cancelTimerFinish(getApplication(), activeTimer);
                updatedTimers.remove(activeTimer);
            }
            activeTimers.setValue(updatedTimers);
            TimerPersistence.saveActiveTimers(getApplication(), updatedTimers);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao remover timer: " + e.getMessage(), e);
        }
    }

    public void moveActiveTimerUp(int position) {
        moveTimer(position, position - 1);
    }

    public void moveActiveTimerDown(int position) {
        moveTimer(position, position + 1);
    }

    public void moveActiveTimerToStart(int position) {
        moveTimer(position, 0);
    }

    public void moveActiveTimerToEnd(int position) {
        List<ActiveTimer> currentTimers = activeTimers.getValue();
        if (currentTimers == null || currentTimers.isEmpty()) {
            return;
        }
        moveTimer(position, currentTimers.size() - 1);
    }

    public void moveActiveTimer(int fromPosition, int toPosition) {
        moveTimer(fromPosition, toPosition);
    }

    private void moveTimer(int fromPosition, int toPosition) {
        try {
            List<ActiveTimer> currentTimers = activeTimers.getValue();
            if (currentTimers == null || currentTimers.isEmpty()) {
                return;
            }
            if (fromPosition < 0 || fromPosition >= currentTimers.size()) {
                return;
            }

            int safeToPosition = Math.max(0, Math.min(toPosition, currentTimers.size() - 1));
            if (fromPosition == safeToPosition) {
                return;
            }

            List<ActiveTimer> updatedTimers = new ArrayList<>(currentTimers);
            ActiveTimer item = updatedTimers.remove(fromPosition);
            updatedTimers.add(safeToPosition, item);
            activeTimers.setValue(updatedTimers);
            TimerPersistence.saveActiveTimers(getApplication(), updatedTimers);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao mover timer: " + e.getMessage(), e);
        }
    }

    private void rescheduleRunningTimers(List<ActiveTimer> timers) {
        if (timers == null) {
            return;
        }
        for (ActiveTimer timer : timers) {
            if (timer.isRunning && timer.timeLeftInMillis > 0 && timer.endTimeInMillis > System.currentTimeMillis()) {
                TimerAlarmScheduler.scheduleTimerFinish(getApplication(), timer);
            }
        }
    }

    private void reconcileTimersWithClock(List<ActiveTimer> timers) {
        if (timers == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (ActiveTimer timer : timers) {
            if (!timer.isRunning) {
                continue;
            }

            if (timer.endTimeInMillis <= 0) {
                timer.endTimeInMillis = now + Math.max(0, timer.timeLeftInMillis);
            }

            long left = timer.endTimeInMillis - now;
            if (left <= 0) {
                timer.timeLeftInMillis = 0;
                timer.isRunning = false;
                timer.endTimeInMillis = 0;
            } else {
                timer.timeLeftInMillis = left;
            }
        }
    }
}
