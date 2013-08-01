package edu.uhmanoa.android.handsfreeworkout.customcomponents;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import edu.uhmanoa.android.handsfreeworkout.R;

public class DialogActivity extends Activity implements OnClickListener {
	
	protected TextView mDialogText;
	protected Button   mDialogPosButton;
	protected Button   mDialogNegButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dialog);
		
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Edmondsans-Bold.otf");
		
		mDialogText = (TextView) findViewById(R.id.exitText);
		mDialogPosButton = (Button) findViewById(R.id.dialogPositiveButton);
		mDialogNegButton = (Button) findViewById(R.id.dialogNegativeButton);
		
		//set the listeners
		mDialogPosButton.setOnClickListener(this);
		mDialogNegButton.setOnClickListener(this);
		
		//style the components
		mDialogPosButton.setTextColor(Color.RED);
		mDialogPosButton.setBackgroundColor(Color.TRANSPARENT);
		mDialogPosButton.setTypeface(font);
		
		mDialogNegButton.setTextColor(Color.GREEN);
		mDialogNegButton.setBackgroundColor(Color.TRANSPARENT);
		mDialogNegButton.setTypeface(font);
		
		mDialogText.setBackgroundColor(Color.TRANSPARENT);
		mDialogText.setTypeface(font);
		
		//so that dialog won't be closed when touched outside the dialog
		this.setFinishOnTouchOutside(false);
	}
	

	@Override
	public void onClick(View view) {
		Intent returnIntent = new Intent();
		//leave the app
		if (view.getId() == R.id.dialogPositiveButton) {
			setResult(RESULT_OK, returnIntent);
			finish();
		}
		
		//stay in the app
		if (view.getId() == R.id.dialogNegativeButton) {
			setResult(RESULT_CANCELED, returnIntent);
			finish();
		}
	}
	//make sure can't ignore the dialog
	@Override
	public void onBackPressed() {
		return;
	}
}
