package edu.uhmanoa.android.handsfreeworkout;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.Button;

/**Creates a custom button specific to the application's needs */
public class CustomButton extends Button {
	protected final float BUTTON_TEXT_SIZE = 30;
	
	public CustomButton(Context context) {
		super(context);
	}
	
	public CustomButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		//style the button
		this.setBackgroundColor(Color.TRANSPARENT);
		this.setTextSize(BUTTON_TEXT_SIZE);
	}
	
	/*First checks if button is enabled before turning it off */
	public void turnOff () {
		if (this.isEnabled()) {
			this.setEnabled(false);
		}	
	}
	
	/**First checks if button is off before turning it on*/
	public void turnOn () {
		if (!this.isEnabled()) {
			this.setEnabled(true);
		}
	}

}
