package edu.uhmanoa.android.handsfreeworkout.ui;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import edu.uhmanoa.android.handsfreeworkout.R;
import edu.uhmanoa.android.handsfreeworkout.services.HandsFreeService;
import edu.uhmanoa.android.handsfreeworkout.utils.Utils;


public class Welcome extends Activity implements View.OnClickListener{

	protected Button startButton;
	protected Intent mHandsFreeIntent;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.welcome);
		Utils.setLayoutFont(this, this, Utils.WELCOME);
		startButton = (Button) findViewById(R.id.speakButton);
		//since Activity implements it
		startButton.setOnClickListener(this);
		startHandsFreeService();
	}
	
	@Override
	public void onClick(View view) {
		//go to the main workout activity
		if (view.getId() == R.id.speakButton) {
			announceInitialTime();
			Intent intent = new Intent(this, Workout.class);
			this.startActivity(intent);
			finish();
		}
	}
	
	
	public void startHandsFreeService() {
		Log.w("Workout", "starting hands free service");
		if (mHandsFreeIntent == null) {
			mHandsFreeIntent = new Intent(this, HandsFreeService.class);
		}
		startService(mHandsFreeIntent);
	}
	
	protected void announceInitialTime() {
		Log.w("Welcome", "announcing update");
			Intent mUpdateIntent = new Intent(HandsFreeService.UpdateReceiver.GET_UPDATE);
			mUpdateIntent.addCategory(Intent.CATEGORY_DEFAULT);
			mUpdateIntent.putExtra(Workout.UPDATE_ACTION, HandsFreeService.INITIAL_CREATE);
		this.sendBroadcast(mUpdateIntent);
	}
}
