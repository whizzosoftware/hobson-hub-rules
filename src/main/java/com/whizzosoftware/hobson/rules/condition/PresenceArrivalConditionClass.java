/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.condition;

import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyConstraintType;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.task.condition.ConditionClassType;
import com.whizzosoftware.hobson.api.task.condition.ConditionEvaluationContext;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClass;

import java.util.ArrayList;
import java.util.List;

/**
 * A condition class for presence arrivals.
 *
 * @author Dan Noguerol
 */
public class PresenceArrivalConditionClass extends TaskConditionClass {
    public static final String ID = "presenceArrival";

    public PresenceArrivalConditionClass(PluginContext context) {
        super(PropertyContainerClassContext.create(context, ID), "A person arrives somewhere", "{person} arrives {location}");
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
        props.add(new TypedProperty.Builder("person", "Person", "The person to monitor", TypedProperty.Type.PRESENCE_ENTITY).
            constraint(PropertyConstraintType.required, true).
            build()
        );
        props.add(new TypedProperty.Builder("location", "Location", "The location the person arrives at", TypedProperty.Type.LOCATION).
            constraint(PropertyConstraintType.required, true).
            build()
        );
        return props;
    }
}
