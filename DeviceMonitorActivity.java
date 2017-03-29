package com.loc.core;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceMonitorActivity extends Activity {
	
	private SignalStrengthListener signalStrengthListener;
	private TextView signalStrengthTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_monitor);
	}
	
	private void monitorDevice() {
		final TextView tvMonitorInfo = (TextView) findViewById(R.id.monitor_info);
		BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
			int scale = -1;
			int level = -1;
			int voltage = -1;
			int temp = -1;
			int chargeType = -1;

			@Override
			public void onReceive(Context context, Intent intent) {

				String strChargeMechanism = "Device not Charging now!";
				level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
				voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
				chargeType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,
						-1);
				boolean usbCharge = chargeType == BatteryManager.BATTERY_PLUGGED_USB;
				boolean acCharge = chargeType == BatteryManager.BATTERY_PLUGGED_AC;
				if (usbCharge)
					strChargeMechanism = "USB Charging";
				if (acCharge)
					strChargeMechanism = "AC Charging";
				tvMonitorInfo.setText("Battery level is " + level + "/" + scale
						+ "\n Temprature is " + temp / 10 + "C \n Voltage is "
						+ voltage / 1000 + " V" + "\n " + strChargeMechanism);
				Log.e("BatteryManager", "level is " + level + "/" + scale
						+ ", temp is " + temp + ", voltage is " + voltage);
			}
		};
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = registerReceiver(batteryReceiver, filter);
		   //setup content stuff
		   signalStrengthTextView = (TextView)findViewById(R.id.signal_info);

		   //start the signal strength listener
		  signalStrengthListener = new SignalStrengthListener();	           
		   ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).listen(signalStrengthListener,SignalStrengthListener.LISTEN_SIGNAL_STRENGTHS);
		 }


		 private class SignalStrengthListener extends PhoneStateListener
		 {
		  @Override
		  public void onSignalStrengthsChanged(android.telephony.SignalStrength signalStrength) {
		    
		     // get the signal strength (a value between 0 and 31)
		     int strengthAmplitude = signalStrength.getGsmSignalStrength();
		    
		     /*
		      	signal strength = 0 or 99 -> no signal
				signal strength >= 12 -> very good signal
				signal strength >= 8 -> good signal
				signal strength >= 5 -> poor signal
				signal strength <5 -> very poor signal
		      */
		    signalStrengthTextView.setText("Signal Strength"+String.valueOf(strengthAmplitude));		
		    super.onSignalStrengthsChanged(signalStrength);
		  }
		 }

	@Override
	protected void onResume() {
		super.onResume();
		monitorDevice();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.device_monitor, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
