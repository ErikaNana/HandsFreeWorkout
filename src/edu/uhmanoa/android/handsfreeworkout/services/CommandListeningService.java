package edu.uhmanoa.android.handsfreeworkout.services;

import java.io.IOException;
import java.util.ArrayList;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import edu.uhmanoa.android.handsfreeworkout.ui.Workout;
import edu.uhmanoa.android.handsfreeworkout.utils.Utils;

public class CommandListeningService extends IntentService {
	protected boolean mSpeechRecAlive;
	protected boolean mFinished; //if speech recognition heard something
	protected boolean mDoingVoiceRec;
	protected boolean mCheckBaseline;
	protected boolean mListeningForCommands;
	protected int mBaselineAmp;
	protected int mCounter;
	protected boolean mPause;
	protected long mTimeWhenStopped;
	protected String mTimerText;
	protected String mStoppedTimerText;
	protected boolean mInitialCreate; //determines if app is in initial creation state
	protected MediaRecorder mRecorder; //recorder that listens for a command
	protected Handler mHandler;
	protected BroadcastReceiver mReceiver;
	
	protected SpeechRecognizer mSpeechRec; 
	protected RecognitionListener mSpeechRecListen; 
	
	/**Names of extras passing back to Workout */
	public static final String ACTION = "action";
	public static final String ACTION_STRING = "action string";
	
	/**Possible action strings*/
	public static final String STOP_TEXT = "Stop";
	public static final String START_TEXT = "Start";
	public static final String PAUSE_TEXT = "Pause";
	public static final String UPDATE_TEXT = "Update";
	public static final String SILENCE_TEXT = "Silence";
	public static final String COMMAND_NOT_RECOGNIZED_TEXT = "Command Not Recognized";
	
	/**Possible actions*/
	public static final int STOP = 1;
	public static final int START = 2;
	public static final int PAUSE = 3;
	public static final int UPDATE = 4;
	public static final int COMMAND_NOT_RECOGNIZED = 5;
	public static final int SILENCE = 6;
	
	/** The time frequency at which check if speech recognizer is still alive */
	protected static final long UPDATE_FREQUENCY = 4000L;
	
	/** The time frequency at which check the max amplitude of the recording */
	protected static final long CHECK_FREQUENCY = 350L; 
	
	public CommandListeningService() {
		super("CommandListeningService");
	}
	
	public void onCreate() {
		//Log.w("CLS", "OnCreate");
		super.onCreate();
		mHandler = new Handler();
		startListening();
		checkMaxAmp.run();
		createTTS();
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
	}
	/* Start listening for commands */
	public void startListening() {
		mListeningForCommands = true;
		//Log.w("CLS", "start listening");

		//reset the stopped time text
		if (mStoppedTimerText != null) {
			mStoppedTimerText = null;
		}
		if (mRecorder != null) {
			stopAndDestroyRecorder();
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
			checkMaxAmp.run();

		} catch (Error e) {
			//Log.w("WorkOut", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			//Log.w("Workout", e.getMessage());
			e.printStackTrace();
		}
	}
	
	/* Stop listening for commands */
	public void stopListening() {
		//Log.w("CLS", "Stop listening");
		mListeningForCommands = false;
		mHandler.removeCallbacks(checkMaxAmp);
		//just to be safe
		stopAndDestroyRecorder();
/*		//if doing voice rec, destroy it
		if (mSpeechRec != null) {
			stopVoiceRec();
		}*/
	}
	
	/**Stops and destroys the recorder */
	public void stopAndDestroyRecorder() {
		if (mRecorder != null) {
			//Log.w("CLS", "destroying the recorder");
			mRecorder.stop();
			mRecorder.reset();
			mRecorder.release();
			mRecorder = null;
		}
	}
	Runnable checkMaxAmp = new Runnable() {

		/**Sets the baseline max amplitude for the first 10 seconds, and for every 1/3 second
		 * after that, checks the max amplitude.  If it hears a sound that has a higher 
		 * amplitude than the one found in the baseline, launches the voice recognizer
		 * activity 
		 * Cases to consider:  panting (like when you're tired)
		 * 					   unnecessary talking*/
		@Override
		public void run() {
			////Log.w("CLS", "running check max amp");
			mInitialCreate = false;
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
					//Log.e("Workout", "spoke at volume:  " + maxAmp);
					//launch the speech recognizer and stop listening
					destroySpeechRecognizer();
					startVoiceRec();
					checkSpeechRec.run();
				}
				frequency = CommandListeningService.CHECK_FREQUENCY;
				mHandler.postDelayed(checkMaxAmp, frequency);
			}
		}
	};
	Runnable checkSpeechRec = new Runnable() {
		
		@Override
		public void run() {
			mCounter +=1;
			//Log.w("VRS", "Counter:  " + mCounter);
			if (mSpeechRecAlive) {
				if (mFinished) {
					//Log.w("Workout", "confirmed result");
					mSpeechRecAlive = false;
					//reset mFinished
					mFinished = false;
				}
			}
			else{			
				//Log.w("VRS", "speechRec is dead");
				destroySpeechRecognizer();
				startVoiceRec();
			}
			if (mCounter < 4) {
				//Log.w("VRS","counter < 4");
				mHandler.postDelayed(checkSpeechRec, UPDATE_FREQUENCY);			
			}
			if (mCounter >= 4) {
				//Log.w("VRS", "counter > 4");
				stopVoiceRec();
				//Log.w("WORKOUT", "SILENCE");
				//reset mCounter
				 mCounter = 0;
				announceFinished(SILENCE, SILENCE_TEXT);
				//startResponseService(SILENCE);
			}
		}
	};
	/**Initializes and starts voice recognition*/
	public void startVoiceRec() {
		//for persistence
		//Log.w("Workout","Starting voice rec");
		stopListening();
/*		if (mTimer == null) {
			//Log.w("Workout","mTimer is null");
			createTimer();
		}*/
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
				//Log.w("VRS", "error");
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
			}
		};
		mSpeechRec.setRecognitionListener(mSpeechRecListen);
		Intent intent = new Intent (RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
		//this extra is required
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		// text prompt to show to the user when asking them to speak. 
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice Recognition");
		mSpeechRec.startListening(intent);
	}
	
	/* Stop speech recognition */
	public void stopVoiceRec() {
		mHandler.removeCallbacks(checkSpeechRec);
		destroySpeechRecognizer();
	}
	
	public void handleInput(ArrayList<String> results) {
		//use a default layout to display the words
		if (results.contains("start")) {
			announceFinished(START,START_TEXT);
		}
		if (results.contains("stop")) {
			announceFinished(STOP, STOP_TEXT);
		}
		if (results.contains("update")) {
			announceFinished(UPDATE, UPDATE_TEXT);
		}
		if (results.contains("pause")) {
			announceFinished(PAUSE, PAUSE_TEXT);
		}
		else {
			announceFinished(COMMAND_NOT_RECOGNIZED,COMMAND_NOT_RECOGNIZED_TEXT);
		}
	}
	
	public void destroySpeechRecognizer() {
		if (mSpeechRec != null) {
			mSpeechRec.destroy();
			mSpeechRec = null;
		}
	}
	
	public void onDestroy() {
		super.onDestroy();
		//stopAndDestroyRecorder();
		//Log.w("CLS", "CLS on destroy");
	}
	/**Broadcast that speaking has finished.  Specifies whether or not to start/stop
	 * the workout based on what has been said in the intent */
	protected void announceFinished(int action, String action_text) {
		//maintenance
		//stopAndDestroyRecorder(); //if you put this here you're gonna have to call the service all the time instead of one instance
		//Log.w("CLS", "announcing done");
		Intent announce = new Intent(Workout.ServiceReceiver.FINISHED_LISTENING);
		//announce.putExtra(START_STOP, action);
		//broadcast with default category
		announce.addCategory(Intent.CATEGORY_DEFAULT);
		this.sendBroadcast(announce);
	}
	

	
	
}
