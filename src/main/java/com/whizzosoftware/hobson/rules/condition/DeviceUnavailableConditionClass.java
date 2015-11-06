/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.condition;

import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.event.DeviceUnavailableEvent;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.*;
import com.whizzosoftware.hobson.api.task.condition.ConditionClassType;
import com.whizzosoftware.hobson.api.task.condition.ConditionEvaluationContext;
import org.apache.commons.lang3.StringUtils;
import org.jruleengine.rule.Assumption;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A condition class for device availability.
 *
 * @author Dan Noguerol
 */
public class DeviceUnavailableConditionClass extends AbstractRuleConditionClass {
    public static final String ID = "deviceNotAvailable";

    public DeviceUnavailableConditionClass(PluginContext context) {
        super(PropertyContainerClassContext.create(context, ID), "A device becomes unavailable", "{devices} become(s) unavailable");
    }
    @Override
    public ConditionClassType getConditionClassType() {
        return ConditionClassType.trigger;
    }

    @Override
    public boolean evaluate(ConditionEvaluationContext context, PropertyContainer values) {
        return false;
    }

    @Override
    protected List<TypedProperty> createProperties() {
        return Collections.singletonList(new TypedProperty.Builder("devices", "Devices", "The device(s) to monitor", TypedProperty.Type.DEVICES).
            constraint(PropertyConstraintType.required, true).
            build()
        );
    }

    @Override
    public List<Assumption> createConditionAssumptions(PropertyContainer condition) {
        List<Assumption> assumpList = new ArrayList<>();
        Collection<DeviceContext> dctxs = (Collection<DeviceContext>)condition.getPropertyValue("devices");
        assumpList.add(new Assumption(ConditionConstants.EVENT_ID, "=", DeviceUnavailableEvent.ID));
        assumpList.add(new Assumption("event.deviceCtx", "containsatleastone", "[" + StringUtils.join(dctxs, ',') + "]"));
        return assumpList;
    }

    @Override
    public JSONArray createAssumptionJSON(PropertyContainer condition) {
        JSONArray a = new JSONArray();
        a.put(createJSONCondition(ConditionConstants.EVENT_ID, "=", DeviceUnavailableEvent.ID));
        Collection<DeviceContext> ctx = (Collection<DeviceContext>)condition.getPropertyValue("devices");
        a.put(createJSONCondition(ConditionConstants.DEVICE_CTX, "containsatleastone", "[" + StringUtils.join(ctx, ',') + "]"));
        return a;
    }
}
