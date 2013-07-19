package edu.uhmanoa.android.handsfreeworkout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class Workout extends Activity implements OnClickListener, TextToSpeech.OnInitListener{

	protected ListView wordsList;
	protected Intent mIntent;
	protected SpeechRecognizer mSpeechRec;
	protected RecognitionListener mSpeechRecListen;
	protected Handler mHandler;
	protected MediaRecorder mRecorder;
	protected ArrayList<Integer> mAverage;
	protected Button mStartButton;
	protected Button mStopButton;
	//so can differentiate between what needs to be said in TTS
	protected HashMap <String, String> replies = new HashMap<String, String>();
	
	protected boolean mSpeechRecAlive;
	protected boolean mFinished;
	protected boolean mDoingSpeechRec;
	protected boolean mCheckBaseline;
	protected boolean mListeningForCommands;
	protected int mBaselineAmp;
	protected TextToSpeech mTts;
	protected int mCounter;
	
	/** Keys for saving and restoring instance data */
	protected static final String SAVED_SPEECH_REC_ALIVE_VALUE = "saved speech";
	protected static final String SAVED_FINISHED_SPEECH_REC_VALUE = "finished speech";
	protected static final String SAVED_DOING_SPEECH_REC_VALUE = "doing speecrech";
	protected static final String SAVED_CHECK_BASELINE_VALUE = "check baseline";
	protected static final String SAVED_BASELINE_AMP_VALUE = "saved baseline";
	protected static final String SAVED_LISTENING_FOR_COMMANDS_VALUE = "listening for commands";
	protected static final String SAVED_AVERAGE_ARRAYLIST_VALUE = "average array list";

	/** Keys for hash table of replies */
	protected static final String WORKOUT_ALREADY_STARTED = "workout already started";
	protected static final String BEGIN_WORKOUT = "begin workout";
	protected static final String STOP_WORKOUT = "workout finished";
	protected static final String WORKOUT_ALREADY_FINISHED = "workout already finished";
	protected static final String UPDATE_WORKOUT = "update";
	protected static final String CREATING_BASELINE = "creating baseline";
	protected static final String FINISHED_BASELINE = "finished baseline";
	protected static final String SILENCE = "silence";
	
	/** The time frequency at which check the max amplitude of the recording */
	protected static final long UPDATE_FREQUENCY = 4000L;
	protected static final long CHECK_FREQUENCY = 2000L; //change this to 50L after debugging
	protected static final long BASELINE_FREQUENCY = 1000L;
		
	/**
	 * ISSUES TO DEAL WITH STILL:
	 * accidental loud noises
	 * leaked services (when phone was moving around)
	 * find a way to persist the TTS? app still works, but get error in LogCat
	 * have it so only check for speech 3 times (silence way only works once on fresh install)
	 */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.workout);
		
		//initialize variables
		mStartButton = (Button) findViewById(R.id.speakButton);
		mStartButton.setOnClickListener(this);
		mStopButton = (Button) findViewById(R.id.stopButton);
		mStopButton.setOnClickListener(this);
		wordsList = (ListView) findViewById(R.id.list);
		mHandler = new Handler();
		mSpeechRecAlive = false;
		mAverage = new ArrayList<Integer>();
		
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
		
		/*initialize TTS (don't need to check if it is installed because for OS 4.1 and up
		it is already included.  But maybe do checks here for older versions later */
		mTts = new TextToSpeech(this,this);
		mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

			@Override
			public void onStart(String arg0) {
				//don't need?	
			}
			
			@Override
			public void onError(String arg0) {
				//don't need?
				
			}
			/**need this so speech recognition doesn't pick up on the feedback */
			@Override
			public void onDone(String utteranceID) {
				String id = utteranceID;
				Log.e("Workout", "utterance:  " + utteranceID);
				
				/**Need to run this on UiThread because listener calls it from separate thread */
				if (id.equals(STOP_WORKOUT)) {

					runOnUiThread(new Runnable() {				
						@Override
						public void run() {					
							stopWorkout();		
						}
					});
				}
				else {
					runOnUiThread(new Runnable() {	
						@Override
						public void run() {	
							startListening();
						}
					});
					
				}
			}
		});
	}
	
	/* Start voice recognition */
	public void startVoiceRec() {
		//flag that we're doing speech recognition
		mDoingSpeechRec = true;
		//for persistence
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
				Log.w("Workout", "on Event");
				
			}

			@Override
			public void onPartialResults(Bundle arg0) {
				Log.w("Workout", "partial results");
			}

			@Override
			public void onReadyForSpeech(Bundle arg0) {
				Log.w("Workout", "ready for speech");
/*				Log.w("Workout", "counter:  " + mCounter);
				if (mCounter >= 3) {
					stopVoiceRec();
					//I don't know why this works instead of startListening, but I think it has to do with threads?
					replies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, SILENCE);
					mTts.speak("", TextToSpeech.QUEUE_FLUSH, replies);
				}*/

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
				// TODO Auto-generated method stub
				
			}
		};
		mSpeechRec.setRecognitionListener(mSpeechRecListen);
		mIntent = new Intent (RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		//this extra is required
		mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		//so can use as a service
		mIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
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
			if (mSpeechRecAlive && mFinished) {
				Log.w("Workout", "confirmed result");
				mSpeechRecAlive = false;
			}
			
			if (!mSpeechRecAlive) {				
				mSpeechRec.destroy();
				startVoiceRec();
			}
			mCounter +=1;
			mHandler.postDelayed(checkSpeechRec, UPDATE_FREQUENCY);
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
						replies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, FINISHED_BASELINE);
						mTts.speak("finished baseline recording", TextToSpeech.QUEUE_FLUSH, replies);
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
					Log.w("Workout", "max amp:  " + maxAmp);
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
	    wordsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
	                results));
		Log.e("Workout", "results");
		int word = 0;
		if (results.contains("start")) {
			word = 1;
		}
		if (results.contains("stop")) {
			word = 2;
		}
		if (results.contains("update")) {
			word = 3;
		}
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
					replies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, WORKOUT_ALREADY_STARTED);
					mTts.speak("workout has already started", TextToSpeech.QUEUE_FLUSH, replies);
				}
				else{
					replies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, BEGIN_WORKOUT);
					mTts.speak("starting workout", TextToSpeech.QUEUE_FLUSH, replies);
				}
				break;
			}
			//stop
			case (2):{
				//workout is in progress
				if (!mStartButton.isEnabled()) {
					replies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, STOP_WORKOUT);
					mTts.speak("stopping workout", TextToSpeech.QUEUE_FLUSH, replies);
				}
				//in limbo
				else{
					replies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, WORKOUT_ALREADY_FINISHED);
					mTts.speak("you are already done with the workout", TextToSpeech.QUEUE_FLUSH, replies);
				}
				break;
			}
			//update
			case (3):{
				replies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UPDATE_WORKOUT);
				mTts.speak("update", TextToSpeech.QUEUE_FLUSH, replies);
				break;
			}
			//none of the commands were spoken
			default:
				startListening();
		}
	}
	/**Handle button clicks */
	@Override
	public void onClick(View view) {
		switch (view.getId()){
			case(R.id.speakButton):{
				Log.w("Workout", "speak button is pressed");
				//inform user of baseline reading
				if (mCheckBaseline) {
					replies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, CREATING_BASELINE);
					mTts.speak("creating baseline", TextToSpeech.QUEUE_FLUSH, replies);				
				}
				else {
					startWorkout();	
				}
				break;
			}
			case (R.id.stopButton):{
				Log.w("Workout", "stop button is pressed");
				stopWorkout();
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
			//reset everything
			mAverage.clear();
			mStartButton.setEnabled(true);
			
		}
	}
	/** Called when activity is interrupted, like orientation change */
	@Override
	protected void onPause() {
		Log.w("Workout", "activity interrupted");
		Log.w("Workout", "(on pause) listening for commands:  " + mListeningForCommands);
		Log.w("Workout", "(on pause) speech rec:  " + mDoingSpeechRec);
		//can't use the stop* methods because of the boolean flags
		if (mRecorder != null) {
			mHandler.removeCallbacks(checkMaxAmp);
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}
		if (mSpeechRec != null) {
			mHandler.removeCallbacks(checkSpeechRec);
			mSpeechRec.destroy();
			mSpeechRec = null;
		}
		Log.w("Workout", "(on pause) baseline:  " + mCheckBaseline);
		super.onPause();
	}
	
	/** Called when user comes back to the activity */
	@Override
	protected void onResume() {
		Log.w("Workout", "on resume");
		//so that can resume, but also considering first run of app
		if (mListeningForCommands) {
			Log.w("Workout", "listening for commands");
			startListening();
		}
		if (mDoingSpeechRec) {
			Log.w("Workout", "resuming speech recognition");
			startVoiceRec();
		}
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
		Log.w("Workout","(save) mCheckBaseLine:  " + mCheckBaseline);
		Log.w("Workout", "(save) doing speechRec:  " + mDoingSpeechRec);
		Log.w("Workout", "(save) listening for commands:  " + mListeningForCommands);
		super.onSaveInstanceState(outState);
	}
	
	/** Restore the instance data*/
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.w("Workout", "restoring instance state");
		super.onRestoreInstanceState(savedInstanceState);
		//retrieve the values and set them
		//maybe test for null and use Bundle.containsKey() method later

		mSpeechRecAlive = savedInstanceState.getBoolean(SAVED_DOING_SPEECH_REC_VALUE);
		mFinished= savedInstanceState.getBoolean(SAVED_FINISHED_SPEECH_REC_VALUE);
		mDoingSpeechRec = savedInstanceState.getBoolean(SAVED_DOING_SPEECH_REC_VALUE);
		mCheckBaseline = savedInstanceState.getBoolean(SAVED_CHECK_BASELINE_VALUE);
		mListeningForCommands = savedInstanceState.getBoolean(SAVED_LISTENING_FOR_COMMANDS_VALUE);
		mBaselineAmp = savedInstanceState.getInt(SAVED_BASELINE_AMP_VALUE);
		mAverage = savedInstanceState.getIntegerArrayList(SAVED_AVERAGE_ARRAYLIST_VALUE);
		Log.w("Workout","(restore) mCheckBaseLine:  " + mCheckBaseline);
		Log.w("Workout", "(restore) doing speechRec:  " + mDoingSpeechRec);
		Log.w("Workout", "(restore) listening for commands:  " + mListeningForCommands);
	}
	/** Called so signal completion of TTS initialization.  Handle the check of TTS installation here.*/
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = mTts.setLanguage(Locale.US);
			//error checking
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				String errorMessage = "The language is not supported, or the language data is missing.  Please check your installation.";
				Toast.makeText(getBaseContext(), errorMessage, Toast.LENGTH_SHORT).show();
			}
		}
	}


}
