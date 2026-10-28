package dev.shanflyer.glsl.language;

import com.intellij.lang.Language;

public final class GlslLanguage extends Language {
    public static final GlslLanguage instance = new GlslLanguage();

    private GlslLanguage() {
        super("ShanGLSL");
    }
}
