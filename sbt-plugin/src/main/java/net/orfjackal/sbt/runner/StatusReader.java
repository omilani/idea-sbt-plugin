// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: omid
 * Date: 2/3/11
 * Time: 6:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class StatusReader implements Runnable {

    public static final String SUCCESS = "[success] Successful.";
    public static final Pattern FINISHED = Pattern.compile("\\[info\\] Total time: .*, completed .*");
    public static final String WAITING = "Waiting for source changes... (press enter to interrupt)";

    private Pattern errorPattern;
    private BufferedReader output;
    private Status status = StatusReader.Status.working;
    private StatusOfCompile current;
    private StatusOfCompile previous;
    private StatusListener listener;

    public StatusReader(Reader output, String projectDir) {
        this.output = new BufferedReader(output);
        errorPattern = Pattern.compile("\\[error\\] (" + projectDir.replace("\\", "\\\\") + "[^:]*):([^:]*): (.*)");
        current = new StatusOfCompile();
        previous = new StatusOfCompile();
    }

    public void close() {
        try { output.close(); } catch (Exception e) { }
    }
    public void run() {
        try { while (true) {
            String line = output.readLine();
            // this may occur between finish & waiting messages
            if (line == null)
                throw new EOFException();
            if (!line.equals("[info] "))
                status = StatusReader.Status.working;
            if (SUCCESS.equals(line))
                current.setSuccess(true);
            else if (FINISHED.matcher(line).matches())
                finished(StatusReader.Status.finished);
            else if (line.endsWith(WAITING))
                finished(StatusReader.Status.finished);
            else try {
                current.addErrorInSource(new StatusError(errorPattern, line, output));
            } catch (IllegalArgumentException e) { }
            synchronized (this) { notify(); }
        } } catch (IOException ioe) {
            finished(StatusReader.Status.closed);
        }
    }

    public Status waitIfWorking() {
        do synchronized (this) {
            try { wait(300); } catch (InterruptedException e) { e.printStackTrace(); }
        }  while (status == Status.working);
        return status;
    }
    public StatusOfCompile getCompileStatus() {
        return previous;
    }
    private synchronized void finished(Status status) {
        this.status = status;
        if (current.isSuccess() || current.getErrors().size() > 0) {
            previous = current;
            current = new StatusOfCompile();
        }
        if (listener != null)
            listener.update(previous);
        notify();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatusListener(StatusListener listener) {
        this.listener = listener;
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public enum Status {
        working,
        finished,
        closed,
    }
}
