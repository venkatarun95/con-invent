package CGL;

/***
 * This file contains two classes CellState and BoardClass. Their functions are detailed in the
 * JavaDoc comments above them.***/

/***Represents the state of a cell.
 * A cell can be active or inactive. Visited or unvisited. For now there are a total of four states.
 * The state representing active and unvisited is meaningless and should not occur in an
 * error free program.
 * 
 * The current state of a cell can be accessed and modified using isActive(), isVisited(), 
 * makeActive(), makeInactive(), makeVisited() etc. The state of the current object can be set to
 * be the same as that of another object by using setState(CellState src)
 * 
 * Internal Representation:
 * The Least Significant Bit (LSB) represents whether the cell is visited or not. That is, it is 1
 * if the cell is visited, 0 otherwise.
 * The second Least Significant Bit represents whether the cell is active or not. The bits are 
 * manipulated using bitwise operations the details of which are easy to understand but not 
 * necessary to be able to use the class.
 * 
 * Further states may be added as required. For instance, when we are dragging a machine on the 
 * board, we may want to show the user its current location by coloring the cells differently. 
 * For this a preview state may be defined.***/
class CellState{
	/*Uses bitwise operations to represent state. 
	 *Here, the first bit (LSB) represents whether cell is visited or not (1 = 00.001 in binary)
	 *and the second bit (2 = 00..010 in binary) represents whether it is active or not.
	 *The third bit represents the cell's preview mode
	*/
	int state;
	private final int VISITED = 1;
	private final int ACTIVE = 2;
	private final int PREVIEW = 4;
	
	CellState(){
		state = 0; //ie. unvisited and inactive. All bits are 0
	}
	
	public void setState(CellState src){this.state = src.state;}
	
	public boolean setState(int src){
		if((src&(~VISITED)&(~ACTIVE)&(~PREVIEW)) != 0){//ie. it is an invalid state
			return false;
		}
		state = src;
		return true;
	}
	
	public boolean isActive(){
		/***Returns status of ACTIVE bit***/
		if((state&ACTIVE) != 0)return true;
		return false;
	}
	public boolean isVisited(){
		/***Returns status of VISITED bit***/
		if((state&VISITED) != 0)return true;
		return false;
	}
	public boolean isPreview(){
		/***Returns status of PREVIEW bit***/
		if((state&PREVIEW) != 0)return true;
		return false;
	}
	
	public void makeActive(){
		/***set the active bit (ie. second least significant bit)***/
		state = (state|ACTIVE)|VISITED; //if it is active, it is also visited
	}
	public void makeInactive(){
		/***clear the active bit***/
		state = state&(~ACTIVE);
	}
	public void makeVisited(){
		/***set the visited bit (ie. LSB - least significant bit)***/
		state = state|VISITED;
	}
	public void makePreview(){
		/***set the preview bit***/
		state = state|PREVIEW;
	}
	public void makeNonPreview(){
		/***clear the preview bit***/
		state = state&(~PREVIEW);
	}
	
	/***Returns an integer representing the state. 
	 * Useful for storage/transmission***/
	public int getStateInt(){
		return state;
	}
}


/* @author campuspc
 *
 */

/***
 * This class represents the board and is designed to contain all associated algorithms. 
 * 
 * Currently, it can compute the next step when ComputeNextState() is called. getCellState(x,y)
 * returns the an object of type CellState which represents the state of the cell at coordinates
 * (x,y). The coordinate system has (0,0) at the top left and increase toward the right and bottom.
 * 
 * In the future, this class should implement methods to detect duplicates as well as methods to 
 * preview machines on the screen before placing them. It is possible to draw on the board using
 * getCellState() and modifying the returned object.
 * 
 * Also, for debugging purposes, the constructor draws a glider and a lone sqare on the screen
 * during initialization.
 ***/
public class BoardClass{
/***The class implementing the main algorithms pertaining to game management.
 * This includes computing the next iteration and different methods of board 
 * initialization, along with methods for detecting win/loss (in challenges).
***/
	CellState[][] board;
	CellState[][] tBoard;  //a temporary board, used when updating state
	int width, height;
	
	//the cell coords. (x,y) which make up the castle of the player and the opponent
	int[][] myCastleCells; 
	int[][] oppCastleCells;
	
	final int maxIterations = 1000; //the number of iterations after which a match can be declared a draw
	
	/***Called by the constructors to perform initializations that are 
	 * independent of constructor type.
	 * 
	 * @param challenge - whether it is in challenge mode or not***/
	private void constructorHelper(boolean challenge){
		//Allocate the memory
		board = new CellState[height][width];
		tBoard = new CellState[height][width];
		for(int y = 0;y < height;y++){
			for(int x = 0;x < width;x++){
				board[y][x] = new CellState(); //the constructor makes it inactive by default
				tBoard[y][x] = new CellState();
			}
		}
		
		//build the castle
		
		myCastleCells = new int[4*((height-1)/3)+4][2];
		oppCastleCells = new int[4*((height-1)/3)+4][2];
		int i = 0;
		for(int y = 1;y+1 < height-1;y+=3){
			myCastleCells[i+0][0]=1; myCastleCells[i+0][1]=y;
			myCastleCells[i+1][0]=1; myCastleCells[i+1][1]=y+1;
			myCastleCells[i+2][0]=2; myCastleCells[i+2][1]=y;
			myCastleCells[i+3][0]=2; myCastleCells[i+3][1]=y+1;
			
			oppCastleCells[i+0][0]=width-2; oppCastleCells[i+0][1]=y;
			oppCastleCells[i+1][0]=width-2; oppCastleCells[i+1][1]=y+1;
			oppCastleCells[i+2][0]=width-3; oppCastleCells[i+2][1]=y;
			oppCastleCells[i+3][0]=width-3; oppCastleCells[i+3][1]=y+1;
			i += 4;
		}
		
		if(challenge){
			int[][] tMyCastleCells = {{1+width/4,height/2}, {2+width/4,height/2}, {1+width/4,height/2+1}, {2+width/4,height/2+1}};
			int[][] tOppCastleCells = {{width-2-width/4,height/2}, {width-3-width/4,height/2}, {width-2-width/4,height/2+1}, {width-3-width/4,height/2+1}};
			
			for(int j = 0;j < tMyCastleCells.length;j++){
				myCastleCells[i+j] = tMyCastleCells[j];
				oppCastleCells[i+j] = tOppCastleCells[j];
			}
		}
		else{
			int[][] tMyCastleCells = {{1+(width/4)*2,height/2}, {2+(width/4)*2,height/2}, {1+(width/4)*2,height/2+1}, {2+(width/4)*2,height/2+1}};
			int[][] tOppCastleCells = {{width-2-(width/4)*2,height/2}, {width-3-(width/4)*2,height/2}, {width-2-(width/4)*2,height/2+1}, {width-3-(width/4)*2,height/2+1}};
			
			for(int j = 0;j < tMyCastleCells.length;j++){
				myCastleCells[i+j] = tMyCastleCells[j];
				oppCastleCells[i+j] = tOppCastleCells[j];
			}
		}
	}
	
	/***Initialize empty board given width and height denoting number of columns and rows
	 * of cells respectively.***/
	BoardClass(int cWidth, int cHeight){
		if(cWidth <= 0 || cHeight <= 0){
			throw new IllegalArgumentException("Width and height specified should be positive.");
		}
		
		width = cWidth+2;height = cHeight+2;//+2 to handle boundaries, so getCellState can return (x+1, y+1) to compensate
		
		constructorHelper(false);
	}
	
	/***Constructor that copies data from another board
	 * 
	 * @param src - the board to copy from
	 * @param challenge - whether or not this is a challenge board***/
	public BoardClass(BoardClass src, boolean challenge){
		width = src.width; height = src.height;
		
		constructorHelper(challenge);
		
		//initialize the board with copied values
		for(int y = 0;y < height;y++){
			for(int x = 0;x < width;x++){
				board[y][x].setState(src.board[y][x]);
			}
		}
	}
	
	/***Constructs a board representing a match.
	 * 
	 * The strings are the ones generated using BoardClass::convertToString()***/
	public BoardClass(String myBoardStr, String oppBoardStr){
		byte[] myBoard = myBoardStr.getBytes();
		byte[] oppBoard = oppBoardStr.getBytes();
		
		//check if both dimensions are same
		if(myBoard[0] != oppBoard[0] || myBoard[1] != oppBoard[1])
			throw new IllegalArgumentException("BoardClass:BoardClass(String, String) :- Error: The dimensions of the two boards do not match");
		//check if strings are as long as they claim to be
		if(myBoard.length != myBoard[0]*myBoard[1]+2)
			throw new IllegalArgumentException("BoardClass:BoardClass(String, String) :- Error: The length of the home-board string does not match the claimed dimensions");
		if(oppBoard.length != oppBoard[0]*oppBoard[1]+2)
			throw new IllegalArgumentException("BoardClass:BoardClass(String, String) :- Error: The length of the home-board string does not match the claimed dimensions");
		
		
		width = (int)myBoard[0]*2 + 2;
		height = (int)myBoard[1] + 2;
		
		constructorHelper(true);
		
		//copy the data
		int i = 2;
		for(int y = 1;y < height-1;y++){
			for(int x = 1;x < (width-2)/2+1;x++){
				if(!board[y][x].setState(myBoard[i])) //if it is not a legal value
					throw new IllegalArgumentException("BoardClass:BoardClass(String, String) :- Error: Illegal character in home board string (\'"+myBoard[i]+"\') at position "+i);
				if(!board[y][width-x-1].setState(oppBoard[i])) //if it is not a legal value
					throw new IllegalArgumentException("BoardClass:BoardClass(String, String) :- Error: Illegal character in opponent board string(\'"+oppBoard[i]+"\') at position "+i);
				i++;
			}
		}
	}
	
	 /***Constructs board from string generated by BoardClass::convertToString(). Currently unused***/
	/*public BoardClass(String str){
		byte[] byteStr = str.getBytes();
		
		width = (int)byteStr[0]+2; //+2 for the padding
		height = (int)byteStr[1]+2;
		
		//initialize the board to create an empty one
		board = new CellState[height][width];
		tBoard = new CellState[height][width];
		for(int y = 0;y < height;y++){
			for(int x = 0;x < width;x++){
				board[y][x] = new CellState(); //the constructor makes it inactive by default
				tBoard[y][x] = new CellState();
			}
		}
		
		int i = 2;
		if(byteStr.length-2 != (width-2)*(height-2)){
			System.err.println("BoardClass:convertFromString :- Error: The length of string given does not match the dimensions of the board");
			return; //wrong size of string
		}
		for(int y = 1;y < height-1;y++){
			for(int x = 1;x < width-1;x++){
				if(!board[y][x].setState(byteStr[i])){
					System.err.println("BoardClass:convertFromString :- Error: The given string has an illegal value at position "+i);
				}
				i++;
			}
		}
	}*/
	
	/***Makes the castle by making the relevant cells active***/
	public void makeCastle(){
		for(int i = 0;i < myCastleCells.length;i++){
			board[myCastleCells[i][1]][myCastleCells[i][0]].makeActive();
		}
	}
	
	/***Returns 1, 0 or -1 depending on whether the user has won, is neutral (for now) or has lost
	 * 
	 * It assumes the board is set up as a match rather than a design board.***/
	private int findMatchVictoryState(){
		for(int i = 0;i < myCastleCells.length;i++){
			if(!(board[oppCastleCells[i][1]][oppCastleCells[i][0]].isActive())){
				return 1; //the user has won as their castle is disturbed
			}
			if(!(board[myCastleCells[i][1]][myCastleCells[i][0]].isActive())){
				return -1; //the user has lost as their castle is disturbed
			}
		}
		return 0; //neither castle has been disturbed yet
	}
	
	/***Simulates an entire match and returns the result.
	 * 
	 * If neither player's castle is destroyed in 1000 iterations, the match
	 * is declared a draw. It assumes the board is set up as a match and
	 * not as a design board***/
	public int runMatchAndFindResult(){
		BoardClass temp = new BoardClass(this, true);
		for(int i = 0;i < maxIterations;i++){
			temp.ComputeNextState();
			if(temp.findMatchVictoryState() != 0){
				return temp.findMatchVictoryState(); //one of them has won
			}
		}
		return 0; //it is a draw
	}
	
	/***Computes the next iteration of the board and stores it back into the board.
	 * 
	 * Method of dealing with boundary cells: Does not modify them
	 * 
	 * Known Error in Function:
	 * When one of the boundary cells is active, it is active only in one of board or tBoard.
	 * Since the boundary cells are not copied, this causes flickering of cells. Although,
	 * an easy fix is available to the problem, a better fix would be to simply not draw the
	 * boundary cells and to ignore them everywhere. This is adopted***/
	public void ComputeNextState(){
		int tScore; //keep track of number of active neighbors
		
		/***Dealing with boundary cells
		 * As there is no rule specifying what to do with boundary 
		 * cells (which do not have all neighbors), this implementation simply 
		 * refuses to update them***/
		
		//Update state of each cell one by one ignoring the boundary cases
		for(int y = 1;y < height-1;y++){  //always a good idea to visit a 2D array row wise for efficiency purposes
			for(int x = 1;x < width-1;x++){
				tScore = 0;
				//visit the neighbors to find number of active neighbors
				for(int dy = -1;dy <= 1;dy++){
					for(int dx = -1;dx <= 1;dx++){
						if(board[y+dy][x+dx].isActive()){
							++tScore;
						}
					}
				}
				
				//In above loop, we counted the cell itself, which we are not supposed to.
				//So the following compensates for it.
				if(board[y][x].isActive())--tScore;
				//Change the state of the current cell based on score
				//Here, we modify only tBoard as modifying board will make the modifications of other cells innacurate
				//after modifying tBoard, we simply swap board and tBoard;
				tBoard[y][x].setState(board[y][x]);//simply sets tBoard[y][x] to board[y][x]
				if(tScore < 2 || tScore > 3) //cell dies
					tBoard[y][x].makeInactive();
				else if(tScore == 3)  //cell is born
					tBoard[y][x].makeActive();
			}
		}
		CellState[][] tSwap = tBoard; //swapping board and tBoard
		tBoard = board;
		board = tSwap;
	}
	
	/***Returns the CellState object of cell at position (x,y). This can be read/modified
	 * as required. 
	 * 
	 * Though this does open a Pandora's box, the only place where the cells
	 * are modified outside of this class as of(19/9/2014) is in BoardCanvas to support
	 * mouse-editing of the board.***/
	public CellState getCellState(int x, int y){
		return board[y+1][x+1];
	}
	
	/***Returns a string representing the board that can be transmitted/stored***/
	public String convertToString(){
		byte[] byteStr = new byte[(width-2)*(height-2)+2];
		byteStr[0] = (byte)(width-2); //add the dimension info
		byteStr[1] = (byte)(height-2);
		int i = 2;
		for(int y = 1;y < height-1;y++){
			for(int x = 1;x < width-1;x++){
				byteStr[i] = (byte)board[y][x].getStateInt();
				
				//will cause problem by initiating a new line/interfering with string formatting
				if(byteStr[i] == '\n' || byteStr[i] == ':' || byteStr[i] == ';'){
					System.err.println("BoardClass:convertToString :- ERROR/WARNING: Encountered "+byteStr[i]+"in string. Will cause problems later on.");
				}
				i++;
			}
		}
		return new String(byteStr);
	}
	
	public int getWidth(){return width-2;} //the width and height as seen by the calling class is smaller.
	public int getHeight(){return height-2;}//this is just an internal adjustment
}