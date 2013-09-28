package com.jinhe.tss.portal.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.jinhe.tss.cache.Pool;
import com.jinhe.tss.framework.component.cache.CacheHelper;
import com.jinhe.tss.framework.sso.Environment;
import com.jinhe.tss.framework.web.dispaly.grid.DefaultGridNode;
import com.jinhe.tss.framework.web.dispaly.grid.GridDataEncoder;
import com.jinhe.tss.framework.web.dispaly.grid.IGridNode;
import com.jinhe.tss.framework.web.dispaly.tree.ITreeTranslator;
import com.jinhe.tss.framework.web.dispaly.tree.LevelTreeParser;
import com.jinhe.tss.framework.web.dispaly.tree.TreeEncoder;
import com.jinhe.tss.framework.web.dispaly.xform.XFormEncoder;
import com.jinhe.tss.portal.PortalConstants;
import com.jinhe.tss.portal.engine.FreeMarkerSupportAction;
import com.jinhe.tss.portal.engine.HTMLGenerator;
import com.jinhe.tss.portal.engine.model.PortalNode;
import com.jinhe.tss.portal.engine.releasehtml.MagicRobot;
import com.jinhe.tss.portal.engine.releasehtml.SimpleRobot;
import com.jinhe.tss.portal.entity.ReleaseConfig;
import com.jinhe.tss.portal.entity.Structure;
import com.jinhe.tss.portal.entity.Theme;
import com.jinhe.tss.portal.helper.StrictLevelTreeParser;
import com.jinhe.tss.portal.service.IPortalService;
import com.jinhe.tss.um.permission.PermissionHelper;
import com.jinhe.tss.util.EasyUtils;
import com.jinhe.tss.util.XMLDocUtil;

@Controller
@RequestMapping("/auth/portal")
public class PortalAction extends FreeMarkerSupportAction {
    
    @Autowired private IPortalService service;

    /**
     * 预览门户
     */
    @RequestMapping("/preview/{portalId}")
    public void previewPortal(HttpServletResponse response, HttpServletRequest request, 
    		@PathVariable("portalId") Long portalId)  {    
    	
    	Long themeId = EasyUtils.convertObject2Long(request.getParameter("themeId"));
    	Long pageId  = EasyUtils.convertObject2Long(request.getParameter("pageId"));
    	
        PortalNode portalNode = service.getPortal(portalId, themeId);
        HTMLGenerator gen = new HTMLGenerator(portalNode, pageId, getFreemarkerParser(portalId));

        printHTML(portalId, gen.toHTML(), false);
    }
    
    /**
     * 获取页面上某个版面或者portletInstanse的xml格式内容。
     * 用以替换某块指定的区域（targetId对应的版面（或页面）中布局器的替换区域：navigatorContentIndex）。
     * 菜单点击的时候会调用到本方法。
     */
    @RequestMapping("/xml/{portalId}/{contentId}/{targetId}")
    public void getPortalXML(HttpServletResponse response, HttpServletRequest request, 
    		@PathVariable("portalId")  Long portalId, 
    		@PathVariable("contentId") Long contentId, 
    		@PathVariable("targetId")  Long targetId) {
    	
    	Long themeId = EasyUtils.convertObject2Long(request.getParameter("themeId"));
        PortalNode portalNode = service.getPortal(portalId, themeId);
        HTMLGenerator gen = new HTMLGenerator(portalNode, contentId, targetId, getFreemarkerParser(portalId));
        
        StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"GBK\"?>");
        sb.append("<Response><Portlet>").append(gen.toXML()).append("</Portlet></Response>");
         
        printHTML(portalId, sb.toString(), false);
    }

    /**
     * 静态发布匿名访问的门户
     */
    @RequestMapping("/tohtml/{id}/{type}")
    public void staticReleasePortal(HttpServletResponse response, 
    		@PathVariable("id") Long id, @PathVariable("type") int type) {
    	
        if(type == 1) { // 发布整个站点
            MagicRobot robot = new MagicRobot(id);
            robot.start();
            String feedback = robot.getFeedback();
            
            printSuccessMessage(feedback);
            
        } else if(type == 2) { // 只发布当前页
            ReleaseConfig releaseConfig = service.getReleaseConfig(id);
			String visitUrl = releaseConfig.getVisitUrl();
            new SimpleRobot(visitUrl).start();
            
            printSuccessMessage("页面静态发布门户成功！");
        }
    }
    
    /**
     * 获取所有的Portal对象（取门户结构PortalStructure）并转换成Tree相应的xml数据格式
     */
    @RequestMapping("/list")
    public void getAllStructures4Tree(HttpServletResponse response) {
        List<?> data = service.getAllStructures();
        TreeEncoder encoder = new TreeEncoder(data, new StrictLevelTreeParser());
        print("SourceTree", encoder);
    }
    
    // resourceId 有可能是："_rootId"， 此时传 0 过来。
    @RequestMapping("/operations/{resourceId}")
    public void getOperationsByResource(HttpServletResponse response, @PathVariable("resourceId") Long resourceId) {
        PermissionHelper permissionHelper = PermissionHelper.getInstance();
        String portalResourceType = PortalConstants.PORTAL_RESOURCE_TYPE;
        
        resourceId = (resourceId == 0) ? PortalConstants.ROOT_ID : resourceId;
        List<?> list = permissionHelper.getOperationsByResource(portalResourceType, resourceId);
        
        print("Operation", "p1,p2," + EasyUtils.list2Str(list) );
    }
           
    /**
     * <p>
     * 获取单个门户结构的节点信息，并转换成XForm相应的xml数据格式；
     * 如果该门户结构是根节点，则要一块取出其对应门户Portal的信息
     * </p>
     */
    @RequestMapping("/{id}")
    public void getStructureInfo(HttpServletResponse response, HttpServletRequest request, @PathVariable("id") Long id) {
        XFormEncoder encoder;
        if( DEFAULT_NEW_ID.equals(id) ) { // 如果是新增,则返回一个空的无数据的模板
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("type", request.getParameter("type"));
            map.put("parentId", request.getParameter("parentId"));
            encoder = new XFormEncoder(PortalConstants.PORTALSTRUCTURE_XFORM, map);           
        }
        else {
            Structure info = service.getStructureWithTheme(id);            
            encoder = new XFormEncoder(PortalConstants.PORTALSTRUCTURE_XFORM, info);
            
            // 如果是门户节点，则带出主题信息列表
            if( info.isRootPortal() ) {
                Object[] objs = genComboThemes(info.getPortalId());
                encoder.setColumnAttribute("currentTheme.id", "editorvalue", (String) objs[0]);
                encoder.setColumnAttribute("currentTheme.id", "editortext",  (String) objs[1]);
                encoder.setColumnAttribute("theme.name", "editable", "false");
            }           
        }
        print("DetailInfo", encoder);
    }
    
    private Object[] genComboThemes(Long portalId) {
        List<?> data = service.getThemesByPortal(portalId);
        return EasyUtils.generateComboedit(data, "id", "name", "|");
    }
    
    /**
     * <p>
     * 保存门户结构信息，如果该门户结构PortalStructure是根节点，则要一块保存其门户Portal的信息。
     * </p>
     */    
    @RequestMapping(method = RequestMethod.POST)
    public void save(HttpServletResponse response, Structure ps) {
        boolean isNew = (ps.getId() == null);
        
        Structure portalStructure;
        if(isNew) {        
        	String tempCode = ps.getCode();
            portalStructure = service.createStructure(ps);
            
            /* 如果是在新增门户根节点时上传文件，则此时code和Id还没生成。code值 = 页面随机生成的一个全局变量。 */
            if( ps.getType().equals(Structure.TYPE_PORTAL) ) {
                File tempDir = Structure.getPortalResourceFileDir(tempCode);
                if(tempDir.exists()) {
                    tempDir.renameTo(portalStructure.getPortalResourceFileDir());
                }
            } 
        } 
        else {
            portalStructure = service.updateStructure(ps);
        }
            
        doAfterSave(isNew, portalStructure, "SourceTree");
    }
    
    /**
     * 删除门户结构
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(HttpServletResponse response, @PathVariable("id") Long id) {
        service.deleteStructure(id);
        printSuccessMessage();
    }
    
    /**
     * <p>
     * 停用/启用 门户结构PortalStructure（将其下的disabled属性设为"1"/"0"）
     * 停用时，如果有子节点，同时停用所有子节点（递归过程，子节点的子节点......)
     * 启用时，如果有父节点且父节点为停用状态，则启用父节点（也是递归过程，父节点的父节点......）
     * </p>
     */
    @RequestMapping("/disable/{id}/{state}")
    public void disable(HttpServletResponse response, 
    		@PathVariable("id") Long id, 
    		@PathVariable("state") int state) {
    	
        service.disable(id, state);
        printSuccessMessage();
    }
    
    /**
     * 排序，同层节点排序
     */
    @RequestMapping(value = "/sort/{id}/{targetId}/{direction}", method = RequestMethod.POST)
	public void sort(HttpServletResponse response, 
            @PathVariable("id") Long id, 
            @PathVariable("targetId") Long targetId,  
            @PathVariable("direction") int direction) {
    	
        service.sort(id, targetId, direction);
        printSuccessMessage();
    }
    
    //******************************** 以下为主题管理  ***************************************
    
    /**
     * 获取一个Portal的所有主题
     */
    @RequestMapping("/theme/list/{portalId}")
    public void getThemes4Tree(HttpServletResponse response, @PathVariable("portalId") Long portalId) {
        List<?> themeList = service.getThemesByPortal(portalId);
        
        Structure root = service.getStructure(portalId); 
        final Long defalutThemeId = root.getTheme().getId();
        
        TreeEncoder encoder = new TreeEncoder(themeList);
        encoder.setTranslator(new ITreeTranslator() {
            public Map<String, Object> translate(Map<String, Object> attributes) {
                if(defalutThemeId.equals(attributes.get("id"))) {
                    attributes.put("isDefault", "1");
                    attributes.put("icon", "../framework/images/portal/default_theme.gif"); // 默认主题
                }
                return attributes;
            }
        });
        print("ThemeList", encoder);        
    }
    
    /**
     * 将Portal的一套主题另存为。。。
     */    
    @RequestMapping(value ="/theme/{themeId}/{name}", method = RequestMethod.POST)
    public void saveThemeAs(HttpServletResponse response, 
    		@PathVariable("themeId") Long themeId, @PathVariable("name") String name) {
    	
        Theme newTheme = service.saveThemeAs(themeId, name);       
        doAfterSave(true, newTheme, "ThemeList");
    }
    
    @RequestMapping(value ="/theme/rename/{themeId}/{name}", method = RequestMethod.PUT)
    public void renameTheme(HttpServletResponse response, 
    		@PathVariable("themeId") Long themeId, @PathVariable("name") String name) {
    	
        service.renameTheme(themeId, name);
        printSuccessMessage();
    }
    
    @RequestMapping(value ="/theme/default/{themeId}", method = RequestMethod.PUT)
    public void specifyDefaultTheme(HttpServletResponse response, 
    		@PathVariable("themeId") Long themeId) {
    	
         service.specifyDefaultTheme(themeId);
         printSuccessMessage();
    }
    
    @RequestMapping(value = "/theme/{themeId}", method = RequestMethod.DELETE)
    public void removeTheme(HttpServletResponse response, 
    		@PathVariable("themeId") Long themeId) {
    	
        service.removeTheme(themeId);
        printSuccessMessage();
    }
    
    @RequestMapping(value ="/theme/personal/{portalId}/{themeId}", method = RequestMethod.POST)
    public void savePersonalTheme(HttpServletResponse response, 
    		@PathVariable("portalId") Long portalId, 
    		@PathVariable("themeId") Long themeId) {
    	
        service.savePersonalTheme(portalId, Environment.getOperatorId(), themeId);
        printSuccessMessage("更改主题成功");
    }
    
    //******************************** 以下为门户发布管理 ***************************************
    @RequestMapping("/release/list")
    public void getAllReleaseConfigs4Tree(HttpServletResponse response) {        
        List<?> list = service.getAllReleaseConfigs();
		TreeEncoder encoder = new TreeEncoder(list); 
        print("ReleaseConfigTree", encoder);
    }
    
    @RequestMapping("/release/{id}")
    public void getReleaseConfig(HttpServletResponse response, @PathVariable("id") Long id) {
        XFormEncoder encoder = null;
        if( DEFAULT_NEW_ID.equals(id) ) {
            encoder = new XFormEncoder(PortalConstants.RELEASE_XFORM_TEMPLET);           
        }
        else{
            ReleaseConfig rconfig = service.getReleaseConfig(id);            
            encoder = new XFormEncoder(PortalConstants.RELEASE_XFORM_TEMPLET, rconfig);
            Object[] objs = genComboThemes(rconfig.getPortal().getId());
            encoder.setColumnAttribute("theme.id", "editorvalue", (String) objs[0]);
            encoder.setColumnAttribute("theme.id", "editortext",  (String) objs[1]);
         
        }        
        print("ReleaseConfig", encoder);
    }
    
    @RequestMapping(value ="/release", method = RequestMethod.POST)
    public void saveReleaseConfig(HttpServletResponse response, ReleaseConfig rconfig) {
        boolean isNew = rconfig.getId() == null ? true : false;             
        rconfig = service.saveReleaseConfig(rconfig);         
        doAfterSave(isNew, rconfig, "ReleaseConfigTree");
    }
    
    @RequestMapping(value = "/release/{id}", method = RequestMethod.DELETE)
    public void removeReleaseConfig(HttpServletResponse response, @PathVariable("id") Long id) {
        service.removeReleaseConfig(id);
        printSuccessMessage();
    }
    
    @RequestMapping("/activePortals")
    public void getActivePortals4Tree(HttpServletResponse response) {
        List<?> data = service.getActivePortals();
        TreeEncoder encoder = new TreeEncoder(data, new StrictLevelTreeParser());
        encoder.setNeedRootNode(false);
        print("SourceTree", encoder);
    }
    
    @RequestMapping("/theme4release/{portalId}")
    public void getThemesByPortal(HttpServletResponse response, @PathVariable("portalId") Long portalId) {
        Object[] objs = genComboThemes(portalId);       
        String returnStr = "<column name=\"themeId\" caption=\"主题\" mode=\"string\" " +
        		" editor=\"comboedit\" editorvalue=\"" + objs[0] + "\" editortext=\"" + objs[1] + "\"/>";
        
        print("ThemeList", returnStr);
    }
    
    @RequestMapping("/activePages/{portalId}")
    public void getActivePagesByPortal4Tree(HttpServletResponse response, @PathVariable("portalId") Long portalId) {
        List<?> data = service.getActivePagesByPortal(portalId);
        TreeEncoder encoder = new TreeEncoder(data, new LevelTreeParser());
        encoder.setNeedRootNode(false);
        print("PageTree", encoder);
    }
       
    //******************************* 以下为门户缓存管理 ***************************************
    @RequestMapping("/cache/{portalId}")
    public void cacheManage(HttpServletResponse response, @PathVariable("portalId") Long portalId) {
        List<?> data = service.getThemesByPortal(portalId);
        StringBuffer sb= new StringBuffer("<actionSet>");
        for( Object temp : data ) {
            Theme theme = (Theme) temp;
            sb.append("<cacheItem id=\"").append(theme.getId()).append("\" name=\"").append(theme.getName()).append("\" />");
        }
        print("CacheManage", sb.append("</actionSet>").toString());
    }
    
    @RequestMapping(value = "/cache/{portalId}/{themeId}", method = RequestMethod.DELETE)
    public void flushCache(HttpServletResponse response, 
    		@PathVariable("portalId") Long portalId, 
    		@PathVariable("themeId") Long themeId) {
    	
        Pool pool = CacheHelper.getLongCache();        
        pool.removeObject(PortalConstants.PORTAL_CACHE + "-" + portalId + "-" + themeId);
        printSuccessMessage();
    }
    
    //******************************* 获取门户流量信息 ******************************************
    
    @RequestMapping("/flowrate/{portalId}")
    public void getFlowRate(HttpServletResponse response, @PathVariable("portalId") Long portalId) {
        List<?> rateItems = service.getFlowRate(portalId);
        List<IGridNode> gridList = new ArrayList<IGridNode>();
        for(Iterator<?> it = rateItems.iterator(); it.hasNext();) {
            Object[] objs = (Object[]) it.next();
            DefaultGridNode gridNode = new DefaultGridNode();
            gridNode.getAttrs().put("name", objs[0]);
            gridNode.getAttrs().put("rate", objs[1]);
            gridList.add(gridNode);
        }
        
        StringBuffer template = new StringBuffer();
        template.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><grid version=\"2\"><declare sequence=\"true\">");
        template.append("<column name=\"name\" caption=\"页面名称\" mode=\"string\" align=\"center\"/>");
        template.append("<column name=\"rate\" caption=\"访问次数\" mode=\"string\" align=\"center\"/>");
        template.append("</declare><data></data></grid>");
        
        GridDataEncoder gEncoder = new GridDataEncoder(gridList, XMLDocUtil.dataXml2Doc(template.toString()));
        print("PageFlowRate", gEncoder);
    }
}