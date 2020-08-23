function isReportGroup() {
    return "0" == getTreeAttribute("type");
}

function isReport() {
    return !isTreeRoot() && !isReportGroup();
}

function closeReportDefine() {
    $("#reportParamsDiv").hide();
    $("#ws").hide();
}

function showGridChart(displayUri, reportId, refresh) {
    closeReportDefine();
    $(".body>.groove>table").show();

    if(displayUri) {
        $("#grid").hide();
        $("#gridTitle").hide();
        $("#gridContainer iframe").hide();
        reportId && closePalette(); // 关闭左栏
        
        var iframeId = "chatFrame_" + reportId;
        var $iframe = $("#" + iframeId);
        if( !$iframe.length ) {
            var iframeEl = $.createElement("iframe", "container", iframeId);
            $("#gridContainer").appendChild(iframeEl);

            $iframe = $(iframeEl);
            $iframe.attr("frameborder", 0).attr("src", displayUri);
        }
        else {
            refresh && $iframe.attr("src", displayUri); // 重新查询后需要刷新（重新加载iframe里的网页）
        }
        $iframe.show();
    }
    else {
        $("#grid").show();
        $("#gridTitle").show();
        $("#gridTitle .title").html("报表【" + getTreeNodeName() + "】查询结果");
    }
}

function showReport() {
    var treeNode = getActiveTreeNode();
    var reportId = treeNode.id;
    var displayUri  = (treeNode.getAttribute("displayUri") || "").trim().replace('|', '&'); 
    var paramConfig = (treeNode.getAttribute("param") || "").trim(); 
    var hasScript = treeNode.getAttribute("hasScript") == "true"; 

    globalValiable.title = treeNode.name;
    globalValiable.reportId = reportId;

    // 判断当前报表是否专门配置了展示页面
    if( displayUri.length > 0 ) {
        // 如果地址里指明了nodb（数据库已经不可访问），则直接打开展示页面，使之读取静态的本地json数据作为展示用
        if( displayUri.indexOf("?nodb=") > 0 ) {
            showGridChart(displayUri, reportId); 
            return;
        }

        // 如果还配置了参数，则由report页面统一生成查询Form，查询后再打开展示页面里。
        if(paramConfig.length > 0) {
            $("#gridContainer iframe").hide();
            $("#chatFrame_" + reportId).show(); // 如之前已打开过报表，则先调出之前的结果

            createQueryForm(reportId, paramConfig, function(params) {
                sendAjax(params);
            });
        }
        else {
            $("#searchForm").html(""); // 删除查询框（其它报表的）
            $("#searchFormDiv").hide();
            delete globalValiable.data;

            if(hasScript || displayUri.indexOf("?type=") > 0) {
                sendAjax(); // 使用通用模板的，有可能此处是不带任何参数的SQL查询
            } 
            else {
                showGridChart(displayUri, reportId); // 直接打开展示页面
            }
        }
    } 
    else {
        createQueryForm(reportId, paramConfig); // 生成查询Form
    }   

    function sendAjax(params) {
        var url = getServiceUrl(reportId, displayUri); // 根据服务地址取出数据放在全局变量里
        $.ajax({
            url : url,
            method : "POST",
            params : params,
            type : "json",
            waiting : true,
            ondata : function() { 
                globalValiable.queryParams = this.params;
                globalValiable.data = this.getResponseJSON();
                
                // 数据在iframe里展示
                showGridChart(displayUri, reportId, true);
            }
        });
    }
}

function searchReport(reportId, download) {       
    var xform = $.F("searchForm");  
    if( xform && !xform.checkForm() ) return;

    var params = getParams();
    if(download) {
        exportReport(reportId, params);
    }
    else {
        showGridChart();
        searchGridReport(reportId, params);
    }
} 

function createQueryForm(reportId, paramConfig, callback) {
    var $panel = $("#searchFormDiv");
    $panel.show();

    // 如果上一次打开的和本次打开的是同一报表的查询框，则直接显示
    if( $.cache.Variables["treeNodeID_SF"] == reportId 
    		&& $.funcCompare($.cache.Variables["callback_SF"], callback) 
    		&& $("#searchFormDiv .tssForm").length ) { 
        return;
    }

    $.cache.Variables["treeNodeID_SF"] = reportId;
    $.cache.Variables["callback_SF"] = callback;

    var searchForm = genQueryForm(getTreeNodeName(), paramConfig);

    $(".btSearch").click( function () {
        if(callback) {
            searchForm.checkForm() && callback( getParams() ); // 在回调函数里读取数据并展示
        } else {
            searchReport(reportId, false);  // 直接Grid展示
        }
    });
    $(".btDownload").click( function () {
        searchReport(reportId, true);
    });
}

function openQueryForm() {
    if( $("#searchFormDiv .tssForm").length ) {
        $('#searchFormDiv').show();
    } else {
        $.alert("请双击您要查询的报表节点");
    }
}