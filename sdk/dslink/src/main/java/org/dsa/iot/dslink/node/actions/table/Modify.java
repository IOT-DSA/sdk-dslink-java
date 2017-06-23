package org.dsa.iot.dslink.node.actions.table;

public class Modify {
	
	private int removeFrom = -1;
	private int insertBefore = -1;
	
	private Modify(int insertBefore) {
		this.insertBefore = insertBefore;
	}
	
	private Modify(int removeFrom, int insertBefore) {
		this.removeFrom = removeFrom;
		this.insertBefore = insertBefore;
	}
	
	
	public boolean isReplace() {
		return (removeFrom > -1 && insertBefore > -1);
	}
	
	public boolean isInsert() {
		return (removeFrom == -1 && insertBefore > -1);
	}
	
	public int getInsertIndex() {
		if (isInsert()) {
			return insertBefore;
		}
		throw new UnsupportedOperationException("not an insert");
	}
	
	public int getReplaceFrom() {
		if (isReplace()) {
			return removeFrom;
		}
		throw new UnsupportedOperationException("not a replace");
	}
	
	public int getReplaceTo() {
		if (isReplace()) {
			return insertBefore;
		}
		throw new UnsupportedOperationException("not a replace");
	}
	
	public String toString() {
		if (isReplace()) {
			return "replace " + Integer.toString(removeFrom) + "-" + Integer.toString(insertBefore);
		} else {
			return "insert " + Integer.toString(insertBefore);
		}
	}
	
	public static Modify fromString(String s) {
		String[] arr = s.split("\\s+");
		if (arr.length != 2) {
			return null;
		}
		if (arr[0].equals("insert")) {
			return insert(Integer.parseInt(arr[1]));
		} else if (arr[0].equals("replace")) {
			String[] arr2 = arr[1].split("-");
			if (arr2.length != 2) {
				return null;
			}
			return replace(Integer.parseInt(arr2[0]), Integer.parseInt(arr2[1]));
		}
		return null;
		
	}
	
	public static Modify insert(int insertBefore) {
		return new Modify(insertBefore);
	}
	
	public static Modify replace(int from, int to) {
		return new Modify(from, to);
	}

}
