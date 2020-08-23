	/*
     *	后台响应数据节点名称
     */
    XML_MAIN_TREE = "RoleGroupTree";
    XML_ROLE_INFO = "RoleInfo";
    XML_ROLE_TO_GROUP_TREE = "Role2GroupTree";
    XML_ROLE_TO_GROUP_EXIST_TREE = "Role2GroupExistTree";
    XML_ROLE_TO_USER_TREE = "Role2UserTree";
    XML_ROLE_TO_USER_EXIST_TREE = "Role2UserExistTree";
    XML_ROLE_TO_PERMISSION = "Role2Permission";
    XML_ROLE_LIST = "RoleList";
    XML_ROLE_GROUP_INFO = "RoleGroupInfo";
    XML_GROUP_TO_USER_LIST_TREE = "Group2UserListTree";
    XML_ROLE_TO_GROUP_IDS = "Role2GroupIds";
    XML_ROLE_TO_USER_IDS = "Role2UserIds";
	XML_SEARCH_ROLE_USERS = "ROlE_USERS_RESULT";

    /*
     *	默认唯一编号名前缀
     */
    CACHE_ROLE_GROUP_DETAIL = "_roleGroup_id";
    CACHE_VIEW_ROLE_GROUP_DETAIL = "_viewRoleGroup_id";
    CACHE_ROLE_DETAIL = "_role_id";
    CACHE_VIEW_ROLE_DETAIL = "_viewRole_id";
 
    /*
     *	XMLHTTP请求地址汇总
     */
    URL_SOURCE_TREE		  = AUTH_PATH + "role/list";
    URL_ROLE_DETAIL		  = AUTH_PATH + "role/detail/"; // {id}/{parentId}
	URL_ROLE_GROUP_DETAIL = AUTH_PATH + "role/group/"; // {id}/{parentId}
    URL_SAVE_ROLE		  = AUTH_PATH + "role";
	URL_SAVE_ROLE_GROUP   = AUTH_PATH + "role"; 
    URL_STOP_NODE		  = AUTH_PATH + "role/disable/";  // {id}/{state}
    URL_DELETE_NODE		  = AUTH_PATH + "role/";  // {id}
    URL_SORT_NODE         = AUTH_PATH + "role/sort/";
	URL_MOVE_NODE		  = AUTH_PATH + "role/move/"; // {id}/{toGroupId}
    URL_GROUP_USERS       = AUTH_PATH + "group/users/";  // {groupId}
    URL_GET_OPERATION     = AUTH_PATH + "role/operations/";  // {id}

    URL_SEARCH_ROLE_USERS = AUTH_PATH + "search/role/users/";
    URL_POPUP_TREE = AUTH_PATH + "role/groups";
	
	if(IS_TEST) {
	    URL_SOURCE_TREE       = "data/role_tree.xml?";
		URL_ROLE_DETAIL       = "data/role_detail.xml?";
		URL_SAVE_ROLE         = "data/_success.xml?";
		URL_ROLE_GROUP_DETAIL = "data/rolegroup_detail.xml?";
		URL_SAVE_ROLE_GROUP   = "data/_success.xml?";
		URL_STOP_NODE         = "data/_success.xml?";
		URL_DELETE_NODE       = "data/_success.xml?";
		URL_GROUP_USERS       = "data/userlist.xml?";
		URL_SORT_NODE         = "data/_success.xml?";
		URL_MOVE_NODE         = "data/_success.xml?";
		URL_GET_OPERATION     = "data/operation.xml?";

		URL_POPUP_TREE = "data/rolegroup_tree.xml";
	}
 
    function init() {
        initMenus();
        initWorkSpace(false);
        initEvents();

        loadInitData();

        bindTreeSearch("sk1", "page2Tree");
        bindTreeSearch("sk2", "page4Tree");
        bindTreeSearch("sk3", "page4Tree2");
    }

    function initMenus() {
        var item1 = {
            label:"新建角色组",
            callback:addNewRoleGroup,           
            visible:function() {return (isRoleGroup() || isRootNode()) && getOperation("2");}
        }
        var item2 = {
            label:"删除",
            callback: function() { delTreeNode(URL_DELETE_NODE); },
            icon:"icon icon-x",
            visible:function() {return !isRootNode() && !isSysRole() && getOperation("2");}
        }
        var item3 = {
            label:"编辑",
            callback:function() { editTreeNode(true); },
            icon:"icon icon-pencil",          
            visible:function() {return !isRootNode() && !isSysRole() && getOperation("2");}
        }
        var item7 = {
            label:"停用",
            callback:function() { stopOrStartTreeNode("1"); },
            icon:"icon icon-triangle-down",           
            visible:function() {return !isRootNode() && !isSysRole() && !isTreeNodeDisabled() && getOperation("2");}
        }
        var item8 = {
            label:"启用",
            callback:function() { stopOrStartTreeNode("0"); },
            icon:"icon icon-triangle-up",        
            visible:function() {return !isRootNode() && !isSysRole() && isTreeNodeDisabled() && getOperation("2");}
        }
        var item9 = {
            label:"新建角色",
            callback:addNewRole,  
            icon:"icon icon-plus",         
            visible:function() {return (isRoleGroup() || isRootNode()) && getOperation("2");}
        }
        var item11 = {
            label:"移动到...",
            callback:moveNodeTo,
            icon:"icon icon-arrow-left",            
            visible:function() {return !isRootNode() && !isSysRole() && getOperation("2");}
        }
        var item10 = {
            label:"给角色授权",
            callback:setRolePermission,           
            visible:function() {return isRole() && getOperation("2");}
        }
        var item12 = {
            label:"授予角色",
            icon:"icon icon-organization",
            callback:function() { 
                setRole2Permission("2"); 
            },        
            visible:function() {return !isRootNode() && !isSysRole() && getOperation("2");}
        }
        var item13 = {
            label:"已授用户列表",
            callback:generalSearchRoleUsers,
            visible:function() {return isRole() && getOperation("2");}
        }

        var menu1 = new $.Menu();
		menu1.addItem(item1);
        menu1.addItem(item9);
        menu1.addItem(item3);
        menu1.addItem(item7);
        menu1.addItem(item8);
		menu1.addItem(item11);
        menu1.addItem(item2);
        menu1.addSeparator();    
		menu1.addItem(item10); 
        menu1.addItem(item12); 
        menu1.addItem(item13);

        $1("tree").contextmenu = menu1;
    }

	function loadInitData() {
        var onresult = function() {
            var roleTreeNode = this.getNodeValue(XML_MAIN_TREE);
			var tree = $.T("tree", roleTreeNode);

            tree.onTreeNodeDoubleClick = function(ev) {
				var treeNode = getActiveTreeNode();
				getTreeOperation(treeNode, function(_operation) {            
					if( !isRootNode() && !isSysRole()) {
						var canEdit = checkOperation("2", _operation);
						editTreeNode(canEdit);
					}
				});
            }
            tree.onTreeNodeMoved = function(ev){ sortTreeNode(URL_SORT_NODE, ev); }
            tree.onTreeNodeRightClick = function(ev) { onTreeNodeRightClick(ev, true); }
        }
		
		$.ajax({url: URL_SOURCE_TREE, onresult: onresult});
    }
	
	/* 是否根节点 */
    function isRootNode() {
        return ("-6" == getTreeNodeId());
    }

    function isSysRole() {
    	var roleId = getTreeNodeId();
        return "-10000" == roleId || "-8" == roleId || "-9" == roleId;
    }
	
	/* 获取节点类型(1角色组/0角色) */
	function isRole() {
		return getTreeAttribute("isGroup") == "0";
	}
	function isRoleGroup() {
		return getTreeAttribute("isGroup") == "1";
	}
 
	function moveNodeTo() {
		var tree = $.T("tree");
        var treeNode = tree.getActiveTreeNode();
		var id  = treeNode.id;
        var pId = treeNode.parent.id;

        var params = {id:id, parentID: pId};
		popupTree(URL_POPUP_TREE, "GroupTree", params, function(target) {
            moveTreeNode(tree, id, target.id);
        });
    }
  
    function editTreeNode(editable) {
        if( isRoleGroup() ) {
            editRoleGroupInfo(editable);
        } else {
            editRoleInfo(editable);
        }
    }

	function addNewRoleGroup() {
        var treeNode = $.T("tree").getActiveTreeNode();
		var parentID = treeNode.id;
		var treeName = "角色组";
		var treeID = DEFAULT_NEW_ID;

		var callback = {};
		callback.onTabChange = function() {
			setTimeout(function() {
				loadRoleGroupDetailData(treeID, true, parentID);
			}, TIMEOUT_TAB_CHANGE);
		};

		var inf = {};
		inf.defaultPage = "page1";
		inf.label = OPERATION_ADD.replace(/\$label/i, treeName);
		inf.callback = callback;
		inf.SID = CACHE_ROLE_GROUP_DETAIL + treeID;
		ws.open(inf);
    }
	
    function editRoleGroupInfo(editable) {
        var treeNode = $.T("tree").getActiveTreeNode();
		var treeID = treeNode.id;
		var treeName = treeNode.name;
		var parentID = treeNode.getAttribute("roleGroupId") || treeNode.parent.id;

		var callback = {};
		callback.onTabChange = function() {
			setTimeout(function() {
				loadRoleGroupDetailData(treeID, editable, parentID);
			}, TIMEOUT_TAB_CHANGE);
		};

		var inf = {};
		if( editable ) {
			inf.label = OPERATION_EDIT.replace(/\$label/i, treeName);
			inf.SID = CACHE_ROLE_GROUP_DETAIL + treeID;
		}else{
			inf.label = OPERATION_VIEW.replace(/\$label/i, treeName);
			inf.SID = CACHE_VIEW_ROLE_GROUP_DETAIL + treeID;
		}
		inf.defaultPage = "page1";
		inf.phases = null;
		inf.callback = callback;
		var tab = ws.open(inf);
    }
	
    /*
     *	树节点数据详细信息加载数据
     *	参数：	string:treeID               树节点id
                boolean:editable            是否可编辑(默认true)
                string:parentID             父节点id
     */
    function loadRoleGroupDetailData(treeID, editable, parentID) {
		$.ajax({
			url : URL_ROLE_GROUP_DETAIL + treeID + "/" + parentID,
			onresult : function() {		
				var roleGroupInfoNode = this.getNodeValue(XML_ROLE_GROUP_INFO);

				var roleGroupInfoNodeID = treeID + "." + XML_ROLE_GROUP_INFO;
				$.cache.XmlDatas[roleGroupInfoNodeID] = roleGroupInfoNode;

				var xform = $.F("page1Form", roleGroupInfoNode);
				xform.editable = editable == false ? "false" : "true";
				
				// 设置翻页按钮显示状态
				$("#page1BtNext").hide();

				//设置保存按钮操作
				var page1BtSaveObj = $1("page1BtSave");
				var page2BtSaveObj = $1("page2BtSave");
				page1BtSaveObj.disabled = page2BtSaveObj.disabled = (editable==false ? true : false)
				page1BtSaveObj.onclick = page2BtSaveObj.onclick = function() {
					saveRoleGroup(treeID, parentID);
				}
			}
		});
    }

    function saveRoleGroup(treeID, parentID) {
		var xform = $.F("page1Form");	
		if( !xform.checkForm() ) return;

		var request = new $.HttpRequest();
        request.url = URL_SAVE_ROLE_GROUP;
 
		// 角色组基本信息
		var roleGroupInfoNode = $.cache.XmlDatas[treeID + "." + XML_ROLE_GROUP_INFO];
		var dataNode = roleGroupInfoNode.querySelector("data");
		request.setFormContent(dataNode);
 
		// 同步按钮状态
		syncButton([$1("page1BtSave"), $1("page2BtSave")], request);

		request.onresult = function() {
			afterSaveTreeNode.call(this, treeID, parentID);
		}
		request.onsuccess = function() {
			afterSaveTreeNode(treeID, xform);
		}
		request.send();
    }
	
	
	var phases = [];
	phases[0] = {page:"page1", label:"基本信息"};
	phases[1] = {page:"page4", label:"授予用户"};
	phases[2] = {page:"page2", label:"授予用户组"};
	
	function addNewRole() {
        var treeNode = $.T("tree").getActiveTreeNode();
		var parentID = treeNode.id;
		var treeName = "角色";
		var treeID = DEFAULT_NEW_ID;

		var callback = {};
		callback.onTabChange = function() {
			setTimeout(function() {
				loadRoleDetailData(treeID, true, parentID);
			}, TIMEOUT_TAB_CHANGE);
		};

		var inf = {};
		inf.defaultPage = "page1";
		inf.label = OPERATION_ADD.replace(/\$label/i, treeName);
		inf.phases = phases;
		inf.callback = callback;
		inf.SID = CACHE_ROLE_DETAIL + treeID;
		var tab = ws.open(inf);
    }

    function editRoleInfo(editable) {
        var treeNode = $.T("tree").getActiveTreeNode();
		var treeID = treeNode.id;
		var treeName = treeNode.name;
		var parentID = treeNode.parent.id;
 
		var callback = {};
		callback.onTabChange = function() {
			setTimeout(function() {
				loadRoleDetailData(treeID, editable, treeID);
			}, TIMEOUT_TAB_CHANGE);
		};

		var inf = {};
		if( editable ) {
			inf.label = OPERATION_EDIT.replace(/\$label/i, treeName);
			inf.SID = CACHE_ROLE_DETAIL + treeID;
		}else{
			inf.label = OPERATION_VIEW.replace(/\$label/i, treeName);
			inf.SID = CACHE_VIEW_ROLE_DETAIL + treeID;
		}
		inf.defaultPage = "page1";
		inf.phases = phases;
		inf.callback = callback;
		ws.open(inf);
    }
	
    /*
     *	树节点数据详细信息加载数据
     *	参数：	string:treeID               树节点id
                boolean:editable            是否可编辑(默认true)
                string:parentID             父节点id
     */
    function loadRoleDetailData(treeID, editable, parentID) {
		var onresult = function() {
			var roleInfoNode = this.getNodeValue(XML_ROLE_INFO);
			var role2UserTreeNode   = this.getNodeValue(XML_ROLE_TO_GROUP_TREE);
			var role2UserExsitInfo  = this.getNodeValue(XML_ROLE_TO_USER_EXIST_TREE);
			var role2GroupTreeNode  = this.getNodeValue(XML_ROLE_TO_GROUP_TREE);
			var role2GroupExsitInfo = this.getNodeValue(XML_ROLE_TO_GROUP_EXIST_TREE);
 
			disableTreeNodes(role2GroupTreeNode, "treeNode[id='-2']");
			disableTreeNodes(role2GroupTreeNode, "treeNode[id='-3']");

			var roleInfoNodeID  = treeID + "." + XML_ROLE_INFO;
			$.cache.XmlDatas[roleInfoNodeID] = roleInfoNode;

			var page1Form = $.F("page1Form", roleInfoNode);
            page1Form.editable = editable ? "true" : "false";

			var page4Tree  = $.T("page4Tree",  role2UserTreeNode);
			var page4Tree3 = $.T("page4Tree3", role2UserExsitInfo);
			var page2Tree  = $.T("page2Tree",  role2GroupTreeNode);
			var page2Tree2 = $.T("page2Tree2", role2GroupExsitInfo);
					
			page4Tree.onTreeNodeDoubleClick = function(ev) {
                var treeNode = page4Tree.getActiveTreeNode();
				$.ajax({
					url : URL_GROUP_USERS + treeNode.id,
					onresult : function() { 
						var sourceListNode = this.getNodeValue(XML_GROUP_TO_USER_LIST_TREE);
						$.T("page4Tree2", sourceListNode);
					}
				});	
            }
			
			// 设置翻页按钮显示状态
			$("#page4BtPrev").show();
			$("#page2BtPrev").show();
			$("#page1BtNext").show();
			$("#page4BtNext").show();

			var disabled = (editable == false);
			
			// 设置按钮操作
			$1("page2BtAdd").onclick = function() {
				addTreeNode(page2Tree, page2Tree2);
			}
			$1("page2BtDel").onclick = function() {
				removeTreeNode($.T("page2Tree2")); // 删除page2里tree节点
			}
			$1("page4BtAdd").onclick = function() {
				addTreeNode($.T("page4Tree2"), page4Tree3);
			}
			$1("page4BtDel").onclick = function() {
				removeTreeNode($.T("page4Tree3")); // 删除page4里tree节点
			}

			// 设置保存按钮操作
			var page1BtSaveObj = $1("page1BtSave");
			var page2BtSaveObj = $1("page2BtSave");
			var page4BtSaveObj = $1("page4BtSave");
			page1BtSaveObj.disabled = page2BtSaveObj.disabled = page4BtSaveObj.disabled = disabled;
			page1BtSaveObj.onclick = page2BtSaveObj.onclick = page4BtSaveObj.onclick = function() {
				saveRole(treeID, parentID);
			}
		}

		$.ajax({url: URL_ROLE_DETAIL + treeID + "/" + parentID, onresult: onresult});
    }
 
    function saveRole(treeID, parentID) {
        // 校验page1Form数据有效性
        var page1Form = $.F("page1Form");
        if( !page1Form.checkForm() ) {
            ws.switchToPhase("page1");
            return;
        }

        var request = new $.HttpRequest();
        request.url = URL_SAVE_ROLE;
 
		// 角色基本信息
		var roleInfoNode = $.cache.XmlDatas[treeID + "." + XML_ROLE_INFO];
		var roleInfoDataNode = roleInfoNode.querySelector("data");
		request.setFormContent(roleInfoDataNode);

		// 角色对用户
		var role2UserIDs = $.T("page4Tree3").getAllNodeIds();
		request.addParam(XML_ROLE_TO_USER_IDS, role2UserIDs.join(","));

		// 角色对用户组
		var role2GroupIDs = $.T("page2Tree2").getAllNodeIds();
		request.addParam(XML_ROLE_TO_GROUP_IDS, role2GroupIDs.join(","));

        // 同步按钮状态
        syncButton([$1("page1BtSave"), $1("page2BtSave"), $1("page4BtSave")], request);

        request.onresult = function() {                   
			afterSaveTreeNode.call(this, treeID, parentID);
        }
        request.onsuccess = function() {                  
            afterSaveTreeNode(treeID, page1Form);
        }
        request.send();
    }		

    /* 角色权限设置 */
    function setRolePermission() {
        var treeNode = getActiveTreeNode();	
        globalValiable = {};	
        globalValiable.roleId = treeNode.id;
        globalValiable.isRole2Resource = "1";
        var title = "设置角色【" + treeNode.name + "】对资源的权限";

        $.openIframePanel("permissionPanel", title, 850, 520, "../um/setpermission.html");
    	$("#permissionPanel").find("h2").html(title);
    }

    /* 综合查询(所有拥有指定角色的用户列表) */
    function generalSearchRoleUsers() {
        var treeNode = getActiveTreeNode();
        var title = "拥有角色【" + treeNode.name +"】的用户列表";
        var url = URL_SEARCH_ROLE_USERS + treeNode.id;
        popupGrid(url, XML_SEARCH_ROLE_USERS, title);
    }

	window.onload = init;