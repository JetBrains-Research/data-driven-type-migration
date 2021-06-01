package org.jetbrains.research.ide.migration;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.Matcher;
import com.intellij.structuralsearch.PatternContextInfo;
import com.intellij.structuralsearch.impl.matcher.MatcherImplUtil;
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import com.intellij.structuralsearch.plugin.replace.ReplacementInfo;
import com.intellij.structuralsearch.plugin.replace.impl.Replacer;
import com.intellij.structuralsearch.plugin.util.CollectingMatchResultSink;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class MyReplacer extends Replacer {
    public MyReplacer(@NotNull Project project, @NotNull ReplaceOptions options) {
        super(project, options);
    }

    public static String testReplace(String in, String what, String by, ReplaceOptions replaceOptions, Project project) {
        final LanguageFileType type = replaceOptions.getMatchOptions().getFileType();
        return testReplace(in, what, by, replaceOptions, project, false, false, type, type.getLanguage());
    }

    /**
     * Copied from {@link Replacer#testReplace(java.lang.String, java.lang.String, java.lang.String, com.intellij.structuralsearch.plugin.replace.ReplaceOptions, com.intellij.openapi.project.Project, boolean, boolean, com.intellij.openapi.fileTypes.LanguageFileType, com.intellij.lang.Language)}.
     * But it doesn't clears `searchCriteria` in the MatchOptions.
     */
    public static String testReplace(String in, String what, String by, ReplaceOptions replaceOptions, Project project, boolean sourceIsFile,
                                     boolean createPhysicalFile, @NotNull LanguageFileType sourceFileType, @NotNull Language sourceDialect) {
        replaceOptions.setReplacement(by);

        final MatchOptions matchOptions = replaceOptions.getMatchOptions();

        Matcher.validate(project, matchOptions);
        checkReplacementPattern(project, replaceOptions);

        final Replacer replacer = new Replacer(project, replaceOptions);
        final Matcher matcher = new Matcher(project, matchOptions);
        try {
            final PsiElement firstElement;
            final PsiElement lastElement;
            final PsiElement parent;
            if (matchOptions.getScope() == null) {
                final PsiElement[] elements = MatcherImplUtil.createTreeFromText(
                        in,
                        new PatternContextInfo(sourceIsFile ? PatternTreeContext.File : PatternTreeContext.Block),
                        sourceFileType,
                        sourceDialect,
                        project,
                        createPhysicalFile
                );

                firstElement = elements[0];
                lastElement = elements[elements.length - 1];
                parent = firstElement.getParent();

                matchOptions.setScope(new LocalSearchScope(elements));
            } else {
                parent = ((LocalSearchScope) matchOptions.getScope()).getScope()[0];
                firstElement = parent.getFirstChild();
                lastElement = parent.getLastChild();
            }

            final CollectingMatchResultSink sink = new CollectingMatchResultSink();
            matcher.testFindMatches(sink);

            final List<ReplacementInfo> replacements = new SmartList<>();
            for (final MatchResult result : sink.getMatches()) {
                replacements.add(replacer.buildReplacement(result));
            }

            int startOffset = firstElement.getTextRange().getStartOffset();
            int endOffset = sourceIsFile ? 0 : (parent.getTextLength() - lastElement.getTextRange().getEndOffset());

            // get nodes from text may contain
            final PsiElement prevSibling = firstElement.getPrevSibling();
            if (prevSibling instanceof PsiWhiteSpace) {
                startOffset -= prevSibling.getTextLength();
            }

            final PsiElement nextSibling = lastElement.getNextSibling();
            if (nextSibling instanceof PsiWhiteSpace) {
                endOffset -= nextSibling.getTextLength();
            }
            replacer.replaceAll(replacements);
            if (firstElement == lastElement && firstElement instanceof PsiFile) {
                return firstElement.getText();
            }
            final String result = parent.getText();
            return result.substring(startOffset, result.length() - endOffset);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IncorrectOperationException(e);
        } finally {
            matchOptions.setScope(null);
        }
    }
}
