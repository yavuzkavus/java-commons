package com.readjournal.task;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;

import com.readjournal.util.DateUtil;

public class CronTasks {
	private volatile List<Cron> crons;
	private volatile Timer timer;

	private final ThreadHouse threadHouse;

	public CronTasks(ThreadHouse threadHouse) {
		crons = Collections.synchronizedList(new ArrayList<Cron>());
		this.threadHouse = threadHouse;
	}

	public void stop() {
		if( timer!=null )
			timer.cancel();
		crons.clear();
		timer = null;
	}

	public void start() {
		timer = new Timer(true);
		timer.schedule(new TimerTask(), 5000, 5000);
	}

	private class TimerTask extends java.util.TimerTask {
		@Override
		public void run() {
			for( Cron cron : crons ) {
				if( cron.isDisabled() )
					continue;
				long passedMillis = System.currentTimeMillis() - cron.getLastRunningMillis();
				if( cron.getRunningHourMinute()!=null ) {
					LocalTime time = LocalTime.now();
					int currHour = time.getHour(),
						currMin = time.getMinute(),
						hour = cron.getRunningHourMinute()[0],
						min = cron.getRunningHourMinute()[1];
					if( hour>=0 ) {
						//addCronAtFixedHour
						if( passedMillis>23*DateUtil.HOUR_MILLIS && (currHour==hour && currMin>=min || currHour==((hour+1)%24) ) ) {
							threadHouse.execute(cron.getRunnable());
							cron.setLastRunningMillis(System.currentTimeMillis());
						}
					}
					else {
						//addCronAtFixedMinute
						if( passedMillis>59L*60_000 && currMin>=min && (currMin-min)<=30 ) {
							threadHouse.execute(cron.getRunnable());
							cron.setLastRunningMillis(System.currentTimeMillis());
						}						
					}
				}
				else if( passedMillis>cron.getRunningIntervalSeconds()*1000 ) {
					threadHouse.execute(cron.getRunnable());
					cron.setLastRunningMillis(System.currentTimeMillis());
				}
			}
		}
	}

	public synchronized Cron addCronAtFixedHour(Runnable runnable, int hour, int minute) {
		if( runnable==null )
			throw new NullPointerException("runnable");
		if( hour<0 || hour>23 )
			throw new IllegalArgumentException("hour should be between 0-23");
		if( minute<0 || minute>59 )
			throw new IllegalArgumentException("minute should be between 0-59");
		Cron cron = new Cron(runnable, new int[] { hour, minute });
		crons.add(cron);
		return cron;
	}

	public synchronized Cron addCronAtFixedMinute(Runnable runnable, int minute) {
		if( runnable==null )
			throw new NullPointerException("runnable");
		if( minute<0 || minute>59 )
			throw new IllegalArgumentException("minute should be between 0-59");
		Cron cron = new Cron(runnable, new int[] { -1, minute });
		crons.add(cron);
		return cron;
	}

	public synchronized void updateCronInterval(Runnable runnable, int intervalSeconds) {
		Cron cron = getCron(runnable);
		if( cron==null )
			new RuntimeException("Cron not found");
	}

	public synchronized Cron addCron(Runnable runnable, int intervalSeconds) {
		return addCron(runnable, intervalSeconds, intervalSeconds-1);
	}

	public synchronized Cron addCron(Runnable runnable, int intervalSeconds, int waitingSeconds) {
		if( runnable==null )
			throw new NullPointerException("runnable");
		if( intervalSeconds<1 )
			throw new IllegalArgumentException("intervalSeconds should be bigger than 0");
		if( getCron(runnable)!=null )
			new RuntimeException("Cron already added");
		Cron cron = new Cron(runnable, intervalSeconds, waitingSeconds);
		crons.add(cron);
		return cron;
	}

	public synchronized boolean addCron(Cron cron){
		if( crons.contains(cron) )
			new RuntimeException("Cron already added");
		crons.add(cron);
		return false;
	}

	public synchronized boolean removeCron(Cron cron) {
		if( !crons.remove(cron) )
			new RuntimeException("Cron not found");
		return true;
	}

	public synchronized boolean removeCron(Runnable runnable) {
		Cron cron = getCron(runnable);
		if( cron==null )
			new RuntimeException("Cron not found");
		return crons.remove(cron);
	}
	
	public synchronized Cron getCron(Runnable runnable) {
		for(Cron c : crons) {
			if( c.getRunnable()==runnable ) {
				return c;
			}
		}
		return null;
	}
}
