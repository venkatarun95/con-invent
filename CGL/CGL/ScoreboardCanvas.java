package CGL;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ScoreboardCanvas extends JPanel implements ListSelectionListener{
	private int dispWidth;
	private int dispHeight;
	
	private DefaultListModel dispUserList;
	private JList dispUserListJList;
	private JScrollPane dispUserListScrollPane;
	
	private JButton btnChallenge;
	private JButton btnUpdateScores;
	
	NetworkComm communicator;
	CGL mainWindow;
	
	public ScoreboardCanvas(int aDispWidth, int aDispHeight, CGL mainWindow){
		dispWidth = aDispWidth;
		dispHeight = aDispHeight;
		communicator = mainWindow.getCommunicator();
		
		String[] users = new String[4];
		users[0] = "User1"; users[1] = "User2"; 
		users[2]="Imaginative Names"; users[3] = "Nice Name";
		
		//Set up the display
		setSize(dispWidth, dispHeight-10);
		setBackground(Color.white);
		setLayout(new BorderLayout());
		
		//Obtain list of users
		dispUserList = new DefaultListModel();
		/*for(int i = 0;i < users.length;i++)
			dispUserList.add(i, users[i]);*/
		
		dispUserListJList = new JList(dispUserList);
		dispUserListJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dispUserListJList.setSelectedIndex(0);
		dispUserListJList.addListSelectionListener(this);
		dispUserListJList.setVisibleRowCount(5);
		
		dispUserListScrollPane = new JScrollPane(dispUserListJList);
		dispUserListScrollPane.setPreferredSize(new Dimension(dispWidth, dispHeight));
		add(dispUserListScrollPane, "Center");
		
		//set up the button
		BtnEventHandler eventHandler = new BtnEventHandler(this, mainWindow);
		btnChallenge = new JButton("Challenge");
		btnUpdateScores = new JButton("Update Scores");
		btnChallenge.addActionListener(eventHandler);
		btnUpdateScores.addActionListener(eventHandler);
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BorderLayout());
		buttonPane.add(btnChallenge, "East");
		buttonPane.add(btnUpdateScores, "West");
		
		add(buttonPane, "South");
		
		communicator.getScoreboard(new UpdateScores(this));
	}
	
	class UpdateScores implements ServerMessageReceiver{
		ScoreboardCanvas parent;
		public UpdateScores(ScoreboardCanvas aParent){
			parent = aParent;
		}
		
		@Override
		public synchronized void processMessage(String msg, boolean success) {
			if(!success){
				JOptionPane.showMessageDialog(parent, "Error: "+msg);
				return;
			}
			//First remove all existing elements
			dispUserList.removeAllElements();
			//Now, add the elements given in msg
			final int usernameColWidth = 30; //size of 'username' column
			for(int i = 0;i < msg.length();i++){
				//extract the next username and password from 'msg'
				String username="", password="";
				while(msg.charAt(i) != ':'){
					username += msg.charAt(i);
					i++;
				}
				String spaces = "";
				for(int j = 0;j < usernameColWidth-username.length();j++)
					spaces += " ";
				i++;
				while(msg.charAt(i) != ';'){
					password += msg.charAt(i);
					i++;
				}
				
				dispUserList.addElement(username+spaces+password);
			}
		}
		
	}
	
	class Challenge implements ServerMessageReceiver{
		private CGL mainWindow;
		
		public Challenge(CGL aMainWindow){
			mainWindow = aMainWindow;
		}
		
		@Override
		public synchronized void processMessage(String msg, boolean success) {
			if(!success){
				JOptionPane.showMessageDialog(null, "Error: "+msg);
				return;
			}
			JOptionPane.showMessageDialog(null, "Challenge has been sent. Waiting for the other user to confirm.");
		}
	}
	
	class BtnEventHandler implements ActionListener{
		ScoreboardCanvas parent;
		CGL mainWindow;
		
		public BtnEventHandler(ScoreboardCanvas aParent, CGL aMainWindow){
			parent = aParent;
			mainWindow = aMainWindow;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == btnUpdateScores){
				communicator.getScoreboard(new UpdateScores(parent));
			}
			else if(e.getSource() == btnChallenge){
				int index = parent.dispUserListJList.getSelectedIndex();
				if(index == -1){ //nothing is selected
					JOptionPane.showMessageDialog(null, "Please select a user to challenge.");
					return;
				}
				String toChallengeStr = (String) parent.dispUserList.getElementAt(index);
				String toChallenge = ""; //extract the username part of it
				for(int i = 0;i < toChallengeStr.length();i++){
					if(toChallengeStr.charAt(i) == ' ')break;
					toChallenge += toChallengeStr.charAt(i);
				}
				if(mainWindow.getIsChallengeMode()){
					JOptionPane.showMessageDialog(null, "Error: The board currently displaying represents a challenge. Please seleect a design board to challenge with.");
					return;
				}
				communicator.challenge(new Challenge(mainWindow), toChallenge, mainWindow.getMainBoardString());
			}
		}
	}
	
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(e.getValueIsAdjusting() == false){
			if(dispUserListJList.getSelectedIndex() == -1){
				btnChallenge.setEnabled(false);
			}
			else{
				btnChallenge.setEnabled(true);
			}
		}
	}
}
