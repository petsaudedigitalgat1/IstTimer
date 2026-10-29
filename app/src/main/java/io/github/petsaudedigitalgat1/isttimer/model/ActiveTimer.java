package io.github.petsaudedigitalgat1.isttimer.model;

import java.util.UUID;

public class ActiveTimer {
    public String timerId;
    public String patientName;
    public String testName;
    public long durationInMillis;
    public boolean isRunning;
    public long timeLeftInMillis;
    public long endTimeInMillis;

    public ActiveTimer(String patientName, String testName, long durationInMillis) {
        this(UUID.randomUUID().toString(), patientName, testName, durationInMillis, false, durationInMillis, 0L);
    }

    public ActiveTimer(
            String timerId,
            String patientName,
            String testName,
            long durationInMillis,
            boolean isRunning,
            long timeLeftInMillis,
            long endTimeInMillis
    ) {
        this.timerId = timerId;
        this.patientName = patientName;
        this.testName = testName;
        this.durationInMillis = durationInMillis;
        this.timeLeftInMillis = timeLeftInMillis;
        this.isRunning = isRunning;
        this.endTimeInMillis = endTimeInMillis;
    }
}
