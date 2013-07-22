package com.jinhe.tss.portal.action;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;

import com.jinhe.tss.framework.web.dispaly.tree.ITreeTranslator;
import com.jinhe.tss.framework.web.dispaly.tree.TreeEncoder;
import com.jinhe.tss.framework.web.dispaly.tree.TreeNode;
import com.jinhe.tss.framework.web.dispaly.xform.IXForm;
import com.jinhe.tss.framework.web.dispaly.xform.XFormEncoder;
import com.jinhe.tss.framework.web.mvc.BaseActionSupport;
import com.jinhe.tss.portal.PortalConstants;
import com.jinhe.tss.portal.entity.ElementGroup;
import com.jinhe.tss.portal.entity.Layout;
import com.jinhe.tss.portal.helper.ElementHelper;
import com.jinhe.tss.portal.helper.ElementTreeParser;
import com.jinhe.tss.portal.helper.IElement;
import com.jinhe.tss.portal.service.IElementService;
import com.jinhe.tss.util.URLUtil;
import com.jinhe.tss.util.XMLDocUtil;
import com.jinhe.tss.util.XmlUtil;

public class LayoutAction extends BaseActionSupport {
 
    @Autowired private IElementService service;

    /**
     * 布局器树型展示.
     */
    public void getAllLayout4Tree() {
        List<?> data = service.getAllElementsAndGroups(ElementGroup.LAYOUT_TYPE);
        TreeEncoder encoder = new TreeEncoder(data, new ElementTreeParser());    
        print("LayoutTree", encoder);
    }

    /**
     * 所有启动的布局器树型展示.
     */
    public void getAllStartLayout4Tree() {
        List<?> data = service.getAllStartElementsAndGroups(ElementGroup.LAYOUT_TYPE);
        TreeEncoder encoder = new TreeEncoder(data, new ElementTreeParser());      
        encoder.setNeedRootNode(false);

        encoder.setTranslator( new ITreeTranslator() {
            public Map<String, Object> translate(Map<String, Object> attributes) {
                if(attributes.get("type") != null) { // 使得组节点不可选
                    attributes.put(TreeNode.TREENODE_ATTRIBUTE_CANSELECTED, "0"); 
                }
                return attributes;
            }           
        });
        
        print("LayoutTree", encoder);
    }
    
    /**
     * 获取布局器参数,并拼装成一个xml返回
     */
    public void getDefaultParams4Xml(Long id) {
        Layout layout = (Layout) service.getElementInfo(ElementGroup.LAYOUT_CLASS, id);
        
        StringBuffer sb = new StringBuffer("<layout ");
        List<?> parameters = XMLDocUtil.dataXml2Doc(layout.getDefinition()).selectNodes("//layout/parameters/param");
        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                Element param = (Element) parameters.get(i);
                String name = param.attributeValue("name");
                String defaultValue = param.attributeValue("defaultValue");
                sb.append(name).append("=\"").append(XmlUtil.toFormXml(defaultValue)).append("\" ");
            }
        }
        sb.append(">").append("model/" + Layout.LAYOUT_NAME + "/" + layout.getCode() + layout.getId() + "/paramsXForm.xml");                    

        print("LayoutParameters", sb.append("</layout>").toString());
    }
    
    /**
     * 布局器详细信息.
     */
    public void getLayoutInfo(Long id, Long groupId) {
        XFormEncoder encoder ;
        if ( DEFAULT_NEW_ID.equals(id) ) { // 如果是新增,则返回一个空的无数据的模板
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("groupId", groupId);          
            encoder = new XFormEncoder(PortalConstants.LAYOUT_XFORM_TEMPLET_PATH, map);
        } else {
            Layout layout = (Layout) service.getElementInfo(ElementGroup.LAYOUT_CLASS, id);
            encoder = new XFormEncoder(PortalConstants.LAYOUT_XFORM_TEMPLET_PATH, (IXForm) layout);
        }
        print("LayoutInfo", encoder);
    }

    /**
     * 新增布局器.
     */
    public void save(Layout layout) {       
        boolean isNew = (layout.getId() == null);
        layout = (Layout) service.saveElement(layout);          
        doAfterSave(isNew, layout, "LayoutTree");
    }

    /**
     * 删除布局器.
     */
    public void delete(Long id) {
        service.deleteElement(ElementGroup.LAYOUT_CLASS, id);
        printSuccessMessage("删除布局器成功");
    }

    /**
     * 停用/启用 布局器（将其下的disabled属性设为"1"/"0"）
     */
    public void disabled(Long id, int state) {
        service.disableElement(ElementGroup.LAYOUT_CLASS, id, state);
        printSuccessMessage();
    }

    /**
     * 布局器排序
     */
    public void sort(Long id, Long targetId, int direction) {
        service.sortElement(id, targetId, direction, ElementGroup.LAYOUT_CLASS);
        printSuccessMessage();
    }

    /**
     * 复制布局器
     * @return
     */
    public void copy(Long id) {       
        String desDir = URLUtil.getWebFileUrl(PortalConstants.LAYOUT_MODEL_DIR).getPath(); 
        IElement copy = service.copyElement(id, new File(desDir), ElementGroup.LAYOUT_CLASS);    
        doAfterSave(true, copy, "LayoutTree");
    }
    
    /**
     * 设置布局器为默认布局器
     */      
    public void setAsDefault(Long id) {      
        service.setLayout4Default(id);
        printSuccessMessage();
    }
    
    /**
     * 获取导入布局器的模板
     */
    public void getUploadTemplate() {
        XFormEncoder encoder = new XFormEncoder(PortalConstants.IMPORT_LAYOUT_XFORM_PATH);
        print("LayoutInfo", encoder);
    }

    /**
     * 导入布局器
     */
    public void importLayout(Long groupId, File file) {
        String desDir = URLUtil.getWebFileUrl(PortalConstants.LAYOUT_MODEL_DIR).getPath(); 
        
        Layout layout = new Layout();
        layout.setGroupId(groupId);
        ElementHelper.importElement(service, file, layout, desDir, "layout.xml");
        
        print("script", "parent.loadInitData();alert(\"导入成功!!!\");var ws = parent.$(\"ws\");ws.closeActiveTab();");
    }

    /**
     * 布局器的导出
     */
    public void getExportLayout(Long id) {      
        String desDir = URLUtil.getWebFileUrl(PortalConstants.LAYOUT_MODEL_DIR).getPath(); 
        IElement info = service.getElementInfo(ElementGroup.LAYOUT_CLASS, id);

        ElementHelper.exportElement(desDir, info, "layout.xml");
    }
}
