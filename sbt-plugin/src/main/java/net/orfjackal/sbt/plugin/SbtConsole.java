// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.filters.*;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.*;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;
import net.orfjackal.sbt.runner.StatusError;
import net.orfjackal.sbt.runner.StatusListener;
import net.orfjackal.sbt.runner.StatusOfCompile;
import net.orfjackal.sbt.runner.StatusReader;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SbtConsole {
    // org.jetbrains.idea.maven.embedder.MavenConsoleImpl

    private static final Logger logger = Logger.getInstance(SbtConsole.class.getName());

    private static final Key<SbtConsole> CONSOLE_KEY = Key.create("SBT_CONSOLE_KEY");

    public static final String CONSOLE_FILTER_REGEXP =
            "\\s" + RegexpFilter.FILE_PATH_MACROS + ":" + RegexpFilter.LINE_MACROS + ":\\s";

    private final String title;
    private final Project project;
    private final ConsoleView consoleView;
    private final ConsoleView compileView;
    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private final SbtRunnerComponent runnerComponent;
    private boolean finished = false;
    private Content compileContent;


    public SbtConsole(String title, Project project, SbtRunnerComponent runnerComponent) {
        this.title = title;
        this.project = project;
        this.consoleView = createConsoleView(project);
        this.compileView = createCompileView(project);
        this.runnerComponent = runnerComponent;
    }

    private static ConsoleView createCompileView(Project project) {
        return TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
    }
    private static ConsoleView createConsoleView(Project project) {
        return createConsoleBuilder(project).getConsole();
    }

    public static TextConsoleBuilder createConsoleBuilder(Project project) {
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);

        Filter[] filters = {new ExceptionFilter(project), new RegexpFilter(project, CONSOLE_FILTER_REGEXP)};
        for (Filter filter : filters) {
            builder.addFilter(filter);
        }
        return builder;
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        finished = true;
    }

    public void attachToProcess(ProcessHandler processHandler, final SbtRunnerComponent runnerComponent) {
        consoleView.attachToProcess(processHandler);
        processHandler.addProcessListener(new ProcessAdapter() {
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run() {
                        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(MessageBundle.message("sbt.console.id"));
                        /* When we retrieve a window from ToolWindowManager before SbtToolWindowFactory is called,
                         * we get an undesirable Content */
                        for (Content each : window.getContentManager().getContents()) {
                            if (each.getUserData(CONSOLE_KEY) == null) {
                                window.getContentManager().removeContent(each, false);
                            }
                        }
                        ensureAttachedToToolWindow(window);
                    }
                });
            }

            public void processTerminated(ProcessEvent event) {
                finish();
            }
        });
    }

    public final void ensureAttachedToToolWindow(ToolWindow window) {
        if (!isOpen.compareAndSet(false, true)) {
            return;
        }

        // org.jetbrains.idea.maven.embedder.MavenConsoleImpl#ensureAttachedToToolWindow
        SimpleToolWindowPanel toolWindowPanel = new SimpleToolWindowPanel(false, true);
        toolWindowPanel.setToolbar(createToolbar());
        toolWindowPanel.setContent(consoleView.getComponent());
        Content content = ContentFactory.SERVICE.getInstance().createContent(toolWindowPanel, title, true);
        compileContent = ContentFactory.SERVICE.getInstance().createContent(compileView.getComponent(), "Compile", true);
        content.putUserData(CONSOLE_KEY, SbtConsole.this);
        compileContent.putUserData(CONSOLE_KEY, SbtConsole.this);

        window.getContentManager().addContent(content);
        window.getContentManager().setSelectedContent(content);
        window.getContentManager().addContent(compileContent);

        removeUnusedTabs(window, content);

        if (!window.isActive()) {
            window.activate(null, false);
        }
    }

    private JComponent createToolbar() {
        JPanel toolbarPanel = new JPanel(new GridLayout());

        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Start SBT", "Start SBT", IconLoader.getIcon("/general/toolWindowRun.png")) {
            @Override
            public void actionPerformed(AnActionEvent event) {
                try {
                    runnerComponent.startIfNotStarted();
                } catch (IOException e) {
                    logger.error("Failed to start SBT", e);
                }
            }

            @Override
            public void update(AnActionEvent event) {
                if (runnerComponent.isSbtAlive()) {
                    event.getPresentation().setEnabled(false);
                } else {
                    event.getPresentation().setEnabled(true);
                }
            }
        });
        toolbarPanel.add(
                ActionManager.getInstance().createActionToolbar("SbtConsoleToolbar", group, false).getComponent());
        return toolbarPanel;
    }

    private void removeUnusedTabs(ToolWindow window, Content content) {
        for (Content each : window.getContentManager().getContents()) {
            if (each.isPinned()) {
                continue;
            }
            if (each == content) {
                continue;
            }

            SbtConsole console = each.getUserData(CONSOLE_KEY);
            if (console == null) {
                continue;
            }

            if (!title.equals(console.title)) {
                continue;
            }

            if (console.isFinished()) {
                window.getContentManager().removeContent(each, false);
            }
        }
    }

    public void attachToCompile(StatusReader status, final boolean focusOnError) {
        status.setStatusListener(new StatusListener() {
            public void update(StatusOfCompile status) {
                // some other action has failed
                if (!status.isSuccess() && status.getErrors().size() == 0)
                    return;

                compileView.clear();
                if (status.isSuccess())
                    compileView.print("Compile Successful\n", ConsoleViewContentType.NORMAL_OUTPUT);
                else {
                    if (focusOnError)
                        forceCompileFocus();
                    compileView.print("Compile Failed\n", ConsoleViewContentType.ERROR_OUTPUT);
                }
                for (StatusError error : status.getErrors())
                    error.print(project, compileView);
            }
        });
    }

    private void forceCompileFocus() {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(MessageBundle.message("sbt.console.id"));
                    if (!window.isActive())
                        window.activate(null, false);
                    if (window.getContentManager().getSelectedContent() != compileContent)
                        window.getContentManager().setSelectedContent(compileContent);
//                    compileView.getComponent().requestFocusInWindow();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
