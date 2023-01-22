package io.roach.pipeline.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Quit;

import io.roach.pipeline.util.CockroachFacts;

@ShellComponent
@ShellCommandGroup(CommandGroups.ADMIN)
public class Exit implements Quit.Command {
    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private Console console;

    @ShellMethod(value = "Exit the shell", key = {"q", "quit", "exit"})
    public void quit() {
        console.printf(AnsiColor.BRIGHT_CYAN, "Did you know? ");
        console.printf(AnsiColor.BRIGHT_YELLOW, "%s\n", CockroachFacts.randomFact());
        applicationContext.close();
        throw new ExitRequest();
    }
}
