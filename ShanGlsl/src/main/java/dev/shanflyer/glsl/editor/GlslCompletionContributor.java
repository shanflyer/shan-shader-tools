package dev.shanflyer.glsl.editor;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import dev.shanflyer.glsl.language.GlslLexer;
import dev.shanflyer.glsl.language.GlslTokens;
import dev.shanflyer.glsl.language.GlslVocabulary;
import dev.shanflyer.glsl.settings.GlslSettings;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

public final class GlslCompletionContributor extends CompletionContributor implements DumbAware {
    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters,
                                       @NotNull CompletionResultSet result) {
        final String text = parameters.getOriginalFile().getText();
        final int offset = Math.min(parameters.getOffset(), text.length());
        final GlslLexer lexer = new GlslLexer();
        lexer.start(text);
        final Set<String> identifiers = new LinkedHashSet<>();
        while (lexer.getTokenType() != null) {
            ProgressManager.checkCanceled();
            final var type = lexer.getTokenType();
            if (lexer.getTokenStart() <= offset && offset <= lexer.getTokenEnd() &&
                (type == GlslTokens.lineComment || type == GlslTokens.blockComment ||
                 type == GlslTokens.string)) {
                return;
            }
            if (type == GlslTokens.identifier &&
                !(lexer.getTokenStart() <= offset && offset <= lexer.getTokenEnd())) {
                identifiers.add(lexer.getTokenText());
            }
            lexer.advance();
        }
        int wordStart = offset;
        while (wordStart > 0 && GlslLexer.IsIdentifierPart(text.charAt(wordStart - 1))) {
            --wordStart;
        }
        int directiveStart = wordStart;
        while (directiveStart > 0 && (text.charAt(directiveStart - 1) == ' ' ||
                                      text.charAt(directiveStart - 1) == '\t')) {
            --directiveStart;
        }
        if (directiveStart > 0 && text.charAt(directiveStart - 1) == '#') {
            Add(result, GlslVocabulary.directives, "preprocessor", AllIcons.Nodes.Annotationtype);
            return;
        }
        Add(result, GlslVocabulary.functions, "GLSL function", AllIcons.Nodes.Function);
        Add(result,
            GlslVocabulary.keywords.stream()
                .filter(word -> !GlslVocabulary.functions.contains(word))
                .toList(),
            "GLSL keyword / type", AllIcons.Nodes.Class);
        Add(result, GlslVocabulary.variables, "GLSL built-in", AllIcons.Nodes.Variable);
        final var settings = GlslSettings.GetInstance().Snapshot();
        identifiers.addAll(GlslSettings.Words(settings.userKeywords1));
        identifiers.addAll(GlslSettings.Words(settings.userKeywords2));
        identifiers.removeAll(GlslVocabulary.keywords);
        identifiers.removeAll(GlslVocabulary.functions);
        identifiers.removeAll(GlslVocabulary.variables);
        Add(result, identifiers, "shader identifier", AllIcons.Nodes.Variable);
    }

    private static void Add(CompletionResultSet result, Iterable<String> words, String description,
                            Icon icon) {
        for (final String word : words) {
            ProgressManager.checkCanceled();
            result.addElement(
                LookupElementBuilder.create(word).withIcon(icon).withTypeText(description, true));
        }
    }
}
