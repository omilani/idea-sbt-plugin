// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import com.intellij.execution.filters.*;
import com.intellij.execution.ui.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.vfs.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

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
    private List<String> info = new ArrayList<String>();
    private int lineNo;

    public StatusError(Pattern pattern, String line, BufferedReader reader) {
        this.warning = line.contains("warning");
        Matcher match = pattern.matcher(line);
        if (!match.matches())
           throw new IllegalArgumentException("not a valid SBT compile error line: " + line);
        file = match.group(1);
        message = match.group(3);
        // sbt output no starts from 1
        lineNo = Integer.parseInt(match.group(2)) - 1;
        for (int i = 0; i < 5; i++) try {
            line = reader.readLine();
            if (!line.startsWith(getBeginning())) return;
            info.add(line.substring(getBeginning().length()));
        } catch (Exception e) { return; }
    }

    protected String getBeginning() {
        return "[error] ";
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
        for (String line : info) view.print("\t" + line + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);

    }
}
