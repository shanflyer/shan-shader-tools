package dev.shanflyer.hlsl.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "ShanHlslSettings", storages = @Storage("shan-hlsl.xml"))
@Service
public final class HlslSettings implements PersistentStateComponent<HlslSettings.Options> {
    public static final class Options {
        public boolean enabled = true;
        public boolean semanticColors = true;
        public String serverPath = "";
        public String arguments = "";

        public Options Copy() {
            final Options result = new Options();
            result.enabled = enabled;
            result.semanticColors = semanticColors;
            result.serverPath = serverPath;
            result.arguments = arguments;
            return result;
        }
    }
    private Options state_ = new Options();
    public static HlslSettings GetInstance() {
        return ApplicationManager.getApplication().getService(HlslSettings.class);
    }
    public synchronized Options Snapshot() {
        return state_.Copy();
    }
    @Override
    public synchronized Options getState() {
        return state_.Copy();
    }
    @Override
    public synchronized void loadState(@NotNull Options state) {
        state_ = state.Copy();
    }
}
