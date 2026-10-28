package dev.shanflyer.glsl.diagnostics;

public final class CompilerOptions {
    public boolean enabled = true;
    public String compilerPath = "";
    public String target = "Vulkan";
    public String arguments = "";
    public String includeDirectories = "";
    public String fallbackStage = "frag";
    public int timeoutMillis = 5000;
    public String userKeywords1 = "";
    public String userKeywords2 = "";

    public CompilerOptions Copy() {
        final CompilerOptions copy = new CompilerOptions();
        copy.enabled = enabled;
        copy.compilerPath = compilerPath;
        copy.target = target;
        copy.arguments = arguments;
        copy.includeDirectories = includeDirectories;
        copy.fallbackStage = fallbackStage;
        copy.timeoutMillis = timeoutMillis;
        copy.userKeywords1 = userKeywords1;
        copy.userKeywords2 = userKeywords2;
        return copy;
    }
}
