package io.roach.pipeline.shell;

import java.util.Locale;

import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class Console {
    private final Terminal terminal;

    public Console(@Autowired @Lazy Terminal terminal) {
        Assert.notNull(terminal, "terminal is null");
        this.terminal = terminal;
    }

    public void information(String text) {
        println(AnsiColor.BRIGHT_CYAN, text);
    }

    public void warning(String text) {
        println(AnsiColor.BRIGHT_YELLOW, text);
    }

    public void error(String text) {
        println(AnsiColor.BRIGHT_RED, text);
    }

    public void printf(AnsiColor color, String format, Object... args) {
        terminal.writer().printf(ansiColor(color, String.format(Locale.US, format, args)));
        terminal.writer().flush();
    }
        
    protected void println(AnsiColor color, String text) {
        terminal.writer().println(ansiColor(color, text));
        terminal.writer().flush();
    }

    protected String ansiColor(AnsiColor color, String message) {
        return AnsiOutput.toString(color, message, AnsiColor.DEFAULT);
    }
}
