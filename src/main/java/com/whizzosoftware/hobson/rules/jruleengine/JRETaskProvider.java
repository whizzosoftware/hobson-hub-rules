/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.event.DeviceUnavailableEvent;
import com.whizzosoftware.hobson.api.event.HobsonEvent;
import com.whizzosoftware.hobson.api.event.PresenceUpdateEvent;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.task.*;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import org.jruleengine.rule.RuleImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rules.*;
import javax.rules.admin.Rule;
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
    private TaskManager taskManager;
    private String ruleUri;
    private File rulesFile;
    private RuleServiceProvider provider;
    private RuleAdministrator administrator;
    private RuleRuntime runtime;
    private final Map<String,JRETask> tasks = new HashMap<>();

    /**
     * Constructor.
     *
     * @param pluginContext the context of the plugin creating tasks
     */
    public JRETaskProvider(PluginContext pluginContext) {
        try {
            Class.forName("org.jruleengine.RuleServiceProviderImpl");
            this.pluginContext = pluginContext;
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

    protected Collection<JRETask> getTasks() {
        return tasks.values();
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

    protected void clearAllTasks() {
        tasks.clear();
    }

    protected void addTask(JRETask task) {
        tasks.put(task.getContext().getTaskId(), task);
    }

    synchronized public void loadRules(InputStream rules) throws Exception {
        // create execution set
        Map props = new HashMap();
        props.put("Content-Type", "application/json");
        RuleExecutionSet res = administrator.getLocalRuleExecutionSetProvider(props).createRuleExecutionSet(rules, null);
        rules.close();
        ruleUri = res.getName();

        // build rule descriptors
        clearAllTasks();
        for (Object o : res.getRules()) {
            if (o instanceof Rule) {
                JRETask task = new JRETask(pluginContext, (RuleImpl)o);
                addTask(task);
            } else {
                logger.error("Found rule that didn't conform to interface: {}", o);
            }
        }

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

            if (event instanceof VariableUpdateNotificationEvent) {
                VariableUpdateNotificationEvent vune = (VariableUpdateNotificationEvent)event;
                for (VariableUpdate update : vune.getUpdates()) {
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
            } else if (event instanceof PresenceUpdateEvent) {
                List inputList = new LinkedList();
                inputList.add(new JREEventContext((PresenceUpdateEvent)event));
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

    @Override
    public void onCreateTasks(final Collection<HobsonTask> tasks) {
        for (HobsonTask task : tasks) {
            try {
                JRETask t = new JRETask(TaskContext.create(pluginContext.getHubContext(), task.getContext().getTaskId()), task.getName(), TaskHelper.getTriggerCondition(taskManager, task.getConditions()));
                logger.info("Adding new task: {}", task.getContext());
                this.tasks.put(task.getContext().getTaskId(), t);

                // write to file
                writeRuleFile();

                // re-load rules
                if (rulesFile != null) {
                    loadRules(new FileInputStream(rulesFile));
                }
            } catch (Exception e) {
                throw new TaskException("Error adding task", e);
            }
        }
    }


    @Override
    public void onUpdateTask(HobsonTask task) {
        try {
            JRETask t = tasks.get(task.getContext().getTaskId());
            if (t != null) {
                logger.info("Updating task: {}", task.getContext());

                tasks.remove(task.getContext().getTaskId());
                tasks.put(task.getContext().getTaskId(), new JRETask(task.getContext(), task.getName(), TaskHelper.getTriggerCondition(taskManager, task.getConditions())));

                // add to file
                writeRuleFile();

                // re-load rules
                if (rulesFile != null) {
                    loadRules(new FileInputStream(rulesFile));
                }
            } else {
                throw new RuntimeException("Task not found");
            }
        } catch (Exception e) {
            throw new TaskException("Error adding task", e);
        }
    }

    @Override
    public void onDeleteTask(TaskContext ctx) {
        try {
            JRETask t = tasks.get(ctx.getTaskId());
            if (t != null) {
                logger.info("Deleting task: {}", ctx.getTaskId());
                tasks.remove(ctx.getTaskId());

                // write change to file
                writeRuleFile();

                // re-load rules
                if (rulesFile != null) {
                    loadRules(new FileInputStream(rulesFile));
                }
            } else {
                throw new RuntimeException("Task not found");
            }
        } catch (Exception e) {
            throw new TaskException("Error deleting task", e);
        }
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
                for (JRETask task : tasks.values()) {
                    rulesArray.put(task.toJSON());
                }
                rootJson.put("rules", rulesArray);
            }

            writer.write(rootJson.toString());
            writer.close();
        } else {
            logger.warn("No rules file defined; unable to write changes");
        }
    }
}
