package com.ADKBareMinimumAndroid;

/**
 * https://github.com/Epsiloni/ADKBareMinimum
 * Based on Simon Monk code.
 * Rewritten by Assaf Gamliel (goo.gl/E2MhJ) (assafgamliel.com).
 * Feel free to contact me with any question, I hope I can help.
 * This code should give you a good jump start with your Android and Arduino project.
 * --
 * This is the minimum you need to communicate between your Android device and your Arduino device.
 * If needed I'll upload the example I made (Sonar distance measureing device).
 */

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.Distance.Test.R;
import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;


public class MainActivity extends Activity implements Runnable {

	private static final int ACCESSORY_TO_RETURN = 0;
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	private static final String TAG = "MainActivity";
	
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		setupAccessory();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
			return mAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}

	@Override
	public void onResume() {
		Log.d(TAG, "Resuming");
		super.onResume();

		if (mInputStream != null && mOutputStream != null) {
			Log.d(TAG, "Resuming: streams were not null");
			return;
		}
		Log.d(TAG, "Resuming: streams were null");
		
		// Looking for more than 1 connected accessory, if found will return the one defined in the constant.
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[ACCESSORY_TO_RETURN]);
		
		// No accessory is connected to the device.
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory, mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "onResume: mAccessory is null");
		}
	}

	@Override
	public void onPause() {
		Log.d(TAG, "Application being paused");
		super.onPause();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Application being destroyed");
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	// Handle the data from the ADK to the Android.
	Handler mHandler = new Handler() {
		private Data t;

		@Override
		public void handleMessage(Message msg) {
			t = (Data) msg.obj;
			
			// Do whatever with the data received from the ADK here.
		}
	};

	// Setting the accessory connection with the device.
	private void setupAccessory() {
		Log.d(TAG, "In setupAccessory");
		
		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent =PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		
		if (getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}
	}

	// Open read and write to and from the Arduino device.
	private void openAccessory(UsbAccessory accessory) {
		Log.d(TAG, "In openAccessory");
		
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, TAG);
			thread.start();
			Log.d(TAG, "Attached");
		} else {
			Log.d(TAG, "openAccessory: accessory open failed");
		}
	}

	// Closing the read and write to and from the Arduino.
	private void closeAccessory() {
		Log.d(TAG, "In closeAccessory");
		
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	// See Google's Demokit.
	private int composeInt(byte hi, byte lo) {
		int val = (int) hi & 0xff;
		val *= 256;
		val += (int) lo & 0xff;
		return val;
	}

	// The running thread.
	// It takes care of the communication between the Android and the Arduino.
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int index;

		// Keeps reading messages forever.
		// There are probably a lot of messages in the buffer, each message 4 bytes.
		while (true) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			index = 0;
			while (index < ret) {
				int len = ret - index;
				
				if (len >= 2) {
					Message m = Message.obtain(mHandler);
					int value = composeInt(buffer[index], buffer[index + 1]);
					m.obj = new Data('a', value);
					mHandler.sendMessage(m);
				}
				index += 2;
			}

		}
	}

	/**
	 * Sending data to the Arduino device.
	 * @param value
	 */
	public void sendCommand(byte value) {
		
		byte[] buffer = new byte[1];
		buffer[0] = (byte) value;
		
		if (mOutputStream != null) {
			try {
				Log.d(TAG, "sendCommand: Sending data to Arduino device: " + buffer);
				mOutputStream.write(buffer);
				
			} catch (IOException e) {
				Log.d(TAG, "sendCommand: Send failed: " + e.getMessage());
			}
		}
		else
		{
			Log.d(TAG, "sendCommand: Send failed: mOutStream was null");
		}
	}

	// Obtaining permission to communicate with a device.
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "USB permission denied");
					}
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					Log.d(TAG, "Detached");
					closeAccessory();
				}
			}
		}
	};
}