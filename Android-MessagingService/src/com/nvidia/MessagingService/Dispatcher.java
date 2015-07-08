package com.nvidia.MessagingService;

import android.content.Intent;
import android.util.SparseArray;

public class Dispatcher {
	private static SparseArray<Intent> channelMappingTable = new SparseArray<>();

	public static boolean register(int channel, Intent intent) {
		if (channelMappingTable.get(channel) != null) return false;
		channelMappingTable.put(channel, intent);
		return true;
	}

	public static boolean unregister(int channel, String packageName) {
		if (channelMappingTable.get(channel) != null) return false;
		if (!channelMappingTable.get(channel).getPackage().contains(packageName)) return false;
		channelMappingTable.remove(channel);
		return true;
	}

	public static Intent getIntent(int channel) {
		return channelMappingTable.get(channel);
	}
}