package edu.uhmanoa.android.handsfreeworkout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class Workout extends Activity implements OnClickListener{

	protected ListView wordsList;
	protected Intent mIntent;
	protected SpeechRecognizer mSpeechRec;
	protected RecognitionListener mSpeechRecListen;
	protected Handler mHandler;
	protected boolean mSpeechRecAlive;
	protected boolean mFinished;
	protected MediaRecorder mRecorder;

	
	/** The time frequency at which check the max amplitude of the recording */
	private static final long CHECK_FREQUENCY = 300L;
	private static final long BASELINE_FREQUENCY = 1000L;
	
	private boolean mCheckBaseline = true;
	private ArrayList<Integer> mAverage;
	private Button mStartButton;
	private Button mStopButton;
	private int mBaselineAmp;
	
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
	}
	
	public void startVoiceRec() {
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
			public void onResults(Bundle arg0) {
				mSpeechRecAlive = true;	
				Log.w("VR", "on Results");
				mSpeechRecAlive = true;	
				mFinished = true;
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
	/** A Handler takes in a Runnable object and schedules its execution; it places the
	 * runnable process as a job in an execution queue to be run after a specified amount
	 * of time
	 * The runnable will be run on the thread to which the handler is attached
	 *  */
	Runnable checkMaxAmp = new Runnable() {
		/**Sets the baseline max amplitude for the first 10 seconds, and for every 1/3 second
		 * after that, checks the max amplitude.  If it hears a sound that has a higher 
		 * amplitude than the one found in the baseline, launches the voice recognizer
		 * activity */
		@Override
		public void run() {
			Long frequency;
			//for first ten seconds do an average
			int maxAmp = mRecorder.getMaxAmplitude();
			if (mCheckBaseline) {
				frequency = Workout.BASELINE_FREQUENCY;
				Log.w("Workout", "setting up baseline");
				Log.w("Workout", "max amp:  " + maxAmp);
				mAverage.add(maxAmp);
				if(mAverage.size() == 10) {
					//set the baseline max amp
					mBaselineAmp = getBaseline();
					mCheckBaseline = false;
				}
			}
			else {
				frequency = Workout.CHECK_FREQUENCY;
				Log.w("Workout", "listening");
				Log.w("Workout", "max amp:  " + maxAmp);
				if (mBaselineAmp > 0) {
					//launch the speech recognizer and stop listening
				}
			}
			Log.w("Workout", "frequency:  " + frequency);
			mHandler.postDelayed(checkMaxAmp, frequency);
		}
	};
	public void startListening() {
		 mRecorder = new MediaRecorder();
		 mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		 mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		 String name = getOutputMediaFilePath();
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
	
	public void stopListening() {
		//speechRec.destroy();
		mHandler.removeCallbacks(checkMaxAmp);
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
	}
	/**Handle button clicks */
	@Override
	public void onClick(View view) {
		switch (view.getId()){
			case(R.id.speakButton):{
				Log.w("Workout", "size of average array (start):  " + this.mAverage.size());
				Log.w("VR", "speak button is pressed");
				startListening();
				//set this button as unclickable to avoid errors
				mStartButton.setEnabled(false);
				break;
			}
			case (R.id.stopButton):{
				Log.w("VR", "stop button is pressed");
				stopListening();
				//reset everything
				mAverage.clear();
				Log.w("Workout", "size of average array(finish):  " + this.mAverage.size());
				mStartButton.setEnabled(true);
				mCheckBaseline = true;
				break;
			}
		}
		
	}
	
	private static String getOutputMediaFilePath(){
		File mediaFile = null;
		//get the base directory where the file gets stored
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		/* construct the file space using the specified directory and name
		 * this is where the pictures will be stored */
		File mediaStorageDir = new File(dir,"HandsFreeWorkout");
		//check to see if there is not a file at the storage directory path contained in mediaStorageDir
		if (!mediaStorageDir.exists()) {
			/* check to make sure that the directory was created correctly
			 * mkdirs returns false if the directory already exists
			 */
			if (!mediaStorageDir.mkdirs()) {
				Log.w("WalkAbout", "directory creation process failed");
				return null;			}
			}
		else {
			//create a new file with the complete path name
			mediaFile = new File(mediaStorageDir.getPath() + File.separator + "recording.3gp");
		}
		Log.w("Workout", mediaFile.getAbsolutePath());
		return mediaFile.getAbsolutePath();
	}
	
	public int getBaseline() {
		int averageTotal = 0;
		for (int number: mAverage) {
			averageTotal += number;
		}
		Log.w("Workout", "average:  " + averageTotal/10);
		return averageTotal/10;
	}


}
