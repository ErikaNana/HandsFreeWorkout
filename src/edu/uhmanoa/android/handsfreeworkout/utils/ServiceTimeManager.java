package edu.uhmanoa.android.handsfreeworkout.utils;

import java.util.ArrayList;

import android.os.SystemClock;
import android.util.Log;

/**Manages the time when Workout is in sleeping mode*/
public class ServiceTimeManager {
	
	protected long mBaseTime;
	protected static ArrayList<Long> mTotalTime = new ArrayList<Long>();
	
	/**Create the ServiceTimeManager with a starting base time*/
	public ServiceTimeManager(long baseTime) {
		//initialize baseTime
		Log.w("STM", "created new time manager with baseTime: " + baseTime);
		mBaseTime = baseTime;
	}
	
	/**set the base time*/
	public void setBaseTime(long newTime) {
		mBaseTime = newTime;
	}
	
	/**Add a section of time to the total time ArrayList*/
	public void addSectionOfTime(long timeOfAction) {
		long timePassed = getTimePassed(timeOfAction);
		mTotalTime.add(timePassed);
	}
	
	/**returns a parsed time*/
	public static long getParsedTime(long time) {
		return time/1000;
	}
	
	/**Gets the total time from adding up the times in the ArrayList*/
	public static long getTotalTime () {
		long totalTime = 0;
		for (long time: mTotalTime) {
			totalTime += time;
		}
/*		Log.e("STM", "totalTime:  " + totalTime);*/
		return totalTime;
	}
	
	/**Gets the total time and formats it*/
	public static String getTotalTimeAndFormat() {
		long totalTime = getTotalTime();
		return getUpdateTimeFromRaw(totalTime);
	}
	
	/**Gets the formatted time based on the raw time*/
	public static String getUpdateTimeFromRaw(long rawTime){
		//get time in hours minutes seconds
		int hours = (int) ((rawTime/(60 * 60)) % 24);
		int minutes = (int) ((rawTime / 60) % 60);
		int seconds = (int) rawTime % 60;
		
		String time = "";
		
		String correctHours = String.valueOf(hours);
		String correctMinutes = String.valueOf(minutes);
		String correctSeconds = String.valueOf(seconds);
		
		if (hours < 10) {
			correctHours = "0" + correctHours;
		}
		if (minutes < 10) {
			correctMinutes = "0" + correctMinutes;
		}
		
		if (seconds <10) {
			correctSeconds = "0" + correctSeconds;
		}
		if (hours == 0) {
			//just minutes
			time = correctMinutes + ":" + correctSeconds;
		}
		if (hours > 0) {
			time = correctHours + ":" + correctMinutes + ":" + correctSeconds;
		}
		Log.w("UTILS", "(getUpdateFromRaw) TIME:  " + time);
		/*return Utils.getUpdate(time, true);*/
		return Utils.getUpdate(time);
	}
	
	/**Gets how much time has passed between the base and the time the action was committed, 
	 * parses it and adds it to the mTotalTime ArrayList.*/
	public long getTimePassed(long timeOfAction) {
		//need to be more accurate, current time needs to be time button was clicked or 
		//when command was said
		long timePassed = getParsedTime((timeOfAction - mBaseTime));
/*		Log.w("STM", "get time passed");
		Log.w("STM", "time of Action:  " + timeOfAction);
		Log.w("STM", "baseTime:  " + mBaseTime);
		Log.w("STM", "timepassed = " + timePassed);*/
		return timePassed;
	}
	/**Clears the mTotalTime ArrayList*/
	public void resetTotalTime() {
		mTotalTime.clear();
	}
	
	/**Gets all of the time that has passed up to the timeOfAction*/
	public long getUpdateTime(long timeOfAction, boolean update) {
		long timePassedRecent = getTimePassed(timeOfAction);
		//if we're updating, want the timeofAction to be as late as possible
		if (update) {
			timePassedRecent = getTimePassed(SystemClock.elapsedRealtime());
		}
		long totalTimeSoFar = getTotalTime();
		long totalTime = totalTimeSoFar + timePassedRecent;
		return totalTime;
	}
}
