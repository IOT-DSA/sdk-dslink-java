package org.dsa.iot.dslink.node.actions.table;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Samuel Grenier
 */
public class BatchRow {

    private List<Row> rows = new LinkedList<>();
    private Modifier modifier;

    public void addRow(Row row) {
        rows.add(row);
    }

    public List<Row> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public void setModifier(Modifier modifier) {
        this.modifier = modifier;
    }

    public Modifier getModifier() {
        return modifier;
    }

    public static class Modifier {

        private final String modifier;

        public Modifier(String modifier) {
            this.modifier = modifier;
        }

        public String get() {
            return modifier;
        }

        public static Modifier makeReplace(int start, int end) {
            return new Modifier("replace " + start + "-" + end);
        }

        @SuppressWarnings("unused")
        public static Modifier makeInsert(int pos) {
            return new Modifier("insert " + pos);
        }
    }
}
