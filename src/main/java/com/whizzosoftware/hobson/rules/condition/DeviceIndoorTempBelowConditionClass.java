/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.condition;

import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.event.device.DeviceVariablesUpdateEvent;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.*;
import com.whizzosoftware.hobson.api.task.condition.ConditionClassType;
import com.whizzosoftware.hobson.api.task.condition.ConditionEvaluationContext;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import org.apache.commons.lang3.StringUtils;
import org.jruleengine.rule.Assumption;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DeviceIndoorTempBelowConditionClass extends AbstractRuleConditionClass {
    public static final String ID = "inTempBelow";

    public DeviceIndoorTempBelowConditionClass(PluginContext context) {
        super(PropertyContainerClassContext.create(context, ID), "An indoor temperature drops below", "{devices} outdoor temperature drops below {inTempF}");
    }

    @Override
    public ConditionClassType getConditionClassType() {
        return ConditionClassType.trigger;
    }

    @Override
    public boolean evaluate(ConditionEvaluationContext context, PropertyContainer values) {
        return true;
    }

    @Override
    protected List<TypedProperty> createProperties() {
        List<TypedProperty> props = new ArrayList<>();
        props.add(new TypedProperty.Builder("devices", "Devices", "The device(s) reporting the temperature", TypedProperty.Type.DEVICES).
            constraint(PropertyConstraintType.required, true).
            constraint(PropertyConstraintType.deviceVariable, VariableConstants.INDOOR_TEMP_F).
            build()
        );
        props.add(new TypedProperty.Builder(VariableConstants.INDOOR_TEMP_F, "Temperature", "The temperature in Fahrenheit", TypedProperty.Type.NUMBER).
            constraint(PropertyConstraintType.required, true).
            build()
        );
        return props;
    }

    @Override
    public List<Assumption> createConditionAssumptions(PropertyContainer condition) {
        List<Assumption> assumpList = new ArrayList<>();
        Collection<DeviceContext> dctxs = (Collection<DeviceContext>)condition.getPropertyValue("devices");
        assumpList.add(new Assumption(ConditionConstants.EVENT_ID, "=", DeviceVariablesUpdateEvent.ID));
        assumpList.add(new Assumption("event.deviceCtx", "containsatleastone", "[" + StringUtils.join(dctxs, ',') + "]"));
        assumpList.add(new Assumption("event.variableName", "<", VariableConstants.INDOOR_TEMP_F));
        return assumpList;
    }

    @Override
    public JSONArray createAssumptionJSON(PropertyContainer condition) {
        JSONArray a = new JSONArray();
        a.put(createJSONCondition(ConditionConstants.EVENT_ID, "=", DeviceVariablesUpdateEvent.ID));
        Collection<DeviceContext> ctx = (Collection<DeviceContext>)condition.getPropertyValue("devices");
        a.put(createJSONCondition(ConditionConstants.DEVICE_CTX, "containsatleastone", "[" + StringUtils.join(ctx, ',') + "]"));
        a.put(createJSONCondition(ConditionConstants.VARIABLE_NAME, "=", VariableConstants.INDOOR_TEMP_F));
        a.put(createJSONCondition(ConditionConstants.VARIABLE_VALUE, "<", condition.getStringPropertyValue(VariableConstants.INDOOR_TEMP_F)));
        return a;
    }
}
