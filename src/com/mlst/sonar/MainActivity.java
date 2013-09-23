package com.mlst.sonar;

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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;
import com.mlst.helloblinky.R;

public class MainActivity extends Activity implements Runnable {

	private static final String TAG = "MainActivity";
	private static final String ACTION_USB_PERMISSION = "com.mlst.sonar.MainActivity.action.USB_PERMISSION";

	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private UsbAccessory mAccessory;
	private TextView distance;
	private Handler handler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		distance = (TextView) findViewById(R.id.distance);
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				int val = msg.arg1;
				getWindow().getDecorView().setBackgroundColor(Color.rgb(255-val, val, 0));
				distance.setText(String.valueOf(val + "cm"));
				
			}
		};
		
		

		mUsbManager = UsbManager.getInstance(this);

		if (getIntent() != null) {
			Intent intent = getIntent();
			UsbAccessory accessory = UsbManager.getAccessory(intent);
			if (accessory != null) {
				Toast.makeText(this, "acc: " + accessory.getModel(),
						Toast.LENGTH_LONG).show();

				openAccessory(accessory);
			}

		}

		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mUsbReceiver);
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
	private ParcelFileDescriptor fileDescriptor;
	private FileInputStream input;
	private FileOutputStream output;

	protected void openAccessory(UsbAccessory accessory) {
		fileDescriptor = mUsbManager.openAccessory(accessory);
		if (fileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = fileDescriptor.getFileDescriptor();
			input = new FileInputStream(fd);
			output = new FileOutputStream(fd);

			Thread t = new Thread(this, "Distance Monitor");
			t.start();

			Log.d(TAG, "accessory opened");
		} else {
			Log.d(TAG, "accessory open fail");
		}

	}

	protected void closeAccessory() {
		try {
			if (fileDescriptor != null) {
				fileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			fileDescriptor = null;
			mAccessory = null;
		}

	}

	protected void send(final int data) {

		try {
			output.write(data);
		} catch (Exception e) {
			Log.e(TAG, "error in sending data to accessory", e);
		}
	}

	@Override
	public void run() {
		byte[] buffer = new byte[64384];
		int retvalue = 0;

		while (retvalue >= 0) {
			try {
				retvalue = input.read(buffer);
			} catch (IOException e) {
				e.printStackTrace();
				retvalue = -1;
				break;
			}

			Message m = Message.obtain(handler, 0);
			byte value = buffer[0];
			m.arg1 = value & 0xFF;
			handler.sendMessage(m);
		}

	}

}