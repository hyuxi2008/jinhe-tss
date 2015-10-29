package com.jinhe.dm.record.file;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.jinhe.dm.data.util.DataExport;
import com.jinhe.dm.record.Record;
import com.jinhe.dm.record.RecordService;
import com.jinhe.dm.record.ddl._Database;
import com.jinhe.tss.framework.Global;
import com.jinhe.tss.framework.web.servlet.AfterUpload;
import com.jinhe.tss.util.EasyUtils;
import com.jinhe.tss.util.FileHelper;

/**
 * 根据数据录入提供的导入模板，填写后导入实现批量录入数据。
 */
public class ImportCSV implements AfterUpload {

	Logger log = Logger.getLogger(this.getClass());
	
	RecordService recordService = (RecordService) Global.getBean("RecordService");

	public String processUploadFile(HttpServletRequest request,
			String filepath, String oldfileName) throws Exception {

		Long recordId  = Long.parseLong(request.getParameter("recordId"));
		Record record = recordService.getRecord(recordId);
		_Database _db = _Database.getDB(record);

		// 解析附件数据
		File targetFile = new File(filepath);
		String dataStr = FileHelper.readFile(targetFile, DataExport.CSV_CHAR_SET);
		String[] rows = EasyUtils.split(dataStr, "\n");
		
		List<Map<String, String>> valuesMaps = new ArrayList<Map<String, String>>();
		for(int index = 1; index < rows.length; index++) { // 第一行为表头，不要
			String row = rows[index];
			String[] fields = row.split(",");
			
			Map<String, String> valuesMap = new HashMap<String, String>();
			for(int j = 0; j < fields.length; j++) {
    			valuesMap.put(_db.fieldCodes.get(j), fields[j]);
        	}
			
			valuesMaps.add(valuesMap);
		}
    	_db.insertBatch(valuesMaps);
		
		// 向前台返回成功信息
		return "parent.alert('导入成功！请刷新查看。'); parent.openActiveTreeNode();";
	}
	
}