package com.readjournal.task;

import java.util.Calendar;

import com.readjournal.util.DateUtil;

public class Cron {
	private final Runnable runnable;
	private final int[] runningHourMinute;
	private final long runningIntervalSeconds; //in seconds
	private volatile long lastRunningMillis;
	private volatile boolean disabled;

	public Cron(Runnable runnable, int[] runningHourMinute) {
		this.runnable = runnable;
		this.runningHourMinute = runningHourMinute;
		this.runningIntervalSeconds = 0;
		if( runningHourMinute[0]>=0 )
			this.lastRunningMillis = System.currentTimeMillis() - 23*DateUtil.HOUR_MILLIS - 1;
		else {
			int min = runningHourMinute[1],
				currMin = Calendar.getInstance().get(Calendar.MINUTE);
			if( currMin>min )
				this.lastRunningMillis = System.currentTimeMillis() - (currMin-min)*60_000 - 1;
			else
				this.lastRunningMillis = System.currentTimeMillis() - (60-(min-currMin))*60_000 - 1;
		}
	}

	public Cron(Runnable runnable, long runningIntervalSeconds) {
		this(runnable, runningIntervalSeconds, runningIntervalSeconds-1);
	}

	public Cron(Runnable runnable, long runningIntervalSeconds, long waitingTimeoutSeconds) {
		this.runnable = runnable;
		this.runningHourMinute = null;
		this.runningIntervalSeconds = runningIntervalSeconds;
		this.lastRunningMillis = System.currentTimeMillis() - runningIntervalSeconds*1000 + waitingTimeoutSeconds*1000;
	}

	public Runnable getRunnable() {
		return runnable;
	}

	public int[] getRunningHourMinute() {
		return runningHourMinute;
	}

	public long getRunningIntervalSeconds() {
		return runningIntervalSeconds;
	}

	public long getLastRunningMillis() {
		return lastRunningMillis;
	}

	void setLastRunningMillis(long lastRunningMillis) {
		this.lastRunningMillis = lastRunningMillis;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
}
