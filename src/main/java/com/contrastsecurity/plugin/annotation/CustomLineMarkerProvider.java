/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.annotation;

import com.contrastsecurity.plugin.toolwindow.ContrastToolWindow;
import com.contrastsecurity.plugin.utility.FileVulnerabilitiesUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
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
    loadCurrentFileVulnerabilities(element.getProject());
    // Get current editor and project
    Project project = element.getProject();
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) return null;

    // Get line number
    int lineNumber = editor.getDocument().getLineNumber(element.getTextOffset());

    if (CollectionUtils.isNotEmpty(lineNumbers) && lineNumbers.contains(lineNumber)) {
      int startOffset = element.getTextOffset();
      int endOffset = startOffset + element.getTextLength();

      // Validate offsets
      Document document = editor.getDocument();
      int fileLength = document.getTextLength();

      if (startOffset < 0 || endOffset > fileLength) {
        return null; // Skip invalid ranges
      }

      // Create a LineMarkerInfo
      LineMarkerInfo<PsiElement> lineMarkerInfo =
          new LineMarkerInfo<>(element, new TextRange(startOffset, endOffset));

      // Add squiggly line highlighting
      addSquigglyLine(editor, element);

      return lineMarkerInfo;
    }
    return null;
  }

  private void addSquigglyLine(Editor editor, PsiElement element) {
    Document document = editor.getDocument();
    MarkupModel markupModel = editor.getMarkupModel();

    // Get start and end offsets of the specific text element
    int startOffset = element.getTextOffset();
    int endOffset = startOffset + element.getTextLength();

    // Create TextAttributes for the squiggly line
    TextAttributes textAttributes = new TextAttributes();
    textAttributes.setEffectColor(new Color(255, 204, 0)); // Effect color for squiggly line
    textAttributes.setEffectType(EffectType.WAVE_UNDERSCORE); // Set to WAVE_UNDERSCORE for squiggly

    // Clear any existing highlights in the same range to avoid conflicts
    RangeHighlighter[] existingHighlighters = markupModel.getAllHighlighters();
    for (RangeHighlighter highlighter : existingHighlighters) {
      if (highlighter.getStartOffset() < endOffset && highlighter.getEndOffset() > startOffset) {
        markupModel.removeHighlighter(highlighter);
      }
    }

    // Adjust the endOffset to avoid spilling over to the next line
    int lineNumber = document.getLineNumber(startOffset);
    int lineEndOffset = document.getLineEndOffset(lineNumber);

    if (endOffset > lineEndOffset) {
      endOffset = lineEndOffset; // Ensure the end offset doesn't spill over
    }

    // Add the squiggly line highlighter based on the adjusted text offsets
    markupModel.addRangeHighlighter(
        startOffset,
        endOffset,
        HighlighterLayer.WEAK_WARNING,
        textAttributes,
        HighlighterTargetArea.EXACT_RANGE);
  }

  private void loadCurrentFileVulnerabilities(@NotNull Project project) {
    ToolWindowManager instance = ToolWindowManager.getInstance(project);
    ToolWindow contrastWindow = instance.getToolWindow("Contrast");
    if (contrastWindow != null) {
      Content content = contrastWindow.getContentManager().getContent(0); // Assuming the first tab
      if (content != null) {
        JComponent component = content.getComponent();
        if (component instanceof ContrastToolWindow) {
          ContrastToolWindow contrastToolWindow = (ContrastToolWindow) component;
          // Now you have the exact object
          VirtualFile file = getCurrentVirtualFile(project);
          if (file != null) {
            String fileName = file.getPath();
            if (StringUtils.isNotEmpty(fileName)) {
              FileVulnerabilitiesUtil fileVulnerabilitiesUtil;
              if (contrastToolWindow.getAssessComponent() != null) {
                fileVulnerabilitiesUtil =
                    new FileVulnerabilitiesUtil(fileName, contrastToolWindow.getAssessComponent());
                if (CollectionUtils.isNotEmpty(fileVulnerabilitiesUtil.getLineNumbers())) {
                  lineNumbers = fileVulnerabilitiesUtil.getLineNumbers();
                }
              }
              if (contrastToolWindow.getScanComponent() != null) {
                fileVulnerabilitiesUtil =
                    new FileVulnerabilitiesUtil(fileName, contrastToolWindow.getScanComponent());
                if (CollectionUtils.isNotEmpty(fileVulnerabilitiesUtil.getLineNumbers())) {
                  lineNumbers = fileVulnerabilitiesUtil.getLineNumbers();
                }
              }
            }
          }
        }
      }
    }
  }

  private VirtualFile getCurrentVirtualFile(Project project) {
    VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
    if (selectedFiles.length > 0) {
      return selectedFiles[0]; // Return the first selected/open file.
    }
    return null; // No file is selected or open.
  }

  /** Refreshes the annotated line number after fetching new set of data */
  public void refresh(Project project) {
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

    VirtualFile[] openFiles = fileEditorManager.getOpenFiles();

    if (openFiles.length > 0) {
      VirtualFile selectedFile =
          fileEditorManager.getSelectedFiles().length > 0
              ? fileEditorManager.getSelectedFiles()[0]
              : null;

      // Close all open files
      for (VirtualFile file : openFiles) {
        fileEditorManager.closeFile(file);
      }

      // Reopen all closed files
      for (VirtualFile file : openFiles) {
        fileEditorManager.openFile(file, true);
      }

      // Set the previously selected file as the active tab again, if it was open
      if (selectedFile != null && fileEditorManager.isFileOpen(selectedFile)) {
        fileEditorManager.setSelectedEditor(
            selectedFile, String.valueOf(fileEditorManager.getEditors(selectedFile)[0]));
      }
    }
  }
}
