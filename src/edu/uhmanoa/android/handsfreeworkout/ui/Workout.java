package edu.uhmanoa.android.handsfreeworkout.ui;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.uhmanoa.android.handsfreeworkout.R;
import edu.uhmanoa.android.handsfreeworkout.customcomponents.CustomButton;
import edu.uhmanoa.android.handsfreeworkout.customcomponents.CustomTimer;
import edu.uhmanoa.android.handsfreeworkout.services.HandsFreeService;
import edu.uhmanoa.android.handsfreeworkout.utils.Utils;

public class Workout extends Activity implements OnClickListener{

	protected CustomButton mStartButton; 
	protected CustomButton mStopButton;
	protected CustomButton mPauseButton;
	protected TextView mWelcomeMessage;
	protected TextView mCommandText;
	protected CustomTimer mTimer; 
	protected TextView mDisplayClock;
	protected ServiceReceiver mReceiver;
	protected Intent mListeningServiceIntent;
	protected Intent mHandsFreeIntent;

	protected long mTimeWhenStopped;
	protected String mTimerText;
	protected boolean mInitialCreate; //determines if app is in initial creation state
	protected boolean mWorkoutRunning;
	protected boolean mWorkoutStopped;
	protected boolean mWorkoutPaused;

	/**Time to be sent for update/stop app*/
	protected String mUpdateTime;

	/** Keys for saving and restoring instance data */
	protected static final String SAVED_TIMER_TEXT_VALUE = "timer text";
	protected static final String SAVED_TIME_WHEN_STOPPED_VALUE = "time when stopped";
	protected static final String SAVED_INITIAL_CREATE = "initial create";
	protected static final String SAVED_COUNTER = "saved counter";
	protected static final String SAVED_PAUSE_COMMAND_TEXT = "saved pause command text";
	protected static final String RUNNING_STATE = "running state";
	protected static final String STOPPED_STATE = "stopped state";
	protected static final String PAUSE_STATE = "paused state";
	protected static final String START_BUTTON_TEXT = "start button text";
	protected static final String SAVED_UPDATE_TIME = "saved update time";

	/** Name of the string that identifies what the response should be */
	public static final String RESPONSE_STRING = "response string";

	/** Codes for the response that feedback should say */
	public static final int WORKOUT_ALREADY_STARTED = 1;
	public static final int STOP_WORKOUT = 2;
	public static final int WORKOUT_ALREADY_FINISHED = 3;
	public static final int UPDATE_WORKOUT = 4;
	public static final int CREATING_BASELINE = 5;
	public static final int FINISHED_BASELINE = 6;
	public static final int RESUME_WORKOUT = 7;
	public static final int SILENCE = 8;
	public static final int PAUSE_WORKOUT = 9;
	public static final int COMMAND_NOT_RECOGNIZED = 10;
	public static final int START_WORKOUT = 11;

	/**Modes for display clock*/
	protected static final int CLASSIC = 1;
	protected static final int HIPSTER = 2;
	protected static final int HYBRID = 3;

	/**Extra for update */
	public static final String UPDATE_TIME_STRING = "update value string";
	public static final String UPDATE_ACTION = "update action";
	/**
	 * ISSUES TO DEAL WITH STILL:
	 * accidental loud noises
	 * 
	 * maybe for baseline, require user to say a phrase for x amount of seconds.  
	 * if amp is within 5% of that, then it is a command
	 * 
	 * need to stop listening if there is a phone call interruption
	 *
	 */


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.e("Workout", " in on Create");
		super.onCreate(savedInstanceState);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		//inflate the layout
		setContentView(R.layout.workout);
		setLayoutFont();

		//initialize variables
		mWelcomeMessage = (TextView) findViewById(R.id.welcomeMessage);
		mCommandText = (TextView) findViewById(R.id.command);
		mDisplayClock = (TextView) findViewById(R.id.displayClock);

		mStartButton = (CustomButton) findViewById(R.id.speakButton);
		mStartButton.setOnClickListener(this);

		mStopButton = (CustomButton) findViewById(R.id.stopButton);
		mStopButton.setOnClickListener(this);

		mPauseButton = (CustomButton) findViewById(R.id.pauseButton);
		mPauseButton.setOnClickListener(this);

		mInitialCreate = true;
		mWorkoutRunning = true;

		//disable button if no recognition service is present
		PackageManager pm = getPackageManager();
		/*queryIntentActivities() = Retrieves all activities that can be performed for
		 * 						    the given intent.  Takes in intent and flags as parameters,
		 * 							which are optional
		 * RecognizerIntent.CONSTANT = Starts an activity that will prompt the user for speech and 
		 * 							   send it through a speech recognizer.  The results will 
		 *                             be returned via activity results (in onActivityResult(int, int, Intent), 
		 *                             if you  start the intent using startActivityForResult(Intent, int))*/
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() == 0) {
			mStartButton.turnOff();
			mStartButton.setText("Recognizer not present");
		}
		/* tell OS that volume buttons should affect "media" volume, since that's the volume
		used for application */
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		//So that app doesn't pick up feedback from Welcome
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				startHandsFreeService();	
			}
		}, 1000L);


	}

	/**Handle button clicks */
	@Override
	public void onClick(View view) {
		//starting new stage
		Log.w("Workout", "running:  " + mWorkoutRunning);
		Log.w("Workout", "paused:  " + mWorkoutPaused);
		Log.w("Workout", "stopped:  " + mWorkoutStopped);
		Log.w("Workout", "start button is pressed");

		switch (view.getId()){
			case(R.id.speakButton):{
				mCommandText.setTextColor(Color.GREEN);
				if (mWorkoutStopped) {
					startHandsFreeService();
					announceUpdate(HandsFreeService.INITIAL_CREATE, "");
					//reset
					mTimerText = "0 seconds";
					setStateVariables(true, false, false);
					createTimer();
					setButtons();
					mStartButton.setText("Resume");
					flashText("Begin");
					break;
				}
				if (mWorkoutPaused) {
					announceUpdate(HandsFreeService.START_BUTTON_RESUME_CLICK, "");
					resumeTimer();
					setButtons();
					flashText("Resume");
					break;
				}
				else{
					//just in case
					announceUpdate(HandsFreeService.START_BUTTON_CLICK, "");
				}
				break;
			}
			case (R.id.stopButton):{
				if (!mWorkoutStopped) {
					if (mTimer != null) {
						mTimer.stop();
						mTimer = null;
					}
					setStateVariables(false, true, true);
					announceUpdate(HandsFreeService.STOP_BUTTON_CLICK, mUpdateTime);
					Log.w("Workout", "stop button is pressed");
					mStartButton.setText("Start");
					setButtons();
					mTimeWhenStopped = 0;	
					mCommandText.setText("Stop");
					mCommandText.setTextColor(Color.RED);
				}
				break;
			}
			case (R.id.pauseButton):{
				Log.w("Workout", "pause button is pressed");
				if (!mWorkoutPaused) {
					setStateVariables(false, true, false);
					announceUpdate(HandsFreeService.PAUSE_BUTTON_CLICK, "");
					pauseTimer();
					setButtons();
					mCommandText.setText("Pause");
					mCommandText.setTextColor(Color.YELLOW);
				}
				break;
			}
		}		
	}

	/**sets the text for the display clock.  it is called at every tick of the clock */
	public void setDisplayClock(int mode) {
		if(mTimer == null) {
			Log.w("Workout", "(set display clock) timer is null");
		}

		String time = (String) mTimer.getText();
		mTimerText = time;
		//want the most current time possible
		mUpdateTime = Utils.getUpdate(time);
		//Log.w("workout", "time in setDisplayClock:  " + time);
		if (!time.equals("")) {
			switch(mode) {
				case HIPSTER:{				
				}
				case HYBRID:{
					time = Utils.getPrettyHybridTime(time);	
				}
				case CLASSIC:{				
				}
			}	
			mDisplayClock.setText(time);
		}	
	}

	/** method for persistence .  modes are just for debugging right now*/
	public void setDisplayClock(int mode, String timerText) {
		switch(mode) {
			case HIPSTER:{				
			}
			case HYBRID:{
				mDisplayClock.setText(Utils.getPrettyHybridTime(timerText));	
			}
			case CLASSIC:{			
			}
		}	
	}

	/** Creates the timer and sets the base time */
	protected void createTimer() {
		Log.w("Workout", "create timer");
		mTimer = (CustomTimer) findViewById(R.id.timer);
		mTimer.setOnChronometerTickListener(new OnChronometerTickListener() {

			@Override
			public void onChronometerTick(Chronometer chronometer) {
				Log.w("Workout", "tick tock");
				setDisplayClock(HYBRID);
			}
		});

		mTimer.setCorrectBaseAndStart(mWorkoutStopped, mWorkoutRunning,mInitialCreate, mWorkoutPaused, mTimeWhenStopped, mTimerText);
		if (!mInitialCreate) {
			setDisplayClock(HYBRID, mTimerText);
			if (!mWorkoutPaused) {
				mCommandText.setText("");
			}
		}
		if (mInitialCreate) {
			mInitialCreate = false;
		}
	}
	/** Destroys the timer */
	protected void destroyTimer() {
		//to prevent nullPointer
		if (mTimer != null) {
			mTimer.stop();
			mTimer = null;
		}
	}

	/** Sets up the pause state */
	public void pauseTimer () {
		//save the time when the timer was stopped
		mTimeWhenStopped = mTimer.getBase() - SystemClock.elapsedRealtime();
		mTimer.stop();
	}

	/*Resumes the timer */
	public void resumeTimer() {
		//adjust the timer to the correct time
		setStateVariables(true, false, false);
		createTimer();
	}
	/** Called when activity is interrupted, like orientation change */
	@Override
	protected void onPause() {
		Log.w("Workout", "activity interrupted");
		if (mTimer != null) {
			mTimerText = (String) mTimer.getText();
			Log.w("Workout", "(on pause) timer Text:  " + mTimerText);
			//for persistence
			if (!mWorkoutPaused) {
				mTimeWhenStopped = mTimer.getBase() - SystemClock.elapsedRealtime();
			}
		}
		//unregister the receiver
		this.unregisterReceiver(mReceiver);
		super.onPause();
	}

	/** Called when user comes back to the activity */
	@Override
	protected void onResume() {
		Log.w("Workout", "on resume");
		//so that can resume, but also considering first run of app
		Log.w("Workout","initial create:  "+ mInitialCreate);

		if (!mInitialCreate) {
				//persist the time 
			if (mWorkoutRunning) {
				createTimer();
			}
				mDisplayClock.setText(Utils.getPrettyHybridTime(mTimerText));
		}
		else {
			//initial create
			createTimer();
			Log.w("Workout", "initial create");
		}

		setButtons();

		if (mWorkoutPaused) {
			mCommandText.setTextColor(Color.YELLOW);
		}

		//special case if workout is stopped
		if(mWorkoutStopped) {
			mStartButton.turnOn();
			mStopButton.turnOff();
			mPauseButton.turnOff();
			mCommandText.setTextColor(Color.RED);
		}

		IntentFilter handsFree = new IntentFilter(ServiceReceiver.UPDATE);
		handsFree.addCategory(Intent.CATEGORY_DEFAULT);

		//initialize the receiver
		mReceiver = new ServiceReceiver();
		this.registerReceiver(mReceiver, handsFree);
		super.onResume();
	}

	/** Save the instance data */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		//store the current values in outState
		Log.w("Workout", "saving instance state");

		outState.putString(SAVED_TIMER_TEXT_VALUE, mTimerText);
		outState.putLong(SAVED_TIME_WHEN_STOPPED_VALUE, mTimeWhenStopped);
		outState.putBoolean(SAVED_INITIAL_CREATE, mInitialCreate);
		outState.putString(SAVED_PAUSE_COMMAND_TEXT, (String) mCommandText.getText());
		outState.putBoolean(RUNNING_STATE, mWorkoutRunning);
		outState.putBoolean(PAUSE_STATE, mWorkoutPaused);
		outState.putBoolean(STOPPED_STATE, mWorkoutStopped);
		outState.putString(START_BUTTON_TEXT, (String) mStartButton.getText());
		outState.putString(SAVED_UPDATE_TIME, mUpdateTime);

/*		Log.w("Workout", "(save) timer text:  " + mTimerText);
		Log.w("Workout", "(save) time when stopped text:  " + mTimeWhenStopped);
		Log.w("Workout", "(save) initial create:  " + mInitialCreate);
		Log.w("Workout", "(save) running workout:  " + mWorkoutRunning);
		Log.w("Workout", "(save) workout paused:  " + mWorkoutPaused);
		Log.w("Workout", "(save) workout stopped:  " + mWorkoutStopped);*/
		super.onSaveInstanceState(outState);
	}

	/** Restore the instance data*/
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.w("Workout", "restoring instance state");
		super.onRestoreInstanceState(savedInstanceState);
		//retrieve the values and set them
		//maybe test for null and use Bundle.containsKey() method later

		mTimerText = savedInstanceState.getString(SAVED_TIMER_TEXT_VALUE);
		mTimeWhenStopped = savedInstanceState.getLong(SAVED_TIME_WHEN_STOPPED_VALUE);
		mInitialCreate = savedInstanceState.getBoolean(SAVED_INITIAL_CREATE);
		mCommandText.setText(savedInstanceState.getString(SAVED_PAUSE_COMMAND_TEXT));
		mWorkoutRunning = savedInstanceState.getBoolean(RUNNING_STATE);
		mWorkoutPaused = savedInstanceState.getBoolean(PAUSE_STATE);
		mWorkoutRunning = savedInstanceState.getBoolean(RUNNING_STATE);
		mWorkoutStopped = savedInstanceState.getBoolean(STOPPED_STATE);
		mStartButton.setText(savedInstanceState.getString(START_BUTTON_TEXT));
		mUpdateTime = savedInstanceState.getString(SAVED_UPDATE_TIME);

		Log.w("Workout", "(restore) timer text:  " + mTimerText);
		Log.w("Workout", "(restore) time when stopped text:  " + mTimeWhenStopped);
		Log.w("Workout", "(restore) initial create:  " + mInitialCreate);
		Log.w("Workout", "(restore) running workout:  " + mWorkoutRunning);
		Log.w("Workout", "(restore) workout paused:  " + mWorkoutPaused);
		Log.w("Workout", "(restore) workout stopped:  " + mWorkoutStopped);
	}

	public void startHandsFreeService() {
		Log.w("Workout", "starting hands free service");
		mHandsFreeIntent = new Intent(this, HandsFreeService.class);
		startService(mHandsFreeIntent);
	}

	protected void announceUpdate(int action, String updateTime) {
		Log.w("Workout", "announcing update");
		Intent announce = new Intent(HandsFreeService.UpdateReceiver.UPDATE);
		announce.putExtra(UPDATE_ACTION, action);
		if (updateTime != "") {
			announce.putExtra(UPDATE_TIME_STRING, updateTime);	
		}
		//broadcast with default category
		announce.addCategory(Intent.CATEGORY_DEFAULT);
		this.sendBroadcast(announce);
	}

	public class ServiceReceiver extends BroadcastReceiver {

		public static final String UPDATE = "get update";

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.w("Workout", "broadcast received");
			Log.w("Workout", "intent action:  " + intent.getAction());
			if (intent.getAction().equals(UPDATE)) {
				int action = intent.getIntExtra(HandsFreeService.APPLICATION_STATE, 0);

				//update the UI to reflect the current state
				switch (action) {
					case HandsFreeService.START:{
						//if already started, don't do anything?
					}
					case HandsFreeService.STOP:{
						announceUpdate(HandsFreeService.STOP_BUTTON_CLICK, mUpdateTime);
					}
					case HandsFreeService.PAUSE:{

					}
					case HandsFreeService.UPDATE:{
						announceUpdate(HandsFreeService.UPDATE_TIME, mUpdateTime);
					}
				}
				Log.w("Workout", "getting update from HFService");
			}
		}
	}

	public void onDestroy() {
		super.onDestroy();
		Log.w("Workout", "is finishing?  " + this.isFinishing());
		//if user is absolutely done with the activity
		if (isFinishing()) {
			this.stopService(mHandsFreeIntent);
		}
		Log.e("Workout", "Workout onDestroy");
	}
	/**Set the layout font */
	public void setLayoutFont() {
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Edmondsans-Bold.otf");
		LinearLayout layout = (LinearLayout) findViewById(R.id.workout_layout);
		int count= layout.getChildCount();
		for (int i = 0; i < count; i++) {
			View view = layout.getChildAt(i);

			if (view instanceof TextView) {
				((TextView) view).setTypeface(font);
			}
			if (view instanceof CustomButton) {
				((Button) view).setTypeface(font);
			}
		}
	}

	/**Set the buttons according to the state of the applications.  For persistence*/
	public void setButtons () {
		if (mWorkoutRunning) {
			mStartButton.turnOff();
		}
		if (!mWorkoutRunning) {
			mStartButton.turnOn();
		}
		if (mWorkoutPaused) {
			mPauseButton.turnOff();
		}
		if(!mWorkoutPaused) {
			mPauseButton.turnOn();
		}
		if (mWorkoutStopped) {
			mStopButton.turnOff();
		}
		if (!mWorkoutStopped) {
			mStopButton.turnOn();
		}
	}

	public void setStateVariables (boolean running, boolean pause, boolean stop) {
		if (running) {
			mWorkoutRunning = true;
		}
		if (!running) {
			mWorkoutRunning = false;
		}
		if (pause) {
			mWorkoutPaused = true;
		}
		if (!pause) {
			mWorkoutPaused = false;
		}
		if (stop) {
			mWorkoutStopped = true;
		}
		if (!stop) {
			mWorkoutStopped = false;
		}
	}

	/**Flash the command text for a second*/
	public void flashText(String command) {
		mCommandText.setText(command);
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				mCommandText.setText("");
			}
		}, 1000L);
	}
}