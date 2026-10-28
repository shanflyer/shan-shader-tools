package dev.shanflyer.glsl.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class CompilerLog {
    private static final Pattern glslangPattern_ =
        Pattern.compile("^(ERROR|WARNING):\\s*(.*?):(\\d+):\\s*(.*)$");
    private static final Pattern nvidiaPattern_ = Pattern.compile(
        "^(\\d+)\\((\\d+)\\)\\s*:\\s*(error|warning)[^:]*:\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern gccPattern_ =
        Pattern.compile("^(.*?):(\\d+)(?::(\\d+))?:\\s*(error|warning)(?:[^:]*):\\s*(.*)$",
                        Pattern.CASE_INSENSITIVE);
    public record Diagnostic(boolean error, String source, int line, String message) {}
    private CompilerLog() {
    }

    public static List<Diagnostic> Parse(String output) {
        final var diagnostics = new ArrayList<Diagnostic>();
        for (final String line : output.split("\\R")) {
            var match = glslangPattern_.matcher(line.trim());
            if (match.matches()) {
                diagnostics.add(new Diagnostic(match.group(1).equals("ERROR"), match.group(2),
                                               ParseLine(match.group(3)), match.group(4)));
                continue;
            }
            match = nvidiaPattern_.matcher(line.trim());
            if (match.matches()) {
                diagnostics.add(new Diagnostic(match.group(3).equalsIgnoreCase("error"),
                                               match.group(1), ParseLine(match.group(2)),
                                               match.group(4)));
                continue;
            }
            match = gccPattern_.matcher(line.trim());
            if (match.matches()) {
                diagnostics.add(new Diagnostic(match.group(4).equalsIgnoreCase("error"),
                                               match.group(1), ParseLine(match.group(2)),
                                               match.group(5)));
            }
        }
        return List.copyOf(diagnostics);
    }

    private static int ParseLine(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
