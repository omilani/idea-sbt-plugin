// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import org.junit.Test;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: omid
 * Date: 2/3/11
 * Time: 7:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class StatusReaderTest {

    @Test
    public void simple() throws IOException {
        String projectDir = "D:\\work\\workspace-0.9\\smp-parent";
        Pattern errorPattern = Pattern.compile("\\[error\\] (" + projectDir.replace("\\", "\\\\") + "[^:]*):([^:]*): (.*)");
        String str = "[error] D:\\work\\workspace-0.9\\smp-parent\\smp-chat\\src\test\\java\\ir\\smp\\chat\\x\\OpenfireTest.scala:53: ')' expected but '}' found.";
        StatusError error = new StatusError(errorPattern, str);
        assertFalse(error.isWarning());
        assertEquals("')' expected but '}' found.", error.getMessage());
        assertEquals(53, error.getLineNo());
        assertEquals("D:\\work\\workspace-0.9\\smp-parent\\smp-chat\\src\test\\java\\ir\\smp\\chat\\x\\OpenfireTest.scala", error.getFile());
        str = "[error] D:\\work\\workspace-0.9\\smp-parent\\smp-wicket\\src\\main\\java\\ir\\smp\\addon\\wicket\\StateList.java:33: warning: non-varargs call of varargs method with inexact argument type for last parameter;";
        error = new StatusError(errorPattern, str);
        assertTrue(error.isWarning());


        PipedWriter writer = new PipedWriter();
        StatusReader status = new StatusReader(new PipedReader(writer), "D:\\work\\workspace-0.9\\smp-parent") ;
        status.start();
        writer.write("> \n");
//        status.waitIfWorking();
//        assertEquals(StatusReader.Status.finished, status.getStatus());
        writer.write("4. Waiting for source changes... (press enter to interrupt)\n");
        try { Thread.sleep(1000); } catch (InterruptedException e) { }
        status.waitIfWorking();
        assertEquals(StatusReader.Status.finished, status.getStatus());
        writer.write("some working\n");
        writer.write("some more working\n");
        writer.write("still more working\n");
        try { Thread.sleep(1500); } catch (InterruptedException e) { }
        assertEquals(StatusReader.Status.working, status.getStatus());
        writer.write("[info] Compiling test sources...\n");
        writer.write("[error] D:\\work\\workspace-0.9\\smp-parent\\smp-chat\\src\test\\java\\ir\\smp\\chat\\x\\OpenfireTest.scala:53: ')' expected but '}' found.\n");
        writer.write("[error]   }\n");
        writer.write("[error]   ^\n");
        writer.write("[error] one error found\n");
        writer.write("[info] == smp-chat / test-compile ==\n");
        writer.write("5. Waiting for source changes... (press enter to interrupt)\n");
        status.waitIfWorking();
        assertEquals(StatusReader.Status.finished, status.getStatus());
        assertFalse(status.getCompileStatus().isSuccess());
        assertFalse(status.getCompileStatus().getErrors().get(0).isWarning());
        assertEquals("D:\\work\\workspace-0.9\\smp-parent\\smp-chat\\src\test\\java\\ir\\smp\\chat\\x\\OpenfireTest.scala", status.getCompileStatus().getErrors().get(0).getFile());
        assertEquals(1, status.getCompileStatus().getErrors().size());

        writer.write("[error] D:\\work\\workspace-0.9\\smp-parent\\smp-wicket\\src\\main\\java\\ir\\smp\\addon\\wicket\\StateList.java:33: warning: non-varargs call of varargs method with inexact argument type for last parameter;\n" +
                "[error] cast to java.lang.Object for a varargs call\n" +
                "[error] cast to java.lang.Object[] for a non-varargs call and to suppress this warning\n" +
                "[error] \t\treturn new SmpUri(\"state\").setCall(\"get\", \"list\", args);\n" +
                "[error] \t\t                                                  ^\n" +
                "[error] Note: Some input files use or override a deprecated API.\n" +
                "[error] Note: Recompile with -Xlint:deprecation for details.\n" +
                "[error] Note: Some input files use unchecked or unsafe operations.\n" +
                "[success] Successful.\n");
        writer.write("5. Waiting for source changes... (press enter to interrupt)\n");
        try { Thread.sleep(1500); } catch (InterruptedException e) { }
        status.waitIfWorking();
        assertEquals(StatusReader.Status.finished, status.getStatus());
        assertEquals(1, status.getCompileStatus().getErrors().size());
        assertTrue(status.getCompileStatus().getErrors().get(0).isWarning());
        assertTrue(status.getCompileStatus().isSuccess());

        status.close();
    }
}
