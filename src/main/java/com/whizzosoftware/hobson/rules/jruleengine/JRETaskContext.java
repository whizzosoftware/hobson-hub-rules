/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.hub.HubContext;
import com.whizzosoftware.hobson.api.task.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates an TaskManager so JRuleEngine use it to execute actions with the appropriate
 * context.
 *
 * @author Dan Noguerol
 */
public class JRETaskContext {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private HubContext ctx;
    private TaskManager taskManager;

    public JRETaskContext(HubContext ctx, TaskManager taskManager) {
        this.ctx = ctx;
        this.taskManager = taskManager;
    }

    public void executeActionSet(String actionSetId) {
        try {
            taskManager.executeActionSet(ctx, actionSetId);
        } catch (Exception e) {
            logger.error("Error executing action set", e);
        }
    }
}
