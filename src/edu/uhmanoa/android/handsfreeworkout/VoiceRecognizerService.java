package edu.uhmanoa.android.handsfreeworkout;

import java.util.Timer;
import java.util.TimerTask;

import android.app.IntentService;
import android.content.Intent;

public class VoiceRecognizerService extends IntentService {
	/** The Timer thread which will execute the check for a recognized word from the 
	 * SpeechRecognizer Listener. Acts like a Thread that can be told to start at a 
	 * specific time and/or at specific time intervals. */
	private Timer updateTimer;

	/** The TimerTask which encapsulates the logic that will check for a recognized word. This 
	 * ends up getting run by the Timer in the same way that a Thread runs a Runnable. */

	private TimerTask updateTask;

	/** The time frequency at which the service should run the listener. (3 seconds) */
	private static final long UPDATE_FREQUENCY = 3000L;
	/**
	 * Note that the constructor that takes a String will NOT be properly instantiated.
	 * Use the constructor that takes no parameters instead, and pass in a String that
	 * contains the name of the service to the super() call.
	 */
	public VoiceRecognizerService() {
		super("VoiceRecognizerService");

	}
	/* This method is invoked on the worker thread with a request to process. Only one Intent is processed at a time, 
	 * but the processing happens on a worker thread that runs independently from other application logic. 
	 * So, if this code takes a long time, it will hold up other requests to the same IntentService, 
	 * but it will not hold up anything else. When all requests have been handled, 
	 * the IntentService stops itself, so you should not call stopSelf().
	 * Note: onCreate() is called before onHandleIntent(), therefore initialization in onCreate() was ideal
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		/*Schedule a task for repeated fixed-rate execution after a specific delay has passed.
		 * Parameters
		 * task  the task to schedule. 
		 * delay  amount of time in milliseconds before first execution. 
		 * period  amount of time in milliseconds between subsequent executions.
		 * Using these parameters will cause the TimerTask, which resets the listener, to be run
		 * every so often */	
		this.updateTimer.scheduleAtFixedRate(updateTask, 0, VoiceRecognizerService.UPDATE_FREQUENCY);
	}
	/**
	 * Override onCreate in IntentService
	 * Called by the system when the service is first created
	 */
	@Override
	public void onCreate() {
		/* initialize updateTimer 
		 * Timers schedule recurring tasks for execution*/
		this.updateTimer = new Timer();
		/* initialize updateTask
		 * TimerTask class represents a task to run at a specified time */
		this.updateTask = new TimerTask() {
			//the task to run 
			@Override
			public void run() {
			//reset the voice recognition listener
			}
		};
	}


}
