package dev.shanflyer.glsl.diagnostics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class ShaderCompiler {
    public record Request(Path file, String text, Path projectDirectory, CompilerOptions options,
                          Map<Path, String> unsavedDocuments) {}
    public record
        Outcome(List<CompilerLog.Diagnostic> diagnostics,
                Map<Integer, SourceTransform.Origin> origins, String problem, String stage) {}

    private ShaderCompiler() {
    }

    public static Outcome Compile(Request request, Runnable checkCanceled) {
        final CompilerOptions options = request.options();
        if (!options.enabled) {
            return Problem("");
        }
        final Path compiler = CompilerLocator.Find(options.compilerPath);
        if (compiler == null) {
            return Problem("GLSL compiler not found. Set glslangValidator in Settings | " +
                           "Languages & Frameworks | Shan GLSL.");
        }
        Path temporary = null;
        Process process = null;
        try {
            checkCanceled.run();
            final List<Path> directories = new ArrayList<>();
            for (final String line : options.includeDirectories.split("\\R")) {
                if (line.isBlank()) {
                    continue;
                }
                final Path path = Path.of(CommandArguments.ExpandEnvironment(line.trim()));
                directories.add(
                    (path.isAbsolute() ? path : request.projectDirectory().resolve(path))
                        .normalize());
            }
            final SourceTransform.Expanded expanded =
                SourceTransform.Expand(request.file(), request.text(), directories, path -> {
                    checkCanceled.run();
                    final String unsaved =
                        request.unsavedDocuments().get(path.toAbsolutePath().normalize());
                    return unsaved != null ? unsaved : SourceTransform.ReadDisk(path);
                });
            final String stage = ShaderStage.Detect(request.file().getFileName().toString(),
                                                    expanded.text(), options.fallbackStage);
            temporary = Files.createTempDirectory("shan-glsl-");
            final Path shader = temporary.resolve("shader." + stage);
            final Path binary = temporary.resolve("shader.spv");
            final Path output = temporary.resolve("compiler.log");
            Files.writeString(shader, expanded.text(), StandardCharsets.UTF_8);
            final List<String> command = Command(compiler, shader, binary, stage, options);
            process = new ProcessBuilder(command)
                          .directory(temporary.toFile())
                          .redirectErrorStream(true)
                          .redirectOutput(output.toFile())
                          .start();
            final long deadline =
                System.nanoTime() +
                TimeUnit.MILLISECONDS.toNanos(Math.clamp(options.timeoutMillis, 250, 60000));
            while (!process.waitFor(50, TimeUnit.MILLISECONDS)) {
                checkCanceled.run();
                if (System.nanoTime() >= deadline) {
                    return Problem("GLSL compiler timed out after " + options.timeoutMillis +
                                   " ms. Check compiler settings.");
                }
                if (Files.size(output) > 1024 * 1024) {
                    return Problem("GLSL compiler output exceeded 1 MiB.");
                }
            }
            checkCanceled.run();
            final String log;
            try (final var stream = Files.newInputStream(output)) {
                log = new String(stream.readNBytes(1024 * 1024), StandardCharsets.UTF_8);
            }
            final List<CompilerLog.Diagnostic> diagnostics = CompilerLog.Parse(log);
            if (process.exitValue() != 0 &&
                diagnostics.stream().noneMatch(CompilerLog.Diagnostic::error)) {
                final String summary = log.isBlank() ? "No diagnostic output" : log.strip();
                return new Outcome(diagnostics, expanded.origins(),
                                   "GLSL compiler exited with " + process.exitValue() + ": " +
                                       summary.substring(0, Math.min(summary.length(), 2000)),
                                   stage);
            }
            return new Outcome(diagnostics, expanded.origins(), "", stage);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Problem("GLSL compilation interrupted.");
        } catch (IOException | IllegalArgumentException exception) {
            return Problem("GLSL compilation failed: " + exception.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                try {
                    process.waitFor(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
            if (temporary != null) {
                try (final var paths = Files.walk(temporary)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            path.toFile().deleteOnExit();
                        }
                    });
                } catch (IOException ignored) {
                    temporary.toFile().deleteOnExit();
                }
            }
        }
    }

    public static List<String> Command(Path compiler, Path shader, Path binary, String stage,
                                       CompilerOptions options) {
        final List<String> command = new ArrayList<>();
        command.add(compiler.toString());
        final var extra = CommandArguments.Split(options.arguments);
        final boolean hasFile = extra.stream().anyMatch(argument -> argument.contains("{file}"));
        if (!hasFile) {
            command.add(shader.toString());
        }
        if (options.target.equals("Vulkan")) {
            command.addAll(List.of("-V", "--target-env", "vulkan1.3"));
        }
        for (final String argument : extra) {
            command.add(argument.replace("{file}", shader.toString())
                            .replace("{stage}", stage)
                            .replace("{output}", binary.toString()));
        }
        if (!options.target.equals("Custom")) {
            command.addAll(List.of("-S", stage));
            // Keep generated binaries inside the disposable compiler directory.
            if (options.target.equals("Vulkan")) {
                command.addAll(List.of("-o", binary.toString()));
            }
        }
        return List.copyOf(command);
    }

    private static Outcome Problem(String message) {
        return new Outcome(List.of(), Map.of(), message, "");
    }
}
