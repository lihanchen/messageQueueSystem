package com.nvidia.MessagingService;

import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;

public abstract class Dispatcher {
	private static SparseArray<Intent> channelMappingTable = new SparseArray<>();

	public static boolean register(int channel, Intent intent) {
		if (channelMappingTable.get(channel) != null) {
			channelMappingTable.put(channel, intent);
			return true;
		}
		Log.i("register", "channel" + channel);
		channelMappingTable.put(channel, intent);
		return true;
	}

	public static boolean unregister(int channel, String packageName) {
		if (channelMappingTable.get(channel) == null) return false;
		if (!channelMappingTable.get(channel).getComponent().getPackageName().contains(packageName)) return false;
		channelMappingTable.remove(channel);
		Log.i("unregister", "channel" + channel);
		return true;
	}

	public static Intent getIntent(int channel) {
		return channelMappingTable.get(channel);
	}
}
