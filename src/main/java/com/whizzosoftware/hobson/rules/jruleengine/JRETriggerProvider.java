/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.rules.jruleengine;

import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.action.HobsonActionRef;
import com.whizzosoftware.hobson.api.action.ActionManager;
import com.whizzosoftware.hobson.api.event.HobsonEvent;
import com.whizzosoftware.hobson.api.event.PresenceUpdateEvent;
import com.whizzosoftware.hobson.api.event.VariableUpdateNotificationEvent;
import com.whizzosoftware.hobson.api.trigger.HobsonTrigger;
import com.whizzosoftware.hobson.api.trigger.TriggerException;
import com.whizzosoftware.hobson.api.trigger.TriggerProvider;
import com.whizzosoftware.hobson.api.util.filewatch.FileWatcherListener;
import com.whizzosoftware.hobson.api.util.filewatch.FileWatcherThread;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import org.jruleengine.rule.RuleImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rules.*;
import javax.rules.admin.Rule;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import java.io.*;
import java.util.*;

/**
 * A JRuleEngine implementation of TriggerProvider.
 *
 * @author Dan Noguerol
 */
public class JRETriggerProvider implements TriggerProvider, FileWatcherListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private volatile ActionManager actionManager;

    private String pluginId;
    private String ruleUri;
    private File rulesFile;
    private RuleServiceProvider provider;
    private RuleAdministrator administrator;
    private RuleRuntime runtime;
    private FileWatcherThread watcherThread;
    private final Map<String,HobsonTrigger> triggers = new HashMap<>();

    /**
     * Constructor.
     *
     * @param pluginId the plugin ID that is creating the trigger
     */
    public JRETriggerProvider(String pluginId) {
        try {
            Class.forName("org.jruleengine.RuleServiceProviderImpl");
            this.pluginId = pluginId;
            provider = RuleServiceProviderManager.getRuleServiceProvider("org.jruleengine");
            administrator = provider.getRuleAdministrator();
            logger.debug("Acquired RuleAdministrator: {}", administrator);
            runtime = provider.getRuleRuntime();
        } catch (Exception e) {
            throw new HobsonRuntimeException("Error create rule trigger provider", e);
        }
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public String getId() {
        return pluginId;
    }

    public void setActionManager(ActionManager actionManager) {
        this.actionManager = actionManager;
    }

    synchronized public void setRulesFile(File rulesFile) {
        this.rulesFile = rulesFile;

        if (watcherThread != null) {
            watcherThread.interrupt();
            watcherThread = null;
        }

        if (rulesFile != null) {
            try {
                // create empty rules file if it doesn't exist
                if (!rulesFile.exists()) {
                    writeRuleFile();
                }

                // load the rules
                logger.info("Rules engine loading file: {}", rulesFile.getAbsolutePath());
                loadRules(new FileInputStream(rulesFile));

                // start watching the rules file for changes
                watcherThread = new FileWatcherThread(rulesFile, this);
                watcherThread.start();
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

        // build rule descriptors
        triggers.clear();
        for (Object o : res.getRules()) {
            if (o instanceof Rule) {
                JRETrigger trigger = new JRETrigger(pluginId, (RuleImpl)o);
                triggers.put(trigger.getId(), trigger);
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
                    inputList.add(new JREActionContext(actionManager));
                    session.executeRules(inputList);
                }
            } else if (event instanceof PresenceUpdateEvent) {
                List inputList = new LinkedList();
                inputList.add(new JREEventContext((PresenceUpdateEvent)event));
                inputList.add(new JREActionContext(actionManager));
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
    public void onFileChanged(File rulesFile) {
        try {
            if (rulesFile.getAbsolutePath().equals(this.rulesFile.getAbsolutePath())) {
                loadRules(new FileInputStream(rulesFile));
            } else {
                setRulesFile(rulesFile);
            }
        } catch (Exception e) {
            logger.error("Error loading rules file change", e);
        }
    }

    @Override
    public Collection<HobsonTrigger> getTriggers() {
        return triggers.values();
    }

    @Override
    public HobsonTrigger getTrigger(String triggerId) {
        return triggers.get(triggerId);
    }

    @Override
    synchronized public void addTrigger(Object trigger) {
        try {
            JRETrigger jret = new JRETrigger(pluginId, (JSONObject)trigger);
            triggers.put(jret.getId(), jret);
            writeRuleFile();
        } catch (Exception e) {
            throw new TriggerException("Error adding trigger", e);
        }
    }

    @Override
    synchronized public void updateTrigger(String triggerId, String name, Object data) {
        try {
            JRETrigger trigger = (JRETrigger)triggers.get(triggerId);
            if (trigger != null) {
                triggers.remove(triggerId);
                triggers.put(triggerId, new JRETrigger(trigger.getProviderId(), new JSONObject(new JSONTokener((String)data))));
                writeRuleFile();
            } else {
                throw new RuntimeException("Trigger not found");
            }
        } catch (Exception e) {
            throw new TriggerException("Error adding trigger", e);
        }
    }

    @Override
    synchronized public void deleteTrigger(String triggerId) {
        try {
            JRETrigger trigger = (JRETrigger)triggers.get(triggerId);
            if (trigger != null) {
                triggers.remove(triggerId);
                writeRuleFile();
            } else {
                throw new RuntimeException("Trigger not found");
            }
        } catch (Exception e) {
            throw new TriggerException("Error deleting trigger", e);
        }
    }

    synchronized private void writeRuleFile() throws Exception {
        FileWriter writer = new FileWriter(rulesFile);
        JSONObject rootJson = new JSONObject();
        rootJson.put("name", "Hobson Rules");
        rootJson.put("description", "Hobson Rules");

        JSONArray synArray = new JSONArray();
        JSONObject syn = new JSONObject();
        syn.put("name", "event");
        syn.put("class", "com.whizzosoftware.hobson.rules.jruleengine.JREEventContext");
        synArray.put(syn);

        syn = new JSONObject();
        syn.put("name", "actions");
        syn.put("class", "com.whizzosoftware.hobson.rules.jruleengine.JREActionContext");
        synArray.put(syn);

        rootJson.put("synonyms", synArray);

        if (triggers.size() > 0) {
            JSONArray rulesArray = new JSONArray();
            for (HobsonTrigger trigger : triggers.values()) {
                JSONObject rule = new JSONObject();
                rule.put("name", trigger.getId());
                rule.put("description", trigger.getName());

                JSONArray assumptions = new JSONArray();
                if (trigger.getConditions().size() > 0) {
                    for (Map<String,Object> conditionMap : trigger.getConditions()) {
                        createAssumptionJson(conditionMap, assumptions);
                    }
                }
                rule.put("assumptions", assumptions);

                JSONArray actions = new JSONArray();
                if (trigger.getActions().size() > 0) {
                    for (HobsonActionRef ref : trigger.getActions()) {
                        JSONObject action = new JSONObject();
                        action.put("method", "actions.executeAction");
                        action.put("arg1", ref.getPluginId());
                        action.put("arg2", ref.getActionId());
                        action.put("arg3", ref.getName());
                        action.put("arg4", new JSONObject(ref.getProperties()).toString());
                        actions.put(action);
                    }
                }
                rule.put("actions", actions);

                rulesArray.put(rule);
            }
            rootJson.put("rules", rulesArray);
        }

        writer.write(rootJson.toString());
        writer.close();
    }

    protected void createAssumptionJson(Map<String,Object> conditionMap, JSONArray a) {
        if ("variableUpdate".equals(conditionMap.get("event"))) {
            JSONObject json = new JSONObject();
            json.put("leftTerm", "event.eventId");
            json.put("op", "=");
            json.put("rightTerm", conditionMap.get("event"));
            a.put(json);

            json = new JSONObject();
            json.put("leftTerm", "event.pluginId");
            json.put("op", "=");
            json.put("rightTerm", conditionMap.get("pluginId"));
            a.put(json);

            json = new JSONObject();
            json.put("leftTerm", "event.deviceId");
            json.put("op", "=");
            json.put("rightTerm", conditionMap.get("deviceId"));
            a.put(json);

            json = new JSONObject();
            json.put("leftTerm", "event.variableName");
            json.put("op", "=");
            json.put("rightTerm", conditionMap.get("name"));
            a.put(json);

            json = new JSONObject();
            json.put("leftTerm", "event.variableValue");
            json.put("op", conditionMap.get("comparator"));
            json.put("rightTerm", conditionMap.get("value").toString());
            a.put(json);
        } else if ("presenceUpdate".equals(conditionMap.get("event"))) {
            JSONObject json = new JSONObject();
            json.put("leftTerm", "event.eventId");
            json.put("op", "=");
            json.put("rightTerm", conditionMap.get("event"));
            a.put(json);

            json = new JSONObject();
            json.put("leftTerm", "event.entityId");
            json.put("op", "=");
            json.put("rightTerm", conditionMap.get("entityId"));
            a.put(json);

            json = new JSONObject();
            json.put("leftTerm", "event.location");
            json.put("op", "=");
            json.put("rightTerm", conditionMap.get("location"));
            a.put(json);
        }
    }
}