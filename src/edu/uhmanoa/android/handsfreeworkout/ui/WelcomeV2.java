package edu.uhmanoa.android.handsfreeworkout.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
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

public class WelcomeV2 extends Activity implements OnClickListener, OnCheckedChangeListener{
	Button expandDistance;
	Button expandInterval;
	RadioButton distancePicker;
	RadioButton intervalPicker;
	CustomStartButton startButton;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.welcome_v2);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
		
		//set the font for the title bar
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Cubano-Regular.otf");
		TextView title = (TextView) findViewById(R.id.customTitleStart);
		title.setTypeface(font);
		
		Utils.setLayoutFont(this,this,Utils.WELCOME);

		expandDistance = (Button) findViewById(R.id.expandDistance);
		expandInterval = (Button) findViewById(R.id.expandInterval);
		distancePicker = (RadioButton) findViewById(R.id.distanceRadioButton);
		intervalPicker = (RadioButton) findViewById(R.id.intervalRadioButton);
		startButton = (CustomStartButton) findViewById(R.id.customStartButton);
		
		//style components
		expandDistance.setTextColor(Color.GREEN);
		expandDistance.setBackgroundColor(Color.TRANSPARENT);
		expandInterval.setTextColor(Color.GREEN);
		expandInterval.setBackgroundColor(Color.TRANSPARENT);
		
		//set button listeners
		expandDistance.setOnClickListener(this);
		expandInterval.setOnClickListener(this);
		startButton.setOnClickListener(this);
		distancePicker.setOnCheckedChangeListener(this);
		intervalPicker.setOnCheckedChangeListener(this);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
			case R.id.expandDistance:{
				TextView description = (TextView) findViewById(R.id.distanceOptionDescription);
				//toggle the visibility of the options
				if (description.isShown()) {
					expandDistance.setTextColor(Color.GREEN);
					description.setVisibility(View.GONE);
				}
				else {
					expandDistance.setTextColor(Color.rgb(156, 154, 158));
					description.setVisibility(View.VISIBLE);					
				}
				break;
			}
			case R.id.expandInterval:{
				TextView intervalDescription = (TextView) findViewById(R.id.intervalOptionDescription);
				if (intervalDescription.isShown()) {
					expandInterval.setTextColor(Color.GREEN);
					intervalDescription.setVisibility(View.GONE);
				}
				else {
					expandInterval.setTextColor(Color.rgb(156, 154, 158));
					intervalDescription.setVisibility(View.VISIBLE);	
				}
				break;
			}
			case R.id.customStartButton:{
				//start the workout activity
				if (startButton.isEnabled()) {
					//start the service
					startService(new Intent(this, HandsFreeService.class));
					//depending on which workout is selected, launch the correct one
					this.startActivity(new Intent(this,Workout.class));
				}
			}
		}
	}
	
	//Use a radio group later to manage the radio buttons easier
	@Override
	public void onCheckedChanged(CompoundButton button, boolean isChecked) {
		switch(button.getId()) {
			case R.id.distanceRadioButton:{
				if (isChecked) {
					if (intervalPicker.isChecked()) {
						intervalPicker.setChecked(false);
					}
					startButton.setEnabled(true);
				}
				break;
			}
			case R.id.intervalRadioButton:{
				if (isChecked) {
					if (distancePicker.isChecked()) {
						distancePicker.setChecked(false);
					}
					startButton.setEnabled(true);
				}
			}
		}
	}
}
