package edu.uhmanoa.android.handsfreeworkout;

import java.io.File;
import java.util.ArrayList;
import android.os.Environment;
import android.util.Log;

/**
 * This is a helper class with useful functions.
 * Made the methods static so that an instance of the class is not needed to call the methods.
 */
public class Utils {

	public static int getBaseline(ArrayList<Integer> mAverage) {
		int averageTotal = 0;
		for (int number: mAverage) {
			averageTotal += number;
		}
		Log.w("Workout", "average:  " + averageTotal/10);
		return averageTotal/10;
	}
	//later make this a file that is private to the application
	public static String getOutputMediaFilePath(){
		File mediaFile = null;
		//get the base directory where the file gets stored
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		/* construct the file space using the specified directory and name
		 * this is where the pictures will be stored */
		File mediaStorageDir = new File(dir,"HandsFreeWorkout");
		//check to see if there is not a file at the storage directory path contained in mediaStorageDir
		if (!mediaStorageDir.exists()) {
			/* check to make sure that the directory was created correctly
			 * mkdirs returns false if the directory already exists
			 */
			if (!mediaStorageDir.mkdirs()) {
				Log.w("WalkAbout", "directory creation process failed");
				return null;			}
			}
		else {
			//create a new file with the complete path name
			mediaFile = new File(mediaStorageDir.getPath() + File.separator + "recording.3gp");
		}
		Log.w("Workout", mediaFile.getAbsolutePath());
		return mediaFile.getAbsolutePath();
	}
	
	public static int getDigits(int number) {
		String stringNumber = String.valueOf(number);
		return stringNumber.length();
	} 
	
}
