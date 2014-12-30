import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.Timer;

/***Removes challenges that have been inactive for a long time so that fresh 
 * challenges may be made. DOES NOT WORK FOR NOW***/
public class GarbageCollector implements ActionListener{
	Timer collectTimer;
	int checkRate; //intervals at which it checks for 
	int challengeTimeout; //time after which an inactive challenge is retired (unsuccessfully). Measures as a multiple of checkRate
	
	int curTimestamp; //current time measured in number of clock ticks

	GlobalData globalData;
	ChallengeHandler challengeHandler;
	
	public GarbageCollector(GlobalData aGlobalData, int aCheckRate, int aChallengeTimeout){
		globalData = aGlobalData;
		checkRate = aCheckRate;
		challengeTimeout = aChallengeTimeout;
		challengeHandler = globalData.challengeHandler;

		curTimestamp = 0;
		
		collectTimer = new Timer(checkRate, this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e){
		int noChallenges = challengeHandler.getNoActiveChallenges();
		for(int i = 0;i < noChallenges;i++){
			Challenge ch = challengeHandler.getActiveChallenge(i);
			assert(ch.isActive());
			int chTimestamp = ch.getTimestamp();
			if(chTimestamp == -1){ //it does not have a timestamp yet
				ch.setTimestamp(curTimestamp);
				continue;
			}
			
			if(ch.getPrevState() == ch.getChallengeState()){
				ch.setTimestamp(++chTimestamp);
				if(chTimestamp > challengeTimeout){ //gotta remove this challenge
					challengeHandler.retireActiveChallengeByIndex(i, false); //false indicates that challenge was not successfully completed
				}
				//do nothing
			}
			else{ //the state has changed. ie. there has been activity. So reset the timestamp
				ch.setTimestamp(0);
			}
		}
		curTimestamp++;
	}
}
