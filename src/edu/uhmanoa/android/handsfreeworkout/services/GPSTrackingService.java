package edu.uhmanoa.android.handsfreeworkout.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class GPSTrackingService extends Service {
	/**Name of extra to pass to HFS*/
	public static final String GPS_STATE = "gps state";
	BroadcastReceiver mReceiver;
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		IntentFilter aliveFilter = new IntentFilter(Receiver.HFS_ALIVE);
		aliveFilter.addCategory(Intent.CATEGORY_DEFAULT);
		mReceiver = new Receiver();
		
		this.registerReceiver(mReceiver, aliveFilter);
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.w("GPS", "on destroy of service");
		announceGPSAlive(false);
		this.unregisterReceiver(mReceiver);
	}
	
	public void announceGPSAlive(boolean alive) {
		Log.w("GPS", "announcing GPS alive:  " + alive);
		Intent gps  = new Intent(HandsFreeService.UpdateReceiver.GPS_ENABLED);
		gps.putExtra(GPS_STATE, alive);
		this.sendBroadcast(gps);
	}

	public class Receiver extends BroadcastReceiver{
		public static final String HFS_ALIVE = "hfs alive";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			//for initial create
			if (intent.getAction().equals(HFS_ALIVE)) {
				Log.w("GPSTrackingService", "Receive HFS is alive");
				announceGPSAlive(true);
			}
		}
		
	}
}
