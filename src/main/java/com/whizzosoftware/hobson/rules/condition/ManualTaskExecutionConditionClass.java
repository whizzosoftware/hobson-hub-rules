/*******************************************************************************
 * Copyright (c) 2016 Whizzo Software, LLC.
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
import com.whizzosoftware.hobson.api.task.condition.ConditionClassType;
import com.whizzosoftware.hobson.api.task.condition.ConditionEvaluationContext;
import org.jruleengine.rule.Assumption;
import org.json.JSONArray;

import java.util.List;

/**
 * A condition for manually executed tasks.
 *
 * @author Dan Noguerol
 */
public class ManualTaskExecutionConditionClass extends AbstractRuleConditionClass {
    public static final String ID = "manualTaskExecution";

    public ManualTaskExecutionConditionClass(PluginContext context) {
        super(PropertyContainerClassContext.create(context, ID), "The task is manually executed", "The task is manually executed");
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
        return null;
    }

    @Override
    public List<Assumption> createConditionAssumptions(PropertyContainer condition) {
        return null;
    }

    @Override
    public JSONArray createAssumptionJSON(PropertyContainer condition) {
        return null;
    }
}
