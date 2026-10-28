package dev.shanflyer.glsl.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import dev.shanflyer.glsl.diagnostics.CompilerOptions;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

@State(name = "ShanGlslSettings", storages = @Storage("shan-glsl.xml"))
@com.intellij.openapi.components.Service
public final class GlslSettings implements PersistentStateComponent<CompilerOptions> {
    private CompilerOptions state_ = new CompilerOptions();
    private Set<String> userOne_ = Set.of();
    private Set<String> userTwo_ = Set.of();

    public static GlslSettings GetInstance() {
        return ApplicationManager.getApplication().getService(GlslSettings.class);
    }

    @Override
    public synchronized CompilerOptions getState() {
        return state_.Copy();
    }
    @Override
    public synchronized void loadState(@NotNull CompilerOptions state) {
        state_ = state.Copy();
        userOne_ = Words(state_.userKeywords1);
        userTwo_ = Words(state_.userKeywords2);
    }
    public synchronized CompilerOptions Snapshot() {
        return state_.Copy();
    }

    public synchronized int UserKeywordGroup(String word) {
        return userTwo_.contains(word) ? 2 : userOne_.contains(word) ? 1 : 0;
    }

    public static Set<String> Words(String text) {
        return Arrays.stream(text.split("[\\s,;]+"))
            .filter(word -> !word.isBlank())
            .collect(Collectors.toSet());
    }
}
