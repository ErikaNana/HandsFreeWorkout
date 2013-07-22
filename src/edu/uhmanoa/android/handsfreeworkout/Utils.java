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
	/* Get the average max amplitude */
	public static int getBaseline(ArrayList<Integer> mAverage) {
		int averageTotal = 0;
		for (int number: mAverage) {
			averageTotal += number;
		}
		Log.w("Workout", "average:  " + averageTotal/10);
		return averageTotal/10;
	}
	/*Get the file path for voice recording output*/
	//later make this a file that is private to the application
	public static String getOutputMediaFilePath(){
		File mediaFile = null;
		//get the base directory where the file gets stored
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		/* construct the file space using the specified directory and name
		 * this is where the file from the voice recording will be stored */
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
	
	/*Get the number of digits for a given number */
	public static int getDigits(int number) {
		String stringNumber = String.valueOf(number);
		return stringNumber.length();
	} 
	
	/**Get the correct update string */
	public static String getUpdate(String inputTime) {
		int hours = 0;
		int minutes;
		int seconds;
		
		String correctHour = " hours,";
		String correctMinute = " minutes and ";
		String correctSecond = " seconds ";
		
		String timeString = "";
		
		//MM:SS or H:MM:SS form
		String[] time = inputTime.split(":");
		//there are hours
		if (time.length > 2) {
			hours = Integer.valueOf(time[0]);
			minutes = Integer.valueOf(time[1]);
			seconds = Integer.valueOf(time[2]);
			if (hours == 1) {
				correctHour = " hour, ";
			}
		}
		else {
			minutes = Integer.valueOf(time[0]);
			seconds = Integer.valueOf(time[1]);
		}

		if (minutes == 1) {
			correctMinute = " minute and ";
		}
		
		if (seconds == 1) {
			correctSecond = " second ";
		}
		
		if (minutes == 0) {
			timeString = seconds + correctSecond;
			return timeString;
		}
		
		if (time.length > 2) {
			timeString = hours + correctHour + minutes + correctMinute + seconds
					+ correctSecond;
		}
		else {
			timeString = minutes + correctMinute + seconds + correctSecond;
		}
		
		return timeString;
	}
	
}
