/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.action.HobsonActionRef;
import com.whizzosoftware.hobson.api.trigger.HobsonTrigger;
import com.whizzosoftware.hobson.bootstrap.api.HobsonRuntimeException;
import org.jruleengine.rule.Action;
import org.jruleengine.rule.Assumption;
import org.jruleengine.rule.RuleImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.*;

/**
 * A HobsonTrigger implementation that uses JRuleEngine.
 *
 * @author Dan Noguerol
 */
public class JRETrigger implements HobsonTrigger {
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
    private final Properties properties = new Properties();

    public JRETrigger(String providerId, RuleImpl rule) throws Exception {
        this.providerId = providerId;
        this.rule = rule;

        Map<String,Object> map = createConditionMap(rule.getAssumptions());
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
     * @param providerId the trigger provider ID
     * @param json the JSON trigger definition
     */
    public JRETrigger(String providerId, JSONObject json) {
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
            throw new HobsonRuntimeException("Error creating rule trigger", e);
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

    protected Map<String,Object> createConditionMap(List<Assumption> assumps) throws Exception {
        Map<String,Object> map = new HashMap<>();

        for (Assumption a : assumps) {
            if (a.getLeftTerm().equals("event.eventId")) {
                map.put("event", a.getRightTerm());
            } else if (a.getLeftTerm().equals("event.pluginId")) {
                map.put("pluginId", a.getRightTerm());
            } else if (a.getLeftTerm().equals("event.deviceId")) {
                map.put("deviceId", a.getRightTerm());
            } else if (a.getLeftTerm().equals("event.variableName")) {
                map.put("name", a.getRightTerm());
            } else if (a.getLeftTerm().equals("event.variableValue")) {
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

    protected void createAssumptions(JSONArray conditions, List<Assumption> assList) throws Exception {
        for (int i=0; i < conditions.length(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            this.conditions.add(createAssumption(condition, assList));
        }
    }

    protected Map<String,Object> createAssumption(JSONObject condition, List<Assumption> assList) {
        if (condition.has("event")) {
            String eventType = condition.getString("event");
            if ("variableUpdate".equals(eventType)) {
                Map<String, Object> conditionMap = new HashMap<>();

                assList.add(new Assumption("event.eventId", "=", eventType));
                conditionMap.put("event", eventType);

                String s = condition.getString("pluginId");
                assList.add(new Assumption("event.pluginId", "=", s));
                conditionMap.put("pluginId", s);

                s = condition.getString("deviceId");
                assList.add(new Assumption("event.deviceId", "=", s));
                conditionMap.put("deviceId", s);

                s = condition.getString("name");
                assList.add(new Assumption("event.variableName", "=", s));
                conditionMap.put("name", s);

                s = condition.getString("comparator");
                assList.add(new Assumption("event.variableValue", s, condition.get("value").toString()));
                conditionMap.put("comparator", s);
                conditionMap.put("value", condition.get("value"));

                return conditionMap;
            } else if ("presenceUpdate".equals(eventType)) {
                Map<String,Object> conditionMap = new HashMap<>();
                assList.add(new Assumption("event.eventId", "=", eventType));
                conditionMap.put("event", eventType);

                String s = condition.getString("entityId");
                assList.add(new Assumption("event.entityId", "=", s));
                conditionMap.put("entityId", s);

                s = condition.getString("location");
                assList.add(new Assumption("event.location", "=", s));
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
