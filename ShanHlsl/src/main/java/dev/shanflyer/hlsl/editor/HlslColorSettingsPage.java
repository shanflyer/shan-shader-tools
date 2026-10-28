package dev.shanflyer.hlsl.editor;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import dev.shanflyer.hlsl.language.HlslFileType;
import java.util.Map;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

public final class HlslColorSettingsPage implements ColorSettingsPage {
    @Override
    public Icon getIcon() {
        return HlslFileType.instance.getIcon();
    }
    @Override
    public @NotNull String getDisplayName() {
        return "Shan HLSL";
    }
    @Override
    public @NotNull SyntaxHighlighter getHighlighter() {
        return new HlslHighlighter();
    }
    @Override
    public @NotNull String getDemoText() {
        return """
            #define <macro>USE_TEXTURE</macro> 1
            // HlslTools syntax and semantic categories.
            struct <type>PixelInput</type> {
                float2 <field>uv</field> : TEXCOORD0;
            };
            Texture2D albedo : register(t0);
            SamplerState samplerState : register(s0);
            float4 <function>PSMain</function>(PixelInput <parameter>input</parameter>) : SV_Target {
                float4 <variable>color</variable> = albedo.Sample(samplerState, input.uv);
                return saturate(color);
            }
            #if 0
            <excluded>float hidden = unavailable;</excluded>
            #endif
            """;
    }
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return Map.of("macro", HlslHighlighter.macro, "type", HlslHighlighter.type, "field",
                      HlslHighlighter.field, "function", HlslHighlighter.function, "parameter",
                      HlslHighlighter.parameter, "variable", HlslHighlighter.variable, "excluded",
                      HlslHighlighter.excluded);
    }
    @Override
    public AttributesDescriptor @NotNull[] getAttributeDescriptors() {
        return new AttributesDescriptor[] {
            new AttributesDescriptor("Keyword", HlslHighlighter.keyword),
            new AttributesDescriptor("Function / method", HlslHighlighter.function),
            new AttributesDescriptor("Type / struct / constant buffer", HlslHighlighter.type),
            new AttributesDescriptor("Field", HlslHighlighter.field),
            new AttributesDescriptor("Parameter", HlslHighlighter.parameter),
            new AttributesDescriptor("Variable", HlslHighlighter.variable),
            new AttributesDescriptor("Semantic / register", HlslHighlighter.semantic),
            new AttributesDescriptor("Macro", HlslHighlighter.macro),
            new AttributesDescriptor("Excluded preprocessor code", HlslHighlighter.excluded),
            new AttributesDescriptor("Comment", HlslHighlighter.comment),
            new AttributesDescriptor("Number", HlslHighlighter.number),
            new AttributesDescriptor("String", HlslHighlighter.string),
            new AttributesDescriptor("Preprocessor", HlslHighlighter.preprocessor),
            new AttributesDescriptor("Operator", HlslHighlighter.operator),
            new AttributesDescriptor("Braces", HlslHighlighter.braces),
            new AttributesDescriptor("Identifier", HlslHighlighter.identifier)};
    }
    @Override
    public ColorDescriptor @NotNull[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }
}
