package edu.uhmanoa.android.handsfreeworkout.ui;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import edu.uhmanoa.android.handsfreeworkout.R;
import edu.uhmanoa.android.handsfreeworkout.services.HandsFreeService;


public class Welcome extends Activity implements View.OnClickListener, OnInitListener {

	protected Button startButton;
	protected TextToSpeech mTTS;
	protected Intent mHandsFreeIntent;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.welcome);
		setLayoutFont();
		startButton = (Button) findViewById(R.id.speakButton);
		//since Activity implements it
		startButton.setOnClickListener(this);
		startHandsFreeService();
	}
	
	/**Set the layout font */
	public void setLayoutFont() {
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Edmondsans-Bold.otf");
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.welcome_layout);
		int count= layout.getChildCount();
		for (int i = 0; i < count; i++) {
			View view = layout.getChildAt(i);
			if (view instanceof TextView) {
				((TextView) view).setTypeface(font);
			}
			if (view instanceof Button) {
				((Button) view).setTypeface(font);
			}
		}
		
	}

	@Override
	public void onClick(View view) {
		//go to the main workout activity
		if (view.getId() == R.id.speakButton) {
			mTTS.speak("begin workout", TextToSpeech.QUEUE_FLUSH, null);
			Intent intent = new Intent(this, Workout.class);
			this.startActivity(intent);
		}
	}
	
	/** Called to signal completion of TTS initialization.  Handle the check of TTS installation here.*/
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			//just in case again
			if (mTTS == null) {
				mTTS = new TextToSpeech(getApplicationContext(),this);
			}
			mTTS.setLanguage(Locale.US);
		}
		startButton.setEnabled(true);
	}
	
	@Override
	protected void onResume(){
		if (mTTS == null) {
			mTTS = new TextToSpeech(getApplicationContext(),this);
		}
		super.onResume();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mTTS != null) {
			mTTS.stop();
			mTTS.shutdown();
			mTTS = null;
		}
	}
	
	public void startHandsFreeService() {
		Log.w("Workout", "starting hands free service");
		if (mHandsFreeIntent == null) {
			mHandsFreeIntent = new Intent(this, HandsFreeService.class);
		}
		startService(mHandsFreeIntent);
	}
}
