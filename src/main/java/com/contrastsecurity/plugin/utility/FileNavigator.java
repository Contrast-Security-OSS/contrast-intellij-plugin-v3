package com.contrastsecurity.plugin.utility;

import com.contrastsecurity.plugin.constants.Constants;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import groovy.util.logging.Slf4j;

@lombok.extern.slf4j.Slf4j
@Slf4j
public class FileNavigator {
  private Editor editor;

  public FileNavigator(Project project) {
    editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
  }

  public void moveToLineNumber(int lineNumber) {
    CaretModel caretModel = editor.getCaretModel();
    if (lineNumber > 0 && lineNumber <= editor.getDocument().getLineCount()) {
      int lineStartOffset =
          editor.getDocument().getLineStartOffset(lineNumber - 1); // Convert 1-based to 0-based
      caretModel.moveToOffset(lineStartOffset);
      editor.getScrollingModel().scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER);
    } else {
      log.error(Constants.LOGS.UNABLE_TO_MOVE_TO_THE_LINE_NUMBER);
    }
  }
}
