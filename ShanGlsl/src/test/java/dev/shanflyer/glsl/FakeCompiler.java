package dev.shanflyer.glsl;

/** Child process used to verify cancellation and timeout cleanup. */
public final class FakeCompiler {
    private FakeCompiler() {
    }
    public static void main(String[] arguments) throws Exception {
        Thread.sleep(30000);
    }
}
