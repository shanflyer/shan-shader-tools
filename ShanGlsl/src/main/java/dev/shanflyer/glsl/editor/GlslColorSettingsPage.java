package dev.shanflyer.glsl.editor;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import dev.shanflyer.glsl.language.GlslFileType;
import java.util.Map;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

public final class GlslColorSettingsPage implements ColorSettingsPage {
    @Override
    public Icon getIcon() {
        return GlslFileType.instance.getIcon();
    }
    @Override
    public @NotNull String getDisplayName() {
        return "Shan GLSL";
    }
    @Override
    public @NotNull SyntaxHighlighter getHighlighter() {
        return new GlslHighlighter();
    }
    @Override
    public @NotNull String getDemoText() {
        return """
                #version 460
                // Colors match the reference extension's three GLSL categories.
                layout(location = 0) in vec3 position;
                layout(set = 0, binding = 0) uniform sampler2D albedo;
                void main() {
                    vec3 normal = normalize(position);
                    gl_Position = vec4(normal, 1.0);
                    <user1>MY_DEFINE</user1>;
                    <user2>MY_MACRO</user2>;
                }
                #include "lighting.glsl"
                """;
    }
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return Map.of("user1", GlslHighlighter.userOne, "user2", GlslHighlighter.userTwo);
    }
    @Override
    public AttributesDescriptor @NotNull[] getAttributeDescriptors() {
        return new AttributesDescriptor[] {
            new AttributesDescriptor("GLSL Keyword", GlslHighlighter.keyword),
            new AttributesDescriptor("GLSL Function", GlslHighlighter.function),
            new AttributesDescriptor("GLSL Variable", GlslHighlighter.variable),
            new AttributesDescriptor("GLSL UserKeyword1", GlslHighlighter.userOne),
            new AttributesDescriptor("GLSL UserKeyword2", GlslHighlighter.userTwo),
            new AttributesDescriptor("Comment", GlslHighlighter.comment),
            new AttributesDescriptor("Number", GlslHighlighter.number),
            new AttributesDescriptor("String", GlslHighlighter.string),
            new AttributesDescriptor("Preprocessor", GlslHighlighter.preprocessor),
            new AttributesDescriptor("Operator", GlslHighlighter.operator),
            new AttributesDescriptor("Braces", GlslHighlighter.braces),
            new AttributesDescriptor("Identifier", GlslHighlighter.identifier)};
    }
    @Override
    public ColorDescriptor @NotNull[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }
}
