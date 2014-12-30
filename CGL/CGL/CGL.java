package CGL;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Canvas;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JOptionPane;

/***The wrapper class creating the final UI. Adds the board and a button to the screen.
 * In this, it is possible to play around with the constructor of Board Canvas,
 * to modify the size, shape and Animation speed of the board.***/
/*Please look at the individual files of the various classes for the documentation on their interface
 * (ie. how they nay be used)*/
public class CGL extends Applet implements ActionListener{
	private BoardCanvas mainBoard;
	private Button btnStartStop, btnResetBoard, btnShowScoreboard, btnNextStep, btnRelogin;
	
	private ScoreboardCanvas scoreboard;
	private JFrame scoreboardFrame;
	
	private BoardChooserPanel challengeHistoryPanel;
	
	private MachineLibrary machineLibrary;
	
	private NetworkComm communicator;
	
	public void init(){
		setLayout(new BorderLayout());
		setBackground(Color.darkGray);
		resize(1000, 1000);
		
		//Manage network
		try{
			communicator = new NetworkComm();
		}catch(IOException e){
			JOptionPane.showMessageDialog(this, "Could not connect to server. Exiting.");
			e.printStackTrace();
			System.exit(1);
		}
		//Authenticate user
		AuthenticateUser authenticator = new AuthenticateUser(this);
		authenticator.authenticateUser();
		
		communicator.setChallengeReceiver(new ChallengeReceiver());
		
		mainBoard = new BoardCanvas(40, 50, 0);
		mainBoard.makeCastle();
		challengeHistoryPanel = new BoardChooserPanel(300, 550, mainBoard, this);
		
		add(challengeHistoryPanel, "East");
		add(mainBoard, "Center");
		
		machineLibrary = new MachineLibrary(200, 600, this);
		add(machineLibrary, "West");
		
		JPanel buttonPanel = new JPanel(new FlowLayout());
		
		btnResetBoard = new Button("Reset");
		btnResetBoard.addActionListener(this);
		buttonPanel.add(btnResetBoard);
		
		btnStartStop = new Button("Start");
		btnStartStop.addActionListener(this);
		buttonPanel.add(btnStartStop);
		
		btnShowScoreboard = new Button("Show Scoreboard");
		btnShowScoreboard.addActionListener(this);
		buttonPanel.add(btnShowScoreboard);
		
		btnNextStep = new Button("NextStep");
		btnNextStep.addActionListener(this);
		buttonPanel.add(btnNextStep);
		
		btnRelogin = new Button("Re-login");
		btnRelogin.addActionListener(this);
		buttonPanel.add(btnRelogin);
		
		buttonPanel.setBackground(Color.darkGray);
		buttonPanel.setForeground(Color.white);
		add(buttonPanel, "South");
		
		//The scoreboard window
		scoreboard = new ScoreboardCanvas(300, 500, this);
		
		scoreboardFrame = new JFrame("Scoreboard");
		scoreboardFrame.add(scoreboard);
		scoreboardFrame.setVisible(false);
		scoreboardFrame.setSize(new Dimension(300,500));
		scoreboardFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		
	}
	
	public String getMainBoardString(){
		return mainBoard.getInitBoardClass().convertToString();
	}
	
	/***Returns whether or not mainboard is in a challenge mode
	 * 
	 * Called by ScoreboardCanvas to check whether it is a design board that can be sent***/
	public boolean getIsChallengeMode(){
		return mainBoard.isChallenge();
	}
	
	/***Sets the main board to a new board. Called by BoardChooserPanel***/
	public void setMainBoard(BoardCanvas board){
		mainBoard.stopAnimation();
		btnStartStop.setLabel("Start"); //stop the animation
		
		this.remove(mainBoard);
		mainBoard = board;
		this.add(mainBoard, "Center");
		mainBoard.revalidate(); //redraws all components
	}
	
	public NetworkComm getCommunicator(){return communicator;}
	
	class AuthenticateUser implements ServerMessageReceiver{
		CGL parent;
		private AuthenticateUser(CGL aParent){
			parent = aParent;
		}
		private void authenticateUser(){
			String username = JOptionPane.showInputDialog((Component)parent, "Enter your username");
			String password = JOptionPane.showInputDialog((Component)parent, "Enter your password");
			if(username == null || password == null){
				JOptionPane.showMessageDialog((Component)parent, "You need to login to use this applet.");
				//System.exit(1); //no need to exit. The server is smart enough
				return;
			}
			communicator.authenticate(username, password, this);			
		}
		@Override
		public synchronized void processMessage(String msg, boolean success) {
			if(!success){
				JOptionPane.showMessageDialog(parent, "Error: "+msg);
				authenticateUser();//keep troubling till the user loggs in
				return;
			}
			JOptionPane.showMessageDialog(parent, "Authentication successful");
		}	
	}
	
	class ChallengeReceiver implements ServerMessageReceiver{
		//int errorCount = 0;
		@Override
		public synchronized void processMessage(String msg, boolean success) {
			if(!success){ 
				JOptionPane.showMessageDialog(null, "Error: "+msg);
				return;
			}
			if(msg.equals("Challenge Accepted")){
				//Challenge has been accepted by this user and this has been conveyed to the server
				return;
			}
			//JOptionPane.showMessageDialog(null, "CGL:ChallengeReceiver:ProcessMessage :- "+msg);
			//extract type of response
			int i;
			for(i = 0;i < msg.length();i++){
				if(msg.charAt(i) == ':')break;
			}
			String respType = msg.substring(0, i);
			//extract challenger's name
			int prevI = i;
			++i;
			for(;i < msg.length();i++){
				if(msg.charAt(i) == ':')break;
			}
			String challenger = msg.substring(prevI+1, i);

			//extract challenged's name
			prevI = i;
			++i;
			for(;i < msg.length();i++){
				if(msg.charAt(i) == ';')break;
			}
			String challenged = msg.substring(prevI+1, i);
			
			//process the challenge based on type
			if(respType.equals("Accepted")){//challenge accepted by the other user
				JOptionPane.showMessageDialog(null, "Your challenge has been accepted by: "+challenged);
				challengeHistoryPanel.addChallenge(msg.substring(respType.length()+1), null, true);
			}
			else if(respType.equals("Rejected")){//challenge rejected by the other user
				JOptionPane.showMessageDialog(null, "Your challenge has been rejected by: "+challenged);
			}
			else if(respType.equals("Proposed")){//challenge proposed by another user
				//ask user for response
				int response = JOptionPane.showConfirmDialog(null, "You have been challenged by "+challenger+". Accept?");
				if(response == 0){ //accepted
					if(mainBoard.isChallenge()){//only send if it is a board made by the user and not a challenge
						JOptionPane.showMessageDialog(null, "Error: The board currently displaying represents a challenge. Please seleect a design board to challenge with.");
					}
					else{
						communicator.challengeResponse(true, challenger, mainBoard.getInitBoardClass().convertToString(), this); //accept
						challengeHistoryPanel.addChallenge(msg.substring(respType.length()+1),  mainBoard.getInitBoardClass().convertToString(), false);
					}
				}
				else{
					communicator.challengeResponse(false, challenger, null, this); //reject
				}
			}
			else{
				System.err.println("Wrong challenge request sent by server: "+msg);
			}
		}
	}
	
	public void dragMachineOverMainboard(BoardClass machine){
		mainBoard.startDragPreview(machine);
	}
	
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == btnStartStop){
			if(btnStartStop.getLabel() == "Start"){
				mainBoard.startAnimation(100);
				btnStartStop.setLabel("Stop");
			}
			else{
				mainBoard.stopAnimation();
				btnStartStop.setLabel("Start");
			}
		}
		else if(e.getSource() == btnResetBoard){
			mainBoard.resetBoard(); //set it back to the initial board design
		}
		else if(e.getSource() == btnShowScoreboard){
			scoreboardFrame.setVisible(true);
		}
		else if(e.getSource() == btnNextStep){
			mainBoard.showNextStep();
		}
		else if(e.getSource() == btnRelogin){
			//Authenticate user
			AuthenticateUser authenticator = new AuthenticateUser(this);
			authenticator.authenticateUser();
		}
	}
	
	public void paint(Graphics g){	
	}
}
