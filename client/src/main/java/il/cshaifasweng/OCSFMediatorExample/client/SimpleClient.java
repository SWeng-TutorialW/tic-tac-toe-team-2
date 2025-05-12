package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import org.greenrobot.eventbus.EventBus;
import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;


public class SimpleClient extends AbstractClient {
	public SimpleClient(String host, int port) {
		super(host, port);
	}

	// Self explainatory
	@Override
	protected void handleMessageFromServer(Object msg) {
		System.out.println("Server sent: " + msg.toString());
		if (msg instanceof Warning) {
			EventBus.getDefault().post(new WarningEvent((Warning) msg));
		} else {
			EventBus.getDefault().post(new WarningEvent(new Warning(msg.toString())));
		}
	}
}

