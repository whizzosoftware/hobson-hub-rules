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
import com.whizzosoftware.hobson.rules.jruleengine.JRETriggerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * A plugin that provides event-based triggers using the JRuleEngine library.
 *
 * @author Dan Noguerol
 */
public class RulesPlugin extends AbstractHobsonPlugin {
    private final static Logger logger = LoggerFactory.getLogger(RulesPlugin.class);

    private JRETriggerProvider triggerProvider;

    public RulesPlugin(String pluginId) {
        super(pluginId);
    }

    @Override
    public String getName() {
        return "Rules Plugin";
    }

    @Override
    public void onStartup(Dictionary dictionary) {
        triggerProvider = new JRETriggerProvider(getId());
        triggerProvider.setRulesFile(getDataFile("rules.json"));
        publishTriggerProvider(triggerProvider);

        setStatus(new PluginStatus(PluginStatus.Status.RUNNING));
        logger.info("Rules plugin has started");
    }

    @Override
    public void onShutdown() {}

    @Override
    public long getRefreshInterval() {
        return 0;
    }

    @Override
    public void onRefresh() {}

    @Override
    public String[] getEventTopics() {
        return new String[] {EventTopics.PRESENCE_TOPIC};
    }

    @Override
    public void onPluginConfigurationUpdate(Dictionary dictionary) {}

    @Override
    public void onHobsonEvent(HobsonEvent event) {
        super.onHobsonEvent(event);

        // for now, the plugin will only process variable and presence update events
        if (event instanceof VariableUpdateNotificationEvent ||
            event instanceof PresenceUpdateEvent) {
            triggerProvider.processEvent(event);
        }
    }
}
