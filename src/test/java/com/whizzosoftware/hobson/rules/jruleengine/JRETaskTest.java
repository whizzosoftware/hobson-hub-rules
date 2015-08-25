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
import com.whizzosoftware.hobson.api.hub.HubContext;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.rules.RulesPlugin;
import com.whizzosoftware.hobson.rules.condition.*;
import org.jruleengine.rule.Action;
import org.jruleengine.rule.Assumption;
import org.jruleengine.rule.RuleImpl;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class JRETaskTest {
    @Test
    public void testEmptyRuleConstructor() throws Exception {
        ArrayList assumps = new ArrayList();
        ArrayList actions = new ArrayList();
        RuleImpl rule = new RuleImpl("rule1", "rule1desc", assumps, actions, true);
        try {
            new JRETask(PluginContext.createLocal("plugin1"), rule);
            fail("Should have thrown exception");
        } catch (HobsonRuntimeException ignored) {
        }
    }

    @Test
    public void testRuleConstructorWithTurnOffTrigger() throws Exception {
        HubContext hctx = HubContext.createLocal();
        PluginContext pctx = PluginContext.create(hctx, "plugin1");

        // create assumptions
        ArrayList assumps = new ArrayList();
        assumps.add(new Assumption(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
        assumps.add(new Assumption(ConditionConstants.DEVICE_CTX, "containsatleastone", "[local:local:com.whizzosoftware.hobson.server-zwave:zwave-32]"));
        assumps.add(new Assumption(ConditionConstants.VARIABLE_NAME, "=", "on"));
        assumps.add(new Assumption(ConditionConstants.VARIABLE_VALUE, "=", "false"));

        // create actions
        ArrayList actions = new ArrayList();
        actions.add(new Action(ConditionConstants.SET_ACTIONSET, Collections.singletonList("actionset1")));
        actions.add(new Action(ConditionConstants.FIRE_TRIGGER, Collections.singletonList(TaskContext.create(hctx, "task1"))));

        // create JRE rule
        RuleImpl rule = new RuleImpl("ruleId", "rule1desc", assumps, actions, true);

        JRETask task = new JRETask(pctx, rule);

        assertEquals("ruleId", task.getContext().getTaskId());

        assertNotNull(task.getTriggerCondition());
        assertEquals(RulesPlugin.CONDITION_CLASS_TURN_OFF, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
        Collection<DeviceContext> ctxs = (Collection<DeviceContext>)task.getTriggerCondition().getPropertyValue("devices");
        assertNotNull(ctxs);
        assertEquals(1, ctxs.size());
        DeviceContext ctx = ctxs.iterator().next();
        assertEquals("com.whizzosoftware.hobson.server-zwave", ctx.getPluginId());
        assertEquals("zwave-32", ctx.getDeviceId());
    }

    @Test
    public void testRuleConstructorWithTurnOnTrigger() throws Exception {
        HubContext hctx = HubContext.createLocal();
        PluginContext pctx = PluginContext.create(hctx, "plugin1");

        // create assumptions
        ArrayList<Assumption> assumps = new ArrayList<>();
        assumps.add(new Assumption(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
        assumps.add(new Assumption(ConditionConstants.VARIABLE_NAME, "=", "on"));
        assumps.add(new Assumption(ConditionConstants.DEVICE_CTX, "containsatleastone", "[local:local:com.whizzosoftware.hobson.server-zwave:zwave-32]"));
        assumps.add(new Assumption(ConditionConstants.VARIABLE_VALUE, "=", "true"));

        // create actions
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(new Action(ConditionConstants.SET_ACTIONSET, Collections.singletonList("actionset1")));
        actions.add(new Action(ConditionConstants.FIRE_TRIGGER, Collections.singletonList(TaskContext.create(hctx, "task1"))));

        // create JRE rule
        RuleImpl rule = new RuleImpl("rule1", "rule1desc", assumps, actions, true);

        JRETask task = new JRETask(pctx, rule);

        assertNotNull("rule1", task.getContext().getTaskId());

        assertNotNull(task.getTriggerCondition());
        assertEquals(RulesPlugin.CONDITION_CLASS_TURN_ON, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
        Collection<DeviceContext> ctxs = (Collection<DeviceContext>)task.getTriggerCondition().getPropertyValue("devices");
        assertNotNull(ctxs);
        assertEquals(1, ctxs.size());
        DeviceContext ctx = ctxs.iterator().next();
        assertEquals("com.whizzosoftware.hobson.server-zwave", ctx.getPluginId());
        assertEquals("zwave-32", ctx.getDeviceId());
    }

    @Test
    public void testConstructorWithTurnOffCondition() throws Exception {
        HubContext hctx = HubContext.createLocal();
        PluginContext pctx = PluginContext.create(hctx, "plugin1");

        JRETask task = new JRETask(
            TaskContext.create(hctx, "task1"),
            "Task 1",
            new PropertyContainer(
                PropertyContainerClassContext.create(pctx, RulesPlugin.CONDITION_CLASS_TURN_OFF),
                Collections.singletonMap("devices", (Object)Collections.singletonList(DeviceContext.createLocal("com.whizzosoftware.hobson.hobson-hub-zwave", "zwave-32")))
            )
        );

        assertNotNull(task.getContext());
        assertNotNull(task.getContext().getTaskId());

        assertNotNull(task.getTriggerCondition());
        assertEquals(RulesPlugin.CONDITION_CLASS_TURN_OFF, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
        Collection<DeviceContext> ctxs = (Collection<DeviceContext>)task.getTriggerCondition().getPropertyValue("devices");
        assertNotNull(ctxs);
        assertEquals(1, ctxs.size());
        DeviceContext ctx = ctxs.iterator().next();
        assertEquals("com.whizzosoftware.hobson.hobson-hub-zwave", ctx.getPluginId());
        assertEquals("zwave-32", ctx.getDeviceId());
    }

    @Test
    public void testJsonConstructorWithTurnOnCondition() throws Exception {
        HubContext hctx = HubContext.createLocal();
        PluginContext pctx = PluginContext.create(hctx, "plugin1");

        JRETask task = new JRETask(
            TaskContext.create(hctx, "task1"),
            "Task 1",
            new PropertyContainer(
                PropertyContainerClassContext.create(pctx, RulesPlugin.CONDITION_CLASS_TURN_ON),
                Collections.singletonMap("devices", (Object)Collections.singletonList(DeviceContext.createLocal("com.whizzosoftware.hobson.hobson-hub-zwave", "zwave-32")))
            )
        );

        assertNotNull(task.getContext());
        assertNotNull(task.getContext().getTaskId());

        assertNotNull(task.getTriggerCondition());
        assertEquals(RulesPlugin.CONDITION_CLASS_TURN_ON, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
        assertNotNull(task.getTriggerCondition().getPropertyValue("devices"));

        Collection<DeviceContext> ctxs = (Collection<DeviceContext>)task.getTriggerCondition().getPropertyValue("devices");
        assertEquals(1, ctxs.size());
        DeviceContext ctx = ctxs.iterator().next();
        assertEquals("com.whizzosoftware.hobson.hobson-hub-zwave", ctx.getPluginId());
        assertEquals("zwave-32", ctx.getDeviceId());
    }

}
