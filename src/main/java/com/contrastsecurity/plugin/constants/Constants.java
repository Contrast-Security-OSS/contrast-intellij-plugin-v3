/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.constants;

public class Constants {
  private Constants() {}

  public static final String OS_VERSION = "OS Version";
  public static final String PLUGIN_NAME = "Plugin Name";
  public static final String PLUGIN_RELEASE_VERSION = "Plugin Release Version";
  public static final String IDE_VERSION = "IDE Version";
  public static final String DESCRIPTION = "Description";
  public static final String PLATFORM = "Platform";
  public static final String CONTRAST_PLUGIN = "Contrast Plugin";
  public static final String ABOUT_PLUGIN = "About Plugin";
  public static final String CONTRAST = "Contrast";
  public static final String ABOUT = "About";
  public static final String ASSESS = "Assess";
  public static final String SCAN = "Scan";
  public static final String SOURCE_LABEL = "label.source";
  public static final String CONTRAST_URL_LABEL = "label.contrastUrl";
  public static final String USER_NAME_LABEL = "label.userName";
  public static final String SERVICE_KEY_LABEL = "label.serviceKey";
  public static final String API_KEY_LABEL = "label.apiKey";
  public static final String ORG_ID_LABEL = "label.orgID";
  public static final String APP_NAME_LABEL = "label.appName";
  public static final String PROJECT_NAME_LABEL = "label.projectName";
  public static final String VULNERABILITY_REFRESH_CYCLE_LABEL = "label.vulnerabilityRefreshCycle";
  public static final String MINUTES_LABEL = "label.minutes";
  public static final String ADD_COMMENT = "label.addComment";
  public static final String REASON_LABEL = "label.reason";
  public static final String EXISTING_TAG_LABEL = "label.applyExistingTag";
  public static final String CREATE_TAG_LABEL = "label.createTag";
  public static final String APPLIED_TAG_LABEL = "label.appliedTag";
  public static final String SESSION_METADATA_LABEL = "label.sessionMetadata";
  public static final String SYSTEM_PROPERTY_LABEL = "System property";
  public static final String SERVER_FILTER_LABEL = "filterLabel.server";
  public static final String APPLICATION_FILTER_LABEL = "filterLabel.application";
  public static final String BUILD_NUMBER_FILTER_LABEL = "filterLabel.buildNumber";
  public static final String SEVERITY_FILTER_LABEL = "filterLabel.severity";
  public static final String FILTER_FILTER_LABEL = "filterLabel.filter";
  public static final String STATUS_FILTER_LABEL = "filterLabel.status";
  public static final String FROM_FILTER_LABEL = "filterLabel.from";
  public static final String TO_FILTER_LABEL = "filterLabel.to";
  public static final String REFRESH_BUTTON = "button.bRefresh";
  public static final String RUN_BUTTON = "button.bRun";
  public static final String CLEAR_BUTTON = "button.bClear";
  public static final String RETRIEVE_BUTTON = "button.bRetrieve";
  public static final String ADD_BUTTON = "button.bAdd";
  public static final String UPDATE_BUTTON = "button.bUpdate";
  public static final String CANCEL_BUTTON = "button.bCancel";
  public static final String CREATE_BUTTON = "button.bCreate";
  public static final String NONE_SESSION = "None";
  public static final String MOST_RECENT_SESSION = "Most recent session";
  public static final String CUSTOM_SESSION = "Custom session";

  public static final String ONE_SPACE = " ";
  public static final String DEFAULT_VR_REFRESH_CYCLE_VALUE = "1440";
  public static final String WHAT_HAPPENED = "What happened?";
  public static final String WHAT_IS_THE_RISK = "What's the risk?";
  public static final String FIRST_DETECTED_DATE = "First Detected Date";
  public static final String LAST_DETECTED_DATE = "Last Detected Date";

  public static class CHECKBOXES {
    private CHECKBOXES() {}

    public static final String CRITICAL = "Critical";
    public static final String HIGH = "High";
    public static final String MEDIUM = "Medium";
    public static final String LOW = "Low";
    public static final String NOTE = "Note";
    public static final String REPORTED = "Reported";
    public static final String CONFIRMED = "Confirmed";
    public static final String SUSPICIOUS = "Suspicious";
    public static final String NOT_A_PROBLEM = "Not a Problem";
    public static final String NOT_A_PROBLEM_REQUEST_KEY = "NotAProblem";
    public static final String REMEDIATED = "Remediated";
    public static final String FIXED = "Fixed";
    public static final String REOPENED = "Reopened";
    public static final String REMEDIATED_AUTO_VERIFIED = "Remediated-Auto-Verified";
    public static final String AUTO_REMEDIATED = "AutoRemediated";
    public static final String REMEDIATED_AUTO_VERIFIED_REQUEST_KEY = "REMEDIATED_AUTO_VERIFIED";
    public static final String NOT_A_PROBLEM_SCAN_REQUEST_KEY = "NOT_A_PROBLEM";
  }

  public static class LOGS {
    private LOGS() {}

    public static final String INVALID_INPUTS = "Invalid Inputs";
    public static final String DELETE_CANCELED = "Delete Action Canceled";
    public static final String NO_PERSISTED_DATA_FOUND = "No persisted data found in local";
    public static final String LOCATION_NOT_FOUND = "Location not found";
    public static final String ERROR_FETCHING_VULNERABILITIES = "Error Fetching Vulnerabilities";
    public static final String ERROR_WHILE_LOADING_SERVERS_AND_BUILD_NUMBERS =
        "Error while loading servers and build numbers";
    public static final String LOADING_WAS_CANCELLED_OR_ANOTHER_APP_WAS_SELECTED =
        "Loading was cancelled or another app was selected.";
    public static final String UNABLE_TO_MOVE_TO_THE_LINE_NUMBER =
        "Unable to move to the line number";
    public static final String NO_SERVER_FILTER_APPLIED = "No server filter applied";
    public static final String NO_BUILD_NUMBER_APPLIED = "No build number filter applied";
    public static final String NO_CREDENTIALS_CONFIGURED = "No Credentials configured";
    public static final String FAILED_TO_MARK_VULNERABILITY = "Failed to mark vulnerability status";
    public static final String UNABLE_LOAD_DATA = "Unable to load data";
  }

  public static class ANNOTATION_POPUP {
    private ANNOTATION_POPUP() {}

    public static final String ACTION_FORMAT = "<html><a href=''>Actions</a></html>";
    public static final String ADVICE_HTML_FORMAT = "<html>Advice: ";
    public static final String CLOSE_HTML_FORMAT = "</html>";
    public static final String LAST_DETECTED_FORMAT = "Last detected: ";
    public static final String STATUS_FORMAT = "Status: ";
    public static final String OR_FORMAT = " | ";
    public static final String OPEN_HTML_FORMAT = "<html>";
    public static final String SQUARE_BRACKET_OPEN = "[";
    public static final String SQUARE_BRACKET_CLOSE = "]";
    public static final String EMPTY_SPACES = "         ";
  }

  public static class API_FILTERS {
    private API_FILTERS() {}

    public static final String QUICK_FILTER = "quickFilter";
    public static final String LICENSED = "LICENSED";
  }

  public static class TITLE {
    private TITLE() {}

    public static final String FILTER = "title.filter";
    public static final String DELETE = "title.delete";
    public static final String ORGANIZATION = "table.organization";
    public static final String CONFIGURED_DETAILS = "table.configuredName";
    public static final String TYPE = "table.type";
    public static final String RETRIEVE_VULNERABILITIES_TITLE = "title.retrieveVulnerabilities";
    public static final String CURRENT_FILE_TITLE = "title.currentFile";
    public static final String VULNERABILITY_REPORT_TITLE = "title.vulnerabilityReport";
    public static final String OVERVIEW = "title.overview";
    public static final String HOW_TO_FIX = "title.howToFix";
    public static final String EVENTS = "title.events";
    public static final String HTTP_REQUEST = "title.httpRequest";
    public static final String MARK_AS = "title.markAs";
  }

  public static class MESSAGES {
    private MESSAGES() {}

    public static final String CONNECTED_MESSAGE = "message.connected";
    public static final String INVALID_CONFIGURATION_MESSAGE = "message.invalidConfiguration";
    public static final String EMPTY_INPUTS_MESSAGE = "message.inputsEmpty";
    public static final String REFRESH_CYCLE_CANNOT_BE_EMPTY = "message.refreshCycleNotBeEmpty";
    public static final String REFRESH_CYCLE_NOT_VALID = "message.refreshCycleNotValid";
    public static final String CONFIGURATION_SAVED = "message.configurationSaved";
    public static final String CONFIGURATION_UPDATED = "message.configurationUpdated";
    public static final String UNABLE_TO_SAVE = "message.unableToSaveConfiguration";
    public static final String UNABLE_TO_UPDATE = "message.unableToUpdateConfiguration";
    public static final String DELETE_CONFIRMATION_MESSAGE = "message.deleteConfirmation";
    public static final String ALREADY_EXISTS = "message.configAlreadyExists";
    public static final String CONFIGURATION_DELETED = "message.configurationDeleted";
    public static final String UNABLE_TO_DELETE_CONFIGURATION =
        "message.unableToDeleteConfiguration";
    public static final String LOADING_FILTERS = "message.loadingFilters";
    public static final String RETRIEVING_VULNERABILITIES = "message.retrievingVulnerabilities";
    public static final String NO_VULNERABILITIES_FOUND = "message.noVulnerabilitiesFound";
    public static final String FETCHED_VULNERABILITIES = "message.fetchedVulnerabilities";
    public static final String RETRIEVE_APP_NAME = "message.retrieveAppNameBeforeAdding";
    public static final String RETRIEVE_PROJECT_NAME = "message.retrieveProjectNameBeforeAdding";
    public static final String NO_PROJECT_OPEN = "message.noProjectOpen";
    public static final String NO_CREDENTIAL_CONFIGURE_FOR_PROJECT =
        "message.noCredentialConfigured";
    public static final String NO_APPLICATION_CONFIGURED = "message.noApplicationConfigured";
    public static final String SYNC_PROCESS = "message.syncProcess";
    public static final String MARKED_VULNERABILITY = "message.markedVulnerability";
    public static final String FAILED_TO_MARK = "message.failedToMark";
    public static final String FAILED_TO_REDIRECT = "message.failedToRedirect";
    public static final String UNABLE_TO_LOAD_DATA = "message.unableToLoadData";
    public static final String TAGGED_VULNERABILITY = "message.vulnerabilityTagged";
    public static final String FAILED_TO_TAG = "message.failedToTag";
    public static final String TAG_LENGTH_EXCEEDED = "message.tagLengthExceeded";
    public static final String TAG_ALREADY_APPLIED = "message.tagAlreadyApplied";
    public static final String SELECT_TAG_FROM_DROPDOWN = "message.tagAlreadyAvailable";
    public static final String COMMENT_LIMIT = "message.commentLimit";
  }

  public static class UTILS {
    private UTILS() {}

    public static final String FOUND = "Found";
    public static final String ISSUES = "issues";
    public static final String OPEN_HTML = "<html>";
    public static final String CLOSE_HTML = "</html>";
    public static final String BR = "<br>";
    public static final String APPEND_ACTION = "/Contrast/static/ng/index.html#/";
    public static final String APPLICATION_ACTION = "/applications/";
    public static final String SCAN_ACTION = "/scans/";
    public static final String VULNERABILITIES_ACTION = "/vulnerabilities/";
    public static final String VULN_ACTION = "/vulns/";
    public static final String OPEN_ROUND_BRACKET = " (";
    public static final String CLOSE_ROUND_BRACKET = ")";
    public static final String LINE_NUMBER = "lineNumber";
    public static final String LOCATION = "location";
    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String SERVERS = "servers";
    public static final String APP_VERSION_TAGS = "appVersionTags";
    public static final String SEVERITIES = "severities";
    public static final String SEVERITY = "severity";
    public static final String STATUS = "status";
    public static final String UN_MAPPED_TRACES = "Un mapped vulnerabilities";
    public static final String CRITICAL = "critical";
    public static final String HIGH = "high";
    public static final String MEDIUM = "medium";
    public static final String LOW = "low";
    public static final String NOTE = "note";
    public static final String MESSAGES = "messages";
    public static final String CALENDER = "Calendar";
    public static final String PREVIOUS = "<";
    public static final String NEXT = ">";
    public static final String[] MONTHS = {
      "January", "February", "March", "April", "May", "June",
      "July", "August", "September", "October", "November", "December"
    };
    public static final String[] WEEKS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    public static final String ALL = "All";
    public static final String LAST_HOUR = "Last Hour";
    public static final String LAST_DAY = "Last Day";
    public static final String LAST_WEEK = "Last Week";
    public static final String LAST_MONTH = "Last Month";
    public static final String LAST_YEAR = "Last Year";
    public static final String CUSTOM = "Custom";
    public static final String[] MARK_AS_COMBOBOX = {
      CHECKBOXES.REPORTED,
      CHECKBOXES.SUSPICIOUS,
      CHECKBOXES.CONFIRMED,
      CHECKBOXES.NOT_A_PROBLEM,
      CHECKBOXES.REMEDIATED,
      CHECKBOXES.FIXED
    };
    public static final String FALSE_POSITIVE = "False positive";
    public static final String IS_CONTROL = "Goes through an internal security control";
    public static final String ES_CONTROL = "Attack is defended by an external control";
    public static final String URL_ACCESS_LIMITED = "URL is only accessible by trusted power users";
    public static final String OTHER = "Other";

    public static final String[] REASON_COMBOBOX = {
      ES_CONTROL, FALSE_POSITIVE, IS_CONTROL, OTHER, URL_ACCESS_LIMITED
    };
    public static final String CREATION = "Creation";
    public static final String TRIGGER = "Trigger";
    public static final String TAG = "Tag";
    public static final String EXISTING_TAGS_IN_ORG = "TagsInOrg";
    public static final String TAGS_IN_VUL = "TagsInVul";
    public static final String MARK_AS_STATUS = "MarkAsStatus";
    public static final String MARK_AS_SUB_STATUS = "MarkAsSubStatus";
  }

  public static class CALENDER_UTIL {
    private CALENDER_UTIL() {}

    public static final String[] TIME =
        new String[] {
          "12:00am", "1:00am", "2:00am", "3:00am", "4:00am", "5:00am", "6:00am", "7:00am", "8:00am",
          "9:00am", "10:00am", "11:00am", "12:00pm", "1:00pm", "2:00pm", "3:00pm", "4:00pm",
          "5:00pm", "6:00pm", "7:00pm", "8:00pm", "9:00pm", "10:00pm", "11:00pm"
        };
  }

  public static class TOOL_TIPS {
    private TOOL_TIPS() {}

    public static final String ABOUT = "tooltip.about";
    public static final String DELETE = "tooltip.delete";
    public static final String EDIT = "tooltip.edit";
    public static final String CONFIGURATIONS_SETTINGS = "tooltip.configurationsSettings";
    public static final String ASSESS = "tooltip.assess";
    public static final String SCAN = "tooltip.scan";
    public static final String REFRESH = "tooltip.refresh";
    public static final String CLEARS_SERVERS_AND_BUILD_NUMBERS =
        "tooltip.clearsServersAndBuildNumbers";
    public static final String REFRESH_SERVERS_AND_BUILD_NUMBERS =
        "tooltip.refreshServersAndBuildNumbers";
    public static final String CLEARS_ALL_APPLIED_FILTERS = "tooltip.clearsAllAppliedFilters";
    public static final String FETCH_VULNERABILITIES = "tooltip.fetchVulnerabilities";
  }

  public static class PLACE_HOLDERS {
    private PLACE_HOLDERS() {}

    public static final String ENTER_URL = "placeholder.enterURL";
    public static final String ENTER_USERNAME = "placeholder.enterUsername";
    public static final String ENTER_SERVICE_KEY = "placeholder.enterServiceKey";
    public static final String ENTER_API_KEY = "placeholder.enterApiKey";
    public static final String ENTER_ORG_ID = "placeholder.enterOrganizationId";
    public static final String NO_APPLICATIONS_FOUND = "placeholder.noApplicationsFound";
    public static final String NO_SERVERS_FOUND = "placeholder.noServersFound";
    public static final String NO_BUILD_NUMBERS_FOUND = "placeholder.noBuildNumbersFound";
    public static final String SYSTEM_PROPERTY = "System property";
    public static final String VALUE = "Value";
  }

  public static class API {
    private API() {}

    public static final String AGENT_SESSION_ID = "agentSessionId";
    public static final String METADATA_FILTERS = "metadataFilters";
    public static final String NOT_A_PROBLEM = "NotAProblem";
    public static final String FALSE_POSITIVE = "FP";
    public static final String ES_CONTROL = "EC";
    public static final String IS_CONTROL = "SC";
    public static final String URL_ACCESS_LIMITED = "URL";
    public static final String OTHER = "OT";
  }

  public static class HTML {
    private HTML() {}

    public static final String OPEN_HTML_CONTENT = "<html><body>";
    public static final String CLOSE_HTML_CONTENT = "</body></html>";
    public static final String OPEN_BOLD = "<b>";
    public static final String CLOSE_BOLD = "</b>";
    public static final String BREAK = "<br>";
    public static final String OPEN_PARA = "<p>";
    public static final String CLOSE_PARA = "</p>";
  }
}
