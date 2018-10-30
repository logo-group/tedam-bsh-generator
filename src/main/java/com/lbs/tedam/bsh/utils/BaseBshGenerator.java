/*
* Copyright 2014-2019 Logo Business Solutions
* (a.k.a. LOGO YAZILIM SAN. VE TIC. A.S)
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/

package com.lbs.tedam.bsh.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lbs.tedam.model.TestReport;
import com.lbs.tedam.util.Constants;
import com.lbs.tedam.util.Enums;
import com.lbs.tedam.util.Enums.FileName;
import com.lbs.tedam.util.Enums.FilePath;
import com.lbs.tedam.util.Enums.FormOpenTypes;
import com.lbs.tedam.util.Enums.OperationTypes;
import com.lbs.tedam.util.Enums.Regex;
import com.lbs.tedam.util.Enums.ScriptParameters;
import com.lbs.tedam.util.Enums.StatusMessages;
import com.lbs.tedam.util.PropUtils;
import com.lbs.tedam.util.TedamFileUtils;
import com.lbs.tedam.util.TedamProcessUtils;
import com.lbs.tedam.util.TedamStringUtils;
import com.lbs.tedam.webservice.rest.client.RestClient;

/**
 * 
 * @author Tarik.Mikyas
 * 
 */
public final class BaseBshGenerator {
	private static final Logger LOGGER = LoggerFactory.getLogger(BaseBshGenerator.class);
	private static String version = "";
	private static int testStepId = 0;

	private BaseBshGenerator() {
	}

	public static void main(String[] args) {
		LOGGER.info("BaseBshGeneratorStarted");
		// args = ("resultReportAddress!equals!TEDAM_TX869_TC7028 version!equals!2.55.7.12 testCaseId!equals!7028 timeRecording!equals!true
		// FormOpen!equals![26000,26200,26201]!ts!28752 GridSearch!equals!100!ps![(1003,TC006989)]!ts!28753!fn!LOXFOrderSlipBrowser%2
		// PopUp!equals!Durumunu!spc!Değiştir!ts!28754!fn!LOXFOrderSlipBrowser%2 FormFill!equals!725!ts!28755!fn!LOXFApprove%2 ButtonClick!equals!304!ps!0!ts!28756!fn!LOXFApprove%2
		// ButtonClick!equals!153!ps!0!ts!28757!fn!LOXFOrderSlipBrowser%2 Report!equals!AyrıntılıMaliyetAnalizi!ps!1!ps!1!ps!100!ts!164580")
		// .split(" ");

		List<EnumMap<ScriptParameters, Object>> paramMapList = BaseBshGeneratorUtil.buildParamMapList(args);
		loadConfigFilePathParam(paramMapList);
		String projectFilePath = PropUtils.getProperty(Constants.TEDAM_EXECUTION_FILE_PATH);
		LOGGER.info("projectFilePath : " + projectFilePath);
		String tempFilePath = PropUtils.getProperty(Constants.TEMP_FILE_PATH);
		LOGGER.info("tempFilePath : " + tempFilePath);
		String baseBshAbsolutePath = projectFilePath + Constants.FILE_SEPARATOR + FilePath.BSH_MODULAR_SCRIPTS.getPath() + Constants.FILE_SEPARATOR + FileName.BASE.getName()
				+ Constants.TEXT_UNDERSCORE + TedamProcessUtils.getClientHostName() + Constants.FILE_EXTENSION_BSH;
		LOGGER.info("baseBshAbsolutePath : " + baseBshAbsolutePath);
		String testSetId = TedamStringUtils.findTestSetTestCaseParameters(args).get(Constants.TEST_SET_ID);
		String testCaseId = TedamStringUtils.findTestSetTestCaseParameters(args).get(Constants.TEST_CASE_ID);
		String testPackageName = "TestSet-" + testSetId + "_TestCase-" + testCaseId;
		String pTestCases = testCaseId;
		String testPackageFileName = testPackageName + Constants.FILE_EXTENSION_XML;
		String testPackageContent;
		try {
			String scriptCommandReportPath = Constants.REPORT_HEADER_SCRIPT + testSetId + Constants.REPORT_TEST_CASE + testCaseId + Constants.FILE_EXTENSION_XLS;
			// TODO:Here is the name of the report draftCommand in the type script command should be produced with the command id.For now, control will be provided via run order.
			// The reportPath part must be a separate method and must be based on command 'in type'.
			generateBaseScript(paramMapList, baseBshAbsolutePath, projectFilePath, scriptCommandReportPath);
			testPackageContent = TedamStringUtils.createBSHPackageContentForTedam(tempFilePath, testPackageName, pTestCases, baseBshAbsolutePath);
			TedamFileUtils.createFile(tempFilePath + Constants.FILE_SEPARATOR + testPackageFileName, testPackageContent);
			LOGGER.info("bshGenerated");
		} catch (FileNotFoundException e) {
			LOGGER.error("" + e);
		} catch (IOException e) {
			LOGGER.error("" + e);
		}
	}

	/**
	 * this method loadConfigFilePathParam <br>
	 * @author Canberk.Erkmen
	 * @param paramMapList
	 *            <br>
	 */
	private static void loadConfigFilePathParam(List<EnumMap<ScriptParameters, Object>> paramMapList) {
		String configFilePath = getConfigFilePath(paramMapList);
		if (!configFilePath.isEmpty()) {
			LOGGER.info(configFilePath + Constants.FILE_SEPARATOR + FileName.CONFIG_PROPERTIES.getName());
			PropUtils.loadPropFile(configFilePath + Constants.FILE_SEPARATOR + FileName.CONFIG_PROPERTIES.getName());
		}
	}

	private static String findTimeRecordingParam(List<EnumMap<ScriptParameters, Object>> paramMapList) {
		String timeRecording = "false";
		for (EnumMap<ScriptParameters, Object> param : paramMapList) {
			if (param.containsKey(ScriptParameters.TIME_RECORDING)) {
				timeRecording = (String) param.get(ScriptParameters.TIME_RECORDING);
				break;
			}
		}
		return timeRecording;
	}

	/**
	 * this method generateBaseScript <br>
	 * @author Tarik.Mikyas
	 * @param args
	 * @param baseBshAbsolutePath
	 *            <br>
	 */
	public static void generateBaseScript(List<EnumMap<ScriptParameters, Object>> paramMapList, String baseBshAbsolutePath, String projectFilePath, String bshReportName) {
		String timeRecording = findTimeRecordingParam(paramMapList);
		EnumMap<ScriptParameters, Object> externalScriptMap = getExternalScriptMap(paramMapList);
		String configFilePath = getConfigFilePath(paramMapList);
		List<TestReport> testReportList = new ArrayList<>();
		StringBuilder scriptContent = new StringBuilder();
		TestReport testReport;
		try {
			if (externalScriptMap == null) {
				scriptContent.append(initiateScript(projectFilePath, configFilePath, timeRecording));
				scriptContent.append(allocateBodyCommand(paramMapList));
				scriptContent.append(endScript());
			} else {
				scriptContent.append(allocateBodyCommand(Arrays.asList(externalScriptMap)));
			}
			TedamFileUtils.writeToTextDocumentUTF8(baseBshAbsolutePath, scriptContent.toString());
			LOGGER.info("TEDAM base.bsh has been produced successfully.");
			testReport = new TestReport("1", FileName.BASE.getName() + Constants.TEXT_UNDERSCORE + TedamProcessUtils.getClientHostName() + Constants.FILE_EXTENSION_BSH);
			testReport.setTestStepId(1);
			testReport.addMessage(Constants.BSH_CREATION_SUCCESS);
			testReport.setStatusMsg(StatusMessages.SUCCEEDED.getStatus());
			testReportList.add(testReport);
			new ScriptService().printTestReport(testReportList, bshReportName, PropUtils.getProperty(Constants.TEMP_FILE_PATH) + Constants.FILE_SEPARATOR);
		} catch (IOException ex) {
			LOGGER.error("Base.bsh creation failed " + ex);
			testReport = new TestReport("1", FileName.BASE.getName() + Constants.TEXT_UNDERSCORE + TedamProcessUtils.getClientHostName() + Constants.FILE_EXTENSION_BSH);
			testReport.setTestStepId(1);
			testReport.addMessage(Constants.BSH_CREATION_FAILED);
			testReport.setStatusMsg(StatusMessages.FAILED.getStatus());
			new ScriptService().printTestReport(testReportList, bshReportName, PropUtils.getProperty(Constants.TEMP_FILE_PATH) + Constants.FILE_SEPARATOR);
		} catch (Exception e) {
			LOGGER.error("Base.bsh creation failed " + e);
			testReport = new TestReport("1", FileName.BASE.getName() + Constants.TEXT_UNDERSCORE + TedamProcessUtils.getClientHostName() + Constants.FILE_EXTENSION_BSH);
			testReport.setTestStepId(1);
			testReport.addMessage(Constants.BSH_CREATION_FAILED);
			testReport.setStatusMsg(StatusMessages.FAILED.getStatus());
			new ScriptService().printTestReport(testReportList, bshReportName, PropUtils.getProperty(Constants.TEMP_FILE_PATH) + Constants.FILE_SEPARATOR);
		}
	}

	/**
	 * this method getExternalScriptMap <br>
	 * @author Canberk.Erkmen
	 * @param paramMapList
	 * @return <br>
	 */
	private static EnumMap<ScriptParameters, Object> getExternalScriptMap(List<EnumMap<ScriptParameters, Object>> paramMapList) {
		EnumMap<ScriptParameters, Object> externalScriptMap = BaseBshGeneratorUtil.getOperationTypeMapFromList(paramMapList, OperationTypes.SCRIPT);
		if (externalScriptMap != null) {
			EnumMap<ScriptParameters, Object> testCaseIdMap = BaseBshGeneratorUtil.getOperationTypeMapFromList(paramMapList, OperationTypes.TEST_CASE_ID);
			String testCaseId = (String) testCaseIdMap.get(ScriptParameters.TEST_CASE_ID);
			externalScriptMap.put(ScriptParameters.TEST_CASE_ID, testCaseId);
		}
		return externalScriptMap;
	}

	/**
	 * this method getConfigFilePath <br>
	 * @author Canberk.Erkmen
	 * @param paramMapList
	 * @return <br>
	 */
	private static String getConfigFilePath(List<EnumMap<ScriptParameters, Object>> paramMapList) {
		EnumMap<ScriptParameters, Object> configFileMap = BaseBshGeneratorUtil.getOperationTypeMapFromList(paramMapList, OperationTypes.CONFIG_FILE_PATH);
		String configFilePath = Constants.EMPTY_STRING;
		if (configFileMap != null) {
			configFilePath = (String) configFileMap.get(ScriptParameters.CONFIG_FILE_PATH);
		}
		return configFilePath;
	}

	/**
	 * Base.bsh, such as Import and AddClassPath, contains sections that must be defined at startup.
	 * 
	 * @param baseBshAbsolutePath
	 * 
	 * 
	 * @param writer
	 *            : output writer
	 * @throws IOException
	 */
	private static StringBuilder initiateScript(String projectFilePath, String configFilePath, String timeRecording) {
		StringBuilder initialContent = new StringBuilder();
		initialContent.append("addClassPath(\"" + projectFilePath + Constants.FILE_SEPARATOR + FilePath.LIB_TEDAM_JGUAR.getPath() + Constants.FILE_SEPARATOR
				+ FileName.TEDAM_JAR_2_1.getName() + "\")" + Constants.TEXT_COMMANEWLINE);

		// import java requirements
		initialContent.append("import javax.xml.parsers.*" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.jcabi.xml.XMLDocument" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import org.w3c.dom.*" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import org.w3c.dom.Element" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.util.*" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.player.*" + Constants.TEXT_COMMANEWLINE);
		initialContent.append(Constants.TEXT_NEWLINE);
		// import system requirements
		initialContent.append("import com.lbs.tedam.bsh.utils.*" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.model.*" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.model.SnapshotValue" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.model.DTO.*" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.*" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.exception.*" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.exception.CreateNewFileException" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.*" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.StatusMessages" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.Regex" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.ScriptParameters" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.FormOpenTypes" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.TedamLogLevel" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.FileNames" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.FileName" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.FilePath" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.FilePaths" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.util.Enums.TedamLauncherVars" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("import com.lbs.tedam.recorder.*" + Constants.TEXT_COMMANEWLINE);

		initialContent.append(Constants.TEXT_NEWLINE);
		// Adding other modular bsh files to base.bsh
		initialContent.append("$I(FunctionalScripts" + Constants.FILE_SEPARATOR + "MethodsNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(FormOpen" + Constants.FILE_SEPARATOR + "FormOpenNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(FormFill" + Constants.FILE_SEPARATOR + "FormFillNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(FilterFill" + Constants.FILE_SEPARATOR + "FilterFillNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(Verify" + Constants.FILE_SEPARATOR + "VerifyBaseNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(ButtonClick" + Constants.FILE_SEPARATOR + "ButtonClickNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(PopUp" + Constants.FILE_SEPARATOR + "PopUpNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(GridSearch" + Constants.FILE_SEPARATOR + "GridSearchNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(GridSelect" + Constants.FILE_SEPARATOR + "GridSelectNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(Report" + Constants.FILE_SEPARATOR + "ReportNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(MessageDialog" + Constants.FILE_SEPARATOR + "MessageDialogNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(RowDelete" + Constants.FILE_SEPARATOR + "RowDeleteNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("$I(DoubleClick" + Constants.FILE_SEPARATOR + "DoubleClickNG.bsh)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append(Constants.TEXT_NEWLINE);
		// Instantiate global variables
		initialContent.append("private static String configFilePath = \"" + configFilePath + "\"" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static List reportList = new ArrayList()" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static boolean passCond = true" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static String sourceOperation" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static int expandedRootConfig = 0" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static boolean isLookup = false" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static boolean addToReport = true" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static boolean continueOnError = true" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static boolean continueOnErrorReport = true" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static boolean isWriteFilters = true" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static Map paramMap = new HashMap()" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static Element formElement = null" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("ScriptService su = new ScriptService(configFilePath)" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("ssvList = new ArrayList()" + Constants.TEXT_COMMANEWLINE);

		initialContent.append("formName = \"\"" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("TimeRecorder recorder = new TestStepTimeRecorder(" + timeRecording + ")" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("TedamLogLevel logLevelInfo = TedamLogLevel.INFO" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("TedamLogLevel logLevelWarn = TedamLogLevel.WARN" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("TedamLogLevel logLevelError = TedamLogLevel.ERROR" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private static boolean printLog = true" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private String headerBase = \"BASE.BSH\"" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private String headerGetSnapshot = \"GETSNAPSHOT.BSH\"" + Constants.TEXT_COMMANEWLINE);
		// initialContent.append("private String baseBshScript = " + baseBshScript);
		initialContent.append("private String projectFile = \"" + projectFilePath + "\"" + Constants.TEXT_COMMANEWLINE);
		long messageDialogSleepTime = Long.valueOf(PropUtils.getProperty("messageDialogSleepTime").trim()).longValue();
		long synchronizeFormsWaitTime = Long.valueOf(PropUtils.getProperty("bsh.synchronizeforms.waitmillis").trim()).longValue();
		long buttonExistWaitMillis = Long.valueOf(PropUtils.getProperty("bsh.buttonClick.buttonExistWaitMillis").trim()).longValue();
		long buttonExistSleepMillis = Long.valueOf(PropUtils.getProperty("bsh.buttonClick.buttonExistSleepMillis").trim()).longValue();
		long snapshotWaitMillis = Long.valueOf(PropUtils.getProperty("bsh.verify.snapshotwaitmillis").trim()).longValue();
		long snapshotSleepMillis = Long.valueOf(PropUtils.getProperty("bsh.verify.snapshotsleepmillis").trim()).longValue();
		long waitForPendingFormWaitTime = Long.valueOf(PropUtils.getProperty("bsh.waitforpendingform.waitmillis").trim()).longValue();
		String tysMachine = String.valueOf(PropUtils.getProperty("tysMachine").trim());
		initialContent.append("private long messageDialogSleepTime = " + messageDialogSleepTime + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private long synchronizeFormsWaitTime = " + synchronizeFormsWaitTime + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private long buttonExistWaitMillis = " + buttonExistWaitMillis + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private long buttonExistSleepMillis = " + buttonExistSleepMillis + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private long snapshotWaitMillis = " + snapshotWaitMillis + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private long snapshotSleepMillis = " + snapshotSleepMillis + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private long waitForPendingFormWaitTime = " + waitForPendingFormWaitTime + Constants.TEXT_COMMANEWLINE);
		initialContent.append("private String tysMachine = \"" + tysMachine + "\"" + Constants.TEXT_COMMANEWLINE);
		initialContent.append("su.log(headerBase, \"messageDialogSleepTime :\" + messageDialogSleepTime + " //
				+ "\" synchronizeFormsWaitTime :\" + synchronizeFormsWaitTime + " //
				+ "\" buttonExistWaitMillis :\" + buttonExistWaitMillis + " //
				+ "\" buttonExistSleepMillis :\" + buttonExistSleepMillis + " //
				+ "\" snapshotWaitMillis :\" + snapshotWaitMillis + " //
				+ "\" snapshotSleepMillis :\" + snapshotSleepMillis + " //
				+ "\" waitForPendingFormWaitTime :\" + waitForPendingFormWaitTime + " //
				+ "\" tysMachine :\" + tysMachine " //
				+ ", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
		// // open the product and create the file which is proof that base.bsh has started.
		// initialContent.append(
		// "su.createControlFile(\"" + ProjectPath.TEMP_FILE.getPath() + Constants.FILE_SEPARATOR + FileName.CONTROL_FILE.getName() + "\")" + Constants.TEXT_COMMANEWLINE);
		initialContent.append(Constants.TEXT_NEWLINE);
		return initialContent;
	}

	/**
	 * It contains operations to be set at the end of base.bsh.
	 * 
	 * @param writer
	 * @throws IOException
	 */
	private static StringBuilder endScript() {
		StringBuilder endContent = new StringBuilder();
		// Output TestReport
		endContent.append("su.printTestReport(reportList, resultFileName,\"" + PropUtils.getProperty(Constants.TEMP_FILE_PATH) + Constants.FILE_SEPARATOR + "\")"
				+ Constants.TEXT_COMMANEWLINE);
		endContent.append("su.fillReleaseInfo(recorder.getRecordList(), \"" + version + "\")" + Constants.TEXT_COMMANEWLINE);
		endContent.append("su.saveTestStepTimeRecordList(recorder.getRecordList())" + Constants.TEXT_COMMANEWLINE);
		// Terminate TestCase
		endContent.append("closeAllForms()" + Constants.TEXT_COMMANEWLINE);
		endContent.append("su.log(headerBase, \"TestCase Ends\", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
		// endContent.append(
		// "su.createFinishFile(\"" + ProjectPath.TEMP_FILE.getPath() + Constants.FILE_SEPARATOR + FileName.FINISH_FILE.getName() + "\")" + Constants.TEXT_COMMANEWLINE);
		// endContent.append("su.log(headerBase, \"scriptService.createFinishFile() cagirildi.\", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
		endContent.append("su.log(headerBase, \"TPW.finishUnitTest()\", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
		endContent.append("TPW.finishUnitTest()" + Constants.TEXT_COMMANEWLINE);
		// endContent.append("su.log(headerBase, \"TPW.finishUnitTest() cagirildi.\", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
		return endContent;
	}

	private static void appendTimeRecorderScriptPartBegin(StringBuilder content, EnumMap<ScriptParameters, Object> paramMap) {
		if (paramMap.containsKey(ScriptParameters.TEST_STEP_ID))
			testStepId = Integer.valueOf((String) paramMap.get(ScriptParameters.TEST_STEP_ID));
		content.append(Constants.TEXT_NEWLINE_SCRIPT + "recorder.record(new Runnable() {" + Constants.TEXT_NEWLINE_SCRIPT + Constants.TEXT_NEWLINE_SCRIPT + "public void run() {"
				+ Constants.TEXT_NEWLINE_SCRIPT + Constants.TEXT_NEWLINE_SCRIPT);
	}

	private static void appendTimeRecorderScriptPartEnd(StringBuilder content) {
		content.append(Constants.TEXT_TAB + "su.setTestStepIdOfLastRecord(recorder.getRecordList()," + testStepId + ")"
				+ Constants.TEXT_COMMANEWLINE);
	}

	private static void appendTestStepIdToTestReport(StringBuilder content) {
		content.append(Constants.TEXT_TAB + "su.setTestStepIdToReportList(reportList," + testStepId + ")"
				+ Constants.TEXT_COMMANEWLINE);
	}

	/**
	 * Organizes output beanshell scripts content with given arguments.
	 * 
	 * @param command
	 * @param writer
	 * @throws IOException
	 */
	private static StringBuilder allocateBodyCommand(List<EnumMap<ScriptParameters, Object>> paramMapList) {
		StringBuilder bodyContent = new StringBuilder();
		int k = 1;
		for (ListIterator<EnumMap<ScriptParameters, Object>> iterator = paramMapList.listIterator(); iterator.hasNext();) {
			EnumMap<ScriptParameters, Object> paramMap = iterator.next();
			EnumMap<ScriptParameters, Object> nextParamMap = null;
			EnumMap<ScriptParameters, Object> nextNextParamMap = null;
			String expectedFormName = "";
			String nextExpectedFormName = "";
			boolean isLastStep = false; // variable controlling whether it is the last step
			if (iterator.hasNext()) {
				/**
				 * If it is not the last step, we keep nextCommand <br>
				 * 1- If the MessageDialog step comes after PopUp or ButtonClick, to embed it in the PopUp and ButtonClick steps <br>
				 * 2- The step ExpectedFormName value is used at the end of the previous step. This is also why it is used.
				 */
				nextParamMap = iterator.next();
				if (nextParamMap.get(ScriptParameters.STEP_FORM_NAME) != null && !nextParamMap.get(ScriptParameters.OPERATION_TYPE).equals(OperationTypes.REPORT)) {
					/**
					 * Due to the use of OperationTypes.Report, there is no stepFormName concept on the screen that is opened depending on the Report step. But we have to ignore it
					 * in order to print REPORT to the expectedFormName field on the Tedam interface.
					 */
					expectedFormName = nextParamMap.get(ScriptParameters.STEP_FORM_NAME).toString();
				}
				if (nextParamMap.get(ScriptParameters.OPERATION_TYPE).equals(OperationTypes.REPORT)) {
					isLastStep = true;
				}
				/**
				 * In the steps for ButtonClick and popUp, the messageDialog step is included. That's why the next step is messageDialog. messageDialog also expects
				 * expectedFormName to be empty. If you have the next step from us messageDialog step, you need its formName. If we are able to access the array dimension too far,
				 * we get the information of the next step 2.
				 */
				if (iterator.hasNext()) {
					nextNextParamMap = iterator.next();
					iterator.previous();
					iterator.previous();
					if (nextNextParamMap.get(ScriptParameters.STEP_FORM_NAME) != null && !nextNextParamMap.get(ScriptParameters.OPERATION_TYPE).equals(OperationTypes.REPORT)) {
						/**
						 * Due to the use of OperationTypes.Report, there is no stepFormName concept on the screen that is opened depending on the Report step. But we have to
						 * ignore it in order to print REPORT to the expectedFormName field on the Tedam interface.
						 */
						nextExpectedFormName = nextNextParamMap.get(ScriptParameters.STEP_FORM_NAME).toString();
					}
				} else {
					iterator.previous();
				}
			} else {
				isLastStep = true;
			}
			OperationTypes opTypes = (OperationTypes) paramMap.get(ScriptParameters.OPERATION_TYPE);
			switch (opTypes) {
			// Absolute path of Resulting report
			case RESULT_REPORT_ADDRESS:
				bodyContent.append("resultFileName = \"" + paramMap.get(ScriptParameters.RESULT_REPORT_ADDRESS) + ".xls" + "\"" + Constants.TEXT_COMMANEWLINE);
				break;
			// Version
			case VERSION:
				version = (String) paramMap.get(ScriptParameters.VERSION);
				bodyContent.append("version = \"" + version + "\"" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("\n");
				break;
			// Test case ID
			case TEST_CASE_ID:
				bodyContent.append("private static String testCaseId = \"" + paramMap.get(ScriptParameters.TEST_CASE_ID) + "\"" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("su.log(headerBase, \" testCaseId :\" + testCaseId, logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("\n");
				break;
			// FormOpen parameters: menuPath - formOpenType
			case FORM_OPEN:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** FormOpen ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_FORMOPEN_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);

				// if (paramMap.get(ScriptParameters.MENU_PATH).toString().startsWith(Regex.EXCHANGE_RATES.getRegex())) {
				// // If the exchange rate button is clicked on the product home screen
				// bodyContent.append(Constants.TEXT_TAB + "formOpenType = \"" + FormOpenTypes.EXCHANGE_RATES_FORM_OPEN.getType() + "\"" + Constants.TEXT_COMMANEWLINE);
				// bodyContent.append(Constants.TEXT_TAB + "formOpen(formOpenType, null)" + Constants.TEXT_COMMANEWLINE);
				// } else if (paramMap.get(ScriptParameters.MENU_PATH).toString().startsWith(Regex.SET_WORK_DATES.getRegex())) {
				// // If you click on the set work dates button on the main screen
				// bodyContent.append(Constants.TEXT_TAB + "formOpenType = \"" + FormOpenTypes.SET_WORK_DATES_FORM_OPEN.getType() + "\"" + Constants.TEXT_COMMANEWLINE);
				// bodyContent.append(Constants.TEXT_TAB + "formOpen(formOpenType, null)" + Constants.TEXT_COMMANEWLINE);
				// } else {
				// If the browser is opened via the product tree
				String formOpenParameter = paramMap.get(ScriptParameters.MENU_PATH).toString();
				bodyContent.append(Constants.TEXT_TAB + "menuPath = \"" + formOpenParameter + "\"" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "formOpenType = \"" + FormOpenTypes.TREE_FORM_OPEN.getType() + "\"" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "formOpen(formOpenType, menuPath)" + Constants.TEXT_COMMANEWLINE);
				// }

				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			case FORM_OPEN_SHORTCUT:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** FormOpen ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_FORMOPEN_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);

				if (paramMap.get(ScriptParameters.MENU_PATH).toString().startsWith(Regex.EXCHANGE_RATES.getRegex())) {
					// If the exchange rate button is clicked on the product home screen
					bodyContent.append(Constants.TEXT_TAB + "formOpenType = \"" + FormOpenTypes.EXCHANGE_RATES_FORM_OPEN.getType() + "\"" + Constants.TEXT_COMMANEWLINE);
					bodyContent.append(Constants.TEXT_TAB + "formOpen(formOpenType, null)" + Constants.TEXT_COMMANEWLINE);
				} else if (paramMap.get(ScriptParameters.MENU_PATH).toString().startsWith(Regex.SET_WORK_DATES.getRegex())) {
					// If the set work dates button is clicked on the product main screen
					bodyContent.append(Constants.TEXT_TAB + "formOpenType = \"" + FormOpenTypes.SET_WORK_DATES_FORM_OPEN.getType() + "\"" + Constants.TEXT_COMMANEWLINE);
					bodyContent.append(Constants.TEXT_TAB + "formOpen(formOpenType, null)" + Constants.TEXT_COMMANEWLINE);
				}
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// FormFill parameters: snapshotDefinitionId
			case FORM_FILL:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** FormFill ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "snapshotID = " + paramMap.get(ScriptParameters.UPLOADED_SNAPSHOT_ID) + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "ssvFormFillList = su.getSnapshotFormFillValueBOList(version, snapshotID)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "formfill(form, ssvFormFillList, snapshotID)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// FilterFill parameters: snapshotDefinitionId
			case FILTER_FILL:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** FilterFill ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_FILTERFILL_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "snapshotID = " + paramMap.get(ScriptParameters.UPLOADED_SNAPSHOT_ID) + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "ssvFilterFillList = su.getSnapshotFilterFillValueBOList(version, snapshotID)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "filterFill(form, ssvFilterFillList, snapshotID)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// ButtonClick parameters: buttonTag - menuButtonItemNo - aError
			case BUTTON_CLICK:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** ButtonClick ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_BUTTONCLICK_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "buttonTag = " + paramMap.get(ScriptParameters.BUTTON_TAG) + Constants.TEXT_COMMANEWLINE);
				// If the standard button is menuButtonItemNo equality will be null.
				bodyContent.append(Constants.TEXT_TAB + "menuButtonItemNo = " + paramMap.get(ScriptParameters.MENU_BUTTON_RESOURCE_TAG) + Constants.TEXT_COMMANEWLINE);
				if ("1".equals(paramMap.get(ScriptParameters.CONTINUE_ON_ERROR))) {
					// If an error is encountered during the execution, if the parameter is given to take caution and continue
					bodyContent.append(Constants.TEXT_TAB + "continueOnError = true" + Constants.TEXT_COMMANEWLINE);
				} else {
					// If an error is encountered during ButtonClick, if the parameter is given to get a failure and end the testcase
					bodyContent.append(Constants.TEXT_TAB + "continueOnError = false" + Constants.TEXT_COMMANEWLINE);
				}
				boolean hasNextFormName = true; // Although the last step is dialogue dialogue, we do not want to call synchForms from ButtonClick
				// Integrate adjacent messageDialog step into current ButtonClick step

				if (nextParamMap != null && nextParamMap.get(ScriptParameters.OPERATION_TYPE).toString().equalsIgnoreCase(Enums.OperationTypes.MESSAGE_DIALOG.toString())) {
					k++;// Dialog also looks like a step in TedamFace
					iterator.next();
					bodyContent.append(BaseBshGeneratorUtil.injectAdjacentMessageDialogStep(nextParamMap, k));
					expectedFormName = nextExpectedFormName;
					hasNextFormName = false;
				} else {
					// If there is no messageDialog type in the next step
					bodyContent.append(Constants.TEXT_TAB + "mdList = null" + Constants.TEXT_COMMANEWLINE);
				}
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "buttonClick(form, buttonTag, menuButtonItemNo, mdList)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep, !hasNextFormName));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// Verify parameters: snapshotDefinitionId - continueOnError
			case VERIFY:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** FieldValueVerify ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_FIELDVALUEVERIFY_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);

				bodyContent.append(Constants.TEXT_TAB + "paramMap.clear()" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(
						Constants.TEXT_TAB + "paramMap.put(\"snapshotDefinitionId\"," + paramMap.get(ScriptParameters.UPLOADED_SNAPSHOT_ID) + ")" + Constants.TEXT_COMMANEWLINE);
				String isIgnoreRowIndex = "0".equalsIgnoreCase(paramMap.get(ScriptParameters.IS_IGNORE_ROW_INDEX).toString())
						? Constants.TEXT_TAB + "boolean isIgnoreRownIndex = false" : Constants.TEXT_TAB + "boolean isIgnoreRownIndex = true";
				bodyContent.append(isIgnoreRowIndex + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "verify(form, paramMap, sourceOperation, isIgnoreRownIndex)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep, true));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// RowCountVerify parameters: gridTag - rowCount - continueOnError
			case ROW_COUNT_VERIFY:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** RowCountVerify ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_ROWCOUNTVERIFY_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "paramMap.clear()" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "paramMap.put(\"gridTag\", " + paramMap.get(ScriptParameters.GRID_TAG) + ")" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "paramMap.put(\"rowCount\", " + paramMap.get(ScriptParameters.ROW_COUNT) + ")" + Constants.TEXT_COMMANEWLINE);
				if ("0".equals(paramMap.get(ScriptParameters.CONTINUE_ON_ERROR))) {
					bodyContent.append(Constants.TEXT_TAB + "continueOnError = false" + Constants.TEXT_COMMANEWLINE);
				}
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "verify(form, paramMap, sourceOperation, false)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "continueOnError = true " + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep, true));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// Verify parameters: message - continueOnError
			case MESSAGE_VERIFY:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** MessageVerify ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_MESSAGEVERIFY_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "paramMap.clear()" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(
						Constants.TEXT_TAB + "paramMap.put(\"message\", \"" + paramMap.get(ScriptParameters.MESSAGE_VERIFY_PARAMETER) + "\")" + Constants.TEXT_COMMANEWLINE);
				if ("0".equals(paramMap.get(ScriptParameters.CONTINUE_ON_ERROR))) {
					bodyContent.append(Constants.TEXT_TAB + "continueOnError = false" + Constants.TEXT_COMMANEWLINE);
				}
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "verify(form, paramMap, sourceOperation, false)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "continueOnError = true " + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// PopUp parameters: popUpItemName - gridTag - columnTag - rowIndex
			// There are 3 different popup parameterization methods:
			// 1- popUpItemName
			// 2- gridTag & popUpItemName
			// 3- gridTag & columnTag & rowIndex & popUpItemName
			case POP_UP:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** PopUp ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_POPUP_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "popUpItemName = \"" + paramMap.get(ScriptParameters.POP_UP_TAG) + "\"" + Constants.TEXT_COMMANEWLINE);
				// If no gridTag information is given within the parameter, the gridTag equation will be null.
				bodyContent.append(Constants.TEXT_TAB + "gridTag = " + paramMap.get(ScriptParameters.GRID_TAG) + Constants.TEXT_COMMANEWLINE);
				// If no columnTag information is given within the parameter, the columnTag constant will be null.
				bodyContent.append(Constants.TEXT_TAB + "columnTag = " + paramMap.get(ScriptParameters.COLUMN_TAG) + Constants.TEXT_COMMANEWLINE);
				// If no rowIndex information is given within the parameter, rowIndex equality will be null.
				bodyContent.append(Constants.TEXT_TAB + "rowIndex = " + paramMap.get(ScriptParameters.ROW_INDEX) + Constants.TEXT_COMMANEWLINE);
				// Integrate adjacent messageDialog step into current ButtonClick step
				boolean hasNextFormNamePu = true; // Although the final step is to have the message dialogue, we do not call SynchForms from PopUp
				if (nextParamMap != null && nextParamMap.get(ScriptParameters.OPERATION_TYPE).toString().equalsIgnoreCase(Enums.OperationTypes.MESSAGE_DIALOG.toString())) {
					k++;// Dialog also appears to be a step in TedamFace.
					iterator.next();
					bodyContent.append(BaseBshGeneratorUtil.injectAdjacentMessageDialogStep(nextParamMap, k));
					expectedFormName = nextExpectedFormName;
					hasNextFormNamePu = false;
				} else {
					// If the messageDialog type does not exist in the next step
					bodyContent.append(Constants.TEXT_TAB + "mdList = null" + Constants.TEXT_COMMANEWLINE);
				}
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "popUp(form, popUpItemName, gridTag, columnTag, rowIndex, mdList, false)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep, !hasNextFormNamePu));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// GridSearch parameters: gridTag - gridSearchParameter
			// There are 2 different GridSearch parametering methods:
			// 1- gridSearchParameter
			// 2- gridTag & gridSearchParameter
			case GRID_SEARCH:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** GridSearch ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "gridTag = " + paramMap.get(ScriptParameters.GRID_TAG) + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "gridSearchParameter = \"" + paramMap.get(ScriptParameters.SEARCH_DETAILS) + "\"" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "gridSearch(form, gridTag, gridSearchParameter)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// GridRowSearch parameter: gridTag - rowIndexList
			case GRID_ROW_SELECT:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** GridRowSelect ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_GRIDROWSELECT_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "paramMap.clear()" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "paramMap.put(\"gridTag\"," + paramMap.get(ScriptParameters.GRID_TAG) + ")" + Constants.TEXT_COMMANEWLINE);
				// Since more than one row can be selected, we can get more than one row index in the parameter.
				bodyContent.append(Constants.TEXT_TAB + "paramMap.put(\"rowIndexList\", su.getRowIndexList(\"" + paramMap.get(ScriptParameters.ROW_INDEX_LIST) + "\"))"
						+ Constants.TEXT_COMMANEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "gridSelect(form, paramMap)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// GridCellSearch parameter: gridTag - rowIndex - columnTag
			case GRID_CELL_SELECT:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** GridCellSelect ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_GRIDCELLSELECT_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "paramMap.clear()" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "paramMap.put(\"gridTag\"," + paramMap.get(ScriptParameters.GRID_TAG) + ")" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "paramMap.put(\"columnTag\"," + paramMap.get(ScriptParameters.COLUMN_TAG) + ")" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "paramMap.put(\"rowIndex\"," + paramMap.get(ScriptParameters.ROW_INDEX_LIST).toString().replace("[", "").replace("]", "")
						+ ")" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "gridSelect(form, paramMap)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// RowDelete parameter: gridTag - rowIndex
			case GRID_DELETE:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** RowDelete ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_ROWDELETE_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "gridTag = " + paramMap.get(ScriptParameters.GRID_TAG) + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "rowIndex = " + paramMap.get(ScriptParameters.ROW_INDEX) + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "rowDelete(form, gridTag, rowIndex)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// DoubleClick parameter: gridTag - rowIndex
			case DOUBLE_CLICK:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** DoubleClick ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_DOUBLECLICK_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "gridTag = " + paramMap.get(ScriptParameters.GRID_TAG) + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "rowIndex = " + paramMap.get(ScriptParameters.ROW_INDEX) + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.containerActivate());
				bodyContent.append(Constants.TEXT_TAB + "doubleClick(form, gridTag, rowIndex)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// Report parameter: reportFileName
			case REPORT:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** Report ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_REPORT_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "reportFileName = " + "\"" + paramMap.get(ScriptParameters.PATH) + "\"" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "reportWaitSleepMillis = " + paramMap.get(ScriptParameters.REPORT_WAIT_SLEEP_MILLIS) + Constants.TEXT_COMMANEWLINE);
				// ContinueOnError
				if ("0".equals(paramMap.get(ScriptParameters.CONTINUE_ON_ERROR_REPORT))) {
					// If error is encountered during reporting, if parameter is given to take caution and continue
					bodyContent.append(Constants.TEXT_TAB + "continueOnErrorReport = false" + Constants.TEXT_COMMANEWLINE);
				} else {
					// If an error is encountered during the reporting, if a failure occurs and parameters are given to end the testcase
					bodyContent.append(Constants.TEXT_TAB + "continueOnErrorReport = true" + Constants.TEXT_COMMANEWLINE);
				}
				// Filters Print
				if ("0".equals(paramMap.get(ScriptParameters.IS_WRITE_FILTERS))) {
					// If you want to print the filters during the report
					bodyContent.append(Constants.TEXT_TAB + "isWriteFilters = false" + Constants.TEXT_COMMANEWLINE);
				} else {
					// If you do not want to print filters during the report
					bodyContent.append(Constants.TEXT_TAB + "isWriteFilters = true" + Constants.TEXT_COMMANEWLINE);
				}
				bodyContent.append(Constants.TEXT_TAB + "report(reportFileName, continueOnErrorReport, isWriteFilters, reportWaitSleepMillis)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			// MessageDialog parameter: mdList
			case MESSAGE_DIALOG:
				bodyContent.append("/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
				bodyContent.append("su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("/**************** MessageDialog ****************/" + Constants.TEXT_NEWLINE);
				appendTimeRecorderScriptPartBegin(bodyContent, paramMap);
				bodyContent.append("sourceOperation = Constants.OPERATION_MESSAGEDIALOG_BSH" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append("messageDialogbshtr = new BSHTestReport(\"MessageDialog\", formName)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "messageDialogParameter = \"" + paramMap.get(ScriptParameters.MESSAGE_DIALOG_DETAILS) + "\"" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "mdList = su.messageDialogParameterParser(messageDialogParameter)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "messageDialogbshtr = messageDialog(mdList, messageDialogbshtr, true)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + "reportList.add(messageDialogbshtr)" + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_FORMNAME + Constants.TEXT_COMMANEWLINE);
				bodyContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
				bodyContent.append(BaseBshGeneratorUtil.syncronizeForm(expectedFormName, isLastStep, true));
				printEndOfTestStep(bodyContent);
				k++;
				break;
			case CONFIG_FILE_PATH:
				break;
			case SCRIPT:
				RestClient restClient = new RestClient();
				String url = PropUtils.getProperty(Constants.BASE_REST_URL) + "TedamRestService/getFileContent";
				String testCaseId = (String) paramMap.get(ScriptParameters.TEST_CASE_ID);
				String fileName = (String) paramMap.get(ScriptParameters.SCRIPT_FILE_NAME);
				String jsonString = restClient.getValue(url, testCaseId, fileName);
				bodyContent.append(jsonString);
				break;
			case TIME_RECORDING:
				break;
			}
		}
		return bodyContent;
	}

	private static void printEndOfTestStep(StringBuilder content) {
		content.append(Constants.SCRIPT_SYNTAX_CHECKISFAILEDBEFORE + Constants.TEXT_NEWLINE);
		appendTimeRecorderScriptPartEnd(content);
		appendTestStepIdToTestReport(content);
		content.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_BREAKCHECKBEGIN + Constants.TEXT_NEWLINE);
		content.append(
				Constants.DOUBLE_TEXT_TAB + Constants.SCRIPT_SYNTAX_SETADDTOREPORT + Constants.TEXT_COMMANEWLINE);
		content.append(Constants.TEXT_TAB + Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
		content.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
		content.append("}" + Constants.TEXT_NEWLINE_SCRIPT + "})" + Constants.TEXT_COMMANEWLINE);
	}

}
