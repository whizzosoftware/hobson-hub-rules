/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.PropertyContainerSet;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.task.HobsonTask;
import com.whizzosoftware.hobson.api.task.MockTaskManager;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.api.task.condition.ConditionClassType;
import com.whizzosoftware.hobson.api.task.condition.ConditionEvaluationContext;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClass;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import com.whizzosoftware.hobson.rules.RulesPlugin;
import com.whizzosoftware.hobson.rules.condition.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.*;
import java.util.*;

public class JRETaskProviderTest {
    @Test
    public void testRuleConstruction() throws Exception {
        PluginContext pctx = PluginContext.createLocal("pluginId");
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(pctx, RulesPlugin.CONDITION_CLASS_TURN_OFF);
        MockTaskManager taskManager = new MockTaskManager();
        taskManager.publishConditionClass(new TaskConditionClass(pccc, "", "") {
            @Override
            public ConditionClassType getType() {
                return ConditionClassType.trigger;
            }

            @Override
            public List<TypedProperty> createProperties() {
                return null;
            }

            @Override
            public boolean evaluate(ConditionEvaluationContext context, PropertyContainer values) {
                return true;
            }
        });

        // validate we start with a non-existent temp file
        File ruleFile = File.createTempFile("rules", ".json");
        assertTrue(ruleFile.delete());
        assertFalse(ruleFile.exists());

        JRETaskProvider provider = new JRETaskProvider(pctx);
        provider.setTaskManager(taskManager);
        provider.setRulesFile(ruleFile);

        provider.onCreateTasks(Collections.singletonList(
                new HobsonTask(
                        TaskContext.create(pctx.getHubContext(), "foo"),
                        "My Task",
                        null,
                        null,
                        Collections.singletonList(
                                new PropertyContainer(
                                        pccc,
                                        Collections.singletonMap("devices", (Object) Collections.singletonList(DeviceContext.createLocal("com.whizzosoftware.hobson.hobson-hub-zwave", "zwave-32")))
                                )
                        ),
                        new PropertyContainerSet("actionset1", null)
                )));

        // make sure the provider updated the rule file
        assertTrue(ruleFile.exists());
        JSONObject jobj = new JSONObject(new JSONTokener(new FileReader(ruleFile)));
        assertPrefix(jobj);

        assertTrue(jobj.has("rules"));

        JSONArray rules = jobj.getJSONArray("rules");
        assertEquals(1, rules.length());
        JSONObject rule = rules.getJSONObject(0);
        assertNotNull(rule.get("name"));
        assertNotNull(rule.get("description"));
        assertNotNull(rule.get("actions"));

        assertTrue(rule.has("assumptions"));
        JSONArray conditions = rule.getJSONArray("assumptions");
        assertEquals(4, conditions.length());
        JSONObject condition = conditions.getJSONObject(0);
        assertEquals(ConditionConstants.EVENT_ID, condition.getString("leftTerm"));
        assertEquals("=", condition.getString("op"));
        assertEquals(VariableUpdateNotificationEvent.ID, condition.getString("rightTerm"));
        condition = conditions.getJSONObject(1);
        assertEquals(ConditionConstants.DEVICE_CTX, condition.getString("leftTerm"));
        assertEquals("containsatleastone", condition.getString("op"));
        assertEquals("[local:local:com.whizzosoftware.hobson.hobson-hub-zwave:zwave-32]", condition.getString("rightTerm"));
        condition = conditions.getJSONObject(2);
        assertEquals(ConditionConstants.VARIABLE_NAME, condition.getString("leftTerm"));
        assertEquals("=", condition.getString("op"));
        assertEquals("on", condition.getString("rightTerm"));
        condition = conditions.getJSONObject(3);
        assertEquals(ConditionConstants.VARIABLE_VALUE, condition.getString("leftTerm"));
        assertEquals("=", condition.getString("op"));
        assertEquals("false", condition.getString("rightTerm"));

        JSONArray actions = rule.getJSONArray("actions");
        assertEquals(1, actions.length());
        JSONObject action = actions.getJSONObject(0);
        assertEquals(ConditionConstants.FIRE_TRIGGER, action.getString("method"));
        assertTrue(action.getString("arg1").startsWith("local:local:"));
    }

    @Test
    public void testRuleExecution() throws Exception {
        MockTaskManager taskManager = new MockTaskManager();
        JRETaskProvider engine = new JRETaskProvider(PluginContext.createLocal("pluginId"));
        engine.setTaskManager(taskManager);
        String rulesJson = "{\n" +
            "\t\"name\": \"Hobson Rules\",\n" +
            "\t\"description\": \"Hobson Rules\",\n" +
            "\t\"rules\": [\n" +
            "\t\t{\n" +
            "\t\t\t\"name\": \"ruleid1\", \n" +
            "\t\t\t\"description\": \"test rule\",\n" +
            "\t\t\t\"assumptions\": [\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"leftTerm\": \"com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.eventId\",\n" +
            "\t\t\t\t\t\"op\": \"=\",\n" +
            "\t\t\t\t\t\"rightTerm\": \"" + VariableUpdateNotificationEvent.ID + "\"\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"leftTerm\": \"com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.deviceCtx\",\n" +
            "\t\t\t\t\t\"op\": \"=\",\n" +
            "\t\t\t\t\t\"rightTerm\": \"local:local:comwhizzosoftwarehobsonserver-zwave:zwave-32\"\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"leftTerm\": \"com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.variableName\",\n" +
            "\t\t\t\t\t\"op\": \"=\",\n" +
            "\t\t\t\t\t\"rightTerm\": \"on\"\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"leftTerm\": \"com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.variableValue\",\n" +
            "\t\t\t\t\t\"op\": \"=\",\n" +
            "\t\t\t\t\t\"rightTerm\": \"true\"\n" +
            "\t\t\t\t}\n" +
            "\t\t\t],\n" +
            "\t\t\t\"actions\": [\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"method\": \"com.whizzosoftware.hobson.rules.jruleengine.JRETaskContext.fireTaskTrigger\",\n" +
            "\t\t\t\t\t\"arg1\": \"local:local:task1\",\n" +
            "\t\t\t\t}\n" +
            "\t\t\t]\n" +
            "\t\t}\n" +
            "\t]\n" +
            "}";
        engine.loadRules(new ByteArrayInputStream(rulesJson.getBytes()));

        Collection<JRETask> tasks = engine.getTasks();
        assertEquals(1, tasks.size());
        JRETask task = tasks.iterator().next();
        assertNotNull("ruleid1", task.getContext().getTaskId());

        assertEquals(0, taskManager.getActionSetExecutions().size());

        engine.processEvent(new VariableUpdateNotificationEvent(System.currentTimeMillis(), new VariableUpdate(DeviceContext.createLocal("comwhizzosoftwarehobsonserver-zwave", "zwave-32"), VariableConstants.ON, true)));

        assertEquals(1, taskManager.getTaskExecutions().size());
        assertEquals("task1", taskManager.getTaskExecutions().get(0).getTaskId());
    }

    @Test
    public void testRuleFileWrite() throws Exception {
        PluginContext ctx = PluginContext.createLocal("plugin1");
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(ctx, RulesPlugin.CONDITION_CLASS_TURN_ON);

        File rulesFile = createEmptyRulesFile();
        MockTaskManager taskManager = new MockTaskManager();
        taskManager.publishConditionClass(new TaskConditionClass(pccc, "", "") {
            @Override
            public ConditionClassType getType() {
                return ConditionClassType.trigger;
            }

            @Override
            public List<TypedProperty> createProperties() {
                return null;
            }

            @Override
            public boolean evaluate(ConditionEvaluationContext context, PropertyContainer values) {
                return true;
            }
        });

        JRETaskProvider engine = new JRETaskProvider(ctx);
        engine.setTaskManager(taskManager);
        engine.setRulesFile(rulesFile);

        engine.onCreateTasks(Collections.singletonList(new HobsonTask(
                TaskContext.createLocal("task1"),
                "New Task",
                null,
                null,
                Collections.singletonList(
                        new PropertyContainer(
                                pccc,
                                Collections.singletonMap("devices", (Object) Collections.singletonList(DeviceContext.createLocal("plugin2", "device1")))
                        )
                ),
                new PropertyContainerSet(
                        "actionset1",
                        null
                )
        )));

        JSONObject json = new JSONObject(new JSONTokener(new FileInputStream(rulesFile)));

        // test prefix
        assertPrefix(json);

        // test rule meta
        assertTrue(json.has("rules"));
        JSONArray rules = json.getJSONArray("rules");
        assertEquals(1, rules.length());
        JSONObject rule = rules.getJSONObject(0);
        assertNotNull(rule.getString("name"));
        assertNotNull("New Task", rule.getString("description"));

        // test rule assumptions
        assertTrue(rule.has("assumptions"));
        JSONArray assumptions = rule.getJSONArray("assumptions");
        assertEquals(4, assumptions.length());
        assertEquals(ConditionConstants.EVENT_ID, assumptions.getJSONObject(0).getString("leftTerm"));

        // test rule actions
        assertTrue(rule.has("actions"));
        JSONArray actions = rule.getJSONArray("actions");
        assertEquals(1, actions.length());
    }

    @Test
    public void testProcessEventForTemperatureOutOfRange() throws Exception {
        File rulesFile = createEmptyRulesFile();
        PluginContext ctx = PluginContext.createLocal("plugin1");
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(ctx, RulesPlugin.CONDITION_CLASS_TEMP_ABOVE);

        MockTaskManager taskManager = new MockTaskManager();
        taskManager.publishConditionClass(new TaskConditionClass(pccc, "", "") {
            @Override
            public ConditionClassType getType() {
                return ConditionClassType.trigger;
            }

            @Override
            public List<TypedProperty> createProperties() {
                return null;
            }

            @Override
            public boolean evaluate(ConditionEvaluationContext context, PropertyContainer values) {
                return true;
            }
        });

        JRETaskProvider engine = new JRETaskProvider(ctx);
        engine.setTaskManager(taskManager);
        engine.setRulesFile(rulesFile);

        PluginContext pctx = PluginContext.createLocal("plugin");

        Map<String,Object> propValues = new HashMap<>();
        ArrayList<DeviceContext> ctxs = new ArrayList<>();
        ctxs.add(DeviceContext.create(pctx, "device1"));
        ctxs.add(DeviceContext.create(pctx, "device2"));
        propValues.put("devices", ctxs);
        propValues.put("tempF", "80");
        engine.onCreateTasks(Collections.singletonList(new HobsonTask(
            TaskContext.createLocal("task1"),
            "My Task",
            null,
            null,
            Collections.singletonList(new PropertyContainer(
                    pccc,
                    propValues
            )),
            new PropertyContainerSet("actionset1", null)
        )));

        assertEquals(0, taskManager.getActionSetExecutions().size());

        engine.processEvent(new VariableUpdateNotificationEvent(System.currentTimeMillis(), new VariableUpdate(DeviceContext.create(pctx, "device1"), VariableConstants.INDOOR_TEMP_F, 81.0)));

        assertEquals(1, taskManager.getTaskExecutions().size());
        assertNotNull(taskManager.getTaskExecutions().get(0).getTaskId());

        engine.processEvent(new VariableUpdateNotificationEvent(System.currentTimeMillis(), new VariableUpdate(DeviceContext.create(pctx, "device1"), VariableConstants.INDOOR_TEMP_F, 79.0)));
        assertEquals(1, taskManager.getTaskExecutions().size());

        engine.processEvent(new VariableUpdateNotificationEvent(System.currentTimeMillis(), new VariableUpdate(DeviceContext.create(pctx, "device3"), VariableConstants.INDOOR_TEMP_F, 81.0)));
        assertEquals(1, taskManager.getTaskExecutions().size());

        engine.processEvent(new VariableUpdateNotificationEvent(System.currentTimeMillis(), new VariableUpdate(DeviceContext.create(pctx, "device2"), VariableConstants.INDOOR_TEMP_F, 81.0)));
        assertEquals(2, taskManager.getTaskExecutions().size());
    }

    protected void assertPrefix(JSONObject json) throws JSONException {
        assertEquals("Hobson Rules", json.getString("name"));
        assertEquals("Hobson Rules", json.getString("description"));
    }

    protected File createEmptyRulesFile() throws IOException {
        File rulesFile = File.createTempFile("hobson-rules", "json");
        rulesFile.deleteOnExit();

        BufferedWriter writer = new BufferedWriter(new FileWriter(rulesFile));
        writer.write(
                "{\n" +
                        "\t\"name\": \"Hobson Rules\",\n" +
                        "\t\"description\": \"Hobson Rules\",\n" +
                        "\t\"synonyms\": [\n" +
                        "\t],\n" +
                        "\t\"rules\": [\n" +
                        "\t]\n" +
                        "}"
        );
        writer.close();
        return rulesFile;
    }
}
