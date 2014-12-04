/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules;

import com.whizzosoftware.hobson.api.action.HobsonAction;
import com.whizzosoftware.hobson.api.action.ActionManager;

import java.util.Collection;
import java.util.Map;

public class MockActionManager implements ActionManager {
    private int setVariableCount;

    @Override
    public void publishAction(HobsonAction hobsonAction) {
    }

    @Override
    public void executeAction(String pluginId, String actionId, Map<String, Object> stringObjectMap) {
        if ("setVariable".equals(actionId)) {
            setVariableCount++;
        }
    }

    @Override
    public Collection<HobsonAction> getAllActions(String userId, String hubId) {
        return null;
    }

    @Override
    public HobsonAction getAction(String userId, String hubId, String pluginId, String actionId) {
        return null;
    }

    public int getSetVariableCount() {
        return setVariableCount;
    }

    public void reset() {
        setVariableCount = 0;
    }
}
