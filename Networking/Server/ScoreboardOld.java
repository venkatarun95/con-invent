import java.util.ArrayList;
import java.util.Iterator;

class ScoreboardMatch{
	public Score opponent;
	public int result; //1 - win, -1 - loss, 0 - draw
}

class Score{
	int score;
	String username;
	int noGames;
	
	boolean dirty; //whether scores need to be recomputed. Although all scores need to be recomputed, as an approximation, this is enough
	ArrayList<ScoreboardMatch> matches; //the list of all matches (and their results)

	public Score(String aUsername, int aScore){
		score = aScore;
		username = aUsername;
		matches = new ArrayList<ScoreboardMatch>();
		dirty = false;
		noGames = 0;
	}
	
	public synchronized String getUsername(){return username;}
	public synchronized int getScore(){return score;}
	public synchronized int getNoGames(){return noGames;}
	
	public synchronized void setScore(int aScore){score = aScore;}
	public synchronized void incrNoGames(){++noGames;}
	
	public synchronized void updateScore(){
		int newScore = 0;
		if(matches.size() == 0)return; //no matches yet
		
		Iterator iter = matches.iterator();
		while(iter.hasNext()){
			ScoreboardMatch match = (ScoreboardMatch)iter.next();

			int addend;
			if(match.result == 1)addend = 400;
			else if(match.result == -1)addend = -400;
			else if(match.result == 0)addend = 0;
			else throw new IllegalStateException("unrecognised result code"); //Should never reach here
			
			newScore += match.opponent.getScore()+addend;			
		}
		newScore /= matches.size();
		score = newScore;
		dirty = false;
	}
	
	public synchronized void addMatch(Score opponent, int result){
		ScoreboardMatch match = new ScoreboardMatch();
		match.opponent = opponent; match.result = result;
		matches.add(match);
		dirty = true;
	}
	
	public synchronized boolean isDirty(){return dirty;}
	
	public Object clone(){  
		try{
        	return super.clone();  
        } catch(CloneNotSupportedException e){
        	System.err.println("Scoreboard.java:Score:clone :- Clone not supported exception found");
        	return this;
        }
    }
}

public class Scoreboard{
	Score[] scores;
	Score[] tempScores; //used for storing the previous state while updating scores
	Authentication auth;
	
	int dirtiness; //records the number of matches before the last complete recomputation was done
	final int maxDirtinessBeforeRecompute = 10; //maximum number of matches before all scores are recomputed

	public Scoreboard(Authentication aAuth){
		auth = aAuth;
		dirtiness = 0;
		
		String[] users = auth.GetUserList();
		scores = new Score[users.length];
		tempScores = new Score[users.length];
		for(int i = 0;i < scores.length;i++){
			scores[i] = new Score(users[i], 400); //initial value
			tempScores[i] = new Score(users[i], 400);
		}
	}

	public synchronized Score[] getScores(){	
		return scores;
	}
	
	private void updateScores(){
		//copy to tempScores
		/*for(int i = 0;i < scores.length;i++){
			tempScores[i] = (Score)scores[i].clone(); //WARNING: OPTIMIZATION: this entire function is highly inefficient
		}
		
		if(dirtiness > maxDirtinessBeforeRecompute){
			for(int i = 0;i < scores.length;i++){
				tempScores[i].updateScore();
			}
			dirtiness = 0;
		}
		else{
			for(int i = 0;i < scores.length;i++){
				if(tempScores[i].isDirty())tempScores[i].updateScore();
			}
		}
		
		//copy back. Cannot simply exchange as each Score object has references to other Score objects representing each match
		for(int i = 0;i < scores.length;i++){
			scores[i] = (Score)tempScores[i].clone();
		}*/
	}
	
	/***Return a string that can be transmitted***/
	public String stringify(){
		//updateScores();//first, update the scores
		
		//now compute string
		String str = "";
		for(int i = 0;i < scores.length;i++){
			str += scores[i].getUsername()+":"+scores[i].getScore()+";";
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
		Score winScore=null, loseScore=null;
		int noFound=0; //indicates the number of users (ie. winner/losers) found
		
		//search for the users
		//WARNING: OPTIMZATION: Linear search is used here. An server hosting more clients will need to do better
		for(int i = 0;i < scores.length && noFound < 2;i++){
			if(scores[i].getUsername().equals(winUser)){
				winScore = scores[i];
				noFound++;
			}
			else if(scores[i].getUsername().equals(loseUser)){
				loseScore = scores[i];
				noFound++;
			}
		}
		
		/*//see if both users have been found
		if(winScore == null || loseScore == null){
			return 1;
		}
		
		//add the match to the relevant users
		winScore.addMatch(loseScore, ((draw)?0:1));
		loseScore.addMatch(winScore, ((draw)?0:-1));
		
		++dirtiness;
		*/
		
		//update the scores according to naive ELO's rating system
		if(!draw){
			//Update the winner's score
			int noWinnerGames = winScore.getNoGames();
			int noLoserGames = loseScore.getNoGames();
			int winnerScore = winScore.getScore(), loserScore = loseScore.getScore();
			
			winScore.setScore((winnerScore*noWinnerGames + loserScore + 400)/(noWinnerGames+1));
			loseScore.setScore((loserScore*noLoserGames + winnerScore - 400)/(noLoserGames+1));
		}
		else{
			//Update the winner's score
			int noWinnerGames = winScore.getNoGames();
			int noLoserGames = loseScore.getNoGames();
			int winnerScore = winScore.getScore(), loserScore = loseScore.getScore();
			
			winScore.setScore((winnerScore*noWinnerGames + loserScore + 0)/(noWinnerGames+1));
			loseScore.setScore((loserScore*noLoserGames + winnerScore - 0)/(noLoserGames+1));
		}
		
		winScore.incrNoGames();
		loseScore.incrNoGames();
		
		return 0;
	}
}
