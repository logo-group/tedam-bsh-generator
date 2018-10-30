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

import java.util.EnumMap;
import java.util.List;

import org.junit.Test;

import com.lbs.tedam.test.BaseTedamTest;
import com.lbs.tedam.util.Constants;
import com.lbs.tedam.util.Enums.FileName;
import com.lbs.tedam.util.Enums.ScriptParameters;
import com.lbs.tedam.util.PropUtils;
import com.lbs.tedam.util.TedamProcessUtils;

public class BaseBshGeneratorTest extends BaseTedamTest {

	/**
	 * @author Tarik.Mikyas <br>
	 *         this method testGenerateBaseScript
	 */
	@Test
	public void testGenerateBaseScript() {
		String[] args = ("projectFile=" + PropUtils.getProperty(Constants.TEDAM_EXECUTION_FILE_PATH) + " path=TEDAM_TX3043_tc17654 version=2.49.6.0 "
				+ "testCaseId=18784 FormOpen=[10000,10100,10101] FormFill=12587!fn!MMXFItemBrowser%0 FilterFill=12588!fn!MMXFItemBrowser%0 ButtonClick=151!ps!1!ps!0!fn!MMXFItemBrowser%0 Verify=12589!ps!1!fn!MMXFItemBrowser%0 RowCountVerify=100!ps!12!ps!1!fn!MMXFItemBrowser%0 MessageVerify=Aynı!spc!özelliklerde!spc!kayıt!spc!mevcut:!spc!dbo.U_002_ITEMS!spc!tablosunun!spc!Code,!spc!SubCompany!spc!alanı!ps!1!fn!MMXFItem%0 PopUp=100!ps!Ekle!fn!MMXFItemBrowser%0 GridSearch=100!ps![(1007,asd),(1001,11),(1003,a2sw),(1014,1)]!fn!MMXFItemBrowser%0 GridCellSelect=100!ps![5]!ps!1007!fn!MMXFItemBrowser%0 GridRowSelect=100!ps![2,1]!fn!MMXFItemBrowser%0 RowDelete=100!ps!1!fn!MMXFItemBrowser%0 DoubleClick=100!ps!3!fn!MMXFItemBrowser%0 Dialog=[(true,ddd)]!fn!MMXFItemBrowser%0 Report=SatisFaturalari!ps!1!ps!1!ps!1!ps!1!fn!REPORT")//
						.split(" ");
		List<EnumMap<ScriptParameters, Object>> paramMapList = BaseBshGeneratorUtil.buildParamMapList(args);
		BaseBshGenerator.generateBaseScript(paramMapList,
				getTempdir() + FileName.BASE.getName() + Constants.TEXT_UNDERSCORE + TedamProcessUtils.getClientHostName() + Constants.FILE_EXTENSION_BSH,
				getResourceConfigFilePath(), "bshReportName");
	}
}
