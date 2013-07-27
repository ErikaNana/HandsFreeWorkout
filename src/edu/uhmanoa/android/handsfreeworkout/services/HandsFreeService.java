package edu.uhmanoa.android.handsfreeworkout.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
	
	protected boolean mSpeechRecAlive;
	protected boolean mFinished; //if speech recognition heard something
	protected boolean mDoingVoiceRec;
	protected boolean mCheckBaseline;
	protected boolean mListeningForCommands;
	protected int mBaselineAmp;
	protected int mCounter;
	
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
	
	/**Possible application states to announce*/
	public static final int STOP = 1;
	public static final int START = 2;
	public static final int PAUSE = 3;
	public static final int UPDATE = 4;
	
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
	
	protected String updatedTime;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	@Override
	public void onCreate() {
		createTTS();
		Log.w("HFS", "on create of service");
		IntentFilter iFilter = new IntentFilter (UpdateReceiver.UPDATE);
		iFilter.addCategory(Intent.CATEGORY_DEFAULT);
		mReceiver = new UpdateReceiver();
		this.registerReceiver(mReceiver, iFilter);
		mHandler = new Handler();
		//for persistence
		if(mRecorder == null) {
			startListening();
		}

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
		Log.w("HFS", "hit oninit:  " + status);
	}
	/* Start voice recognition */
	public void startVoiceRec() {
		//starting new stage
		stopListening();
		//for persistence
		Log.w("Workout","Starting voice rec");

		mDoingVoiceRec = true;
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
/*				handleInput(results);*/
			}

			@Override
			public void onRmsChanged(float arg0) {
				//don't need		
			}
		};
		mSpeechRec.setRecognitionListener(mSpeechRecListen);
		Intent mIntent = new Intent (RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		//this extra is required
		mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		// text prompt to show to the user when asking them to speak. 
		mIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice Recognition");
		mSpeechRec.startListening(mIntent);
	}

	/* Stop speech recognition */
	public void stopVoiceRec() {
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
			Log.w("Workout", "checking if alive");
			if (mSpeechRecAlive) {
				if (mFinished) {
					Log.w("Workout", "confirmed result");
					mSpeechRecAlive = false;
					//reset mFinished
					mFinished = false;
				}
			}

			else{				
				mSpeechRec.destroy();
				startVoiceRec();
			}
			if (mCounter < 4) {
				mHandler.postDelayed(checkSpeechRec, UPDATE_FREQUENCY);			
			}
			if (mCounter >= 4) {
				stopVoiceRec();
				Log.w("WORKOUT", "SILENCE");
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
					Log.e("Workout", "spoke at volume:  " + maxAmp);
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
			Log.w("WorkOut", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.w("Workout", e.getMessage());
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
			stopVoiceRec();
		}
	}
	protected void createTTS() {
		Log.w("HFS", "creating TTS");
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
				Log.e("Workout", "utterance:  " + utteranceID);
				if (!utteranceID.equals(STOP_WORKOUT)) {
					startListening();
				}
				if (utteranceID.equals(STOP_WORKOUT)) {
					stopListening();
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
	
	/**Announce to Workout the new application state*/
	protected void announceGetUpdate(int action) {
		Intent announce = new Intent(Workout.ServiceReceiver.UPDATE);
		announce.putExtra(APPLICATION_STATE, action);
		announce.addCategory(Intent.CATEGORY_DEFAULT);
		this.sendBroadcast(announce);
		Log.w("HFS", "announcing update");
	}
	
	public class UpdateReceiver extends BroadcastReceiver {
		
		public static final String UPDATE = "update";
		
		/**Possible action values from Workout*/
		public static final int START_BUTTON_CLICK = 1;
		public static final int STOP_BUTTON_CLICK = 2;
		public static final int PAUSE_BUTTON_CLICK = 3;
		public static final int UPDATE_TIME = 4;
		public static final int INITIAL_CREATE = 5;
		public static final int BEGIN_WORKOUT = 6;
		public static final int START_BUTTON_RESUME_CLICK = 7;
		
		@Override
		public void onReceive(Context context, Intent intent) {
			stopListening();
			//just in case
			createTTS();
			Log.w("HFS", "broadcast received");
			Log.w("HFS", "intent action:  " + intent.getAction());

			//get the update action and the update string
			int action = intent.getIntExtra(Workout.UPDATE_ACTION, 0);
			String updateTime = intent.getStringExtra(Workout.UPDATE_TIME_STRING);
			
			if (updateTime != "") {
				//set this as updateText
				updatedTime = updateTime;
			}

			switch(action) {
				case START_BUTTON_CLICK:{
					mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, WORKOUT_ALREADY_STARTED);
					mTTS.speak("workout is in progress", TextToSpeech.QUEUE_FLUSH, mReplies);
					break;
				}
				case STOP_BUTTON_CLICK:{
					mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, STOP_WORKOUT);
					mTTS.speak("stopping workout.  Workout duration:  ", TextToSpeech.QUEUE_FLUSH, mReplies);
					break;
				}
				case PAUSE_BUTTON_CLICK:{
					mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, PAUSE_WORKOUT);
					mTTS.speak("pausing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
					break;
				}
				case UPDATE_TIME:{
					mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UPDATE_WORKOUT);
					mTTS.speak("have elapsed", TextToSpeech.QUEUE_FLUSH, mReplies);
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
			}
		}
	}


}
