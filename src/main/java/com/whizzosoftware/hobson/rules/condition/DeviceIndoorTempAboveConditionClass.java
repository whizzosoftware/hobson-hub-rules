/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.condition;

import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.*;
import com.whizzosoftware.hobson.api.task.condition.ConditionClassType;
import com.whizzosoftware.hobson.api.task.condition.ConditionEvaluationContext;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClass;
import com.whizzosoftware.hobson.api.variable.VariableConstants;

import java.util.ArrayList;
import java.util.List;

public class DeviceIndoorTempAboveConditionClass extends TaskConditionClass {
    public static final String ID = "inTempAbove";

    public DeviceIndoorTempAboveConditionClass(PluginContext context) {
        super(PropertyContainerClassContext.create(context, ID), "An indoor temperature rises above", "{devices} indoor temperature rises above {inTempF}");
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
        props.add(new TypedProperty.Builder("inTempF", "Temperature", "The temperature in Fahrenheit", TypedProperty.Type.NUMBER).
            constraint(PropertyConstraintType.required, true).
            build()
        );
        return props;
    }
}
