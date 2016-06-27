/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.condition;

import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.presence.PresenceEntityContext;
import com.whizzosoftware.hobson.api.presence.PresenceLocationContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import org.jruleengine.rule.Assumption;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PresenceArrivalConditionClassTest {
    @Test
    public void testCreateAssumptions() {
        PluginContext pctx = PluginContext.createLocal("plugin1");
        PresenceArrivalConditionClass pdcc = new PresenceArrivalConditionClass(pctx);
        Map<String,Object> values = new HashMap<>();
        values.put("person", PresenceEntityContext.createLocal("person1"));
        values.put("location", PresenceLocationContext.createLocal("location1"));
        PropertyContainer conditions = new PropertyContainer(
                pdcc.getContext(),
                values
        );
        List<Assumption> assumps = pdcc.createConditionAssumptions(conditions);
        assertEquals(4, assumps.size());

        assertEquals("com.whizzosoftware.hobson.rules.jruleengine.JREEventContext.eventId", assumps.get(0).getLeftTerm());
        assertEquals("=", assumps.get(0).getOperator());
        assertEquals("presenceUpdateNotify", assumps.get(0).getRightTerm());

        assertEquals("event.person", assumps.get(1).getLeftTerm());
        assertEquals("=", assumps.get(1).getOperator());
        assertEquals("local:person1", assumps.get(1).getRightTerm());

        assertEquals("event.oldLocation", assumps.get(2).getLeftTerm());
        assertEquals("<>", assumps.get(2).getOperator());
        assertEquals("local:location1", assumps.get(2).getRightTerm());

        assertEquals("event.newLocation", assumps.get(3).getLeftTerm());
        assertEquals("=", assumps.get(3).getOperator());
        assertEquals("local:location1", assumps.get(3).getRightTerm());
    }
}
