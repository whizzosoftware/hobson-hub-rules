/*
 *******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.event.*;
import com.whizzosoftware.hobson.api.event.device.DeviceUnavailableEvent;
import com.whizzosoftware.hobson.api.event.device.DeviceVariablesUpdateEvent;
import com.whizzosoftware.hobson.api.event.presence.PresenceUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.task.*;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClass;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClassProvider;
import com.whizzosoftware.hobson.api.variable.DeviceVariableUpdate;
import com.whizzosoftware.hobson.rules.condition.AbstractRuleConditionClass;
import com.whizzosoftware.hobson.rules.condition.ConditionConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rules.*;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import java.io.*;
import java.util.*;

/**
 * A JRuleEngine implementation of TaskProvider.
 *
 * @author Dan Noguerol
 */
public class JRETaskProvider implements TaskProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private PluginContext pluginContext;
    private TaskConditionClassProvider conditionClassProvider;
    private TaskManager taskManager;
    private String ruleUri;
    private File rulesFile;
    private RuleServiceProvider provider;
    private RuleAdministrator administrator;
    private RuleRuntime runtime;
    private final Map<String,HobsonTask> tasks = new HashMap<>();

    /**
     * Constructor.
     *
     * @param pluginContext the context of the plugin creating tasks
     */
    public JRETaskProvider(PluginContext pluginContext, TaskConditionClassProvider conditionClassProvider) {
        try {
            Class.forName("org.jruleengine.RuleServiceProviderImpl");
            this.pluginContext = pluginContext;
            this.conditionClassProvider = conditionClassProvider;
            provider = RuleServiceProviderManager.getRuleServiceProvider("org.jruleengine");
            administrator = provider.getRuleAdministrator();
            logger.debug("Acquired RuleAdministrator: {}", administrator);
            runtime = provider.getRuleRuntime();
        } catch (Exception e) {
            throw new HobsonRuntimeException("Error create rule task provider", e);
        }
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    synchronized public void setRulesFile(File rulesFile) {
        this.rulesFile = rulesFile;

        if (rulesFile != null) {
            try {
                // create empty rules file
                writeRuleFile();

                // load the rules
                logger.debug("Rules engine loading file: {}", rulesFile.getAbsolutePath());
                loadRules(new FileInputStream(rulesFile));
            } catch (Exception e) {
                throw new HobsonRuntimeException("Error loading rules file", e);
            }
        } else {
            throw new HobsonRuntimeException("Invalid rules file specified; unable to load");
        }
    }

    synchronized public void loadRules(InputStream rules) throws Exception {
        // create execution set
        Map props = new HashMap();
        props.put("Content-Type", "application/json");
        RuleExecutionSet res = administrator.getLocalRuleExecutionSetProvider(props).createRuleExecutionSet(rules, null);
        rules.close();
        ruleUri = res.getName();

        // register execution set
        administrator.registerRuleExecutionSet(ruleUri, res, null);
        logger.debug("Loaded rules: {}", ruleUri);
    }

    /**
     * Allow the engine to process an incoming event.
     *
     * @param event the event to process
     */
    public void processEvent(HobsonEvent event) {
        StatelessRuleSession session = null;
        try {
            session = (StatelessRuleSession)runtime.createRuleSession(
                ruleUri,
                new HashMap(),
                RuleRuntime.STATELESS_SESSION_TYPE
            );

            if (event instanceof DeviceVariablesUpdateEvent) {
                DeviceVariablesUpdateEvent vune = (DeviceVariablesUpdateEvent)event;
                for (DeviceVariableUpdate update : vune.getUpdates()) {
                    List inputList = new LinkedList();
                    inputList.add(new JREEventContext(update));
                    inputList.add(new JRETaskContext(pluginContext, taskManager));
                    session.executeRules(inputList);
                }
            } else if (event instanceof DeviceUnavailableEvent) {
                DeviceUnavailableEvent due = (DeviceUnavailableEvent)event;
                List inputList = new LinkedList();
                inputList.add(new JREEventContext(due));
                inputList.add(new JRETaskContext(pluginContext, taskManager));
                session.executeRules(inputList);
            } else if (event instanceof PresenceUpdateNotificationEvent) {
                List inputList = new LinkedList();
                inputList.add(new JREEventContext((PresenceUpdateNotificationEvent)event));
                inputList.add(new JRETaskContext(pluginContext, taskManager));
                session.executeRules(inputList);
            }
        } catch (Exception e) {
            logger.error("Error executing rules", e);
        } finally {
            if (session != null) {
                try {
                    session.release();
                } catch (Exception e) {
                    logger.error("Error closing rule session", e);
                }
            }
        }
    }

    private void onCreateTask(HobsonTask task, boolean writeRulesFile) {
        if (task != null && doesOwnTask(task)) {
            logger.info("Adding new task: {}", task.getContext());
            tasks.put(task.getContext().getTaskId(), task);
            if (writeRulesFile) {
                try {
                    writeRuleFile();
                    if (rulesFile != null) {
                        loadRules(new FileInputStream(rulesFile));
                    }
                } catch (Exception e) {
                    throw new TaskException("Error writing rules file", e);
                }
            }
        }
    }

    @Override
    public void onRegisterTasks(final Collection<TaskContext> tasks) {
        Iterator<TaskContext> it = tasks.iterator();
        while (it.hasNext()) {
            HobsonTask task = taskManager.getTask(it.next());
            if (task != null && doesOwnTask(task)) {
                onCreateTask(task, !it.hasNext());
            }
        }
    }

    @Override
    public void onUpdateTask(TaskContext ctx) {
        try {
            HobsonTask t = tasks.get(ctx.getTaskId());
            if (t != null && doesOwnTask(t)) {
                logger.info("Updating task: {}", ctx);

                tasks.put(ctx.getTaskId(), taskManager.getTask(ctx));

                // add to file
                writeRuleFile();

                // re-load rules
                if (rulesFile != null) {
                    loadRules(new FileInputStream(rulesFile));
                }
            }
        } catch (Exception e) {
            throw new TaskException("Error updating task", e);
        }
    }

    @Override
    public void onDeleteTask(TaskContext ctx) {
        try {
            HobsonTask t = tasks.get(ctx.getTaskId());
            if (t != null && doesOwnTask(t)) {
                logger.info("Deleting task: {}", ctx.getTaskId());
                tasks.remove(ctx.getTaskId());

                // write change to file
                writeRuleFile();

                // re-load rules
                if (rulesFile != null) {
                    loadRules(new FileInputStream(rulesFile));
                }
            }
        } catch (Exception e) {
            throw new TaskException("Error deleting task", e);
        }
    }

    private boolean doesOwnTask(HobsonTask task) {
        PropertyContainer triggerCondition = TaskHelper.getTriggerCondition(taskManager, task.getConditions());

        if (triggerCondition != null) {
            TaskConditionClass tcc = taskManager.getConditionClass(triggerCondition.getContainerClassContext());
            if (tcc != null) {
                return (tcc instanceof AbstractRuleConditionClass);
            }
        }

        return false;
    }

    synchronized private void writeRuleFile() throws Exception {
        if (rulesFile != null) {
            logger.trace("Writing rules file");

            FileWriter writer = new FileWriter(rulesFile);
            JSONObject rootJson = new JSONObject();
            rootJson.put("name", "Hobson Rules");
            rootJson.put("description", "Hobson Rules");

            if (tasks.size() > 0) {
                JSONArray rulesArray = new JSONArray();
                for (HobsonTask task : tasks.values()) {
                    rulesArray.put(createTaskJSON(conditionClassProvider, task));
                }
                rootJson.put("rules", rulesArray);
            }

            writer.write(rootJson.toString());
            writer.close();
        } else {
            logger.warn("No rules file defined; unable to write changes");
        }
    }

    private JSONObject createTaskJSON(TaskConditionClassProvider provider, HobsonTask task) {
        JSONObject rule = new JSONObject();
        rule.put("name", task.getContext().getTaskId());
        if (task.getName() != null) {
            rule.put("description", task.getName());
        } else {
            rule.put("description", task.getContext().getTaskId());
        }
        rule.put("enabled", task.isEnabled());

        PropertyContainer triggerCondition = TaskHelper.getTriggerCondition(provider, task.getConditions());

        if (triggerCondition != null) {
            TaskConditionClass tcc = provider.getConditionClass(triggerCondition.getContainerClassContext());
            if (tcc != null) {
                if (tcc instanceof AbstractRuleConditionClass) {
                    rule.put("assumptions", ((AbstractRuleConditionClass)tcc).createAssumptionJSON(triggerCondition));
                } else {
                    throw new HobsonRuntimeException("Unable to create rule with non-rule based trigger condition: " + triggerCondition);
                }
            } else {
                throw new HobsonRuntimeException("Unable to create JSON for rule with unknown trigger condition: " + triggerCondition);
            }
        }

        JSONArray actions = new JSONArray();
        JSONObject action = new JSONObject();
        action.put("method", ConditionConstants.FIRE_TRIGGER);
        action.put("arg1", task.getContext());
        actions.put(action);
        rule.put("actions", actions);

        return rule;
    }
}
