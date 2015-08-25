/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.condition;

import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.property.TypedPropertyConstraint;
import com.whizzosoftware.hobson.api.task.condition.ConditionClassType;
import com.whizzosoftware.hobson.api.task.condition.ConditionEvaluationContext;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClass;
import com.whizzosoftware.hobson.api.variable.VariableConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeviceTemperatureBelowConditionClass extends TaskConditionClass {
    public static final String CONDITION_CLASS_TEMP_BELOW = "tempBelow";

    public DeviceTemperatureBelowConditionClass(PluginContext context) {
        super(PropertyContainerClassContext.create(context, CONDITION_CLASS_TEMP_BELOW), "A device temperature drops below", "{devices} temperature drops below {tempF}");
    }

    @Override
    public ConditionClassType getType() {
        return ConditionClassType.trigger;
    }

    @Override
    public boolean evaluate(ConditionEvaluationContext context, PropertyContainer values) {
        return true;
    }

    @Override
    protected List<TypedProperty> createProperties() {
        List<TypedProperty> props = new ArrayList<>();
        props.add(new TypedProperty("devices", "Devices", "The device(s) reporting the temperature", TypedProperty.Type.DEVICES, Collections.singletonMap(TypedPropertyConstraint.deviceVariable, VariableConstants.TEMP_F)));
        props.add(new TypedProperty("tempF", "Temperature", "The temperature in Fahrenheit", TypedProperty.Type.NUMBER));
        return props;
    }
}
