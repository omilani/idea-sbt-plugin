// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.BeforeRunTask;
import org.jdom.Element;

public class SbtBeforeRunTask extends BeforeRunTask {

    private String action;
    private boolean background;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isBackground() {
        return background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    @Override
    public void writeExternal(Element element) {
        super.writeExternal(element);
        element.setAttribute("background", Boolean.toString(background));
        if (action != null) {
            element.setAttribute("action", action);
        }
    }

    @Override
    public void readExternal(Element element) {
        super.readExternal(element);
        action = element.getAttributeValue("action");
        background = "true".equals(element.getAttributeValue("background"));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        SbtBeforeRunTask that = (SbtBeforeRunTask) o;

        if (action != null ? !action.equals(that.action) : that.action != null) {
            return false;
        }
        if (background != that.background) return false;
        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }
}
