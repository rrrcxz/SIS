import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class CreatePatient {
	private static Socket universal;
	private static int port = 53217;
	// message writer
	private static MsgEncoder encoder;
	// message reader
	private static MsgDecoder decoder;
	// scope of this component
	private static final String SCOPE = "SIS.Scope1";
	// name of this component
	private static final String NAME = "Patient";
	// messages types that can be handled by this component
	private static final List<String> TYPES = new ArrayList<String>(
			Arrays.asList(new String[] { "Setting", "Confirm", "Alert" }));
	// summary for all incoming / outgoing messages
	private static final String incomingMessages = "more concentration";
	private static final String outgoingMessages = "add to list";
	// shared by all kinds of emergencies that can be generated by this component
	private static KeyValueList alert = new KeyValueList();
	private static int numOfPatient = 0;

	/*
	 * Main program
	 */
	public static void main(String[] args) {
		while (true) {
			try {
				// try to establish a connection to SISServer
				universal = connect();

				// bind the message reader to inputstream of the socket
				decoder = new MsgDecoder(universal.getInputStream());
				// bind the message writer to outputstream of the socket
				encoder = new MsgEncoder(universal.getOutputStream());

				/*
				 * construct a Connect message to establish the connection
				 */
				KeyValueList conn = new KeyValueList();
				conn.putPair("Scope", SCOPE);
				conn.putPair("MessageType", "Connect");
				conn.putPair("IncomingMessages", incomingMessages);
				conn.putPair("OutgoingMessages", outgoingMessages);
				conn.putPair("Role", "Controller");
				conn.putPair("Name", NAME);
				encoder.sendMsg(conn);

				initRecord();

				// KeyValueList for inward messages, see KeyValueList for
				// details
				KeyValueList kvList;

				while (true) {
					// attempt to read and decode a message, see MsgDecoder for
					// details
					kvList = decoder.getMsg();
					// process that message
					ProcessMsg(kvList);
				}

			} catch (Exception e) {
				// if anything goes wrong, try to re-establish the connection
				try {
					// wait for 1 second to retry
					Thread.sleep(1000);
				} catch (InterruptedException e2) {
				}
				System.out.println("Try to reconnect");
				try {
					universal = connect();
				} catch (IOException e1) {
				}
			}
		}
	}

	static Socket connect() throws IOException {
		Socket socket = new Socket("127.0.0.1", port);
		return socket;
	}

	private static void initRecord() {

		alert.putPair("Scope", SCOPE);
		alert.putPair("MessageType", "Alert");
		alert.putPair("Sender", NAME);

		// Receiver may be different for each message, so it doesn't make sense
		// to set here
		// alert.putPair("Receiver", "RECEIVER");
	}

	static void ProcessMsg(KeyValueList kvList) throws IOException {

		String scope = kvList.getValue("Scope");

		String broadcast = kvList.getValue("Broadcast");
		String direction = kvList.getValue("Direction");

		if (broadcast != null && broadcast.equals("True")) {

			if (direction != null && direction.equals("Up")) {
				if (!scope.startsWith(SCOPE)) {
					return;
				}
			} else if (direction != null && direction.equals("Down")) {
				if (!SCOPE.startsWith(scope)) {
					return;
				}
			}
		} else {
			if (!SCOPE.equals(scope)) {
				return;
			}
		}
		String messageType = kvList.getValue("MessageType");
		if (!TYPES.contains(messageType)) {
			return;
		}
		String sender = kvList.getValue("Sender");

		String receiver = kvList.getValue("Receiver");

		String purpose = kvList.getValue("Purpose");
		String request = kvList.getValue("request");
		switch (messageType) {
		// System.out.println("here");
		case "Alert":
			switch (sender) {
			case "Ambulance":
				switch (receiver) {
				case "Patient":
					System.out.println("patient received, start processing...");
					String alertMsgAboutBP = "find the patient";
					System.out.println("========= Send out Alert message =========");
					alert.putPair("Note", alertMsgAboutBP);
					alert.putPair("Date", System.currentTimeMillis() + "");
					numOfPatient += 1;
					alert.putPair("Patient", numOfPatient + "");
					encoder.sendMsg(alert);

				}
				break;
			}
			break;
		case "Confirm":
			System.out.println("Connect to SISServer successful.");
			break;
		case "Setting":
			if (receiver.equals(NAME)) {
				switch (purpose) {

				case "Kill":
					System.exit(0);
					break;
				}
			}
			break;
		}
	}
}