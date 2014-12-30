import java.util.ArrayList;
import java.util.Iterator;

public class Scoreboard{
	double[] scores;
	Authentication auth;
	String[] users;

	public Scoreboard(Authentication aAuth){
		auth = aAuth;
		
		users = auth.GetUserList();
		scores = new double[users.length];
		for(int i = 0;i < scores.length;i++){
			scores[i] = 0;
		}
	}

	/*public synchronized Score[] getScores(){	
		return scores;
	}*/
	
	/***Return a string that can be transmitted***/
	public String stringify(){		
		//now compute string
		String str = "";

		//Bubble sort!
		boolean changedFlag = false;
		for(int i = 0;i < scores.length;i++){
			for(int j = 1;j < scores.length-i;j++){
				if(scores[j-1] < scores[j]){
					double tempD = scores[j-1];
					scores[j-1] = scores[j];
					scores[j] = tempD;

					String tempS = users[j-1];
					users[j-1] = users[j];
					users[j] = tempS;
					changedFlag = true;
				}
			}
			if(!changedFlag)
				break;
			changedFlag = false;
		}

		for(int i = 0;i < scores.length;i++){
			str += users[i]+":"+scores[i]+";";
		}
		return str;
	}
	
	/***Report the result of a match. Scores are updated accordingly.
	  *
	  * @param winUser - the username of the winner
	  * @param loseUser - the username of the loser
	  * @param draw - whether or not match is a draw. If yes, winUser and loseUser can be interchanged
	  * 
	  * Returns an error code. 0 - success, 1 - one (or both) of the users are not found (or both are the same)***/
	public int reportMatchResult(String winUser, String loseUser, boolean draw, double weightage){
		//int winScore=0, loseScore=0;
		int noFound=0; //indicates the number of users (ie. winner/losers) found
		int winnerID=-1, loserID=-1;
		//search for the users
		//WARNING: OPTIMZATION: Linear search is used here. An server hosting more clients will need to do better
		for(int i = 0;i < scores.length && noFound < 2;i++){
			if(users[i].equals(winUser)){
				//winScore = scores[i];
				winnerID = i;
				noFound++;
			}
			else if(users[i].equals(loseUser)){
				//loseScore = scores[i];
				loserID = i;
				noFound++;
			}
		}
		
		//see if both users have been found
		if(noFound < 2){
			return 1;
		}

		if(!draw){
			scores[winnerID] += weightage;
			scores[loserID] -= weightage;
		}
		
		return 0;
	}
}
