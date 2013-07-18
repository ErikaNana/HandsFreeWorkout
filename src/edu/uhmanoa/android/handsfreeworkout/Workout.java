package edu.uhmanoa.android.handsfreeworkout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class Workout extends Activity implements OnClickListener{

	protected ListView wordsList;
	protected Intent mIntent;
	protected SpeechRecognizer mSpeechRec;
	protected RecognitionListener mSpeechRecListen;
	protected Handler mHandler;
	protected MediaRecorder mRecorder;
	protected ArrayList<Integer> mAverage;
	protected Button mStartButton;
	protected Button mStopButton;
	
	protected boolean mSpeechRecAlive;
	protected boolean mFinished;
	protected boolean mDoingSpeechRec;
	protected boolean mCheckBaseline;
	protected boolean mListeningForCommands;
	protected int mBaselineAmp;
	
	protected static final String SAVED_SPEECH_REC_ALIVE_VALUE = "saved speech";
	protected static final String SAVED_FINISHED_SPEECH_REC_VALUE = "finished speech";
	protected static final String SAVED_DOING_SPEECH_REC_VALUE = "doing speecrech";
	protected static final String SAVED_CHECK_BASELINE_VALUE = "check baseline";
	protected static final String SAVED_BASELINE_AMP_VALUE = "saved baseline";
	protected static final String SAVED_LISTENING_FOR_COMMANDS_VALUE = "listening for commands";
	protected static final String SAVED_AVERAGE_ARRAYLIST_VALUE = "average array list";
	
	/** The time frequency at which check the max amplitude of the recording */
	protected static final long UPDATE_FREQUENCY = 4000L;
	protected static final long CHECK_FREQUENCY = 1000L; //change this to 50L after debugging
	protected static final long BASELINE_FREQUENCY = 1000L;

	/**
	 * ISSUES TO DEAL WITH STILL:
	 * accidental loud noises
	 * leaked services (when phone was moving around)
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
				Log.w("VR", "on beginning of speech");
				mSpeechRecAlive = true;
			}

			@Override
			public void onBufferReceived(byte[] arg0) {
				Log.w("VR", "buffer received");	
			}

			@Override
			public void onEndOfSpeech() {
				Log.w("VR", "end of speech");

			}

			@Override
			public void onError(int error) {
				mSpeechRecAlive = false;	
				switch (error) {
					case (SpeechRecognizer.ERROR_AUDIO):{
						Log.w("VR", "audio");
					}
					case (SpeechRecognizer.ERROR_CLIENT):{
						Log.w("VR", "client");
					}
					case (SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS):{
						Log.w("VR", "insufficient permissions");
					}
					case (SpeechRecognizer.ERROR_NETWORK):{
						Log.w("VR", "network");
					}
					case (SpeechRecognizer.ERROR_NETWORK_TIMEOUT):{
						Log.w("VR", "network timeout");
					}
					case (SpeechRecognizer.ERROR_NO_MATCH):{
						Log.w("VR", "no_match");
					}
					case (SpeechRecognizer.ERROR_RECOGNIZER_BUSY):{
						Log.w("VR", "recognizer busy");
					}
					case (SpeechRecognizer.ERROR_SERVER):{
						Log.w("VR", "server");
					}
					case (SpeechRecognizer.ERROR_SPEECH_TIMEOUT):{
						Log.w("VR", "speech timeout");
					}
				}
			}
			@Override
			public void onEvent(int arg0, Bundle arg1) {
				Log.w("VR", "on Event");
				
			}

			@Override
			public void onPartialResults(Bundle arg0) {
				Log.w("VR", "partial results");
			}

			@Override
			public void onReadyForSpeech(Bundle arg0) {
				Log.w("VR", "ready for speech");	
			}

			@Override
			public void onResults(Bundle bundle) {
				Log.w("VR", "on Results");
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
			mHandler.postDelayed(checkSpeechRec, Workout.UPDATE_FREQUENCY);
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
					Log.w("Workout", "current digits" + digitsCurrent);
					Log.w("Workout", "baseline digits" + digitsBaseline);
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
		 mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

		 try {
			mRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.w("WorkOut", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.w("Workout", e.getMessage());
			e.printStackTrace();
		}
		mRecorder.start();
		checkMaxAmp.run();
	}
	/* Stop listening for commands */
	public void stopListening() {
		mListeningForCommands = false;
		mHandler.removeCallbacks(checkMaxAmp);
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
	}
	/*Handle input from the speech recognizer */
	public void handleInput(ArrayList<String> results) {
		//convert String Array to String ArrayList to match constructor for ArrayAdapter
	        wordsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
	                results));
		Log.e("Workout", "results");
		//start listening for commands again
		startListening();
 
	}
	/**Handle button clicks */
	@Override
	public void onClick(View view) {
		switch (view.getId()){
			case(R.id.speakButton):{
				Log.w("Workout", "speak button is pressed");
				//check for accidental clicks
				if (!mListeningForCommands) {
					startListening();
					mStartButton.setEnabled(false);
				}
				break;
			}
			case (R.id.stopButton):{
				Log.w("Workout", "stop button is pressed");
				//check for accidental clicks
				if (mListeningForCommands) {
					stopListening();
					if (mDoingSpeechRec) {
						stopVoiceRec();
					}
					//reset everything
					mAverage.clear();
					mStartButton.setEnabled(true);
					mCheckBaseline = true;
				}
				break;
			}
		}
		
	}
	

	/** Called when activity is interrupted, like orientation change */
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
	/**
	 * Save the instance data
	 */
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
	
	/**
	 * Restore the instance data
	 */
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
}
