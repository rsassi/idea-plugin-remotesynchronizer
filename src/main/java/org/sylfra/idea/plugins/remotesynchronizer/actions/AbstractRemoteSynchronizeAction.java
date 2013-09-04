package org.sylfra.idea.plugins.remotesynchronizer.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.sylfra.idea.plugins.remotesynchronizer.RemoteSynchronizerPlugin;
import org.sylfra.idea.plugins.remotesynchronizer.synchronizing.Synchronizer;
import org.sylfra.idea.plugins.remotesynchronizer.synchronizing.SynchronizerThreadManager;
import org.sylfra.idea.plugins.remotesynchronizer.utils.ConfigPathsManager;
import org.sylfra.idea.plugins.remotesynchronizer.utils.Utils;

/**
 * Common code for {@link RemoteSynchronizeOpenedAction} and
 * {@link RemoteSynchronizeSelectedAction}
 *
 * @author <a href="mailto=sylfradev@yahoo.fr">Sylvain FRANCOIS</a>
 */
public abstract class AbstractRemoteSynchronizeAction extends AnAction {
    /**
     * Copy selected/opened files
     */
    public void actionPerformed(AnActionEvent e) {
        VirtualFile[] files = getFiles(Utils.getPlugin(e), e.getDataContext());
        Synchronizer.performSynchronization(Utils.getPlugin(e), files);
    }


    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(isEnabled(e));
    }

    protected boolean isEnabled(AnActionEvent e) {
        RemoteSynchronizerPlugin plugin = Utils.getPlugin(e);
        return (plugin != null)
                && ((plugin.getConfig().getGeneralOptions().isAllowConcurrentRuns())
                || (!plugin.getCopierThreadManager().hasRunningSynchro()));
    }

    protected abstract VirtualFile[] getFiles(RemoteSynchronizerPlugin plugin,
                                              DataContext dataContext);
}
