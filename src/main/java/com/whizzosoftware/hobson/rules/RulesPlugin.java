/*
 *******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.rules;

import com.whizzosoftware.hobson.api.event.*;
import com.whizzosoftware.hobson.api.event.device.DeviceUnavailableEvent;
import com.whizzosoftware.hobson.api.event.device.DeviceVariablesUpdateEvent;
import com.whizzosoftware.hobson.api.event.presence.PresenceUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.plugin.PluginType;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.task.condition.*;
import com.whizzosoftware.hobson.rules.condition.*;
import com.whizzosoftware.hobson.rules.jruleengine.JRETaskProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * A plugin that provides event-based tasks using the JRuleEngine library.
 *
 * @author Dan Noguerol
 */
public class RulesPlugin extends AbstractHobsonPlugin implements TaskConditionClassProvider {
    private final static Logger logger = LoggerFactory.getLogger(RulesPlugin.class);

    private JRETaskProvider taskProvider;

    public RulesPlugin(String pluginId, String version, String description) {
        super(pluginId, version, description);
    }

    @Override
    public String getName() {
        return "Rules Plugin";
    }

    @Override
    public void onStartup(PropertyContainer config) {
        // set up the task provider
        taskProvider = new JRETaskProvider(getContext(), this);
        taskProvider.setTaskManager(getTaskManager());

        File rulesFile = getDataFile("rules.json");
        rulesFile.deleteOnExit();
        logger.debug("Using local rules file: {}", rulesFile.getAbsolutePath());
        taskProvider.setRulesFile(rulesFile);

        setTaskProvider(taskProvider);

        // publish condition classes
        publishTaskConditionClass(new DeviceIndoorTempAboveConditionClass(getContext()));
        publishTaskConditionClass(new DeviceIndoorTempBelowConditionClass(getContext()));
        publishTaskConditionClass(new DeviceTurnsOnConditionClass(getContext()));
        publishTaskConditionClass(new DeviceTurnsOffConditionClass(getContext()));
        publishTaskConditionClass(new DeviceUnavailableConditionClass(getContext()));
        publishTaskConditionClass(new PresenceArrivalConditionClass(getContext()));
        publishTaskConditionClass(new PresenceDepartureConditionClass(getContext()));

        // set the plugin status to running
        setStatus(PluginStatus.running());
        logger.debug("Rules plugin has started");
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public long getRefreshInterval() {
        return 0;
    }

    @Override
    public PluginType getType() {
        return PluginType.CORE;
    }

    @Override
    public void onRefresh() {}

    @Override
    protected TypedProperty[] getConfigurationPropertyTypes() {
        return null;
    }

    @EventHandler
    public void onHobsonEvent(HobsonEvent event) {
        // for now, the plugin will only process variable updates, presence update events and device availability events
        if (event instanceof DeviceVariablesUpdateEvent ||
            event instanceof DeviceUnavailableEvent ||
            event instanceof PresenceUpdateNotificationEvent) {
            taskProvider.processEvent(event);
        }
    }

    @Override
    public TaskConditionClass getConditionClass(PropertyContainerClassContext ctx) {
        return getTaskManager().getConditionClass(ctx);
    }
}
