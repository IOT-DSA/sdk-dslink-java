package org.dsa.iot.broker.utils;

import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.util.StringUtils;

/**
 * @author Samuel Grenier
 */
public class ParsedPath {

    private final boolean isRemote;
    private final String[] splitPath;
    private final String fullPath;

    private ParsedPath(boolean isRemote,
                       String[] split,
                       String fullPath) {
        this.isRemote = isRemote;
        this.splitPath = split;
        this.fullPath = fullPath;
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
    public String[] split() {
        return splitPath.clone();
    }

    /**
     * @return The full path
     */
    public String full() {
        return fullPath;
    }

    /**
     * @return The base path of the remote path.
     */
    public String base() {
        if (isRemote()) {
            String[] tmp = new String[splitPath.length - 2];
            System.arraycopy(splitPath, 2, tmp, 0, tmp.length);
            return "/" + StringUtils.join(tmp, "/");
        } else {
            return fullPath;
        }
    }

    /**
     * @param downstream Downstream name.
     * @param path Path to parse.
     * @return The parsed path.
     */
    public static ParsedPath parse(String downstream,
                                   String path) {
        path = NodeManager.normalizePath(path, true);
        String[] split = NodeManager.splitPath(path);
        boolean ir = split.length > 1 && split[0].equals(downstream);
        return new ParsedPath(ir, split, path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ParsedPath)) {
            return false;
        }

        String other = ((ParsedPath) o).full();
        return other == null ? full() == null : other.equals(full());
    }

    @Override
    public int hashCode() {
        return fullPath != null ? fullPath.hashCode() : 0;
    }
}
