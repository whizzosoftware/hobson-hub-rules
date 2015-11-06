/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.hub.HubContext;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.presence.PresenceEntityContext;
import com.whizzosoftware.hobson.api.presence.PresenceLocationContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.rules.condition.*;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class TaskJSONCreatorTest {
//    @Test
//    public void testEmptyRuleConstructor() throws Exception {
//        ArrayList assumps = new ArrayList();
//        ArrayList actions = new ArrayList();
//        RuleImpl rule = new RuleImpl("rule1", "rule1desc", assumps, actions, true);
//        try {
//            new JRETask(PluginContext.createLocal("plugin1"), rule);
//            fail("Should have thrown exception");
//        } catch (HobsonRuntimeException ignored) {
//        }
//    }

//    @Test
//    public void testRuleConstructorWithTurnOffTrigger() throws Exception {
//        HubContext hctx = HubContext.createLocal();
//        PluginContext pctx = PluginContext.create(hctx, "plugin1");
//
//        // create assumptions
//        ArrayList assumps = new ArrayList();
//        assumps.add(new Assumption(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
//        assumps.add(new Assumption(ConditionConstants.DEVICE_CTX, "containsatleastone", "[local:local:com.whizzosoftware.hobson.server-zwave:zwave-32]"));
//        assumps.add(new Assumption(ConditionConstants.VARIABLE_NAME, "=", "on"));
//        assumps.add(new Assumption(ConditionConstants.VARIABLE_VALUE, "=", "false"));
//
//        // create actions
//        ArrayList actions = new ArrayList();
//        actions.add(new Action(ConditionConstants.SET_ACTIONSET, Collections.singletonList("actionset1")));
//        actions.add(new Action(ConditionConstants.FIRE_TRIGGER, Collections.singletonList(TaskContext.create(hctx, "task1"))));
//
//        // create JRE rule
//        RuleImpl rule = new RuleImpl("ruleId", "rule1desc", assumps, actions, true);
//
//        JRETask task = new JRETask(pctx, rule);
//
//        assertEquals("ruleId", task.getContext().getTaskId());
//
//        assertNotNull(task.getTriggerCondition());
//        assertEquals(DeviceTurnsOffConditionClass.ID, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
//        Collection<DeviceContext> ctxs = (Collection<DeviceContext>)task.getTriggerCondition().getPropertyValue("devices");
//        assertNotNull(ctxs);
//        assertEquals(1, ctxs.size());
//        DeviceContext ctx = ctxs.iterator().next();
//        assertEquals("com.whizzosoftware.hobson.server-zwave", ctx.getPluginId());
//        assertEquals("zwave-32", ctx.getDeviceId());
//    }
//
//    @Test
//    public void testRuleConstructorWithTurnOnTrigger() throws Exception {
//        HubContext hctx = HubContext.createLocal();
//        PluginContext pctx = PluginContext.create(hctx, "plugin1");
//
//        // create assumptions
//        ArrayList<Assumption> assumps = new ArrayList<>();
//        assumps.add(new Assumption(ConditionConstants.EVENT_ID, "=", VariableUpdateNotificationEvent.ID));
//        assumps.add(new Assumption(ConditionConstants.VARIABLE_NAME, "=", "on"));
//        assumps.add(new Assumption(ConditionConstants.DEVICE_CTX, "containsatleastone", "[local:local:com.whizzosoftware.hobson.server-zwave:zwave-32]"));
//        assumps.add(new Assumption(ConditionConstants.VARIABLE_VALUE, "=", "true"));
//
//        // create actions
//        ArrayList<Action> actions = new ArrayList<>();
//        actions.add(new Action(ConditionConstants.SET_ACTIONSET, Collections.singletonList("actionset1")));
//        actions.add(new Action(ConditionConstants.FIRE_TRIGGER, Collections.singletonList(TaskContext.create(hctx, "task1"))));
//
//        // create JRE rule
//        RuleImpl rule = new RuleImpl("rule1", "rule1desc", assumps, actions, true);
//
//        JRETask task = new JRETask(pctx, rule);
//
//        assertNotNull("rule1", task.getContext().getTaskId());
//
//        assertNotNull(task.getTriggerCondition());
//        assertEquals(DeviceTurnsOnConditionClass.ID, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
//        Collection<DeviceContext> ctxs = (Collection<DeviceContext>)task.getTriggerCondition().getPropertyValue("devices");
//        assertNotNull(ctxs);
//        assertEquals(1, ctxs.size());
//        DeviceContext ctx = ctxs.iterator().next();
//        assertEquals("com.whizzosoftware.hobson.server-zwave", ctx.getPluginId());
//        assertEquals("zwave-32", ctx.getDeviceId());
//    }
//
//    @Test
//    public void testRuleConstructorWithPresenceArrivalTrigger() throws Exception {
//        HubContext hctx = HubContext.createLocal();
//        PluginContext pctx = PluginContext.create(hctx, "plugin1");
//
//        // create assumptions
//        ArrayList<Assumption> assumps = new ArrayList<>();
//        assumps.add(new Assumption(ConditionConstants.EVENT_ID, "=", PresenceUpdateNotificationEvent.ID));
//        assumps.add(new Assumption(ConditionConstants.PERSON_CTX, "=", "local:local:4d048446-d0d2-4e6d-b4b5-73d3d23001e2"));
//        assumps.add(new Assumption(ConditionConstants.OLD_LOCATION_CTX, "=", "local:local:f872fb53-f665-47fd-800a-1ad7f7fc010d"));
//        assumps.add(new Assumption(ConditionConstants.NEW_LOCATION_CTX, "<>", "local:local:f872fb53-f665-47fd-800a-1ad7f7fc010d"));
//
//        // create actions
//        ArrayList<Action> actions = new ArrayList<>();
//        actions.add(new Action(ConditionConstants.SET_ACTIONSET, Collections.singletonList("actionset1")));
//        actions.add(new Action(ConditionConstants.FIRE_TRIGGER, Collections.singletonList(TaskContext.create(hctx, "task1"))));
//
//        // create JRE rule
//        RuleImpl rule = new RuleImpl("rule1", "rule1desc", assumps, actions, true);
//
//        JRETask task = new JRETask(pctx, rule);
//
//        assertNotNull("rule1", task.getContext().getTaskId());
//
//        assertNotNull(task.getTriggerCondition());
//        assertEquals(PresenceArrivalConditionClass.ID, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
//    }

//    @Test
//    public void testConstructorWithTurnOffCondition() throws Exception {
//        HubContext hctx = HubContext.createLocal();
//        final PluginContext pctx = PluginContext.create(hctx, "plugin1");
//
//        JRETask task = new JRETask(
//            new MockListConditionClassProvider(new DeviceTurnsOffConditionClass(pctx)),
//            TaskContext.create(hctx, "task1"),
//            "Task 1",
//            new PropertyContainer(
//                    PropertyContainerClassContext.create(pctx, DeviceTurnsOffConditionClass.ID),
//                    Collections.singletonMap("devices", (Object) Collections.singletonList(DeviceContext.createLocal("com.whizzosoftware.hobson.hobson-hub-zwave", "zwave-32")))
//            )
//        );
//
//        assertNotNull(task.getContext());
//        assertNotNull(task.getContext().getTaskId());
//
//        assertNotNull(task.getTriggerCondition());
//        assertEquals(DeviceTurnsOffConditionClass.ID, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
//        Collection<DeviceContext> ctxs = (Collection<DeviceContext>)task.getTriggerCondition().getPropertyValue("devices");
//        assertNotNull(ctxs);
//        assertEquals(1, ctxs.size());
//        DeviceContext ctx = ctxs.iterator().next();
//        assertEquals("com.whizzosoftware.hobson.hobson-hub-zwave", ctx.getPluginId());
//        assertEquals("zwave-32", ctx.getDeviceId());
//    }
//
//    @Test
//    public void testConstructorWithTurnOnCondition() throws Exception {
//        HubContext hctx = HubContext.createLocal();
//        final PluginContext pctx = PluginContext.create(hctx, "plugin1");
//
//        JRETask task = new JRETask(
//            new MockListConditionClassProvider(new DeviceTurnsOnConditionClass(pctx)),
//            TaskContext.create(hctx, "task1"),
//            "Task 1",
//            new PropertyContainer(
//                    PropertyContainerClassContext.create(pctx, DeviceTurnsOnConditionClass.ID),
//                    Collections.singletonMap("devices", (Object) Collections.singletonList(DeviceContext.createLocal("com.whizzosoftware.hobson.hobson-hub-zwave", "zwave-32")))
//            )
//        );
//
//        assertNotNull(task.getContext());
//        assertNotNull(task.getContext().getTaskId());
//
//        assertNotNull(task.getTriggerCondition());
//        assertEquals(DeviceTurnsOnConditionClass.ID, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
//        assertNotNull(task.getTriggerCondition().getPropertyValue("devices"));
//
//        Collection<DeviceContext> ctxs = (Collection<DeviceContext>)task.getTriggerCondition().getPropertyValue("devices");
//        assertEquals(1, ctxs.size());
//        DeviceContext ctx = ctxs.iterator().next();
//        assertEquals("com.whizzosoftware.hobson.hobson-hub-zwave", ctx.getPluginId());
//        assertEquals("zwave-32", ctx.getDeviceId());
//    }
//
//    @Test
//    public void testConstructorWithPresenceArrivalCondition() throws Exception {
//        HubContext hctx = HubContext.createLocal();
//        final PluginContext pctx = PluginContext.create(hctx, "plugin1");
//
//        Map<String,Object> propMap = new HashMap<>();
//        propMap.put("person", PresenceEntityContext.createLocal("person1"));
//        propMap.put("location", PresenceLocationContext.createLocal("location1"));
//
//        JRETask task = new JRETask(
//            new MockListConditionClassProvider(new PresenceArrivalConditionClass(pctx)),
//            TaskContext.create(hctx, "task1"),
//            "Task 1",
//            new PropertyContainer(
//                    PropertyContainerClassContext.create(pctx, PresenceArrivalConditionClass.ID),
//                    propMap
//            )
//        );
//
//        assertNotNull(task.getContext());
//        assertNotNull(task.getContext().getTaskId());
//
//        assertNotNull(task.getTriggerCondition());
//        assertEquals(PresenceArrivalConditionClass.ID, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
//        assertNotNull(task.getTriggerCondition().getPropertyValue("person"));
//        assertNotNull(task.getTriggerCondition().getPropertyValue("location"));
//
//        PresenceEntityContext ctx = (PresenceEntityContext)task.getTriggerCondition().getPropertyValue("person");
//        assertEquals("local", ctx.getHubId());
//        assertEquals("local", ctx.getUserId());
//        assertEquals("person1", ctx.getEntityId());
//
//        PresenceLocationContext ctx2 = (PresenceLocationContext)task.getTriggerCondition().getPropertyValue("location");
//        assertEquals("local", ctx2.getHubId());
//        assertEquals("local", ctx2.getUserId());
//        assertEquals("location1", ctx2.getLocationId());
//    }
//
//    @Test
//    public void testConstructorWithPresenceDepartureCondition() throws Exception {
//        HubContext hctx = HubContext.createLocal();
//        final PluginContext pctx = PluginContext.create(hctx, "plugin1");
//
//        Map<String,Object> propMap = new HashMap<>();
//        propMap.put("person", PresenceEntityContext.createLocal("person1"));
//        propMap.put("location", PresenceLocationContext.createLocal("location1"));
//
//        JRETask task = new JRETask(
//            new MockListConditionClassProvider(new PresenceDepartureConditionClass(pctx)),
//            TaskContext.create(hctx, "task1"),
//            "Task 1",
//            new PropertyContainer(
//                    PropertyContainerClassContext.create(pctx, PresenceDepartureConditionClass.ID),
//                    propMap
//            )
//        );
//
//        assertNotNull(task.getContext());
//        assertNotNull(task.getContext().getTaskId());
//
//        assertNotNull(task.getTriggerCondition());
//        assertEquals(PresenceDepartureConditionClass.ID, task.getTriggerCondition().getContainerClassContext().getContainerClassId());
//        assertNotNull(task.getTriggerCondition().getPropertyValue("person"));
//        assertNotNull(task.getTriggerCondition().getPropertyValue("location"));
//
//        PresenceEntityContext ctx = (PresenceEntityContext)task.getTriggerCondition().getPropertyValue("person");
//        assertEquals("local", ctx.getHubId());
//        assertEquals("local", ctx.getUserId());
//        assertEquals("person1", ctx.getEntityId());
//
//        PresenceLocationContext ctx2 = (PresenceLocationContext)task.getTriggerCondition().getPropertyValue("location");
//        assertEquals("local", ctx2.getHubId());
//        assertEquals("local", ctx2.getUserId());
//        assertEquals("location1", ctx2.getLocationId());
//    }
}
