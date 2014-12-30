import java.util.ArrayList;
import java.util.Iterator;

class ScoreboardMatch{
	public Score opponent;
	public int result; //1 - win, -1 - loss, 0 - draw
}

public class Scoreboard{
	int[] scores;
	int[] tempScores; //used for storing the previous state while updating scores
	Authentication auth;
	String[] users;

	int[][] wins;
	int[][] losses;
	int[][] draws;

	int dirtyness = 0;//number of matches that have ppened since the last update

	public Scoreboard(Authentication aAuth){
		auth = aAuth;
		
		users = auth.GetUserList();
		scores = new int[users.length];
		tempScores = new int[users.length];
		for(int i = 0;i < scores.length;i++){
			scores[i] = 400;//new Score(users[i], 400); //initial value
			tempScores[i] = 400;//new Score(users[i], 400);
		}

		wins = new int[scores.length][scores.length];
		losses = new int[scores.length][scores.length];
		draws = new int[scores.length][scores.length];
		for(int i = 0;i < scores.length;i++){
			for(int j = 0;j < scores.length;j++){
				wins[i][j] = losses[i][j] = draws[i][j] = 0;
				if(i == j)draws[i][j] = 1;
			}
		}
	}

	/*public synchronized Score[] getScores(){	
		return scores;
	}*/
	
	private void updateScores(){
		//Check if an update is required
		if(dirtyness < 5i){
			dirtyness++;
			return; //nope
		}
		dirtyness=0;

		//calculate scores and store in tempScores
		for(int i = 0;i < scores.length;i++){
			int noMatches = 0;
			tempScores[i] = 0;
			for(int j = 0;j < scores.length;j++){
				if(scores[i]-scores[i] > 400 || scores[i]-scores[j] < -400)
					continue;
				tempScores[i] += wins[i][j]*(scores[j] + 400);
				tempScores[i] += losses[i][j]*(scores[j] - 400);
				tempScores[i] += draws[i][j]*(scores[j] + 0);
				noMatches += wins[i][j] + losses[i][j] + draws[i][j];
			}
			if(noMatches == 0){
				tempScores[i] = 400;
				continue;
			}
			tempScores[i] /= noMatches;
		}

		//copy back to 'scores'
		for(int i = 0;i < scores.length;i++){
			scores[i] = tempScores[i];
		}
	}
	
	/***Return a string that can be transmitted***/
	public String stringify(){		
		//now compute string
		String str = "";
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
	public int reportMatchResult(String winUser, String loseUser, boolean draw){
		int winScore=0, loseScore=0;
		int noFound=0; //indicates the number of users (ie. winner/losers) found
		int winnerID=-1, loserID=-1;
		//search for the users
		//WARNING: OPTIMZATION: Linear search is used here. An server hosting more clients will need to do better
		for(int i = 0;i < scores.length && noFound < 2;i++){
			if(users[i].equals(winUser)){
				winScore = scores[i];
				winnerID = i;
				noFound++;
			}
			else if(users[i].equals(loseUser)){
				loseScore = scores[i];
				loserID = i;
				noFound++;
			}
		}
		
		//see if both users have been found
		if(noFound < 2){
			return 1;
		}

		if(!draw){
			wins[winnerID][loserID]++;
			losses[loserID][winnerID]++;
		}
		else{
			draws[winnerID][loserID]++;
			draws[loserID][winnerID]++;
		}
		
		updateScores();//update the scores

		return 0;
	}
}
