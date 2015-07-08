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
	/**
	 * Called when the activity is first created.
	 */

	public final static int MAX_FILE_SIZE = 10 * 1024 * 1024; //10MB
	int notificationID = 0;

	Messenger messenger = null;
	ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			messenger = new Messenger(service);
		}

		public void onServiceDisconnected(ComponentName name) {
			messenger = null;
		}
	};
	AlertDialog.Builder alertBuilder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent.getAction() == Intent.ACTION_VIEW) {

			NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
			nBuilder.setContentTitle("NVIDIA Messaging Service");
			nBuilder.setSmallIcon(R.mipmap.notification_icon);
			nBuilder.setVibrate(new long[]{500, 100});
			nBuilder.setContentText("Received Message From " + intent.getStringExtra("source") + "\n" + (String) intent.getSerializableExtra("content"));
			nBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Received Message From " + intent.getStringExtra("source") + "\n" + (String) intent.getSerializableExtra("content")));
			nBuilder.setTicker((String) intent.getSerializableExtra("content"));
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(notificationID++, nBuilder.build());
			this.finish();
		}

		setContentView(R.layout.main);
		EditText editTextID = (EditText) findViewById(R.id.editTextID);
		EditText editTextIP = (EditText) findViewById(R.id.editTextIP);
		SharedPreferences sp = getSharedPreferences("com.nvidia.MessagingServiceTest.sp", MODE_PRIVATE);
		editTextID.setText(sp.getString("ID", "LHC"));
		editTextIP.setText(sp.getString("IP", "192.168.1.100"));
		alertBuilder = new AlertDialog.Builder(MyActivity.this).setPositiveButton("OK", null).setTitle("Message");
		intent = new Intent();
		intent.setClassName("com.nvidia.MessagingService", "com.nvidia.MessagingService.MessagingService");
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

//		(findViewById(R.id.buttonStopServer)).setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if (messenger == null) return;
//				Intent intent = new Intent();
//				intent.setComponent(new ComponentName("com.nvidia.MessagingService", "com.nvidia.MessagingService.MessagingService"));
//				MyActivity.this.unbindService(connection);
//				messenger = null;
//				stopService(intent);
//			}
//		});

		(findViewById(R.id.buttonSend)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (messenger == null) {
					showMessageBox("Failed communicating with service");
					return;
				}

				((EditText) findViewById(R.id.editTextMsg)).selectAll();

				String message = ((EditText) findViewById(R.id.editTextMsg)).getText().toString();
				String target = ((EditText) findViewById(R.id.editTextTarget)).getText().toString();
				Integer channel = 5;

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
//
//		(findViewById(R.id.buttonBroadcastPic)).setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				new Thread() {
//					public void run() {
//						try {
//							File file = new File("/sdcard/vim.png");
//							if (file.length() > MAX_FILE_SIZE) {
//								showMessageBox("file too large");
//								return;
//							}
//
//							FileInputStream fis = new FileInputStream(file);
//							byte buffer[] = new byte[(int) file.length()];
//							if (fis.read(buffer) == -1) {
//								showMessageBox("Error reading the file");
//								return;
//							}
//							fis.close();
//
//							com.nvidia.MessagingService.Message msg
//									= new com.nvidia.MessagingService.Message(editTextID.getText().toString(), null, 0, buffer, com.nvidia.MessagingService.Message.Type.binary);
//
//							messenger.send(Message.obtain(null, MessagingService.messageWhat.SendText.ordinal(), msg));
//
//						} catch (Exception e) {
//							Log.e("ERROR", "ERROR", e);
//						}
//					}
//				}.start();
//			}
//		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(connection);
	}

	public void showMessageBox(String msg) {
		alertBuilder.setMessage(msg).create().show();
	}

}