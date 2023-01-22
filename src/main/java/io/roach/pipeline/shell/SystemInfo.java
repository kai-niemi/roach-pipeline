package io.roach.pipeline.shell;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@ShellCommandGroup(CommandGroups.ADMIN)
public class SystemInfo {
    @Autowired
    private Console console;

    @ShellMethod(value = "Print local system information", key = {"system-info", "si"})
    public void systemInfo() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        console.printf(AnsiColor.BRIGHT_YELLOW, ">> OS\n");
        console.printf(AnsiColor.BRIGHT_CYAN," Arch: %s | OS: %s | Version: %s\n", os.getArch(), os.getName(), os.getVersion());
        console.printf(AnsiColor.BRIGHT_CYAN," Available processors: %d\n", os.getAvailableProcessors());
        console.printf(AnsiColor.BRIGHT_CYAN," Load avg: %f\n", os.getSystemLoadAverage());

        RuntimeMXBean r = ManagementFactory.getRuntimeMXBean();
        console.printf(AnsiColor.BRIGHT_YELLOW, ">> Runtime\n");
        console.printf(AnsiColor.BRIGHT_CYAN," Uptime: %s\n", r.getUptime());
        console.printf(AnsiColor.BRIGHT_CYAN," VM name: %s | Vendor: %s | Version: %s\n", r.getVmName(), r.getVmVendor(), r.getVmVersion());

        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        console.printf(AnsiColor.BRIGHT_YELLOW, ">> Threads\n");
        console.printf(AnsiColor.BRIGHT_CYAN," Peak threads: %d\n", t.getPeakThreadCount());
        console.printf(AnsiColor.BRIGHT_CYAN," Live thread #: %d\n", t.getThreadCount());
        console.printf(AnsiColor.BRIGHT_CYAN," Total started threads: %d\n", t.getTotalStartedThreadCount());
        console.printf(AnsiColor.BRIGHT_CYAN," Current thread CPU time: %d\n", t.getCurrentThreadCpuTime());
        console.printf(AnsiColor.BRIGHT_CYAN," Current thread User time #: %d\n", t.getCurrentThreadUserTime());

        Arrays.stream(t.getAllThreadIds()).sequential().forEach(value -> {
            console.printf(AnsiColor.BRIGHT_CYAN," Thread (%d): %s %s\n", value,
                    t.getThreadInfo(value).getThreadName(),
                    t.getThreadInfo(value).getThreadState().toString()
            );
        });

        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        console.printf(AnsiColor.BRIGHT_YELLOW, ">> Memory\n");
        console.printf(AnsiColor.BRIGHT_CYAN," Heap: %s\n", m.getHeapMemoryUsage().toString());
        console.printf(AnsiColor.BRIGHT_CYAN," Non-heap: %s\n", m.getNonHeapMemoryUsage().toString());
        console.printf(AnsiColor.BRIGHT_CYAN," Pending GC: %s\n", m.getObjectPendingFinalizationCount());
    }

}
