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
import com.whizzosoftware.hobson.api.property.PropertyContainerSet;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.rules.RulesPlugin;
import com.whizzosoftware.hobson.rules.condition.*;
import org.jruleengine.rule.Action;
import org.jruleengine.rule.Assumption;
import org.jruleengine.rule.RuleImpl;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
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
        assumps.add(new Assumption(ConditionConstants.PLUGIN_ID, "=", "com.whizzosoftware.hobson.server-zwave"));
        assumps.add(new Assumption(ConditionConstants.DEVICE_ID, "=", "zwave-32"));
        assumps.add(new Assumption(ConditionConstants.VARIABLE_NAME, "=", "on"));
        assumps.add(new Assumption(ConditionConstants.VARIABLE_VALUE, "=", "false"));

        // create actions
        ArrayList actions = new ArrayList();
        actions.add(new Action(ConditionConstants.EXECUTE_ACTIONSET, Collections.singletonList("actionset1")));

        // create JRE rule
        RuleImpl rule = new RuleImpl("rule1", "rule1desc", assumps, actions, true);

        JRETask task = new JRETask(pctx, rule);

        assertNotNull("rule1", task.getContext().getTaskId());
        assertEquals("rule1desc", task.getName());

        assertTrue(task.getConditionSet().hasPrimaryProperty());
        assertEquals(RulesPlugin.CONDITION_CLASS_TURN_OFF, task.getConditionSet().getPrimaryProperty().getContainerClassContext().getContainerClassId());
        assertNotNull(task.getConditionSet().getPrimaryProperty().getPropertyValue("device"));
        assertEquals("com.whizzosoftware.hobson.server-zwave", ((DeviceContext) task.getConditionSet().getPrimaryProperty().getPropertyValue("device")).getPluginId());
        assertEquals("zwave-32", ((DeviceContext) task.getConditionSet().getPrimaryProperty().getPropertyValue("device")).getDeviceId());
    }

    @Test
    public void testRuleConstructorWithTurnOnTrigger() throws Exception {
        HubContext hctx = HubContext.createLocal();
        PluginContext pctx = PluginContext.create(hctx, "plugin1");

        // create assumptions
        ArrayList<Assumption> assumps = new ArrayList<>();
        assumps.add(new Assumption(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
        assumps.add(new Assumption(ConditionConstants.PLUGIN_ID, "=", "com.whizzosoftware.hobson.server-zwave"));
        assumps.add(new Assumption(ConditionConstants.DEVICE_ID, "=", "zwave-32"));
        assumps.add(new Assumption(ConditionConstants.VARIABLE_NAME, "=", "on"));
        assumps.add(new Assumption(ConditionConstants.VARIABLE_VALUE, "=", "true"));

        // create actions
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(new Action(ConditionConstants.EXECUTE_ACTIONSET, Collections.singletonList("actionset1")));

        // create JRE rule
        RuleImpl rule = new RuleImpl("rule1", "rule1desc", assumps, actions, true);

        JRETask task = new JRETask(pctx, rule);

        assertNotNull("rule1", task.getContext().getTaskId());
        assertEquals("rule1desc", task.getName());

        assertTrue(task.getConditionSet().hasPrimaryProperty());
        assertEquals(RulesPlugin.CONDITION_CLASS_TURN_ON, task.getConditionSet().getPrimaryProperty().getContainerClassContext().getContainerClassId());
        assertNotNull(task.getConditionSet().getPrimaryProperty().getPropertyValue("device"));
        assertEquals("com.whizzosoftware.hobson.server-zwave", ((DeviceContext) task.getConditionSet().getPrimaryProperty().getPropertyValue("device")).getPluginId());
        assertEquals("zwave-32", ((DeviceContext) task.getConditionSet().getPrimaryProperty().getPropertyValue("device")).getDeviceId());
        assertTrue(task.getActionSet().hasId());
        assertEquals("actionset1", task.getActionSet().getId());
    }

    @Test
    public void testConstructorWithTurnOffCondition() throws Exception {
        HubContext hctx = HubContext.createLocal();
        PluginContext pctx = PluginContext.create(hctx, "plugin1");

        JRETask task = new JRETask(
            TaskContext.create(pctx, "task1"),
            "rule1",
            new PropertyContainerSet(
                new PropertyContainer(
                    PropertyContainerClassContext.create(pctx, RulesPlugin.CONDITION_CLASS_TURN_OFF),
                    Collections.singletonMap("device", (Object) DeviceContext.createLocal("com.whizzosoftware.hobson.hobson-hub-zwave", "zwave-32"))
                )
            ),
            new PropertyContainerSet("actionset1", null)
        );

        assertNotNull(task.getContext());
        assertNotNull(task.getContext().getTaskId());
        assertEquals("rule1", task.getName());

        assertTrue(task.getConditionSet().hasPrimaryProperty());
        assertEquals(RulesPlugin.CONDITION_CLASS_TURN_OFF, task.getConditionSet().getPrimaryProperty().getContainerClassContext().getContainerClassId());
        assertNotNull(task.getConditionSet().getPrimaryProperty().getPropertyValue("device"));
        assertEquals("com.whizzosoftware.hobson.hobson-hub-zwave", ((DeviceContext) task.getConditionSet().getPrimaryProperty().getPropertyValue("device")).getPluginId());
        assertEquals("zwave-32", ((DeviceContext) task.getConditionSet().getPrimaryProperty().getPropertyValue("device")).getDeviceId());

        assertEquals("actionset1", task.getActionSet().getId());
    }

    @Test
    public void testJsonConstructorWithTurnOnCondition() throws Exception {
        HubContext hctx = HubContext.createLocal();
        PluginContext pctx = PluginContext.create(hctx, "plugin1");

        JRETask task = new JRETask(
            TaskContext.create(pctx, "task1"),
            "rule2",
            new PropertyContainerSet(
                new PropertyContainer(
                    PropertyContainerClassContext.create(pctx, RulesPlugin.CONDITION_CLASS_TURN_ON),
                    Collections.singletonMap("device", (Object)DeviceContext.createLocal("com.whizzosoftware.hobson.hobson-hub-zwave", "zwave-32"))
                )
            ),
            new PropertyContainerSet("actionset2", null)
        );

        assertNotNull(task.getContext());
        assertNotNull(task.getContext().getTaskId());
        assertEquals("rule2", task.getName());

        assertTrue(task.getConditionSet().hasPrimaryProperty());
        assertEquals(RulesPlugin.CONDITION_CLASS_TURN_ON, task.getConditionSet().getPrimaryProperty().getContainerClassContext().getContainerClassId());
        assertNotNull(task.getConditionSet().getPrimaryProperty().getPropertyValue("device"));
        assertEquals("com.whizzosoftware.hobson.hobson-hub-zwave", ((DeviceContext) task.getConditionSet().getPrimaryProperty().getPropertyValue("device")).getPluginId());
        assertEquals("zwave-32", ((DeviceContext) task.getConditionSet().getPrimaryProperty().getPropertyValue("device")).getDeviceId());

        assertEquals("actionset2", task.getActionSet().getId());
    }

}
