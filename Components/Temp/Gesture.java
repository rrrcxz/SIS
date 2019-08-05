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
import java.util.Timer;
import java.util.TimerTask;

public class Gesture {

	// socket for connection to SISServer
	static Socket universal;
	private static int port = 53217;
	// message writer
	static MsgEncoder encoder;
	// message reader
	static MsgDecoder decoder;
	// scope of this component
	private static final String SCOPE = "SIS.Scope1";
	// name of this component
	private static final String NAME = "Gesture";
	// messages types that can be handled by this component
	private static final List<String> TYPES = new ArrayList<String>(
			Arrays.asList(new String[] { "Setting", "Confirm" }));

	private static int refreshRate = 500, max = 40, min = 15;
	private static Date startDate = new Date(), endDate = new Date();

	private static Timer timer = new Timer();
	// shared by all kinds of records that can be generated by this component
    private static KeyValueList record = new KeyValueList();
    // shared by all kinds of alerts that can be generated by this component
    private static KeyValueList alert = new KeyValueList();

    private static GestureReading reading = new GestureReading();

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) {
    	while (true)
        {
            try
            {
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
				conn.putPair("Role", "Basic");
                conn.putPair("Name", NAME);
                encoder.sendMsg(conn);

                initRecord();

                // KeyValueList for inward messages, see KeyValueList for
                // details
                KeyValueList kvList;

                while (true)
                {
                    // attempt to read and decode a message, see MsgDecoder for
                    // details
                    kvList = decoder.getMsg();

                    // process that message
                    ProcessMsg(kvList);
                }

            }
            catch (Exception e)
            {
                // if anything goes wrong, try to re-establish the connection
                e.printStackTrace();
                try
                {
                    // wait for 1 second to retry
                    Thread.sleep(1000);
                }
                catch (InterruptedException e2)
                {
                }
                System.out.println("Try to reconnect");
                try
                {
                    universal = connect();
                }
                catch (IOException e1)
                {
                }
            }
        }
    }
    /*
     * used for connect(reconnect) to SISServer
     */
    static Socket connect() throws IOException
    {
        Socket socket = new Socket("127.0.0.1", port);
        return socket;
    }
    
    private static void initRecord()
    {
        record.putPair("Scope", SCOPE);
        record.putPair("MessageType", "Reading");
        record.putPair("Sender", NAME);

        // Receiver may be different for each message, so it doesn't make sense
        // to set here
        // record.putPair("Receiver", "");

        alert.putPair("Scope", SCOPE);
        alert.putPair("MessageType", "Alert");
        alert.putPair("Sender", NAME);
        alert.putPair("Purpose", "TempAlert");

        // Receiver may be different for each message, so it doesn't make sense
        // to set here
        // alert.putPair("Receiver", "");
    }
    
    private static void componentTask()
    {
        try
        {
            //collect();
			reading.gesture = "I need help";
			if (reading.gesture != "I need help") {//normal
				//reading
				reading.date = System.currentTimeMillis();
				record.putPair("Gesture", reading.temp + "");
				record.putPair("Date", reading.date + "");
				//send reading message to GUI, uploader
				encoder.sendMsg(record);
				//send reading message to controller
				record.putPair("Receiver", "DiController");
				encoder.sendMsg(record);
				record.removePair("Receiver");
			} else { //alert
				reading.date = System.currentTimeMillis(); //[Duncan Add Nov 16]
				alert.putPair("Gesture", reading.temp + "");
				alert.putPair("Date", reading.date + "");
				//send alert message to GUI, uploader
				alert.removePair("Receiver");//[Duncan Add Nov 16]
				alert.putPair("Receiver", "DiUploader");//[Duncan Add Nov 16]
                		encoder.sendMsg(alert);
				//send alert message to controller
				alert.removePair("Receiver");
				alert.putPair("Receiver", "DiController");
                encoder.sendMsg(alert);
			}
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private static void ProcessMsg(KeyValueList kvList) throws Exception
    {

        String scope = kvList.getValue("Scope");
        if (!SCOPE.startsWith(scope))
        {
            return;
        }

        String messageType = kvList.getValue("MessageType");
        if (!TYPES.contains(messageType))
        {
            return;
        }

        String sender = kvList.getValue("Sender");

        String receiver = kvList.getValue("Receiver");

        String purpose = kvList.getValue("Purpose");

        switch (messageType)
        {
        case "Confirm":
            System.out.println("Connect to SISServer successful.");
            break;
        case "Setting":
            if (receiver.equals(NAME))
            {
                System.out.println("Message from " + sender);
                System.out.println("Message type: " + messageType);
                System.out.println("Message Purpose: " + purpose);
                switch (purpose)
                {

                case "Activate":
                    String rRate = kvList.getValue("RefreshRate");
                    String sDate = kvList.getValue("StartDate");
                    String eDate = kvList.getValue("EndDate");
                    //String maxx = kvList.getValue("Max");
                    //String minn = kvList.getValue("Min");

                    if (rRate != null && !rRate.equals(""))
                    {

                        refreshRate = Integer.parseInt(rRate);

                    }

                    if (sDate != null && !sDate.equals("") && eDate != null
                            && !eDate.equals(""))
                    {
                        startDate.setTime(Long.parseLong(sDate));
                        endDate.setTime(Long.parseLong(eDate));
                    }

//                    if (maxx != null && !maxx.equals("") && minn != null
//                            && !minn.equals(""))
//                    {
//                        max = Integer.parseInt(maxx);
//                        min = Integer.parseInt(minn);
//                    }

                    try
                    {
                        timer.cancel();
                        timer = new Timer();
                    }
                    catch (Exception e)
                    {
                        // TODO: handle exception
                    }
                    timer.schedule(new TimerTask()
                    {

                        @Override
                        public void run()
                        {
                            // TODO Auto-generated method stub
                            if (System.currentTimeMillis() - endDate.getTime() > 0)
                            {
                                cancel();
                            }
                            else
                            {
                                componentTask();
                            }
                        }
                    }, startDate, refreshRate);
                    System.out.println("Algorithm Activated");
                    break;

                case "Kill":
                    try
                    {
                        timer.cancel();
                    }
                    catch (Exception e)
                    {
                        // TODO: handle exception
                    }
                    System.exit(0);
                    break;

                case "Deactivate":
                    try
                    {
                        timer.cancel();
                    }
                    catch (Exception e)
                    {
                        // TODO: handle exception
                    }
                    System.out.println("Algorithm Deactivated");
                    break;
                }
            }
            break;
        }
    }
    
    private static void collect() throws Exception
    {
        Runtime rt = Runtime.getRuntime();
        String comm = "tempered.exe";
        Process proc = rt.exec(comm);

        BufferedReader std = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        //BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        String s = null;
        //int count = 0;
        while((s = std.readLine()) != null)
        {
            try
            {
				//System.out.println(s);
                reading.gesture = s;
				//System.out.println(reading.temp);
                //++count;
            }
            catch(Exception e) {}
        }
        //reading.temp /= count;
		System.out.println(reading.gesture);
    }
}

class GestureReading
{
    String gesture;
    long date;
}