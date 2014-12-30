import java.net.*;
import java.io.*;
import java.util.ArrayList;

/***Class with all the global data that needs to be shared***/
class GlobalData{
	public Authentication auth;
	public Scoreboard scoreboard;
	public ChallengeHandler challengeHandler;

	/***Construct a fresh object with no previous data***/
	public GlobalData(){
		auth = new Authentication();
		scoreboard = new Scoreboard(auth);
		challengeHandler = new ChallengeHandler(this);
	}

	/***Construct an object with challenges loaded from file***/
	public GlobalData(String fileName, SaveStatus saveStatus, int saveRate){
		saveStatus.startSaving(saveRate, this);
		auth = new Authentication();
		scoreboard = new Scoreboard(auth);
		challengeHandler = saveStatus.loadChallengeHandlerFromFile(fileName, scoreboard, this);
	}
}

public class CentralServer{
	static ServerSocket serverSock;
	static int port = 9898; //some random number
	
	static GlobalData globalData;
	
	static ArrayList clientServers;
	
	static GarbageCollector garbageCollector;

	static SaveStatus saveStatus;

	public static void main(String[] args){
		final int saveRate = 30000;
		//saveStatus has creates a complete hotch-spotch of function calls all over the place. Tread carefully
		if(args.length == 1){
			SaveStatus saveStatus = new SaveStatus();
			globalData = new GlobalData(args[0], saveStatus, saveRate);
		}
		else{
			globalData = new GlobalData();
			saveStatus = new SaveStatus(saveRate, globalData);
		}
		clientServers = new ArrayList();
		garbageCollector = new GarbageCollector(globalData, 15, 2); //ie. check every 15 seconds, timeout challenge if state does not change after two such checks
		
		
		//THIS DOES NOT WORK FOR NOW

		System.out.println("CentralServer:main :- Starting server on port "+port);
		try{
			serverSock = new ServerSocket(port);
		}
		catch(Exception e){
			System.out.println("CentralServer:main :- Error. Could not open ServerSocket");
			e.printStackTrace();
		}
		try{
			System.out.println("Listening...");
			while(true){
				Socket sock = serverSock.accept();
				ClientServer cServe = new ClientServer(sock, globalData);
				clientServers.add(cServe);
				cServe.start();
				System.out.println("CentralServer:main :- Accepted connection from client.");
			}
		}
		catch(IOException e){
			System.out.println("CentralServer:main :- Error in accepting connections.");
			e.printStackTrace();	
		}
	}
	
	public static ArrayList getClientServerList(){
		return clientServers;
	}
}
