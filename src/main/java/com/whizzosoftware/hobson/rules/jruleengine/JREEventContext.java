/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.event.DeviceUnavailableEvent;
import com.whizzosoftware.hobson.api.event.ExecuteTaskEvent;
import com.whizzosoftware.hobson.api.event.PresenceUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.variable.VariableChange;

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
    private Object variableOldValue;
    private Object variableValue;
    private String oldLocationCtx;
    private String newLocationCtx;
    private String personCtx;
    private String taskCtx;

    public JREEventContext(VariableChange update) {
        this.eventId = VariableUpdateNotificationEvent.ID;
        this.deviceCtx = update.getContext().getDeviceContext().toString();
        this.variableName = update.getName();
        this.variableOldValue = update.getOldValue();
        this.variableValue = update.getNewValue();
    }

    public JREEventContext(DeviceUnavailableEvent event) {
        this.eventId = event.getEventId();
        this.deviceCtx = event.getDeviceContext().toString();
    }

    public JREEventContext(PresenceUpdateNotificationEvent event) {
        this.eventId = event.getEventId();
        this.personCtx = event.getEntityContext().toString();
        this.oldLocationCtx = event.getOldLocation() != null ? event.getOldLocation().toString() : null;
        this.newLocationCtx = event.getNewLocation() != null ? event.getNewLocation().toString() : null;
    }

    public JREEventContext(ExecuteTaskEvent event) {
        this.eventId = event.getEventId();
        this.taskCtx = event.getTask().toString();
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

    public Object variableOldValue() {
        return variableOldValue;
    }

    public Object variableValue() {
        return variableValue;
    }

    public String personCtx() {
        return personCtx;
    }

    public String oldLocationCtx() {
        return oldLocationCtx;
    }

    public String newLocationCtx() {
        return newLocationCtx;
    }

    public String taskCtx() {
        return taskCtx;
    }
}
