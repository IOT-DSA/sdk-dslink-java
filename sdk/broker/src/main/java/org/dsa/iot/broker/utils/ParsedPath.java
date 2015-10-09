package org.dsa.iot.broker.utils;

import org.dsa.iot.dslink.node.NodeManager;

/**
 * @author Samuel Grenier
 */
public class ParsedPath {

    private final boolean isRemote;
    private final String[] splitPath;

    private ParsedPath(boolean isRemote,
                       String[] split) {
        this.isRemote = isRemote;
        this.splitPath = split;
    }

    /**
     * @return Whether the path references a remote endpoint or not.
     */
    public boolean isRemote() {
        return isRemote;
    }

    /**
     * @return The split path during parsing.
     */
    public String[] splitPath() {
        return splitPath.clone();
    }

    /**
     * @param downstream Downstream name.
     * @param path Path to parse.
     * @return The parsed path.
     */
    public static ParsedPath parse(String downstream,
                                   String path) {
        String[] split = NodeManager.splitPath(path);
        boolean ir = split.length > 1 && split[0].equals(downstream);
        return new ParsedPath(ir, split);
    }

}
