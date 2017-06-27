package org.dsa.iot.dslink.node.actions.table;

public class Replace extends Modify {
	
	private int from, to;
	
	public Replace(int from, int to) {
		this.from = from;
		this.to = to;
	}
	
	@Override
	public boolean isReplace() {
		return true;
	}
	
	public int getReplaceFrom() {
		return from;
	}
	
	public int getReplaceTo() {
		return to;
	}

	@Override
	public String toString() {
		return "replace " + Integer.toString(from) + "-" + Integer.toString(to);
	}
}
