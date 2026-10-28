package dev.shanflyer.glsl.editor;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import dev.shanflyer.glsl.language.GlslTokens;
import dev.shanflyer.glsl.settings.GlslSettings;
import org.jetbrains.annotations.NotNull;

public final class GlslUserKeywordAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element.getNode() == null) {
            return;
        }
        final var type = element.getNode().getElementType();
        if (type != GlslTokens.identifier && type != GlslTokens.keyword &&
            type != GlslTokens.function && type != GlslTokens.variable) {
            return;
        }
        final int group = GlslSettings.GetInstance().UserKeywordGroup(element.getText());
        final var color = group == 2   ? GlslHighlighter.userTwo
                          : group == 1 ? GlslHighlighter.userOne
                                       : null;
        if (color != null) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(color)
                .create();
        }
    }
}
