package minesweeper;

import java.util.ArrayList;

public class Theory {
	private ArrayList<Set> partialSet;
	private ArrayList<Integer[]> toFlag;
	private int tested;
	private double successRate;
	
	public Theory (ArrayList<Set> partialSet, ArrayList<Integer[]> toFlag, int tested, double successRate){

		this.partialSet = partialSet;
		this.toFlag = toFlag;
		this.tested = tested;
		this.successRate = successRate;
	}

	//getters and setters
	
	public Theory (ArrayList<Set> partialSet){
		this.partialSet = partialSet;
	}
	
	@Override
    public String toString() {
		String sToFlag = "";
		if (toFlag != null){
			for (int i = 0; i < toFlag.size(); i++){
				sToFlag += toFlag.get(i)[0] + ", " + toFlag.get(i)[1] + "\n";
			}
		} else sToFlag = "";
		
        return "Theory{" +
                "partialSet=" + partialSet.toString() +
                ", toFlag='" + sToFlag +
                ", nTested='" + tested + 
                ", successRate='" + successRate + "}\n";
    }
	
	public int getTested() {
		return tested;
	}

	public void setTested(int tested) {
		this.tested = tested;
	}

	public double getSuccessRate() {
		return successRate;
	}

	public void setSuccessRate(double successRate) {
		this.successRate = successRate;
	}

	public ArrayList<Integer[]> getToFlag() {
		return toFlag;
	}
	
	public int compareTo(Theory theory) {
		for (int i = 0; i < this.partialSet.size() || i < theory.partialSet.size(); i++){
			try {
				this.partialSet.get(i);
			} catch (IndexOutOfBoundsException e){
				return -1;
			}
			try {
				theory.partialSet.get(i);
			} catch (IndexOutOfBoundsException e){
				return 1;
			}
			
			Set origSet = this.partialSet.get(i);
			Set newSet = theory.partialSet.get(i);

			if (origSet.compareTo(newSet, false) != 0){
				return origSet.compareTo(newSet, false);
			}
		}
		return 0;
	}
}
