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
	protected boolean mStart= true;
	
	/**Just need this for the listener*/
	protected static final String STRING = "string";
	
/*	protected boolean mInitialized;*/
	
	/**Context needs to be getApplicationContext()*/
	public AsyncVoiceFeedback(Context context, HandsFreeService service) {
		mContext = context;
	}
	
	@Override
	protected Void doInBackground(Integer... params) {
		mReplies.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, STRING);
		createTTS();
		
/*		while (!mInitialized) {
			try { //so thread doesn't think it's in an infinite while loop
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			continue;
		}*/
		
		int action;
		action = params[0];
		switch(action) {
		case HandsFreeService.START_BUTTON_CLICK:{
			mTTS.speak("workout is in progress", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;
		}
		case HandsFreeService.STOP_BUTTON_CLICK:{
			//get update time
			mStart = false;
			mTTS.speak("stopping workout.  Workout duration:  "+ mUpdateTime, TextToSpeech.QUEUE_FLUSH, mReplies);
			break;
		}
		case HandsFreeService.PAUSE_BUTTON_CLICK:{
			mTTS.speak("pausing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;
		}
		case HandsFreeService.UPDATE_TIME:{	
			mTTS.speak(mUpdateTime + "have elapsed", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;
		}
		case HandsFreeService.INITIAL_CREATE:{
			mTTS.speak("begin workout", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;	
		}
		case HandsFreeService.START_BUTTON_RESUME_CLICK:{
			mTTS.speak("continuing workout", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;	
		}
		case HandsFreeService.COMMAND_NOT_RECOGNIZED_RESULT:{
			mTTS.speak("command not recognized", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;	
		}
		case HandsFreeService.ALREADY_PAUSED:{
			mTTS.speak("workout is already paused", TextToSpeech.QUEUE_FLUSH, mReplies);
			break;	
		}
		case HandsFreeService.NOTHING_SAID:{
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
		destroyTTS();
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
/*		mInitialized = true;*/
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
