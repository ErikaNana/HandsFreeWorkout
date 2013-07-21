package edu.uhmanoa.android.handsfreeworkout;

import java.util.HashMap;
import java.util.Locale;

import android.app.IntentService;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

public class FeedbackService extends IntentService implements TextToSpeech.OnInitListener {
	/* Name of the string that identifies what the response should be */
	protected static final String RESPONSE_STRING = "response string";
	/* So can differentiate between what needs to be said in TTS */
	protected HashMap <String, String> mReplies = new HashMap<String, String>();
	protected TextToSpeech mTts;
	/* Value to determine what needs to be said */
	protected int mResponse;
	
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
			mTts.setLanguage(Locale.US);
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
		if (!mTts.isSpeaking()) {
			Log.w("FeedbackService", "not speaking");
			//should always be speaking, acts like an error check
			destroyTTS();
			createTTS();
		}
		super.onDestroy();
	}
	/** Turn off and destroy TTS */
	protected void destroyTTS() {
		mTts.stop();
		mTts.shutdown();
	}
	/** Callback method */
	@Override
	protected void onHandleIntent(Intent intent) {
		//get data from the incoming intent
		mResponse = intent.getIntExtra(RESPONSE_STRING, 0);
	}
	/** Determines what needs to be said */
	protected void selectPhrase (int code) {
		//based on the contents of response, say the appropriate thing
		switch (mResponse) {
			case Workout.WORKOUT_ALREADY_STARTED:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, WORKOUT_ALREADY_STARTED);
				mTts.speak("workout has already started", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.RESUME_WORKOUT:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, RESUME_WORKOUT);
				mTts.speak("continuing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.STOP_WORKOUT:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, STOP_WORKOUT);
				mTts.speak("stopping workout", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			
			case Workout.WORKOUT_ALREADY_FINISHED:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, WORKOUT_ALREADY_FINISHED);
				mTts.speak("you are already done with the workout", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.UPDATE_WORKOUT:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UPDATE_WORKOUT);
				mTts.speak("update", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.CREATING_BASELINE:{
				Log.w("FeedbackService", "creating baseline");
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, CREATING_BASELINE);
				mTts.speak("creating baseline", TextToSpeech.QUEUE_FLUSH, mReplies);	
				break;
			}
			case Workout.FINISHED_BASELINE:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, FINISHED_BASELINE);
				mTts.speak("finished baseline recording.  starting workout.", TextToSpeech.QUEUE_FLUSH, mReplies);	
				break;
			}
			case Workout.SILENCE:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, SILENCE);
				mTts.speak("silence", TextToSpeech.QUEUE_FLUSH, mReplies);
				break;
			}
			case Workout.PAUSE_WORKOUT:{
				mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, PAUSE_WORKOUT);
				mTts.speak("pausing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
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
		mTts = new TextToSpeech(getApplicationContext(),this);
		mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

			@Override
			public void onStart(String arg0) {
				//don't need
			}
			
			@Override
			public void onError(String arg0) {
				//don't need
				
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
					announceFinished(START);
				}
			}
		});
	}
}
