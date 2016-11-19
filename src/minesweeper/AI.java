/*
Author: Dylan Dao
Last Updated: July 15, 2016
Description: AI that uses reinforced machine learning to learn to play Minesweeper
 */

package minesweeper;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

class AI {
	
	private Minesweeper CurrentGame;
	
	//keep track of theories
	private static ArrayList<Theory> TheoryList;
	//holds rules with certainty
	private static ArrayList<Theory> RuleList;
	//holds theories that have a chance to work
	private static ArrayList<Theory> ProbabilityList;
	//holds theories that always fail
	private static ArrayList<Theory> ZeroList;
	
	//size of board
	private int xSize;
	private int ySize;

	//hold first move coordinates after optimization
	private static int firstX = -1;
	private static int firstY = -1;

	//hold number of games that have passed
	static int nGame = 0;
	
	//compare/sort theories
	private Comparator<Theory> theoryCmp;

	private int gamesWon = 0;

	//IMPORTANT FOR DATA RELIABILITY SCALING
	//number of games won before completing performance data set
	private int maxGamesWon = 100;
	//number of times to test each cell during first move analysis
	private int firstMoveRepeat = 10;
	
	AI(Minesweeper currentGame) {

		//set minesweeper game instance as a global variable
		CurrentGame = currentGame;

		//create theory comparator, uses compare method in Theory class
		theoryCmp = Theory::compareTo;

		//get grid size
		xSize = CurrentGame.gridSizeX;
		ySize = CurrentGame.gridSizeY;

		//user tells AI how many games to win before stopping
		try {
			BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Enter number of games to win: ");
			maxGamesWon = Integer.parseInt(bReader.readLine());
		} catch (IOException e){
			System.out.println("Failed to get number of games to win");
		}

		//if first move
		if (firstX == -1) {

			//delete existing first move data csv
			File file = new File("data/" + ySize + "_" + xSize + "firstMoveData.csv");
			file.delete();

			file = new File("data/" + ySize + "_" + xSize + "learn.csv");
			file.delete();

			//do first move optimization analysis
			firstMoveAnalysis();
			//delete existing first move data csv
			System.out.println ("First Move: " + firstY + " " + firstX);
		}

		//while games won < max number of games that should be won before learning ends
		while (gamesWon < maxGamesWon) {
			//while game is not won
			do {

				CurrentGame.restartGame();
				CurrentGame.firstClick(firstY, firstX, true);
				fringeScan();

				System.out.println("Theories left: " + TheoryList.size());
				File theories = new File("data/" + ySize + "_" + xSize + "theories.txt");
				try {
					PrintWriter writer = new PrintWriter(theories);
					writer.write(TheoryList.toString());
					writer.close();
				} catch (FileNotFoundException e) {
					System.out.println("ERROR: Creating theories.txt");
				}

			} while (getUndetermined() != 0);

			//for last data point
			if (gamesWon == maxGamesWon) {
				CurrentGame.restartGame();
			}
			gamesWon++;

			//GAME STATS
			System.out.println("Games: " + nGame);
			System.out.println("Games won: " + gamesWon);
		}

	}

	private static CellProcessor[] getFirstMoveProcessors() {

		return new CellProcessor[] {

				new NotNull(), // gameNumber
				new NotNull(), // locY
				new NotNull(), // locX
				new NotNull()  // cellsOpened
		};
	}
	
	private void fringeScan(){

		//number of times to test theory before making it a rule
		int nTest = 10;

		//if lists do not exist, create them

		if (TheoryList == null) TheoryList = new ArrayList<>();

		if (RuleList == null) RuleList = new ArrayList<>();

		if (ProbabilityList == null) ProbabilityList = new ArrayList<>();

		if (ZeroList == null) ZeroList = new ArrayList<>();

		//keep track if a scan has been completed with no rules
		boolean secondScan = false;

		//keep track of whether or not to restart the loop
		boolean restart = true;

		do {
			//scan for numbers

			//enable breaking out of loop
			outerloop:
			for (int i = 0; i < ySize; i++) {
				for (int j = 0; j < xSize; j++) {
					restart = true;
					
					if (!CurrentGame.gridButton[i][j].getText().equals("")) {

						//identify subset
						ArrayList<Set> combinedSet = createSet(i, j, true, 0, 0, new ArrayList<>(), new ArrayList<>());

						//sort by set
						TheoryList.sort(theoryCmp);

						//cast combinedSet as theory to compare
						Theory tCombinedSet = new Theory(combinedSet);

						//if there is an associated theory
						if (Collections.binarySearch(TheoryList, tCombinedSet, theoryCmp) >= 0) {

							//get matched theory
							Theory record = TheoryList.get(Collections.binarySearch(TheoryList, tCombinedSet, theoryCmp));

							//update number of times theory was tested
							int newTime = record.getTested() + 1;

							//numbers required to calculate new success rate
							double successRate = record.getSuccessRate();
							double num = 0;

							System.out.println("testing theory " + record.toString());

							//test theory and calculate new success rate
							boolean r = false;

							if (!testTheory(i, j, TheoryList.get(Collections.binarySearch(TheoryList, tCombinedSet, theoryCmp)))) {
								//update success rate with 1 failure
								num = record.getTested() * successRate;

								//if game is lost, reset game and fringe scan back to beginning

								r = true;

								System.out.println("Theory failed");

							} else {
								//update success rate with 1 success
								num = record.getTested() * successRate + 1;
								System.out.println("Theory succeeded");

							}

							double newSuccessRate = num / newTime;

							//update record with new success rate
							record.setSuccessRate(newSuccessRate);

							record.setTested(newTime);

							System.out.println(record.toString());

							//if theory was tested nTest times, add to ruleset

							//if no success after nTest times, add to zerolist
							if (newTime == nTest && newSuccessRate == 0) {

								//remove from theory list
								TheoryList.remove(Collections.binarySearch(TheoryList, tCombinedSet, theoryCmp));

								ZeroList.add(record);

								ZeroList.sort(theoryCmp);

								File zeroes = new File ("data/" + ySize + "_" + xSize + "zeroes.txt");
								try {
									PrintWriter writer = new PrintWriter(zeroes);
									writer.write(ZeroList.toString());
									writer.close();
								} catch (FileNotFoundException e){
									System.out.println("ERROR: zeroes.txt cannot be created");
								}
							//if 100% success
							} else if (newTime == nTest && newSuccessRate == 1) {
								//remove from theorylist
								TheoryList.remove(Collections.binarySearch(TheoryList, tCombinedSet, theoryCmp));

								//check if rule can be found in probability list
								if (Collections.binarySearch(RuleList, tCombinedSet, theoryCmp) < 0) {
									RuleList.add(record);
								} else {
									Theory compareRecord = RuleList.get(Collections.binarySearch(RuleList, tCombinedSet, theoryCmp));

									//sum number of flags
									int recordSum = 0;
									int compareRecordSum = 0;
									for (int q = 0; q < record.getToFlag().size(); q++) {
										recordSum += record.getToFlag().get(q)[1];
										compareRecordSum += compareRecord.getToFlag().get(q)[1];
									}

									//if less flags are cleared with rule, replace rule
									if (recordSum < compareRecordSum) {
										RuleList.set(Collections.binarySearch(RuleList, tCombinedSet, theoryCmp), record);
									}
								}

								//sort rule list
								RuleList.sort(theoryCmp);

								//print rule list
								File rules = new File("data/" + ySize + "_" + xSize +"rules.txt");
								try {
									PrintWriter writer = new PrintWriter(rules);
									writer.write(RuleList.toString());
									writer.close();
								} catch (FileNotFoundException e) {
									System.out.println("ERROR: rules.txt cannot be created");
								}

							} else if (newTime == nTest) {
								//remove from theorylist
								TheoryList.remove(Collections.binarySearch(TheoryList, tCombinedSet, theoryCmp));

								//if more mines are cleared
								if (Collections.binarySearch(ProbabilityList, tCombinedSet, theoryCmp) < 0) {
									ProbabilityList.add(record);
								} else {
									Theory compareRecord = ProbabilityList.get(Collections.binarySearch(ProbabilityList, tCombinedSet, theoryCmp));

									//sum number of flags
									int recordSum = 0;
									int compareRecordSum = 0;
									for (int q = 0; q < record.getToFlag().size(); q++) {
										recordSum += record.getToFlag().get(q)[1];
										compareRecordSum += compareRecord.getToFlag().get(q)[1];
									}

									//if less flags are created, replace possibility
									if (recordSum < compareRecordSum) {
										ProbabilityList.set(Collections.binarySearch(ProbabilityList, tCombinedSet, theoryCmp), record);
									}
								}

								ProbabilityList.sort(theoryCmp);

								//print probability list
								File rules = new File("data/" + ySize + "_" + xSize + "prob.txt");
								try {
									PrintWriter writer = new PrintWriter(rules);
									writer.write(ProbabilityList.toString());
									writer.close();
								} catch (FileNotFoundException e) {
									System.out.println("ERROR: prob.txt cannot be created");
								}

							} else {
								//update theory with new success rate, times tested
								TheoryList.set(Collections.binarySearch(TheoryList, tCombinedSet, theoryCmp), record);
							}

							//if theory failed/game lost
							if (r){

								CurrentGame.restartGame();

								CurrentGame.firstClick(firstY, firstX, true);

								//reset board scan
								break outerloop;
							}

						//if identified subset has an associated probability
						} else if (Collections.binarySearch(RuleList, tCombinedSet, theoryCmp) >= 0) {
							if (!secondScan && !(Collections.binarySearch(ProbabilityList, tCombinedSet, theoryCmp) > 0)) {
								System.out.println("Using rule");
								//System.out.println(RuleList.get(Collections.binarySearch(RuleList, tCombinedSet, theoryCmp)).toString());

								Theory record = RuleList.get(Collections.binarySearch(RuleList, tCombinedSet, theoryCmp));

								System.out.println(record.toString());

								//~System.out.println(record.toString());

								if (!testTheory(i, j, record)){
									System.out.println("rule failed, remove from list");
									RuleList.remove(Collections.binarySearch(RuleList, tCombinedSet, theoryCmp));
								}

							} else if (Collections.binarySearch(ProbabilityList, tCombinedSet, theoryCmp) > 0) {
								Theory record = ProbabilityList.get(Collections.binarySearch(ProbabilityList, tCombinedSet, theoryCmp));
								RuleList.remove(Collections.binarySearch(RuleList, record, theoryCmp));
							}

						//if identified subset has an associated rule
						} else if (Collections.binarySearch(ProbabilityList, tCombinedSet, theoryCmp) >= 0) {
							if (secondScan) {
								//~System.out.println(ProbabilityList.toString());
								System.out.println("Using probability");

								Theory record = ProbabilityList.get(Collections.binarySearch(ProbabilityList, tCombinedSet, theoryCmp));

								System.out.println(record);

								if (!testTheory(i, j, record)) {
									System.out.println("Probability failed");
									CurrentGame.restartGame();

									CurrentGame.firstClick(firstY, firstX, true);

									secondScan = false;

									break outerloop;
								} else {
									secondScan = false;
								}
							}
						//if identified subset has associated zero
						} else if (Collections.binarySearch(ZeroList, tCombinedSet, theoryCmp) >= 0) {
							//System.out.println("zero at: " + i + ", " + j);
							//System.out.println("Using zero");
							//System.out.println(ZeroList.get(Collections.binarySearch(ZeroList, tCombinedSet, theoryCmp)).toString());
							//do nothing

						} else { //if not found
							//generate theories
							System.out.println("Generating theories");
							TheoryList.addAll(generateTheories(combinedSet));
						}
					}

					//if board has been scanned and board has only been scanned once
					if (i == (ySize - 1) && j == (xSize - 1) && !secondScan) {
						//repeat scan
						secondScan = true;
						break outerloop;
					//if second scan
					} else if (i == (ySize - 1) && j == (xSize - 1)){
						//end game
						restart = false;
					}
				}
			}

		} while (restart);
	}

	private boolean testTheory (int y, int x, Theory theory){

		//combined set theory handling

		//keep track of numbers that have been found by coordinate
		ArrayList <Integer[]> numberCoord = new ArrayList<>();

		ArrayList <Integer> uniqueID = new ArrayList<>();

		uniqueID.add((y*ySize) + x);

		//scan around the number for undetermined cells
		for (int i = 0; i < 8; i++){
			int yNew = y + CurrentGame.pos[i][0];
			int xNew = x + CurrentGame.pos[i][1];

			//if undetermined and not selected
			try {
				if (CurrentGame.gridButton[yNew][xNew].isEnabled()) {

						//scan around undetermined for numbers
					for (int j = 0; j < 8; j++) {
						int nyNew = yNew + CurrentGame.pos[j][0];
						int nxNew = xNew + CurrentGame.pos[j][1];

						try {
							//if there is a number in the vicinity that isn't the base number and not already found
							if (!CurrentGame.gridButton[nyNew][nxNew].getText().equals("") //a number
									&& !uniqueID.contains((nyNew * ySize) + nxNew)) { //not already found

								//create coordinate
								Integer[] coord = {nyNew, nxNew};

								//add coordinate of number to list
								numberCoord.add(coord);
								uniqueID.add((nyNew * ySize) + nxNew);
							}
						} catch (IndexOutOfBoundsException e){
							//do nothing
						}
					}
				}
			} catch (IndexOutOfBoundsException e){
				//do nothing
			}
		}

		//keep track of matches
		ArrayList <Integer[]> matchList = new ArrayList<>();

		//scan around numbers for matches

		//loop through number list
		for (Integer[] coord : numberCoord){

			ArrayList <Integer> match = new ArrayList<>();

			//get number specified by coordinate
			int baseNumber = Integer.parseInt(CurrentGame.gridButton[coord[0]][coord[1]].getText());

			//first number in match is the base number
			match.add(baseNumber);

			for (int a = 0; a < 8; a++){
				for (int b = 0; b < 8; b++){
					try {
						if (y + CurrentGame.pos[a][0] == (coord[0] + CurrentGame.pos[b][0])//y position matches
								&& x + CurrentGame.pos[a][1] == (coord[1] + CurrentGame.pos[b][1])//x position matches
								&& CurrentGame.gridButton[y + CurrentGame.pos[a][0]][x + CurrentGame.pos[a][1]].isEnabled()//undetermined
								&& !CurrentGame.gridButton[y + CurrentGame.pos[a][0]][x + CurrentGame.pos[a][1]].isSelected()
								) {//not flagged

							//add relative location to match
							match.add(a);
						}
					} catch (IndexOutOfBoundsException e){
						//do nothing
					}
				}
			}

			//add to match list
			matchList.add(match.toArray(new Integer[match.size()]));
		}

		ArrayList <Integer> shared = new ArrayList<>();

		//find all cells that are shared
		for (Integer[] matchArray : matchList)
			for (Integer match : matchArray)
				if (!shared.contains(match))
					shared.add(match);

			ArrayList <Integer> notShared = new ArrayList<>();

			//0 to represent base
			notShared.add(0);

			//if not shared, add to notShared
			for (int e = 0 ; e < 8; e++){
				try {
					if (!shared.contains(e)
							&& CurrentGame.gridButton[y + CurrentGame.pos[e][0]][x + CurrentGame.pos[e][1]].isEnabled()
							&& !CurrentGame.gridButton[y + CurrentGame.pos[e][0]][x + CurrentGame.pos[e][1]].isSelected()) {
						notShared.add(e);
					}
				} catch (IndexOutOfBoundsException a){
					//do nothing
				}
			}

			//sort the current matches by mine number and then by length (smallest first)
			matchList.sort((arg0, arg1) -> {
				if (!arg0[0].equals(arg1[0]))
					return arg0[0] - arg1[0];
				else
					return arg0.length - arg1.length;
			});

			//prevent original cell from being located
			Integer[] baseCoord = {y, x};

			numberCoord.add(0, baseCoord);

			//add to the matchlist at index 0
			matchList.add(0, notShared.toArray(new Integer[notShared.size()]));

			//flag all specified by theory's toFlag
			for (int i = 0; i < matchList.size(); i++) {
				int restriction = theory.getToFlag().get(i)[1];
				for (int j = 1; j < matchList.get(i).length && j <= restriction; j++) {

					if (!CurrentGame.gridButton[y + CurrentGame.pos[matchList.get(i)[j]][0]][x + CurrentGame.pos[matchList.get(i)[j]][1]].isSelected()) {
						CurrentGame.flag(y + CurrentGame.pos[matchList.get(i)[j]][0], x + CurrentGame.pos[matchList.get(i)[j]][1]);
					} else restriction++;
				}
			}

			//clear all adjacent cells that aren't flagged

			//keep track of if game lost
			boolean cont = true;

			//clear all that isn't flagged
			for (int v = 0; v < CurrentGame.pos.length && cont; v++) {
				int yNew = y + CurrentGame.pos[v][0];
				int xNew = x + CurrentGame.pos[v][1];

				try {
					if (!CurrentGame.gridButton[yNew][xNew].isSelected()) {
						if (CurrentGame.clearCells(yNew, xNew, true, 0) == -1) { //if game lost
							cont = false;
						}
					}
				} catch (IndexOutOfBoundsException e) {
					//do nothing
				}
			}

		//return if game was lost
		return cont;
	}

	//generates all possible configurations of mines for a given pattern
	private ArrayList<Theory> generateTheories (ArrayList<Set> combinedSet){

		//hold generated theories
		ArrayList<Theory> theoryList= new ArrayList<>();

		//hold which relative locations to flag
		ArrayList<Integer[]> toFlag = new ArrayList<>();
		
		//get all adjacent mine numbers
		Integer[] adjacentSets = new Integer[combinedSet.size()];
		for (int a = 1; a < combinedSet.size(); a++){
			adjacentSets[a] = combinedSet.get(a).getMineCount();
		}
		adjacentSets[0] = 0;
		
		//generate possibilities
		ArrayList<int[]> configs = createPossibility(combinedSet, 0, null);
		
		//cycle through number of possible mines on the board
		for (int[] config : configs){
			for (int j = 0; j < adjacentSets.length; j++){

				//hold numbers to flag in format {mine number, array of relative positions to flag}
				Integer[] iToFlag = new Integer[2];
				iToFlag[0] = adjacentSets[j];
				iToFlag[1] = config[j];

				toFlag.add(iToFlag);

				//if last set
				if (j == adjacentSets.length - 1){
					//cast to new local variable to avoid recursive errors
					ArrayList<Integer[]> newToFlag = new ArrayList<>();
					newToFlag.addAll(toFlag);

					//add theories
					theoryList.add(new Theory(combinedSet, newToFlag, 0, 0.0));

					//clear toFlag to avoid recursive errors
					toFlag.clear();
				}
			}
		}

		//return the list of generated theories
		return theoryList;
	}

	//generates all of the possible mine configurations to be made into theories by method generateTheories
	//based upon the adjacent numbers, flag a certain number of flags that are shared between the adjacent number and the main set
	//constructs a probability tree, each recursive iteration setting the number of mines to flag for one adjacent number
	private ArrayList <int[]> createPossibility (ArrayList<Set> combinedSet, int currentStage, int[] existingProb){

		//hold all possibilities in form {array of relative locations to flag}
		ArrayList <int[]> allProbs = new ArrayList<>();

		//ensure that the number of mines flagged is less than the shared undetermined cells between the cell and the adjacent numbers
		int restriction;

		//create new probability set if none exists
		if (existingProb == null){
			existingProb = new int[combinedSet.size()];
			//initialize as all zero
			for(int b = 0; b < existingProb.length; b++){
				existingProb[b] = 0;
			}
			//if first interation, set the max mine restriction to the number of unshared mines
			restriction = combinedSet.get(0).getNotShared();
		} else {
			//if not first iteration, get the number of shared undetermined cells between the adjacent number and the base set
			restriction = combinedSet.get(currentStage).getUnShared();
		}
		
		int length = existingProb.length;
		
		//count left
		//sum up all existing probs to determine how many more undetermined can be flagged
		int used = 0;
		for (int value : existingProb){
			used += value;
		}
		
		//subtract from total number of undetermined mines to get how many left available
		int left = combinedSet.get(0).getUnShared() - used;
		
		//create jth stage of probability set
		for (int j = 0; j <= restriction && j <= left; j++){
			
			existingProb[currentStage] = j;
			
			//if last adjacent number/no more tiers in probability tree
			if (currentStage == combinedSet.size()-1){

				//add possibilities to allProbs
				int[] newExisting = existingProb.clone();
				
				allProbs.add(newExisting);
			//if set is not complete
			} else {
				//go to next tier in possibility tree
				int newCurrent = currentStage + 1;
				//repeat generation
				allProbs.addAll(createPossibility(combinedSet, newCurrent, existingProb));
			}
		}

		//return flagging possibilities generated, only recieved by generatedTheories after all all possibilities are generated
		return allProbs;
	}
	
	//identifies and returns a subset of cells to be considered for theory generation/theory testing/rule application
	private ArrayList<Set> createSet (int i, int j, boolean repeat, int baseY, int baseX, ArrayList<Integer> alreadyY, ArrayList<Integer> alreadyX){

		//alreadyY and alreadyX keeps track of cells already added to the set so they aren't repeated

		//hold combined set of numbers, or the numbers adjacent to the undetermined cells of a base number
		ArrayList<Set> combinedSet = new ArrayList<>();

		Integer unShared = 0; //unknown
		Integer flagShared = 0; //flags
		Integer notShared = 0; //unknowns of base number that are not shared with any other number

		//if method was called by itself
		if (!repeat){
			int[][] newSet = new int[CurrentGame.pos.length][2];
			int[][] baseSet = new int[CurrentGame.pos.length][2];

			//get coordinates of cells around (baseX, baseY) cell
			for (int a = 0; a < newSet.length; a++){
				baseSet[a][0] = baseY + CurrentGame.pos[a][0];
				baseSet[a][1] = baseX + CurrentGame.pos[a][1];

				newSet[a][0] = i + CurrentGame.pos[a][0];
				newSet[a][1] = j + CurrentGame.pos[a][1];
			}

			//if cell exists, sound surrounding number of flags
			for (int b = 0; b < 8; b++){
				for (int[] set:newSet){
					try {
						//ensure that cells aren't counted twice
						if ((baseSet[b][0] == set[0]) && (baseSet[b][1] == set[1])){
							if (CurrentGame.gridButton[baseSet[b][0]][baseSet[b][1]].isSelected()){
								flagShared++;
							}
							//and undetermined
							else if (CurrentGame.gridButton[baseSet[b][0]][baseSet[b][1]].isEnabled()){
								unShared++;
							}
						}
					} catch (ArrayIndexOutOfBoundsException e){
						//do nothing
					}
				}
			}
		} else { //if method was called from a different method
			for (int k = 0; k < CurrentGame.pos.length; k++){

				//variables to hold radial coordinates of base number
				int yNew = i+CurrentGame.pos[k][0];
				int xNew = j+CurrentGame.pos[k][1];

				try {
					//count adjacent flags
					if (CurrentGame.gridButton[yNew][xNew] != null //button exists
							&& CurrentGame.gridButton[yNew][xNew].isSelected()){ //cell is flagged
						
						flagShared++;
						
						//if numbers around, create new set

						//scan around undetermined cell for other numbers
						for (int v = 0; v < CurrentGame.pos.length; v++){

							try {
								if (!CurrentGame.gridButton[yNew + CurrentGame.pos[v][0]][xNew + CurrentGame.pos[v][1]].getText().equals("") //if undetermined cell
										&& !((yNew + CurrentGame.pos[v][0]) == i && (xNew + CurrentGame.pos[v][1]) == j)){ //if not base cell

									if (alreadyY.isEmpty()){
										alreadyY.add(yNew + CurrentGame.pos[v][0]);
										alreadyX.add(xNew + CurrentGame.pos[v][1]);
										combinedSet.addAll(createSet(yNew + CurrentGame.pos[v][0], xNew + CurrentGame.pos[v][1], false, i, j, alreadyY, alreadyX));
									} else {
										boolean matchFound = false;
										for (int d = 0; d < alreadyY.size() && !matchFound; d++){
											if ((yNew + CurrentGame.pos[v][0]) == alreadyY.get(d) && (xNew + CurrentGame.pos[v][1]) == alreadyX.get(d)){
												matchFound = true;
											}
										}
										
										if (!matchFound){
											alreadyY.add(yNew + CurrentGame.pos[v][0]);
											alreadyX.add(xNew + CurrentGame.pos[v][1]);
											combinedSet.addAll(createSet(yNew + CurrentGame.pos[v][0], xNew + CurrentGame.pos[v][1], false, i, j, alreadyY, alreadyX));
										}
									}
								}
							} catch (ArrayIndexOutOfBoundsException e){
								//do nothing
							}
						}
					}
					
					//count adjacent undetermined
					else if (CurrentGame.gridButton[yNew][xNew] != null 
							&& CurrentGame.gridButton[yNew][xNew].isEnabled()){
						
						//if base, add to unShared
						unShared++;
						
						//keep track of adjacent cells
						int adj = 0;
						
						//if numbers around, create new set
						for (int v = 0; v < CurrentGame.pos.length; v++){
								
							try {
								if (!CurrentGame.gridButton[yNew + CurrentGame.pos[v][0]][xNew + CurrentGame.pos[v][1]].getText().equals("")
									&& !((yNew + CurrentGame.pos[v][0]) == i && (xNew + CurrentGame.pos[v][1]) ==j)){
									
									adj++; //number of adjacent cells

									//if alreadyY list is empty
									if (alreadyY.isEmpty()){
										//add coordinates of base cell to alreadyX and alreadyY
										alreadyY.add(yNew + CurrentGame.pos[v][0]);
										alreadyX.add(xNew + CurrentGame.pos[v][1]);
										combinedSet.addAll(createSet(yNew + CurrentGame.pos[v][0], xNew + CurrentGame.pos[v][1], false, i, j, alreadyY, alreadyX));
									} else {
										//check if there is a match found in the list of already found
										boolean matchFound = false;
										for (int d = 0; d < alreadyY.size() && !matchFound; d++){
											if ((yNew + CurrentGame.pos[v][0]) == alreadyY.get(d) && (xNew + CurrentGame.pos[v][1]) == alreadyX.get(d)){
												matchFound = true;
											}
										}

										//if no match is found, add to already found lists
										if (!matchFound){
											alreadyY.add(yNew + CurrentGame.pos[v][0]);
											alreadyX.add(xNew + CurrentGame.pos[v][1]);
											combinedSet.addAll(createSet(yNew + CurrentGame.pos[v][0], xNew + CurrentGame.pos[v][1], false, i, j, alreadyY, alreadyX));
										}

										//do nothing if cell has already been examined for subsets
									}
								}
							} catch (ArrayIndexOutOfBoundsException e){
								//do nothing
							}
						}//end loop
						//if there are no adjacent cells, increase number of notShared cells of base cell
						if (adj == 0) notShared++;
					}
				
				} catch (ArrayIndexOutOfBoundsException e){
					//do nothing
				}
				
			}//end loop
		}

		//create set
		Set set = new Set (Integer.parseInt(CurrentGame.gridButton[i][j].getText()), unShared, notShared, flagShared, repeat);
		
		combinedSet.add(set);
		
		//sort collection by if base first, then by # of mines
		combinedSet.sort((Set arg0, Set arg1) -> (arg0.compareTo(arg1, true)));

		return combinedSet;
	}

	//count number of undetermined cells
	private int getUndetermined(){
		int count = 0;
		for (int y = 0; y < ySize; y++)
			for (int x = 0; x < xSize; x++)
				if (CurrentGame.gridButton[y][x].isEnabled() && !CurrentGame.gridButton[y][x].isSelected())
					count++;
		
		return count;
	}

	//opens all cells a certain number of times
	private void firstMoveAnalysis(){

		//set up csv
		CellProcessor[] processor = getFirstMoveProcessors();

		String[] header = {"gameNumber", "locY", "locX", "cellsOpened"};

		CsvBeanWriter bw;

		//set up file
		File firstMoveData = new File("data/" + ySize + "_" + xSize + "firstMoveData.csv");

		//go through all cells
		for (int i = 0; i < ySize; i++) {
			for (int j = 0; j < xSize; j++) {
				//repeat action 10 times for each cell
				for (int repeat = 0; repeat < firstMoveRepeat; repeat++) {

					int[] returned = CurrentGame.firstClick(i, j, true);

					int count = returned[0];
					int y = returned[1];
					int x = returned[2];

					int gameNum = 1;

					try {
						//get number of lines in file
						LineNumberReader lnr = new LineNumberReader(new FileReader(firstMoveData));
						lnr.skip(Long.MAX_VALUE);
						gameNum = lnr.getLineNumber();
						lnr.close();

					} catch (IOException e){
						//System.out.println("failed to get game number");
					}

					//save data to object
					firstMoveData data = new firstMoveData(gameNum, y, x, count);

					try {

						if (firstMoveData.exists()) {

							bw = new CsvBeanWriter(new FileWriter(firstMoveData, true),
									CsvPreference.STANDARD_PREFERENCE);

							bw.write(data, header, processor);

						} else {

							bw = new CsvBeanWriter(new FileWriter(firstMoveData),
									CsvPreference.STANDARD_PREFERENCE);

							bw.writeHeader(header);

							bw.write(data, header, processor);

						}

						bw.close();

					} catch (IOException e){
						System.out.println("failed to write to firstMoveData file");
					}

					CurrentGame.restartGame();
				}
			}
		}

		//process data
		firstMoveData readData;

		ArrayList<firstMoveData> firstMoveDataArray = new ArrayList<>();

		//reader
		try{
			CsvBeanReader br = new CsvBeanReader(new FileReader(firstMoveData), CsvPreference.STANDARD_PREFERENCE);

			br.getHeader(true);

			//read data
			for (int i = 0; i < ySize; i++){
				for (int j = 0; j < xSize; j++){
					int sum = 0;
					for (int repeat = 0; repeat < firstMoveRepeat; repeat++) {
						//sum all cells opened
						readData = br.read(firstMoveData.class, header, processor);
						sum += Integer.parseInt(readData.getCellsOpened());
					}
					//calculate average
					int average = sum/firstMoveRepeat;
					firstMoveDataArray.add(new firstMoveData(0, i, j, average));
				}
			}

			Comparator<firstMoveData> cmp = (firstMoveData arg0, firstMoveData arg1) -> (arg0.compareTo(arg1));

			//save the location with the highest average number of cells opened to be used each following game
			firstMoveData max = Collections.max(firstMoveDataArray, cmp);

			firstX = Integer.parseInt(max.getLocX());
			firstY = Integer.parseInt(max.getLocY());

			br.close();
		}
		catch (IOException e){
			System.out.println("failed to read firstMoveData file");
		}

		//start playing
		fringeScan();
	}
}
