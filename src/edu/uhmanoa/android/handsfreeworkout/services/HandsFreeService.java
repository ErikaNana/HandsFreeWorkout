package edu.uhmanoa.android.handsfreeworkout.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import edu.uhmanoa.android.handsfreeworkout.ui.Workout;
import edu.uhmanoa.android.handsfreeworkout.utils.Utils;

public class HandsFreeService extends Service implements TextToSpeech.OnInitListener{
	
	protected SpeechRecognizer mSpeechRec; 
	protected RecognitionListener mSpeechRecListen; 
	protected UpdateReceiver mReceiver;
	protected MediaRecorder mRecorder; //recorder that listens for a command
	protected Handler mHandler;
	protected AlarmManager am;
	protected PendingIntent pi;
	
	/**Intents*/
	protected Intent mRecognizerIntent;
	protected Intent mGetUpdateIntent;
	protected Intent mGetCurrentStateIntent;
	
	protected boolean mSpeechRecAlive;
	protected boolean mFinished; //if speech recognition heard something
	protected boolean mDoingVoiceRec;
	protected boolean mCheckBaseline;
	protected boolean mListeningForCommands;
	protected int mBaselineAmp;
	protected int mCounter;
	protected String mUpdateTime;
	protected boolean mSleeping;
	protected long mBaseTime;
	protected ArrayList<Long> mTotalTime;
	
	/* So can differentiate between what needs to be said in TTS */
	protected HashMap <String, String> mReplies = new HashMap<String, String>();
	protected TextToSpeech mTTS;
	
	/* Value to determine what needs to be said */
	protected int mResponse;
	
	/** The time frequency at which check if speech recognizer is still alive */
	protected static final long UPDATE_FREQUENCY = 4000L;
	
	/** The time frequency at which check the max amplitude of the recording */
	protected static final long CHECK_FREQUENCY = 350L; 
	
	/**Name of the current application state to be passed to Workout*/
	public static final String APPLICATION_STATE = "application state";
	
	/**Possible application states*/
	public static final int STOP = 1;
	public static final int START = 2;
	public static final int PAUSE = 3;
	public static final int ALREADY_PAUSED = 4;
	
	/**Commands that don't require announcing to Workout*/
	public static final int UPDATE = 5;
	public static final int COMMAND_NOT_RECOGNIZED_RESULT = 6;
	
	/** Keys for hash table of replies */
	protected static final String WORKOUT_ALREADY_STARTED = "workout already started";
	protected static final String RESUME_WORKOUT = "resume workout";
	protected static final String STOP_WORKOUT = "workout finished";
	protected static final String WORKOUT_ALREADY_FINISHED = "workout already finished";
	protected static final String UPDATE_WORKOUT = "update";
	protected static final String CREATING_BASELINE = "creating baseline";
	protected static final String FINISHED_BASELINE = "finished baseline";
	protected static final String SILENCE = "silence";
	protected static final String PAUSE_WORKOUT = "pause workout";
	protected static final String COMMAND_NOT_RECOGNIZED = "command not recognized";
	protected static final String START_WORKOUT = "start workout";
	protected static final String ALREADY_PAUSED_WORKOUT = "already paused";
	
	/**Possible action values from Workout*/
	public static final int START_BUTTON_CLICK = 7;
	public static final int STOP_BUTTON_CLICK = 8;
	public static final int PAUSE_BUTTON_CLICK = 9;
	public static final int UPDATE_TIME = 10;
	public static final int INITIAL_CREATE = 11;
	public static final int BEGIN_WORKOUT = 12;
	public static final int START_BUTTON_RESUME_CLICK = 13;
	
	/**Current state of the application*/
	protected boolean mWorkoutRunning;
	protected boolean mWorkoutPaused;
	protected boolean mWorkoutStopped;
	
	/**Command that was said*/
	protected int command;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	@Override
	public void onCreate() {
		createTTS();
		Log.w("HFS", "on create of service");
		IntentFilter iFilter = new IntentFilter (UpdateReceiver.GET_UPDATE);
		iFilter.addCategory(Intent.CATEGORY_DEFAULT);
		IntentFilter updateFilter = new IntentFilter(UpdateReceiver.RECEIVE_CURRENT_STATE);
		updateFilter.addCategory(Intent.CATEGORY_DEFAULT);
		IntentFilter iSleeping = new IntentFilter(UpdateReceiver.SLEEPING);
		iSleeping.addCategory(Intent.CATEGORY_DEFAULT);
		
		mReceiver = new UpdateReceiver();
		this.registerReceiver(mReceiver, iFilter);
		this.registerReceiver(mReceiver, updateFilter);
		this.registerReceiver(mReceiver, iSleeping);
		
		mHandler = new Handler();
		//for persistence
		if(mRecorder == null) {
			startListening();
		}
		mTotalTime = new ArrayList<Long>();
	}
	/** Want service to continue running until it is explicitly stopped*/
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	/** Called to signal completion of TTS initialization.  Handle the check of TTS installation here.*/
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			mTTS.setLanguage(Locale.US);
		}

	}
	/* Start voice recognition */
	public void startVoiceRec() {
		//starting new stage
		stopListening();
		//for persistence
		Log.w("HFS","Starting voice rec");

		mDoingVoiceRec = true;
		mSpeechRec = SpeechRecognizer.createSpeechRecognizer(this);
		mSpeechRecListen = new RecognitionListener() {

			/** Methods to override android.speech */
			@Override
			public void onBeginningOfSpeech() {
				mSpeechRecAlive = true;
			}

			@Override
			public void onBufferReceived(byte[] arg0) {
			}

			@Override
			public void onEndOfSpeech() {
			}

			@Override
			public void onError(int error) {
				mSpeechRecAlive = false;	
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
			}

			@Override
			public void onResults(Bundle bundle) {
				mCounter = 0;
				Log.w("HFS", "on Results");
				mSpeechRecAlive = true;	
				mFinished = true;
				//heard result so stop listening for words
				stopAndDestroyVoiceRec();
				//handle the output
				ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
				command = COMMAND_NOT_RECOGNIZED_RESULT;
				if (results.contains("start")) {
					command = START;
				}
				if (results.contains("pause")) {
					command = PAUSE;
				}
				if (results.contains("stop")) {
					command = STOP;
				}
				if (results.contains("update")) {
					command = UPDATE;
				}
				if (command != COMMAND_NOT_RECOGNIZED_RESULT) {
					if (!mSleeping) {
						announceGetCurrentState();
					}
					if (mSleeping) {
						updateBasedOnUI();
					}
				}
				else {
					respond(COMMAND_NOT_RECOGNIZED_RESULT);
				}
			}

			@Override
			public void onRmsChanged(float arg0) {
				//don't need		
			}
		};
		mSpeechRec.setRecognitionListener(mSpeechRecListen);
		if (mRecognizerIntent == null) {
			mRecognizerIntent = new Intent (RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			//this extra is required
			mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			// text prompt to show to the user when asking them to speak. 
			mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice Recognition");
		}
		mSpeechRec.startListening(mRecognizerIntent);
	}
	/**Simulates accessing the UI.  Responds and updates state variables and time accordingly*/
	private void updateBasedOnUI() {
		switch (command) {
		//app is not listening if stop button is pressed
			case START:{
				if (mWorkoutRunning) {
					respond(START_BUTTON_CLICK);
				}
				if (mWorkoutStopped) { //starting a new workout (can't happen if stopped since stopped listening)
					setStateVariables(true, false, false);
					mBaseTime = SystemClock.elapsedRealtime();
					//because when stopped, pause and stop states are true
					break;
				}
				if (mWorkoutPaused) { //resume after pause
					respond(START_BUTTON_RESUME_CLICK);
					setStateVariables(true, false, false);
					mBaseTime = SystemClock.elapsedRealtime();
				}
				break;
			}
			case STOP:{

				if (mWorkoutRunning) { //stopping the workout
					long timePassed = getTimePassed();
					mTotalTime.add(timePassed);
					//calculate how much total time has passed and respond
				}
				long totalTime = Utils.getTotalTime(mTotalTime);
				mUpdateTime = Utils.getUpdateTimeFromRaw(totalTime);
				respond(STOP_BUTTON_CLICK);
				mTotalTime.clear();
				setStateVariables(false, true, true);
				//reset variables?  Are they needed here?
/*				}
				if (mWorkoutPaused) {
					//just get the total time and respond
				}*/
				break;
			}
			case PAUSE:{
				if (mWorkoutRunning) { //pausing the workout
					setStateVariables(false, true, false);
					respond(PAUSE_BUTTON_CLICK);
					
					long timePassed = getTimePassed();
					mTotalTime.add(timePassed);
					break;
				}
				if (mWorkoutPaused) {
					respond(ALREADY_PAUSED);
				}
				if (mWorkoutStopped) {
					//not listening so no need worry?
				}
				
				break;
			}
			case UPDATE:{
				//get time from UI
				//get the total time passed and respond
				if (mWorkoutRunning) {
					long timePassedRecent = getTimePassed();
					long totalTimeSoFar = Utils.getTotalTime(mTotalTime);
					long totalTime = totalTimeSoFar + timePassedRecent;
					mUpdateTime = Utils.getUpdateTimeFromRaw(totalTime);
				}
				else {
					mUpdateTime = Utils.getUpdateTimeFromRaw(Utils.getTotalTime(mTotalTime));
				}

				respond(UPDATE_TIME);
				break;
			}
			default:{
				respond(COMMAND_NOT_RECOGNIZED_RESULT);
			}
		}
	}
	/* Stop speech recognition */
	public void stopAndDestroyVoiceRec() {
		mDoingVoiceRec = false;
		mHandler.removeCallbacks(checkSpeechRec);
		mSpeechRec.destroy();
		mSpeechRec = null;
	}

	/** This periodically checks to see if there was input into speechRecognizer */
	Runnable checkSpeechRec = new Runnable() {

		@Override
		public void run() {
			mCounter +=1;
			Log.w("HFS", "checking if alive");
			if (mSpeechRecAlive) {
				if (mFinished) {
/*					Log.w("HFS", "confirmed result");*/
					mSpeechRecAlive = false;
					//reset mFinished
					mFinished = false;
				}
			}

			else{
				if (mSpeechRec != null) {
					mSpeechRec.destroy();	
				}
				startVoiceRec();
			}
			if (mCounter < 4) {
				mHandler.postDelayed(checkSpeechRec, UPDATE_FREQUENCY);			
			}
			if (mCounter >= 4) {
				stopAndDestroyVoiceRec();
				Log.w("HFS", "SILENCE");
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, SILENCE);
				mTTS.speak("SILENCE", TextToSpeech.QUEUE_FLUSH,mReplies);
				mCounter = 0;
			}

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
/*			mInitialCreate = false;*/
			//if there is no recorder (like when launch speech recognizer) don't do anything
			if (mRecorder == null) {
				return;
			}
			else {
				Long frequency;
				//for first ten seconds do an average
				int maxAmp = mRecorder.getMaxAmplitude();
				mBaselineAmp = 8000;
				//get number of digits
				int digitsCurrent = Utils.getDigits(maxAmp);
				int digitsBaseline = Utils.getDigits(mBaselineAmp);

				//if the difference is one or greater, then it is a command
				int difference = digitsCurrent - digitsBaseline;
				if (difference > 0) {
					Log.e("HFS", "spoke at volume:  " + maxAmp);
					//launch the speech recognizer and stop listening
					startVoiceRec();
					checkSpeechRec.run();
				}
				frequency = CHECK_FREQUENCY;
				mHandler.postDelayed(checkMaxAmp, frequency);
			}
		}
	};

	/* Start listening for commands */
	public void startListening() {
		//for persistence
		if (mTTS == null) {
			createTTS();
		}
/*		Log.w("HFS", "mRecorder:  " + mRecorder);
		Log.w("HFS", "mSpeechRec:  " + mSpeechRec);*/
		mListeningForCommands = true;
		Log.w("HFS", "start listening");
		//for persistence

		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		String name = Utils.getOutputMediaFilePath();
		mRecorder.setOutputFile(name);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
			mRecorder.start();
			checkMaxAmp.run();

		} catch (Error e) {
			Log.w("HFS", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.w("HFS", e.getMessage());
			e.printStackTrace();
		}
	}
	/* Stop listening for commands */
	public void stopListening() {
		Log.w("HFS", "Stop listening");
		mListeningForCommands = false;
		mHandler.removeCallbacks(checkMaxAmp);
		//just to be safe
		stopAndDestroyRecorder();
		//if doing voice rec, destroy it
		if (mSpeechRec != null) {
			stopAndDestroyVoiceRec();
		}
	}
	
	protected void createTTS() {
		Log.e("HFS", "create TTS");
		/*initialize TTS (don't need to check if it is installed because for OS 4.1 and up
		it is already included.  But maybe do checks here for older versions later */
		if (mTTS!= null) {
			return;
		}
		mTTS = new TextToSpeech(getApplicationContext(),this);
		mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {

			@Override
			public void onStart(String arg0) {
				//don't need
			}

			@Override
			public void onError(String arg0) {
				Log.e("FBS", "on Error");

			}
			/**need this so speech recognition doesn't pick up on the feedback */
			@Override
			public void onDone(String utteranceID) {
				Log.e("HFS", "utterance:  " + utteranceID);
				if (!utteranceID.equals(STOP_WORKOUT)) {
					//delay it to be safe
					mHandler.removeCallbacks(checkSpeechRec);
					mHandler.removeCallbacks(checkSpeechRec);
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							startListening();
						}
					}, 250L);

				}
				if (utteranceID.equals(STOP_WORKOUT)) {
					//already stopped listening in respond()
				}
			}
		});
	}
	public void destroySpeechRecognizer() {
		if (mSpeechRec != null) {
			mSpeechRec.destroy();
			mSpeechRec = null;
		}
	}
	/**Stops and destroys the recorder */
	public void stopAndDestroyRecorder() {
		if (mRecorder != null) {
			mRecorder.stop();
			mRecorder.reset();
			mRecorder.release();
			mRecorder = null;
		}
	}
	/** Turn off and destroy TTS */
	protected void destroyTTS() {
		mTTS.stop();
		mTTS.shutdown();
		mTTS = null;
	}
	@Override
	public void onDestroy() {
		Log.w("HFS", "on destroy of service");
		destroySpeechRecognizer();
		stopAndDestroyRecorder();
		destroyTTS();
		this.unregisterReceiver(mReceiver);
	}
	
	/**Updates the UI and gets current time in app for updates*/
	protected void announceGetUpdate(int action) {
		if (mGetUpdateIntent == null) {
			mGetUpdateIntent = new Intent(Workout.ServiceReceiver.UPDATE);
			mGetUpdateIntent.addCategory(Intent.CATEGORY_DEFAULT);
		}
		mGetUpdateIntent.putExtra(APPLICATION_STATE, action);
		//Wake up the app just in case
		this.sendBroadcast(mGetUpdateIntent);
		Log.w("HFS", "announcing update");
	}
	
	/**Sends a broadcast to get the current state from Workout.  Only called if command is
	 * recognized in VoiceRec */
	protected void announceGetCurrentState() {
		if (mGetCurrentStateIntent == null) {
			mGetCurrentStateIntent = new Intent(Workout.ServiceReceiver.GET_CURRENT_STATE);			
		}
		this.sendBroadcast(mGetCurrentStateIntent);			

		Log.w("HFS", "announcing get current state");
	}
	
	protected void respond (int action) {
		Log.e("HFS", "respond");
		stopListening();
		createTTS();
		switch(action) {
			case START_BUTTON_CLICK:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, WORKOUT_ALREADY_STARTED);
				mTTS.speak("workout is in progress", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case STOP_BUTTON_CLICK:{
				//get update time
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, STOP_WORKOUT);
				mTTS.speak("stopping workout.  Workout duration:  "+ mUpdateTime, TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case PAUSE_BUTTON_CLICK:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, PAUSE_WORKOUT);
				mTTS.speak("pausing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case UPDATE_TIME:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UPDATE_WORKOUT);
				mTTS.speak(mUpdateTime + "have elapsed", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case INITIAL_CREATE:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, START_WORKOUT);
				mTTS.speak("begin workout", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;	
			}
			case START_BUTTON_RESUME_CLICK:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, RESUME_WORKOUT);
				mTTS.speak("continuing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;	
			}
			case COMMAND_NOT_RECOGNIZED_RESULT:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, COMMAND_NOT_RECOGNIZED);
				mTTS.speak("command not recognized", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;	
			}
			case ALREADY_PAUSED:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ALREADY_PAUSED_WORKOUT);
				mTTS.speak("workout is already paused", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;	
			}
		}
	}
	public class UpdateReceiver extends BroadcastReceiver {
		
		public static final String GET_UPDATE = "get update";
		public static final String RECEIVE_CURRENT_STATE = "receive current state";
		public static final String SLEEPING = "sleeping";
		
		/**Names of the current states and base time*/
		public static final String WORKOUT_RUNNING = "workout running";
		public static final String WORKOUT_PAUSED = "workout paused";
		public static final String WORKOUT_STOPPED = "Workout stopped";
		public static final String CURRENT_BASE_TIME = "current base time";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String type = intent.getAction();
			/**This is sent in onPause of Workout*/
			if (type.equals(SLEEPING)) {
				mSleeping = intent.getBooleanExtra(Workout.APP_SLEEPING, false);
				
				//update variables if sleeping
				if (mSleeping) {
					//states are now update
					mWorkoutRunning = intent.getBooleanExtra(WORKOUT_RUNNING, false);
					mWorkoutPaused = intent.getBooleanExtra(WORKOUT_PAUSED, false);
					mWorkoutStopped = intent.getBooleanExtra(WORKOUT_STOPPED, false);
					mBaseTime = intent.getLongExtra(CURRENT_BASE_TIME, 0);
				}
				Log.e("HFS", "APP IS SLEEPING:  " + mSleeping);
			}
			if (type.equals(GET_UPDATE)) {
				Log.w("HFS", "broadcast received: " + intent.getAction());

				//get the update action and the update string
				int action = intent.getIntExtra(Workout.UPDATE_ACTION, 0);
				String updateTime = intent.getStringExtra(Workout.UPDATE_TIME_STRING);
				
				if (updateTime != "") {
					//set this as updateText
					mUpdateTime = updateTime;
				}
				respond(action);
			}
			/**This only happens when a command is recognized in VoiceRec*/
			if (type.equals(RECEIVE_CURRENT_STATE)) {
				Log.w("HFS", "receiving the current state");
				mWorkoutRunning = intent.getBooleanExtra(WORKOUT_RUNNING, false);
				mWorkoutPaused = intent.getBooleanExtra(WORKOUT_PAUSED, false);
				mWorkoutStopped = intent.getBooleanExtra(WORKOUT_STOPPED, false);
				Log.w("HFS","workout running:  " + mWorkoutRunning);
				Log.w("HFS","workout paused:  " + mWorkoutPaused);
				Log.w("HFS","workout stopped:  " + mWorkoutStopped);
			
				/*Do stuff according to the current state of the app*/
				switch (command) {
				//app is not listening if stop button is pressed
					case START:{
						if (mWorkoutRunning) {
							respond(START_BUTTON_CLICK);
						}
						if (mWorkoutPaused) {
							respond(START_BUTTON_RESUME_CLICK);
						}
						announceGetUpdate(HandsFreeService.START);
						break;
					}
					case STOP:{
						announceGetUpdate(HandsFreeService.STOP);
						break;
					}
					case PAUSE:{
						if (mWorkoutRunning) {
							respond(PAUSE_BUTTON_CLICK);
							announceGetUpdate(HandsFreeService.PAUSE);
						}
						if (mWorkoutPaused) {
							respond(ALREADY_PAUSED);
						}
						break;
					}
					case UPDATE:{
						//get time from UI
						announceGetUpdate(HandsFreeService.UPDATE);
						break;
					}
					default:{
						respond(COMMAND_NOT_RECOGNIZED_RESULT);
					}
				}
			}
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
	
	/**Gets how much time has passed between the base and the current time, parses it 
	 * and adds it to the mTotalTime ArrayList*/
	public long getTimePassed() {
		long timePassed = Utils.getParsedTime((SystemClock.elapsedRealtime() - mBaseTime));
		for (Long thing: mTotalTime) {
			Log.w("HFS", "timePassed:  " + thing);
		}
		return timePassed;
	}
}
