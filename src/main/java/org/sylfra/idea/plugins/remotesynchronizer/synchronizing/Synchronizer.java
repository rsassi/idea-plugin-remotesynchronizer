package org.sylfra.idea.plugins.remotesynchronizer.synchronizing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.sylfra.idea.plugins.remotesynchronizer.RemoteSynchronizerPlugin;
import org.sylfra.idea.plugins.remotesynchronizer.utils.ConfigPathsManager;

/**
 * @author Thomas Demande
 */
public class Synchronizer {

    public static void performSynchronization(RemoteSynchronizerPlugin plugin, VirtualFile[] files, boolean fromCompilationListener) {
        if (files == null)
            return;

        // Checks configuration allows concurent runs when a synchro is running
        if ((!plugin.getConfig().getGeneralOptions().isAllowConcurrentRuns())
              && (plugin.getCopierThreadManager().hasRunningSynchro())) {
            plugin.getConsolePane().doPopup();
            return;
        }

        if (plugin.getConfig().getGeneralOptions().isSaveBeforeCopy())
            FileDocumentManager.getInstance().saveAllDocuments();

        if (!plugin.getCopierThreadManager().hasRunningSynchro())
            refreshVfsIfJavaSelected(files, plugin.getPathManager());

        SynchronizerThreadManager manager = plugin.getCopierThreadManager();
        manager.launchSynchronization(files, fromCompilationListener);
    }

    public static void performSynchronization(RemoteSynchronizerPlugin plugin, VirtualFile[] files) {
        performSynchronization(plugin, files, false);
    }

    public static void performSynchronization(VirtualFile[] virtualFiles) {
        Project project = ProjectUtil.guessProjectForFile(virtualFiles[0]);
        performSynchronization(RemoteSynchronizerPlugin.getInstance(project), virtualFiles);
    }

    private static void refreshVfsIfJavaSelected(final VirtualFile[] files,
                                          final ConfigPathsManager pathManager) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                for (VirtualFile file : files)
                    if (pathManager.isJavaSource(file)) {
                        LocalFileSystem.getInstance().refresh(false);
                        return;
                    }
            }
        });
    }
}
