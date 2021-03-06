/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.api.task.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates a TaskManager so JRuleEngine can use it to execute actions within the appropriate
 * context.
 *
 * @author Dan Noguerol
 */
public class JRETaskContext {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private PluginContext ctx;
    private TaskManager taskManager;

    public JRETaskContext(PluginContext ctx, TaskManager taskManager) {
        this.ctx = ctx;
        this.taskManager = taskManager;
    }

    /**
     * Sets the action set ID. This is really just a dummy method so that we can
     * retain the task's action set ID in the JRuleEngine JSON definition.
     *
     * @param actionSetId the action set ID
     */
    public void setActionSet(String actionSetId) {
    }

    public void fireTaskTrigger(String taskCtxStr) {
        try {
            taskManager.fireTaskTrigger(TaskContext.create(taskCtxStr));
        } catch (Exception e) {
            logger.error("Error firing task trigger", e);
        }
    }
}
