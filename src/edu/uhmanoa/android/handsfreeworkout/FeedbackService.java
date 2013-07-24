package edu.uhmanoa.android.handsfreeworkout;

import java.util.HashMap;
import java.util.Locale;

import android.app.IntentService;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

public class FeedbackService extends IntentService implements TextToSpeech.OnInitListener {
	/* So can differentiate between what needs to be said in TTS */
	protected HashMap <String, String> mReplies = new HashMap<String, String>();
	protected TextToSpeech mTTS;
	/* Value to determine what needs to be said */
	protected int mResponse;
	/*The intent*/
	Intent mIntent;
	
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
	
	/**Intent extra for the the broadcast receiver */
	protected static final String START_STOP = "start stop workout";
	protected static final String START = "start";
	protected static final String STOP = "stop";
	

	public FeedbackService() {
		super("FeedbackService");
	}
	/** Called to signal completion of TTS initialization.  Handle the check of TTS installation here.*/
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			mTTS.setLanguage(Locale.US);
			//handle the intent
			selectPhrase(mResponse);
		}
	}
	@Override
	public void onCreate() {
		Log.w("FeedbackService", "creating service");
		createTTS();
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		Log.w("Feedback Service", "onDestroy");
		if (!mTTS.isSpeaking()) {
			Log.e("FeedbackService", "not speaking");
/*			//should always be speaking, acts like an error check
			destroyTTS();
			createTTS();*/
		}
		super.onDestroy();
	}
	/** Turn off and destroy TTS */
	protected void destroyTTS() {
		mTTS.stop();
		mTTS.shutdown();
		mTTS = null;
	}
	/** Callback method */
	@Override
	protected void onHandleIntent(Intent intent) {
		//get data from the incoming intent
		mIntent = intent;
		mResponse = mIntent.getIntExtra(Workout.RESPONSE_STRING, 0);
	}
	/** Determines what needs to be said */
	protected void selectPhrase (int code) {
		//get the correct string to say if update or stop workout
		String toSay = "";
		if (code == Workout.UPDATE_WORKOUT || code == Workout.STOP_WORKOUT) {
			toSay = mIntent.getStringExtra(Workout.UPDATE_TIME_STRING);
		}
		//based on the contents of response, say the appropriate thing
		switch (mResponse) {
			case Workout.WORKOUT_ALREADY_STARTED:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, WORKOUT_ALREADY_STARTED);
				mTTS.speak("workout is in progress", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.RESUME_WORKOUT:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, RESUME_WORKOUT);
				mTTS.speak("continuing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.STOP_WORKOUT:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, STOP_WORKOUT);
				mTTS.speak("stopping workout.  Workout duration:  " + toSay, TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			
			case Workout.WORKOUT_ALREADY_FINISHED:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, WORKOUT_ALREADY_FINISHED);
				mTTS.speak("you are already done with the workout", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.UPDATE_WORKOUT:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UPDATE_WORKOUT);
				mTTS.speak(toSay + "have elapsed", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.CREATING_BASELINE:{
				Log.w("FeedbackService", "creating baseline");
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, CREATING_BASELINE);
				mTTS.speak("creating baseline", TextToSpeech.QUEUE_FLUSH, mReplies);	
				break;
			}
			case Workout.FINISHED_BASELINE:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, FINISHED_BASELINE);
				mTTS.speak("finished baseline recording.  starting workout.", TextToSpeech.QUEUE_FLUSH, mReplies);	
				break;
			}
			case Workout.SILENCE:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, SILENCE);
				mTTS.speak("silence", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.PAUSE_WORKOUT:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, PAUSE_WORKOUT);
				mTTS.speak("pausing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.COMMAND_NOT_RECOGNIZED:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, COMMAND_NOT_RECOGNIZED);
				mTTS.speak("command not recognized", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
		}	
	}
	/**Broadcast that speaking has finished.  Specifies whether or not to start/stop
	 * the workout based on what has been said in the intent */
	protected void announceFinished(String action) {
		Intent announce = new Intent(Workout.FinishedSpeakingReceiver.FINISHED_SPEAKING);
		announce.putExtra(START_STOP, action);
		//broadcast with default category
		announce.addCategory(Intent.CATEGORY_DEFAULT);
		this.sendBroadcast(announce);
	}
	
	protected void createTTS() {
		/*initialize TTS (don't need to check if it is installed because for OS 4.1 and up
		it is already included.  But maybe do checks here for older versions later */
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
				String id = utteranceID;
				Log.e("Workout", "utterance:  " + utteranceID);
				/* just need to announce the start here because when it is done it 
				 * destroys the TTS object, so can't announce stop */
				if (id.equals(STOP_WORKOUT)) {
					//being a good citizen
					destroyTTS();
					announceFinished(STOP);
				}
				else {
					destroyTTS();
					announceFinished(START);
				}
			}
		});
	}
}
