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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lbs.tedam.util.Constants;
import com.lbs.tedam.util.Enums.OperationTypes;
import com.lbs.tedam.util.Enums.Regex;
import com.lbs.tedam.util.Enums.ScriptParameters;
import com.lbs.tedam.util.EnumsV2.TedamBoolean;

/**
 * 
 * @author Tarik.Mikyas
 * 
 */
public final class BaseBshGeneratorUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(BaseBshGeneratorUtil.class);

	/**
	 * This method parses parameter String and returns needed pieces in a map.
	 * 
	 * @param command
	 */
	public static List<EnumMap<ScriptParameters, Object>> buildParamMapList(String[] args) {
		List<EnumMap<ScriptParameters, Object>> paramMapList = new ArrayList<>();
		for (String arg : args) {
			EnumMap<ScriptParameters, Object> paramMap = new EnumMap<ScriptParameters, Object>(ScriptParameters.class);
			try {
				if (!arg.contains(Regex.EQUALS.getRegex())) {
					LOGGER.error("Invalid arg : " + arg);
					continue;
				}
				// Regular space spaces are implemented.
				String command = arg.replace(Regex.SPACE.getRegex(), Constants.TEXT_BLANK);
				String[] commandKeyAndValue = command.split(Regex.EQUALS.getRegex());
				OperationTypes operationType = OperationTypes.fromName(commandKeyAndValue[0]);
				String parameterLine = commandKeyAndValue[1];
				// If there is any formName information in argument
				if (parameterLine.contains(Regex.FORM_NAME.getRegex())) {
					int regIndex = parameterLine.indexOf(Regex.FORM_NAME.getRegex());
					String formName = parameterLine.substring(regIndex + Regex.FORM_NAME.getRegex().length());
					paramMap.put(ScriptParameters.STEP_FORM_NAME, formName);
					parameterLine = parameterLine.substring(0, parameterLine.indexOf(Regex.FORM_NAME.getRegex()));
				}
				if (parameterLine.contains(Regex.TEST_STEP.getRegex())) {
					int regIndex = parameterLine.indexOf(Regex.TEST_STEP.getRegex());
					String testStepId = parameterLine.substring(regIndex + Regex.TEST_STEP.getRegex().length());
					paramMap.put(ScriptParameters.TEST_STEP_ID, testStepId);
					parameterLine = parameterLine.substring(0, parameterLine.indexOf(Regex.TEST_STEP.getRegex()));
				}
				String[] parameters = parameterLine.split(Regex.PARAMETER_SPLITTER.getRegex());
				paramMap.put(ScriptParameters.OPERATION_TYPE, operationType);
				// Decompose according to the step type
				switch (operationType) {
				case TIME_RECORDING:
					paramMap.put(ScriptParameters.TIME_RECORDING, parameters[0]);
					break;
				case FORM_OPEN:
					paramMap.put(ScriptParameters.MENU_PATH, parameters[0]);
					break;
				case FORM_OPEN_SHORTCUT:
					paramMap.put(ScriptParameters.MENU_PATH, parameters[0]);
					break;
				case FORM_FILL:
					paramMap.put(ScriptParameters.UPLOADED_SNAPSHOT_ID, parameters[0]);
					break;
				case FILTER_FILL:
					paramMap.put(ScriptParameters.UPLOADED_SNAPSHOT_ID, parameters[0]);
					break;
				case BUTTON_CLICK:
					paramMap.put(ScriptParameters.BUTTON_TAG, parameters[0]);
					if (parameters.length > 2) {
						// If button is MenuButton then second parameter of three parameters is menuButtonResourceTag(menuButtonItemNo)
						paramMap.put(ScriptParameters.CONTINUE_ON_ERROR, parameters[2]);
						paramMap.put(ScriptParameters.MENU_BUTTON_RESOURCE_TAG, parameters[1]);
					} else if (parameters.length > 1) {
						// If there is only 2 parameters then second parameter is continueOnError.
						paramMap.put(ScriptParameters.CONTINUE_ON_ERROR, parameters[1]);
					} else {
						// If there is only one parameter, continueOnError parameter's value is default value.
						paramMap.put(ScriptParameters.CONTINUE_ON_ERROR, 0);
					}
					break;
				case VERIFY:
					if (parameters.length == 1) { // the old data only has uploadedSnapshotId. Parameters for not having IS_IGNORE_ROW_INDEX. all with length i 1
													// in data
													// default false (0) set to be set.
						paramMap.put(ScriptParameters.UPLOADED_SNAPSHOT_ID, parameters[0]);
						paramMap.put(ScriptParameters.IS_IGNORE_ROW_INDEX, TedamBoolean.FALSE.getCode().toString());
					} else if (parameters.length == 2) {
						paramMap.put(ScriptParameters.UPLOADED_SNAPSHOT_ID, parameters[0]);
						paramMap.put(ScriptParameters.IS_IGNORE_ROW_INDEX, parameters[1]);
					}
					paramMap.put(ScriptParameters.UPLOADED_SNAPSHOT_ID, parameters[0]);
					break;
				case ROW_COUNT_VERIFY:
					if (parameters.length == 3) {
						// If the gridTag / number of lines and the values for continuing in the line are given as parameters.
						paramMap.put(ScriptParameters.GRID_TAG, parameters[0]);
						paramMap.put(ScriptParameters.ROW_COUNT, parameters[1]);
						paramMap.put(ScriptParameters.CONTINUE_ON_ERROR, parameters[2]);
					} else if (parameters.length == 2) {
						// If the number of lines and the values for continuing in the line are given as parameters.
						paramMap.put(ScriptParameters.ROW_COUNT, parameters[0]);
						paramMap.put(ScriptParameters.CONTINUE_ON_ERROR, parameters[1]);
					}
					break;
				case MESSAGE_VERIFY:
					paramMap.put(ScriptParameters.MESSAGE_VERIFY_PARAMETER, parameters[0]);
					paramMap.put(ScriptParameters.CONTINUE_ON_ERROR, parameters[1]);
					break;
				case POP_UP:
					paramMap.put(ScriptParameters.POP_UP_TAG, parameters[parameters.length - 1]);
					if (parameters.length > 1) {
						// If the pop text / gridTag values are given as parameters.
						paramMap.put(ScriptParameters.GRID_TAG, parameters[0]);
					}
					if (parameters.length > 3) {
						// If the pop text / gridTag / rowIndex / columnTag values are given as parameters.
						paramMap.put(ScriptParameters.ROW_INDEX, parameters[1]);
						paramMap.put(ScriptParameters.COLUMN_TAG, parameters[2]);
					}
					break;
				case GRID_SEARCH:
					if (parameters.length > 1) {
						// If the gridTag / search details values are given as parameters.
						paramMap.put(ScriptParameters.GRID_TAG, parameters[0]);
						paramMap.put(ScriptParameters.SEARCH_DETAILS, parameters[1]);
					} else {
						// If only the search detail values are given as parameters.
						paramMap.put(ScriptParameters.SEARCH_DETAILS, parameters[0]);
					}
					break;
				case GRID_CELL_SELECT:
					paramMap.put(ScriptParameters.COLUMN_TAG, parameters[2]);
					paramMap.put(ScriptParameters.GRID_TAG, parameters[0]);
					paramMap.put(ScriptParameters.ROW_INDEX_LIST, parameters[1]);
					break;
				case GRID_ROW_SELECT:
					paramMap.put(ScriptParameters.GRID_TAG, parameters[0]);
					paramMap.put(ScriptParameters.ROW_INDEX_LIST, parameters[1]);
					break;
				case GRID_DELETE:
					paramMap.put(ScriptParameters.GRID_TAG, parameters[0]);
					paramMap.put(ScriptParameters.ROW_INDEX, parameters[1].replace("[", "").replace("]", ""));
					break;
				case DOUBLE_CLICK:
					paramMap.put(ScriptParameters.GRID_TAG, parameters[0]);
					paramMap.put(ScriptParameters.ROW_INDEX, parameters[1].replace("[", "").replace("]", ""));
					break;
				case MESSAGE_DIALOG:
					paramMap.put(ScriptParameters.MESSAGE_DIALOG_DETAILS, parameters[0]);
					break;
				case REPORT:
					paramMap.put(ScriptParameters.PATH, parameters[0]);
					paramMap.put(ScriptParameters.CONTINUE_ON_ERROR_REPORT, parameters[1]);
					paramMap.put(ScriptParameters.IS_WRITE_FILTERS, parameters[2]);
					paramMap.put(ScriptParameters.REPORT_WAIT_SLEEP_MILLIS, parameters[3]);
					break;
				case RESULT_REPORT_ADDRESS:
					paramMap.put(ScriptParameters.RESULT_REPORT_ADDRESS, parameters[0]);
					break;
				case VERSION:
					paramMap.put(ScriptParameters.VERSION, parameters[0]);
					break;
				case TEST_CASE_ID:
					paramMap.put(ScriptParameters.TEST_CASE_ID, parameters[0]);
					break;
				case CONFIG_FILE_PATH:
					paramMap.put(ScriptParameters.CONFIG_FILE_PATH, parameters[0]);
					break;
				case SCRIPT:
					paramMap.put(ScriptParameters.SCRIPT_FILE_NAME, parameters[0]);
					break;
				}
			} catch (Exception e) {
				LOGGER.error("There was an error generating Base.BSH  " + e);
			}
			paramMapList.add(paramMap);
		}
		return paramMapList;
	}

	/**
	 * this method syncronize <br>
	 * @author Tarik.Mikyas
	 * @param expectedFormName
	 * @param isLastStep
	 * @param isNotSynchronize
	 *            <br>
	 */
	public static StringBuilder syncronizeForm(String expectedFormName, boolean isLastStep, boolean isNotSynchronize) {
		StringBuilder syncronizeContent = new StringBuilder();
		boolean isSynchronizeForms = false;
		// If it is the last step, you do not need to call isLastStep = true synchronizeForms() procedure
		// If verify is not required to call the isNotSynchronize = true synchronizeForms() procedure
		// If RowCountVerify does not need to call the isNotSynchronize = true synchronizeForms() procedure
		if (isLastStep || isNotSynchronize) {
			isSynchronizeForms = true;
		}
		if (expectedFormName.isEmpty()) {
			syncronizeContent.append(formNameSyncronizerInterpret(isSynchronizeForms)); // If true, synchronizeForms () will not be written.
		} else {
			syncronizeContent.append(waitFormPendingFormName(expectedFormName));
		}
		syncronizeContent.append(Constants.TEXT_NEWLINE);
		return syncronizeContent;
	}

	/**
	 * this method syncronize <br>
	 * @author Tarik.Mikyas
	 * @param expectedFormName
	 * @param isLastStep
	 *            <br>
	 */
	public static StringBuilder syncronizeForm(String expectedFormName, boolean isLastStep) {
		StringBuilder syncronizeContent = new StringBuilder();
		if (expectedFormName.isEmpty()) {
			syncronizeContent.append(formNameSyncronizerInterpret(isLastStep));
		} else {
			syncronizeContent.append(waitFormPendingFormName(expectedFormName));
		}
		syncronizeContent.append(Constants.TEXT_NEWLINE);
		return syncronizeContent;
	}

	/**
	 * This method includes buttonClick and popup steps in successive steps of messageDialog.
	 * 
	 * @author Ozgur.Ozbil
	 * @param nextCommandProp
	 * @param k
	 */
	public static StringBuilder injectAdjacentMessageDialogStep(Map<ScriptParameters, Object> nextCommandProp, int k) {
		StringBuilder messageDialogContent = new StringBuilder();
		messageDialogContent.append(Constants.TEXT_TAB + "// MessageDialog //" + Constants.TEXT_NEWLINE);
		messageDialogContent.append(Constants.TEXT_TAB + "/** ################# STEP " + k + "################# **/" + Constants.TEXT_NEWLINE);
		messageDialogContent.append(Constants.TEXT_TAB + "su.log(headerBase, \" ##### MessageDialog ##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
		messageDialogContent.append(Constants.TEXT_TAB + "su.log(headerBase, \" ##### STEP " + k + "##### \", logLevelInfo, printLog)" + Constants.TEXT_COMMANEWLINE);
		messageDialogContent
				.append(Constants.TEXT_TAB + "messageDialogParameter = \"" + nextCommandProp.get(ScriptParameters.MESSAGE_DIALOG_DETAILS) + "\"" + Constants.TEXT_COMMANEWLINE);
		messageDialogContent.append(Constants.TEXT_TAB + "mdList = su.messageDialogParameterParser(messageDialogParameter)" + Constants.TEXT_COMMANEWLINE);
		// The next step is skipped because this messagedialog is integrated into this line.
		return messageDialogContent;
	}

	/**
	 * Adds a synchronizer that waits for defined amount of seconds for a new form comes forward
	 * 
	 * @author Ozgur.Ozbil
	 * @author Tarik.Mikyas
	 */
	private static StringBuilder formNameSyncronizerInterpret(boolean isSynchronizeForms) {
		StringBuilder formNameSyncronizerContent = new StringBuilder();
		// If the last step is not necessary to call synchronizeForms() procedure.
		// If verify is not needed to call the synchronizeForms() procedure.
		// If RowCountVerify does not need to call the synchronizeForms() procedure.
		if (!isSynchronizeForms) {
			// If there is no Expected formname, wait a new screen for a certain period of time and if the new screen does not open, continue with the current screen
			formNameSyncronizerContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
			formNameSyncronizerContent.append(Constants.TEXT_TAB + "synchronizeForms(synchronizeFormsWaitTime)" + Constants.TEXT_COMMANEWLINE);
			formNameSyncronizerContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
			return formNameSyncronizerContent;
		}

		return formNameSyncronizerContent;
	}

	/**
	 * Adds a synchronizer that waits for defined seconds for a form with given expectedFormName to come forward.
	 * 
	 * @author Ozgur.Ozbil
	 * @param expectedFormName
	 */
	private static StringBuilder waitFormPendingFormName(String expectedFormName) {
		StringBuilder waitFormContent = new StringBuilder();
		// Expected formName, if any, used sync unit
		waitFormContent.append(Constants.SCRIPT_SYNTAX_CONTINUECHECKBEGIN + Constants.TEXT_NEWLINE);
		waitFormContent.append(Constants.TEXT_TAB + "expectedFormName = \"" + expectedFormName + "\"" + Constants.TEXT_COMMANEWLINE);
		waitFormContent.append(Constants.TEXT_TAB + "passCond = waitForPendingForm(expectedFormName)" + Constants.TEXT_COMMANEWLINE);
		waitFormContent.append(Constants.TEXT_TAB + "formName = expectedFormName" + Constants.TEXT_COMMANEWLINE);
		waitFormContent.append(Constants.SCRIPT_SYNTAX_CURLYBRACEEND + Constants.TEXT_NEWLINE);
		return waitFormContent;
	}

	/**
	 * Adds form initiater
	 * 
	 * @author Ozgur.Ozbil
	 */
	public static StringBuilder containerActivate() {
		StringBuilder containerActivateContent = new StringBuilder();
		// Used to capture and activate the form.
		containerActivateContent.append(Constants.TEXT_TAB + "form = TPW.getContainer(formName + Constants.FILE_EXTENSION_JFM)" + Constants.TEXT_COMMANEWLINE);
		containerActivateContent.append(Constants.TEXT_TAB + "form.activate()" + Constants.TEXT_COMMANEWLINE);
		return containerActivateContent;
	}

	public static EnumMap<ScriptParameters, Object> getOperationTypeMapFromList(List<EnumMap<ScriptParameters, Object>> paramMapList, OperationTypes operationType) {
		for (EnumMap<ScriptParameters, Object> paramMap : paramMapList) {
			if (paramMap.get(ScriptParameters.OPERATION_TYPE).equals(operationType)) {
				return paramMap;
			}
		}
		return null;
	}
}
