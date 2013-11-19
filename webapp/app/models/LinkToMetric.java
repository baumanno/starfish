package models;

public class LinkToMetric {
	String start;
	String end;
	String MName;
	String FName;
	String MNameDrop;
	
	
	public LinkToMetric(String start, String end, String mName, String fName, String mNameDrop) {
		super();
		this.start = start;
		this.end = end;
		MName = mName;
		FName = fName;
		MNameDrop = mNameDrop;
	}
	
	public LinkToMetric() {
		super();
	}
	public String getStart() {
		return start;
	}
	public void setStart(String start) {
		this.start = start;
	}
	public String getEnd() {
		return end;
	}
	public void setEnd(String end) {
		this.end = end;
	}
	public String getMName() {
		return MName;
	}
	public void setMName(String mName) {
		MName = mName;
	}
	public String getFName() {
		return FName;
	}
	public void setFName(String fName) {
		FName = fName;
	}
	public String mNameDrop(String mNameDrop){
		return MNameDrop = mNameDrop;
	}
	

}
