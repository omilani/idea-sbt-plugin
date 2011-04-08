// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import net.orfjackal.sbt.runner.Status;
import net.orfjackal.sbt.runner.StatusListener;
import net.orfjackal.sbt.runner.StatusOfCompile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
* TODO:
 * show icon instead of text, find some icons
 * indicatingComponent is very small, move it to left and show something bigger
 * click on it should open the sbt window
*/
public class SbtStatusWidget extends JLabel implements StatusListener, ProjectComponent {


    private SbtRunnerComponent sbt;
    private Project project;

    public SbtStatusWidget(Project project) {
        super((MessageBundle.message("sbt.tasks.status.begin")));
        sbt = project.getComponent(SbtRunnerComponent.class);
        this.project = project;
        System.out.println("create");
    }

    public void status(Status status) {
        setForeground(Color.BLACK);
        setText(MessageBundle.message("sbt.tasks.status." + status));
    }
    public void compile(StatusOfCompile status) {
        setText(MessageBundle.message("sbt.tasks.status." + (status.isSuccess() ? "success" : "failure")));
        if (!status.isSuccess()) setForeground(Color.RED);
    }

    public void projectOpened() {
        sbt.addStatusListener(this);
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar == null) return;
        statusBar.addCustomIndicationComponent(this);
        System.out.println("open");
    }

    public void projectClosed() {
        sbt.removeStatusListener(this);
    }

    @NotNull
    public String getComponentName() {
        return "sbt-status";
    }

    public void initComponent() {
    }

    public void disposeComponent() {
        sbt.removeStatusListener(this);
    }
}
