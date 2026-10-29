package io.github.petsaudedigitalgat1.isttimer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.github.petsaudedigitalgat1.isttimer.util.AlarmPlayer;

public class StopAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String timerId = intent.getStringExtra(io.github.petsaudedigitalgat1.isttimer.util.TimerAlarmScheduler.EXTRA_TIMER_ID);
        if (timerId == null) return;
        AlarmPlayer.stop(timerId);
    }
}
