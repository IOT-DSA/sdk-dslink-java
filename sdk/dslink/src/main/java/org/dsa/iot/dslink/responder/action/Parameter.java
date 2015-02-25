package org.dsa.iot.dslink.responder.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;

/**
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class Parameter {
    
    @NonNull private final String name;
    @NonNull private final ValueType type;
    private final Value defValue;

}
