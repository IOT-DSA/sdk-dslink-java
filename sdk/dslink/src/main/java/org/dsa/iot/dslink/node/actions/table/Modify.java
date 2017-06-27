package org.dsa.iot.dslink.node.actions.table;

public abstract class Modify {
	
	public boolean isReplace() {
		return false;
	};
	
	public boolean isInsert() {
		return true;
	};
	
	public static Modify fromString(String s) {
		String[] arr = s.split("\\s+");
		if (arr.length != 2) {
			return null;
		}
		if ("insert".equals(arr[0])) {
			return insert(Integer.parseInt(arr[1]));
		} else if ("replace".equals(arr[0])) {
			String[] arr2 = arr[1].split("-");
			if (arr2.length != 2) {
				return null;
			}
			return replace(Integer.parseInt(arr2[0]), Integer.parseInt(arr2[1]));
		}
		return null;
		
	}
	
	public static Modify insert(int insertBefore) {
		return new Insert(insertBefore);
	}
	
	public static Modify replace(int from, int to) {
		return new Replace(from, to);
	}

}
