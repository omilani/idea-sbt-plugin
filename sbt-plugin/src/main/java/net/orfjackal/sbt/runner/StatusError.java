// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: omid
 * Date: 2/3/11
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatusError {

    private boolean warning;
    private String file, message;
    private int lineNo;

    public StatusError(Pattern pattern, String line) {
        this.warning = line.contains("warning");
        Matcher match = pattern.matcher(line);
        if (!match.matches())
           throw new IllegalArgumentException("not a valid SBT compile error line: " + line);
        file = match.group(1);
        message = match.group(3);
        // sbt output no starts from 1
        lineNo = Integer.parseInt(match.group(2)) - 1;
    }

    public boolean isWarning() {
        return warning;
    }

    public String getFile() {
        return file;
    }

    public String getMessage() {
        return message;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void print(Project project, ConsoleView view) {
        VirtualFile file = project.getBaseDir().getFileSystem().findFileByPath(this.file);
        String relative = file.getPath().substring(project.getBaseDir().getPath().length());
        OpenFileHyperlinkInfo link = new OpenFileHyperlinkInfo(project, file, lineNo);
        if (warning)
            view.print("[warning] ", ConsoleViewContentType.NORMAL_OUTPUT);
        else
            view.print("[error] ", ConsoleViewContentType.ERROR_OUTPUT);
        view.printHyperlink(relative, link);
        view.print("\t" + message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }
}
