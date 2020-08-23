    /*
     *	后台响应数据节点名称
     */
    XML_DEFAULT_TOOLBAR = "DefaultToolBar";
    XML_MAIN_TREE = "RuleTree";
    XML_RULE_INFO = "RuleInfo";
    XML_RULE_TO_ROLE_TREE = "Rule2RoleTree";
    XML_RULE_TO_ROLE_EXIST_TREE = "Rule2RoleExistTree";
    XML_RULE_TO_GROUP_TREE = "Rule2GroupTree";
    XML_RULE_TO_GROUP_EXIST_TREE = "Rule2GroupExistTree";
    XML_RULE_TO_USER_TREE = "Rule2UserTree";
    XML_RULE_TO_USER_EXIST_TREE = "Rule2UserExistTree";
 
    XML_GROUP_TO_USER_LIST_TREE = "Group2UserListTree";
    XML_RULE_TO_GROUP_IDS = "Rule2GroupIds";
    XML_RULE_TO_USER_IDS = "Rule2UserIds";
    XML_RULE_TO_ROLE_IDS = "Rule2RoleIds";

    /* 默认唯一编号名前缀 */
    CACHE_RULE_DETAIL = "_rule_id";
	
    /* XMLHTTP请求地址汇总*/
	URL_INIT        = AUTH_PATH + "subauthorize/list"; 
	URL_RULE_DETAIL = AUTH_PATH + "subauthorize/detail/"; 
	URL_GROUP_USERS = AUTH_PATH + "group/users/";  // {groupId}
	URL_SAVE_RULE   = AUTH_PATH + "subauthorize"; 
	URL_STOP_RULE   = AUTH_PATH + "subauthorize/disable/"; 
	URL_DEL_RULE    = AUTH_PATH + "subauthorize/"; 
	
	if(IS_TEST) {
		URL_INIT        = "data/subauth_tree.xml?";
		URL_RULE_DETAIL = "data/subauth_detail.xml?";
		URL_GROUP_USERS = "data/subauth_users.xml?";
		URL_SAVE_RULE   = "data/_success.xml?";
		URL_STOP_RULE   = "data/_success.xml?";
		URL_DEL_RULE    = "data/_success.xml?";
	}

    function init(){
        initMenus();
        initWorkSpace(false);
        initEvents();

        loadInitData();

        bindTreeSearch("sk1", "page2Tree");
        bindTreeSearch("sk2", "page3Tree");
        bindTreeSearch("sk3", "page4Tree");
        bindTreeSearch("sk4", "page4Tree2");
    }
  
    function initMenus() {  
        var item1 = {
            label:"停用",
            callback: function() { stopOrStartTreeNode("1", URL_STOP_RULE); },
            icon:"icon icon-triangle-down",
            visible:function(){return !isTreeRoot() && !isTreeNodeDisabled();}
        }
        var item2 = {
            label:"启用",
            callback: function() { stopOrStartTreeNode("0", URL_STOP_RULE); },
            icon:"icon icon-triangle-up",
            visible:function(){return !isTreeRoot() && isTreeNodeDisabled();}
        }
        var item3 = {
            label:"新建转授策略",
            callback:addNewRule,
            icon:"icon icon-plus",
            enable:function(){return true;},
            visible:function(){return isTreeRoot();}
        }
        var item4 = {
            label:"删除",
            callback: function() {  delTreeNode(URL_DEL_RULE); },
            icon:"icon icon-x",
            visible:function(){return !isTreeRoot();}
        }
        var item5 = {
            label:"编辑",
            callback:editRuleInfo,
            icon:"icon icon-pencil",
            visible:function(){return !isTreeRoot();}
        }

        var menu1 = new $.Menu();
        menu1.addItem(item1);
        menu1.addItem(item2);
        menu1.addItem(item5);
        menu1.addItem(item4);
        menu1.addItem(item3);

        $1("tree").contextmenu = menu1;
    }
 
    function loadInitData(){
        var request = new $.HttpRequest();
        request.url = URL_INIT;
        request.onresult = function(){
            var ruleTreeNode = this.getNodeValue(XML_MAIN_TREE);			
			var tree = $.T("tree", ruleTreeNode);
			
			tree.onTreeNodeDoubleClick = function(ev) { editRuleInfo(); }
            tree.onTreeNodeRightClick = function(ev) { onTreeNodeRightClick(ev); }
        }
        request.send();
    }
 
    function addNewRule(){
        var treeID = DEFAULT_NEW_ID;

        var phases = [];
        phases[0] = {page:"page1", label:"基本信息"};
        phases[1] = {page:"page3", label:"转出角色"};
        phases[2] = {page:"page4", label:"用户列表"};
        phases[3] = {page:"page2", label:"用户组织"};

        var callback = {};
        callback.onTabChange = function(){
            setTimeout(function(){
                loadRuleDetailData(treeID,true,true);
            }, TIMEOUT_TAB_CHANGE);
        };

        var inf = {};
        inf.defaultPage = "page1";
        inf.label = OPERATION_ADD.replace(/\$label/i, "转授策略");
        inf.phases = phases;
        inf.callback = callback;
        inf.SID = CACHE_RULE_DETAIL + treeID;
        ws.open(inf);
    }

    function editRuleInfo() {
		if( isTreeRoot() ) return;
		
		var treeNode = $.T("tree").getActiveTreeNode();
		var treeNodeID = treeNode.id;
		var treeNodeName = treeNode.name;

		var phases = [];
		phases[0] = {page:"page1", label:"基本信息"};
		phases[1] = {page:"page3", label:"转出角色"};
		phases[2] = {page:"page4", label:"用户列表"};
		phases[3] = {page:"page2", label:"用户组织"};

		var callback = {};
		callback.onTabChange = function(){
			setTimeout(function(){
				loadRuleDetailData(treeNodeID);
			}, TIMEOUT_TAB_CHANGE);
		};

		var inf = {};
		inf.label = OPERATION_EDIT.replace(/\$label/i, treeNodeName);
		inf.SID = CACHE_RULE_DETAIL + treeNodeID;
		inf.defaultPage = "page1";
		inf.phases = phases;
		inf.callback = callback;
		ws.open(inf);
    }
 
    function loadRuleDetailData(treeID) {
		var request = new $.HttpRequest();
		request.url = URL_RULE_DETAIL + treeID;
		request.onresult = function(){
			var ruleInfoNode            = this.getNodeValue(XML_RULE_INFO);
			var rule2RoleTreeNode       = this.getNodeValue(XML_RULE_TO_ROLE_TREE);
			var rule2RoleExistTreeNode  = this.getNodeValue(XML_RULE_TO_ROLE_EXIST_TREE);
			var rule2UserTreeNode       = this.getNodeValue(XML_RULE_TO_GROUP_TREE);
			var rule2UserExistTreeNode  = this.getNodeValue(XML_RULE_TO_USER_EXIST_TREE);
			var rule2GroupTreeNode      = this.getNodeValue(XML_RULE_TO_GROUP_TREE);
			var rule2GroupExistTreeNode = this.getNodeValue(XML_RULE_TO_GROUP_EXIST_TREE);			
			
			disableTreeNodes(rule2RoleTreeNode,  "treeNode[isGroup='1']");
			disableTreeNodes(rule2GroupTreeNode, "treeNode[id='-2']");
			disableTreeNodes(rule2GroupTreeNode, "treeNode[id='-3']");

			$.cache.XmlDatas[treeID + "." + XML_RULE_INFO] = ruleInfoNode;

			var page1FormObj = $.F("page1Form", ruleInfoNode);

			var page3Tree  = $.T("page3Tree",  rule2RoleTreeNode);
			var page3Tree2 = $.T("page3Tree2", rule2RoleExistTreeNode);
			var page2Tree  = $.T("page2Tree",  rule2GroupTreeNode);
			var page2Tree2 = $.T("page2Tree2", rule2GroupExistTreeNode);			
			var page4Tree  = $.T("page4Tree",  rule2UserTreeNode);
			var page4Tree3 = $.T("page4Tree3", rule2UserExistTreeNode);
			
			page4Tree.onTreeNodeDoubleClick = function(ev){
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
			
			// 设置按钮操作
			$1("page3BtAdd").onclick = function(){
				addTreeNode(page3Tree, page3Tree2);
			}
			$1("page3BtDel").onclick = function(){
				removeTreeNode($.T("page3Tree2"));
			}
			$1("page2BtAdd").onclick = function(){
				addTreeNode(page2Tree, page2Tree2);
			}
			$1("page2BtDel").onclick = function(){
				removeTreeNode($.T("page2Tree2"));
			}
			$1("page4BtAdd").onclick = function(){
				addTreeNode($.T("page4Tree2"), page4Tree3);
			}
			$1("page4BtDel").onclick = function(){
				 removeTreeNode($.T("page4Tree3"));
			}

			// 设置保存按钮操作
			$1("page1BtSave").onclick = $1("page2BtSave").onclick = $1("page3BtSave").onclick = $1("page4BtSave").onclick = function(){
				saveRule(treeID);
			}
		}
		request.send();
    }

    function saveRule(cacheID) {
        // 校验page1Form数据有效性
        var page1FormObj = $.F("page1Form");
        if( !page1FormObj.checkForm() ) {
            ws.switchToPhase("page1");
            return;
        }

        var request = new $.HttpRequest();
        request.url = URL_SAVE_RULE;
 
		// 策略基本信息
		var ruleInfoNode = $.cache.XmlDatas[cacheID + "." + XML_RULE_INFO];
		var ruleInfoDataNode = ruleInfoNode.querySelector("data");
		request.setFormContent(ruleInfoDataNode);

		// 转授出去的角色
		var rule2RoleIDs = $.T("page3Tree2").getAllNodeIds();
		request.addParam(XML_RULE_TO_ROLE_IDS, rule2RoleIDs.join(","));

		// 转授给用户
		var rule2UserIDs = $.T("page4Tree3").getAllNodeIds();
		request.addParam(XML_RULE_TO_USER_IDS, rule2UserIDs.join(","));

		// 转授给用户组
		var rule2GroupIDs = $.T("page2Tree2").getAllNodeIds();
		request.addParam(XML_RULE_TO_GROUP_IDS, rule2GroupIDs.join(","));
		
		//同步按钮状态
		syncButton([$1("page1BtSave"), $1("page2BtSave"), $1("page3BtSave"), $1("page4BtSave")], request);

		request.onsuccess = function(){	
			loadInitData();
			ws.closeActiveTab();
		}

		request.send();
	}
	
    window.onload = init;