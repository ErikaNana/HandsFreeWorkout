package edu.uhmanoa.android.handsfreeworkout.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.os.Bundle;
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
import edu.uhmanoa.android.handsfreeworkout.services.CommandListeningService;
import edu.uhmanoa.android.handsfreeworkout.services.FeedbackService;
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
	
	protected boolean mPause;
	protected long mTimeWhenStopped;
	protected String mTimerText;
	protected String mStoppedTimerText;
	protected boolean mInitialCreate; //determines if app is in initial creation state
	
	/** Keys for saving and restoring instance data */
	protected static final String SAVED_SPEECH_REC_ALIVE_VALUE = "saved speech";
	protected static final String SAVED_FINISHED_SPEECH_REC_VALUE = "finished speech";
	protected static final String SAVED_DOING_SPEECH_REC_VALUE = "doing speecrech";
/*	protected static final String SAVED_CHECK_BASELINE_VALUE = "check baseline";*/
	protected static final String SAVED_BASELINE_AMP_VALUE = "saved baseline";
	protected static final String SAVED_LISTENING_FOR_COMMANDS_VALUE = "listening for commands";
	protected static final String SAVED_AVERAGE_ARRAYLIST_VALUE = "average array list";
	protected static final String SAVED_TIMER_TEXT_VALUE = "timer text";
	protected static final String SAVED_TIME_WHEN_STOPPED_VALUE = "time when stopped";
	protected static final String SAVED_INITIAL_CREATE = "initial create";
	protected static final String SAVED_COUNTER = "saved counter";
	protected static final String SAVED_PAUSE = "saved pause";
	protected static final String SAVED_PAUSE_COMMAND_TEXT = "saved pause command text";
	protected static final String SAVED_STOP_TIMER_TEXT = "saved stop timer text";
	
	/** The time frequency at which check if speech recognizer is still alive */
	protected static final long UPDATE_FREQUENCY = 4000L;
	/** The time frequency at which check the max amplitude of the recording */
	protected static final long CHECK_FREQUENCY = 350L; 
	/** The time frequency at which do recording for baseline */
	protected static final long BASELINE_FREQUENCY = 1000L;
	
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
	/**
	 * ISSUES TO DEAL WITH STILL:
	 * accidental loud noises
	 * 
	 * maybe for baseline, require user to say a phrase for x amount of seconds.  
	 * if amp is within 5% of that, then it is a command
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
	}


	public void handleInput(ArrayList<String> results) {
		//use a default layout to display the words
		String command = "command not recognized";
		
		int word = 0;
		if (results.contains("start")) {
			command = "Start";
			word = 1;
		}
		if (results.contains("stop")) {
			command = "Stop";
			word = 2;
		}
		if (results.contains("update")) {
			command = "Update";
			word = 3;
		}
		if (results.contains("pause")) {
			command = "Pause";
			word = 4;
		}
		Log.w("Workout", "command:  " + command);
		mCommandText.setText(command);
		replyToCommands(word);
	}
	/** Replies to commands */
	public void replyToCommands(int word) {
		Log.w("Workout", "replying");
		/*Log.w("Workout", "listening:  " + mListeningForCommands);*/
		switch(word) {
			//start
			case (1):{
				//workout has already started
				if (!mStartButton.isEnabled()) {
					Log.w("Workout", "pause is:  " + mPause);
					if (mPause) {
						//if it's stopped, start it (for pause case, and need to say "Start" first)
						Log.w("Workout", "timer is stopped");
						resumeTimer();						
						Log.w("WORKOUT", "RESUME WORKOUT");
						startResponseService(RESUME_WORKOUT);
					}
					else {
						Log.w("WORKOUT", "WORKOUT ALREADY STARTED");
						startResponseService(WORKOUT_ALREADY_STARTED);						
					}
				}
				break;
			}
			//stop
			case (2):{
				//workout is in progress
				if (!mStartButton.isEnabled()) {
					Log.w("WORKOUT", "STOP WORKOUT");
					startResponseService(STOP_WORKOUT);
				}
				//in limbo
				else{
					Log.w("WORKOUT", "WORKOUT ALREADY FINISHED");
					startResponseService(WORKOUT_ALREADY_FINISHED);
				}
				break;
			}
			//update
			case (3):{
				Log.w("WORKOUT", "UPDATE WORKOUT");
				startResponseService(UPDATE_WORKOUT);
				break;
			}
			//pause
			case (4):{
				pauseTimer();
				Log.w("WORKOUT", "PAUSE WORKOUT");
				startResponseService(PAUSE_WORKOUT);
				break;
			}
			//none of the commands were spoken
			default:
				Log.w("WORKOUT", "COMMAND NOT RECOGNIZED");
				startResponseService(COMMAND_NOT_RECOGNIZED);
		}
	}
	/**Handle button clicks */
	@Override
	public void onClick(View view) {
		//starting new stage
		mCommandText.setText("");
		switch (view.getId()){
			case(R.id.speakButton):{
				Log.w("Workout", "start button is pressed");
				if (mPause) {
					resumeTimer();
				}
				startResponseService(RESUME_WORKOUT);
				break;
			}
			case (R.id.stopButton):{
				Log.w("Workout", "stop button is pressed");
				startResponseService(STOP_WORKOUT);
				break;
			}
			case (R.id.pauseButton):{
				Log.w("Workout", "pause button is pressed");
				if (!mPause) {
					pauseTimer();
					startResponseService(PAUSE_WORKOUT);
				}
				break;
			}
		}		
	}
	public void stopWorkout () {
		//check for accidental clicks
		Log.w("Workout", "stopping workout");
		if (!mStartButton.isEnabled()) {
			Log.w("Workout", "stop listening to commands");
			//only do these if they are not null
			//turn on start button
			mStartButton.turnOn();
		}
		//reset everything
		mPauseButton.turnOn();
		mPause = false;
		mStoppedTimerText = (String) mTimer.getText();
		destroyTimer();
		//reset mTimerText to default
		mTimerText = "0 seconds";
		mTimeWhenStopped = 0;
	}
	
	/**sets the text for the display clock */
	public void setDisplayClock(int mode) {
		if(mTimer == null) {
			Log.w("Workout", "(set display clock) timer is null");
		}
		String time = (String) mTimer.getText();
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
				setDisplayClock(HYBRID);
			}
		});
		mTimer.setCorrectBaseAndStart(mInitialCreate, mPause, mTimeWhenStopped, mTimerText);
		if (!mInitialCreate) {
			setDisplayClock(HYBRID, mTimerText);
			if (!mPause) {
				mCommandText.setText("");
			}
			else {
				mPauseButton.turnOff();
			}
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
		mPause = true;
		//save the time when the timer was stopped
		mTimeWhenStopped = mTimer.getBase() - SystemClock.elapsedRealtime();
		mTimer.stop();
		//turn off pause button and turn on start button
		mPauseButton.turnOff();
		mStartButton.turnOn();
	}
	
	/*Resumes the timer */
	public void resumeTimer() {
		//adjust the timer to the correct time
		mPause = false;
		mTimer.setCorrectBaseAndStart(mInitialCreate, mPause, mTimeWhenStopped, mTimerText);
		mPauseButton.turnOn();
	}
	/** Called when activity is interrupted, like orientation change */
	@Override
	protected void onPause() {
		Log.w("Workout", "activity interrupted");
		if (mTimer != null) {
			mTimerText = (String) mTimer.getText();
			Log.w("Workout", "timer Text:  " + mTimerText);
			//for persistence
			if (!mPause) {
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
		//set the text of the display clock
		createTimer();
		Log.w("Workout","initial create:  "+ mInitialCreate);
		if (!mInitialCreate) {
				//persist the time 
				mDisplayClock.setText(Utils.getPrettyHybridTime(mTimerText));
				if (mStoppedTimerText != null) {
					mDisplayClock.setText(Utils.getPrettyHybridTime(mStoppedTimerText));
				}
				//turn of pause button and stop button to prevent accidental clicks
				mPauseButton.turnOff();
				mStopButton.turnOff();
		}
		else {
			//initial create
			Log.w("Workout", "initial create");
			startResponseService(START_WORKOUT);
		}
		
		//create IntentFilter to match with FINISHED_SPEAKING action
		IntentFilter intentFilter = new IntentFilter(ServiceReceiver.FINISHED_SPEAKING);
		//broadcasting an Intent with CATEGORY_DEFAULT, so add this category
		intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
		IntentFilter commandIF = new IntentFilter(ServiceReceiver.FINISHED_LISTENING);
		commandIF.addCategory(Intent.CATEGORY_DEFAULT);
		//initialize the receiver
		mReceiver = new ServiceReceiver();
		/* Register a Broadcast Receiver to be run in the main activity thread
		 * The receiver will be called with any broadcast Intent that matches filter
		 * in the main application thread*/
		this.registerReceiver(mReceiver, intentFilter);
		this.registerReceiver(mReceiver, commandIF);
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
		outState.putBoolean(SAVED_PAUSE, mPause);
		outState.putString(SAVED_PAUSE_COMMAND_TEXT, (String) mCommandText.getText());
		outState.putString(SAVED_STOP_TIMER_TEXT, mStoppedTimerText);
		
		Log.w("Workout", "(save) timer text:  " + mTimerText);
		Log.w("Workout", "(save) time when stopped text:  " + mTimeWhenStopped);
		Log.w("Workout", "(save) initial create:  " + mInitialCreate);
		Log.w("Workout", "(save) pause:  " + mPause);
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
		mPause = savedInstanceState.getBoolean(SAVED_PAUSE);
		mCommandText.setText(savedInstanceState.getString(SAVED_PAUSE_COMMAND_TEXT));
		mStoppedTimerText = savedInstanceState.getString(SAVED_STOP_TIMER_TEXT);

		Log.w("Workout", "(restore) timer text:  " + mTimerText);
		Log.w("Workout", "(restore) time when stopped text:  " + mTimeWhenStopped);
		Log.w("Workout", "(restore) initial create:  " + mInitialCreate);
		Log.w("Workout", "(restore) pause:  " + mPause);
	}

	/**Creates an intent with the specified extra and starts the feedback service */
	public void startResponseService(int response) {
		Log.w("Workout", "starting service");
		Intent intent = new Intent(this, FeedbackService.class);
		intent.putExtra(RESPONSE_STRING, response);
		
		//if it is update or stop, need to pass in time as well
		if (response == UPDATE_WORKOUT || response == STOP_WORKOUT) {
			String timerTime = Utils.getUpdate((String) mTimer.getText());
			if (response == STOP_WORKOUT) {
				//stop the workout before replying
				stopWorkout();
			}
			intent.putExtra(UPDATE_TIME_STRING, timerTime);
		}
		//turn off buttons prevent interruptions for feedback
		mStartButton.turnOff();
		mPauseButton.turnOff();
		mStopButton.turnOff();

		this.startService(intent); 
	}
	public void startListeningService() {
		Log.w("Workout", "starting listening service");
		mListeningServiceIntent = new Intent(this, CommandListeningService.class);
		this.startService(mListeningServiceIntent);
	}
	/** Broadcast Receiver for FeedbackService */
	public class ServiceReceiver extends BroadcastReceiver {
		
		public static final String FINISHED_SPEAKING = "finished speaking";
		public static final String FINISHED_LISTENING = "finished listening";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.w("Workout", "broadcast received");
			Log.w("Workout", "intent action:  " + intent.getAction());
			if (intent.getAction().equals(FINISHED_SPEAKING)) {
				String action = intent.getStringExtra(FeedbackService.START_STOP);
				Log.w("Workout", "action:  " + action);
				if (action.equals(FeedbackService.START)) {
					Log.w("Workout", "On receive intial create:  " + mInitialCreate);
					//for a flash effect, but for pause want text to remain until start again
					if(!mPause) { //still doing the workout
						mCommandText.setText("");
						mPauseButton.turnOn();
						if (mInitialCreate) {
							startListeningService();
							mInitialCreate = false;
							mStartButton.turnOff();
						}
					}
					if (mPause) {
						//disable pause button and turn on start button
						mStartButton.turnOn();
						mPauseButton.turnOff();
					}
				}
				if(action.equals(FeedbackService.STOP)) {
					//maybe use this to prompt the user to save the workout
					//enable the start button and disable the pause button
					mStartButton.turnOn();
					mPauseButton.turnOff();
				}
				
				//stop will always be enabled
				mStopButton.turnOn();
			}
			if (intent.getAction().equals(FINISHED_LISTENING)) {
				Log.w("Workout", "success");
				
			}
		}
	}

	public void onStop() {
		super.onStop();
		Log.e("Workout","in on Stop");
	}
	public void onRestart() {
		Log.e("Workout", "in on Restart");
	}
	public void onDestroy() {
		super.onDestroy();
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
}
