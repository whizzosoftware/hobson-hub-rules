/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.condition;

import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.hobson.rules.RulesPlugin;
import org.jruleengine.rule.Assumption;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskConditionFactory {

    public static PropertyContainer createCondition(PluginContext ctx, List<Assumption> assumps) {
        // build an assumption map for quick analysis
        Map<String,Assumption> assumpMap = new HashMap<>();
        for (Assumption assump : assumps) {
            assumpMap.put(assump.getLeftTerm(), assump);
        }

        if (assumpMap.containsKey(ConditionConstants.EVENT_ID)) {
            String eventId = assumpMap.get(ConditionConstants.EVENT_ID).getRightTerm();

            switch (eventId) {
                case VariableUpdateNotificationEvent.ID:
                    return createVariableUpdateCondition(ctx, assumpMap);
            }

            return null;
        } else {
            throw new HobsonRuntimeException("No event ID assumption found in rule task");
        }
    }

    private static PropertyContainer createVariableUpdateCondition(PluginContext pctx, Map<String,Assumption> assumpMap) {
        if (assumpMap.containsKey(ConditionConstants.VARIABLE_NAME) && assumpMap.containsKey(ConditionConstants.VARIABLE_VALUE)) {
            String varName = assumpMap.get(ConditionConstants.VARIABLE_NAME).getRightTerm();
            String varValue = assumpMap.get(ConditionConstants.VARIABLE_VALUE).getRightTerm();
            switch (varName) {
                case VariableConstants.ON:
                    if ("false".equalsIgnoreCase(varValue.trim())) {
                        return new PropertyContainer(
                            PropertyContainerClassContext.create(pctx, RulesPlugin.CONDITION_CLASS_TURN_OFF),
                            Collections.singletonMap(
                                "device",
                                (Object) DeviceContext.create(
                                    pctx.getHubContext(),
                                    assumpMap.get(ConditionConstants.PLUGIN_ID).getRightTerm(),
                                    assumpMap.get(ConditionConstants.DEVICE_ID).getRightTerm()
                                )
                            )
                        );
                    } else if ("true".equalsIgnoreCase(varValue.trim())) {
                        return new PropertyContainer(
                            PropertyContainerClassContext.create(pctx, RulesPlugin.CONDITION_CLASS_TURN_ON),
                            Collections.singletonMap(
                                "device",
                                (Object) DeviceContext.create(
                                    pctx.getHubContext(),
                                    assumpMap.get(ConditionConstants.PLUGIN_ID).getRightTerm(),
                                    assumpMap.get(ConditionConstants.DEVICE_ID).getRightTerm()
                                )
                            )
                        );
                    } else {
                        throw new HobsonRuntimeException("Variable update condition for \"on\" variable must be true or false");
                    }
            }
        } else {
            throw new HobsonRuntimeException("Variable update condition has no variable name and/or value defined");
        }

        return null;
    }
}
