package edu.uhmanoa.android.handsfreeworkout.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;
import edu.uhmanoa.android.handsfreeworkout.R;
import edu.uhmanoa.android.handsfreeworkout.customcomponents.CustomStartButton;
import edu.uhmanoa.android.handsfreeworkout.services.HandsFreeService;
import edu.uhmanoa.android.handsfreeworkout.utils.Utils;

public class WelcomeV2 extends Activity implements OnClickListener{
	Button options;
	RadioButton distancePicker;
	CustomStartButton startButton;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.welcome_v2);
		Utils.setLayoutFont(this,this,Utils.WELCOME);
		

		View divider = findViewById(R.id.divider);
		options = (Button) findViewById(R.id.option);
		distancePicker = (RadioButton) findViewById(R.id.distanceRadioButton);
		
		startButton = (CustomStartButton) findViewById(R.id.customStartButton);
		
		//style components
		options.setTextColor(Color.GREEN);
		divider.setBackgroundColor(Color.GREEN);
		options.setBackgroundColor(Color.TRANSPARENT);
		
		//set button listeners
		options.setOnClickListener(this);
		startButton.setOnClickListener(this);
		distancePicker.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					startButton.setEnabled(true);
				}
				else {
					startButton.setEnabled(false);
				}
			}
		});
		//start the service
		startService(new Intent(this, HandsFreeService.class));
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
			case R.id.option:{
				TextView description = (TextView) findViewById(R.id.distanceOptionDescription);
				//toggle the visibility of the options
				if (description.isShown()) {
					options.setTextColor(Color.GREEN);
					description.setVisibility(View.GONE);
				}
				else {
					options.setTextColor(Color.rgb(156, 154, 158));
					description.setVisibility(View.VISIBLE);					
				}
				break;
			}
			case R.id.customStartButton:{
				//start the workout activity
				this.startActivity(new Intent(this,Workout.class));
			}
		}
	}
}
