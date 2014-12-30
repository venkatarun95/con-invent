package CGL;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import CGL.ScoreboardCanvas.BtnEventHandler;
import CGL.ScoreboardCanvas.UpdateScores;

class ChallengeData{
	String myName;
	String oppName;
	String myBoard;
	String oppBoard;
	int result;
	
	public ChallengeData(String aMyName, String aOppName, String aMyBoard, String aOppBoard){
		myName = aMyName;
		oppName = aOppName;
		aMyBoard = myBoard;
		oppBoard = aOppBoard;
	}
	
	/***Converts from a string that is transmitted to challenge data
	 * 
	 * @param msg - The message received. Possibly from server
	 * @param challenger - true if current user is the challenger. The challenger's board appears first in string
	 */
	public ChallengeData(String msg, String aMyBoard, boolean challenger){
		//extract challenger's name
		int i=0, prevI;
		for(;i < msg.length();i++){
			if(msg.charAt(i) == ':')break;
		}
		String challengerName = msg.substring(0, i);
		//extract challenged's name
		prevI = i;
		++i;
		for(;i < msg.length();i++){
			if(msg.charAt(i) == ';')break;
		}
		String challengedName = msg.substring(prevI+1, i);
		//extract challenger's board
		prevI = i;
		++i;
		for(;i < msg.length();i++){
			if(msg.charAt(i) == ':')break;
		}
		String challengerBoard = msg.substring(prevI+1, i);
		String challengedBoard = msg.substring(i+1);
		if(challenger){
			myName = challengerName;
			oppName = challengedName;
			myBoard = challengerBoard;
			oppBoard = challengedBoard;
		}
		else{
			myName = challengedName;
			oppName = challengerName;
			myBoard = aMyBoard;//challengedBoard;
			oppBoard = challengerBoard;
		}
		//Find the result
		BoardClass board = new BoardClass(myBoard,oppBoard);
		result = board.runMatchAndFindResult();
	}
	
	public String getPrintable(){
		int noSpaces = 15;
		String res = oppName;
		for(int i = oppName.length();i < noSpaces;i++)
			res += " ";
		
		//append the result
		if(result == 1){
			res += "Result: YOU WON";
		}
		else if(result == -1){
			res += "Result: YOU LOST";
		}
		else if(result == 0){
			res += "Result: DRAW";
		}
		else{
			res += "Result: ERROR. Please report this";
		}
		return res;
	}
	
	public String getOpponent(){return oppName;}
	public String getMyBoard(){return myBoard;}
	public String getOppBoard(){return oppBoard;}
	public int getResult(){return result;}
}

public class BoardChooserPanel extends Panel implements ListSelectionListener{
	private int dispWidth;
	private int dispHeight;
	
	private DefaultListModel<String> challengeList;
	private JList<String> challengeListJList;
	private JScrollPane challengeListScrollPane;
	
	private DefaultListModel<String> boardList;
	private JList<String> boardListJList;
	private JScrollPane boardListScrollPane;
	
	private ArrayList<BoardCanvas> boards;
	private int boardWidth, boardHeight, boardNoCellsX, boardNoCellsY;
	
	private JButton btnChallengePlay;
	private JButton btnBoardPlay;
	private JButton btnAddBoard;
	
	private ArrayList<ChallengeData> challenges; //will hold objects of type ChallengeData
	
	CGL mainWindow;
	
	public BoardChooserPanel(int aDispWidth, int aDispHeight, BoardCanvas firstBoard, CGL aMainWindow){
		dispWidth = aDispWidth;
		dispHeight = aDispHeight;
		mainWindow = aMainWindow;
		
		challenges = new ArrayList<ChallengeData>();
		
		boards = new ArrayList<BoardCanvas>();
		boards.add(firstBoard);
		boardWidth = firstBoard.getWidth(); boardHeight = firstBoard.getHeight();
		boardNoCellsX = firstBoard.getNoCellsX();
		boardNoCellsY = firstBoard.getNoCellsY();
		
		//Set up the display
		setSize(dispWidth, dispHeight-10);
		setBackground(Color.darkGray);
		setLayout(new BoxLayout(this, 1));
		
		//Prepare the CHALLENGES LIST to display
		challengeList = new DefaultListModel<String>(); //elements will be 'add'ed to this as and when required
		
		challengeListJList = new JList<String>(challengeList);
		challengeListJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		challengeListJList.setSelectedIndex(0);
		challengeListJList.addListSelectionListener(this);
		challengeListJList.setVisibleRowCount(5);
		challengeListJList.setBackground(Color.darkGray);
		challengeListJList.setForeground(Color.white);
		
		challengeListScrollPane = new JScrollPane(challengeListJList);
		challengeListScrollPane.setPreferredSize(new Dimension(dispWidth, dispHeight/2-5));
		add(challengeListScrollPane);
		
		BtnEventHandler eventHandler = new BtnEventHandler(this, mainWindow);
		btnChallengePlay = new JButton("Play");
		btnChallengePlay.addActionListener(eventHandler);
		add(btnChallengePlay);
		
		//Prepare the BOARDS LIST to display
		//Prepare the list to display
		boardList = new DefaultListModel<String>(); //elements will be 'add'ed to this as and when required
		boardList.add(0, "Board 1");
		
		boardListJList = new JList<String>(boardList);
		boardListJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		boardListJList.setSelectedIndex(0);
		boardListJList.addListSelectionListener(this);
		boardListJList.setVisibleRowCount(5);
		boardListJList.setBackground(Color.darkGray);
		boardListJList.setForeground(Color.white);
		
		boardListScrollPane = new JScrollPane(boardListJList);
		boardListScrollPane.setPreferredSize(new Dimension(dispWidth, dispHeight/2-5));
		boardListScrollPane.setBackground(Color.darkGray);
		add(boardListScrollPane);
		
		//set up the button
		btnBoardPlay = new JButton("Play");
		btnBoardPlay.addActionListener(eventHandler);
		btnAddBoard = new JButton("Add Board");
		btnAddBoard.addActionListener(eventHandler);
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout());
		buttonPane.add(btnBoardPlay);
		buttonPane.add(btnAddBoard);
		buttonPane.setBackground(Color.darkGray);
		
		add(buttonPane);
	}
	
	/***Adds a new challenge to the list displayed
	 * 
	 * @param msg - The message received. Possibly from server
	 * @param myBoard - required if challenger is false as then the server does not know about our board
	 * @param challenger - true if current user is the challenger. The challenger's board appears first in string
	 */
	public void addChallenge(String msg, String myBoard, boolean challenger){
		if((challenger==false && myBoard==null) || (challenger==true) && myBoard!=null){
			throw new IllegalArgumentException("'myBoard' is required if and only if challenger is false");
		}
		ChallengeData newChallenge= new ChallengeData(msg, myBoard, challenger);
		
		//report result
		try{
			int result = newChallenge.getResult();
			ChallengeResultReporter reporter = new ChallengeResultReporter();
			mainWindow.getCommunicator().reportChallengeResult(result, reporter);
			
			//display match details
			challenges.add(0, newChallenge);
			challengeList.add(0, newChallenge.getPrintable());
		}catch(IllegalArgumentException e){ //happens if one of the users sends a challenge board instead of a design one
			JOptionPane.showMessageDialog(null, "Error: Error in one (or both) of the two boards. Probably size does not match");
			//do not report anything
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		// TODO handle this 
	}
	
	class BtnEventHandler implements ActionListener{
		BoardChooserPanel parent;
		CGL mainWindow;
		
		public BtnEventHandler(BoardChooserPanel aParent, CGL aMainWindow){
			parent = aParent;
			mainWindow = aMainWindow;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == btnBoardPlay){
				int index = boardListJList.getSelectedIndex();
				if(index == -1){
					JOptionPane.showMessageDialog(null, "Please choose a board to play.");
					return;
				}
				BoardCanvas board = boards.get(index);
				mainWindow.setMainBoard(board);
			}
			else if(e.getSource() == btnAddBoard){
				BoardCanvas board = new BoardCanvas( boardNoCellsX, boardNoCellsY, 0);
				board.makeCastle();
				boards.add(board);
				int noElements = boardList.getSize();
				noElements = (noElements == -1)?0:noElements;
				boardList.add(noElements, "Board "+(noElements+1));
				mainWindow.setMainBoard(board);
			}
			else if(e.getSource() == btnChallengePlay){
				int index = challengeListJList.getSelectedIndex();
				if(index == -1){
					JOptionPane.showMessageDialog(null, "Please choose a challenge to play.");
					return;
				}
				//BoardClass challengeClass = new BoardClass(challenges.get(index).getOppBoard());
				//BoardCanvas challengeCanvas = new BoardCanvas(challengeClass.getWidth(), challengeClass.getHeight(), 0);
				//challengeCanvas.setInitBoardClass(challengeClass);
				ChallengeData challenge = challenges.get(index);
				BoardCanvas challengeCanvas = new BoardCanvas(challenge.getMyBoard(), challenge.getOppBoard(), 0);
				mainWindow.setMainBoard(challengeCanvas);
			}
		}
	}
	
	class ChallengeResultReporter implements ServerMessageReceiver{
		@Override
		public void processMessage(String msg, boolean success) {
			if(!success)
				JOptionPane.showMessageDialog(null, "Error in reporting results: "+msg);
			//else all is well. Be quiet
		}
	}
}
