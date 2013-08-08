package edu.uhmanoa.android.handsfreeworkout.customcomponents;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class WorkoutTypeAdapter extends ArrayAdapter<WorkoutType> {
	Context mContext;
	TextView wDescription;
	WorkoutType mTypes[] = null;
	
	public WorkoutTypeAdapter(Context context, int layoutResourceId, WorkoutType[] types) {
		super(context, layoutResourceId, types);
		mContext = context;
		mTypes = types;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView != null) {
			((WorkoutTypeView)convertView).setTitle(mTypes[position]);
			((WorkoutTypeView)convertView).setDescription(mTypes[position]);
			return convertView;
		}
		else {
			WorkoutTypeView workoutTypeView = new WorkoutTypeView(mContext, 
					mTypes[position].mTitle, mTypes[position].mDescription);
			return workoutTypeView;
		}
	}
}
