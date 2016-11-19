package minesweeper;

public class firstMoveData {
	private String gameNumber;
	private String locY;
	private String locX;
	private String cellsOpened;
	
	public firstMoveData(){
		//default no args constructor
	}
	
    public firstMoveData(int gameNumber, int locY, int locX, int cellsOpened) {
        this.gameNumber = "" + gameNumber;
        this.locY = "" + locY;
        this.locX = "" + locX;
        this.cellsOpened = "" + cellsOpened;
    }
    
 // getters and setters

    @Override
    public String toString() {
        return "Course{" +
                "gameNumber=" + gameNumber +
                ", locY='" + locY + '\'' +
                ", locX='" + locX + '\'' +
                ", cellsOpened='" + cellsOpened + '\'' +
                '}';
    }

	public String getGameNumber() {
		return gameNumber;
	}

	public void setGameNumber(String gameNumber) {
		this.gameNumber = gameNumber;
	}

	public String getCellsOpened() {
		return cellsOpened;
	}

	public void setCellsOpened(String cellsOpened) {
		this.cellsOpened = cellsOpened;
	}

	public String getLocY() {
		return locY;
	}

	public void setLocY(String locY) {
		this.locY = locY;
	}

	public String getLocX() {
		return locX;
	}

	public void setLocX(String locX) {
		this.locX = locX;
	}
    
	public int compareTo(firstMoveData data) {
		
		String compareQuantity = data.getCellsOpened(); 
		
		return Integer.valueOf(this.cellsOpened) - Integer.valueOf(compareQuantity);
		
	}	
    
}
