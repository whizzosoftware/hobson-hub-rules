/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.action.HobsonActionRef;
import com.whizzosoftware.hobson.api.task.HobsonTask;
import com.whizzosoftware.hobson.api.util.VariableChangeIdHelper;
import org.jruleengine.rule.Action;
import org.jruleengine.rule.Assumption;
import org.jruleengine.rule.RuleImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.*;

/**
 * A HobsonTask implementation that uses JRuleEngine.
 *
 * @author Dan Noguerol
 */
public class JRETask implements HobsonTask {
    private String providerId;
    /**
     * The actual JRuleEngine rule that will be evaluated
     */
    private RuleImpl rule;
    /**
     * The list of conditions that comprise the rule's assumptions
     */
    private final List<Map<String,Object>> conditions = new ArrayList<>();
    /**
     * The list of actions that the rule will execute
     */
    private final List<HobsonActionRef> actions = new ArrayList<>();
    /**
     * Free-form properties about the task
     */
    private final Properties properties = new Properties();

    public JRETask(String providerId, RuleImpl rule) throws Exception {
        this.providerId = providerId;
        this.rule = rule;

        Map<String,Object> map = populateChangeId(createConditionMap(rule.getAssumptions()));
        if (map != null && map.size() > 0) {
            conditions.add(map);
        }

        for (Object o : rule.getActions()) {
            if (o instanceof Action) {
                actions.add(createActionRef((Action)o));
            }
        }
    }

    /**
     * Constructor.
     *
     * @param providerId the task provider ID
     * @param json the JSON task definition
     */
    public JRETask(String providerId, JSONObject json) {
        this.providerId = providerId;

        try {
            ArrayList<Assumption> assumptions = new ArrayList<>();
            if (json.has("conditions")) {
                createAssumptions(json.getJSONArray("conditions"), assumptions);
            }

            ArrayList actionList = new ArrayList();
            if (json.has("actions")) {
                JSONArray actionsJson = json.getJSONArray("actions");
                for (int i=0; i < actionsJson.length(); i++) {
                    Action action = createAction(actionsJson.getJSONObject(i));
                    actionList.add(action);
                    this.actions.add(createActionRef(action));
                }
            }

            rule = new RuleImpl(UUID.randomUUID().toString(), json.getString("name"), assumptions, actionList, true);
        } catch (Exception e) {
            throw new HobsonRuntimeException("Error creating rule task", e);
        }
    }

    @Override
    public String getId() {
        return rule.getName();
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public String getName() {
        return rule.getDescription();
    }

    @Override
    public Type getType() {
        return Type.EVENT;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public boolean hasConditions() {
        return (conditions.size() > 0);
    }

    @Override
    public Collection<Map<String, Object>> getConditions() {
        return conditions;
    }

    @Override
    public boolean hasActions() {
        return (actions.size() > 0);
    }

    @Override
    public Collection<HobsonActionRef> getActions() {
        return actions;
    }

    @Override
    public boolean isEnabled() {
        return rule.isEnabled();
    }

    @Override
    public void execute() {
        for (HobsonActionRef action : actions) {
            // TODO
        }
    }

    protected Map<String,Object> createConditionMap(List<Assumption> assumps) throws Exception {
        Map<String,Object> map = new HashMap<>();

        for (Assumption a : assumps) {
            if (a.getLeftTerm().equals("event.eventId") || a.getLeftTerm().equals("com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.eventId")) {
                map.put("event", a.getRightTerm());
            } else if (a.getLeftTerm().equals("event.pluginId") || a.getLeftTerm().equals("com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.pluginId")) {
                map.put("pluginId", a.getRightTerm());
            } else if (a.getLeftTerm().equals("event.deviceId") || a.getLeftTerm().equals("com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.deviceId")) {
                map.put("deviceId", a.getRightTerm());
            } else if (a.getLeftTerm().equals("event.variableName") || a.getLeftTerm().equals("com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.variableName")) {
                map.put("name", a.getRightTerm());
            } else if (a.getLeftTerm().equals("event.variableValue") || a.getLeftTerm().equals("com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.variableValue")) {
                map.put("value", createValue(a.getRightTerm()));
                map.put("comparator", a.getOperator());
            } else if (a.getLeftTerm().equals("event.entityId")) {
                map.put("entityId", a.getRightTerm());
            } else if (a.getLeftTerm().equals("event.location")) {
                map.put("location", a.getRightTerm());
            }
        }

        return map;
    }

    protected Map<String,Object> populateChangeId(Map<String, Object> map) {
        if ("variableUpdate".equals(map.get("event"))) {
            if (map.get("value") != null && map.get("comparator") != null) {
                if ("on".equals(map.get("name"))) {
                    if (map.get("value").equals(true) && map.get("comparator").equals("=")) {
                        map.put("changeId", "turnOn");
                    } else if (map.get("value").equals(false) && map.get("comparator").equals("=")) {
                        map.put("changeId", "turnOff");
                    }
                }
            }
        }

        return map;
    }

    /**
     * Create an assumption list.
     *
     * @param conditions the JSON representation of the conditions
     * @param assumpList a List of Assumption objects to populate with the generated assumptions
     *
     * @throws Exception on failure
     */
    protected void createAssumptions(JSONArray conditions, List<Assumption> assumpList) throws Exception {
        for (int i=0; i < conditions.length(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            this.conditions.add(populateChangeId(createAssumption(condition, assumpList)));
        }
    }

    /**
     * Create an assumption.
     *
     * @param condition the JSON representation of the condition
     * @param assumpList the List of Assumption objects to populate with the generated assumptions
     *
     * @return a Map representation of the condition
     */
    protected Map<String,Object> createAssumption(JSONObject condition, List<Assumption> assumpList) {
        if (condition.has("event")) {
            String eventType = condition.getString("event");
            if ("variableUpdate".equals(eventType)) {
                Map<String, Object> conditionMap = new HashMap<>();

                assumpList.add(new Assumption("event.eventId", "=", eventType));
                conditionMap.put("event", eventType);

                String s = condition.getString("pluginId");
                assumpList.add(new Assumption("event.pluginId", "=", s));
                conditionMap.put("pluginId", s);

                s = condition.getString("deviceId");
                assumpList.add(new Assumption("event.deviceId", "=", s));
                conditionMap.put("deviceId", s);

                // handle variable value comparison
                if (condition.has("variable")) {
                    JSONObject cvar = condition.getJSONObject("variable");

                    s = cvar.getString("name");
                    assumpList.add(new Assumption("event.variableName", "=", s));
                    conditionMap.put("name", s);

                    s = cvar.getString("comparator");
                    assumpList.add(new Assumption("event.variableValue", s, cvar.get("value").toString()));
                    conditionMap.put("comparator", s);
                    conditionMap.put("value", cvar.get("value"));
                // handle change IDs
                } else if (condition.has("changeId")) {
                    Map<String,Object> var = VariableChangeIdHelper.getVariableForChangeId(condition.getString("changeId"));
                    String name = (String)var.keySet().toArray()[0];
                    Object value = var.get(name);
                    conditionMap.put("name", name);
                    conditionMap.put("value", value);
                    conditionMap.put("comparator", "=");
                }

                return conditionMap;
            } else if ("presenceUpdate".equals(eventType)) {
                Map<String,Object> conditionMap = new HashMap<>();
                assumpList.add(new Assumption("event.eventId", "=", eventType));
                conditionMap.put("event", eventType);

                String s = condition.getString("entityId");
                assumpList.add(new Assumption("event.entityId", "=", s));
                conditionMap.put("entityId", s);

                s = condition.getString("location");
                assumpList.add(new Assumption("event.location", "=", s));
                conditionMap.put("location", s);

                return conditionMap;
            } else {
                throw new HobsonRuntimeException("Unsupported event type: " + eventType);
            }
        } else {
            throw new HobsonRuntimeException("Unable to recognize condition: " + condition.toString());
        }
    }

    protected HobsonActionRef createActionRef(Action action) throws Exception {
        HobsonActionRef ref = new HobsonActionRef((String)action.getValues().get(0), (String)action.getValues().get(1), (String)action.getValues().get(2));
        JSONObject json = new JSONObject(new JSONTokener((String)action.getValues().get(3)));
        Iterator it = json.keys();
        while (it.hasNext()) {
            String key = (String)it.next();
            ref.addProperty(key, json.get(key));
        }
        return ref;
    }

    protected Action createAction(JSONObject json) throws Exception {
        List values = new ArrayList();
        values.add(json.getString("pluginId"));
        json.remove("pluginId");
        values.add(json.getString("actionId"));
        json.remove("actionId");
        values.add(json.getString("name"));
        json.remove("name");
        if (json.has("properties")) {
            JSONObject props = json.getJSONObject("properties");
            values.add(props.toString());
        }
        return new Action("actions.executeAction", values);
    }

    protected Object createValue(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        } else {
            return value;
        }
    }
}
