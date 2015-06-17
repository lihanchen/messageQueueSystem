package com.nvidia.MessgingService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.File;
import java.io.FileInputStream;

public class MyActivity extends Activity {
	/**
	 * Called when the activity is first created.
	 */

	public final static int MAX_FILE_SIZE = 10 * 1024 * 1024; //10MB


	public static Activity activity;

	Messenger messenger=null;
	ServiceConnection connection=new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			messenger=new Messenger(service);
		}
		public void onServiceDisconnected(ComponentName name) {
			messenger = null;
		}
	};
	AlertDialog.Builder alertBuilder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		activity = this;
		EditText editTextID = (EditText) findViewById(R.id.editTextID);
		EditText editTextIP = (EditText) findViewById(R.id.editTextIP);
		editTextID.setText(Integer.toString((int) (Math.random() * 100)));
		editTextIP.setText("dhcp-172-17-185-197.nvidia.com");
		alertBuilder=new AlertDialog.Builder(MyActivity.this).setPositiveButton("OK", null).setTitle("Message");

		Intent intent = new Intent(MyActivity.this, MessagingServices.class);
		intent.putExtra("ID", editTextID.getText().toString());
		intent.putExtra("IP", editTextIP.getText().toString());
		startService(intent);
		bindService(new Intent(MyActivity.this, MessagingServices.class), connection, Context.BIND_AUTO_CREATE);

		(findViewById(R.id.buttonStartServer)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MyActivity.this, MessagingServices.class);
				intent.putExtra("ID", editTextID.getText().toString());
				intent.putExtra("IP", editTextIP.getText().toString());
				startService(intent);
			}
		});

		(findViewById(R.id.buttonStopServer)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (messenger == null) return;
				Intent intent = new Intent(MyActivity.this, MessagingServices.class);
				intent.putExtra("ID", editTextID.getText().toString());
				intent.putExtra("IP", editTextIP.getText().toString());
				MyActivity.this.unbindService(connection);
				messenger = null;
				stopService(intent);
			}
		});

		(findViewById(R.id.buttonSend)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (messenger==null){
					showMessageBox("Failed communicating with service");
					return;
				}

				((EditText) findViewById(R.id.editTextMsg)).selectAll();

				String message=((EditText)findViewById(R.id.editTextMsg)).getText().toString();
				String target=((EditText)findViewById(R.id.editTextTarget)).getText().toString();

				com.nvidia.MessgingService.Message msg
						=new com.nvidia.MessgingService.Message(editTextID.getText().toString(), target, message);
				try {
					messenger.send(Message.obtain(null, MessagingServices.messageWhat.Send.ordinal(), msg));
				}catch(Exception e){
					Log.e("ERROR","ERROR",e);
				}
			}
		});

		(findViewById(R.id.buttonSendGroup)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (messenger == null) {
					showMessageBox("Failed communicating with service");
					return;
				}

				((EditText) findViewById(R.id.editTextGroupMsg)).selectAll();
				String message = ((EditText) findViewById(R.id.editTextGroupMsg)).getText().toString();

				com.nvidia.MessgingService.Message msg
						=new com.nvidia.MessgingService.Message(editTextID.getText().toString(), null, message);
				try {
					messenger.send(Message.obtain(null, MessagingServices.messageWhat.Send.ordinal(), msg));
				}catch(Exception e){
					Log.e("ERROR","ERROR",e);
				}
			}
		});

		(findViewById(R.id.buttonBroadcastPic)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				new Thread() {
					public void run() {
						try {
							File file = new File("/sdcard/vim.png");
							if (file.length() > MAX_FILE_SIZE) {
								showMessageBox("file too large");
								return;
							}

							FileInputStream fis = new FileInputStream(file);
							byte buffer[] = new byte[(int) file.length()];
							if (fis.read(buffer) == -1) {
								showMessageBox("Error reading the file");
								return;
							}
							fis.close();

							com.nvidia.MessgingService.Message msg
									= new com.nvidia.MessgingService.Message(editTextID.getText().toString(), null, buffer, com.nvidia.MessgingService.Message.Type.binary);

							messenger.send(Message.obtain(null, MessagingServices.messageWhat.Send.ordinal(), msg));

						} catch (Exception e) {
							Log.e("ERROR", "ERROR", e);
						}
					}
				}.start();
			}
		});


	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(connection);
	}

	public void showMessageBox(String msg){
		alertBuilder.setMessage(msg).create().show();
	}

}