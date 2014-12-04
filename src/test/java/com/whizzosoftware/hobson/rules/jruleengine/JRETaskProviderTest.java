/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.action.HobsonActionRef;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.task.HobsonTask;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import com.whizzosoftware.hobson.rules.MockActionManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.util.Collection;

public class JRETaskProviderTest {
    @Test
    public void testEmptyRuleConstruction() throws Exception {
        JRETaskProvider provider = new JRETaskProvider("pluginId");

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

            assertTrue(json.has("synonyms"));
            assertSynonyms(json.getJSONArray("synonyms"));

            assertFalse(json.has("rules"));
        } finally {
            assertTrue(ruleFile.delete());
        }
    }

    @Test
    public void testJSONRuleConstruction() throws Exception {
        JRETaskProvider provider = new JRETaskProvider("pluginId");

        // validate we start with a non-existent temp file
        File ruleFile = File.createTempFile("rules", ".json");
        assertTrue(ruleFile.delete());
        assertFalse(ruleFile.exists());

        try {
            provider.setRulesFile(ruleFile);
            JSONObject json = new JSONObject(new JSONTokener("{'name':'My Task','conditions':[{'event':'variableUpdate','pluginId':'com.whizzosoftware.hobson.hobson-hub-zwave','deviceId':'zwave-32','variable':{'name':'on','comparator':'=','value':false}}],'actions':[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'log'}}]}"));
            provider.addTask(json);

            // make sure the provider updated the rule file
            assertTrue(ruleFile.exists());
            JSONObject jobj = new JSONObject(new JSONTokener(new FileReader(ruleFile)));
            assertPrefix(jobj);

            assertTrue(jobj.has("synonyms"));
            assertSynonyms(jobj.getJSONArray("synonyms"));

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
            assertEquals("event.eventId", condition.getString("leftTerm"));
            assertEquals("=", condition.getString("op"));
            assertEquals("variableUpdate", condition.getString("rightTerm"));
            condition = conditions.getJSONObject(1);
            assertEquals("event.pluginId", condition.getString("leftTerm"));
            assertEquals("=", condition.getString("op"));
            assertEquals("com.whizzosoftware.hobson.hobson-hub-zwave", condition.getString("rightTerm"));
            condition = conditions.getJSONObject(2);
            assertEquals("event.deviceId", condition.getString("leftTerm"));
            assertEquals("=", condition.getString("op"));
            assertEquals("zwave-32", condition.getString("rightTerm"));
            condition = conditions.getJSONObject(3);
            assertEquals("event.variableName", condition.getString("leftTerm"));
            assertEquals("=", condition.getString("op"));
            assertEquals("on", condition.getString("rightTerm"));
            condition = conditions.getJSONObject(4);
            assertEquals("event.variableValue", condition.getString("leftTerm"));
            assertEquals("=", condition.getString("op"));
            assertEquals("false", condition.getString("rightTerm"));

            JSONArray actions = rule.getJSONArray("actions");
            assertEquals(1, actions.length());
            JSONObject action = actions.getJSONObject(0);
            assertEquals("actions.executeAction", action.getString("method"));
            assertEquals("com.whizzosoftware.hobson.server-api", action.getString("arg1"));
            assertEquals("log", action.getString("arg2"));
            assertEquals("My Action", action.getString("arg3"));
            assertEquals("{\"message\":\"log\"}", action.getString("arg4"));
        } finally {
            assertTrue(ruleFile.delete());
        }
    }

    @Test
    public void testRuleExecution() throws Exception {
        MockActionManager actionContext = new MockActionManager();
        JRETaskProvider engine = new JRETaskProvider("pluginId");
        engine.setActionManager(actionContext);
        String rulesJson = "{\n" +
                "\t\"name\": \"Hobson Rules\",\n" +
                "\t\"description\": \"Hobson Rules\",\n" +
                "\t\"synonyms\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"name\": \"event\", \n" +
                "\t\t\t\"class\": \"com.whizzosoftware.hobson.rules.jruleengine.JREEventContext\"\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"name\": \"actions\", \n" +
                "\t\t\t\"class\": \"com.whizzosoftware.hobson.rules.jruleengine.JREActionContext\"\n" +
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
                "\t\t\t\t\t\"rightTerm\": \"variableUpdate\"\n" +
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
                "\t\t\t\t\t\"method\": \"actions.executeAction\",\n" +
                "\t\t\t\t\t\"arg1\": \"com.whizzosoftware.hobson.server-api\",\n" +
                "\t\t\t\t\t\"arg2\": \"setVariable\",\n" +
                "\t\t\t\t\t\"arg3\": \"My Action\",\n" +
                "\t\t\t\t\t\"arg4\": \"{'pluginId':'com.whizzosoftware.hobson.server-zwave','deviceId':'zwave-32','variableName':'on','value':false}\",\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";
        engine.loadRules(new ByteArrayInputStream(rulesJson.getBytes()));

        Collection<HobsonTask> tasks = engine.getTasks();
        Assert.assertEquals(1, tasks.size());
        HobsonTask task = tasks.iterator().next();
        Assert.assertNotNull("ruleid1", task.getId());
        Assert.assertEquals("test rule", task.getName());
        Assert.assertEquals(1, task.getActions().size());
        HobsonActionRef ref = task.getActions().iterator().next();
        Assert.assertEquals("com.whizzosoftware.hobson.server-api", ref.getPluginId());
        Assert.assertEquals("setVariable", ref.getActionId());

        actionContext.reset();
        Assert.assertEquals(0, actionContext.getSetVariableCount());

        engine.processEvent(new VariableUpdateNotificationEvent(new VariableUpdate("comwhizzosoftwarehobsonserver-zwave", "zwave-32", VariableConstants.ON, true)));

        Assert.assertEquals(1, actionContext.getSetVariableCount());
    }

    protected void assertPrefix(JSONObject json) throws JSONException {
        assertEquals("Hobson Rules", json.getString("name"));
        assertEquals("Hobson Rules", json.getString("description"));
    }

    protected void assertSynonyms(JSONArray synonyms) throws JSONException {
        assertEquals(2, synonyms.length());
        assertEquals("event", synonyms.getJSONObject(0).getString("name"));
        assertEquals("com.whizzosoftware.hobson.rules.jruleengine.JREEventContext", synonyms.getJSONObject(0).getString("class"));
        assertEquals("actions", synonyms.getJSONObject(1).getString("name"));
        assertEquals("com.whizzosoftware.hobson.rules.jruleengine.JREActionContext", synonyms.getJSONObject(1).getString("class"));

    }
}
