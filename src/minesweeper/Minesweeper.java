package minesweeper;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;

public class Minesweeper implements ActionListener{
	
	private JFrame mainFrame;
	
	public int[][] pos;
	
	public int gridSizeX;
	public int gridSizeY;
	
	private MouseListener[][] ml;
	
	public JToggleButton[][] gridButton;
	public boolean[][] bombGrid;
	
	private JLabel NBomb;
	private JLabel NFlag;
	public static int nBomb;
	public int nFlag;
	
	private JLabel status;
	
	public static void main (String args[]){
		new Minesweeper();
	}
	
	public Minesweeper() {
		int[][] posTemp = {{-1,-1} //top left
		,{-1, 0} //top center
		,{-1, 1} //top right
		,{0, -1} //middle left
		,{0, 1} //middle right
		,{1, -1} //bottom left
		,{1, 0} //bottom center
		,{1, 1}}; //bottom right
		
		pos = posTemp;
		
		resetGame();
	}
	
	//gets game parameters from console
	private void gridSizeDialog(){
		BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));

		try{
			//get size of grid from console
			System.out.print("Enter x-size: ");
			gridSizeX = Integer.parseInt(bReader.readLine());
			System.out.print("Enter y-size: ");
			gridSizeY = Integer.parseInt(bReader.readLine());

			//get number of bombs from console
			System.out.print("Number of Bombs: ");
			nBomb = Integer.parseInt(bReader.readLine());
			nFlag = nBomb;
			
		} catch (IOException e){
			System.out.println("Number not valid");
		}
		
		//initialize grids
		gridButton = new JToggleButton[gridSizeY][gridSizeX];
		bombGrid = new boolean[gridSizeY][gridSizeX];
		ml = new preML[gridSizeY][gridSizeX];
	}
	
	//randomly generate bomb locations
	private void generateBombs(int yo, int xo){
		for (int i = 0; i < nBomb; i++){
			int x;
			int y;
			do {
				x = (int)(Math.random()*gridSizeX);
				y = (int)(Math.random()*gridSizeY);
				
			//reroll if
			} while (bombGrid[y][x] //bomb already in randomly selected location
					|| (x > xo - 2 && x < xo + 2) //within 1 x-unit of the first click
					&& (y > yo - 2 && y < yo + 2)); //within 1 y-unit of the first click 
			bombGrid[y][x] = true;
		}
	}
	
	//create the GUI
	public void createView(){
		
		mainFrame = new JFrame();
		mainFrame.setPreferredSize(new Dimension(1000, 1000));
		mainFrame.setTitle("Minesweeper AI");
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		
		JPanel gameBoard = new JPanel(new GridLayout(gridSizeY, gridSizeX));
		
		//create buttons
		for (int i = 0; i < gridSizeY; i++){
			for (int j = 0; j < gridSizeX; j++){
				gridButton[i][j] = new JToggleButton();
				gridButton[i][j].setPreferredSize(new Dimension(50,50));
				gridButton[i][j].setFont(new Font("Consolas", Font.PLAIN, 100));
				
				//remove default listeners
				MouseListener[] mouseListeners = gridButton[i][j].getMouseListeners();
				for (int k = 0; k < mouseListeners.length; k++)
					gridButton[i][j].removeMouseListener(mouseListeners[k]);
				
				//set first-move mouse listener
				ml[i][j] = new preML(i, j);
				gridButton[i][j].addMouseListener(ml[i][j]);
				gameBoard.add(gridButton[i][j]);
			}
		}
		
		//panel containing information labels
		JPanel label = new JPanel();
		
		NBomb = new JLabel("Number of Bombs: " + nBomb);
		NFlag = new JLabel("Flags Left: " + nFlag);
		
		label.add(NBomb);
		label.add(NFlag);
		
		//panel containing utility buttons
		JPanel bottom = new JPanel();
		
		status = new JLabel("Playing");
		
		JButton restart = new JButton ("Restart");
		restart.addActionListener(this);
		restart.setActionCommand("restart");
		
		JButton ai = new JButton("AI Solve");
		ai.addActionListener(this);
		ai.setActionCommand("ai");
		
		JButton reset = new JButton("Reset");
		reset.addActionListener(this);
		reset.setActionCommand("reset");
		
		bottom.add(status);
		bottom.add(restart);
		bottom.add(ai);
		bottom.add(reset);
		
		mainPanel.add(label, BorderLayout.NORTH);
		
		mainPanel.add(gameBoard, BorderLayout.CENTER);
		
		mainPanel.add(bottom, BorderLayout.SOUTH);
		
		mainFrame.add(mainPanel);
		
		mainFrame.pack();
		mainFrame.setVisible(true);
	}
	
	//opens cells and adjacent cells if no bombs in proximity
	public int clearCells(int y, int x, boolean ifAI, int count){
		
		//for AI
		//if AI selected bomb
		if (ifAI && bombGrid[y][x]) return -1;
		
		//increase the number of recorded tiles opened
		count++;
		
		int nAdjBomb = 0;
		
		//disable button
		gridButton[y][x].setEnabled(false);
		gridButton[y][x].setSelected(false);
		
		//check adjacent cells if they exist for bombs
		for (int i = 0; i < pos.length; i++){
			try {
				if (bombGrid[y + pos[i][0]][x + pos[i][1]]){
					nAdjBomb++;
				}
			} catch (ArrayIndexOutOfBoundsException e){}
		}
		
		//if no bombs, open adjacent cells
		if (nAdjBomb == 0){
			for (int i = 0; i < pos.length; i++){
				try {
					if (gridButton[y + pos[i][0]][x + pos[i][1]].isEnabled())
						count = clearCells(y + pos[i][0], x + pos[i][1], ifAI, count);
				} catch (ArrayIndexOutOfBoundsException e){}
			}
		} else { //if adjacent bombs
			//show user adjacent bomb number
			gridButton[y][x].setText("" + nAdjBomb);
		}
		
		return count;
	}
	
	//losing game routines
	public void loseGame(){
		//disable all buttons
		for (int i = 0; i < gridSizeY; i++){
			for (int j = 0; j < gridSizeX; j++){
				gridButton[i][j].setEnabled(false);
			}
		}
		
		//show bomb locations
		for (int i = 0; i < gridSizeY; i++)
			for (int j = 0; j < gridSizeX; j++)
				if (bombGrid[i][j])
					gridButton[i][j].setIcon(createImageIcon("/resources/bomb.png", "bomb"));
		
		//tell user they lost
		status.setText("Lose");
		status.setForeground(Color.RED);
	}
	
	//check if the user won
	public boolean winCheck(){
		
		//count the remaining cells
		int counter = 0;
		for (int i = 0; i < gridSizeY; i++)
			for (int j = 0; j < gridSizeX; j++){
				if (gridButton[i][j].isEnabled())
					counter++;
			}
		
		//if number of cells = number of bombs, WIN
		if (counter == nBomb)
			return true;
		return false;
	}
	
	public void flag (int y, int x){
		if (nFlag > 0){
			//add flag
			gridButton[y][x].setSelected(true);
			gridButton[y][x].setIcon(createImageIcon("/resources/flag.png", "flag"));
			
			//add to flag count
			nFlag--;
			NFlag.setText("Flags Left: " + nFlag);
		}
	}
	
	//win game routines
	public void winGame(){
		//disable all buttons
		for (int i = 0; i < gridSizeY; i++){
			for (int j = 0; j < gridSizeX; j++){
				gridButton[i][j].setEnabled(false);
			}
		}
		//tell user they won
		status.setText("Win");
		status.setForeground(Color.GREEN);
	}
	
	//restart game with same initial parameters
	public void restartGame(){

		recordLearnData();

		System.out.println("Restarting game");

		AI.nGame++;
		
		//reset all buttons & listeners
		for (int i = 0; i < gridSizeY; i++)
			for (int j = 0; j < gridSizeX; j++){
				
				//reset bombs
				bombGrid[i][j] = false;
				
				//reset all buttons
				gridButton[i][j].setEnabled(true);
				gridButton[i][j].setSelected(false);
				
				//reset button labels/text
				gridButton[i][j].setText("");
				gridButton[i][j].setIcon(null);
				
				//remove all existing listeners
				MouseListener[] mListener = gridButton[i][j].getMouseListeners();
				for (int a = 0; a < mListener.length; a++)
					gridButton[i][j].removeMouseListener(mListener[a]);
				
				//assign first-move listener
				ml[i][j] = new preML(i, j);
				gridButton[i][j].addMouseListener(ml[i][j]);
			}
		
		//reset flag count
		nFlag = nBomb;
		NFlag.setText("Flags Left: " + nFlag);
		
		//reset playing label
		status.setText("Playing");
		status.setForeground(Color.BLACK);
	}

	private int countCorrectFlags(){
		int count = 0;
		for (int i = 0; i < gridSizeY; i++){
			for (int j = 0; j < gridSizeX; j++){
				try {
					if (gridButton[i][j].isSelected() && bombGrid[i][j]){
						count++;
					}
				} catch (IndexOutOfBoundsException e){
					//do nothing
				}
			}
		}

		return count;
	}

	public void recordLearnData(){
		try {
			File file = new File("data/" + gridSizeY + "_" + gridSizeX + "learn.csv");

			PrintWriter writer = new PrintWriter(new FileWriter(file, true));

			String info = AI.nGame + "," + countCorrectFlags() + "\n";

			writer.append (info);
			writer.close();

		} catch (FileNotFoundException e){
			//do nothing
		} catch (IOException e){
			//do nothing
		}
	}
	
	//start new game with new parameters
	public void resetGame(){
		//delete main frame
		if (mainFrame != null){
			mainFrame.dispose();
		}
		//ask for new parameters
		gridSizeDialog();
		//create new frame
		createView();
	}
	
	//listen to utility button presses
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("restart")){ //restart game
			restartGame();
		} else if (e.getActionCommand().equals("reset")){ //reset game
			resetGame();
		} else { //AI solve
			new AI(this);
		}
		
	}
	
	public int[] firstClick (int y, int x, boolean ifAI){
		//generate bombs
		generateBombs(y, x);
		
		//clear adjacent cells
		int count = clearCells(y, x, ifAI, 0);
		
		//set main button listener
		for (int i = 0; i < gridSizeY; i++){
			for (int j = 0; j < gridSizeX; j++){
				gridButton[i][j].removeMouseListener(ml[i][j]);
				gridButton[i][j].addMouseListener(new ML(i,j));
			}
		}
		
		int[] toSend = {count, y, x};
			return toSend;
	}
	
	//listen to mouse clicks
	public class ML implements MouseListener{
		
		int x;
		int y;
		
		public ML(int y, int x){
			//get clicked button location
			this.y = y;
			this.x = x;
		}
		
		@Override
		public void mouseClicked(MouseEvent e){
			//right click button
			if (SwingUtilities.isRightMouseButton(e)
					&& gridButton[y][x].isEnabled()){
				
				//toggle if selected/flagged or not
				if (!gridButton[y][x].isSelected()){
					flag (y, x);
				}
				else {
					//remove flag
					gridButton[y][x].setSelected(false);
					gridButton[y][x].setIcon(null);
					
					//remove from flag count
					nFlag++;
					NFlag.setText("Flags Left: " + nFlag);
				}
				
			//clearing cells
			} else if (gridButton[y][x].isEnabled() && !gridButton[y][x].isSelected()){
				
				//if bomb not selected, clear cells
				if (!bombGrid[y][x]){
				gridButton[y][x].setEnabled(false);
				clearCells(y, x, false, 0);
				
				//check for game win
				if (winCheck())
					winGame();
				
				} else { //bomb selected, lose game
				loseGame();
				}
			} 
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {}
	}
	
	//first move mouse listener
	public class preML implements MouseListener{
		
		int x;
		int y;
		
		public preML(int y, int x){
			//get clicked button location
			this.y = y;
			this.x = x;
		}
		
		@Override
		public void mouseClicked(MouseEvent e){
			firstClick(y, x, false);
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {}
	}
	
	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}
}
