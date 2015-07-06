package org.dsa.iot.dslink.node.actions.table;

import org.dsa.iot.dslink.node.actions.Parameter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Constructs a table for action results. This class is not thread safe.
 *
 * @author Samuel Grenier
 */
public class Table {

    private List<Parameter> columns;
    private List<Row> rows;

    public void addColumn(Parameter parameter) {
        if (parameter == null) {
            throw new NullPointerException("parameter");
        } else if (columns == null) {
            columns = new LinkedList<>();
        }
        columns.add(parameter);
    }

    public void addRow(Row row) {
        if (row == null) {
            throw new NullPointerException("row");
        } else if (rows == null) {
            rows = new LinkedList<>();
        }
        rows.add(row);
    }

    public List<Parameter> getColumns() {
        return columns != null ? Collections.unmodifiableList(columns) : null;
    }

    public List<Row> getRows() {
        return rows != null ? Collections.unmodifiableList(rows) : null;
    }
}
