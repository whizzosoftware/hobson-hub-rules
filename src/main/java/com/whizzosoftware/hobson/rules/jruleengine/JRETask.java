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
import com.whizzosoftware.hobson.api.event.DeviceUnavailableEvent;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.hobson.rules.condition.*;
import org.apache.commons.lang3.StringUtils;
import org.jruleengine.rule.Action;
import org.jruleengine.rule.Assumption;
import org.jruleengine.rule.RuleImpl;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * A HobsonTask implementation that uses JRuleEngine for event-based tasking.
 *
 * @author Dan Noguerol
 */
public class JRETask {
    private TaskContext context;
    private String name;
    private PropertyContainer triggerCondition;
    private RuleImpl rule;

    public JRETask(PluginContext context, RuleImpl rule) throws Exception {
        this.context = TaskContext.create(context.getHubContext(), rule.getName());
        this.rule = rule;
        this.triggerCondition = TaskConditionFactory.createCondition(context, rule.getAssumptions());
    }

    public JRETask(TaskContext context, String name, PropertyContainer triggerCondition) {
        this.context = context;
        this.name = name;
        this.triggerCondition = triggerCondition;

        try {
            // create assumptions for trigger condition
            ArrayList<Assumption> assumptions = new ArrayList<>();
            assumptions.addAll(createConditionAssumptions(triggerCondition));

            // create rule
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new Action(ConditionConstants.FIRE_TRIGGER, Collections.singletonList(context)));
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

    public TaskContext getContext() {
        return context;
    }

    public PropertyContainer getTriggerCondition() {
        return triggerCondition;
    }

    public JSONObject toJSON() {
        JSONObject rule = new JSONObject();
        rule.put("name", getContext().getTaskId());
        if (name != null) {
            rule.put("description", name);
        } else {
            rule.put("description", getContext().getTaskId());
        }

        JSONArray ruleAssumps = new JSONArray();
        if (triggerCondition != null) {
            createAssumptionJson(triggerCondition, ruleAssumps);
        }
        rule.put("assumptions", ruleAssumps);

        JSONArray actions = new JSONArray();
        JSONObject action = new JSONObject();
        action.put("method", ConditionConstants.FIRE_TRIGGER);
        action.put("arg1", getContext());
        actions.put(action);
        rule.put("actions", actions);

        return rule;
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
            case DeviceTurnsOnConditionClass.ID:
            case DeviceTurnsOffConditionClass.ID: {
                Collection<DeviceContext> dctxs = (Collection<DeviceContext>)condition.getPropertyValue("devices");
                if (dctxs != null) {
                    assumpList.add(new Assumption(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
                    assumpList.add(new Assumption("event.deviceCtx", "containsatleastone", "[" + StringUtils.join(dctxs, ',') + "]"));
                    assumpList.add(new Assumption("event.variableName", "=", VariableConstants.ON));
                    assumpList.add(new Assumption("event.variableValue", "=", DeviceTurnsOnConditionClass.ID.equals(conditionClassId) ? "true" : "false"));
                    return assumpList;
                } else {
                    throw new HobsonRuntimeException("No devices property found");
                }
            }
            case DeviceUnavailableEvent.ID: {
                Collection<DeviceContext> dctxs = (Collection<DeviceContext>)condition.getPropertyValue("devices");
                assumpList.add(new Assumption(ConditionConstants.EVENT_ID, "=", DeviceUnavailableEvent.ID));
                assumpList.add(new Assumption("event.deviceCtx", "containsatleastone", "[" + StringUtils.join(dctxs, ',') + "]"));
                return assumpList;
            }
            case DeviceIndoorTempAboveConditionClass.ID: {
                Collection<DeviceContext> dctxs = (Collection<DeviceContext>)condition.getPropertyValue("devices");
                assumpList.add(new Assumption(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
                assumpList.add(new Assumption("event.deviceCtx", "containsatleastone", "[" + StringUtils.join(dctxs, ',') + "]"));
                assumpList.add(new Assumption("event.variableName", "=", VariableConstants.INDOOR_TEMP_F));
                return assumpList;
            }
            default:
                throw new HobsonRuntimeException("Unable to recognize condition: " + condition.toString());

        }
    }

    protected void createAssumptionJson(PropertyContainer condition, JSONArray a) {
        PropertyContainerClassContext tccc = condition.getContainerClassContext();
        if (tccc.getContainerClassId().equals(DeviceTurnsOnConditionClass.ID) || tccc.getContainerClassId().equals(DeviceTurnsOffConditionClass.ID)) {
            a.put(createJSONCondition(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
            Collection<DeviceContext> ctx = (Collection<DeviceContext>)condition.getPropertyValue("devices");
            a.put(createJSONCondition(ConditionConstants.DEVICE_CTX, "containsatleastone", "[" + StringUtils.join(ctx, ',') + "]"));
            a.put(createJSONCondition(ConditionConstants.VARIABLE_NAME, "=", VariableConstants.ON));
            a.put(createJSONCondition(ConditionConstants.VARIABLE_VALUE, "=", (tccc.getContainerClassId().equals(DeviceTurnsOnConditionClass.ID)) ? "true" : "false"));
        } else if (tccc.getContainerClassId().equals(DeviceIndoorTempAboveConditionClass.ID)) {
            a.put(createJSONCondition(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
            Collection<DeviceContext> ctx = (Collection<DeviceContext>)condition.getPropertyValue("devices");
            a.put(createJSONCondition(ConditionConstants.DEVICE_CTX, "containsatleastone", "[" + StringUtils.join(ctx, ',') + "]"));
            a.put(createJSONCondition(ConditionConstants.VARIABLE_NAME, "=", VariableConstants.INDOOR_TEMP_F));
            a.put(createJSONCondition(ConditionConstants.VARIABLE_VALUE, ">", condition.getStringPropertyValue("tempF")));
        } else if (tccc.getContainerClassId().equals(DeviceIndoorTempBelowConditionClass.ID)) {
            a.put(createJSONCondition(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
            Collection<DeviceContext> ctx = (Collection<DeviceContext>)condition.getPropertyValue("devices");
            a.put(createJSONCondition(ConditionConstants.DEVICE_CTX, "containsatleastone", "[" + StringUtils.join(ctx, ',') + "]"));
            a.put(createJSONCondition(ConditionConstants.VARIABLE_NAME, "=", VariableConstants.INDOOR_TEMP_F));
            a.put(createJSONCondition(ConditionConstants.VARIABLE_VALUE, "<", condition.getStringPropertyValue("tempF")));
        } else if (tccc.getContainerClassId().equals(DeviceUnavailableEvent.ID)) {
            a.put(createJSONCondition(ConditionConstants.EVENT_ID, "=", DeviceUnavailableEvent.ID));
            Collection<DeviceContext> ctx = (Collection<DeviceContext>)condition.getPropertyValue("devices");
            a.put(createJSONCondition(ConditionConstants.DEVICE_CTX, "containsatleastone", "[" + StringUtils.join(ctx, ',') + "]"));
        }
    }

    protected JSONObject createJSONCondition(String leftTerm, String comparator, String rightTerm) {
        JSONObject json = new JSONObject();
        json.put("leftTerm", leftTerm);
        json.put("op", comparator);
        json.put("rightTerm", rightTerm);
        return json;
    }
}
