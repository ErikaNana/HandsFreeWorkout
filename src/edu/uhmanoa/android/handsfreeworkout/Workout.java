package edu.uhmanoa.android.handsfreeworkout;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class Workout extends Activity {
	//use to handle the activity
	protected ListView wordsList;
	protected Intent intent;
	protected SpeechRecognizer speechRec;
	protected RecognitionListener speechRecListen;
	protected Handler handler;
	protected boolean speechRecAlive;
	protected boolean finished;

	/** The TimerTask which encapsulates the logic that will check for a recognized word. This 
	 * ends up getting run by the Timer in the same way that a Thread runs a Runnable. */
	
	/** The time frequency at which the service should run the listener. (3 seconds) */
	private static final long UPDATE_FREQUENCY = 4000L;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.workout);
		
		//initialize variables
		Button speakButton = (Button) findViewById(R.id.speakButton);
		wordsList = (ListView) findViewById(R.id.list);
		
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
			speakButton.setEnabled(false);
			speakButton.setText("Recognizer not present");
		}
		//initialize variables
		handler = new Handler();
		speechRecAlive = false;
	}
	
	public void startVoiceRec() {
		speechRec = SpeechRecognizer.createSpeechRecognizer(this);
		speechRecListen = new RecognitionListener() {
			
			
			/** Methods to override android.speech */
			@Override
			public void onBeginningOfSpeech() {
				Log.w("VR", "on beginning of speech");
				speechRecAlive = true;
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
				speechRecAlive = false;	
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
			public void onResults(Bundle arg0) {
				speechRecAlive = true;	
				Log.w("VR", "on Results");
				speechRecAlive = true;	
				finished = true;
			}

			@Override
			public void onRmsChanged(float arg0) {
				// TODO Auto-generated method stub
				
			}
		};
		speechRec.setRecognitionListener(speechRecListen);
		intent = new Intent (RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		//this extra is required
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		//so can use as a service
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
		// text prompt to show to the user when asking them to speak. 
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice Recognition");
		speechRec.startListening(intent);
	}
	/** A Handler takes in a Runnable object and schedules its execution; it places the
	 * runnable process as a job in an execution queue to be run after a specified amount
	 * of time
	 * The runnable will be run on the thread to which the handler is attached
	 *  */
	Runnable updateVoiceReg = new Runnable() {
		//what to run at the time interval
		@Override
		public void run() {
			Time now = new Time();
			now.setToNow();
			Log.w("Workout", "checking if alive");
			if (speechRecAlive && finished) {
				Log.w("Workout", "confirmed result");
				speechRecAlive = false;
				
			}
			if (!speechRecAlive) {
				speechRec.destroy();
				startVoiceRec();
				//mute the beeps
			}
			Log.w("Workout", "life status:  " + speechRecAlive);
			/** Causes the Runnable r to be added to the message queue, to be run after the 
			 * specified amount of time elapses. */
			handler.postDelayed(updateVoiceReg, Workout.UPDATE_FREQUENCY);
			
		}
	};
	
	public void startRepeatingTask() {
		updateVoiceReg.run();
	}
	
	public void stopRepeatingTask() {
		handler.removeCallbacks(updateVoiceReg);
		speechRec.destroy();
	}
	/* Method that is called in XML file to handle the action of the button click */
	public void speakButtonClicked(View view) {
		Log.w("VR", "speak button is pressed");
		startVoiceRec();
		startRepeatingTask();
	}
	
	public void stopButtonClicked(View view) {
		Log.w("VR", "stop button is pressed");
		stopRepeatingTask();
	}

}
