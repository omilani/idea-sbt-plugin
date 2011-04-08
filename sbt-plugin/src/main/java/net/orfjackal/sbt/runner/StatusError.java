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
 * TODO: detect tab length, now is assumed 4
 */
public class StatusError {

    public static final Pattern last_line = Pattern.compile("\\[.*\\][\t ]*\\^");

    private boolean warning;
    private String file, message;
    private List<String> info = new ArrayList<String>();
    private int lineNo, columnNo;

    public StatusError(Pattern pattern, String line, BufferedReader reader) {
        this.warning = line.contains("warning");
        Matcher match = pattern.matcher(line);
        if (!match.matches())
           throw new IllegalArgumentException("not a valid SBT compile error line: " + line);
        file = match.group(1);
        message = match.group(3);
        // sbt line number starts from 1
        lineNo = Integer.parseInt(match.group(2)) - 1;
        while (true) try {
            line = reader.readLine();
            // this shouldn't happen but anyway
            if (!line.startsWith(getBeginning())) return;
            if (last_line.matcher(line).matches()) {
                columnNo = 0;
                for (int charNo = getBeginning().length(); line.charAt(charNo) != '^'; charNo++)
                    if (line.charAt(charNo) == '\t') do {
                        columnNo++;
                    } while (columnNo % getTabLength() != 0);
                    else columnNo++;
                info.remove(info.size() - 1);
                return;
            } else
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
        OpenFileHyperlinkInfo link = new OpenFileHyperlinkInfo(project, file, lineNo, columnNo);
        if (warning)
            view.print("[warning] ", ConsoleViewContentType.NORMAL_OUTPUT);
        else
            view.print("[error] ", ConsoleViewContentType.ERROR_OUTPUT);
        view.printHyperlink(relative, link);
        view.print("\t" + message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
        for (String line : info) view.print("\t" + line + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);

    }

    public int getTabLength() {
        return 4;
    }
}
