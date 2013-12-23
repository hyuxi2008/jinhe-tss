package com.jinhe.tss.um.action;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.jinhe.tss.framework.component.param.ParamConstants;
import com.jinhe.tss.framework.exception.BusinessException;
import com.jinhe.tss.framework.web.dispaly.tree.LevelTreeParser;
import com.jinhe.tss.framework.web.dispaly.tree.TreeEncoder;
import com.jinhe.tss.framework.web.dispaly.tree.TreeNodeOptionsEncoder;
import com.jinhe.tss.framework.web.dispaly.xform.XFormEncoder;
import com.jinhe.tss.framework.web.mvc.BaseActionSupport;
import com.jinhe.tss.um.UMConstants;
import com.jinhe.tss.um.entity.Role;
import com.jinhe.tss.um.entity.permission.RolePermissionsFull;
import com.jinhe.tss.um.entity.permission.RoleResources;
import com.jinhe.tss.um.permission.PermissionHelper;
import com.jinhe.tss.um.permission.PermissionService;
import com.jinhe.tss.um.permission.dispaly.IPermissionOption;
import com.jinhe.tss.um.permission.dispaly.ResourceTreeParser;
import com.jinhe.tss.um.permission.dispaly.TreeNodeOption4Permission;
import com.jinhe.tss.um.service.IRoleService;
import com.jinhe.tss.util.DateUtil;
import com.jinhe.tss.util.EasyUtils;
 
@Controller
@RequestMapping("/auth/role")
public class RoleAction extends BaseActionSupport {

	@Autowired private IRoleService roleService;
	@Autowired private PermissionService permissionService;
	
    /**
     * 获取所有的角色（不包系统级的角色）
     */
	@RequestMapping("/list")
    public void getAllRole2Tree(HttpServletResponse response) {
        List<?> roles = roleService.getAllVisiableRole();
        TreeEncoder treeEncoder = new TreeEncoder(roles, new LevelTreeParser());
        treeEncoder.setNeedRootNode(false);
        print("RoleGroupTree", treeEncoder);
    }

	/**
	 * 获取用户可见的角色组
	 */
	@RequestMapping("/groups")
	public void getAllRoleGroup2Tree(HttpServletResponse response) {
	    List<?> canAddGroups = roleService.getAddableRoleGroups();
		TreeEncoder treeEncoder = new TreeEncoder(canAddGroups, new LevelTreeParser());
		treeEncoder.setNeedRootNode(false);
		print("GroupTree", treeEncoder);
	}
	
   /**
     * 保存一个Role对象的明细信息、角色对用户信息、角色对用户组的信息
     */
	@RequestMapping(method = RequestMethod.POST)
    public void saveRole(HttpServletResponse response, HttpServletRequest request, Role role) {
        boolean isNew = (role.getId() == null);
        
        if(ParamConstants.TRUE.equals(role.getIsGroup())) {
        	roleService.saveRoleGroup(role);
        }
        else {
        	String role2UserIds  = request.getParameter("Role2UserIds");
        	String role2GroupIds = request.getParameter("Role2GroupIds");
        	roleService.saveRole2UserAndRole2Group(role, role2UserIds, role2GroupIds);
        }
        
        doAfterSave(isNew, role, "RoleGroupTree");
    }
    
    /**
     * 获得角色组信息
     */
	@RequestMapping("/group/{id}/{parentId}")
    public void getRoleGroupInfo(HttpServletResponse response, 
    		@PathVariable("id") Long id, 
    		@PathVariable("parentId") Long parentId) {
		
        XFormEncoder xFormEncoder;
        if (UMConstants.DEFAULT_NEW_ID.equals(id)) { // 如果是新增，则返回一个空的无数据的模板
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("parentId", parentId);
            map.put("isGroup", ParamConstants.TRUE);
            xFormEncoder = new XFormEncoder(UMConstants.ROLEGROUP_XFORM, map);
        }
        else {
            Role role = roleService.getRoleById(id);
            xFormEncoder = new XFormEncoder(UMConstants.ROLEGROUP_XFORM, role);
        }
        print("RoleGroupInfo", xFormEncoder);     
    }
    
    /**
     * 获取一个Role（角色）对象的明细信息、角色对用户组信息、角色对用户信息
     */
	@RequestMapping("/detail/{id}/{parentId}")
    public void getRoleInfo(HttpServletResponse response, 
    		@PathVariable("id") Long id, 
    		@PathVariable("parentId") Long parentId) {        
		
        if ( UMConstants.DEFAULT_NEW_ID.equals(id) ) { // 新建角色
            getNewRoleInfo(parentId);
        } 
        else { // 编辑角色
            getEditRoleInfo(id);
        }
    }

    private void getNewRoleInfo(Long parentId) {
        XFormEncoder roleXFormEncoder;
        TreeEncoder usersTreeEncoder;
        TreeEncoder groupsTreeEncoder;
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("parentId", parentId);
        map.put("isGroup", ParamConstants.FALSE);
        
        // 默认的有效时间
        map.put("startDate", DateUtil.format(new Date()));
        Calendar calendar = new GregorianCalendar();
        calendar.add(UMConstants.ROLE_LIFE_TYPE, UMConstants.ROLE_LIFE_TIME);
        map.put("endDate", DateUtil.format(calendar.getTime()));
        
        // 如果是新增,则返回一个空的无数据的模板
        roleXFormEncoder = new XFormEncoder(UMConstants.ROLE_XFORM, map);
       
        Map<String, Object> data = roleService.getInfo4CreateNewRole();

        usersTreeEncoder = new TreeEncoder(data.get("Role2UserTree"));
        usersTreeEncoder.setNeedRootNode(false);
    
        groupsTreeEncoder = new TreeEncoder(data.get("Role2GroupTree"), new LevelTreeParser());
        groupsTreeEncoder.setNeedRootNode(false);
        
        TreeEncoder roleToUserTree = new TreeEncoder(null);
        TreeEncoder roleToGroupTree = new TreeEncoder(null);

        print(new String[]{"RoleInfo", "Role2GroupTree", "Role2UserTree", "Role2GroupExistTree", "Role2UserExistTree"}, 
                new Object[]{roleXFormEncoder, groupsTreeEncoder, usersTreeEncoder, roleToGroupTree, roleToUserTree});
    }

    private void getEditRoleInfo(Long id) {
        Map<String, Object> data = roleService.getInfo4UpdateExistRole(id);
        
        Role role = (Role)data.get("RoleInfo");         
        XFormEncoder roleXFormEncoder = new XFormEncoder(UMConstants.ROLE_XFORM, role);
    
        TreeEncoder usersTreeEncoder = new TreeEncoder(data.get("Role2UserTree"));
        usersTreeEncoder.setNeedRootNode(false);
    
        TreeEncoder groupsTreeEncoder = new TreeEncoder(data.get("Role2GroupTree"), new LevelTreeParser());
        groupsTreeEncoder.setNeedRootNode(false);
        
        TreeEncoder roleToGroupTree = new TreeEncoder(data.get("Role2GroupExistTree"));
        TreeEncoder roleToUserTree = new TreeEncoder(data.get("Role2UserExistTree"));

        print(new String[]{"RoleInfo", "Role2GroupTree", "Role2UserTree", "Role2GroupExistTree", "Role2UserExistTree"}, 
                new Object[]{roleXFormEncoder, groupsTreeEncoder, usersTreeEncoder, roleToGroupTree, roleToUserTree});  
    }
	
	/**
	 * 删除角色
	 */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public void delete(HttpServletResponse response, @PathVariable("id") Long id) {
		roleService.delete(id);
		printSuccessMessage();
	}
	
	/**
	 * 停用/启用角色
	 */
    @RequestMapping(value = "/disable/{id}/{state}")
	public void disable(HttpServletResponse response, 
			@PathVariable("id") Long id, 
			@PathVariable("state") int state) {
    	
		roleService.disable(id, state);
        printSuccessMessage();
	}
 
	/**
	 * 移动
	 */
    @RequestMapping(value = "/move/{id}/{toGroupId}", method = RequestMethod.POST)
	public void move(HttpServletResponse response, 
			@PathVariable("id") Long id, 
			@PathVariable("toGroupId") Long toGroupId) {
    	
		roleService.move(id, toGroupId);        
        printSuccessMessage();
	}
	
	@RequestMapping("/operations/{id}")
	public void getOperation(HttpServletResponse response, @PathVariable("id") Long id) {
        // 角色（组）树上： 匿名角色节点只需一个“角色权限设置”菜单即可
        if(id.equals(UMConstants.ANONYMOUS_ROLE_ID)) {
        	print("Operation", "p1,p2," + UMConstants.ROLE_EDIT_OPERRATION);
        }
        else {
        	List<?> list = PermissionHelper.getInstance().getOperationsByResource(id, 
        			RolePermissionsFull.class.getName(), RoleResources.class);
        	print("Operation", "p1,p2," + EasyUtils.list2Str(list));
        }
	}
	
	/**
	 * 查询应用系统列表，以便挑出资源进行授权
	 */
	@RequestMapping("/apps")
	public void getApplications(HttpServletResponse response, 
			@PathVariable("roleId") Long roleId, 
			@PathVariable("isRole2Resource") Integer isRole2Resource) {
		
		List<?> apps = roleService.getPlatformApplication();
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("roleId", roleId);
		map.put("isRole2Resource", isRole2Resource);
		map.put("permissionRank", UMConstants.LOWER_PERMISSION);
		map.put("applicationId", UMConstants.TSS_APPLICATION_ID);
		map.put("resourceType", UMConstants.GROUP_RESOURCE_TYPE_ID);
		
		List<?> types = roleService.getResourceTypeByAppId(UMConstants.TSS_APPLICATION_ID);
		String[] resourceTypeEditor = EasyUtils.generateComboedit(types, "resourceTypeId", "name", "|");

		XFormEncoder xFormEncoder = new XFormEncoder(UMConstants.SERACH_PERMISSION_XFORM, map);
		String[] appEditor = EasyUtils.generateComboedit(apps, "applicationId", "name", "|");
		xFormEncoder.setColumnAttribute("applicationId", "editorvalue", appEditor[0]);
		xFormEncoder.setColumnAttribute("applicationId", "editortext",  appEditor[1]);
		xFormEncoder.setColumnAttribute("resourceType", "editorvalue", resourceTypeEditor[0]);
		xFormEncoder.setColumnAttribute("resourceType", "editortext",  resourceTypeEditor[1]);

		print("SearchPermissionFrom", xFormEncoder);
	}
	
	/**
	 * 根据应用获得资源类型。 做 应用系统/资源类型/授权级别 三级下拉框时用
	 */
	@RequestMapping("/resourceTypes/{applicationId}")
	public void getResourceTypes(HttpServletResponse response, @PathVariable("applicationId") String applicationId) {
		List<?> types = roleService.getResourceTypeByAppId(applicationId);
		String[] resourceTypeEditor = EasyUtils.generateComboedit(types, "resourceTypeId", "name", "|");
		
		StringBuffer sb = new StringBuffer();
        sb.append("<column name=\"resourceType\" caption=\"资源类型\" mode=\"string\" editor=\"comboedit\" ");
        sb.append(" editorvalue=\"").append(resourceTypeEditor[0]).append("\" ");
        sb.append(" editortext=\"").append(resourceTypeEditor[1]).append("\"/>");

		print("ResourceTypeList", sb);
	}

	@RequestMapping("/permission/initsearch/{isRole2Resource}/{roleId}")
	public void initSetPermission(HttpServletResponse response, HttpServletRequest request, 
			@PathVariable("isRole2Resource") Integer isRole2Resource, 
			@PathVariable("roleId") Long roleId) {
		
		if( ParamConstants.TRUE.equals(isRole2Resource) ) {
			getApplications(response, roleId, isRole2Resource);
			return;
		}
		
		String applicationId = request.getParameter("applicationId");
    	String resourceType  = request.getParameter("resourceType");
    	applicationId = applicationId == null ? PermissionHelper.getApplicationID() : applicationId;
    	
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("roleId", roleId);
		map.put("isRole2Resource", isRole2Resource);
		map.put("applicationId", applicationId);
		map.put("resourceType", resourceType);
		map.put("permissionRank", UMConstants.LOWER_PERMISSION);

		XFormEncoder xFormEncoder = new XFormEncoder(UMConstants.SERACH_PERMISSION_XFORM, map);
		print("SearchPermissionFrom", xFormEncoder);
	}
	
	// ===========================================================================
	// 授权相关方法
	// ===========================================================================	
	
	/**
	 * 获取授权用的矩阵图
	 */
	@RequestMapping("/permission/matrix/{permissionRank}/{isRole2Resource}/{roleId}")
	public void getPermissionMatrix(HttpServletResponse response, HttpServletRequest request,  
			@PathVariable("permissionRank") String permissionRank, 
			@PathVariable("isRole2Resource") Integer isRole2Resource, 
			@PathVariable("roleId") Long roleId) {  
		
	    if( EasyUtils.isNullOrEmpty(permissionRank) ){
            throw new BusinessException("请选择授权级别");
        }
	    
	    List<Long[]> roleUsers = roleService.getRoles4Permission();
	    Object[] matrixInfo;
	    
	    String applicationId = request.getParameter("applicationId");
    	String resourceType  = request.getParameter("resourceType");
	    
	    //  角色对资源授权（“角色维护”菜单，多个资源授权给单个角色）时，生成 资源－操作选项 矩阵
	    if( ParamConstants.TRUE.equals(isRole2Resource) ) {
            if( EasyUtils.isNullOrEmpty(applicationId) ){
                throw new BusinessException("请选择应用系统");
            }
            if( EasyUtils.isNullOrEmpty(resourceType) ){
                throw new BusinessException("请选择资源类型");
            }
            
            permissionService = PermissionHelper.getPermissionService(applicationId, permissionService);
            matrixInfo = permissionService.genResource2OperationMatrix(applicationId, resourceType, 
                    roleId, permissionRank, roleUsers);
        } 
        // 资源对角色授权（“资源授予角色”菜单，单个资源授权给多个角色）时，生成 角色－操作选项 矩阵。
        else {
            if( applicationId == null ) {
                applicationId = PermissionHelper.getApplicationID();
            }
            
            permissionService = PermissionHelper.getPermissionService(applicationId, permissionService);
            matrixInfo = permissionService.genRole2OperationMatrix(applicationId, resourceType, 
                    roleId, permissionRank, roleUsers); // 此时roleId其实是资源ID（resourceId）
        }
        
        TreeNodeOptionsEncoder treeNodeOptionsEncoder = new TreeNodeOptionsEncoder();
        List<?> operations = (List<?>) matrixInfo[1];
        if(null != operations){
            for ( Object temp : operations ) {
                treeNodeOptionsEncoder.add(new TreeNodeOption4Permission((IPermissionOption) temp));
            }
        }       
        
        TreeEncoder treeEncoder = new TreeEncoder(matrixInfo[0], new ResourceTreeParser());
        treeEncoder.setOptionsEncoder(treeNodeOptionsEncoder);
        treeEncoder.setNeedRootNode(false);
        
        print("PermissionMatrix", treeEncoder);
	}
	
	/**
	 * permissionRank  授权级别(1:普通(10)，2/3:可授权，可授权可传递(11))
	 * permissions   角色资源权限选项的集合, 当资源对角色授权时:  role1|2224,role2|4022
	 */
	@RequestMapping(value = "/permission/{permissionRank}/{isRole2Resource}/{roleId}", method = RequestMethod.POST)
	public void savePermission(HttpServletResponse response, HttpServletRequest request,  
			@PathVariable("permissionRank") String permissionRank, 
			@PathVariable("isRole2Resource") Integer isRole2Resource, 
			@PathVariable("roleId") Long roleId) {  
		
		String applicationId = request.getParameter("applicationId");
    	String resourceType  = request.getParameter("resourceType");
    	String permissions   = request.getParameter("permissions");
    	
	    if( applicationId == null ) {
            applicationId = PermissionHelper.getApplicationID();
        }
	    
        permissionService = PermissionHelper.getPermissionService(applicationId, permissionService);
        
	    // 角色对资源授权（“角色维护”菜单，多个资源授权给单个角色）
        if( ParamConstants.TRUE.equals(isRole2Resource) ) {
            permissionService.saveResources2Role(applicationId, resourceType, roleId, permissionRank, permissions);
        } 
        // 资源对角色授权（“资源授予角色”菜单，单个资源授权给多个角色）
        else {
            permissionService.saveResource2Roles(applicationId, resourceType, roleId, permissionRank, permissions);
        }
        
        printSuccessMessage();
	}
}
