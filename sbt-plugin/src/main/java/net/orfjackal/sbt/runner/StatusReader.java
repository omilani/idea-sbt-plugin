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


    public static final Pattern compile = Pattern.compile("\\[info\\] Compiling .* sources...");
    public static final Pattern wait_change = Pattern.compile(".* Waiting for source changes\\.\\.\\. \\(press enter to interrupt\\)");
    /* a little hacky: the '> ' message of input does not end with line break, so should use
       another method than readLine if we want to read it, but i was too lazy. so i use this final message
       but it's also printed just before wait_change, which is corrected in the next line.
     */
    public static final Pattern wait_input = Pattern.compile("\\[info\\] Total time: .* s, completed .*");
    public static final Pattern final_success = Pattern.compile("\\[success\\] Successful\\.");
    public static final Pattern final_error = Pattern.compile("\\[error\\] Error running .*: Compilation failed");
    public final Pattern error;

    private BufferedReader output;
    private Status status = StatusReader.Status.wait_input;
    private StatusOfCompile current;
    private StatusOfCompile previous;
    private StatusListener listener;

    public StatusReader(Reader output, String projectDir) {
        this.output = new BufferedReader(output);
        error = Pattern.compile("\\[error\\] (" + projectDir.replace("\\", "\\\\") + "[^:]*):([^:]*): (.*)");
        current = new StatusOfCompile();
        previous = new StatusOfCompile();
    }

    public void close() {
        try { output.close(); } catch (Exception e) { }
    }

    /**
     * problem here: set project doesn't show the wait_input message, so we never stop status.working
     * are there other commands like that? find them and see what to do
     * the proper solution would be to not use bufferedReader so we would be able to read prompt before command entered
     */
    public void run() {
        try { while (true) {
            String line = output.readLine();
        synchronized (this) {
            if (line == null) throw new EOFException();
            else if (wait_change.matcher(line).matches()) finished(Status.wait_change); // see this.wait_input
            else if (wait_input.matcher(line).matches()) this.status = Status.wait_input;
            else if (final_success.matcher(line).matches()) current.setSuccess(true);
            else if (final_error.matcher(line).matches()) current.setSuccess(false);
            else if (error.matcher(line).matches()) current.addErrorInSource(new StatusError(error, line, output));
            else this.status = StatusReader.Status.working;
        } } } catch (IOException ioe) {
            finished(StatusReader.Status.closed);
        }
    }

    public synchronized Status waitForWorking() {
        // wait 1/3 seconds, maybe sbt will discover changes and will begin working - sbt waits 1 sec. for checking changes
        try { wait(333); } catch (InterruptedException e) {}
        while (status == Status.working) try {
            wait(); } catch (InterruptedException e) {}
        return status;
    }
    public StatusOfCompile getCompileStatus() {
        return previous;
    }
    private void finished(Status status) {
        this.status = status;
        if (listener != null)
            listener.update(current);
        previous = current;
        current = new StatusOfCompile();
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
        working, closed, wait_change, wait_input,
    }
}
