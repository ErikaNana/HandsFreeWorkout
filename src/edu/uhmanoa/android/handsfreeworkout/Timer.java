package edu.uhmanoa.android.handsfreeworkout;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.Chronometer;

public class Timer extends Chronometer{
	protected boolean mInitialCreate;
	protected boolean mPause;
	protected String mTimerText;
	protected long mTimeWhenStopped;
	
	public Timer(Context context) {
		super(context);
	}
	
	public Timer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	/** Sets the correct base for the timer and runs it */
	public void setCorrectBaseAndStart(boolean initialCreate, boolean pause, long timeWhenStopped, String timerText) {
		//if initial start up
		if (initialCreate) {
			//use "this" keyword because modifying the current object
			this.setBase(SystemClock.elapsedRealtime());
			this.start();
		}
		else {
			this.setText(timerText);
			if (!mPause) {
				/* elapsedRealtime() = returns ms since boot
				 * best method to use to get current time */
				this.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
				this.start();
			}
			else {
				this.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
			}
		}
	}
}
