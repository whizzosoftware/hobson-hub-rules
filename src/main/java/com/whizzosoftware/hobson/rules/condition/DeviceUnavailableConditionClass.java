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

import java.util.Collections;
import java.util.List;

/**
 * A condition class for device availability.
 *
 * @author Dan Noguerol
 */
public class DeviceUnavailableConditionClass extends TaskConditionClass {
    public static final String CONDITION_CLASS_DEVICE_NOT_AVAILABLE = "deviceNotAvailable";

    public DeviceUnavailableConditionClass(PluginContext context) {
        super(PropertyContainerClassContext.create(context, CONDITION_CLASS_DEVICE_NOT_AVAILABLE), "A device becomes unavailable", "{devices} become(s) unavailable");
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
}
