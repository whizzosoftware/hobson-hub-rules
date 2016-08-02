/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules;

import com.whizzosoftware.hobson.api.event.*;
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.task.TaskProvider;
import com.whizzosoftware.hobson.api.task.condition.*;
import com.whizzosoftware.hobson.rules.condition.*;
import com.whizzosoftware.hobson.rules.condition.ManualTaskExecutionConditionClass;
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

    public RulesPlugin(String pluginId) {
        super(pluginId);
    }

    @Override
    public String getName() {
        return "Rules Plugin";
    }

    @Override
    public void onStartup(PropertyContainer config) {
        taskProvider = new JRETaskProvider(getContext(), this);
        taskProvider.setTaskManager(getTaskManager());

        File rulesFile = getDataFile("rules.json");
        rulesFile.deleteOnExit();
        logger.debug("Using local rules file: {}", rulesFile.getAbsolutePath());
        taskProvider.setRulesFile(rulesFile);

        // publish condition classes
        publishConditionClass(new DeviceIndoorTempAboveConditionClass(getContext()));
        publishConditionClass(new DeviceIndoorTempBelowConditionClass(getContext()));
        publishConditionClass(new DeviceTurnsOnConditionClass(getContext()));
        publishConditionClass(new DeviceTurnsOffConditionClass(getContext()));
        publishConditionClass(new DeviceUnavailableConditionClass(getContext()));
        publishConditionClass(new PresenceArrivalConditionClass(getContext()));
        publishConditionClass(new PresenceDepartureConditionClass(getContext()));
        publishConditionClass(new ManualTaskExecutionConditionClass(getContext()));

        // set the plugin status to running
        setStatus(PluginStatus.running());
        logger.debug("Rules plugin has started");
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public String[] getEventTopics() {
        return new String[] {EventTopics.PRESENCE_TOPIC};
    }

    @Override
    public long getRefreshInterval() {
        return 0;
    }

    @Override
    public TaskProvider getTaskProvider() {
        return taskProvider;
    }

    @Override
    public void onRefresh() {}

    @Override
    protected TypedProperty[] createSupportedProperties() {
        return null;
    }

    @Override
    public void onPluginConfigurationUpdate(PropertyContainer config) {}

    @Override
    public void onHobsonEvent(HobsonEvent event) {
        super.onHobsonEvent(event);

        // for now, the plugin will only process variable updates, presence update events and device availability events
        if (event instanceof VariableUpdateNotificationEvent ||
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
