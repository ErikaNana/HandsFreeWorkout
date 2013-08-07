package edu.uhmanoa.android.handsfreeworkout.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import edu.uhmanoa.android.handsfreeworkout.R;
import edu.uhmanoa.android.handsfreeworkout.customcomponents.CustomStartButton;
import edu.uhmanoa.android.handsfreeworkout.services.GPSTrackingService;
import edu.uhmanoa.android.handsfreeworkout.services.HandsFreeService;
import edu.uhmanoa.android.handsfreeworkout.utils.Utils;

public class WelcomeV2 extends Activity implements OnClickListener, OnCheckedChangeListener{
	Button expandDistance;
	Button expandInterval;
	RadioButton distancePicker;
	RadioButton intervalPicker;
	CustomStartButton startButton;
	LocationManager manager;
	
	static final int ENABLE_GPS_REQUEST_CODE = 1;
	
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
		manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public void onResume() {
		super.onResume();
		//connect to GoogleServices
		connectToGoogleServices();
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
				if (startButton.isEnabled()) {
					//check if GPS is enabled
					if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
						showGPSDialog();
					}
					else {
						startGPS();
						startWorkout();
					}
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
	
	
	protected void showGPSDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
				.setMessage("GPS is not enabled!  Continue?")
				.setTitle(R.string.app_name);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				buildNoGPSDialog();
			}
		});
		builder.setNegativeButton("No, enable it now", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//show settings to allow for configuration of current location sources
				startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), ENABLE_GPS_REQUEST_CODE);
			}
		});
		AlertDialog dialog = builder.create();
		//so dialog doesn't get closed when touched outside of it
		dialog.setCanceledOnTouchOutside(false);
		//so dialog doesn't get dismissed by back button
		dialog.setCancelable(false);
		dialog.show();
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ENABLE_GPS_REQUEST_CODE) {
			if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				startWorkout();
			}
			else {	
				buildNoGPSDialog();
			}
		}
		if (requestCode == CONNECTION_FAILURE_RESOLUTION_REQUEST) {
			if (resultCode == Activity.RESULT_OK) {
				//attempt to connect again
				connectToGoogleServices();
			}
		}
	}
	
	protected void buildNoGPSDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
		.setMessage("Workout will continue without GPS enabled")
		.setTitle(R.string.app_name);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				startWorkout();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.setCancelable(false);
		dialog.show();
	}
	
	//starts the workout
	protected void startWorkout() {
		//start workout service
		startService(new Intent(this, HandsFreeService.class));
		//depending on which workout is selected, launch the correct one
		this.startActivity(new Intent(this,Workout.class));
	}
	
	protected void startGPS() {
		startService(new Intent(this, GPSTrackingService.class));
	}
	
	//Define a DialogFragment that displays the error dialog
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
    private void connectToGoogleServices() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.w("Location Updates",
                    "Google Play services is available.");
        // Google Play services was not available for some reason
        } else {
        	Log.w("Welcome", "Google services not available");
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(this.getFragmentManager(), "Location");
            }
        }
    }
}
