package com.jinhe.tss.framework.component.log;

import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.jinhe.tss.framework.web.dispaly.grid.GridDataEncoder;
import com.jinhe.tss.framework.web.dispaly.xform.IXForm;
import com.jinhe.tss.framework.web.dispaly.xform.XFormEncoder;
import com.jinhe.tss.framework.web.mvc.BaseActionSupport;

@Controller
@RequestMapping("/log")
public class LogAction extends BaseActionSupport {

    /**
     * 日志展示模板路径
     */
    static final String LOG_XFORM_TEMPLET_PATH = "template/log/xform/LogXForm.xml";
    static final String LOG_GRID_TEMPLET_PATH = "template/log/grid/LogGrid.xml";
    
    static final Integer PAGE_SIZE = 50;  

    private Long  id;
    private int   page = 1;
    
    private LogQueryCondition condition = new LogQueryCondition();
    
    @Autowired private LogService service;

    public String getAllApps4Tree(){
        List<?> data = service.getAllApps();
        
        StringBuffer sb = new StringBuffer("<actionSet><treeNode name=\"全部\" id=\"_rootId\">");
        for(Iterator<?> it = data.iterator(); it.hasNext();){
            String appCode = (String) it.next();
            sb.append("<treeNode id=\"" + appCode + "\" name=\"" + appCode + "\" icon=\"images/app.gif\"/>");
        }
        return print("AppTree", sb.append("</treeNode></actionSet>"));
    }
    
    public String queryLogs4Grid(){
        condition.setPagesize(PAGE_SIZE);
        condition.setCurrentPage(page);
        Object[] objs = service.getLogsByCondition(condition);
        
        GridDataEncoder encoder = new GridDataEncoder(objs[0], LOG_GRID_TEMPLET_PATH);
        
        int totalRows = (Integer) objs[1];
        int currentPageRows = ((List<?>) objs[0]).size();
        
        String pageInfo = generatePageInfo(totalRows, page, PAGE_SIZE, currentPageRows);
        return print(new String[]{"LogList", "PageList"}, new Object[]{encoder, pageInfo});
    }
    
    public String getLogInfo(){
        Log log = service.getLogById(id);          
        return print("LogInfo", new XFormEncoder(LOG_XFORM_TEMPLET_PATH, (IXForm) log));
    }

}

