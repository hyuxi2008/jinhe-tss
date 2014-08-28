/* 右键菜单 */

;(function ($, factory) {

    $.Menu = factory();

})(tssJS, function () {

    'use strict';

    var
    /* 样式名称 */
    CSS_CLASS_MENU = "menu",
	CSS_CLASS_MENU_ITEM_ACITVE = "active",
	CSS_CLASS_MENU_SEPARATOR = "separator",

	/* 菜单唯一编号名前缀 */
	_UNIQUE_ID_MENU_PREFIX = "_menu_id",
	_UNIQUE_ID_ITEM_PREFIX = "_item_id",

	Menus = {
		menuZIndex: 1001,
		collection: {},
		add: function(menu) {
			Menus.collection[menu.id] = menu;
		},
		del: function(menu) {
			delete Menus.collection[menu.id];
		},
		inactiveAllMenus: function() {
			for(var menuId in this.collection) {
				Menus.collection[menuId].inactive();
			}
		},
		hideAllMenus: function() {
			for(var menuId in this.collection) {
				Menus.collection[menuId].hide();
			}
		},

		// 根据菜单项ID获取所属Menu实例
		getMenuByItemID: function(id) {
			for(var menuId in this.collection) {
				var curMenu = this.collection[menuId];
				var menuItem = curMenu.items[id];
				if(menuItem) {
					return curMenu;
				}
			}
		}
	},

	Menu = function() {
		this.items = {};
		this.parentMenuItem; //submenu所属的菜单项

		this.id = $.getUniqueID(_UNIQUE_ID_MENU_PREFIX);
		this.object = $.createElement("div", CSS_CLASS_MENU);
		this.object.id = this.id;

		this.isActive = false;
		this.setVisible(false);
		
		// 绑定事件
		this.object.onselectstart = _Menu_onSelectStart;
		$.Event.addEvent(document, "mousedown", _Menu_Document_onMouseDown);
		$.Event.addEvent(window, "resize", _Menu_Window_onResize);
		
		Menus.add(this);
	};
		
	Menu.prototype = {
			
		/*
		 *	将实例绑定到指定对象
		 *	参数：  object:srcElement       HTML对象
					string:eventName		事件名称
		 */
		attachTo: function(srcElement, eventName) {
			this.srcElement = srcElement;
			
			var thisObj = this;
			$.Event.addEvent(srcElement, eventName, function(eventObj) {
				$.Event.cancel(eventObj);

				var x = eventObj.clientX + document.body.scrollLeft;
				var y = eventObj.clientY + document.body.scrollTop;
				thisObj.show(x, y);
			});
		},

		/*
		 *	显示菜单
		 *	参数：  number:x            菜单参考点位置
					number:y            菜单参考点位置
		 */
		show: function(x, y) {
			Menus.inactiveAllMenus();
			
			var visibleItemsCount = this.refreshItems();
			if(0 == visibleItemsCount) {
				return;
			}

			this.active();

			if( $("#" + this.id).length == 0 ) {
				document.body.appendChild(this.object);
			}

			this.object.style.zIndex = Menus.menuZIndex++;

			this.moveTo(x, y);
			this.setVisible(true);
		},

		hide: function() {
			this.setVisible(false);
			this.moveTo(0, 0);
			this.inactive();
		},

		moveTo: function(x, y) {
			this.object.style.left = x + "px";
			this.object.style.top  = y + "px";
		},

		/* 激活当前菜单 */
		active: function() {
			this.isActive = true;
		},

		inactive: function() {
			this.isActive = false;
		},

		/* 不激活当前菜单的所有菜单项 */
		inactiveAllItems: function() {
			for(var key in this.items) {
				this.items[key].inactive();
			}
		},

		/*
		 *	刷新菜单项状态 
		 *	返回值：number:visibleItemsCount   可见菜单项的数量
		 */
		refreshItems: function() {
			var visibleItemsCount = 0;
			for(var item in this.items) {
				var curMenuItem = this.items[item];
				curMenuItem.refresh();
				if(curMenuItem.isVisible) {
					visibleItemsCount ++;
				}
			}
			return visibleItemsCount;
		},

		/*
		 *	设置菜单是否可见
		 *	参数：  boolean:visible     菜单是否可见
		 */
		setVisible: function(visible) {
			this.object.style.visibility = visible ? "visible" : "hidden";
		},

		/*
		 *	添加菜单项
		 *	参数：      object:menuItem     菜单项定义
		 *	返回值：    string:id     菜单项唯一ID
		 */
		addItem: function(menuItem) {
			var menuItem = new MenuItem(menuItem);
			menuItem.dockTo(this.object);

			this.items[menuItem.id] = menuItem;
			return menuItem.id;
		},

		/* 删除菜单项 */
		delItem: function(id) {
			var menuItem = this.items[id];
			if(menuItem) {
				menuItem.dispose();
				delete this.items[id];
			}
		},

		/* 添加分隔线 */
		addSeparator: function() {
			var separator = document.createElement("div");
			separator.className = CSS_CLASS_MENU_SEPARATOR;

			this.object.appendChild(separator);
		},

		/* 释放实例 */
		dispose: function() {
			for(var item in this.items) {
				this.delItem(item);
			}
			$.removeNode(this.object);

			for(var item in this) {
				delete this[item];
			}
		}
	};

	/* 控制右键菜单项 */
	var MenuItem = function(itemProperties) {
		for(var name in itemProperties) {
			this[name] = itemProperties[name];
		}

		this.isEnable = true;

		this.id = $.getUniqueID(_UNIQUE_ID_ITEM_PREFIX);

		this.object = document.createElement("div");
		this.object.id = this.id;
		this.object.noWrap = true;
		this.object.title = this.label;
		this.object.innerHTML = this.bold ? ("<b>" + this.label + "</b>") : this.label;

		if(this.icon && "" != this.icon) {
			var img = $.createElement("img");
			img.src = this.icon;
			this.object.appendChild(img);
		}
		if(this.submenu) {
			var img = $.createElement("div", "hasChild");
			this.object.appendChild(img);
			
			this.submenu.parentMenuItem = this;
		}
		
		this.object.onmouseover   = _Menu_Item_onMouseOver;
		this.object.onmouseout    = _Menu_Item_onMouseOut;
		this.object.onmousedown   = _Menu_Item_onMouseDown;
		this.object.onclick       = _Menu_Item_onClick;
		this.object.oncontextmenu = _Menu_Item_onContextMenu;
	};

	MenuItem.prototype = {

		/* 将菜单项插入指定容器  */
		dockTo: function(container) {
			container.appendChild(this.object);
		},

		/* 高亮菜单项 */
		active: function() {
			if( !!this.isEnable ) {
				this.object.className = CSS_CLASS_MENU_ITEM_ACITVE;
			}
		},

		/* 低亮菜单项 */
		inactive: function() {
			if( !!this.isEnable ) {
				this.object.className = "";
			}
			if( this.submenu ) {
				this.submenu.inactiveAllItems();
				this.submenu.hide();
			}
		},

		setVisible: function(visible) {
			this.isVisible = !!visible;
			this.object.style.display = this.isVisible ? "block" : "none";
		},

		/* 设置菜单项是否可用 */
		setEnable: function(enable) {
			this.isEnable = !!enable;
			this.object.className = this.isEnable ? "" : "disable";
		},

		/* 刷新菜单项状态 */
		refresh: function() {
			var isVisible = true;
			if(this.visible) {
				isVisible = $.execCommand(this.visible);
			}

			var isEnable = true;
			if(this.enable) {
				isEnable = $.execCommand(this.enable);
			}

			this.setVisible(isVisible);
			this.setEnable(isEnable);
		},

		/* 执行菜单项回调方法 */
		execCallBack: function(event) {
			if(this.isEnable) {
				$.execCommand(this.callback, event);
			}
		},

		/* 显示子菜单 */
		showSubMenu: function() {
			if( this.submenu ) {
				var position = $.absPosition(this.object);
				var x = position.left + this.object.offsetWidth;
				var y = position.top;
				this.submenu.show(x, y);
			}
		},

		dispose: function() {
			$.removeNode(this.object);

			for(var propertyName in this) {
				delete this[propertyName];
			}
		}
	};

	var _Menu_Document_onMouseDown = function(eventObj) {
		Menus.hideAllMenus();
	}, 

	_Menu_Window_onResize = function(eventObj) {
		Menus.hideAllMenus();
	},

	_Menu_Item_onMouseDown = function(eventObj) {
		eventObj = eventObj || window.event;
		$.Event.cancelBubble(eventObj);
	},

	// 高亮菜单项
	_Menu_Item_onMouseOver = function(eventObj) {
		eventObj = eventObj || window.event;

		var id = this.id;
		var menu = Menus.getMenuByItemID(id);
		if(menu) {
			menu.inactiveAllItems();
			var menuItem = menu.items[id];
			menuItem.active();
			menuItem.showSubMenu();
		}
	},

	// 低亮菜单项
	_Menu_Item_onMouseOut = function(eventObj) {
		var id = this.id;
		var menu = Menus.getMenuByItemID(id);
		if(menu) {
			var menuItem = menu.items[id];
			if(null == menuItem.submenu || false == menuItem.submenu.isActive) {
				menuItem.inactive();            
			}
		}
	},

	// 执行菜单项回调方法
	_Menu_Item_onClick = function(eventObj) {
		eventObj = eventObj || window.event;

		var id = this.id;
		var menu = Menus.getMenuByItemID(id);
		if(menu) {
			var menuItem = menu.items[id];
			if(menuItem.isEnable) {
				if(menuItem.callback) {
					Menus.hideAllMenus();
				}
				menuItem.execCallBack(eventObj);

				if(null == menuItem.submenu) {
					Menus.inactiveAllMenus();
					Menus.hideAllMenus();
				}
			}
		}
	},

	/* 鼠标右键点击 */
	_Menu_Item_onContextMenu = function(eventObj) {
		eventObj = eventObj || window.event;
		$.Event.cancel(eventObj);
	},

	/* 鼠标拖动选择文本 */
	_Menu_onSelectStart = function(eventObj) {
		eventObj = eventObj || window.event;
		$.Event.cancel(eventObj);
	};

	return Menu;
});