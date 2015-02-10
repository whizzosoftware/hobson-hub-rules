/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.action.ActionManager;
import com.whizzosoftware.hobson.api.util.UserUtil;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class encapsulates an ActionManager so JRuleEngine use it to execute actions. This is needed since JRuleEngine
 * only works with a flat list of arguments and ActionManager requires a Map.
 *
 * @author Dan Noguerol
 */
public class JREActionContext {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ActionManager actionManager;

    public JREActionContext(ActionManager actionManager) {
        this.actionManager = actionManager;
    }

    public void executeAction(String pluginId, String actionId, String name, String args) {
        try {
            Map<String,Object> map = new HashMap<>();
            JSONObject json = new JSONObject(new JSONTokener(args));
            Iterator<String> it = json.keys();
            while (it.hasNext()) {
                String s = it.next();
                map.put(s, json.get(s));
            }
            actionManager.executeAction(UserUtil.DEFAULT_USER, UserUtil.DEFAULT_HUB, pluginId, actionId, map);
        } catch (Exception e) {
            logger.error("Error executing action", e);
        }
    }
}
