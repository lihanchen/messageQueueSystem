package com.nvidia.MessagingServiceTest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MyActivity extends Activity {


	public final static int MAX_FILE_SIZE = 10 * 1024 * 1024; //10MB
	public static String action;


	//Register to service
	Messenger messenger = null;
	ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			messenger = new Messenger(service);

			if (action.equals("Register")) {
				try {
					int bindingChannel = 0;
					Intent callbackIntent = new Intent();
					callbackIntent.setClassName("com.nvidia.MessagingServiceTest", "com.nvidia.MessagingServiceTest.MyActivity");
					callbackIntent.setAction("push received");
					callbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					messenger.send(Message.obtain(null, Enum.IPCmessageWhat.Register.ordinal(), bindingChannel, -1, callbackIntent));
					unbindService(connection);
				} catch (Exception e) {
					Log.e("ERROR", "ERROR", e);
				}
			}

			if (action.equals("Reply")) {
				try {
					int bindingChannel = 0;
					Bundle bundle = new Bundle();
					bundle.putString("destination", "Computer");
					bundle.putString("content", "Copy that!");
					bundle.putInt("channel", bindingChannel);
					messenger.send(Message.obtain(null, Enum.IPCmessageWhat.SendText.ordinal(), -1, -1, bundle));
					unbindService(connection);
				} catch (Exception e) {
					Log.e("ERROR", "ERROR", e);
				}
			}
		}

		public void onServiceDisconnected(ComponentName name) {
			messenger = null;
		}
	};
	AlertDialog.Builder alertBuilder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final SharedPreferences sp = getSharedPreferences("com.nvidia.MessagingServiceTest.sp", MODE_PRIVATE);
		Intent intent = getIntent();


		//Pushing service callback processor
		if (intent.getAction().equals("push received")) {
			NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
			nBuilder.setContentTitle("NVIDIA Messaging Service");
			nBuilder.setSmallIcon(R.mipmap.notification_icon);
			nBuilder.setVibrate(new long[]{500, 100});
			nBuilder.setContentText("Received Message From " + intent.getStringExtra("source") + "\n" + intent.getSerializableExtra("content"));
			nBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Received Message From " + intent.getStringExtra("source") + "\n" + intent.getSerializableExtra("content")));
			nBuilder.setTicker((String) intent.getSerializableExtra("content"));
			int notificationID = sp.getInt("NotificationID", 0);
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(notificationID++, nBuilder.build());
			sp.edit().putInt("NotificationID", notificationID).apply();

			//reply
			Intent replyIntent = new Intent();
			replyIntent.setClassName("com.nvidia.MessagingService", "com.nvidia.MessagingService.MessagingService");
			action = "Reply";
			startService(replyIntent);
			bindService(replyIntent, connection, Context.BIND_AUTO_CREATE);

			this.finish();
			return;
		}


		setContentView(R.layout.main);
		EditText editTextID = (EditText) findViewById(R.id.editTextID);
		EditText editTextIP = (EditText) findViewById(R.id.editTextIP);

		editTextID.setText(sp.getString("ID", "LHC"));
		editTextIP.setText(sp.getString("IP", "192.168.1.100"));
		alertBuilder = new AlertDialog.Builder(MyActivity.this).setPositiveButton("OK", null).setTitle("Message");

		//Register to service
		intent = new Intent();
		intent.setClassName("com.nvidia.MessagingService", "com.nvidia.MessagingService.MessagingService");
		action = "Register";
		startService(intent);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);


		(findViewById(R.id.buttonStartServer)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setComponent(new ComponentName("com.nvidia.MessagingService", "com.nvidia.MessagingService.MessagingService"));
				startService(intent);
				sp.edit().putString("IP", ((EditText) findViewById(R.id.editTextIP)).getText().toString()).putString("ID", ((EditText) findViewById(R.id.editTextID)).getText().toString()).apply();
				Bundle bundle = new Bundle();
				bundle.putString("IP", ((EditText) findViewById(R.id.editTextIP)).getText().toString());
				bundle.putString("ID", ((EditText) findViewById(R.id.editTextID)).getText().toString());
				try {
					messenger.send(Message.obtain(null, Enum.IPCmessageWhat.ChangeConnectionPreferences.ordinal(), bundle));
				} catch (Exception e) {
					Log.e("ERROR", "ERROR", e);
				}
			}
		});

		(findViewById(R.id.buttonSend)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (messenger == null) {
					showMessageBox("Failed communicating with service");
					return;
				}

				((EditText) findViewById(R.id.editTextMsg)).selectAll();

				String message = ((EditText) findViewById(R.id.editTextMsg)).getText().toString();
				String target = ((EditText) findViewById(R.id.editTextTarget)).getText().toString();
				Integer channel = 0;

				Bundle bundle = new Bundle();
				bundle.putString("destination", target);
				bundle.putString("content", message);
				bundle.putInt("channel", channel);
				try {
					messenger.send(Message.obtain(null, Enum.IPCmessageWhat.SendText.ordinal(), bundle));
				} catch (Exception e) {
					Log.e("ERROR", "ERROR", e);
				}
			}
		});

		(findViewById(R.id.buttonSendGroup)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (messenger == null) {
					showMessageBox("Failed communicating with service");
					return;
				}

				((EditText) findViewById(R.id.editTextMsg)).selectAll();

				String message = ((EditText) findViewById(R.id.editTextMsg)).getText().toString();
				Integer channel = 4;

				Bundle bundle = new Bundle();
				bundle.putString("destination", null);
				bundle.putString("content", message);
				bundle.putInt("channel", channel);
				try {
					messenger.send(Message.obtain(null, Enum.IPCmessageWhat.SendText.ordinal(), bundle));
				} catch (Exception e) {
					Log.e("ERROR", "ERROR", e);
				}
			}
		});

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (getIntent().getAction().equals(Intent.ACTION_MAIN)) {
			try {
				Intent callbackIntent = new Intent();
				callbackIntent.setClassName("com.nvidia.MessagingServiceTest", "com.nvidia.MessagingServiceTest.MyActivity");
				callbackIntent.setAction(Intent.ACTION_VIEW);
				callbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				messenger.send(Message.obtain(null, Enum.IPCmessageWhat.Unregister.ordinal(), 0, -1, callbackIntent));
			} catch (Exception e) {
				Log.e("ERROR", "ERROR", e);
			}
		}
	}

	public void showMessageBox(String msg) {
		alertBuilder.setMessage(msg).create().show();
	}

}