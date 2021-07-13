package com.pmd;


import net.sourceforge.pmd.*;
import net.sourceforge.pmd.benchmark.TimeTracker;
import net.sourceforge.pmd.benchmark.TimedOperation;
import net.sourceforge.pmd.benchmark.TimedOperationCategory;
import net.sourceforge.pmd.cli.PMDCommandLineInterface;
import net.sourceforge.pmd.processor.AbstractPMDProcessor;
import net.sourceforge.pmd.processor.MultiThreadProcessor;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.stat.Metric;
import net.sourceforge.pmd.util.ClasspathClassLoader;
import net.sourceforge.pmd.util.IOUtil;
import net.sourceforge.pmd.util.ResourceLoader;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.ReaderDataSource;

import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * check java code whether legal
 */
public class StringInputPMD extends PMD {
    private static final Logger LOG = Logger.getLogger(StringInputPMD.class.getName());
    private static Map<String, List<RuleViolation>> ruleViolationMap = new ConcurrentHashMap<String, List<RuleViolation>>() {
    };

    public static Map<String, List<RuleViolation>> doStringInputPMD(StringInputPMDConfiguration configuration) {
        ruleViolationMap.clear();
        RuleSetFactory ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(configuration, new ResourceLoader());
        RuleSets ruleSets = RulesetsFactoryUtils.getRuleSetsWithBenchmark(configuration.getRuleSets(), ruleSetFactory);
        if (ruleSets == null) {
            return null;
        } else {
            List dataSources = getApplicableFiles(configuration);
            try {

                Renderer renderer;
                List renderers;
                try {
                    renderer = configuration.createRenderer();
                    renderers = Collections.singletonList(renderer);
                    renderer.setReportFile(configuration.getReportFile());
                    renderer.start();
                } catch (Throwable var24) {

                    throw var24;
                }

                RuleContext ctx = new RuleContext();
                final AtomicInteger violations = new AtomicInteger(0);
                ctx.getReport().addListener(new ThreadSafeReportListener() {
                    @Override
                    public void ruleViolationAdded(RuleViolation ruleViolation) {
                        String key = configuration.getInputContentName();
                        List ruleList = null;
                        if (ruleViolationMap.get(key) == null) {
                            ruleList = new LinkedList<RuleViolation>();
                        } else {
                            ruleList = ruleViolationMap.get(key);
                        }
                        ruleList.add(ruleViolation);
                        ruleViolationMap.put(key, ruleList);
                        violations.getAndIncrement();
                    }

                    @Override
                    public void metricAdded(Metric metric) {
                    }
                });

                try {
                    processStringInputs(configuration, ruleSetFactory, dataSources, ctx, renderers);
                } catch (Throwable var23) {


                    throw var23;
                }
            } catch (Exception var26) {
                String message = var26.getMessage();
                if (message != null) {
                    LOG.severe(message);
                } else {
                    LOG.log(Level.SEVERE, "Exception during processing", var26);
                }

                LOG.log(Level.FINE, "Exception during processing", var26);
                LOG.info(PMDCommandLineInterface.buildUsageText());
            } finally {
                if (configuration.getClassLoader() instanceof ClasspathClassLoader) {
                    IOUtil.tryCloseClassLoader(configuration.getClassLoader());
                }

            }
            return getRuleViolationMap();
        }
    }

    public static Map<String, List<RuleViolation>> getRuleViolationMap() {
        return ruleViolationMap;
    }

    public static void processStringInputs(StringInputPMDConfiguration configuration, RuleSetFactory ruleSetFactory, List<DataSource> stringInput, RuleContext ctx, List<Renderer> renderers) {
        ctx.getReport().addListener(configuration.getAnalysisCache());
        RuleSetFactory silentFactory = new RuleSetFactory(ruleSetFactory, false);
        newFileProcessor(configuration).processFiles(silentFactory, stringInput, ctx, renderers);
        configuration.getAnalysisCache().persist();
    }

    private static AbstractPMDProcessor newFileProcessor(PMDConfiguration configuration) {
        return new MultiThreadProcessor(configuration);
    }

    public static List<DataSource> getApplicableFiles(StringInputPMDConfiguration configuration) {
        TimedOperation to = TimeTracker.startOperation(TimedOperationCategory.COLLECT_FILES);

        List var3;
        try {
            var3 = internalGetApplicableDSList(configuration);
        } catch (Throwable var6) {
            if (to != null) {
                try {
                    to.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }
            }
            throw var6;
        }
        if (to != null) {
            to.close();
        }
        return var3;
    }


    private static List<DataSource> internalGetApplicableDSList(StringInputPMDConfiguration configuration) {
        List<DataSource> dataSourceList = new ArrayList();
        if (null != configuration.getInputContent() && null != configuration.getInputContentName()) {
            String content = configuration.getInputContent();
            String contentName = configuration.getInputContentName();
            List<DataSource> dataSources = new ArrayList<>();
            Reader reader = new StringReader(content);
            dataSources.add(new ReaderDataSource(reader, contentName));
            dataSourceList.addAll(dataSources);
        }
        return dataSourceList;
    }
}
