package CGL;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.Timer;

interface ServerMessageReceiver{
	public void processMessage(String msg, boolean success);
}

public class NetworkComm extends Thread{
	SocketToServer connect;
	
	Timer challengeCheckTimer;
	int challengeCheckRate;
	ServerMessageReceiver challengeReceiver;
	
	String challengedUser; //if a challenge has been made, this contains the name of the user who is challenged
	int challengeTimeout; //number of checks (ie. after challengeTimeout*challengeCheckRate milliseconds)
	int challengeTimeoutCounter;
	
	public NetworkComm() throws IOException{
		connect = new SocketToServer();
		connect.initiateConnection();
		challengeReceiver = null;
	}
	
	public void setChallengeReceiver(ServerMessageReceiver aChallengeReceiver){
		challengeReceiver = aChallengeReceiver;
		challengeCheckRate = 5*1000; //every 5 seconds
		challengeTimeout = 3; //ie. 15 seconds
		challengeCheckTimer = new Timer(challengeCheckRate, new ChallengeChecker());
		challengeCheckTimer.start();
	}
	
	
	class ChallengeChecker implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				challengeTimeoutCounter++;
				if(challengeTimeoutCounter > challengeTimeout && challengedUser != null){
					challengedUser = null; //challenge stands cancelled
					String reply = connect.communicate("ChangeOfStateRequest: CHALLENGE");
					if(!reply.equals("ChangeOfStateRequest: Accepted. Proceed.")){
						challengeReceiver.processMessage("Error: Cannot cancel challenge: "+reply, false);
						return;
					}
					reply = connect.communicate("ChallengeRequest: TerminateChallenge");
					if(!reply.equals("ChallengeRequest: Successful. Challenge removed.")){
						challengeReceiver.processMessage("Error: Cannot cancel challenge: "+reply, false);
						return;
					}
					challengeReceiver.processMessage("Challenge Timeout: Challenged user taking too long to respond. Challenge cancelled.", false);
				}
				
				String reply = connect.communicate("ChangeOfStateRequest: CHALLENGE");
				
				if(!reply.equals("ChangeOfStateRequest: Accepted. Proceed.")){
					challengeReceiver.processMessage("Error: "+reply, false);
					return;
				}
				reply = connect.communicate("ChallengeRequest: Check for challenges.");
				if(!reply.equals("ChallengeRequest: No challenges yet.") && !reply.equals("ChallengeRequest: Error=\"User not authenticated. Please login.\"")){ //a challenge has been made/accepted
					challengeReceiver.processMessage(reply, true);
					if(!reply.substring(0,9).equals("Proposed:")){
						//ie. sent challenge has either been accepted or rejected
						challengedUser=null;
					}
				}
				//System.out.println(reply);
			} catch (IOException e1) {
				challengeReceiver.processMessage("NetworkComm:ChallengeChecker: Could not communicate with server.", false);
			}
		}
	}
	
	public void rawCommunicate(String msg, ServerMessageReceiver receiver){
		String reply;
		try{
			reply = connect.communicate(msg);
		}catch(IOException e){
			receiver.processMessage(e.getMessage(), false);
			return;
		}
		receiver.processMessage(reply, true);
	}
	
	public void authenticate(String username, String password, ServerMessageReceiver receiver){
		String reply;
		try{
			reply = connect.communicate("ChangeOfStateRequest: AUTHENTICATION");
			if(!reply.equals("ChangeOfStateRequest: Accepted. Proceed.")){
				receiver.processMessage("Error: "+reply, false);
				return;
			}
			reply = connect.communicate(username+":"+password);
			if(reply.equals("AuthenticationRequest: Accepted")){
				receiver.processMessage(reply, true); //authentication successful
				return;
			}
			else{
				receiver.processMessage(reply, false);
			}
			
		}catch(IOException e){
			receiver.processMessage("NetworkComm:authenticate: Could not communicate with server.", false);
		}
	}
	
	public void getScoreboard(ServerMessageReceiver receiver){
		try{
			String reply = connect.communicate("ChangeOfStateRequest: SCOREBOARD");
			if(!reply.equals("ChangeOfStateRequest: Accepted. Proceed.")){
				receiver.processMessage("Error: "+reply, false);
				return;
			}
			reply = connect.communicate("GetScoreboard");
			if(reply.equals("ScoreboardRequest: Error=\"User not authenticated. Please login.\"")){
				receiver.processMessage("Error: "+reply, false);
				return;
			}
			receiver.processMessage(reply, true);
			
		}catch(IOException e){
			receiver.processMessage("NetworkComm:getScoreboard: Could not communicate with server.", false);
		}
	}
	
	public void challenge(ServerMessageReceiver receiver, String user, String board){
		try{
			String reply = connect.communicate("ChangeOfStateRequest: CHALLENGE");
			if(!reply.equals("ChangeOfStateRequest: Accepted. Proceed.")){
				receiver.processMessage("Error: "+reply, false);
				return;
			}
			reply = connect.communicate(user+";"+board);
			if(reply.equals("ChallengeRequest: Challenge registered.")){
				challengedUser = user;
				challengeTimeoutCounter = 0; //start counting
				receiver.processMessage(reply, true);
			}
			else{
				receiver.processMessage(reply, false);
			}
			
		}catch(IOException e){
			receiver.processMessage("NetworkComm:challenge: Could not communicate with server.", false);
		}
	}
	
	/***Sends user's response about a challenge***/
	public void challengeResponse(boolean accepted, String challenger, String board, ServerMessageReceiver receiver){
		try{
			String reply = connect.communicate("ChangeOfStateRequest: CHALLENGE");
			if(!reply.equals("ChangeOfStateRequest: Accepted. Proceed.")){
				receiver.processMessage("Error: "+reply, false);
				return;
			}
			if(accepted)
				reply = connect.communicate("ChallengeRequest: AcceptChallenge : "+challenger+":"+board);
			else
				reply = connect.communicate("ChallengeRequest: RejectChallenge : "+challenger);
			if(reply.equals("ChallengeRequest: Succesful.")){
				receiver.processMessage("Challenge Accepted", true);
			}
			else{
				receiver.processMessage(reply, false);
			}
			
		}catch(IOException e){
			receiver.processMessage("NetworkComm:challengeResponse: Could not communicate with server.", false);
		}
	}
	
	/***Sends the result of the challenge as computed here***/
	public void reportChallengeResult(int result, ServerMessageReceiver receiver){
		try{
			String reply = connect.communicate("ChangeOfStateRequest: CHALLENGE");
			if(!reply.equals("ChangeOfStateRequest: Accepted. Proceed.")){
				receiver.processMessage("Error: "+reply, false);
				return;
			}
			if(result==1)
				reply = connect.communicate("ChallengeRequest: ReportChallengeResult=\'WON\'");
			else if(result==-1)
				reply = connect.communicate("ChallengeRequest: ReportChallengeResult=\'LOST\'");
			else if(result==0)
				reply = connect.communicate("ChallengeRequest: ReportChallengeResult=\'DRAW\'");
			else{
				receiver.processMessage("Error: Wrong 'result' argument passes to NetworkComm:ChallengeChecker:reportChallengeResult", false);
				return;
			}
			if(!reply.equals("ChallengeRequest: Result reporting successful.")){
				receiver.processMessage("Error: "+reply, false);
				return;
			}
			receiver.processMessage("Result reported successfully", true);			
		}catch(IOException e){
			receiver.processMessage("NetworkComm:challenge: Could not communicate with server.", false);
		}
	}
}

class SocketToServer{
	Socket sock;
	boolean socketOpen;
	PrintWriter outStream;
	BufferedReader inStream;	

	public void initiateConnection() throws IOException{
		try {
			//Map<String, String> env = System.getenv();
			//System.out.print(env.toString());
			String ip = JOptionPane.showInputDialog("Enter server IP address:");
			int port = 9898;//Integer.parseInt(JOptionPane.showInputDialog("Enter server port: "));
            sock = new Socket(ip, port);
            outStream = new PrintWriter(sock.getOutputStream(), true);
            inStream = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			socketOpen = true;
	    } 
		catch (UnknownHostException e) {
	        throw new IOException("Cannot find server.");
	    } 
		catch (IOException e) {
	        throw new IOException("Error in IO connection.");
	    }
	}
	
	public String communicate(String msg) throws IOException{
		sendMessageLine(msg);
		return getMessageLine();
	}

	private void sendMessageLine(String msg) throws IOException{
		if(!socketOpen){
			//try again. If unsuccessful initiateConnection() itself will throw IOException
			initiateConnection();
		}
		outStream.println(msg);
	}
	private String getMessageLine() throws IOException {
		String msg=null;
		try{
			msg = inStream.readLine();
		}catch (IOException e) {
	        System.err.println("Error in IO connection.");
			throw new IOException("Error in IO Connection.");
	    }
		if(msg == null){
			socketOpen = false;
			System.err.println("ClientComm:GetMessageLine :- Connection dropped by server.");
			throw new IOException("ClientComm:GetMessageLine :- Connection dropped by server.");
		}
		return msg;
	}
}
