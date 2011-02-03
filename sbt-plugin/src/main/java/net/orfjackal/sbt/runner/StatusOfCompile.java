// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: omid
 * Date: 2/3/11
 * Time: 7:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class StatusOfCompile {
    private boolean success;
    private List<StatusError> errors = new ArrayList<StatusError>();

    public void addErrorInSource(StatusError message) {
        errors.add(message);
    }
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<StatusError> getErrors() {
        return errors;
    }


}
