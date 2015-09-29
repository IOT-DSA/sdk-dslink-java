package org.dsa.iot.commons;

import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;

import java.util.*;

/**
 * A special type of action that supports full persistence and parameter
 * validation.
 *
 * A trivial use case would be for a settings action that require its settings
 * to be persisted after every invocation.
 *
 * @author Samuel Grenier
 */
public abstract class ParameterizedAction extends Action {

    private final Map<String, ParameterInfo> params = new LinkedHashMap<>();

    /**
     * Constructs a parameterized action that supports persistence and full
     * parameter validation.
     *
     * @param permission Permission of the action.
     */
    public ParameterizedAction(Permission permission) {
        super(permission, null);
    }

    /**
     * Using {@code actRes} to retrieve parameters will return the unvalidated
     * parameter.
     *
     * @param actRes Action results to set output.
     * @param params Validated parameters.
     */
    public abstract void handle(ActionResult actRes,
                                Map<String, Value> params);

    /**
     * The {@code param} will be converted to a {@link ParameterInfo} instance
     * with all of its properties copied over. By default, the copied parameter
     * will be not optional, will be persistent, and will not contain a
     * validator.
     *
     * {@inheritDoc}
     *
     * @see #addParameter(ParameterInfo)
     */
    @Override
    public ParameterizedAction addParameter(Parameter param) {
        if (param == null) {
            throw new NullPointerException("param");
        }
        String name = param.getName();
        ValueType type = param.getType();
        ParameterInfo copy = new ParameterInfo(name, type);
        copy.setOptional(false);
        copy.setPersistent(true);
        copy.setDefaultValue(param.getDefault());
        copy.setDescription(param.getDescription());
        copy.setPlaceHolder(param.getPlaceHolder());
        copy.setEditorType(param.getEditorType());
        addParameter(copy);
        return this;
    }

    /**
     * @param paramInfo Parameter with extra options.
     */
    public void addParameter(ParameterInfo paramInfo) {
        if (paramInfo == null) {
            throw new NullPointerException("paramInfo");
        }
        String name = paramInfo.getName();
        if (params.containsKey(name)) {
            String err = "Parameter name already exists: " + name;
            throw new IllegalStateException(err);
        }
        params.put(name, paramInfo);
        super.addParameter(paramInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void invoke(ActionResult actionResult) {
        if (!hasPermission()) return;
        List<Parameter> list = new ArrayList<>();
        Map<String, Value> map = new HashMap<>();
        for (ParameterInfo info : params.values()) {
            String name = info.getName();
            Value value = actionResult.getParameter(name);
            if (!info.optional() && value == null) {
                throw new RuntimeException("Missing parameter: " + name);
            }
            if (value != null) {
                ValueUtils.checkType(name, info.getType(), value);
                Handler<Value> validator = info.getValidator();
                if (validator != null) {
                    long time = value.getDate().getTime();
                    validator.handle(value);
                    // Ensure original time stamp is preserved
                    value.setTime(time);
                }
                map.put(name, value);
            }
            if (info.persist()) {
                info.setDefaultValue(value);
            }
            list.add(info);
        }
        setParams(list);
        handle(actionResult, map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setParams(Collection<Parameter> newParams) {
        super.params = new JsonArray();
        for (Parameter p : newParams) {
            super.addParameter(p);
        }
        postParamsUpdate();
    }

    /**
     * A special type of parameter that contains extra information as to how
     * the parameter should behave when being validated.
     */
    public static class ParameterInfo extends Parameter {
        private boolean optional;
        private boolean persist;
        private Handler<Value> validator;

        /**
         * {@inheritDoc}
         */
        public ParameterInfo(String name, ValueType type) {
            this(name, type, null);
        }

        /**
         * {@inheritDoc}
         */
        public ParameterInfo(String name, ValueType type, Value def) {
            super(name, type, def);
        }

        /**
         * The {@code validator} can override the value or throw an
         * {@link Exception} for any invalid values.
         *
         * @param validator Validation callback.
         */
        public void setValidator(Handler<Value> validator) {
            this.validator = validator;
        }

        /**
         * @return The validator the parameter is attached to.
         */
        public Handler<Value> getValidator() {
            return validator;
        }

        /**
         * If the parameter is optional then it will not be required during
         * validation.
         *
         * @param optional Whether this parameter is optional.
         */
        public void setOptional(boolean optional) {
            this.optional = optional;
        }

        /**
         * @return Whether this parameter is optional.
         */
        public boolean optional() {
            return optional;
        }

        /**
         * Before the action is invoked, the persistence values will be
         * configured and validated. This will ensure the parameters always
         * have the correct default values.
         *
         * @param persist Whether this parameter is persistent.
         */
        public void setPersistent(boolean persist) {
            this.persist = persist;
        }

        /**
         * @return Whether this parameter is persistent.
         */
        public boolean persist() {
            return persist;
        }
    }
}
