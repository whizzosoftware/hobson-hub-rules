/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.condition;

import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
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
import java.util.Collections;
import java.util.List;

public class DeviceTurnsOnConditionClass extends AbstractRuleConditionClass {
    public static final String ID = "turnOn";

    public DeviceTurnsOnConditionClass(PluginContext context) {
        super(PropertyContainerClassContext.create(context, ID), "A device turns on", "{devices} turns on");
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
        return Collections.singletonList(new TypedProperty.Builder("devices", "Devices", "The device(s) to monitor", TypedProperty.Type.DEVICES).
            constraint(PropertyConstraintType.required, true).
            constraint(PropertyConstraintType.deviceVariable, VariableConstants.ON).
            build()
        );
    }

    @Override
    public List<Assumption> createConditionAssumptions(PropertyContainer condition) {
        List<Assumption> assumpList = new ArrayList<>();
        Collection<DeviceContext> dctxs = (Collection<DeviceContext>)condition.getPropertyValue("devices");
        if (dctxs != null) {
            assumpList.add(new Assumption(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
            assumpList.add(new Assumption("event.deviceCtx", "containsatleastone", "[" + StringUtils.join(dctxs, ',') + "]"));
            assumpList.add(new Assumption("event.variableName", "=", VariableConstants.ON));
            assumpList.add(new Assumption("event.variableValue", "=", DeviceTurnsOnConditionClass.ID.equals(getContext().getContainerClassId()) ? "true" : "false"));
            return assumpList;
        } else {
            throw new HobsonRuntimeException("No devices property found");
        }
    }

    @Override
    public JSONArray createAssumptionJSON(PropertyContainer condition) {
        JSONArray a = new JSONArray();
        PropertyContainerClassContext tccc = condition.getContainerClassContext();
        a.put(createJSONCondition(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
        Collection<DeviceContext> ctx = (Collection<DeviceContext>)condition.getPropertyValue("devices");
        a.put(createJSONCondition(ConditionConstants.DEVICE_CTX, "containsatleastone", "[" + StringUtils.join(ctx, ',') + "]"));
        a.put(createJSONCondition(ConditionConstants.VARIABLE_NAME, "=", VariableConstants.ON));
        a.put(createJSONCondition(ConditionConstants.VARIABLE_VALUE, "=", (tccc.getContainerClassId().equals(DeviceTurnsOnConditionClass.ID)) ? "true" : "false"));
        return a;
    }
}
