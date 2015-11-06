/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.condition;

import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.event.PresenceUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.presence.PresenceEntityContext;
import com.whizzosoftware.hobson.api.presence.PresenceLocationContext;
import com.whizzosoftware.hobson.api.property.PropertyConstraintType;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.task.condition.ConditionClassType;
import com.whizzosoftware.hobson.api.task.condition.ConditionEvaluationContext;
import org.jruleengine.rule.Assumption;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * A condition class for presence departures.
 *
 * @author Dan Noguerol
 */
public class PresenceDepartureConditionClass extends AbstractRuleConditionClass {
    public static final String ID = "presenceDeparture";

    public PresenceDepartureConditionClass(PluginContext context) {
        super(PropertyContainerClassContext.create(context, ID), "A person departs from somewhere", "{person} departs from {location}");
    }

    @Override
    public ConditionClassType getConditionClassType() {
        return ConditionClassType.trigger;
    }

    @Override
    public boolean evaluate(ConditionEvaluationContext context, PropertyContainer values) {
        return true;
    }

    @Override
    protected List<TypedProperty> createProperties() {
        List<TypedProperty> props = new ArrayList<>();
        props.add(new TypedProperty.Builder("person", "Person", "The person to monitor", TypedProperty.Type.PRESENCE_ENTITY).
            constraint(PropertyConstraintType.required, true).
            build()
        );
        props.add(new TypedProperty.Builder("location", "Location", "The location the person departs from", TypedProperty.Type.LOCATION).
            constraint(PropertyConstraintType.required, true).
            build()
        );
        return props;
    }

    @Override
    public List<Assumption> createConditionAssumptions(PropertyContainer condition) {
        List<Assumption> assumpList = new ArrayList<>();
        assumpList.add(new Assumption(ConditionConstants.EVENT_ID, "=", PresenceUpdateNotificationEvent.ID));
        assumpList.add(new Assumption("event.person", "=", condition.getPropertyValue("person").toString()));
        assumpList.add(new Assumption("event.oldLocation", "=", condition.getPropertyValue("location").toString()));
        assumpList.add(new Assumption("event.newLocation", "<>", null));
        return assumpList;
    }

    @Override
    public JSONArray createAssumptionJSON(PropertyContainer condition) {
        JSONArray a = new JSONArray();
        a.put(createJSONCondition(ConditionConstants.EVENT_ID, "=", PresenceUpdateNotificationEvent.ID));
        a.put(createJSONCondition(ConditionConstants.PERSON_CTX, "=", condition.getPropertyValue("person").toString()));
        a.put(createJSONCondition(ConditionConstants.OLD_LOCATION_CTX, "=", condition.getPropertyValue("location").toString()));
        a.put(createJSONCondition(ConditionConstants.NEW_LOCATION_CTX, "<>", condition.getPropertyValue("location").toString()));
        return a;
    }
}
