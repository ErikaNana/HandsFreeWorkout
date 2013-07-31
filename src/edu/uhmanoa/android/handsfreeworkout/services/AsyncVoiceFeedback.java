package edu.uhmanoa.android.handsfreeworkout.services;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

public class AsyncVoiceFeedback extends AsyncTask<Integer, Void, Void> implements TextToSpeech.OnInitListener{
	protected TextToSpeech mTTS;
	protected Context mContext;
	protected HashMap <String, String> mReplies = new HashMap<String, String>();
	protected String mUpdateTime;
	protected Intent mDoneSpeakingIntent;
	protected boolean mStart;
	
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
		
	/** Name of action to be done in HandsFreeService*/
	public static final String START_STOP_ACTION = "start stop action";
	protected HandsFreeService mService;
	
	protected boolean mInitialized;
	
	/**Context needs to be getApplicationContext()*/
	public AsyncVoiceFeedback(Context context, HandsFreeService service) {
		mContext = context;
	}
	
	@Override
	protected Void doInBackground(Integer... params) {
		createTTS();
		
		while (!mInitialized) {
			continue;
		}
		
		int action;
		action = params[0];
		switch(action) {
		case HandsFreeService.START_BUTTON_CLICK:{
			mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, WORKOUT_ALREADY_STARTED);
			mTTS.speak("workout is in progress", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;
		}
		case HandsFreeService.STOP_BUTTON_CLICK:{
			//get update time
			mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, STOP_WORKOUT);
			mTTS.speak("stopping workout.  Workout duration:  "+ mUpdateTime, TextToSpeech.QUEUE_FLUSH, mReplies);
			break;
		}
		case HandsFreeService.PAUSE_BUTTON_CLICK:{
			mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, PAUSE_WORKOUT);
			mTTS.speak("pausing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;
		}
		case HandsFreeService.UPDATE_TIME:{
			mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UPDATE_WORKOUT);
			mTTS.speak(mUpdateTime + "have elapsed", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;
		}
		case HandsFreeService.INITIAL_CREATE:{
			mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, START_WORKOUT);
			mTTS.speak("begin workout", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;	
		}
		case HandsFreeService.START_BUTTON_RESUME_CLICK:{
			mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, RESUME_WORKOUT);
			mTTS.speak("continuing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;	
		}
		case HandsFreeService.COMMAND_NOT_RECOGNIZED_RESULT:{
			mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, COMMAND_NOT_RECOGNIZED);
			mTTS.speak("command not recognized", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;	
		}
		case HandsFreeService.ALREADY_PAUSED:{
			mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ALREADY_PAUSED_WORKOUT);
			mTTS.speak("workout is already paused", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;	
		}
		case HandsFreeService.NOTHING_SAID:{
			mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, SILENCE);
			mTTS.speak("silence", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;	
		}
	}
		return null;
	}
	
	protected void createTTS() {
		Log.e("HFS", "create TTS");
		/*initialize TTS (don't need to check if it is installed because for OS 4.1 and up
		it is already included.  But maybe do checks here for older versions later */
		if (mTTS!= null) {
			return;
		}

		mTTS = new TextToSpeech(mContext,this);
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
				Log.e("AVFB", "utterance:  " + utteranceID);
				if (!utteranceID.equals(STOP_WORKOUT)) {
				//broadcast done talking and should start listening again
					mStart = true;
				}
				if (utteranceID.equals(STOP_WORKOUT)) {
					//already stopped listening in respond()
					mStart = false;
				}
				Log.w("AVFB", "Stop is:  " + mStart);
				Log.e("AVFB", "YES!!!!");
				announceDoneSpeaking();
			}
		});
	}
	
	protected void announceDoneSpeaking() {
		mDoneSpeakingIntent = new Intent(HandsFreeService.UpdateReceiver.DONE_SPEAKING);
		mDoneSpeakingIntent.putExtra(HandsFreeService.UpdateReceiver.START_OR_NOT, mStart);
		mContext.sendBroadcast(mDoneSpeakingIntent);			
		Log.w("HFS", "announcing get current state");
	}
	@Override
	public void onInit(int status) {
		while (mTTS == null) {
			createTTS();
		}
		Log.w("AVFBS", "mTTS:  " + mTTS);
		if (status == TextToSpeech.SUCCESS) {
			mTTS.setLanguage(Locale.US);
		}
		mInitialized = true;
	}
	
	/** Turn off and destroy TTS */
	protected void destroyTTS() {
		mTTS.stop();
		mTTS.shutdown();
		mTTS = null;
	}
	
	protected void setUpdateTime(String updateTime) {
		mUpdateTime = updateTime;
	}
}
