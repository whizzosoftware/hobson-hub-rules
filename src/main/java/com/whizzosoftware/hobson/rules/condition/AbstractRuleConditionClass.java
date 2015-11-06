/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.condition;

import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClass;
import org.jruleengine.rule.Assumption;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * An abstract base class for all condition classes the rules plugin publishes. This provides methods to convert
 * conditions to/from JRuleEngine Assumptions.
 *
 * @author Dan Noguerol
 */
abstract public class AbstractRuleConditionClass extends TaskConditionClass {

    public AbstractRuleConditionClass(PropertyContainerClassContext context, String name, String descriptionTemplate) {
        super(context, name, descriptionTemplate);
    }

    protected JSONObject createJSONCondition(String leftTerm, String comparator, String rightTerm) {
        JSONObject json = new JSONObject();
        json.put("leftTerm", leftTerm);
        json.put("op", comparator);
        json.put("rightTerm", rightTerm);
        return json;
    }

    abstract public List<Assumption> createConditionAssumptions(PropertyContainer condition);
    abstract public JSONArray createAssumptionJSON(PropertyContainer condition);
}
