package edu.uhmanoa.android.handsfreeworkout.customcomponents;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Chronometer;

public class CustomTimer extends Chronometer{
	
	public CustomTimer(Context context) {
		super(context);
	}
	
	public CustomTimer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	/** Sets the correct base for the timer and runs it */
	public void setCorrectBaseAndStart(boolean workoutStopped, boolean workoutRunning, boolean initialCreate, boolean mWorkoutPaused, long timeWhenStopped, String timerText, long baseTime) {
/*		Log.w("CustomTimer", "initialCreate:  " + initialCreate);*/
		Log.w("CustomTimer", "workoutPaused:  " + mWorkoutPaused);
/*		Log.w("CustomTimer", "timeWhenStopped:  " + timeWhenStopped);*/
		Log.w("CustomTimer", "timerText:  " + timerText);
		Log.w("CustomTimer", "workout stopped:  " + workoutStopped);
		Log.w("CustomTimer", "workout running:  " + workoutRunning);
		//if initial start up
		this.setText(timerText);
		if (initialCreate) {
			//use "this" keyword because modifying the current object
			this.setBase(SystemClock.elapsedRealtime());
			this.start();
		}
		else {
			if (workoutRunning) {
				if (baseTime == 0) {
					this.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
				}
				else {
					long timePassed = baseTime - SystemClock.elapsedRealtime();
					this.setBase(SystemClock.elapsedRealtime() + timePassed);
				}
				this.start();
			}
			if (workoutStopped) {
/*				Log.w("CustomTimer", "in workout stopped");*/
				this.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);			
			}
			if (mWorkoutPaused) {
/*				Log.w("CustomTimer", "in workout paused");*/
				this.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
			}
		}
	}
}
