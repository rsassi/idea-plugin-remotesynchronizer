package org.sylfra.idea.plugins.remotesynchronizer.maven;

import com.intellij.idea.LoggerFactory;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.startup.StartupManager;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.idea.maven.importing.MavenImporter;
import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.sylfra.idea.plugins.remotesynchronizer.model.Config;
import org.sylfra.idea.plugins.remotesynchronizer.model.SynchroMapping;
import org.sylfra.idea.plugins.remotesynchronizer.model.TargetMappings;
import org.sylfra.idea.plugins.remotesynchronizer.utils.ConfigStateComponent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Thomas Demande
 */
public class RemoteSynchonizerMavenImporter extends MavenImporter {

    private static final Logger LOGGER = LoggerFactory.getInstance().getLoggerInstance(RemoteSynchonizerMavenImporter.class.getName());
    public static final String IMPORTABLE_PLUGIN_GROUPID = "org.apache.maven.plugins";
    public static final String IMPORTABLE_PLUGIN_ARTIFACTID = "maven-compiler-plugin";
    public static final String REMOTE_SYNCHRONIZER_SKIP_PROPERTY = "remoteSynchronizer.skip";
    public static final String REMOTE_SYNCHRONIZER_SOURCE_PROPERTY = "remoteSynchronizer.source";
    public static final String REMOTE_SYNCHRONIZER_TARGET_PROPERTY = "remoteSynchronizer.destination";
    public static final String MAVEN_TARGET_MAPPING_NAME = "Maven";

    public RemoteSynchonizerMavenImporter() {
        super(IMPORTABLE_PLUGIN_GROUPID, IMPORTABLE_PLUGIN_ARTIFACTID);
    }

    @Override
    public void preProcess(Module module, MavenProject mavenProject, MavenProjectChanges mavenProjectChanges, MavenModifiableModelsProvider mavenModifiableModelsProvider) {

    }

    @Override
    public void process(MavenModifiableModelsProvider mavenModifiableModelsProvider, final Module module, MavenRootModelAdapter mavenRootModelAdapter, MavenProjectsTree mavenProjectsTree, final MavenProject mavenProject, MavenProjectChanges mavenProjectChanges, Map<MavenProject, String> mavenProjectStringMap, List<MavenProjectsProcessorTask> mavenProjectsProcessorTasks) {

        if (isRemoteSyncEnabled(mavenProject)) {
        StartupManager.getInstance(module.getProject()).runWhenProjectIsInitialized(new Runnable() {
            public void run() {
                configureRemoteSyncForModuleFromMaven(module, mavenProject);
            }
        });
    } else {
            String reason = shouldSkipRemoteSyncForProject(mavenProject) ? REMOTE_SYNCHRONIZER_SKIP_PROPERTY + " set to true." : "no " + REMOTE_SYNCHRONIZER_TARGET_PROPERTY + " property defined in Maven project.";
            LOGGER.warn("remoteSync configuration via Maven disabled for module: [" + module.getName() + "]. Cause: " + reason);
        }
    }

    private boolean shouldSkipRemoteSyncForProject(MavenProject mavenProject) {
        if ("true".equals(mavenProject.getProperties().getProperty(REMOTE_SYNCHRONIZER_SKIP_PROPERTY))) {
            return true;
        }
        return false;
    }

    private String getTargetDirectoryForProject(MavenProject mavenProject) {
        return mavenProject.getProperties().getProperty(REMOTE_SYNCHRONIZER_TARGET_PROPERTY);
    }

    private boolean isRemoteSyncEnabled(MavenProject mavenProject) {
        //We rely on the REMOTE_SYNCHRONIZER_TARGET_PROPERTY Maven property to be set for knowing we should enable remote sync
        return mavenProject.getProperties().getProperty(REMOTE_SYNCHRONIZER_TARGET_PROPERTY) != null;
    }

    private String getSourceDirectoryForProject(MavenProject mavenProject) {
        if (mavenProject.getProperties().getProperty(REMOTE_SYNCHRONIZER_SOURCE_PROPERTY) == null) {
            return mavenProject.getOutputDirectory();
        } else {
            return mavenProject.getProperties().getProperty(REMOTE_SYNCHRONIZER_SOURCE_PROPERTY);
        }
    }

    private void configureRemoteSyncForModuleFromMaven(Module module, MavenProject mavenProject) {
        final ConfigStateComponent configStateComponent = ServiceManager.getService(module.getProject(), ConfigStateComponent.class);
        final Config configState = configStateComponent.getState();
        final List<TargetMappings> targetMappings = configState != null ? configState.getTargetMappings() : new ArrayList<TargetMappings>();
        TargetMappings mavenTargetMappings = getMavenTargetMappings(targetMappings);

        addModuleMappingToMavenMappings(mavenTargetMappings, mavenProject);
        if (configState != null) {
            configState.fireConfigChanged();
        }

        LOGGER.warn("Configured remoteSync via Maven for module: [" + module.getName() + "] with sourceDirectory [" + getSourceDirectoryForProject(mavenProject) + "] and targetDirectory [" + getTargetDirectoryForProject(mavenProject) + "]");
    }

    private void addModuleMappingToMavenMappings(TargetMappings mavenTargetMappings, final MavenProject mavenProject) {
        SynchroMapping[] synchroMappings = mavenTargetMappings.getSynchroMappings();
        synchroMappings = this.removeCurrentModuleSynchroMapping(synchroMappings, mavenProject);
        SynchroMapping moduleSynchroMapping = new SynchroMapping(getTargetDirectoryForProject(mavenProject), getSourceDirectoryForProject(mavenProject), true);
        synchroMappings = (SynchroMapping[]) ArrayUtils.add(synchroMappings, moduleSynchroMapping);
        mavenTargetMappings.setSynchroMappings(synchroMappings);
    }

    private TargetMappings getMavenTargetMappings(List<TargetMappings> targetMappings) {
        for (TargetMappings currentTargetMappings : targetMappings) {
            if (MAVEN_TARGET_MAPPING_NAME.equals(currentTargetMappings.getName())) {
                return currentTargetMappings;
            }
        }
        return addMavenTargetMappings(targetMappings);
    }

    private TargetMappings addMavenTargetMappings(List<TargetMappings> targetMappings) {
        TargetMappings mavenMappings = new TargetMappings();
        mavenMappings.setActive(true);
        mavenMappings.setExecuteOnCompile(true);
        mavenMappings.setName(MAVEN_TARGET_MAPPING_NAME);
        targetMappings.add(mavenMappings);
        return mavenMappings;
    }

    private SynchroMapping[] removeCurrentModuleSynchroMapping(SynchroMapping[] synchroMappings, final MavenProject mavenProject){
        for (SynchroMapping synchroMapping : synchroMappings) {
            try {
                if (new File(synchroMapping.getSrcPath()).getCanonicalPath().equals(new File(getSourceDirectoryForProject(mavenProject)).getCanonicalPath())) {
                    return (SynchroMapping[]) ArrayUtils.removeElement(synchroMappings, synchroMapping);
                }
            } catch (IOException e) {
                LOGGER.warn("Exception getting file paths.", e);
            }
        }
        return synchroMappings;
    }
}
