package edu.uhmanoa.android.handsfreeworkout;

import java.io.IOException;
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
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Workout extends Activity implements OnClickListener{
	
	protected Intent mIntent; //intent to start voice rec activity
	protected SpeechRecognizer mSpeechRec; 
	protected RecognitionListener mSpeechRecListen; 
	protected Handler mHandler; //handler that handles the voice rec and speech rec checks
	protected MediaRecorder mRecorder; //recorder that listens for a command
	protected ArrayList<Integer> mAverage; 
	protected Button mStartButton; 
	protected Button mStopButton;
	protected Button mPauseButton;
	protected TextView mWelcomeMessage;
	protected TextView mCommandText;
	protected Chronometer mTimer; 
	protected TextView mDisplayClock;
	
	protected boolean mSpeechRecAlive;
	protected boolean mFinished; //if speech recognition heard something
	protected boolean mDoingSpeechRec;
	protected boolean mCheckBaseline;
	protected boolean mListeningForCommands;
	protected int mBaselineAmp;
	protected int mCounter;
	protected boolean mPause;
	protected FinishedSpeakingReceiver mReceiver;
	protected long mTimeWhenStopped;
	protected String mTimerText;
	protected boolean mInitialCreate; //determines if app is in initial creation state
	
	/** Keys for saving and restoring instance data */
	protected static final String SAVED_SPEECH_REC_ALIVE_VALUE = "saved speech";
	protected static final String SAVED_FINISHED_SPEECH_REC_VALUE = "finished speech";
	protected static final String SAVED_DOING_SPEECH_REC_VALUE = "doing speecrech";
	protected static final String SAVED_CHECK_BASELINE_VALUE = "check baseline";
	protected static final String SAVED_BASELINE_AMP_VALUE = "saved baseline";
	protected static final String SAVED_LISTENING_FOR_COMMANDS_VALUE = "listening for commands";
	protected static final String SAVED_AVERAGE_ARRAYLIST_VALUE = "average array list";
	protected static final String SAVED_TIMER_TEXT_VALUE = "timer text";
	protected static final String SAVED_TIME_WHEN_STOPPED_VALUE = "time when stopped";
	protected static final String SAVED_INITIAL_CREATE = "initial create";
	protected static final String SAVED_COUNTER = "saved counter";
	protected static final String SAVED_PAUSE = "saved pause";
	protected static final String SAVED_PAUSE_COMMAND_TEXT = "saved pause command text";
	
	/** The time frequency at which check if speech recognizer is still alive */
	protected static final long UPDATE_FREQUENCY = 4000L;
	/** The time frequency at which check the max amplitude of the recording */
	protected static final long CHECK_FREQUENCY = 350L; 
	/** The time frequency at which do recording for baseline */
	protected static final long BASELINE_FREQUENCY = 1000L;
	
	/** Name of the string that identifies what the response should be */
	protected static final String RESPONSE_STRING = "response string";
	
	/** Codes for the response that feedback should say */
	protected static final int WORKOUT_ALREADY_STARTED = 1;
	protected static final int STOP_WORKOUT = 2;
	protected static final int WORKOUT_ALREADY_FINISHED = 3;
	protected static final int UPDATE_WORKOUT = 4;
	protected static final int CREATING_BASELINE = 5;
	protected static final int FINISHED_BASELINE = 6;
	protected static final int RESUME_WORKOUT = 7;
	protected static final int SILENCE = 8;
	protected static final int PAUSE_WORKOUT = 9;
	protected static final int COMMAND_NOT_RECOGNIZED = 10;
	
	/**Modes for display clock*/
	protected static final int CLASSIC = 1;
	protected static final int HIPSTER = 2;
	protected static final int HYBRID = 3;
	/**Extra for update */
	protected static final String UPDATE_TIME_STRING = "update value string";
	/**
	 * ISSUES TO DEAL WITH STILL:
	 * accidental loud noises
	 * 
	 * start up error with TTS (creating baseline gets cutoff and repeated)
	 * 
	 * maybe for baseline, require user to say a phrase for x amount of seconds.  
	 * if amp is within 5% of that, then it is a command
	 * 
	 * error checking (can't allow to say paused twice)
	 * 
	 * take out trailing 0 in timer, if it's still seconds just display the number
	 * and then "seconds" or second after it
	 * maybe just don't display the chronometer, but instead display a constantly updating
	 * textView so it can be customized and acts like a timer.  it's like interrupted when
	 * just got to broadcast receiver, didn't have a chance to startListening()
	 * 
	 * deal with when feedback is interrupted (persist feedback if in limbo).  needs to 
	 * start listening again (use a boolean)
	 * 
	 * NOTE: when workout is paused with voice, start button isn't enabled to restart
	 * it.  when workout is paused with button, start button is enabled to restart
	 */
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
		
		mStartButton = (Button) findViewById(R.id.speakButton);
		mStartButton.setOnClickListener(this);
		
		mStopButton = (Button) findViewById(R.id.stopButton);
		mStopButton.setOnClickListener(this);

		
		mPauseButton = (Button) findViewById(R.id.pauseButton);
		mPauseButton.setOnClickListener(this);
		

		mHandler = new Handler();
		mSpeechRecAlive = false;
		mAverage = new ArrayList<Integer>();
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
			mStartButton.setEnabled(false);
			mStartButton.setText("Recognizer not present");
		}
		mCheckBaseline = true;
	}
	
	/* Start voice recognition */
	public void startVoiceRec() {
		//for persistence
		Log.w("Workout","Starting voice rec");
		if (!mCheckBaseline && mTimer == null) {
			Log.w("Workout","mTimer is null");
			createTimer();
		}
		mDoingSpeechRec = true;
		mStartButton.setEnabled(false);
		mSpeechRec = SpeechRecognizer.createSpeechRecognizer(this);
		mSpeechRecListen = new RecognitionListener() {
			
			
			/** Methods to override android.speech */
			@Override
			public void onBeginningOfSpeech() {
				Log.w("Workout", "on beginning of speech");
				mSpeechRecAlive = true;
			}

			@Override
			public void onBufferReceived(byte[] arg0) {
				Log.w("Workout", "buffer received");	
			}

			@Override
			public void onEndOfSpeech() {
				Log.w("Workout", "end of speech");

			}

			@Override
			public void onError(int error) {
				mSpeechRecAlive = false;	
				switch (error) {
					case (SpeechRecognizer.ERROR_AUDIO):{
						Log.w("Workout", "audio");
					}
					case (SpeechRecognizer.ERROR_CLIENT):{
						Log.w("Workout", "client");
					}
					case (SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS):{
						Log.w("Workout", "insufficient permissions");
					}
					case (SpeechRecognizer.ERROR_NETWORK):{
						Log.w("Workout", "network");
					}
					case (SpeechRecognizer.ERROR_NETWORK_TIMEOUT):{
						Log.w("Workout", "network timeout");
					}
					case (SpeechRecognizer.ERROR_NO_MATCH):{
						Log.w("Workout", "no_match");
					}
					case (SpeechRecognizer.ERROR_RECOGNIZER_BUSY):{
						Log.w("Workout", "recognizer busy");
					}
					case (SpeechRecognizer.ERROR_SERVER):{
						Log.w("Workout", "server");
					}
					case (SpeechRecognizer.ERROR_SPEECH_TIMEOUT):{
						Log.w("Workout", "speech timeout");
					}
				}
			}
			@Override
			public void onEvent(int arg0, Bundle arg1) {
				//don't need
			}

			@Override
			public void onPartialResults(Bundle arg0) {
				//don't need
			}

			@Override
			public void onReadyForSpeech(Bundle arg0) {
				Log.w("Workout", "ready for speech");
				Log.w("Workout", "counter:  " + mCounter);
			}
			
			@Override
			public void onResults(Bundle bundle) {
				Log.w("Workout", "on Results");
				mSpeechRecAlive = true;	
				mFinished = true;
				//heard result so stop listening for words
				stopVoiceRec();
				//handle the output
				ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
				handleInput(results);
			}

			@Override
			public void onRmsChanged(float arg0) {
				//don't need		
			}
		};
		mSpeechRec.setRecognitionListener(mSpeechRecListen);
		mIntent = new Intent (RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		//this extra is required
		mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		// text prompt to show to the user when asking them to speak. 
		mIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice Recognition");
		mSpeechRec.startListening(mIntent);
	}
	
	/* Stop speech recognition */
	public void stopVoiceRec() {
		mDoingSpeechRec = false;
		mHandler.removeCallbacks(checkSpeechRec);
		mSpeechRec.destroy();
		mSpeechRec = null;
		mCounter = 0;
	}
	/** A Handler takes in a Runnable object and schedules its execution; it places the
	 * runnable process as a job in an execution queue to be run after a specified amount
	 * of time
	 * The runnable will be run on the thread to which the handler is attached*/
	
	/** This periodically checks to see if there was input into speechRecognizer */
	Runnable checkSpeechRec = new Runnable() {
		
		@Override
		public void run() {
			Log.w("Workout", "checking if alive");
			if (mSpeechRecAlive) {
				if (mFinished) {
					Log.w("Workout", "confirmed result");
					mSpeechRecAlive = false;
				}
			}
			
			else{				
				mSpeechRec.destroy();
				startVoiceRec();
			}
			if (mCounter < 3) {
				mHandler.postDelayed(checkSpeechRec, UPDATE_FREQUENCY);			
			}
			else {
				stopVoiceRec();
				startResponseService(SILENCE);
			}
			mCounter +=1;
		}
	};
	
	Runnable checkMaxAmp = new Runnable() {
		/**Sets the baseline max amplitude for the first 10 seconds, and for every 1/3 second
		 * after that, checks the max amplitude.  If it hears a sound that has a higher 
		 * amplitude than the one found in the baseline, launches the voice recognizer
		 * activity 
		 * Cases to consider:  panting (like when you're tired)
		 * 					   unnecessary talking*/
		@Override
		public void run() {
			//if there is no recorder (like when launch speech recognizer) don't do anything
			if (mRecorder == null) {
				return;
			}
			else {
				Long frequency;
				//for first ten seconds do an average
				int maxAmp = mRecorder.getMaxAmplitude();
			
				if (mCheckBaseline) {
					frequency = Workout.BASELINE_FREQUENCY;
					mAverage.add(maxAmp);
					if(mAverage.size() == 10) {
						//set the baseline max amp
						mBaselineAmp = Utils.getBaseline(mAverage);
						stopListening();
						startResponseService(FINISHED_BASELINE);
						mCheckBaseline = false;
					}
				}
				else {
					//get number of digits
					int digitsCurrent = Utils.getDigits(maxAmp);
					int digitsBaseline = Utils.getDigits(mBaselineAmp);
					
					//if the difference is one or greater, then it is a command
					int difference = digitsCurrent - digitsBaseline;
					if (difference > 0) {
						Log.e("Workout", "spoke at volume:  " + maxAmp);
						//launch the speech recognizer and stop listening
						stopListening();
						startVoiceRec();
						checkSpeechRec.run();
					}
					frequency = Workout.CHECK_FREQUENCY;
				}
				mHandler.postDelayed(checkMaxAmp, frequency);
			}
		}
	};
	/* Start listening for commands */
	public void startListening() {
		mListeningForCommands = true;
		//for persistence
		mStartButton.setEnabled(false);
		if (!mCheckBaseline && mTimer == null) {
			Log.w("Workout","mTimer is null");
			createTimer();
		}

		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		String name = Utils.getOutputMediaFilePath();
		mRecorder.setOutputFile(name);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
			mRecorder.start();
			mInitialCreate = false;
			checkMaxAmp.run();

		} catch (IllegalStateException e) {
			Log.w("WorkOut", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.w("Workout", e.getMessage());
			e.printStackTrace();
		}
	}
	/* Stop listening for commands */
	public void stopListening() {
		mListeningForCommands = false;
		mHandler.removeCallbacks(checkMaxAmp);
		mRecorder.stop();
		mRecorder.reset();
		mRecorder.release();
		mRecorder = null;
	}
	/*Handle input from the speech recognizer */
	public void handleInput(ArrayList<String> results) {
		//use a default layout to display the words
		mDoingSpeechRec = false;
		String command = "command not recognized";
		
		int word = 0;
		if (results.contains("start")) {
			command = "start";
			word = 1;
		}
		if (results.contains("stop")) {
			command = "stop";
			word = 2;
		}
		if (results.contains("update")) {
			command = "update";
			word = 3;
		}
		if (results.contains("pause")) {
			command = "pause";
			word = 4;
		}
		Log.w("Workout", "command:  " + command);
		mCommandText.setText(command);
		replyToCommands(word);
	}
	/** Replies to commands */
	public void replyToCommands(int word) {
		Log.w("Workout", "replying");
		Log.w("Workout", "voice recording:  " + mDoingSpeechRec);
		Log.w("Workout", "listening:  " + mListeningForCommands);
		Log.w("Workout", "mRecorder = " + mRecorder);
		Log.w("Workout", "mSpeechRec = " + mSpeechRec);

		switch(word) {
			//start
			case (1):{
				//workout has already started
				if (!mStartButton.isEnabled()) {
					Log.w("Workout", "pause is:  " + mPause);
					if (mPause) {
						if (mPause) {
							//if it's stopped, start it (for pause case, and need to say "Start" first)
							Log.w("Workout", "timer is stopped");
							resumeTimer();
						}
						startResponseService(RESUME_WORKOUT);
					}
					else {
						startResponseService(WORKOUT_ALREADY_STARTED);						
					}
				}
				break;
			}
			//stop
			case (2):{
				//workout is in progress
				if (!mStartButton.isEnabled()) {
					startResponseService(STOP_WORKOUT);
				}
				//in limbo
				else{
					startResponseService(WORKOUT_ALREADY_FINISHED);
				}
				break;
			}
			//update
			case (3):{
				startResponseService(UPDATE_WORKOUT);
				break;
			}
			//pause
			case (4):{
				pauseTimer();
				startResponseService(PAUSE_WORKOUT);
				break;
			}
			//none of the commands were spoken
			default:
				startResponseService(COMMAND_NOT_RECOGNIZED);
		}
	}
	/**Handle button clicks */
	@Override
	public void onClick(View view) {
		switch (view.getId()){
			case(R.id.speakButton):{
				//to be safe
				mStartButton.setEnabled(false);
				Log.w("Workout", "speak button is pressed");
				//inform user of baseline reading
				if (mCheckBaseline) {
					startResponseService(CREATING_BASELINE);
				}
				else {
					//redundant code
					if (mPause) {
						resumeTimer();
						startResponseService(RESUME_WORKOUT);
					}
					else {
						startResponseService(RESUME_WORKOUT);
					}
				}
				break;
			}
			case (R.id.stopButton):{
				Log.w("Workout", "stop button is pressed");
				stopWorkout();
				break;
			}
			case (R.id.pauseButton):{
				Log.w("Workout", "pause button is pressed");
				if (mListeningForCommands) {
					pauseTimer();
					if(!mStartButton.isEnabled()) {
						stopListening();
						mStartButton.setEnabled(true);
					}
				}
				break;
			}
		}		
	}
	public void startWorkout() {
		//check for accidental clicks
		Log.w("Workout", "starting workout");
		if (!mListeningForCommands) {
			startListening();
			Log.w("Workout", "listening to commands");
			mStartButton.setEnabled(false);
		}
		stopService(getIntent());
	}
	public void stopWorkout () {
		//check for accidental clicks
		Log.w("Workout", "stopping workout");
		if (!mStartButton.isEnabled()) {
			Log.w("Workout", "stop listening to commands");
			//only do these if they are not null
			if (mRecorder != null) {
				stopListening();
				if (mDoingSpeechRec) {
					stopVoiceRec();
				}
			}
			mStartButton.setEnabled(true);
		}
		//reset everything
		mAverage.clear();
		if (!mPauseButton.isEnabled()) {
			mPauseButton.setEnabled(true);
		}
		mPause = false;
		destroyTimer();
		mTimeWhenStopped = 0;
	}
	
	/**sets the text for the display clock */
	public void setDisplayClock(int mode) {
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
		mTimer = (Chronometer) findViewById(R.id.timer);
		mTimer.setOnChronometerTickListener(new OnChronometerTickListener() {
			
			@Override
			public void onChronometerTick(Chronometer chronometer) {
				setDisplayClock(HYBRID);
			}
		});
		/* elapsedRealtime() = returns ms since boot
		 * best method to use to get current time */
		if (mInitialCreate) {
			mTimer.setBase(SystemClock.elapsedRealtime());
			mTimer.start();
		}
		else {
			//persisting the timer
			mTimer.setText(mTimerText);
			setDisplayClock(HYBRID, mTimerText);
			//mDisplayClock.setText(Utils.getPrettyHybridTime(mTimerText));
			
			if (!mPause) {
				mCommandText.setText("");	
				resumeTimer();
				mTimer.start();
			}
			else {
				mTimer.setBase(SystemClock.elapsedRealtime() + mTimeWhenStopped);
				mPauseButton.setEnabled(false);
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
		mPauseButton.setEnabled(false);
	}
	
	/*Resumes the timer */
	public void resumeTimer() {
		//adjust the timer to the correct time
		mTimer.setBase(SystemClock.elapsedRealtime() + mTimeWhenStopped);
		mTimer.start();
		if(!mPauseButton.isEnabled()) {
			mPauseButton.setEnabled(true);
			mPause = false;
		}
	}
	/** Called when activity is interrupted, like orientation change */
	@Override
	protected void onPause() {
		Log.w("Workout", "activity interrupted");
		Log.w("Workout", "(on pause) listening for commands:  " + mListeningForCommands);
		Log.w("Workout", "(on pause) speech rec:  " + mDoingSpeechRec);
		Log.w("Workout", "(on pause) speech rec alive:  " + mSpeechRecAlive);
		if (mTimer != null) {
			mTimerText = (String) mTimer.getText();
			Log.w("Workout", "timer Text:  " + mTimerText);
			//for persistence
			if (!mPause) {
				mTimeWhenStopped = mTimer.getBase() - SystemClock.elapsedRealtime();
			}
		}
		//can't use the stop* methods because of the boolean flags
		if (mRecorder != null) {
			mHandler.removeCallbacks(checkMaxAmp);
			mRecorder.stop();
			mRecorder.reset();
			mRecorder.release();
			mRecorder = null;
		}
		if (mSpeechRec != null) {
			mHandler.removeCallbacks(checkSpeechRec);
			mSpeechRec.destroy();
			mSpeechRec = null;
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
		if (mInitialCreate) {
			mDisplayClock.setText("0 seconds");
		}
		Log.w("Workout","initial create:  "+ mInitialCreate);
		if (mListeningForCommands) {
			Log.w("Workout", "listening for commands");
			startListening();
		}
		if (mDoingSpeechRec) {
			Log.w("Workout", "resuming speech recognition");
			Log.w("Workout", "speech rec alive:  " + mSpeechRecAlive);
			startVoiceRec();
			checkSpeechRec.run();
		}
		
		//create IntentFilter to match with FINISHED_SPEAKING action
		IntentFilter intentFilter = new IntentFilter(FinishedSpeakingReceiver.FINISHED_SPEAKING);
		//broadcasting an Intent with CATEGORY_DEFAULT, so add this category
		intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
		//initialize the receiver
		mReceiver = new FinishedSpeakingReceiver();
		/* Register a Broadcast Receiver to be run in the main activity thread
		 * The receiver will be called with any broadcast Intent that matches filter
		 * in the main application thread*/
		this.registerReceiver(mReceiver, intentFilter);
		super.onResume();
	}
	
	/** Save the instance data */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		//store the current values in outState
		Log.w("Workout", "saving instance state");

		outState.putBoolean(SAVED_SPEECH_REC_ALIVE_VALUE, mSpeechRecAlive);
		outState.putBoolean(SAVED_FINISHED_SPEECH_REC_VALUE, mFinished);
		outState.putBoolean(SAVED_DOING_SPEECH_REC_VALUE, mDoingSpeechRec);
		outState.putBoolean(SAVED_CHECK_BASELINE_VALUE, mCheckBaseline);
		outState.putBoolean(SAVED_LISTENING_FOR_COMMANDS_VALUE, mListeningForCommands);
		outState.putInt(SAVED_BASELINE_AMP_VALUE, mBaselineAmp);
		outState.putIntegerArrayList(SAVED_AVERAGE_ARRAYLIST_VALUE, mAverage);
		outState.putString(SAVED_TIMER_TEXT_VALUE, mTimerText);
		outState.putLong(SAVED_TIME_WHEN_STOPPED_VALUE, mTimeWhenStopped);
		outState.putBoolean(SAVED_INITIAL_CREATE, mInitialCreate);
		outState.putInt(SAVED_COUNTER, mCounter);
		outState.putBoolean(SAVED_PAUSE, mPause);
		outState.putString(SAVED_PAUSE_COMMAND_TEXT, (String) mCommandText.getText());
		
		Log.w("Workout", "(save) doing speechRec:  " + mDoingSpeechRec);
		Log.w("Workout", "(save) listening for commands:  " + mListeningForCommands);
		Log.w("Workout", "(save) timer text:  " + mTimerText);
		Log.w("Workout", "(save) time when stopped text:  " + mTimeWhenStopped);
		Log.w("Workout", "(save) initial create:  " + mInitialCreate);
		Log.w("Workout", "(save) counter:  " + mCounter);
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

		mSpeechRecAlive = savedInstanceState.getBoolean(SAVED_SPEECH_REC_ALIVE_VALUE);
		mFinished= savedInstanceState.getBoolean(SAVED_FINISHED_SPEECH_REC_VALUE);
		mDoingSpeechRec = savedInstanceState.getBoolean(SAVED_DOING_SPEECH_REC_VALUE);
		mCheckBaseline = savedInstanceState.getBoolean(SAVED_CHECK_BASELINE_VALUE);
		mListeningForCommands = savedInstanceState.getBoolean(SAVED_LISTENING_FOR_COMMANDS_VALUE);
		mBaselineAmp = savedInstanceState.getInt(SAVED_BASELINE_AMP_VALUE);
		mAverage = savedInstanceState.getIntegerArrayList(SAVED_AVERAGE_ARRAYLIST_VALUE);
		mTimerText = savedInstanceState.getString(SAVED_TIMER_TEXT_VALUE);
		mTimeWhenStopped = savedInstanceState.getLong(SAVED_TIME_WHEN_STOPPED_VALUE);
		mInitialCreate = savedInstanceState.getBoolean(SAVED_INITIAL_CREATE);
		mCounter = savedInstanceState.getInt(SAVED_COUNTER);
		mPause = savedInstanceState.getBoolean(SAVED_PAUSE);
		mCommandText.setText(savedInstanceState.getString(SAVED_PAUSE_COMMAND_TEXT));

		Log.w("Workout", "(restore) doing speechRec:  " + mDoingSpeechRec);
		Log.w("Workout", "(restore) listening for commands:  " + mListeningForCommands);
		Log.w("Workout", "(restore) timer text:  " + mTimerText);
		Log.w("Workout", "(restore) time when stopped text:  " + mTimeWhenStopped);
		Log.w("Workout", "(restore) initial create:  " + mInitialCreate);
		Log.w("Workout", "(restore) counter:  " + mCounter);
		Log.w("Workout", "(restore) pause:  " + mPause);
	}

	
	/**Creates an intent with the specified extra and starts the feedback service */
	public void startResponseService(int response) {
		Intent intent = new Intent(this, FeedbackService.class);
		intent.putExtra(RESPONSE_STRING, response);
		//if it is update or stop, need to pass in time as well

		if (response == UPDATE_WORKOUT || response == STOP_WORKOUT) {
			String timerTime = Utils.getUpdate((String) mTimer.getText());
			if (response == STOP_WORKOUT) {
				stopWorkout();
			}
			intent.putExtra(UPDATE_TIME_STRING, timerTime);
		}
		
		this.startService(intent); 
	}
	
	/** Broadcast Receiver for FeedbackService */
	public class FinishedSpeakingReceiver extends BroadcastReceiver {
		public static final String FINISHED_SPEAKING = "edu.uhmanoa.android.handsfreeworkout.MESSAGE_PROCESSED";
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.w("Workout", "broadcast received");
			if (intent.getAction().equals(FINISHED_SPEAKING)) {
				String action = intent.getStringExtra(FeedbackService.START_STOP);
				Log.w("Workout", "action:  " + action);
				if (action.equals(FeedbackService.START)) {
					startListening();
				}
				if(action.equals(FeedbackService.STOP)) {
					//maybe use this to prompt the user to save the workout
				}
			}
			else {
				Log.w("Workout", "intent action:  " + intent.getAction());
			}
			//for a flash effect, but for pause want text to remain until start again
			if(!mPause) {
				mCommandText.setText("");			
			}
		}

	}
	/**Set the layout font */
	public void setLayoutFont() {
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Edmondsans-Bold.otf");
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		int count= layout.getChildCount();
		for (int i = 0; i < count; i++) {
			View view = layout.getChildAt(i);
			if (view instanceof TextView) {
				((TextView) view).setTypeface(font);
			}
			if (view instanceof Chronometer) {
				((Chronometer) view).setTypeface(font);
			}
			if (view instanceof Button) {
				((Button) view).setTypeface(font);
			}
		}
		
	}
}
