/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.action.HobsonActionRef;
import com.whizzosoftware.hobson.api.task.HobsonTask;
import org.jruleengine.rule.Action;
import org.jruleengine.rule.Assumption;
import org.jruleengine.rule.RuleImpl;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Map;

public class JRETaskTest {
    @Test
    public void testEmptyRuleConstructor() throws Exception {
        ArrayList assumps = new ArrayList();
        ArrayList actions = new ArrayList();
        RuleImpl rule = new RuleImpl("rule1", "rule1desc", assumps, actions, true);
        JRETask trig = new JRETask("providerId", rule);
        assertNotNull("rule1", trig.getId());
        assertEquals("rule1desc", trig.getName());
        assertEquals(0, trig.getConditions().size());
        assertEquals(0, trig.getActions().size());
    }

    @Test
    public void testSingleRuleConstructor() throws Exception {
        ArrayList assumps = new ArrayList();
        assumps.add(new Assumption("event.eventId", "=", "variableUpdate"));
        assumps.add(new Assumption("event.pluginId", "=", "com.whizzosoftware.hobson.server-zwave"));
        assumps.add(new Assumption("event.deviceId", "=", "zwave-32"));
        assumps.add(new Assumption("event.variableName", "=", "on"));
        assumps.add(new Assumption("event.variableValue", "=", "true"));
        ArrayList actions = new ArrayList();
        ArrayList params = new ArrayList();
        params.add("val1");
        params.add("val2");
        params.add("val3");
        params.add("{'foo':'bar'}");
        actions.add(new Action("method", params));
        RuleImpl rule = new RuleImpl("rule1", "rule1desc", assumps, actions, true);

        JRETask trig = new JRETask("providerId", rule);

        assertNotNull("rule1", trig.getId());
        assertEquals("rule1desc", trig.getName());

        assertEquals(1, trig.getConditions().size());
        Map<String,Object> map = trig.getConditions().iterator().next();
        assertEquals("variableUpdate", map.get("event"));
        assertEquals("com.whizzosoftware.hobson.server-zwave", map.get("pluginId"));
        assertEquals("zwave-32", map.get("deviceId"));
        assertEquals("on", map.get("name"));
        assertEquals("=", map.get("comparator"));
        assertEquals(true, map.get("value"));

        assertEquals(1, trig.getActions().size());
        HobsonActionRef ref = trig.getActions().iterator().next();
        assertEquals("val1", ref.getPluginId());
        assertEquals("val2", ref.getActionId());
        assertEquals("val3", ref.getName());
        assertEquals(1, ref.getProperties().size());
        assertEquals("bar", ref.getProperties().get("foo"));
    }

    @Test
    public void testJsonWithVariableComparatorConstructor() throws Exception {
        String json = "{'name':'rule1','conditions':[{'event':'variableUpdate','pluginId':'com.whizzosoftware.hobson.server-zwave','deviceId':'zwave-32','variable':{'name':'on','comparator':'=','value':false}}],'actions':[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'log'}}]}";
        JSONObject jsonObj = new JSONObject(new JSONTokener(json));

        JRETask task = new JRETask("provider1", jsonObj);

        assertEquals("provider1", task.getProviderId());
        assertNotNull(task.getId());
        assertEquals("rule1", task.getName());
        assertEquals(HobsonTask.Type.EVENT, task.getType());

        assertEquals(1, task.getConditions().size());
        Map<String,Object> map = task.getConditions().iterator().next();
        assertEquals("variableUpdate", map.get("event"));
        assertEquals("com.whizzosoftware.hobson.server-zwave", map.get("pluginId"));
        assertEquals("zwave-32", map.get("deviceId"));
        assertEquals("on", map.get("name"));
        assertEquals("=", map.get("comparator"));
        assertEquals(false, map.get("value"));

        assertEquals(1, task.getActions().size());
        HobsonActionRef ref = task.getActions().iterator().next();
        assertEquals("com.whizzosoftware.hobson.server-api", ref.getPluginId());
        assertEquals("log", ref.getActionId());
        assertEquals(1, ref.getProperties().size());
        assertEquals("log", ref.getProperties().get("message"));
    }

    @Test
    public void testJsonWithChangeIdConstructor() throws Exception {
        String json = "{'name':'rule1','conditions':[{'event':'variableUpdate','pluginId':'com.whizzosoftware.hobson.server-zwave','deviceId':'zwave-32','changeId':'turnOff'}],'actions':[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'log'}}]}";
        JSONObject jsonObj = new JSONObject(new JSONTokener(json));

        JRETask task = new JRETask("provider1", jsonObj);

        assertEquals("provider1", task.getProviderId());
        assertNotNull(task.getId());
        assertEquals("rule1", task.getName());
        assertEquals(HobsonTask.Type.EVENT, task.getType());

        assertEquals(1, task.getConditions().size());
        Map<String,Object> map = task.getConditions().iterator().next();
        assertEquals("variableUpdate", map.get("event"));
        assertEquals("com.whizzosoftware.hobson.server-zwave", map.get("pluginId"));
        assertEquals("zwave-32", map.get("deviceId"));
        assertEquals("on", map.get("name"));
        assertEquals("=", map.get("comparator"));
        assertEquals(false, map.get("value"));

        assertEquals(1, task.getActions().size());
        HobsonActionRef ref = task.getActions().iterator().next();
        assertEquals("com.whizzosoftware.hobson.server-api", ref.getPluginId());
        assertEquals("log", ref.getActionId());
        assertEquals(1, ref.getProperties().size());
        assertEquals("log", ref.getProperties().get("message"));
    }
}
