package edu.uhmanoa.android.handsfreeworkout.customcomponents;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import edu.uhmanoa.android.handsfreeworkout.R;

@SuppressLint("ViewConstructor")
public class WorkoutTypeView extends RelativeLayout implements OnClickListener{
	TextView mWorkoutTitle;
	Button mExpandButton;
	TextView mExpandDescription;

	public WorkoutTypeView(Context context, String title, String description) {
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		//after inflated, want this object to be the root ViewGroup of the inflated layout
		inflater.inflate(R.layout.workout_type_view, this, true);
		
		//intialize the variables
		mWorkoutTitle = (TextView) findViewById(R.id.workoutTitle);
		mExpandButton = (Button) findViewById(R.id.expandDescriptionButton);
		mExpandDescription = (TextView) findViewById(R.id.expandDescription);
		
		//set the font
		Typeface font = Typeface.createFromAsset(context.getAssets(), "fonts/Cubano-Regular.otf");
		mWorkoutTitle.setTypeface(font);
		mExpandButton.setTypeface(font);
		mExpandDescription.setTypeface(font);
		
		//set the text
		mWorkoutTitle.setText(title);
		mExpandDescription.setText(description);
		
		//style the components
		mExpandButton.setOnClickListener(this);
		mExpandButton.setBackgroundColor(Color.TRANSPARENT);
		mExpandButton.setTextColor(Color.GREEN);
		
	}
	
	public String getTitle() {
		return (String) mWorkoutTitle.getText();
	}
	public void setTitle(WorkoutType type) {
		mWorkoutTitle.setText(type.mTitle);
	}
	public void setDescription(WorkoutType	type) {
		mExpandDescription.setText(type.mDescription);
	}
	public void setTitleColor(int color) {
		mWorkoutTitle.setTextColor(color);
	}
	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		if (mExpandDescription.isShown()) {
			mExpandButton.setTextColor(Color.GREEN);
			mExpandDescription.setVisibility(View.GONE);
		}
		else {
			mExpandButton.setTextColor(Color.rgb(156, 154, 158));
			mExpandDescription.setVisibility(View.VISIBLE);					
		}
	}
}
