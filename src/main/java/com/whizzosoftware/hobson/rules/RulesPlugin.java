/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules;

import com.whizzosoftware.hobson.api.event.EventTopics;
import com.whizzosoftware.hobson.api.event.HobsonEvent;
import com.whizzosoftware.hobson.api.event.PresenceUpdateEvent;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.task.TaskProvider;
import com.whizzosoftware.hobson.rules.condition.DeviceTemperatureAboveConditionClass;
import com.whizzosoftware.hobson.rules.condition.DeviceTemperatureBelowConditionClass;
import com.whizzosoftware.hobson.rules.condition.DeviceTurnsOffConditionClass;
import com.whizzosoftware.hobson.rules.condition.DeviceTurnsOnConditionClass;
import com.whizzosoftware.hobson.rules.jruleengine.JRETaskProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * A plugin that provides event-based tasks using the JRuleEngine library.
 *
 * @author Dan Noguerol
 */
public class RulesPlugin extends AbstractHobsonPlugin {
    private final static Logger logger = LoggerFactory.getLogger(RulesPlugin.class);

    public static final String CONDITION_CLASS_TURN_ON = "turnOn";
    public static final String CONDITION_CLASS_TURN_OFF = "turnOff";
    public static final String CONDITION_CLASS_TEMP_ABOVE = "tempAbove";
    public static final String CONDITION_CLASS_TEMP_BELOW = "tempBelow";

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
        taskProvider = new JRETaskProvider(getContext());
        taskProvider.setTaskManager(getTaskManager());

        try {
            File rulesFile = File.createTempFile("rules", ".json", getDataDirectory());
            rulesFile.deleteOnExit();
            logger.debug("Using local rules file: {}", rulesFile.getAbsolutePath());
            taskProvider.setRulesFile(rulesFile);

            // publish condition classes
            publishConditionClass(new DeviceTurnsOnConditionClass(getContext()));
            publishConditionClass(new DeviceTurnsOffConditionClass(getContext()));
            publishConditionClass(new DeviceTemperatureAboveConditionClass(getContext()));
            publishConditionClass(new DeviceTemperatureBelowConditionClass(getContext()));

            // set the plugin status to running
            setStatus(PluginStatus.running());
            logger.debug("Rules plugin has started");
        } catch (IOException e) {
            logger.error("Unable to create local rules file", e);
            setStatus(PluginStatus.failed("Unable to create local rules file"));
        }
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

        // for now, the plugin will only process variable and presence update events
        if (event instanceof VariableUpdateNotificationEvent ||
            event instanceof PresenceUpdateEvent) {
            taskProvider.processEvent(event);
        }
    }
}
