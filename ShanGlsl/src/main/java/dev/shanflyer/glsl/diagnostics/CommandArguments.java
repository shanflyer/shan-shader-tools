package dev.shanflyer.glsl.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class CommandArguments {
    private static final Pattern environmentPattern_ =
        Pattern.compile("%([A-Za-z_][A-Za-z0-9_]*)%|\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");
    private CommandArguments() {
    }

    public static String ExpandEnvironment(String value) {
        return environmentPattern_.matcher(value).replaceAll(match -> {
            final String name = match.group(1) != null ? match.group(1) : match.group(2);
            final String environment = System.getenv(name);
            return java.util.regex.Matcher.quoteReplacement(environment == null ? match.group()
                                                                                : environment);
        });
    }

    public static List<String> Split(String command) {
        final var result = new ArrayList<String>();
        final StringBuilder token = new StringBuilder();
        char quote = 0;
        boolean started = false;
        for (int i = 0; i < command.length(); ++i) {
            final char character = command.charAt(i);
            if (character == '\\' && i + 1 < command.length() && command.charAt(i + 1) == '"') {
                token.append('"');
                ++i;
                started = true;
            } else if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                } else {
                    token.append(character);
                }
                started = true;
            } else if (character == '"' || character == '\'') {
                quote = character;
                started = true;
            } else if (Character.isWhitespace(character)) {
                if (started) {
                    result.add(ExpandEnvironment(token.toString()));
                }
                token.setLength(0);
                started = false;
            } else {
                token.append(character);
                started = true;
            }
        }
        if (quote != 0) {
            throw new IllegalArgumentException("Unclosed quote in compiler arguments");
        }
        if (started) {
            result.add(ExpandEnvironment(token.toString()));
        }
        return result;
    }
}
