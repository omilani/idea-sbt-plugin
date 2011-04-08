// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

/**
 * compile is called exactly after status when result is available
 * status is called only if it has changed
 */
public interface StatusListener {
    public void status(Status status);
    public void compile(StatusOfCompile status);
}
