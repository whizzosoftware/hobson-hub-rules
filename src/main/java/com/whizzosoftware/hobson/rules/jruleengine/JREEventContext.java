/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.event.DeviceUnavailableEvent;
import com.whizzosoftware.hobson.api.event.PresenceUpdateEvent;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;

/**
 * A context object that provides methods that rules can use for their conditions. This is needed for JRuleEngine
 * because it doesn't support nested getters (e.g. getEvent.getEventId)
 *
 * @author Dan Noguerol
 */
public class JREEventContext {
    private String eventId;
    private String deviceCtx;
    private String variableName;
    private Object variableValue;
    private String entityId;
    private String location;

    public JREEventContext(VariableUpdate update) {
        this.eventId = VariableUpdateNotificationEvent.ID;
        this.deviceCtx = update.getDeviceContext().toString();
        this.variableName = update.getName();
        this.variableValue = update.getValue();
    }

    public JREEventContext(DeviceUnavailableEvent event) {
        this.eventId = event.getEventId();
        this.deviceCtx = event.getDeviceContext().toString();
    }

    public JREEventContext(PresenceUpdateEvent event) {
        this.eventId = event.getEventId();
        this.entityId = event.getEntityId();
        this.location = event.getLocation();
    }

    public String eventId() {
        return eventId;
    }

    public String deviceCtx() {
        return deviceCtx;
    }

    public String variableName() {
        return variableName;
    }

    public Object variableValue() {
        return variableValue;
    }

    public String entityId() {
        return entityId;
    }

    public String location() {
        return location;
    }
}
