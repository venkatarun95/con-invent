
/***Represents the credentials of a user***/
class User{
	private String username;
	private String password;
	private int state; //0 - offline, 1 - online
	private int noAttempts; //number of attempts made

	public User(String user, String pass){
		username = user;
		password = pass;
		state = 0;
		noAttempts = 0; //unacceptable when it is greater than 1000
	}

	public synchronized String getUsername(){
		return username;
	}
	public synchronized int checkPassword(String pswd){
		++noAttempts;
		if(noAttempts > 1000)
			return 4; //Error Code: tried to login too many times
		if(password.equals(pswd))
			return 1;
		return 0;
	}

	public synchronized boolean isOnline(){
		return (state==1)?true:false;
	}
	public synchronized void setStateOnline(){
		state = 1;
	}
	public synchronized void setStateOffline(){
		state = 0;
	}
}

public class Authentication{
	private User[] regUsers; //registered users
	
	public Authentication(){
		regUsers = new User[3];
		regUsers[0] = new User("User1", "nicename");
		regUsers[1] = new User("User2", "imaginative");
		regUsers[2] = new User("User3", "helloworld");
	}
	
	/***Returns whether the user credentials are valid. Logs in user if they are.***/
	public synchronized int AuthenticateNewUser(String username, String password){
		for(int i = 0;i < regUsers.length;i++){
			if(regUsers[i].getUsername().equals(username)){
				int authState = regUsers[i].checkPassword(password);
				if(authState == 1){ //correct password
					if(regUsers[i].isOnline()){ //user already online. Return with error code 3
						return 3;
					}
					regUsers[i].setStateOnline();
					return 1;
				}
				else if(authState == 4) //too many attempts at guessing the password
					return 4;
				else
					return 0; //user was not found
			}
		}
		return 2; //Error code indicating that user was not found
	}

	/***Sets user state to 'offline'***/
	public synchronized void LogOffUser(String username){
		for(int i = 0;i < regUsers.length;i++){
			if(regUsers[i].getUsername().equals(username)){
				regUsers[i].setStateOffline();
			}
		}
	}

	/***Returns list of all users (signed in or not)***/
	public synchronized String[] GetUserList(){
		String[] userList = new String[regUsers.length];
		for(int i = 0;i < regUsers.length;i++){
			userList[i] = new String(regUsers[i].getUsername());
		}
		return userList;
	}
}
