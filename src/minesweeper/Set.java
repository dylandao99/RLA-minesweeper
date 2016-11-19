package minesweeper;

public class Set {

	private Integer mineCount;
	private Integer unShared;
	private Integer notShared;
	private Integer flagShared;
	private boolean isBase;

	public Set(Integer mineCount, Integer unShared, Integer notShared, Integer flagShared, boolean isBase) {

		this.mineCount = mineCount;
		this.unShared = unShared;
		this.notShared = notShared;
		this.flagShared = flagShared;
		this.isBase = isBase;
	}

	//getters and setters

	public Set() {
		//default no args
	}

	//getters and setters

	@Override
    public String toString() {
        return "Set{" +
                "mineCount=" + mineCount +
                ", unShared=" + unShared +
                ", notShared=" + notShared +
                ", flagShared=" + flagShared +
                ", isBase=" + isBase() + "}";
    }

	public Integer getMineCount() {
		return mineCount;
	}
	
	public Integer getUnShared() {
		return unShared;
	}


	public boolean isBase() {
		return isBase;
	}

	public int compareTo(Set data, boolean isSet) {
			
		int compareQuantity = data.getMineCount();
			
		if (this.isBase() && isSet) {
			return -1;
		}
		else if (this.getMineCount() != compareQuantity) {
			return this.getMineCount() - compareQuantity;
		}
		else if (!this.getUnShared().equals(data.getUnShared())){
			return this.getUnShared() - data.getUnShared();
		} else {
			return this.getNotShared() - data.getNotShared();
		}
			
	}

	public Integer getNotShared() {
		return notShared;
	}
}
