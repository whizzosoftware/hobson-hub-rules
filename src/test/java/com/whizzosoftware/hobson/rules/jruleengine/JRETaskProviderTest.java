/*
 *******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.event.device.DeviceUnavailableEvent;
import com.whizzosoftware.hobson.api.event.presence.PresenceUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.event.device.DeviceVariablesUpdateEvent;
import com.whizzosoftware.hobson.api.hub.HubContext;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.presence.PresenceEntityContext;
import com.whizzosoftware.hobson.api.presence.PresenceLocationContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerSet;
import com.whizzosoftware.hobson.api.task.*;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClass;
import com.whizzosoftware.hobson.api.variable.DeviceVariableUpdate;
import com.whizzosoftware.hobson.api.variable.DeviceVariableContext;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
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
        DeviceTurnsOffConditionClass pcc = new DeviceTurnsOffConditionClass(pctx);
        final MockTaskManager taskManager = createMockTaskManager(pcc);

        // validate we start with a non-existent temp file
        File ruleFile = File.createTempFile("rules", ".json");
        assertTrue(ruleFile.delete());
        assertFalse(ruleFile.exists());

        JRETaskProvider provider = new JRETaskProvider(pctx, taskManager);
        provider.setTaskManager(taskManager);
        provider.setRulesFile(ruleFile);

        createTask(taskManager, provider, HubContext.createLocal(), Collections.singletonList(
            new PropertyContainer(
                pcc.getContext(),
                Collections.singletonMap("devices", (Object) Collections.singletonList(DeviceContext.createLocal("com.whizzosoftware.hobson.hobson-hub-zwave", "zwave-32")))
            )
        ));

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
        assertEquals(DeviceVariablesUpdateEvent.ID, condition.getString("rightTerm"));
        condition = conditions.getJSONObject(1);
        assertEquals(ConditionConstants.DEVICE_CTX, condition.getString("leftTerm"));
        assertEquals("containsatleastone", condition.getString("op"));
        assertEquals("[local:com.whizzosoftware.hobson.hobson-hub-zwave:zwave-32]", condition.getString("rightTerm"));
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
        assertTrue(action.getString("arg1").startsWith("local:"));
    }

//    @Test
//    public void testRuleExecution() throws Exception {
//        final MockTaskManager taskManager = new MockTaskManager();
//        JRETaskProvider engine = new JRETaskProvider(PluginContext.createLocal("pluginId"), new MockTaskManagerConditionClassProvider(taskManager));
//        engine.setTaskManager(taskManager);
//        String rulesJson = "{\n" +
//            "\t\"name\": \"Hobson Rules\",\n" +
//            "\t\"description\": \"Hobson Rules\",\n" +
//            "\t\"rules\": [\n" +
//            "\t\t{\n" +
//            "\t\t\t\"name\": \"ruleid1\", \n" +
//            "\t\t\t\"description\": \"test rule\",\n" +
//            "\t\t\t\"assumptions\": [\n" +
//            "\t\t\t\t{\n" +
//            "\t\t\t\t\t\"leftTerm\": \"com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.eventId\",\n" +
//            "\t\t\t\t\t\"op\": \"=\",\n" +
//            "\t\t\t\t\t\"rightTerm\": \"" + VariableUpdateNotificationEvent.ID + "\"\n" +
//            "\t\t\t\t},\n" +
//            "\t\t\t\t{\n" +
//            "\t\t\t\t\t\"leftTerm\": \"com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.deviceCtx\",\n" +
//            "\t\t\t\t\t\"op\": \"=\",\n" +
//            "\t\t\t\t\t\"rightTerm\": \"local:local:comwhizzosoftwarehobsonserver-zwave:zwave-32\"\n" +
//            "\t\t\t\t},\n" +
//            "\t\t\t\t{\n" +
//            "\t\t\t\t\t\"leftTerm\": \"com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.variableName\",\n" +
//            "\t\t\t\t\t\"op\": \"=\",\n" +
//            "\t\t\t\t\t\"rightTerm\": \"on\"\n" +
//            "\t\t\t\t},\n" +
//            "\t\t\t\t{\n" +
//            "\t\t\t\t\t\"leftTerm\": \"com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.variableValue\",\n" +
//            "\t\t\t\t\t\"op\": \"=\",\n" +
//            "\t\t\t\t\t\"rightTerm\": \"true\"\n" +
//            "\t\t\t\t}\n" +
//            "\t\t\t],\n" +
//            "\t\t\t\"actions\": [\n" +
//            "\t\t\t\t{\n" +
//            "\t\t\t\t\t\"method\": \"com.whizzosoftware.hobson.rules.jruleengine.JRETaskContext.fireTaskTrigger\",\n" +
//            "\t\t\t\t\t\"arg1\": \"local:local:task1\",\n" +
//            "\t\t\t\t}\n" +
//            "\t\t\t]\n" +
//            "\t\t}\n" +
//            "\t]\n" +
//            "}";
//        engine.loadRules(new ByteArrayInputStream(rulesJson.getBytes()));
//
//        Collection<JRETask> tasks = engine.getTasks();
//        assertEquals(1, tasks.size());
//        JRETask task = tasks.iterator().next();
//        assertNotNull("ruleid1", task.getContext().getTaskId());
//
//        assertEquals(0, taskManager.getActionSetExecutions().size());
//
//        engine.processEvent(new VariableUpdateNotificationEvent(System.currentTimeMillis(), new VariableUpdate(DeviceContext.createLocal("comwhizzosoftwarehobsonserver-zwave", "zwave-32"), VariableConstants.ON, true)));
//
//        assertEquals(1, taskManager.getTaskExecutions().size());
//        assertEquals("task1", taskManager.getTaskExecutions().get(0).getTaskId());
//    }

    @Test
    public void testRuleFileWrite() throws Exception {
        PluginContext ctx = PluginContext.createLocal("plugin1");
        DeviceTurnsOnConditionClass pcc = new DeviceTurnsOnConditionClass(ctx);

        File rulesFile = createEmptyRulesFile();
        final MockTaskManager taskManager = createMockTaskManager(pcc);

        JRETaskProvider engine = new JRETaskProvider(ctx, taskManager);
        engine.setTaskManager(taskManager);
        engine.setRulesFile(rulesFile);

        List<TaskContext> tasks = new ArrayList<>();
        taskManager.createTask(HubContext.createLocal(), "task1", "My task1", Collections.singletonList(new PropertyContainer(pcc.getContext(), Collections.singletonMap("devices", (Object)Collections.singletonList(DeviceContext.createLocal("plugin2", "device1"))))), new PropertyContainerSet("actionset1", null));
        taskManager.createTask(HubContext.createLocal(), "task2", "My task2", Collections.singletonList(new PropertyContainer(pcc.getContext(), Collections.singletonMap("devices", (Object)Collections.singletonList(DeviceContext.createLocal("plugin2", "device1"))))), new PropertyContainerSet("actionset1", null));
        for (HobsonTask task : taskManager.getTasks(HubContext.createLocal())) {
            tasks.add(task.getContext());
        }
        engine.onRegisterTasks(tasks);

        JSONObject json = new JSONObject(new JSONTokener(new FileInputStream(rulesFile)));

        // test prefix
        assertPrefix(json);

        // test rule meta
        assertTrue(json.has("rules"));
        JSONArray rules = json.getJSONArray("rules");
        assertEquals(2, rules.length());

        // test first rule
        JSONObject rule = rules.getJSONObject(0);
        assertNotNull(rule.getString("name"));
        assertNotNull(rule.getString("description"));

        // test rule assumptions
        assertTrue(rule.has("assumptions"));
        JSONArray assumptions = rule.getJSONArray("assumptions");
        assertEquals(4, assumptions.length());
        assertEquals(ConditionConstants.EVENT_ID, assumptions.getJSONObject(0).getString("leftTerm"));

        // test rule actions
        assertTrue(rule.has("actions"));
        JSONArray actions = rule.getJSONArray("actions");
        assertEquals(1, actions.length());

        // test second rule
        rule = rules.getJSONObject(1);
        assertNotNull(rule.getString("name"));
        assertNotNull(rule.getString("description"));

        // test rule assumptions
        assertTrue(rule.has("assumptions"));
        assumptions = rule.getJSONArray("assumptions");
        assertEquals(4, assumptions.length());
        assertEquals(ConditionConstants.EVENT_ID, assumptions.getJSONObject(0).getString("leftTerm"));

        // test rule actions
        assertTrue(rule.has("actions"));
        actions = rule.getJSONArray("actions");
        assertEquals(1, actions.length());

        // create a third task
        taskManager.createTask(HubContext.createLocal(), "My task3", null, Collections.singletonList(new PropertyContainer(pcc.getContext(), Collections.singletonMap("devices", (Object)Collections.singletonList(DeviceContext.createLocal("plugin2", "device1"))))), new PropertyContainerSet("actionset1", null));
        for (HobsonTask task : taskManager.getTasks(HubContext.createLocal())) {
            if (!tasks.contains(task.getContext())) {
                engine.onRegisterTasks(Collections.singletonList(task.getContext()));
            }
        }
        json = new JSONObject(new JSONTokener(new FileInputStream(rulesFile)));

        // test prefix
        assertPrefix(json);

        // test rule meta
        assertTrue(json.has("rules"));
        rules = json.getJSONArray("rules");
        assertEquals(3, rules.length());
    }

    @Test
    public void testProcessEventForTemperatureOutOfRange() throws Exception {
        File rulesFile = createEmptyRulesFile();
        PluginContext ctx = PluginContext.createLocal("plugin1");
        DeviceIndoorTempAboveConditionClass pcc = new DeviceIndoorTempAboveConditionClass(ctx);

        final MockTaskManager taskManager = createMockTaskManager(pcc);

        JRETaskProvider engine = new JRETaskProvider(ctx, taskManager);
        engine.setTaskManager(taskManager);
        engine.setRulesFile(rulesFile);

        PluginContext pctx = PluginContext.createLocal("plugin");

        Map<String,Object> propValues = new HashMap<>();
        ArrayList<DeviceContext> ctxs = new ArrayList<>();
        ctxs.add(DeviceContext.create(pctx, "device1"));
        ctxs.add(DeviceContext.create(pctx, "device2"));
        propValues.put("devices", ctxs);
        propValues.put("inTempF", "80");
        createTask(taskManager, engine, HubContext.createLocal(), Collections.singletonList(new PropertyContainer(
            pcc.getContext(),
            propValues
        )));

        assertEquals(0, taskManager.getActionSetExecutions().size());

        engine.processEvent(new DeviceVariablesUpdateEvent(System.currentTimeMillis(), new DeviceVariableUpdate(DeviceVariableContext.create(pctx, "device1", VariableConstants.INDOOR_TEMP_F), null, 81.0)));

        assertEquals(1, taskManager.getTaskExecutions().size());
        assertNotNull(taskManager.getTaskExecutions().get(0).getTaskId());

        engine.processEvent(new DeviceVariablesUpdateEvent(System.currentTimeMillis(), new DeviceVariableUpdate(DeviceVariableContext.create(pctx, "device1", VariableConstants.INDOOR_TEMP_F), null, 79.0)));
        assertEquals(1, taskManager.getTaskExecutions().size());

        engine.processEvent(new DeviceVariablesUpdateEvent(System.currentTimeMillis(), new DeviceVariableUpdate(DeviceVariableContext.create(pctx, "device3", VariableConstants.INDOOR_TEMP_F), null, 81.0)));
        assertEquals(1, taskManager.getTaskExecutions().size());

        engine.processEvent(new DeviceVariablesUpdateEvent(System.currentTimeMillis(), new DeviceVariableUpdate(DeviceVariableContext.create(pctx, "device2", VariableConstants.INDOOR_TEMP_F), null, 81.0)));
        assertEquals(2, taskManager.getTaskExecutions().size());
    }

    @Test
    public void testProcessEventForDeviceUnavailable() throws Exception {
        File rulesFile = createEmptyRulesFile();
        PluginContext ctx = PluginContext.createLocal("plugin1");
        DeviceUnavailableConditionClass pcc = new DeviceUnavailableConditionClass(ctx);

        final MockTaskManager taskManager = createMockTaskManager(pcc);

        JRETaskProvider engine = new JRETaskProvider(ctx, taskManager);
        engine.setTaskManager(taskManager);
        engine.setRulesFile(rulesFile);

        PluginContext pctx = PluginContext.createLocal("plugin");

        Map<String,Object> propValues = new HashMap<>();
        ArrayList<DeviceContext> ctxs = new ArrayList<>();
        ctxs.add(DeviceContext.create(pctx, "device1"));
        propValues.put("devices", ctxs);
        createTask(taskManager, engine, HubContext.createLocal(), Collections.singletonList(new PropertyContainer(
            pcc.getContext(),
            propValues
        )));

        assertEquals(0, taskManager.getActionSetExecutions().size());

        engine.processEvent(new DeviceUnavailableEvent(System.currentTimeMillis(), DeviceContext.create(pctx, "device1")));

        assertEquals(1, taskManager.getTaskExecutions().size());
        assertNotNull(taskManager.getTaskExecutions().get(0).getTaskId());
    }

    @Test
    public void testProcessEventForPresenceArrival() throws Exception {
        File rulesFile = createEmptyRulesFile();
        PluginContext ctx = PluginContext.createLocal("plugin1");
        PresenceArrivalConditionClass pcc = new PresenceArrivalConditionClass(ctx);

        final MockTaskManager taskManager = createMockTaskManager(pcc);
        taskManager.publishConditionClass(new PresenceArrivalConditionClass(ctx));

        JRETaskProvider engine = new JRETaskProvider(ctx, taskManager);
        engine.setTaskManager(taskManager);
        engine.setRulesFile(rulesFile);

        Map<String,Object> propValues = new HashMap<>();
        propValues.put("person", PresenceEntityContext.createLocal("person1"));
        propValues.put("location", PresenceLocationContext.createLocal("location2"));

        createTask(taskManager, engine, HubContext.createLocal(), Collections.singletonList(new PropertyContainer(
            pcc.getContext(),
            propValues
        )));

        assertEquals(0, taskManager.getActionSetExecutions().size());

        // send a matching event
        engine.processEvent(new PresenceUpdateNotificationEvent(
            System.currentTimeMillis(),
            PresenceEntityContext.createLocal("person1"),
            PresenceLocationContext.createLocal("location1"),
            PresenceLocationContext.createLocal("location2")
        ));
        assertEquals(1, taskManager.getTaskExecutions().size());
        assertNotNull(taskManager.getTaskExecutions().get(0).getTaskId());

        // send an event with the wrong person
        taskManager.getTaskExecutions().clear();
        assertEquals(0, taskManager.getTaskExecutions().size());
        engine.processEvent(new PresenceUpdateNotificationEvent(
                System.currentTimeMillis(),
                PresenceEntityContext.createLocal("person2"),
                PresenceLocationContext.createLocal("location1"),
                PresenceLocationContext.createLocal("location2")
        ));
        assertEquals(0, taskManager.getTaskExecutions().size());

        // send an event with wrong new location
        taskManager.getTaskExecutions().clear();
        assertEquals(0, taskManager.getTaskExecutions().size());
        engine.processEvent(new PresenceUpdateNotificationEvent(
                System.currentTimeMillis(),
                PresenceEntityContext.createLocal("person1"),
                PresenceLocationContext.createLocal("location1"),
                PresenceLocationContext.createLocal("location3")
        ));
        assertEquals(0, taskManager.getTaskExecutions().size());

        // send an event with identical old/new locations
        taskManager.getTaskExecutions().clear();
        assertEquals(0, taskManager.getTaskExecutions().size());
        engine.processEvent(new PresenceUpdateNotificationEvent(
                System.currentTimeMillis(),
                PresenceEntityContext.createLocal("person1"),
                PresenceLocationContext.createLocal("location1"),
                PresenceLocationContext.createLocal("location1")
        ));
        assertEquals(0, taskManager.getTaskExecutions().size());
    }

    @Test
    public void testProcessEventForPresenceDeparture() throws Exception {
        File rulesFile = createEmptyRulesFile();
        PluginContext ctx = PluginContext.createLocal("plugin1");
        PresenceDepartureConditionClass pcc = new PresenceDepartureConditionClass(ctx);

        final MockTaskManager taskManager = createMockTaskManager(pcc);

        JRETaskProvider engine = new JRETaskProvider(ctx, taskManager);
        engine.setTaskManager(taskManager);
        engine.setRulesFile(rulesFile);

        Map<String,Object> propValues = new HashMap<>();
        propValues.put("person", PresenceEntityContext.createLocal("person1"));
        propValues.put("location", PresenceLocationContext.createLocal("location2"));

        createTask(taskManager, engine, HubContext.createLocal(), Collections.singletonList(new PropertyContainer(
                pcc.getContext(),
                propValues
        )));

        assertEquals(0, taskManager.getActionSetExecutions().size());

        // send a matching event
        engine.processEvent(new PresenceUpdateNotificationEvent(
                System.currentTimeMillis(),
                PresenceEntityContext.createLocal("person1"),
                PresenceLocationContext.createLocal("location2"),
                PresenceLocationContext.createLocal("location3")
        ));
        assertEquals(1, taskManager.getTaskExecutions().size());
        assertNotNull(taskManager.getTaskExecutions().get(0).getTaskId());

        // send an event with the wrong person
        taskManager.getTaskExecutions().clear();
        assertEquals(0, taskManager.getTaskExecutions().size());
        engine.processEvent(new PresenceUpdateNotificationEvent(
                System.currentTimeMillis(),
                PresenceEntityContext.createLocal("person2"),
                PresenceLocationContext.createLocal("location1"),
                PresenceLocationContext.createLocal("location2")
        ));
        assertEquals(0, taskManager.getTaskExecutions().size());

        // send an event with wrong new location
        taskManager.getTaskExecutions().clear();
        assertEquals(0, taskManager.getTaskExecutions().size());
        engine.processEvent(new PresenceUpdateNotificationEvent(
                System.currentTimeMillis(),
                PresenceEntityContext.createLocal("person1"),
                PresenceLocationContext.createLocal("location3"),
                PresenceLocationContext.createLocal("location1")
        ));
        assertEquals(0, taskManager.getTaskExecutions().size());

        // send an event with identical old/new locations
        taskManager.getTaskExecutions().clear();
        assertEquals(0, taskManager.getTaskExecutions().size());
        engine.processEvent(new PresenceUpdateNotificationEvent(
                System.currentTimeMillis(),
                PresenceEntityContext.createLocal("person1"),
                PresenceLocationContext.createLocal("location2"),
                PresenceLocationContext.createLocal("location2")
        ));
        assertEquals(0, taskManager.getTaskExecutions().size());
    }

    private void assertPrefix(JSONObject json) throws JSONException {
        assertEquals("Hobson Rules", json.getString("name"));
        assertEquals("Hobson Rules", json.getString("description"));
    }

    private File createEmptyRulesFile() throws IOException {
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

    private void createTask(TaskManager manager, TaskProvider provider, HubContext hctx, List<PropertyContainer> conditions) {
        manager.createTask(hctx, "My Task", null, conditions, new PropertyContainerSet("actionset1", null));
        List<TaskContext> ctxs = new ArrayList<>();
        Collection<HobsonTask> tasks = manager.getTasks(hctx);
        for (HobsonTask task : tasks) {
            ctxs.add(task.getContext());
        }
        provider.onRegisterTasks(ctxs);
    }

    private MockTaskManager createMockTaskManager(TaskConditionClass tcc) {
        MockTaskManager mgr = new MockTaskManager();
        mgr.publishConditionClass(tcc);
        return mgr;
    }
}
