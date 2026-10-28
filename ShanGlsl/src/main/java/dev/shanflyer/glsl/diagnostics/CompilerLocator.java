package dev.shanflyer.glsl.diagnostics;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CompilerLocator {
    private CompilerLocator() {
    }

    public static Path Find(String configured) {
        try {
            if (!configured.isBlank()) {
                final Path path = Path.of(CommandArguments.ExpandEnvironment(configured));
                return IsExecutable(path) ? path.toAbsolutePath() : FindOnPath(path.toString());
            }
            final String sdk = System.getenv("VULKAN_SDK");
            final List<Path> candidates = new ArrayList<>();
            if (sdk != null && !sdk.isBlank()) {
                candidates.add(Path.of(sdk, "Bin", "glslangValidator.exe"));
                candidates.add(Path.of(sdk, "bin", "glslangValidator"));
                candidates.add(Path.of(sdk, "bin", "glslang"));
            }
            for (final Path candidate : candidates) {
                if (IsExecutable(candidate)) {
                    return candidate;
                }
            }
            for (final String name :
                 List.of("glslangValidator.exe", "glslangValidator", "glslang")) {
                final Path path = FindOnPath(name);
                if (path != null) {
                    return path;
                }
            }
        } catch (InvalidPathException exception) {
            return null;
        }
        return null;
    }

    private static boolean IsExecutable(Path path) {
        return Files.isRegularFile(path) && Files.isExecutable(path);
    }

    private static Path FindOnPath(String name) {
        if (name.contains("/") || name.contains("\\")) {
            return null;
        }
        final String environment = System.getenv("PATH");
        if (environment == null) {
            return null;
        }
        for (final String directory : environment.split(java.io.File.pathSeparator)) {
            try {
                final Path path = Path.of(directory.replace("\"", ""), name);
                if (IsExecutable(path)) {
                    return path.toAbsolutePath();
                }
            } catch (InvalidPathException ignored) { /* Skip invalid unrelated PATH entries. */
            }
        }
        return null;
    }
}
