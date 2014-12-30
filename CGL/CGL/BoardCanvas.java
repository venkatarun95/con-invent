package CGL;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.Timer;

/***A complete package for drawing a board. Can be simply added using <code>add</code> 
 * as it extends <code>Canvas</code>. 
 * 
 * This supports the automatic, timer based animation of the board as well as mouse based 
 * editing (using <code>drawingOn</code> property). It also supports drag and
 * drop editing of the board wherein the user can just drag a machine from the library and 
 * drop it at the appropriate location. This is essential to convenient editing. Deletion
 * of a group of cells is also important but not yet implemented.
 * 
 * Note, from this version onwards, animation does not affect the initial state of the board 
 * and it can always be reset to the initial state
 * 
 * Note: Preferably no detail of the graphics/editing of the board should go outside this 
 * class. Indeed it doesn't.
 * 
 * Known Bugs:
 * -Possible error in timer: What happens when updateRate is faster than the computer can compute?
 * Ans. It behaves well as the swing timer is used here. However, it remains to be investigated
 * as to what will happen if the animation is stopped. Will it stop appropriately or continue
 * to execute till the backlog is over.
 * ***/


public class BoardCanvas extends Canvas implements MouseListener, MouseMotionListener{
	private int noCellsX, noCellsY;
	
	private int updateRate; //in milliseconds. If 0, update does not happen
	private Timer updateTimer;
	private boolean isAnimating;
	
	private BoardClass initBoard; //the initial state of the board
	private BoardClass displayingBoard; //the board being displayed. Used during animation
	private boolean isChallenge; //true if board represents a challenge (ie. two board with one half invisible)
	
	//The color scheme
	private Color colorUnvisited, colorVisited, colorActive, colorGrid;
	private Color colorPreviewActive, colorPreviewInactive; //Active/Inactive denotes actual state of cell beneath preview 
	
	private boolean drawingOn; //indicates whether it should handle on screen drawing by mouse
	private boolean dragPreviewOn; //and dragging objects on the board
	private BoardClass dragPreviewBoard; //the board to be dragged around
	
	/***Called by the constructors to perform initializations that are independent of constructor type***/
	private void constructorHelper(){
		//setSize(width, height);  //initialize the canvas size and color
		setBackground(Color.gray);
		
		//Default colors for the various components
		//ie. Color Scheme
		colorUnvisited = Color.darkGray;
		colorVisited = Color.gray;
		colorActive = Color.blue;
		colorGrid = Color.black;
		colorPreviewActive = Color.cyan;//new Color(0.5f, 0.0f, 0.0f);
		colorPreviewInactive = Color.cyan;//new Color(0.9f, 0.3f, 0.3f);
		/*colorUnvisited = Color.white;
		colorVisited = Color.lightGray;
		colorActive = Color.blue;
		colorGrid = Color.darkGray;
		colorPreviewActive = Color.green;
		colorPreviewInactive = Color.cyan;*/
		
		ActionListener updateTimerListener = new ActionListener(){
			public void actionPerformed(ActionEvent e){
				showNextStep();				
			}
		};
		updateTimer = new Timer(updateRate, updateTimerListener);
		if(updateRate != 0)updateTimer.start();
		isAnimating = (updateRate==0)?false:true;
		
		addMouseListener(this);
		addMouseMotionListener(this);
		
		drawingOn= true;
		dragPreviewOn = false;
	}
	
	/***Constructor:
	 * 
	 * Mode = not a challenge
	 * aWidth, aHeight: dimensions of the board as drawn on the screen
	 * aNoCellsX, aNoCellsY: number of columns and rows resp. in the grid
	 * aUpdateRate: no. of milliseconds between two updates of the board 
	 * while animation. If 0, animation is switched off***/
	BoardCanvas(int aNoCellsX, int aNoCellsY, int aUpdateRate){		
		noCellsX = aNoCellsX; noCellsY = aNoCellsY;  //no of cells in the area
		
		//the main class from which we draw board data
		initBoard = new BoardClass(noCellsX, noCellsY); 
		displayingBoard = new BoardClass(initBoard, false);
		
		isChallenge = false;
		updateRate = aUpdateRate;
		
		constructorHelper();
	}
	
	/***Constructor
	 * 
	 * Mode = challenge
	 * 
	 * @param myBoard The two strings are the 'convertToString()' strings of the two
	 * BoardClass objects of the respective boards.
	 * @param oppBoard Similar to myBoard
	 * @param aUpdateRate no. of milliseconds between two updates of the board 
	 * while animation. If 0, animation is switched off
	 * 
	 * @exception IllegalArgumentException if the two strings are not appropriate.
	 * ***/
	BoardCanvas(String myBoard, String oppBoard, int aUpdateRate){
		initBoard = new BoardClass(myBoard, oppBoard);
		displayingBoard = new BoardClass(initBoard, true);
		
		isChallenge = true;
		updateRate = aUpdateRate;
		
		noCellsX = initBoard.getWidth()/2; noCellsY = initBoard.getHeight();
		
		constructorHelper();
	}
	
	/*public void setInitBoardClass(BoardClass board){
		initBoard = board;
		displayingBoard = new BoardClass(initBoard);
	}*/
	
	public BoardClass getInitBoardClass(){
		return initBoard;
	}
	
	public boolean isChallenge(){return isChallenge;} 
	
	public void makeCastle(){
		initBoard.makeCastle();
		resetBoard();
	}
	
	public void paint(Graphics g){
		paintBoard(g);
	}
	
	/***Draws the board represented by <code>board</code>. Called by the paint method***/
	private void paintBoard(Graphics g){
		int width = getWidth();
		int height = getHeight();
		//System.out.println(width+" "+height);
		float cellWidth = ((float)width)/noCellsX;   //calculate the width of an individual cell
		float cellHeight = ((float)height)/noCellsY;
		//Draw the filled rectangles denoting the cells
		CellState state;
		for(int y = 0;y < noCellsY;y++){
			for(int x = 0;x < noCellsX;x++){
				state = displayingBoard.getCellState(x, y);
				if(state.isPreview()){
					if(state.isActive())
							g.setColor(colorPreviewActive);
					else
							g.setColor(colorPreviewInactive);
				}
				else if(!state.isVisited())
					g.setColor(colorUnvisited);
				else if(state.isVisited() && !state.isActive())
					g.setColor(colorVisited);
				else if(state.isActive())
					g.setColor(colorActive);
				else
					throw new IllegalStateException("The cell state is unrecognised");
				
				g.fillRect((int)(x*cellWidth), (int)(y*cellHeight), (int)(cellWidth)+1, (int)(cellHeight)+1);
			}
		}
		
		//Draw the grid to mark the cells
		g.setColor(colorGrid);
		for(int x = 0;x < noCellsX;x++){
			g.drawLine((int)(x*cellWidth), 0, (int)(x*cellWidth), height);
		}
		for(int y = 0;y < noCellsY;y++){
			g.drawLine(0, (int)(y*cellHeight), width, (int)(y*cellHeight));
		}
		
		g.setColor(Color.lightGray);
		//Warning: The following will make the last row and column of cells appear smaller by a pixel
		g.drawLine(width-1, 0, width-1, height);//Draw the lines at the outer boundaries
		g.drawLine(0, height-1, width, height-1);//These cannot be drawn by the loop as they overshoot by a pixel
	}
	
	/***Methods for enabling previewing of machines on the board***/
	
	public void startDragPreview(BoardClass aDragPreviewBoard){
		/***Starts to drag preview prevBoard. This process stops if mouse is released
		 * @param prevBoard - the board to preview***/
		dragPreviewBoard = aDragPreviewBoard;
		dragPreviewOn = true;
	}

	/***Display 'prevBoard' in the board in preview mode. Does not affect the board's behavior
	 * Note: This method clears any previous things in preview before drawing the current pattern
	 * In case arguments are out of range, this function simply clears the board
	 * Also, this function can be called for clearing the board of preview elements by setting 
	 * <code>prevBoard</code> as null
	 * 
	 * @param startX - x coordinate of top left corner of preview area
	 * @param startY - y coordinate of top left corner of preview area
	 * @param prevBoard - the board which is to be drawn as preview. If it is <code>null</code>, board is cleared***/
	private void dispPreview(int startX, int startY, BoardClass prevBoard){
		//Clear the board
		for(int y = 0;y < noCellsY;y++){
			for(int x = 0;x < noCellsX;x++){
				displayingBoard.getCellState(x, y).makeNonPreview();
			}
		}
		
		//Do not do anything if prevBoard is null
		if(prevBoard == null)return;
		
		//Check bounds
		
		int width = getWidth();
		int height = getHeight();
		float cellWidth = ((float)width)/noCellsX;   //calculate the width of an individual cell
		float cellHeight = ((float)height)/noCellsY;
		
		if(startX*(width-(startX+prevBoard.getWidth())*cellWidth) < 0 || (startY*(height-(startY+prevBoard.getHeight())*cellHeight)) < 0){
			return; //do nothing
		}
		
		//Draw 'prevBoard'
		for(int y = 0;y < prevBoard.getHeight();y++){
			for(int x = 0;x < prevBoard.getWidth();x++){
				if(prevBoard.getCellState(x,y).isActive()){
						displayingBoard.getCellState(x+startX, y+startY).makePreview();
				}
			}
		}
	}
	
	/***Adds <code>pattern</code> to the board. Called when mouse is released while in 
	 * preview more* In case arguments are out of range, this function simply clears the board
	 * 
	 * @param startX - x coordinate of top left corner where <code>pattern</code> is to be added
	 * @param startY - y coordinate of top left corner of preview area
	 * @param pattern - the board which is to be drawn***/
	private void addPatternToBoard(int startX, int startY, BoardClass pattern){
		//Do not do anything if prevBoard is null
		if(pattern == null)throw new IllegalArgumentException("Board to be added cannot be null");
		
		//check bounds
		int width = getWidth();
		int height = getHeight();
		float cellWidth = ((float)width)/noCellsX;   //calculate the width of an individual cell
		float cellHeight = ((float)height)/noCellsY;
		if(startX*(width-(startX+pattern.getWidth())*cellWidth) < 0 || (startY*(height-(startY+pattern.getHeight())*cellHeight)) < 0){
			return; //do nothing
		}
		
		//Draw 'prevBoard'
		for(int y = 0;y < pattern.getHeight();y++){
			for(int x = 0;x < pattern.getWidth();x++){
				if(pattern.getCellState(x,y).isActive()){
						displayingBoard.getCellState(x+startX, y+startY).makeActive();
						if(!isAnimating)
							initBoard.getCellState(x+startX, y+startY).makeActive();
				}
			}
		}
	}
	/***Display a machine on the board in preview mode***/
	//?????
	
	/***Compute the next step and display it***/
	public void showNextStep(){
		displayingBoard.ComputeNextState();
		repaint();
	}
	
	/***Start updating the board every 'aUpdateRate' milliseconds. Ie. start the timer.
	 * 
	 * If argument is 0, previous updateRate will be used***/
	public void startAnimation(int aUpdateRate){
		if(aUpdateRate == 0){
			if(updateRate == 0){
				throw new IllegalArgumentException("The previous update rate was also 0. Function will do nothing.");
			}
		}
		else
		updateRate = aUpdateRate;
		updateTimer.setDelay(updateRate);
		updateTimer.start();
		isAnimating = true;
	}
	
	/***Halt the animation***/
	public void stopAnimation(){
		updateTimer.stop();
		isAnimating = false;
	}
	
	/***Reset the board to initBoard design***/
	public void resetBoard(){
		displayingBoard = new BoardClass(initBoard, isChallenge);
		repaint();
	}
	
	/***Returns number of cells in horizontal direction***/
	public int getNoCellsX(){
		return noCellsX;
	}
	
	/***Returns number of cells in vertical direction***/
	public int getNoCellsY(){
		return noCellsY;
	}
	
	
	/***Implementation of the abstract mouse event handlers.
	 * Basic structure and names of handlers copied from somewhere in the internet.***/
	/***Mouse event handler***/
	public void mouseEntered( MouseEvent e ) {
		// called when the pointer enters the applet's rectangular area
	}
	/***Mouse event handler***/
	public void mouseExited( MouseEvent e ) {
		/***Stop the dragging process***/
		// called when the pointer leaves the applet's rectangular area
		dragPreviewOn = false;
		dispPreview(0,0,null); //clear the board of preview elements
	}
	/***Mouse event handler***/
	public void mouseClicked( MouseEvent e ) {
		// called after a press and release of a mouse button
		// with no motion in between
		// (If the user presses, drags, and then releases, there will be
		// no click event generated.)
		int x, y;
		if(drawingOn){
			float cellWidth = ((float)getWidth())/noCellsX;   //calculate the width of an individual cell
			float cellHeight = ((float)getHeight())/noCellsY;
			x = (int)(e.getX()/cellWidth);
			y = (int)(e.getY()/cellHeight);
			if(displayingBoard.getCellState(x, y).isActive()){
				displayingBoard.getCellState(x, y).makeInactive();
				if(!isAnimating)
					initBoard.getCellState(x, y).makeInactive();
			}
			else{
				displayingBoard.getCellState(x, y).makeActive();
				if(!isAnimating)
					initBoard.getCellState(x, y).makeActive();
			}
		}
		repaint();
		e.consume();
		
	}
	/***Mouse event handler***/
	public void mousePressed( MouseEvent e ) {  
		// called after a button is pressed down
	}
	/***Mouse event handler***/
	public void mouseReleased( MouseEvent e ) { 
		/***Stop the dragging process***/
		// called after a button is released
		if(dragPreviewOn){
			float cellWidth = ((float)getWidth())/noCellsX;   //calculate the width of an individual cell
			float cellHeight = ((float)getHeight())/noCellsY;
			addPatternToBoard((int)(e.getX()/cellWidth), (int)(e.getY()/cellHeight), dragPreviewBoard);
			dragPreviewBoard = null;
		}
		dragPreviewOn = false;
		dispPreview(0,0,null); //clear the board of preview elements
		repaint();
		e.consume();
	}
	/***Mouse event handler***/
	public void mouseMoved( MouseEvent e ) {  
		// called during motion when no buttons are down
	}
	/***Mouse event handler***/
	public void mouseDragged( MouseEvent e ) {  
		/***Handles dragging of objects on class***/
		// called during motion with buttons down
		if(dragPreviewOn){
			float cellWidth = ((float)getWidth())/noCellsX;   //calculate the width of an individual cell
			float cellHeight = ((float)getHeight())/noCellsY;
			dispPreview((int)(e.getX()/cellWidth), (int)(e.getY()/cellHeight), dragPreviewBoard);
			repaint();
			e.consume();
		}
	}
	
	
	//All of this is just to avoid flicker while redrawing the Canvas
	//Placed it at the end as it is not important to the main program and does not affect it
	//This, I believe, improves readability.
	private Image offScreenImage;
	private Dimension offScreenSize;
	private Graphics offScreenGraphics;
	/***Overriding the default update method to avoid flicker while redrawing the board.***/
	public final synchronized void update (Graphics g) {
		int width = getWidth();
		int height = getHeight();
		Dimension d = new Dimension(width, height);
		if((offScreenImage == null) || (d.width != offScreenSize.width) || (d.height != offScreenSize.height)) {
			offScreenImage = createImage(d.width, d.height);
			offScreenSize = d;
			offScreenGraphics = offScreenImage.getGraphics();
		}
		offScreenGraphics.clearRect(0, 0, d.width, d.height);
		paint(offScreenGraphics);
		g.drawImage(offScreenImage, 0, 0, null);
	}
}

