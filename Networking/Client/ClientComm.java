import java.net.*;
import java.io.*;

public class ClientComm{
	static Socket sock;
	static int port = 9898;
	static boolean socketOpen;
	
	static PrintWriter outStream;
	static BufferedReader inStream;

	public static void main(String[] args){
		try {
            sock = new Socket("localhost", port);
            outStream = new PrintWriter(sock.getOutputStream(), true);
            inStream = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			socketOpen = true;
			/*String username = stdIn.readLine();
			String password = stdIn.readLine();
			
			SendMessageLine("ChangeOfStateRequest: AUTHENTICATION");
			System.out.println("Message from server: "+GetMessageLine());
			SendMessageLine(username+":"+password);
			System.out.println("Message from server: "+GetMessageLine());*/
            String echo;
            String userInp;

			userInp = stdIn.readLine();
            if (userInp != null) {
                System.out.println("Client: " + userInp);
                SendMessageLine(userInp);
            }

            while (socketOpen) {
				echo = GetMessageLine();
                System.out.println("Server: " + echo);
                
                userInp = stdIn.readLine();
                if (userInp != null) {
                    System.out.println("Client: " + userInp);
                    SendMessageLine(userInp);
                }
	    	}
	    } 
		catch (UnknownHostException e) {
	        System.err.println("Cannot find host.");
	        System.exit(1);
	    } 
		catch (IOException e) {
	        System.err.println("Error in IO connection.");
	        System.exit(1);
	    }
	}

	private static void SendMessageLine(String msg){
		outStream.println(msg);
	}
	private static String GetMessageLine(){
		String msg=null;
		try{
			msg = inStream.readLine();
		}catch (IOException e) {
	        System.err.println("Error in IO connection.");
			e.printStackTrace();
	        System.exit(1);
	    }
		if(msg == null){
			socketOpen = false;
			System.err.println("ClientComm:GetMessageLine :- Connection dropped by server.");
			return null;
		}
		return msg;
	}
}
