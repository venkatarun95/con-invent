import java.util.ArrayList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Iterator;

import javax.swing.Timer;

class SaveStatus implements ActionListener{
	private GlobalData globalData;

	private Timer saveTimer;
	private int saveRate;
	private int backupID;
	
	public SaveStatus(int aSaveRate, GlobalData aGlobalData){
		globalData = aGlobalData;

		saveRate = aSaveRate;
		backupID = 0;
		saveTimer = new Timer(saveRate, this);
		saveTimer.start();
	}

	/***Construct object but does nothing. It still has to be started by 'startSaving'***/
	public SaveStatus(){
		saveTimer = null;
	}

	public void startSaving(int aSaveRate, GlobalData aGlobalData){
		if(saveTimer != null){
			System.err.println("SaveStatus:startSaving :- Already started saving, cannot start saving again.");
			return;
		}

		globalData = aGlobalData;

		saveRate = aSaveRate;
		backupID = 0;
		saveTimer = new Timer(saveRate, this);
		saveTimer.start();
	}

	public ChallengeHandler loadChallengeHandlerFromFile(String fileName, Scoreboard scoreboard, GlobalData globalData){
		ArrayList<Challenge> activeChallenges=new ArrayList<Challenge>();
		ArrayList<Challenge> retiredChallenges=new ArrayList<Challenge>(); //these will be initialized within generateChallengeFromString
		try{
			BufferedReader inp = new BufferedReader(new FileReader(fileName));
			//only retired challenges will be loaded from file. Active challenges are discarded as loading them creates issues as it is possible that they may never retire.
			generateChallengesFromString(inp.readLine(), retiredChallenges); 
			inp.close();
		}catch(IOException e){
			System.err.println("SaveStatus:loadFromFile :- Error while reading from file.");
			e.printStackTrace();
		}

		ChallengeHandler challengeHandler = new ChallengeHandler(activeChallenges, retiredChallenges, globalData);
		
		Iterator iter = retiredChallenges.iterator();
		if(iter == null){
			System.err.println("SaveStatus:loadChallengeHandlerFromFile :- Error: retiredChallenger returned is null");
			return challengeHandler;
		}
		while(iter.hasNext()){
			Challenge next = (Challenge)iter.next();
			next.reportResultToScoreboard(scoreboard);
		}

		return challengeHandler;
	}

	private String generateSaveString(){
		String data = "";//":::ACTIVE CHALLENGES:::";
		/*int noActiveChallenges = globalData.challengeHandler.getNoActiveChallenges();
		for(int i = 0;i < noActiveChallenges;i++){
			data += globalData.challengeHandler.getActiveChallenge(i).convertToString()+"@@@";
		}*/

		data += ":::RETIRED CHALLENGES:::";

		int noRetiredChallenges = globalData.challengeHandler.getNoRetiredChallenges();
		for(int i = 0;i < noRetiredChallenges;i++){
			data += globalData.challengeHandler.getRetiredChallenge(i).convertToString()+"@@@";
		}

		return data;
	}

	/***Parsese the string and stores the result in activeChallenges and retireChallenges
	 *
	 * Note: It only loads retired challenges as loading active challenges creates issues
	 * as it is possible that an active challenge loaded from file may never retire, which
	 * prevents the concerned user from engaging in any other type of challenge***/
	private void generateChallengesFromString(String str, ArrayList<Challenge> retiredChallenges) throws IllegalArgumentException{
		/*if(!str.substring(0,23).equals(":::ACTIVE CHALLENGES:::")){
			System.err.println("SaveStatus:generateServerStatusFromString :- Error: Active challenges not found in string: ");
			throw new IllegalArgumentException("Active challenges not found in string.");
		}

		//activeChallenges and retiredChallenges are allocated by caller (loadChallengeHandlerFromFile)

		//activeChallenges = new ArrayList<Challenge>();
		//retiredChallenges = new ArrayList<Challenge>();
		int start = 23, end = 23;
		while(!(str.charAt(end) == ':' && str.charAt(end+1) == ':' && str.charAt(end+2) == ':') && end < str.length()){
			String challengeStr;
			if(str.charAt(end) == '@'){
				//if(start = end){end++; continue;}
				challengeStr = str.substring(start, end);
				activeChallenges.add(new Challenge(challengeStr, false));
				//System.out.println(challengeStr);
				start = end = end+3;
				continue;
			}
			end++;
		}*/
		//System.out.println("SaveStatus:generateChallengesFromString :- "+str);
		int start = 0, end = 0;
		if(!str.substring(0, 24).equals(":::RETIRED CHALLENGES:::")){
			System.err.println("SaveStatus:generateServerStatusFromString :- Error: Retired challenges not found in string: ");
			throw new IllegalArgumentException("Retired challenges not found in string.: "+str.substring(end, end+24));
		}
		start = end = 24;
		while(end < str.length() && !(str.charAt(end) == ':' && str.charAt(end+1) == ':' && str.charAt(end+2) == ':')){
			String challengeStr;
			if(str.charAt(end) == '@'){
				challengeStr = str.substring(start, end);
				retiredChallenges.add(new Challenge(challengeStr, true));
				start = end = end+3;
				continue;
			}
			end++;
		}
	}

	public void actionPerformed(ActionEvent e){
		try{
			FileWriter outFile = new FileWriter("CGL_CentralServer_Backup_"+backupID);
			outFile.write(generateSaveString());
			System.out.println("Saving status at file ID: "+backupID);
			outFile.close();
			backupID++;
			backupID %= 10;
		}
		catch(IOException e1){
			System.err.println("SaveStatus:actionPerformed :- Error: Could not save status to file");
			e1.printStackTrace();
		}
	}
}
