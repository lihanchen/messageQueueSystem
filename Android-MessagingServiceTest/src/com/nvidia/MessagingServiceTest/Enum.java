package com.nvidia.MessagingServiceTest;

public class Enum {


	public enum MessageType {
		JSON, binary, string
	}

	public enum IPCmessageWhat {
		SendText, ChangeConnectionPreferences, CloseConnection, Success, Failed
	}
}
