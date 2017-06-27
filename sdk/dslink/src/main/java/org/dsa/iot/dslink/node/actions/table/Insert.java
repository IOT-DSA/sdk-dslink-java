package org.dsa.iot.dslink.node.actions.table;

public class Insert extends Modify {
	
	private int insertBefore;
	
	public Insert(int insertBefore) {
		this.insertBefore = insertBefore;
	}
	
	@Override
	public boolean isInsert() {
		return true;
	}
	
	public int getInsertIndex() {
		return insertBefore;
	}
	
	@Override
	public String toString() {
		return "insert " + Integer.toString(insertBefore);
	}

}
