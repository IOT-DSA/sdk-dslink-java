package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.EditorType;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.TimeUtils;
import org.dsa.iot.dslink.util.handler.CompleteHandler;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.historian.utils.QueryData;
import org.dsa.iot.historian.utils.TimestampRange;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class OverwriteHistoryAction implements Handler<ActionResult> {
    private static final String TIME_RANGE_PARAMETER_NAME = "Timerange";
    private static final String ACTION_NAME = "overwriteHistory";
    private static final String DISPLAY_NAME = "Overwrite history";
    private static final String NEW_VALUE_PARAMETER_NAME = "NewValue";

    private final Watch watch;
    private Database db;

    OverwriteHistoryAction(final Watch watch, Node parentNode, Permission permission, final Database db) {
        this.watch = watch;
        this.db = db;

        initializeNode(parentNode, permission);
    }

    private void initializeNode(Node parentNode, Permission permission) {
        NodeBuilder b = parentNode.createChild(ACTION_NAME);
        b.setSerializable(false);
        b.setDisplayName(DISPLAY_NAME);
        Action action = new Action(permission, this);

        Parameter timeRangeParameter = new Parameter(TIME_RANGE_PARAMETER_NAME, ValueType.STRING);
        timeRangeParameter.setEditorType(EditorType.DATE_RANGE);
        action.addParameter(timeRangeParameter);

        Parameter newValueParameter = new Parameter(NEW_VALUE_PARAMETER_NAME, ValueType.DYNAMIC);
        action.addParameter(newValueParameter);

        b.setAction(action);
        b.build();
    }

    @Override
    public void handle(ActionResult event) {
        TimestampRange timeRange = parseTimestampRangeFromDsaValue(event);
        final Value newValue = event.getParameter(NEW_VALUE_PARAMETER_NAME);

        final String path = watch.getPath();
        final long from = timeRange.from.getTimeInMillis();
        final long to = timeRange.to.getTimeInMillis();
        db.query(path, from, to, new CompleteHandler<QueryData>() {
            private final List<Long> timestampsOfValuesToOverwrite = new ArrayList<>();

            @Override
            public void handle(QueryData event) {
                timestampsOfValuesToOverwrite.add(event.getTimestamp());
            }

            @Override
            public void complete() {
                db.getProvider().deleteRange(watch, from, to);

                for (Long timestamp : timestampsOfValuesToOverwrite) {
                    db.write(path, newValue, timestamp);
                }
            }
        });
    }

    private static TimestampRange parseTimestampRangeFromDsaValue(ActionResult event) {
        final Calendar from;
        final Calendar to;
        Value v = event.getParameter(TIME_RANGE_PARAMETER_NAME);
        if (v != null) {
            String range = event.getParameter(TIME_RANGE_PARAMETER_NAME).getString();
            String[] split = range.split("/");
            from = TimeUtils.decode(split[0], null);
            to = TimeUtils.decode(split[1], null);
        } else { // Assume date is today
            from = TimeUtils.alignDay(Calendar.getInstance());
            to = Calendar.getInstance(); //now
        }

        return new TimestampRange(from, to);
    }
}
