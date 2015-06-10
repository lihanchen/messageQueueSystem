package com.nvidia.MessgingService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class MyActivity extends Activity {
	/**
	 * Called when the activity is first created.
	 */
	final public String serverURI="amqp://lhc:123@172.17.187.114:5672";
	final public String broadcastExchangeName="broadcast_group";

	enum messageWhat{
		ReceivedP2PMessage, ReceivedBroadCast, sendFailed, FatalErr
	}

	public String currentID;
	public ConnectionFactory factory;
	public Channel sendChannel;
	public static Activity activity;
	Thread broadcastReceiver,queueReceiver;

	public MyHandler handler=new MyHandler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		activity=this;
		currentID=Integer.toString((int)(Math.random()*1000));
		((EditText)findViewById(R.id.editTextID)).setText(currentID);
		new Thread(){
			@Override
			public void run() {
				try {
					factory = new ConnectionFactory();
					factory.setUri(serverURI);
					factory.setConnectionTimeout(2000);
					Connection conn = factory.newConnection();
					sendChannel = conn.createChannel();
				} catch (Exception e) {
					handler.obtainMessage(messageWhat.FatalErr.ordinal()).sendToTarget();
					e.printStackTrace();
				}
			}
		}.start();

		(findViewById(R.id.buttonSend)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String message=((EditText)findViewById(R.id.editTextMsg)).getText().toString();
				String target=((EditText)findViewById(R.id.editTextTarget)).getText().toString();
				new Thread() {
					public void run(){
						try {
							sendChannel.basicPublish("", target, null, message.getBytes());
						} catch (Exception e){
							handler.obtainMessage(messageWhat.sendFailed.ordinal()).sendToTarget();
							e.printStackTrace();
						}
					}
				}.start();
			}
		});

		(findViewById(R.id.buttonSendGroup)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String message=((EditText)findViewById(R.id.editTextGroupMsg)).getText().toString();
				new Thread() {
					public void run(){
						try {
							sendChannel.basicPublish(broadcastExchangeName, "", null, message.getBytes());
						} catch (Exception e){
							handler.obtainMessage(messageWhat.sendFailed.ordinal()).sendToTarget();
							e.printStackTrace();
						}
					}
				}.start();
			}
		});

		(findViewById(R.id.buttonStartService)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startService(new Intent(MyActivity.this, MessagingServices.class));
			}
		});

	}

}

class MyHandler extends Handler {
	@Override
	public void handleMessage(Message msg) {

		if (msg.what==MyActivity.messageWhat.ReceivedBroadCast.ordinal()){
			showMessageBox("Received Broadcast:\n"+msg.obj);
		}

		if (msg.what==MyActivity.messageWhat.ReceivedP2PMessage.ordinal()){
			showMessageBox("Received Message:\n"+msg.obj);
		}

		if (msg.what==MyActivity.messageWhat.FatalErr.ordinal()){
			showMessageBox("Server Error");
		}

		if (msg.what==MyActivity.messageWhat.sendFailed.ordinal()){
			showMessageBox("Send Failed");
		}
	}

	public void showMessageBox(String msg){
		new AlertDialog.Builder(MyActivity.activity)
				.setPositiveButton("OK", null)
				.setMessage(msg)
				.setTitle("Message")
				.create().show();
	}
}