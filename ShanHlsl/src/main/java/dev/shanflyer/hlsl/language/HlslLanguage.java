package dev.shanflyer.hlsl.language;

import com.intellij.lang.Language;

public final class HlslLanguage extends Language {
    public static final HlslLanguage instance = new HlslLanguage();

    private HlslLanguage() {
        super("ShanHLSL");
    }
}
