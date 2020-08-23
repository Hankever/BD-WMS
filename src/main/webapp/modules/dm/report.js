/* XMLHTTP请求地址汇总 */
URL_SOURCE_TREE    = AUTH_PATH + "rp/";
URL_GROUPS_TREE    = AUTH_PATH + "rp/groups";
URL_SOURCE_DETAIL  = AUTH_PATH + "rp/detail";
URL_SAVE_SOURCE    = AUTH_PATH + "rp";
URL_DELETE_SOURCE  = AUTH_PATH + "rp/";
URL_DISABLE_SOURCE = AUTH_PATH + "rp/disable/";
URL_SORT_SOURCE    = AUTH_PATH + "rp/sort/";
URL_COPY_SOURCE    = AUTH_PATH + "rp/copy/";
URL_MOVE_SOURCE    = AUTH_PATH + "rp/move/";
URL_GET_OPERATION  = AUTH_PATH + "rp/operations/";  // {id}
URL_REPORT_JOB     = AUTH_PATH + "rp/schedule";
URL_SUBSCRIBE_SOURCE  = AUTH_PATH + "rp/mailable/";
URL_EXPORT_REPORT  = AUTH_PATH + "export/report/";
URL_RECORD_TREE    = AUTH_PATH + "rc/all";
URL_IMPORT_RECORD  = AUTH_PATH + "export/record2report";

if(IS_TEST) {
	URL_SOURCE_TREE    = "data/report_tree.xml?";
	URL_GROUPS_TREE    = "data/groups_tree.xml?";
	URL_SOURCE_DETAIL  = "data/report_detail.xml?";
	URL_SAVE_SOURCE    = "data/_success.xml?";
	URL_DELETE_SOURCE  = "data/_success.xml?";
	URL_DISABLE_SOURCE = "data/_success.xml?";
	URL_SORT_SOURCE    = "data/_success.xml?";
	URL_COPY_SOURCE    = "data/_success.xml?";
	URL_MOVE_SOURCE    = "data/_success.xml?";
	URL_GET_OPERATION  = "data/_operation.xml?";
	URL_REPORT_JOB     = "data/report_schedule.json";
	URL_SUBSCRIBE_SOURCE = "data/_success.xml?";
	URL_EXPORT_REPORT  = "data/_success.xml?";
	URL_RECORD_TREE    = "data/record_tree.xml?";
	URL_IMPORT_RECORD  = "data/_success.json?";
}

/* 页面初始化 */
$(function() {
	initMenus();
	initWorkSpace();
	initEvents();

	loadInitData();

    if(document.body.offsetHeight < 560) {
    	$("#ws").css("overflow-y", "auto");
    }
});

/* 菜单初始化 */
function initMenus() {
	/* 树菜单初始化  */
	ICON = "images/"
	var item1 = {
		label:"报表查询",
		callback:showReport,
		icon:"icon icon-search",
		visible:function() {return isReport() && getOperation("1");}
	}
	var item10 = {
		label:"查看报表定义",
		callback: function() {
			openReportDefine(false, true);
		},
		icon:"icon icon-file-text",
		visible:function() { return isReport() && getOperation("1") && !getOperation("2"); }
	}
	var item2 = {
		label:"修改",
		callback: function() {
			openReportDefine(false, false);
		},
		icon:"icon icon-pencil",
		visible:function() { return !isTreeRoot() && getOperation("2"); }
	}
	var item3 = {
		label:"新增报表（服务）",
		callback: function() {
			openReportDefine(true, false, "1");
		},
		icon: ICON + "report_0.gif",
		visible:function() {return (isReportGroup() || isTreeRoot()) && getOperation("2");}
	}
	var item32 = {
		label:"新增数据表链接",
		callback: recordAsReport,
		icon: ICON + "record_0.png",
		visible:function() {return (isReportGroup() || isTreeRoot()) && getOperation("2");}
	}
	var item4 = {
		label:"新增分组",
		callback: function() {
			openReportDefine(true, false, "0");
		},
		icon: ICON + "icon_folder_new.gif",
		visible:function() {return (isReportGroup() || isTreeRoot()) && getOperation("2");}
	}
	var item5 = {
		label:"删除",
		callback:deleteReport,
		icon:"icon icon-x",
		visible:function() {return !isTreeRoot() && getOperation("3");}
	}
	var item6 = {
		label:"复制到",
		callback:copyReportTo,
		icon:"icon icon-file-symlink-file",
		visible:function() {return isReport() && getOperation("2");}
	}
	var item7 = {
		label:"移动到",
		callback:moveReport,
		icon:"icon icon-arrow-right",
		visible:function() {return !isTreeRoot() && getOperation("2");}
	}
	var item8 = {
		label:"停用",
		callback:disableReport,
		icon:"icon icon-triangle-down",
		visible:function() {return !isTreeRoot() && !isTreeNodeDisabled() && getOperation("4");}
	}
	var item9 = {
		label:"启用",
		callback:enableReport,
		icon:"icon icon-triangle-up",
		visible:function() {return !isTreeRoot() && isTreeNodeDisabled() && getOperation("4");}
	}
	var item11 = {
		label:"测试数据服务",
		callback:testRestfulReportService,
		icon:"icon icon-tools",
		visible:function() {return isReport() && getOperation("2");}
	}
	var item12 = {
		label:"定时邮件推送",
		callback:scheduleReport,
		icon:"icon icon-clock",
		visible:function() {return isReport() && getOperation("2");}
	}
	var item13 = {
        label:"导出报表定义",
        callback:exportReportDef,
        visible:function() {return !isTreeRoot() && getOperation("2");}
    }
    var item14 = {
        label:"导入报表定义",
        callback:importReportDef,
        visible:function() {return !isReport() && getOperation("2");}
    }
    var item15 = {
        label:"允许订阅",
        callback: subscribe,
        visible:function() {return isReport() && getTreeAttribute("mailable") != "1" && getOperation("2");}
    }
    var item16 = {
        label:"禁止订阅",
        callback: subscribe,
        visible:function() {return isReport() && getTreeAttribute("mailable") == "1" && getOperation("2");}
    }

	var menu = new $.Menu();
	menu.addItem(item1);
	menu.addItem(item10);
	menu.addItem(item2);
	menu.addItem(item3);
	menu.addItem(item32);
	menu.addItem(item4);
	menu.addSeparator();
	menu.addItem(item6);
	menu.addItem(item7);
	menu.addItem(item13);
	menu.addItem(item14);
	menu.addItem(item8);
	menu.addItem(item9);
	menu.addItem(item5);
	menu.addSeparator();
	menu.addItem(item11);
	menu.addItem(item12);
	menu.addItem(item15);
	menu.addItem(item16);	
	menu.addItem(createPermissionMenuItem("D1"));
	
	$1("tree").contextmenu = menu;
}

function loadInitData() {
	var onresult = function() {
		var tree = $.T("tree", this.getNodeValue(XML_SOURCE_TREE));

		tree.onTreeNodeDoubleClick = function(ev) {
			var treeNode = getActiveTreeNode();
			getTreeOperation(treeNode, function(_operation) {            
				if( isReport() ) {
					showReport();
				}
				if( isReportGroup() && getOperation("2") ) {
					openReportDefine(false, false);
				}
			});
		}
		tree.onTreeNodeRightClick = function(ev) {
			onTreeNodeRightClick(ev, true);
			$("#searchFormDiv").hide();
		}
		tree.onTreeNodeMoved = function(ev) {
			$("#searchFormDiv").hide();
			sortTreeNode(URL_SORT_SOURCE, ev);
		}
	}

	$.ajax({url : URL_SOURCE_TREE, onresult : onresult});
}

function openReportDefine(isCreate, readonly, type) { 
	var treeNode = $.T("tree").getActiveTreeNode();
	var treeNodeID = treeNode.id;
	var treeName = treeNode.name;
	type = type || treeNode.getAttribute("type") ;

	var params = {};
	if( isCreate ) {
		params["parentId"] = treeNodeID; // 新增
		readonly = false;
		treeName = "报表" + (type === "0" ? "组" : "");
	} 
	else {
		params["reportId"] = treeNodeID; // 修改					
	}
	
	/* 新增时，传入的是父节点ID，如果父节点是打开状态，需要先关闭父节点的Tab页；修改时先关闭再重新打开，以load最新的对象。 */
	ws.closeTab(treeNodeID);

    var callback = {};
    callback.onTabChange = function() {
        setTimeout(function() {
            loadReport(treeNodeID, type, params, readonly);
        }, TIMEOUT_TAB_CHANGE);
    };
    callback.onTabClose = function(sid) {
    	$("#reportParamsDiv").hide();
        delete $.cache.XmlDatas[sid];
    };

    var operation = readonly ? OPERATION_VIEW : (isCreate ? OPERATION_ADD : OPERATION_EDIT);
    var inf = {};
    inf.defaultPage = "page1";
    inf.label = operation.replace(/\$label/i, treeName);
    inf.callback = callback;
    inf.SID = treeNodeID;
    var tab = ws.open(inf);
}

function loadReport(treeNodeID, type, params, readonly) {
	$("#searchFormDiv").hide();
	$(".body>.groove>table").hide();
	$(ws).show();

	var sourceInfoNode = $.cache.XmlDatas[treeNodeID];
	if(sourceInfoNode) {
		return initForm();	
	}

	function initForm() {
		var xform = $.F("reportForm", sourceInfoNode);
		attachReminder(treeNodeID, xform); // 离开提醒
		
		if(readonly) {
			$("#sourceSave").hide();
			xform.setEditable("false");
		} 
		else {
			$("#sourceSave").show().click(function() { saveReport(treeNodeID); });
		}	
		$("#sourceClose").click(function() { ws.closeActiveTab(); });
	}

	$.ajax({
		url : URL_SOURCE_DETAIL + "/" + type,
		params : params,
		onresult : function() {
			sourceInfoNode = this.getNodeValue(XML_SOURCE_INFO);
			$.cache.XmlDatas[treeNodeID] = sourceInfoNode;
			initForm();
		},
		onexception : function() { 
			closeReportDefine();
		}
	});
}

function saveReport(treeNodeID) {
	var xform = $.F("reportForm");	
	if( !xform.checkForm() ) return;

	var request = new $.HttpRequest();
	request.url = URL_SAVE_SOURCE;

	var sourceInfoNode = $.cache.XmlDatas[treeNodeID];
	var dataNode = sourceInfoNode.querySelector("data");
	request.setFormContent(dataNode);

	syncButton([$1("sourceSave")], request); // 同步按钮状态

	request.onresult = function() { // 新增结果返回              
		afterSaveTreeNode.call(this, treeNodeID, treeNodeID);
	}
	request.onsuccess = function() { // 更新
		afterSaveTreeNode(treeNodeID, xform);
		
		modifyTreeNode(treeNodeID, "param", xform.getData("param"));
		modifyTreeNode(treeNodeID, "displayUri", xform.getData("displayUri")); 
		modifyTreeNode(treeNodeID, "hasScript", xform.getData("script") ? "true" : "false");

		delete $.cache.Variables["treeNodeID_SF"];
	}
	request.send();
}

function deleteReport()  { delTreeNode(URL_DELETE_SOURCE); }
function disableReport() { stopOrStartTreeNode("1", URL_DISABLE_SOURCE); }
function enableReport()  { stopOrStartTreeNode("0", URL_DISABLE_SOURCE); }

function subscribe() {
	var treeNode = getActiveTreeNode();
	var treeNodeID = treeNode.id;
	var mailable = Math.abs(parseInt(treeNode.attrs["mailable"]||"0") - 1);
	$.ajax({
        url : URL_SUBSCRIBE_SOURCE + treeNodeID + "/" + mailable,
		onsuccess : function() { 
			modifyTreeNode(treeNodeID, "mailable", mailable+"" );
		}
    });
}
 
function copyReportTo() {
	var treeNode = getActiveTreeNode();
	var id  = treeNode.id;
	var pId = treeNode.parent.id;

    var params = {id: id, parentID: pId, _title: "复制报表到："};
    popupTree(URL_GROUPS_TREE, "SourceTree", params, function(target) {
    	var targetId = (target.id == '_root' ? '0' : target.id);
        $.ajax({
            url : URL_COPY_SOURCE + id + "/" + targetId,
            onresult : function() { 
                var xmlNode = this.getNodeValue(XML_SOURCE_TREE).querySelector("treeNode");
                appendTreeNode(target.id, xmlNode);
            }
        });
    });
}

function moveReport() {
	var tree = $.T("tree");
	var treeNode = tree.getActiveTreeNode();
	var id  = treeNode.id;
    var pId = treeNode.parent.id;

    var params = {id:id, parentID: pId, "_title": "移动报表到："};
    popupTree(URL_GROUPS_TREE, "SourceTree", params, function(target) {
    	var targetId = (target.id == '_root' ? '0' : target.id);
        moveTreeNode(tree, id, targetId, URL_MOVE_SOURCE);
    });
}		

function recordAsReport() {
	var tree = $.T("tree");
	var treeNode = tree.getActiveTreeNode();
	var id  = treeNode.id, name = treeNode.name;

    var params = {"_title": "链接数据表到【" + name + "】", 'treeType':'multi'};
    popupTree(URL_RECORD_TREE, "SourceTree", params, function(target) {
    	$.post(URL_IMPORT_RECORD, {"reportGroup": id, "recordIds": target.join(",")}, function(msg) {
    		loadInitData();
    		$.alert("成功链接数据表");
    	});
    });
}
 
var cur_api, cur_params;
function testRestfulReportService() {
	var treeNode = getActiveTreeNode();
	var treeID   = treeNode.id;
	var nodeName = treeNode.name;
	var paramConfig = (treeNode.getAttribute("param") || "").trim(); 
	var displayUri  = (treeNode.getAttribute("displayUri") || "").trim().replace('|', '&'); 
	var url = getServiceUrl(treeID, displayUri);

	if(paramConfig.length > 0) {
		createQueryForm(treeID, paramConfig, sendAjax);
	} 
	else {
		sendAjax();
	}

	function sendAjax(params) {
		cur_api = {};
        cur_api.url  = url;
        cur_api._url = url;
        cur_api.method = "POST";
        cur_api._href = "p2_1";

        params = params||{};
        params.rows = 10;
        params.page = 1;
        cur_params = JSON.stringify( params ),

        $.openIframePanel("testServicePanel", "调试数据服务接口【" +nodeName+ "】", 1000, 600, "../../more/api_test.html");
/*
		$.getJSON(url, params, function(data) { 
				var result = "执行接口调用脚本:<br>";
				result += "$.getJSON('" + url + "', " + JSON.stringify(params||{});
				result += ", function(data) { alert(data.length); });<br>";
				result += "返回JSON格式数据:<br>"
				result += JSON.stringify(data);

				alert(result, "调试数据服务接口：" + url);
			}
		);
*/
	}
}

// ------------------------------------------------ 配置报表定时邮件 -----------------------------------------------

function scheduleReport() {
	var scheduleTemplate = [];
	scheduleTemplate.push({'label':'定时规则', 'name':'scheduleRule', 'type':'String', 'nullable':'false', 'defaultValue':'0 0 12 * * ?'}); 
	scheduleTemplate.push({'label':'收件人', 'name':'receiverEmails', 'type':'String', 'nullable':'false', 'width':"400px"}); 

	var treeNode = getActiveTreeNode();
	var paramConfig = $.parseJSON( (treeNode.getAttribute("param") || "").trim() ); 
	if(paramConfig && paramConfig.length) {
		paramConfig.each(function(i, param) {
			var tmp = {};
			tmp.label = "参数-" + param.label;
			tmp.name = "param" + (i+1);
			tmp.type = "String";
			tmp.nullable = param.nullable;
			tmp.defaultValue = param.defaultValue;
			scheduleTemplate.push(tmp);
		});
	}

	var scheduleForm;
	$.ajax({
		url: URL_REPORT_JOB + "?reportId=" + treeNode.id + "&self=false",
		method: "GET",
		type: "json",
		ondata: function() {
			var scheduleInfo = this.getResponseJSON();
			if(scheduleInfo && scheduleInfo.length >= 3) {
				scheduleTemplate[0].defaultValue = scheduleInfo[1];

				var jobInfo = scheduleInfo[2].split(":");
				scheduleTemplate[1].defaultValue = jobInfo[2];

				var pairs = jobInfo[3].split(",");
				for (var i = 0; i < pairs.length; i++) {
                    var keyValue = pairs[i].split("=");
                    scheduleTemplate.each(function(n, item) {
                    	if(item.name === keyValue[0]) {
                    		item.defaultValue = keyValue[1];
                    	}
                    });
                }
			}	

			scheduleForm = $.json2Form("scheduleForm", scheduleTemplate);
			$.cache.XmlDatas["scheduleFormXML"] = scheduleForm.template.sourceXML;

			scheduleForm.reportId = treeNode.id;
			scheduleForm.reportName = treeNode.name;

			$1("scheduleRule").placeholder = "请参照下面列出的各种定时规则写法";
			$1("receiverEmails").placeholder = "输入完整的邮件地址，多个地址以逗号分隔";
		}
	});

	$("#scheduleFormDiv").show(true);
	$("#scheduleFormDiv").find("h3").html("报表【" + treeNode.name + "】定时邮件配置");
	
	$("#scheduleSave").click( function () { 
		if( scheduleForm.checkForm() ) {
			var scheduleFormXML = $.cache.XmlDatas["scheduleFormXML"];
			var dataNode = scheduleFormXML.querySelector("data");
			var fieldNodes = dataNode.querySelectorAll("row *");
        
	        var result = {};
	        var paramsValue = [];
	        $.each(fieldNodes, function(i, node) {
	        	var value = $.XML.getText(node);
	        	if(node.nodeName.indexOf("param") < 0) {
	        		result[node.nodeName] = value;
	        	} else {
	        		paramsValue.push(node.nodeName + "=" + value);
	        	}
	        });

	        var reportId = scheduleForm.reportId;
	        var reportName = scheduleForm.reportName;
	        var receiverEmails = result.receiverEmails.replace(/\，/g, ","); // 替换中文逗号及（空格 replace(/\s/g, ",")）
			var configVal = result.scheduleRule + " | " + reportId + ":" + reportName + ":" + receiverEmails + ":" + paramsValue.join(",");

			$.ajax({
				url: URL_REPORT_JOB,
				params: {"reportId": reportId, "configVal": configVal, "self": false},
				method: "POST",
				onsuccess: function() {
					$("#scheduleFormDiv").hide();
				}
			});
		}
	});
	$("#cancelSchedule").click( function () { 
        var params = {"reportId": scheduleForm.reportId, "self": false}
        $.ajax({
            url: URL_REPORT_JOB,
            params: params,
            method: "DELETE",
            onsuccess: function() {
                $("#scheduleFormDiv").hide();
            }
        });
    });
	$("#closeScheduleForm").click( function() { $("#scheduleFormDiv").hide(); });
}

// -------------------------------------------------   配置报表参数   ------------------------------------------------
var paramTree, count = 0;
function configParams() {
	var rform = $.F("reportForm");
	var _paramsConfig = (rform.getData("param")||'').revertEntry(); // getData里执行了XML符号处理方法 converEntity，& --> &amp;  需要替换回来
	var paramsConfig = $.parseJSON(_paramsConfig) || [];

	var paramNodes = [];
	paramsConfig.each(function(index, item){
		var paramNode = {"id": index+1, "name": item.label, "value": JSON.stringify(item)};
		paramNodes.push(paramNode); 
	});

	var treeData = [{"id": "_root", "name": "查询参数列表", "children": paramNodes}];
	paramTree = $.T("paramTree", treeData);	
	initParamTreeMenus();

	paramTree.onTreeNodeActived = function(ev) {
		if(paramTree.getActiveTreeNodeId() != '_root') {
			editParamConfig();
		}
	}
	paramTree.onTreeNodeRightClick = function(ev) {
		if(paramTree.getActiveTreeNodeId() != '_root') {
			editParamConfig();
		}
		paramTree.el.contextmenu.show(ev.clientX, ev.clientY);
	}
	paramTree.onTreeNodeMoved = function(ev) {
		ev.ownTree.sortTreeNode(ev.dragNode, ev.destNode);
	}

	$("#reportParamsDiv").show(true).center().css("top", "60px");

	// 默认选中第一个参数，如果没有则清空表单
	var paramNodeIds = paramTree.getAllNodeIds();
	count = paramNodeIds.length;
	if(count > 0) {
		paramTree.setActiveTreeNode(paramNodeIds[0]);
		editParamConfig();
	}
	else {
		REPORT_PARAM_FIELDS.each(function(i, field){
    		$("#_" + field).value('');
    	});
    	createNewParam();
	}
}

function initParamTreeMenus() {
    var item1 = {
        label:"删除",
        icon:"icon icon-x",
        callback: function() { paramTree.removeActiveNode(); },
        visible: function() { 
        	return paramTree.getActiveTreeNodeId() !== '_root';
        }
    }
    var item2 = {
        label:"新建参数",
        icon:"icon icon-plus",
        callback: createNewParam,
        visible: function() { 
        	return paramTree.getActiveTreeNodeId() === '_root'; 
        }
    }
 
    var menu1 = new $.Menu();
    menu1.addItem(item2);
	menu1.addItem(item1);
    paramTree.el.contextmenu = menu1;
}

function createNewParam() {
	var id = $.now(), name = '新增参数' + (++count);
	var newNode = {'id': id, 'name': name, 'value': '{"label": "' +name+ '"}'};
	var _root = paramTree.getTreeNodeById("_root");
	paramTree.addTreeNode(newNode, _root);
	paramTree.setActiveTreeNode(id);
	editParamConfig();
}

function closeConfigParams() {
	$("#reportParamsDiv").hide();
}

var REPORT_PARAM_FIELDS = ['label', 'code', 'type', 'nullable', 'defaultValue', 'checkReg', 'errorMsg', 'width', 'height', 'options', 'multiple', 'onchange', 'isMacrocode'];

function editParamConfig() {
	var activeNode = paramTree.getActiveTreeNode();
	if( !activeNode ) return;

    var valuesMap = $.parseJSON(paramTree.getActiveTreeNodeAttr("value")) || {};
    REPORT_PARAM_FIELDS.each(function(i, field){
    	var fieldEl = $1("_" + field);
    	var fieldValue = valuesMap[field] || '';

		if(field === 'type') {
			fieldValue = fieldValue.toLowerCase();
			if(fieldValue == "date" || fieldValue == "datetime") {
				$("#optionalBox").css("display", "none");
			}
			else {
				$("#optionalBox").css("display", "block");
			}
		}

		if( field === 'options' ) {
			if(fieldValue.codes) {
				fieldValue = fieldValue.codes + ',' + fieldValue.names;
			}
			else if( valuesMap['jsonUrl'] ) {
				fieldValue = valuesMap['jsonUrl']
			}
		}
		if(field === 'multiple') {
		    if(fieldValue == "true") {
				$1("_height").removeAttribute("readonly");
			} else {
				$1("_height").setAttribute("readonly", "readonly");
			}
		}

	    if(field === 'width' || field === 'height') { 
	    	fieldValue = fieldValue ? fieldValue.replace('px', '') : (field === 'width' ? 250 : 18)
	    	$('#_' + field + '_').html(fieldValue);
		}
		if( $(fieldEl).attr("type") == 'checkbox' ) { // checkbox
			fieldEl.checked = fieldValue == 'true';
			if(field == 'nullable') {
				fieldEl.checked = fieldValue == 'false';
			}
		} else {
			fieldEl.value = fieldValue;
		}

		if( field == 'onchange' ) {
			$(".ld>input").value("");
			if( fieldValue ) {
				if(fieldValue.indexOf("getNextLevelOption(") >= 0) {
					var c = fieldValue.replace(/\^/g, "");
					c = c.substr(c.indexOf("(")+1, c.indexOf(")")-c.indexOf("(")-1).split(",");
					if(c.length == 3) {
						$("#ldField").value( c[0].trim() );
						$("#ldService").value( c[1].trim() );
						$("#ldParam").value( c[2].trim() );
					}
				}
				else {
					$("#ldField").value("");
					$("#ldService").value( fieldValue.trim() );
					$("#ldParam").value("");
				}
			}

			$1("ldField").onblur = $1("ldService").onblur = $1("ldParam").onblur = function() {
				var v,
					v1 = ($("#ldField").value()||"").trim(),
					v2 = ($("#ldService").value()||"").trim(),
					v3 = ($("#ldParam").value()||"").trim();

				if(v1 && v2 && v3) {
					v = "getNextLevelOption(^" +v1+ "^, ^" +v2+ "^, ^" +v3+ "^)";
				} else {
					v = v2;
				}

				$("#_onchange").value(v);
				if(v) {
					valuesMap['onchange'] = v;
				} else {
					delete valuesMap['onchange'];
				}
				activeNode.setAttribute("value", JSON.stringify(valuesMap));
			};
		}

    	fieldEl.onchange = function() {
    		var newValue;
    		if( $(fieldEl).attr("type") == 'checkbox' ) { // checkbox
				newValue = fieldEl.checked ? 'true' : "";
				if(field == 'nullable') {
					newValue = fieldEl.checked ? 'false' : "";
				}
			} else {
				newValue = fieldEl.value;
			}

			if( $.isNullOrEmpty(newValue) ) {
				if(field === 'label') {
					return $(fieldEl).notice("参数名称不能为空");
				}
				delete valuesMap[field];
			}
			else {
				newValue = newValue.trim();  // code、正则等配置头尾不能有空格
				
				if(field === 'label') {
					newValue = newValue.replace(/\&|\|/g, ""); // 过滤名称里的特殊字符:|、&
				}
				if(field === 'code') {
					// code只能为英文字母、数字、下划线
					if( !/^[a-zA-Z][a-zA-Z0-9_]*$/.test(newValue) ) {
						return $(fieldEl).notice("不能含有字母、数字、下划线以外的字符");
					}
				}
				valuesMap[field] = newValue;
			}			

    		if(field === 'label') {
				activeNode.attrs[field] = newValue;
				activeNode.li.a.title = newValue;
				$(activeNode.li.a).html(newValue);
    		}
    		if(field === 'width' || field === 'height') { 
		    	$('#_' + field + '_').html(newValue);
			}
			
    		if(field === 'options' && newValue) {
    			newValue = newValue.replace(/，/ig, ',') // 替换中文逗号
    			if(newValue.indexOf('|') < 0 && newValue.indexOf('/') >= 0) {
    				delete valuesMap['options'];
    				valuesMap['jsonUrl'] = newValue;
    			}
    			else {
	    			delete valuesMap['jsonUrl'];

	    			newValue = newValue.replace(/&/ig, ' '); // 替换&等符号
					var tmpArray = newValue.split(",");
					var names = (tmpArray.length > 1 ? tmpArray[1] : tmpArray[0]);
	    			valuesMap['options'] = {"codes": tmpArray[0], "names": names};
	    		}
    		} 

    		if(field === 'multiple') {
    			if(newValue == "true") {
    				$1("_height").removeAttribute("readonly");
    			} else {
    				$1("_height").setAttribute("readonly", "readonly");
					$1("_height").value = 18;
					$('#_height_').html('18');
					delete valuesMap['height'];
    			}
    		}
    		if(field === 'type') {
    			if(newValue == "date" || newValue == "datetime" || newValue == "hidden") {
    				if(newValue != "hidden") {
    					$1("_defaultValue").setAttribute("placeholder", "日期类型示例：today-3");
    				}
					$("#optionalBox").css("display", "none");
    			}
				else {
					$("#optionalBox").css("display", "block");
				}
    		}
    		activeNode.setAttribute("value", JSON.stringify(valuesMap));
    	}

    	if(field == 'options') {
    		fieldEl.onblur = fieldEl.onchange;
    	}
    });
}

function saveConfigParams() {
	var result = [];
	var paramNodes = paramTree.getAllNodes();
	paramNodes.each(function(i, node){
		var valuesMap = $.parseJSON(node.attrs["value"] || '{}');
		result.push( valuesMap );
	});

	var formatResult = JSON.stringify(result).replace(/\"/g, "'").replace(/\{'label/g, "\n  {'label");
	$.F("reportForm").updateDataExternal("param", formatResult.replace(/\}]/g, "}\n]"));

	closeConfigParams();
}

function exportReportDef() {
	var url = URL_EXPORT_REPORT + getTreeNodeId();
    var frameName = createExportFrame();
    $1(frameName).setAttribute("src", url);
}

function importReportDef() {
	var params = {"isTree": false};
	params._title = "请选择相应的数据源";

	popupTree("/tss/param/xml/combo/datasource_list", "ParamTree", params, function(target) {
	    function checkFileWrong(subfix) {
			return subfix != ".json";
		}

		var ds = target.attrs["value"];
		var url = URL_UPLOAD_FILE + "?groupId=" + getTreeNodeId() + "&dataSource=" + ds;
		url += "&afterUploadClass=com.boubei.tss.dm.ext.ImportReport";
		var importDiv = createImportDiv("只支持json文件格式导入", checkFileWrong, url);
		$(importDiv).show().center();
    });	
}	
