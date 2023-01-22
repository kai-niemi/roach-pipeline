package io.roach.pipeline;

import java.util.Arrays;
import java.util.LinkedList;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.boot.ApplicationRunnerAutoConfiguration;
import org.springframework.shell.jline.InteractiveShellRunner;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.util.StringUtils;

import io.roach.pipeline.config.ApplicationProfiles;

@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        ApplicationRunnerAutoConfiguration.class // We want control
})
@Configuration
public class Application implements ApplicationRunner, PromptProvider {
    private static void printHelpAndExit(String message) {
        System.out.println("Usage: java --jar pipeline.jar <options> [args..]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("--help               this help");
        System.out.println("--disable-shell      disable interactive shell");
        System.out.println("--disable-api        disable REST API (offline mode)");
        System.out.println();
        System.out.println("Arguments refers to Spring profiles to activate:");
        System.out.println("     crdb - Use CockroachDB for job metadata (active by default)");
        System.out.println("       h2 - Use H2 for job metadata");
        System.out.println("  verbose - Enable verbose SQL trace logging");
        System.out.println();
        System.out.println("All other options are passed to the interactive shell.");
        System.out.println();
        System.out.println(message);
        System.exit(0);
    }

    public static void main(String[] args) {
        boolean offline = false;
        LinkedList<String> argsList = new LinkedList<>(Arrays.asList(args));
        LinkedList<String> profiles = new LinkedList<>();
        LinkedList<String> passThroughArgs = new LinkedList<>();

        while (!argsList.isEmpty()) {
            String arg = argsList.pop();
            if (arg.startsWith("--")) {
                if (arg.equals("--help")) {
                    printHelpAndExit("");
                } else if (arg.equals("--disable-shell")) {
                    System.setProperty("--spring.shell.interactive.enabled", "false");
                } else if (arg.equals("--offline")) {
                    offline = true;
                } else {
                    passThroughArgs.add(arg);
                }
            } else {
                if (ApplicationProfiles.all().contains(arg)) {
                    profiles.add(arg);
                } else {
                    printHelpAndExit("No such profile: " + arg);
                }
            }
        }

        if (!profiles.isEmpty()) {
            System.setProperty("spring.profiles.active", StringUtils.collectionToCommaDelimitedString(profiles));
        }

        new SpringApplicationBuilder(Application.class)
                .web(offline ? WebApplicationType.NONE : WebApplicationType.SERVLET)
                .headless(true)
                .logStartupInfo(true)
                .run(passThroughArgs.toArray(new String[] {}));
    }

    @Autowired
    @Lazy
    private InteractiveShellRunner shellRunner;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        shellRunner.run(args);
    }

    @Override
    public AttributedString getPrompt() {
        return new AttributedString("pipeline:$ ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
    }
}

                                   