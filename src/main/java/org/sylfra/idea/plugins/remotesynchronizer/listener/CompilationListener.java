package org.sylfra.idea.plugins.remotesynchronizer.listener;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.sylfra.idea.plugins.remotesynchronizer.RemoteSynchronizerPlugin;
import org.sylfra.idea.plugins.remotesynchronizer.synchronizing.Synchronizer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Thomas Demande
 */
public class CompilationListener implements CompilationStatusListener {

    public void compilationFinished(boolean b, int i, int i2, final CompileContext compileContext) {
        //To ensure usable sync feature, every file will be sync'ed after a compilation
        final VirtualFile[] virtualFiles = getAllSynchronizedFiles(RemoteSynchronizerPlugin.getInstance(compileContext.getProject()));

        // Sending synchronization order, in the UI thread. Compiler context might be null if no SDK has been defined in the project
        UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            public void run() {
                if (compileContext != null) {
                    Synchronizer.performSynchronization(RemoteSynchronizerPlugin.getInstance(compileContext.getProject()), virtualFiles, true);
                }
            }
        });
    }

    public void fileGenerated(final String s, final String s2) {
        //Nothing special to perform, as we take all files from Synchronizer config, not files just being compiled
    }

    private VirtualFile[] getAllSynchronizedFiles(RemoteSynchronizerPlugin plugin) {
        return ProjectRootManager.getInstance(plugin.getProject()).getContentRoots();
    }
}
