package com.nvidia.RabbitMQ;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.lang.ref.WeakReference;
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

	public MyHandler handler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		currentID=Integer.toString((int)(Math.random()*1000));
		((EditText)findViewById(R.id.editTextID)).setText(currentID);
		handler=new MyHandler(this);
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


//		(findViewById(R.id.buttonSetID)).setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				showMessageBox("shit");
//			}
//		});

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

		Thread queueReceiver=new Thread(){
			@Override
			public void run() {
				try {
					factory = new ConnectionFactory();
					factory.setUri(serverURI);
					factory.setConnectionTimeout(2000);
					Channel channel;
					Connection conn = factory.newConnection();
					channel = conn.createChannel();
					channel.queueDeclare(currentID, false, false, false, null);
					QueueingConsumer consumer = new QueueingConsumer(channel);
					channel.basicConsume(currentID, true, consumer);
					while (true){
						try {
							String msg=new String(consumer.nextDelivery().getBody());
							Log.e("rabbitMQ"," [x] Received '" + msg + "'");
							handler.obtainMessage(messageWhat.ReceivedP2PMessage.ordinal(),msg).sendToTarget();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					handler.obtainMessage(messageWhat.FatalErr.ordinal()).sendToTarget();
					e.printStackTrace();
				}
			}
		};
		queueReceiver.start();

		Thread broadcastReceiver=new Thread(){
			@Override
			public void run() {
				try {
					factory = new ConnectionFactory();
					factory.setUri(serverURI);
					factory.setConnectionTimeout(2000);
					Channel channel;
					Connection conn = factory.newConnection();
					channel = conn.createChannel();
					channel.exchangeDeclare(broadcastExchangeName, "fanout");
					String queueName = channel.queueDeclare().getQueue();
					channel.queueBind(queueName, broadcastExchangeName, "");
					channel.queueDeclare(currentID, false, false, false, null);

					QueueingConsumer consumer = new QueueingConsumer(channel);
					channel.basicConsume(queueName, true, consumer);
					while (true){
						try {
							String msg=new String(consumer.nextDelivery().getBody());
							Log.e("rabbitMQ", " [x] Received Broadcast'" + msg + "'");
							handler.obtainMessage(messageWhat.ReceivedBroadCast.ordinal(),msg).sendToTarget();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					handler.obtainMessage(messageWhat.FatalErr.ordinal()).sendToTarget();
					e.printStackTrace();
				}
			}
		};
		broadcastReceiver.start();

	}
}

class MyHandler extends Handler {
	Activity activity;
	AlertDialog.Builder builder;

	public MyHandler(Activity activity) {
		this.activity=activity;
		builder = new AlertDialog.Builder(activity);
		builder.setPositiveButton("OK",null);
	}

	@Override
	public void handleMessage(Message msg) {
		if (msg.what==MyActivity.messageWhat.FatalErr.ordinal()){
			showMessageBox("Server Error");
		}

		if (msg.what==MyActivity.messageWhat.ReceivedBroadCast.ordinal()){
			showMessageBox("Received Broadcast:\n"+msg.obj);
		}

		if (msg.what==MyActivity.messageWhat.ReceivedP2PMessage.ordinal()){
			showMessageBox("Received Message:\n"+msg.obj);
		}

		if (msg.what==MyActivity.messageWhat.sendFailed.ordinal()){
			showMessageBox("Received Message:\n"+msg.obj);
		}
	}

	public void showMessageBox(String msg){
		builder.setMessage(msg).setTitle("Message");
		builder.create().show();
	}
}