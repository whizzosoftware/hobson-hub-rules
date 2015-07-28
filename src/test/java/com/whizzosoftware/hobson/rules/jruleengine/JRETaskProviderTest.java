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
import com.whizzosoftware.hobson.api.task.HobsonTask;
import com.whizzosoftware.hobson.api.task.MockTaskManager;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import com.whizzosoftware.hobson.rules.RulesPlugin;
import com.whizzosoftware.hobson.rules.condition.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JRETaskProviderTest {
    @Test
    public void testEmptyRuleConstruction() throws Exception {
        JRETaskProvider provider = new JRETaskProvider(PluginContext.createLocal("pluginId"));

        // validate we start with a non-existent temp file
        File ruleFile = File.createTempFile("rules", ".json");
        assertTrue(ruleFile.delete());
        assertFalse(ruleFile.exists());

        try {
            provider.setRulesFile(ruleFile);

            // make sure the provider created a new rule file
            assertTrue(ruleFile.exists());
            JSONObject json = new JSONObject(new JSONTokener(new FileReader(ruleFile)));
            assertPrefix(json);

            assertFalse(json.has("rules"));
        } finally {
            assertTrue(ruleFile.delete());
        }
    }

    @Test
    public void testRuleConstruction() throws Exception {
        PluginContext pctx = PluginContext.createLocal("pluginId");
        MockTaskManager taskManager = new MockTaskManager();
        JRETaskProvider provider = new JRETaskProvider(pctx);
        provider.setTaskManager(taskManager);

        // validate we start with a non-existent temp file
        File ruleFile = File.createTempFile("rules", ".json");
        assertTrue(ruleFile.delete());
        assertFalse(ruleFile.exists());

        try {
            provider.setRulesFile(ruleFile);

            provider.onCreateTask(
                "My Task",
                    null, new PropertyContainerSet(
                    new PropertyContainer(
                        PropertyContainerClassContext.create(pctx, RulesPlugin.CONDITION_CLASS_TURN_OFF),
                        Collections.singletonMap("device", (Object) DeviceContext.createLocal("com.whizzosoftware.hobson.hobson-hub-zwave", "zwave-32"))
                    )
                ),
                new PropertyContainerSet("actionset1", null)
            );

            // make sure the provider updated the rule file
            assertTrue(ruleFile.exists());
            JSONObject jobj = new JSONObject(new JSONTokener(new FileReader(ruleFile)));
            assertPrefix(jobj);

            assertTrue(jobj.has("rules"));

            JSONArray rules = jobj.getJSONArray("rules");
            assertEquals(1, rules.length());
            JSONObject rule = rules.getJSONObject(0);
            assertNotNull(rule.get("name"));
            assertEquals("My Task", rule.get("description"));
            assertNotNull(rule.get("actions"));

            assertTrue(rule.has("assumptions"));
            JSONArray conditions = rule.getJSONArray("assumptions");
            assertEquals(5, conditions.length());
            JSONObject condition = conditions.getJSONObject(0);
            assertEquals(ConditionConstants.EVENT_ID, condition.getString("leftTerm"));
            assertEquals("=", condition.getString("op"));
            assertEquals(VariableUpdateNotificationEvent.ID, condition.getString("rightTerm"));
            condition = conditions.getJSONObject(1);
            assertEquals(ConditionConstants.PLUGIN_ID, condition.getString("leftTerm"));
            assertEquals("=", condition.getString("op"));
            assertEquals("com.whizzosoftware.hobson.hobson-hub-zwave", condition.getString("rightTerm"));
            condition = conditions.getJSONObject(2);
            assertEquals(ConditionConstants.DEVICE_ID, condition.getString("leftTerm"));
            assertEquals("=", condition.getString("op"));
            assertEquals("zwave-32", condition.getString("rightTerm"));
            condition = conditions.getJSONObject(3);
            assertEquals(ConditionConstants.VARIABLE_NAME, condition.getString("leftTerm"));
            assertEquals("=", condition.getString("op"));
            assertEquals("on", condition.getString("rightTerm"));
            condition = conditions.getJSONObject(4);
            assertEquals(ConditionConstants.VARIABLE_VALUE, condition.getString("leftTerm"));
            assertEquals("=", condition.getString("op"));
            assertEquals("false", condition.getString("rightTerm"));

            JSONArray actions = rule.getJSONArray("actions");
            assertEquals(1, actions.length());
            JSONObject action = actions.getJSONObject(0);
            assertEquals(ConditionConstants.EXECUTE_ACTIONSET, action.getString("method"));
            assertEquals("actionset1", action.getString("arg1"));
        } finally {
            assertTrue(ruleFile.delete());
        }
    }

    @Test
    public void testRuleExecution() throws Exception {
        MockTaskManager taskManager = new MockTaskManager();
        JRETaskProvider engine = new JRETaskProvider(PluginContext.createLocal("pluginId"));
        engine.setTaskManager(taskManager);
        String rulesJson = "{\n" +
            "\t\"name\": \"Hobson Rules\",\n" +
            "\t\"description\": \"Hobson Rules\",\n" +
            "\t\"synonyms\": [\n" +
            "\t\t{\n" +
            "\t\t\t\"name\": \"event\", \n" +
            "\t\t\t\"class\": \"com.whizzosoftware.hobson.rules.jruleengine.JREEventContext\"\n" +
            "\t\t},\n" +
            "\t\t{\n" +
            "\t\t\t\"name\": \"taskManager\", \n" +
            "\t\t\t\"class\": \"com.whizzosoftware.hobson.rules.jruleengine.JRETaskContext\"\n" +
            "\t\t}\n" +
            "\t],\n" +
            "\t\"rules\": [\n" +
            "\t\t{\n" +
            "\t\t\t\"name\": \"ruleid1\", \n" +
            "\t\t\t\"description\": \"test rule\",\n" +
            "\t\t\t\"assumptions\": [\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"leftTerm\": \"event.eventId\",\n" +
            "\t\t\t\t\t\"op\": \"=\",\n" +
            "\t\t\t\t\t\"rightTerm\": \"" + VariableUpdateNotificationEvent.ID + "\"\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"leftTerm\": \"event.pluginId\",\n" +
            "\t\t\t\t\t\"op\": \"=\",\n" +
            "\t\t\t\t\t\"rightTerm\": \"comwhizzosoftwarehobsonserver-zwave\"\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"leftTerm\": \"event.deviceId\",\n" +
            "\t\t\t\t\t\"op\": \"=\",\n" +
            "\t\t\t\t\t\"rightTerm\": \"zwave-32\"\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"leftTerm\": \"event.variableName\",\n" +
            "\t\t\t\t\t\"op\": \"=\",\n" +
            "\t\t\t\t\t\"rightTerm\": \"on\"\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"leftTerm\": \"event.variableValue\",\n" +
            "\t\t\t\t\t\"op\": \"=\",\n" +
            "\t\t\t\t\t\"rightTerm\": \"true\"\n" +
            "\t\t\t\t}\n" +
            "\t\t\t],\n" +
            "\t\t\t\"actions\": [\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"method\": \"taskManager.executeActionSet\",\n" +
            "\t\t\t\t\t\"arg1\": \"actionset1\",\n" +
            "\t\t\t\t}\n" +
            "\t\t\t]\n" +
            "\t\t}\n" +
            "\t]\n" +
            "}";
        engine.loadRules(new ByteArrayInputStream(rulesJson.getBytes()));

        Collection<HobsonTask> tasks = engine.getTasks();
        Assert.assertEquals(1, tasks.size());
        HobsonTask task = tasks.iterator().next();
        Assert.assertNotNull("ruleid1", task.getContext().getTaskId());
        Assert.assertEquals("test rule", task.getName());
        Assert.assertTrue(task.getActionSet().hasId());
        Assert.assertEquals("actionset1", task.getActionSet().getId());

        Assert.assertEquals(0, taskManager.getActionSetExecutions().size());

        engine.processEvent(new VariableUpdateNotificationEvent(System.currentTimeMillis(), new VariableUpdate(DeviceContext.createLocal("comwhizzosoftwarehobsonserver-zwave", "zwave-32"), VariableConstants.ON, true)));

        Assert.assertEquals(1, taskManager.getActionSetExecutions().size());
        Assert.assertEquals("actionset1", taskManager.getActionSetExecutions().get(0));
    }

    @Test
    public void testRuleFileWrite() throws Exception {
        File rulesFile = createEmptyRulesFile();
        PluginContext ctx = PluginContext.createLocal("plugin1");
        MockTaskManager taskManager = new MockTaskManager();
        JRETaskProvider engine = new JRETaskProvider(ctx);
        engine.setTaskManager(taskManager);
        engine.setRulesFile(rulesFile);

        engine.onCreateTask(
            "New Task",
                null, new PropertyContainerSet(
                new PropertyContainer(
                    PropertyContainerClassContext.create(ctx, RulesPlugin.CONDITION_CLASS_TURN_ON),
                    Collections.singletonMap("device", (Object)DeviceContext.createLocal("plugin2", "device1"))
                )
            ),
            new PropertyContainerSet(
                "actionset1",
                null
            )
        );

        JSONObject json = new JSONObject(new JSONTokener(new FileInputStream(rulesFile)));

        // test prefix
        assertPrefix(json);

        // test rule meta
        assertTrue(json.has("rules"));
        JSONArray rules = json.getJSONArray("rules");
        assertEquals(1, rules.length());
        JSONObject rule = rules.getJSONObject(0);
        assertNotNull(rule.getString("name"));
        assertEquals("New Task", rule.getString("description"));

        // test rule assumptions
        assertTrue(rule.has("assumptions"));
        JSONArray assumptions = rule.getJSONArray("assumptions");
        assertEquals(5, assumptions.length());
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
        MockTaskManager taskManager = new MockTaskManager();
        JRETaskProvider engine = new JRETaskProvider(ctx);
        engine.setRulesFile(rulesFile);
        engine.setTaskManager(taskManager);

        PluginContext pctx = PluginContext.createLocal("plugin");

        Map<String,Object> propValues = new HashMap<>();
        propValues.put("device", DeviceContext.create(pctx, "device"));
        propValues.put("tempF", "80");
        engine.onCreateTask(
                "My Task",
                null,
                new PropertyContainerSet(
                        new PropertyContainer(
                                PropertyContainerClassContext.create(pctx, RulesPlugin.CONDITION_CLASS_TEMP_ABOVE),
                                propValues
                        )
                ),
                new PropertyContainerSet("actionset1", null)
        );

        assertEquals(0, taskManager.getActionSetExecutions().size());

        engine.processEvent(new VariableUpdateNotificationEvent(System.currentTimeMillis(), new VariableUpdate(DeviceContext.create(pctx, "device"), VariableConstants.TEMP_F, 81.0)));
        assertEquals(1, taskManager.getActionSetExecutions().size());
        assertEquals("actionset1", taskManager.getActionSetExecutions().get(0));

        engine.processEvent(new VariableUpdateNotificationEvent(System.currentTimeMillis(), new VariableUpdate(DeviceContext.create(pctx, "device"), VariableConstants.TEMP_F, 79.0)));
        assertEquals(1, taskManager.getActionSetExecutions().size());
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
