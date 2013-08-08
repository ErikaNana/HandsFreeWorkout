package edu.uhmanoa.android.handsfreeworkout.customcomponents;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;

public class CustomStartButton extends Button {
	public CustomStartButton(Context context) {
		super(context);
	}
	
	public CustomStartButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		Typeface font = Typeface.createFromAsset(context.getAssets(), "fonts/Cubano-Regular.otf");
		this.setTypeface(font);
		this.setEnabled(false);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (enabled) {
			this.setTextColor(Color.GREEN);
		}
		if (!enabled) {
			this.setTextColor(Color.BLACK);
		}
	}
	
}
