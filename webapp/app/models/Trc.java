package models;

public class Trc {
    
	public String from;
	public String to;
	
    public Trc() {}
    
    public Trc(String from, String to){
    	this.from=from;
    	this.to=to;
    }
    
    public final String getFrom() {
        return from;
    }
    
    public final String getTo() {
        return to;
    }
    
}