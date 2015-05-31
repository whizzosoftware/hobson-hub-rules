/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerSet;
import com.whizzosoftware.hobson.api.task.HobsonTask;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.hobson.rules.RulesPlugin;
import com.whizzosoftware.hobson.rules.condition.ConditionConstants;
import com.whizzosoftware.hobson.rules.condition.TaskConditionFactory;
import org.jruleengine.rule.Action;
import org.jruleengine.rule.Assumption;
import org.jruleengine.rule.RuleImpl;

import java.util.*;

/**
 * A HobsonTask implementation that uses JRuleEngine for event-based tasking.
 *
 * @author Dan Noguerol
 */
public class JRETask extends HobsonTask {
    /**
     * The actual JRuleEngine rule that will be evaluated
     */
    private RuleImpl rule;

    public JRETask(PluginContext context, RuleImpl rule) throws Exception {
        this.rule = rule;
        setContext(TaskContext.create(context, rule.getDescription()));
        setName(rule.getDescription());
        setConditionSet(new PropertyContainerSet(TaskConditionFactory.createCondition(context, rule.getAssumptions())));

        if (rule.getActions().size() == 1) {
            Action a = (Action)rule.getActions().get(0);
            if (ConditionConstants.EXECUTE_ACTIONSET.equals(a.getMethod())) {
                setActionSet(new PropertyContainerSet((String) a.getValues().get(0), null));
            } else {
                throw new HobsonRuntimeException("No action set execution in task definition");
            }
        } else {
            throw new HobsonRuntimeException("No actions in task definition");
        }
    }

    public JRETask(TaskContext context, String name, PropertyContainerSet conditionSet, PropertyContainerSet actionSet) {
        setContext(context);
        setName(name);
        setConditionSet(conditionSet);
        setActionSet(actionSet);

        try {
            // create assumptions for trigger condition
            ArrayList<Assumption> assumptions = new ArrayList<>();
            if (conditionSet.hasPrimaryProperty()) {
                assumptions.addAll(createConditionAssumptions(conditionSet.getPrimaryProperty()));
            }

            // create rule
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new Action(ConditionConstants.EXECUTE_ACTIONSET, Collections.singletonList(actionSet.getId())));
            rule = new RuleImpl(
                context.getTaskId(),
                name,
                assumptions,
                actions,
                true
            );
        } catch (Exception e) {
            throw new HobsonRuntimeException("Error creating rule task", e);
        }
    }

    /**
     * Create list of assumptions based on a list of conditions.
     *
     * @param ctx the plugin context associated with the task
     * @param conditions the conditions
     */
    protected ArrayList<Assumption> createAssumptions(PluginContext ctx, List<PropertyContainer> conditions) {
        ArrayList<Assumption> ruleAssumps = new ArrayList<>();
        for (PropertyContainer condition : conditions) {
            ArrayList<Assumption> condAssumps = createConditionAssumptions(condition);
            ruleAssumps.addAll(condAssumps);
        }
        return ruleAssumps;
    }

    /**
     * Creates a list of assumptions representing a condition.
     *
     * @param condition the condition
     *
     * @return an list of assumptions
     */
    protected ArrayList<Assumption> createConditionAssumptions(PropertyContainer condition) {
        ArrayList<Assumption> assumpList = new ArrayList<>();
        String conditionClassId = condition.getContainerClassContext().getContainerClassId();
        switch (conditionClassId) {
            case RulesPlugin.CONDITION_CLASS_TURN_ON:
            case RulesPlugin.CONDITION_CLASS_TURN_OFF:
                DeviceContext dctx = (DeviceContext)condition.getPropertyValue("device");
                assumpList.add(new Assumption(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
                assumpList.add(new Assumption(ConditionConstants.PLUGIN_ID, "=", dctx.getPluginId()));
                assumpList.add(new Assumption("event.deviceId", "=", dctx.getDeviceId()));
                assumpList.add(new Assumption("event.variableName", "=", VariableConstants.ON));
                assumpList.add(new Assumption("event.variableValue", "=", RulesPlugin.CONDITION_CLASS_TURN_ON.equals(conditionClassId) ? "true" : "false"));
                return assumpList;
            default:
                throw new HobsonRuntimeException("Unable to recognize condition: " + condition.toString());

        }
    }
}
