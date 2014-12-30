import java.net.*;
import java.io.*;

enum InteractionState{
		NEUTRAL, AUTHENTICATION, SCOREBOARD, CHALLENGE
}

public class ClientServer extends Thread{
	Socket clientSock;
	GlobalData globalData;
	boolean socketOpen;

	InteractionState interactionState; //the state in which the communication lies
	String username;
	boolean authenticated;

	public ClientServer(Socket sock, GlobalData gData){
		clientSock = sock;
		globalData = gData;
		socketOpen = true;
		interactionState = InteractionState.NEUTRAL;
		username = "Not logged in yet.";
		authenticated = false;
	}
	public void run(){
		try{
			PrintWriter out = new PrintWriter(clientSock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
			String inputLine;
			while((inputLine = in.readLine()) != null && socketOpen){
				//System.out.println("ClientServer:run :- Received string from client: "+inputLine);
				out.println(ProcessRequest(inputLine)); //send response
			}
			globalData.challengeHandler.removeActiveChallengeByChallenger(username, false);
			System.out.println("ClientServer:run :- Logging off and closing connection with client");
			if(authenticated)
				globalData.auth.LogOffUser(username);
			clientSock.close();
		}
		catch(IOException e){
			System.out.println("ClientServer:run :- IOException encountered. Closing connection with client");
		}
	}
	
	public String getUsername(){return username;}

	private String ProcessRequest(String fromClient){
		switch(interactionState){
			case NEUTRAL:
				return ProcessNeutralRequest(fromClient);
			case AUTHENTICATION:
				return ProcessAuthenticationRequest(fromClient);
			case SCOREBOARD:
				return ProcessScoreboardRequest(fromClient);
			case CHALLENGE:
				return ProcessChallengeRequest(fromClient);
			default:
				System.err.println("The interactionState is in an illegal state. Closing thread and communication.");
				socketOpen = false;
				return "UnrecognisedState: Error=\"Server side error. Please report this.\"";
		}
	}

	/***Processes change of state***/
	private String ProcessNeutralRequest(String fromClient){
		assert(interactionState == InteractionState.NEUTRAL);
		if(fromClient.equals("ChangeOfStateRequest: AUTHENTICATION")){
			interactionState = InteractionState.AUTHENTICATION;
			return "ChangeOfStateRequest: Accepted. Proceed.";
		}
		else if(fromClient.equals("ChangeOfStateRequest: SCOREBOARD")){
			interactionState = InteractionState.SCOREBOARD;
			return "ChangeOfStateRequest: Accepted. Proceed.";
		}
		else if(fromClient.equals("ChangeOfStateRequest: CHALLENGE")){
			interactionState = InteractionState.CHALLENGE;
			return "ChangeOfStateRequest: Accepted. Proceed.";
		}
		else{
			return "UnexpectedString: State=NEUTRAL";
		}
	}

	private String ProcessScoreboardRequest(String fromClient){
		assert(interactionState == InteractionState.SCOREBOARD);
		interactionState = InteractionState.NEUTRAL; //change back to normal

		if(!authenticated)
			return "ScoreboardRequest: Error=\"User not authenticated. Please login.\"";
		
		if(fromClient.equals("GetScoreboard")){
			return globalData.scoreboard.stringify();
		}
		else{
			return "ScoreboardRequest: Error=\"Request not understood.\"";
		}
	}
	
	private String ProcessChallengeRequest(String fromClient){
		assert(interactionState == InteractionState.CHALLENGE);
		interactionState = InteractionState.NEUTRAL;
		
		if(!authenticated)
			return "ChallengeRequest: Error=\"User not authenticated. Please login.\"";
		
		if(fromClient.equals("ChallengeRequest: Check for challenges.")){
			Challenge res = globalData.challengeHandler.checkForActiveChallengesByChallenged(username);
			//the user has been challenged
			if(res != null && res.getChallengeState() == Challenge.CHALLENGE_STATUS.CHALLENGE_PROPOSED){
					res.setChallengeState(Challenge.CHALLENGE_STATUS.CHALLENGE_PROCESSING);
					return "Proposed:"+res.convertToString();
			}
			res = globalData.challengeHandler.checkForActiveChallengesByChallenger(username);
			if(res != null){
				if(res.getChallengeState() == Challenge.CHALLENGE_STATUS.CHALLENGE_ACCEPTED){
					res.setChallengeState(Challenge.CHALLENGE_STATUS.CHALLENGE_RUNNING);
					return "Accepted:"+res.convertToString();
				}
				else if(res.getChallengeState() == Challenge.CHALLENGE_STATUS.CHALLENGE_REJECTED){
					//res.setChallengeState(Challenge.CHALLENGE_STATUS.CHALLENGE_RUNNING);
					globalData.challengeHandler.retireActiveChallengeByChallenge(res);
					return "Rejected:"+res.convertToString();
				}
			}
			//Nopes. Its cool now.
			return "ChallengeRequest: No challenges yet.";
			
		}
		else if(fromClient.equals("ChallengeRequest: TerminateChallenge")){
			int statCode = globalData.challengeHandler.removeActiveChallengeByChallenger(username, false);
			switch(statCode){
				case 0:
					return "ChallengeRequest: Successful. Challenge removed.";
				case 1:
					return "ChallengeRequest: Error=\"Challenge once accepted cannot be removed\"";
				case 2:
					return "ChallengeRequest: Error=\"Cannot find challenge to remove\"";
				default:
					System.err.println("ClientServer:ProcessChallengeRequest :- Wrong status code returned by 'removeActiveChallengeByChallenger()'");
					return "ChallengeRequest: Error=\"Server side error. Please report this.\"";
			}
		}
		else if(fromClient.substring(0,36).equals("ChallengeRequest: AcceptChallenge : ")){//This is a compound request with two parts (see next line)
			int statCode = globalData.challengeHandler.acceptChallenge(username, fromClient.substring(36));
			switch(statCode){
				case 0:
					return "ChallengeRequest: Succesful.";
				case 1:
					return "ChallengeRequest: Error=\"No such challenge found to accept.\"";
				case 2:
					return "ChallengeRequest: Error=\"Challenge not in a state to be accepted.\"";
				default:
					System.err.println("ClientServer:ProcessChallengeRequest :- Wrong status code returned by 'acceptChallenge()'");
					return "ChallengeRequest: Error=\"Server side error. Please report this.\"";
			}			
		}
		else if(fromClient.substring(0,36).equals("ChallengeRequest: RejectChallenge : ")){//This is a compound request with two parts (see next line)
			int statCode = globalData.challengeHandler.rejectChallenge(username, fromClient.substring(36));
			switch(statCode){
				case 0:
					return "ChallengeRequest: Succesful.";
				case 1:
					return "ChallengeRequest: Error=\"No such challenge found to reject.\"";
				case 2:
					return "ChallengeRequest: Error=\"Challenge not in a state to be rejected.\"";
				default:
					System.err.println("ClientServer:ProcessChallengeRequest :- Wrong status code returned by 'rejectChallenge()'");
					return "ChallengeRequest: Error=\"Server side error. Please report this.\"";
			}	
		}
		else if(fromClient.substring(0,39).equals("ChallengeRequest: ReportChallengeResult")){
			int result;
			if(fromClient.substring(39).equals("=\'WON\'"))
				result = 1;
			else if(fromClient.substring(39).equals("=\'LOST\'"))
				result = -1;
			else if(fromClient.substring(39).equals("=\'DRAW\'"))
				result = 2;
			else
				return "ChallengeRequest: Error=\"Program error. Unrecognised result of match reported. \"";
			int statCode = globalData.challengeHandler.reportResult(username, result);
			switch(statCode){
				case 0:
					return "ChallengeRequest: Result reporting successful.";
				case 1:
					return "ChallengeRequest: Error=\"No challenge found to report results.\"";
				case 2:
					return "ChallengeRequest: Error=\"The users disagree in results\""; //WARNING: Only one of the users will get this message (the one that sent the results later. Most probably the challenger)
				default:
					System.err.println("ClientServer:ProcessChallengeRequest :- Wrong status code returned by 'reportResult()'");
					return "ChallengeRequest: Error=\"Server side error. Please report this.\"";
			}
		}
		else{ //it is a challenge
			boolean found = false;
			int pos=0;
			for(int i = 0;i < fromClient.length();i++){
				if(fromClient.charAt(i) == ';'){
					pos = i;
					found = true;
					break;
				}
			}
			if(!found)
				return "ChallengeRequest: Error=\"Wrong format for challenge. ';' not found.\"";

			String toChallenge = fromClient.substring(0,pos);
			String board = fromClient.substring(pos+1, fromClient.length());
		
			int status = globalData.challengeHandler.challenge(toChallenge, username, board);
			switch(status){
				case 0:
					return "ChallengeRequest: Challenge registered.";
				case 1:
					return "ChallengeRequest: Error=\"User not found\"";
				case 2:
					return "ChallengeRequest: Error=\"Challenged user has a challenge pending. Please try again later.\"";
				case 3:
					return "ChallengeRequest: Error=\"Cannot challenge self. Please challenge someone else.\"";
				case 4:
					return "ChallengeRequest: Error=\"You have a challenge pending. Please wait before sending out another challenge.\"";
				default:
					System.err.println("ClientServer: ProcessChallengeRequest - Unexpected value from ChallengeHandler.challenge");
					return "ChallengeRequest: Error=\"Server side error. Please report this\"";
			}
		}
		//System.err.println("ClientServer: ProcessChallengeRequest - Control should never reach here.");
		//return "ChallengeRequest: Error=\"Server side error. Please report this\"";
	}

	private String ProcessAuthenticationRequest(String fromClient){
		assert(interactionState == InteractionState.AUTHENTICATION);
		interactionState = InteractionState.NEUTRAL; //change back to normal

		if(authenticated)
			return "AuthenticationRequest: Error=\"Already logged in. Cannot log in again.\"";
		//Expected message format: 'username:password'
		//extract username and password from string

		//find position of ':'
		boolean found = false;
		int pos=0;
		for(int i = 0;i < fromClient.length();i++){
			if(fromClient.charAt(i) == ':'){
				pos = i;
				found = true;
				break;
			}
		}
		if(!found)
			return "AuthenticationRequest: Error=\"wrong format for username/password. ':' not found.\"";

		String user = fromClient.substring(0,pos);
		String password = fromClient.substring(pos+1, fromClient.length());

		interactionState = InteractionState.NEUTRAL; //change back to noral irrespective of result

		int authState = globalData.auth.AuthenticateNewUser(user, password);
		switch(authState){
			case 0:
				return "AuthenticationRequest: Error=\"Incorrect password\"";
			case 1:
				username = user;
				authenticated = true;
				System.out.println("ChallengeHandler:ProcessAuthenticationRequest"+user);
				return "AuthenticationRequest: Accepted";
			case 2:
				return "AuthenticationRequest: Error=\"User not found\"";
			case 3:
				return "AuthenticationRequest: Error=\"User already online\"";
			case 4:
				return "AuthenticationRequest: Error=\"Too many attempts to login\"";
			default:
				return "AuthenticationRequest: Error=\"Server side error. Please report this.\"";
		}
	}
}
