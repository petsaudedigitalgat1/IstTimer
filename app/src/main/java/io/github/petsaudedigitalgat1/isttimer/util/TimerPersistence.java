package io.github.petsaudedigitalgat1.isttimer.util;

import android.content.Context;
import android.content.SharedPreferences;

import io.github.petsaudedigitalgat1.isttimer.model.ActiveTimer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class TimerPersistence {

    private static final String PREF_NAME = "isttimer_prefs";
    private static final String KEY_ACTIVE_TIMERS = "active_timers_json";

    private TimerPersistence() {
    }

    public static void saveActiveTimers(Context context, List<ActiveTimer> activeTimers) {
        if (context == null) {
            return;
        }

        JSONArray array = new JSONArray();
        if (activeTimers != null) {
            for (ActiveTimer timer : activeTimers) {
                if (timer == null) {
                    continue;
                }
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("timerId", timer.timerId);
                    obj.put("patientName", timer.patientName);
                    obj.put("testName", timer.testName);
                    obj.put("durationInMillis", timer.durationInMillis);
                    obj.put("isRunning", timer.isRunning);
                    obj.put("timeLeftInMillis", timer.timeLeftInMillis);
                    obj.put("endTimeInMillis", timer.endTimeInMillis);
                    array.put(obj);
                } catch (Exception ignored) {
                }
            }
        }

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_ACTIVE_TIMERS, array.toString()).apply();
    }

    public static List<ActiveTimer> loadActiveTimers(Context context) {
        List<ActiveTimer> result = new ArrayList<>();
        if (context == null) {
            return result;
        }

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = preferences.getString(KEY_ACTIVE_TIMERS, "[]");

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }

                ActiveTimer timer = new ActiveTimer(
                        obj.optString("timerId", ""),
                        obj.optString("patientName", ""),
                        obj.optString("testName", ""),
                        obj.optLong("durationInMillis", 0L),
                        obj.optBoolean("isRunning", false),
                        obj.optLong("timeLeftInMillis", 0L),
                        obj.optLong("endTimeInMillis", 0L)
                );
                result.add(timer);
            }
        } catch (Exception ignored) {
        }

        return result;
    }
}
