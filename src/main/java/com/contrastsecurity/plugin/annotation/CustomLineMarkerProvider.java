/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.annotation;

import com.contrastsecurity.plugin.toolwindow.ContrastToolWindow;
import com.contrastsecurity.plugin.utility.FileVulnerabilitiesUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.content.Content;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomLineMarkerProvider implements LineMarkerProvider {

  private List<Integer> lineNumbers = new ArrayList<>();

  @Override
  public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    Project project = element.getProject();

    // Defensive: skip if element is too large or document is inconsistent
    VirtualFile file = PsiUtilCore.getVirtualFile(element);
    if (file == null) return null;

    loadCurrentFileVulnerabilities(project, file);

    FileEditor[] editors = FileEditorManager.getInstance(project).getEditors(file);
    if (editors.length == 0) return null;

    for (FileEditor fileEditor : editors) {
      if (!(fileEditor instanceof TextEditor)) continue;
      Editor editor = ((TextEditor) fileEditor).getEditor();
      Document document = editor.getDocument();

      int offset = element.getTextOffset();
      if (offset < 0 || offset >= document.getTextLength()) continue;

      int lineNumber = document.getLineNumber(offset);
      if (CollectionUtils.isNotEmpty(lineNumbers) && lineNumbers.contains(lineNumber)) {
        addSquigglyLine(editor, element);
        return new LineMarkerInfo<>(element, element.getTextRange());
      }
    }
    return null;
  }

  private void addSquigglyLine(Editor editor, PsiElement element) {
    Document document = editor.getDocument();
    MarkupModel markupModel = editor.getMarkupModel();

    int startOffset = element.getTextRange().getStartOffset();
    int endOffset = element.getTextRange().getEndOffset();

    if (startOffset < 0 || endOffset > document.getTextLength()) return;

    CharSequence fullText = document.getCharsSequence().subSequence(startOffset, endOffset);

    // Trim leading and trailing whitespace
    int relativeStart = -1, relativeEnd = -1;
    for (int i = 0; i < fullText.length(); i++) {
      if (!Character.isWhitespace(fullText.charAt(i))) {
        relativeStart = i;
        break;
      }
    }
    for (int i = fullText.length() - 1; i >= 0; i--) {
      if (!Character.isWhitespace(fullText.charAt(i))) {
        relativeEnd = i + 1;
        break;
      }
    }

    if (relativeStart == -1 || relativeEnd == -1 || relativeStart >= relativeEnd) return;

    int actualStart = startOffset + relativeStart;
    int actualEnd = startOffset + relativeEnd;

    TextAttributes attributes = new TextAttributes();
    attributes.setEffectColor(new Color(255, 204, 0)); // Yellow squiggly
    attributes.setEffectType(EffectType.WAVE_UNDERSCORE);

    markupModel.addRangeHighlighter(
        actualStart,
        actualEnd,
        HighlighterLayer.WEAK_WARNING,
        attributes,
        HighlighterTargetArea.EXACT_RANGE);
  }

  private void loadCurrentFileVulnerabilities(@NotNull Project project, @NotNull VirtualFile file) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow contrastWindow = toolWindowManager.getToolWindow("Contrast");
    if (contrastWindow == null) return;

    Content content = contrastWindow.getContentManager().getContent(0);
    if (content == null) return;

    JComponent component = content.getComponent();
    if (!(component instanceof ContrastToolWindow)) return;

    ContrastToolWindow contrastToolWindow = (ContrastToolWindow) component;
    String fileName = file.getPath();
    if (StringUtils.isEmpty(fileName)) return;

    List<Integer> newLines = new ArrayList<>();

    if (contrastToolWindow.getAssessComponent() != null) {
      FileVulnerabilitiesUtil assessUtil =
          new FileVulnerabilitiesUtil(fileName, contrastToolWindow.getAssessComponent());
      if (CollectionUtils.isNotEmpty(assessUtil.getLineNumbers())) {
        newLines.addAll(assessUtil.getLineNumbers());
      }
    }

    if (contrastToolWindow.getScanComponent() != null) {
      FileVulnerabilitiesUtil scanUtil =
          new FileVulnerabilitiesUtil(fileName, contrastToolWindow.getScanComponent());
      if (CollectionUtils.isNotEmpty(scanUtil.getLineNumbers())) {
        newLines.addAll(scanUtil.getLineNumbers());
      }
    }

    lineNumbers = newLines;
  }

  /** Refreshes the annotated line number after fetching new set of data */
  public void refresh(Project project) {
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              ApplicationManager.getApplication()
                  .runReadAction(
                      () -> {
                        FileEditorManager fileEditorManager =
                            FileEditorManager.getInstance(project);
                        VirtualFile[] openFiles = fileEditorManager.getOpenFiles();

                        if (openFiles.length == 0) return;

                        VirtualFile selectedFile =
                            fileEditorManager.getSelectedFiles().length > 0
                                ? fileEditorManager.getSelectedFiles()[0]
                                : null;

                        for (VirtualFile file : openFiles) {
                          fileEditorManager.closeFile(file);
                        }

                        for (VirtualFile file : openFiles) {
                          fileEditorManager.openFile(file, false);
                        }

                        if (selectedFile != null && fileEditorManager.isFileOpen(selectedFile)) {
                          fileEditorManager.openFile(selectedFile, true); // re-focus selected file
                        }
                      });
            });
  }
}
