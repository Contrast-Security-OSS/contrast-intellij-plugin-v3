/*******************************************************************************
 * Copyright © 2026 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.components;

import com.contrastsecurity.assess.v3.dto.TraceFile;
import com.contrastsecurity.plugin.annotation.MouseHoverHandler;
import com.contrastsecurity.plugin.models.AnnotationPopupDTO;
import com.contrastsecurity.plugin.models.ScanVulnerabilityDTO;
import com.contrastsecurity.plugin.service.CacheDataService;
import com.contrastsecurity.plugin.service.ScanBackgroundLoader;
import com.contrastsecurity.plugin.toolwindow.ContrastToolWindow;
import com.contrastsecurity.plugin.utility.FileVulnerabilitiesUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.util.messages.MessageBusConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class MyFileEditorListener implements FileEditorManagerListener {

  private final Project project;
  private ScanComponent scanComponent;
  private AssessComponent assessComponent;
  private final CacheDataService cacheDataService = new CacheDataService();
  private VirtualFile lastLoadedFile;

  @Getter @Setter private TraceFile vulnerabilities;
  private final Map<VirtualFile, Map<Integer, AnnotationPopupDTO>> hoverPopupMap = new HashMap<>();
  private MouseHoverHandler mouseHoverHandler;

  public MyFileEditorListener(Project project) {
    this.project = project;
    initHoverHandler();
    registerListener();
  }

  public MyFileEditorListener(Project project, ScanComponent scanComponent) {
    this(project);
    this.scanComponent = scanComponent;
  }

  public MyFileEditorListener(Project project, AssessComponent assessComponent) {
    this(project);
    this.assessComponent = assessComponent;
  }

  private void initHoverHandler() {
    mouseHoverHandler = new MouseHoverHandler(hoverPopupMap, project);
    mouseHoverHandler.addMouseHoverListener();
  }

  private void registerListener() {
    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    VirtualFile selectedFile = event.getNewFile();
    if (selectedFile != null) {
      if (!selectedFile.equals(lastLoadedFile)) {
        lastLoadedFile = selectedFile;
        loadReportForSelectedFile(selectedFile);
      } else {
        log.debug("Same file re-selected, skipping reload: {}", selectedFile.getName());
      }
    } else {
      lastLoadedFile = null;
      resetToolWindowPanels();
    }
  }

  private void resetToolWindowPanels() {
    ToolWindow contrastWindow = ToolWindowManager.getInstance(project).getToolWindow("Contrast");
    if (contrastWindow == null) return;
    Content content = contrastWindow.getContentManager().getContent(0);
    if (content == null) return;
    JComponent component = content.getComponent();
    if (component instanceof ContrastToolWindow contrastToolWindow) {
      if (contrastToolWindow.getScanComponent() != null) {
        contrastToolWindow.getScanComponent().loadDefaultCurrentFile();
      }
      if (contrastToolWindow.getAssessComponent() != null) {
        contrastToolWindow.getAssessComponent().loadDefaultCurrentFilePanel();
      }
    }
  }

  private void loadReportForSelectedFile(VirtualFile file) {
    if (Objects.nonNull(assessComponent)) {
      String appId = assessComponent.getAppId();
      if (StringUtils.isNotEmpty(appId)) {
        loadAssessCurrentFileReport(file, appId);
      } else {
        assessComponent.loadDefaultCurrentFilePanel();
      }
    }
    if (Objects.nonNull(scanComponent)) {
      String projectId = scanComponent.getProjectId();
      if (StringUtils.isNotEmpty(projectId)) {
        loadScanCurrentFileReport(file, projectId);
      } else {
        scanComponent.loadDefaultCurrentFile();
      }
    }
  }

  private void loadAssessCurrentFileReport(VirtualFile file, String appId) {
    TraceFile traceFile = (TraceFile) cacheDataService.get(appId);
    if (traceFile != null && Objects.nonNull(assessComponent)) {
      assessComponent.loadCurrentFileReport(file.getName(), file.getPath().replace("/", "."), traceFile.getFileVulnerabilitiesData());
      FileVulnerabilitiesUtil util = new FileVulnerabilitiesUtil(file.getPath(), assessComponent);
      hoverPopupMap.put(file, util.getPopupDTOMap());
      ScanBackgroundLoader scanBackgroundLoader = new ScanBackgroundLoader(appId);
      scanBackgroundLoader.startBackgroundLoading(hoverPopupMap);

    } else if (Objects.nonNull(assessComponent)) {
      assessComponent.loadDefaultCurrentFilePanel();
    }
  }

  private void loadScanCurrentFileReport(VirtualFile file, String projectId) {
    ScanVulnerabilityDTO cacheData = (ScanVulnerabilityDTO) cacheDataService.get(projectId);
    if (cacheData != null && Objects.nonNull(scanComponent)) {
      scanComponent.loadCurrentFileReport(cacheData, file.getPath());
      FileVulnerabilitiesUtil util = new FileVulnerabilitiesUtil(file.getPath(), scanComponent);
      hoverPopupMap.put(file, util.getPopupDTOMap());
      ScanBackgroundLoader scanBackgroundLoader = new ScanBackgroundLoader(projectId);
      scanBackgroundLoader.startBackgroundLoading(hoverPopupMap);
    } else if (Objects.nonNull(scanComponent)) {
      scanComponent.loadDefaultCurrentFile();
    }
  }

  public void reopenActiveFile() {
    VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
    if (selectedFiles.length > 0) {
      loadReportForSelectedFile(selectedFiles[0]);
    }
  }
}
