package com.nvidia.MessagingService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
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
		SharedPreferences sp = getSharedPreferences("com.nvidia.MessagingService.sp", MODE_PRIVATE);
		editTextID.setText(sp.getString("ID", "LHC"));
		editTextIP.setText(sp.getString("IP", "192.168.1.100"));
		alertBuilder=new AlertDialog.Builder(MyActivity.this).setPositiveButton("OK", null).setTitle("Message");
		Intent intent = new Intent(MyActivity.this, MessagingServices.class);
		startService(intent);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);


		(findViewById(R.id.buttonStartServer)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String[] msg = new String[]{editTextIP.getText().toString(), editTextID.getText().toString()};
				try {
					messenger.send(Message.obtain(null, MessagingServices.messageWhat.changeConnectionPreferences.ordinal(), msg));
				} catch (Exception e) {
					Log.e("ERROR", "ERROR", e);
				}
			}
		});

		(findViewById(R.id.buttonStopServer)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (messenger == null) return;
				Intent intent = new Intent(MyActivity.this, MessagingServices.class);
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

				com.nvidia.MessagingService.Message msg
						= new com.nvidia.MessagingService.Message(editTextID.getText().toString(), target, message);
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

				com.nvidia.MessagingService.Message msg
						= new com.nvidia.MessagingService.Message(editTextID.getText().toString(), null, message);
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

							com.nvidia.MessagingService.Message msg
									= new com.nvidia.MessagingService.Message(editTextID.getText().toString(), null, buffer, com.nvidia.MessagingService.Message.Type.binary);

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