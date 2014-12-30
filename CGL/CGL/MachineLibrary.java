package CGL;

import java.awt.BorderLayout;

import java.awt.Button;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;


public class MachineLibrary extends Panel implements ActionListener{
	private int width;
	private int height;
	
	JScrollPane scroll;
	MachineLibraryPanel libraryPanel;
	BoardCanvas[] machines;
	
	Button btnNewMachinePanel;
	
	public MachineLibrary(int aWidth, int aHeight, CGL mainWindow){
		width = aWidth;
		height = aHeight;
		
		setLayout(new BorderLayout());
		setSize(width, height);
		setBackground(Color.darkGray);
		setForeground(Color.white);
		
		libraryPanel = new MachineLibraryPanel(width-width/10, 6, mainWindow);
		scroll = new JScrollPane(libraryPanel);
		scroll.setPreferredSize(new Dimension(width-10, height));
		scroll.setSize(width, height);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(scroll, "Center");
		
		btnNewMachinePanel = new Button("New Machine Panel");
		btnNewMachinePanel.addActionListener(this);
		add(btnNewMachinePanel, "South");
		
		/*scroll = new Scrollbar(Scrollbar.VERTICAL, 0, 1, 0, 100);
		add(scroll);
		scroll.setBounds(width-width/10, height-height/10, width/10, height/10);
		
		libraryCanvas = new MachineLibraryCanvas(width-width/10);
		add(libraryCanvas);
		libraryCanvas.setBounds(0, 0, width-width/10, libraryCanvas.getHeight());*/
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == btnNewMachinePanel){
			libraryPanel.addNewMachinePanel(); //this function will handle it
		}
		revalidate(); //redraws everything
	}
}

class MachineLibraryPanel extends JPanel implements ActionListener{
	private int width;
	private int height;
	private int noBoards;
	private final double widthToHeightRatio = 1; //aspect ratio of each rectangle representing a machine
	
	BoardCanvas[] machines;
	JPanel[] machinePanels;
	
	Button[] btnsNewInstance;
	Button[] btnsRemove;
	JPanel[] btnPanels;
	
	Button btnNewMachinePanel; //it resides with the parent though
	
	CGL mainWindow; //so that we may add machines to the main board
	
	public MachineLibraryPanel(int aWidth, int noInitialBoards, CGL aMainWindow){
		width = aWidth;
		noBoards = noInitialBoards;
		mainWindow = aMainWindow;
		setLayout(new BoxLayout(this, 1));
		
		machines = new BoardCanvas[noBoards];
		machinePanels = new JPanel[noBoards];
		
		btnsNewInstance = new Button[noBoards];
		btnsRemove = new Button[noBoards];
		btnPanels = new JPanel[noBoards];
		
		for(int i = 0;i < noBoards;i++){
			machines[i] = new BoardCanvas(6, 6, 0);
			machines[i].setSize(width, width);
			
			btnsNewInstance[i] = new Button("New Instance");
			btnsRemove[i] = new Button("Remove");
			btnPanels[i] = new JPanel(new FlowLayout());
			
			btnsNewInstance[i].addActionListener(this);
			btnsRemove[i].addActionListener(this);
			
			btnPanels[i].add(btnsRemove[i]);
			btnPanels[i].add(btnsNewInstance[i]);
			btnPanels[i].setBackground(Color.darkGray);
			btnPanels[i].setForeground(Color.white);
			
			machinePanels[i] = new JPanel(new BorderLayout());
			machinePanels[i].add(machines[i], "Center");
			machinePanels[i].add(btnPanels[i], "South");
			add(machinePanels[i]);
		}
		//setSize(width, width*noBoards);
	}
	
	/***Adds a new machine panel after asking the user for the required size
	 * 
	 *  Not very efficient, but serves our purpose***/
	public void addNewMachinePanel() {
		int side = new Integer(JOptionPane.showInputDialog(null, "Enter  size of the board's side"));
		
		BoardCanvas[] newMachines = new BoardCanvas[noBoards+1];//allocate memory
		Button[] newBtnsNewInstance = new Button[noBoards+1];
		Button[] newBtnsRemove = new Button[noBoards+1];
		JPanel[] newMachinePanels = new JPanel[noBoards+1];
		JPanel[] newBtnPanels = new JPanel[noBoards+1];
		
		//copy data
		for(int i = 0;i < noBoards;i++){
			newMachines[i] = machines[i];
			newBtnsNewInstance[i] = btnsNewInstance[i];
			newBtnsRemove[i] = btnsRemove[i];
			newMachinePanels[i] = machinePanels[i];
			newBtnPanels[i] = btnPanels[i];
		}
		
		//create the new entities
		newMachines[noBoards] = new BoardCanvas(side, side, 0);
		newBtnsNewInstance[noBoards] = new Button("New Instance");
		newBtnsRemove[noBoards] = new Button("Remove");
		
		newMachines[noBoards].setSize(width, width);
		
		machines = newMachines;
		btnsNewInstance = newBtnsNewInstance;
		btnsRemove = newBtnsRemove;
		machinePanels = newMachinePanels;
		btnPanels = newBtnPanels;
		
		btnPanels[noBoards] = new JPanel(new FlowLayout());
		btnsNewInstance[noBoards].addActionListener(this);
		btnsRemove[noBoards].addActionListener(this);
		
		btnPanels[noBoards].add(btnsRemove[noBoards]);
		btnPanels[noBoards].add(btnsNewInstance[noBoards]);
		btnPanels[noBoards].setBackground(Color.darkGray);
		btnPanels[noBoards].setForeground(Color.white);
		
		machinePanels[noBoards] = new JPanel(new BorderLayout());
		machinePanels[noBoards].add(machines[noBoards], "Center");
		machinePanels[noBoards].add(btnPanels[noBoards], "South");
		add(machinePanels[noBoards]);
		++noBoards;
		revalidate();
	}

	/***Deletes the specified element from the array. 
	 * 
	 * 	Used when the 'Remove' button is clicked. It is not very efficient,
	 * 	but for our purposes, it doesn't matter.***/
	private void deleteArrayElement(Object[] arr, int index){
		if(index >= arr.length || index < 0){ //we cannot delete something not in the array
			System.err.println("MachineLibraryCanvas:deleteArrayElement :- index is out of bounds (not within the array");
			return; //don't break the execution. Just return.
		}
		for(int i = index;i < arr.length-1;i++){
			arr[i] = arr[i+1];
		}
		arr[arr.length-1] = null; //the remaining element will be automatically garbage collected

	}

	@Override
	/***Handle various button-click events***/
	public void actionPerformed(ActionEvent e) {
		//See if a 'New Instance' button was clicked
		for(int i = 0;i < noBoards;i++){
			if(e.getSource() == btnsNewInstance[i]){
				mainWindow.dragMachineOverMainboard(machines[i].getInitBoardClass());
				return;
			}
		}
		//See if a 'Remove' button was clicked
		for(int i = 0;i < noBoards;i++){
			if(e.getSource() == btnsRemove[i]){
				btnPanels[i].remove(btnsNewInstance[i]);
				btnPanels[i].remove(btnsRemove[i]);
				machinePanels[i].remove(btnPanels[i]);
				machinePanels[i].remove(machines[i]);
				remove(machinePanels[i]);
				
				deleteArrayElement(btnsNewInstance, i);
				deleteArrayElement(btnsRemove, i);
				deleteArrayElement(btnPanels, i);
				deleteArrayElement(machines, i);
				deleteArrayElement(machinePanels, i);
				--noBoards;
				mainWindow.revalidate(); //redraws all components
				return;
			}
		}
	}
}

