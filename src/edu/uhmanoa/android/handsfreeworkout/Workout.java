package edu.uhmanoa.android.handsfreeworkout;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class Workout extends Activity {
	//use to handle the activity
	private static final int REQUEST_CODE = 1234;
	private ListView wordsList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.workout);
		
		//initialize variables
		Button speakButton = (Button) findViewById(R.id.speakButton);
		wordsList = (ListView) findViewById(R.id.list);
		
		//disable button if no recognition service is present
		PackageManager pm = getPackageManager();
		/*queryIntentActivities() = Retrieves all activities that can be performed for
		 * 						    the given intent.  Takes in intent and flags as parameters,
		 * 							which are optional
		 * RecognizerIntent.CONSTANT = Starts an activity that will prompt the user for speech and 
		 * 							   send it through a speech recognizer.  The results will 
		 *                             be returned via activity results (in onActivityResult(int, int, Intent), 
		 *                             if you  start the intent using startActivityForResult(Intent, int))*/
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() == 0) {
			speakButton.setEnabled(false);
			speakButton.setText("Recognizer not present");
		}
	}
	
	/* Method that is called in XML file to handle the action of the button click */
	public void speakButtonClicked(View view) {
		//startVoiceRecognitionActivity();
		Intent intent = new Intent (RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		//this extra is required
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		// text prompt to show to the user when asking them to speak. 
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice Recognition");
		//launch the activity
		startActivityForResult(intent, REQUEST_CODE);
	}
	
	/** Handles the results from the voice recognition activity */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
		//populate the wordsList with the String values the recognition engine thought it heard
			ArrayList<String> words = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			/** android.R.layout.simple_list_item_1 is a default row layout file in res/layout
			 * folder which contains the corresponding design for the row in ListView
			 * We bind the ArrayList items to the row layout using listview.setAdapter() 
			 * bind once, so can just make the adapter and set it all in one line*/
            wordsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                    words));

		}
		//preserve functionality
		super.onActivityResult(requestCode, resultCode, data);
	}

}
