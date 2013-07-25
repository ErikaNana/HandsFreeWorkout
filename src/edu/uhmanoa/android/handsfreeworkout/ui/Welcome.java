package edu.uhmanoa.android.handsfreeworkout.ui;
import edu.uhmanoa.android.handsfreeworkout.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class Welcome extends Activity implements View.OnClickListener {

	protected Button startButton;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.welcome);
		setLayoutFont();
		startButton = (Button) findViewById(R.id.speakButton);
		//since Activity implements it
		startButton.setOnClickListener(this);
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
			Intent intent = new Intent(this, Workout.class);
			this.startActivity(intent);
		}
	}
}
