
window._alert = window.alert;
if(tssJS) {
  window.alert = tssJS.alert;  
}

TOMCAT_URL = "/tss";
if(location.protocol === 'file:') {
    TOMCAT_URL = 'http://localhost:9000/tss';
}

BAR_CODE_URL   = TOMCAT_URL + "/imgcode/bar/";

BASE_JSON_URL  = TOMCAT_URL + '/data/json/';
BASE_JSONP_URL = TOMCAT_URL + '/data/jsonp/';
function json_url(id, appCode)  { return BASE_JSON_URL  + id + (appCode ? "?appCode="+appCode : ""); }
function jsonp_url(id, appCode) { return BASE_JSONP_URL + id + (appCode ? "?appCode="+appCode : ""); }


BASE_RECORD_URL= TOMCAT_URL + '/xdata/';
function record_urls(tableId) {   // 一个录入表所拥有的增、删、改、查等接口
    var result = {};
    result.CREATE = BASE_RECORD_URL + tableId;
    result.UPDATE = BASE_RECORD_URL + tableId+ '/';  //{id}
    result.DELETE = BASE_RECORD_URL + tableId+ '/';  //{id}
    result.GET    = BASE_RECORD_URL + tableId+ '/';  //{id}
    result.QUERY  = BASE_RECORD_URL + 'json/' +tableId;
    result.BATCH  = BASE_RECORD_URL + 'batch/'+tableId;  // POST|DELETE
    result.CUD    = BASE_RECORD_URL + 'cud/'  +tableId;       // 批量增删改数据：CSV格式
    result.CUD_JSON    = BASE_RECORD_URL + 'cud/json/'  +tableId;  // 批量增删改数据：JSON格式
    result.ATTACH = BASE_RECORD_URL + 'attach/json/' +tableId+ '/';
    result.CSV_EXP= BASE_RECORD_URL + 'export/' +tableId;
    result.CSV_TL = BASE_RECORD_URL + 'import/tl/' +tableId;
    result.CSV_IMP= '/tss/auth/file/upload?afterUploadClass=com.boubei.tss.dm.record.file.ImportCSV&recordId=' +tableId;

    return result; 
}

// 依据录入表的名称或表名，获取ID，支持多个录入表一起查询
function record_id(nameOrTable, callback) {
    $.get(BASE_RECORD_URL + 'id', {'nameOrTables': nameOrTable}, function(ids) {
        callback(ids);
    });
}

//author:GNILUW
//依据报表的名称获取ID
function report_id(name, callback) {
    $.getJSON('/tss/data/' + name + '/define', {}, function(defines){
        callback(defines[7])
    })
}

// window.onbeforeunload = function() { return "检查当前是否有修改未保存，您确定要离开吗？"; }

// ["A","B","C"].contains("A,D")  ==> true
Array.prototype.containsPart = function(obj) {
    var i = this.length, result = false;
    while (i--) {
        var objArray = obj.split(",");
        var curr = this[i];
        objArray.each(function(i, item){
            if ( curr === item) {
                result = true;
            }
        });
    }
    return result;
};

/* 禁止弹2次confirm框 */
$.messager.single = {};
$.messager.single.confirm = function(title,m,callback){
    var divObj = $("div[class='panel window panel-htop messager-window']");
    if(divObj.length){
        $(".messager-body").window('close');
    }
    $.messager.confirm(title, m, callback)
}

// 用户权限信息
var userCode, 
userName, 
userDomain,
userGroups = [], 
userRoles = [],
userRoleNames = [], 
userHas,
userPartner = parent.global_userlist,
isCustomer;

if(tssJS){
    userHas = tssJS.Cookie.decode("userHas");
    if(userHas) {
        userGroups = userHas[0];
        userRoles  = userHas[1];
        userRoleNames = userHas[11];
        userCode   = userHas[3];
        userName   = userHas[4];
        userDomain = userHas[12];
        userRoleNames.each(function(i, item){
            userRoles.push(item);
        });
        isCustomer = userRoleNames.contains('客户') && userGroups[userGroups.length-1][1] == 'customer';
    }
}

!userPartner && jQuery && jQuery.getJSON(TOMCAT_URL+"/wx/api/users", {}, function(result) {
    userPartner = result;
});

function getLoginInfo(callback) {
    callback(tssJS.Cookie.decode("userHas"))
}

function initCombobox(id, code, params, init) {
    var url = '/tss/param/json/combo/' + code;
    $.get(url, params, function(data){
        var _data = [];
        $.each(data, function(i, item){
            _data.push({'id': item[0], 'text': item[1]});
        });

        $('#' + id).combobox( {
            panelHeight: '120px',
            width: '130px',
            valueField: 'id',
            textField: 'text',
            editable: false,
            data: _data
        });

        if(data[init]) {
            $('#' + id).combobox('setValue', data[init][0]);
        }
    });
}

function save(recordId, url, obj) {
    obj = obj || {};
    var fm = obj.fm || 'fm';
    var dlg = obj.dlg || 'dlg';
    var tableId = obj.tableId || 't1';
    var dlg_buttons = obj.dlg_buttons || 'dlg-buttons';

    var id = $("#" + dlg + " input[name='id']").val();
    var isCreate = !id;

    var $saveBtn = $('#' + dlg_buttons + '>a[onclick^="save"]');
    $saveBtn.linkbutton("disable");

    var flag = $("#" + fm).form('validate');
    if (flag) {
        var b_data = $("#" + fm).serializeArray(),
        a_data = {};
        b_data.each(function(i, item) {
            if(a_data[item.name]){
                a_data[item.name]+=','+item.value;
            }
            else{
                a_data[item.name] = item.value;
            }
        })
        $.post( url || (BASE_RECORD_URL + recordId + (isCreate ? ( obj._tempID ? "?_tempID=" + obj._tempID : "") : "/"+id) ), a_data,
            function(result) {
                $saveBtn.linkbutton("enable");
                checkException(result, function() {
                    closeDialog(fm, dlg);
                    if(obj.callback){
                        obj.callback(result);
                    }
                    else{
                        $('#' + tableId).datagrid('reload');
                    }
                });
            }
            );
    } 
    else {
        $.messager.alert('提示', "必填项（红线输入框）不能为空，请填写完成后再保存");
        $saveBtn.linkbutton("enable");
    }
}

function checkException(result, callback) {
    if(result && result.errorMsg) {
        $.messager.show({ title: '异常信息提示', msg: result.errorMsg });
    } 
    else if(result && (typeof result == 'string') && (result = eval('(' + result + ')') ).errorMsg){
        $.messager.show({ title: '异常信息提示', msg: result.errorMsg });
    }
    else {
        callback && callback();
    }
}

function openDialog(title, clear, fm, dlg) {
    fm = fm || 'fm';
    dlg = dlg || 'dlg';
    $('#' + dlg).dialog( {"modal": true} ).dialog('open').dialog('setTitle', title).dialog('center');

    clear && $('#' + fm).form('clear');
}

function closeDialog(fm,dlg) {
    fm = fm || 'fm';
    dlg = dlg || 'dlg';
    $('#' + dlg).dialog('close'); // close the dialog
    $('#' + fm).form('clear');
}

function getSelectedRow(tblID) {
    tblID = tblID || 't1';
    var row = $('#' + tblID).datagrid('getSelected');
    if (!row) {
        $.messager.alert({
            title: '提示',
            msg: '您没有选中任何行，请先点击选中需要操作的行。'
        });
    }
    return row;
}

function doRemove(elID, recordID, index,callback){
    elID = elID || "t1";
    var row = getSelectedRow(elID);
    row && $.messager.confirm('Confirm', '删除该行以后将无法再恢复，您确定要删除这行数据吗? ', function(result){
        result && tssJS.ajax({
         url: BASE_RECORD_URL + recordID +"/"+ row.id,
         method: 'DELETE',
         ondata: function(result) {
            checkException(result, function() {
                callback && callback()
                if( index >= 0 ) {
                    $('#' + elID).datagrid('deleteRow', index);
                } else {
                        $('#' + elID).datagrid('reload'); // reload the grid data
                    }
                });
        }
    });       
    });
}

function getAttachs(tableId, itemId, callback) {
    tssJS.ajax({ 
        url: BASE_RECORD_URL + "attach/json/" + tableId + "/" + itemId, 
        method: "GET", 
        ondata: function(){
            var data  = this.getResponseJSON();
            data && data.each(function(i, item) {
                callback(item);
            });
        } 
    });
}

function clone(from, to){   
   for(var key in from){    
      to[key] = from[key];   
  }   
}  

function cloneObj(obj) {  
    var newObj = {};  
    if (obj instanceof Array) {  
        newObj = [];  
    }  
    for (var key in obj) {  
        var val = obj[key];  
        if( val === null ){
            newObj[key] = null;
        }
        else{
            newObj[key] = typeof val === 'object' ? cloneObj(val): val;  
        }
    }  
    return newObj;  
}

/* --------------------------------------------------------- 导入导出 start ------------------------------------------------------------------*/

function exportDataGrid(id, name) {
    var _header = $('#' + id).datagrid("getColumnFields"), header = [];
    _header.each(function(i, code) {
        var field = $('#' + id).datagrid("getColumnOption", code);
        if( !field.hidden ) {
            header.push(field);
        }
    })

    var data = [];
    $.each( $('#' + id).datagrid("getRows"), function(index, row) {
        data.push(row);
    });

    if(data.length == 0) {
        $.messager.show({ title: '提示', msg: '没有任何数据可以导出，请先查询' });
    }
    tssJS.Data.data2CSV(name, header, data);
}

function _export(recordId, _params, fields) {
    if( _params ) {
        _params.page = 1;
        _params.pagesize = 100000;
    } else {
        _params = {};
    }

    if( fields ) _params.fields = fields; // 指定导出列，'name,phone,addr'

    var queryString = "?";
    $.each(_params, function(key, value) {
        if( queryString.length > 1 ) {
            queryString += "&";
        }
        queryString += (key + "=" + value);
    });

    var url = encodeURI("/tss/auth/xdata/export/" + recordId + queryString);
    tssJS("#" + createExportFrame()).attr( "src", url);
}

/* 创建导出用iframe */
function createExportFrame() {
    var frameName = "exportFrame";
    if( $1(frameName) == null ) {
        var exportDiv = tssJS.createElement("div"); 
        tssJS(exportDiv).hide().html("<iframe id='" + frameName + "' style='display:none'></iframe>");
        document.body.appendChild(exportDiv);
    }
    return frameName;
}

function batchImport(recordId, customize) {
    customize = customize || {};
    var afterUploadClass = customize.afterUploadClass || 'com.boubei.tss.dm.record.file.ImportCSV';

    var url = "/tss/auth/file/upload?afterUploadClass=" + afterUploadClass;
    url += "&recordId=" + recordId;
    if (customize.uniqueCodes) {
        url += "&uniqueCodes=" + customize.uniqueCodes; // 定义在页面全局变量里：uniqueCodes="oto,sjphone";
    }
    if (customize.ignoreExist === false || customize.ignoreExist) {
        url += "&ignoreExist=" + customize.ignoreExist; // 是否覆盖式导入：true:false
    }
    if (customize.together === false || customize.together) {
        url += "&together=" + customize.together; // 是否完整性导入（即不允许部分导入）：true:false
    }
    if(customize.headerTL) { // 模板表头字段映射，适用于第三方系统导出的数据导入至TSS数据表，映射表以外的字段则被忽略不进行导入
        url += "&headerTL=" + customize.headerTL; // eg: headerTL = 订单号:订单编码,货品:sku,数量:qty;
    }
    if(customize.otherConfig ) {
        url += customize.otherConfig;
    }

    var importDiv = createImportDiv(url);
    tssJS(importDiv).show();
}

function abstractCreateImportDiv(customSubmission){
    var remark = "请点击图标选择Excel文件导入";
    function checkFileWrong(subfix) {
        return subfix != ".csv" && subfix != ".xls" && subfix != ".xlsx";
    }
    var importDiv = $1("importDiv");
    if( importDiv == null ) {
        importDiv = tssJS.createElement("div", null, "importDiv");    
        document.body.appendChild(importDiv);

        var str = [];
        str[str.length] = "<form id='importForm' method='post' target='fileUpload' enctype='multipart/form-data'>";
        str[str.length] = "  <div class='fileUpload'> <input type='file' name='file' id='sourceFile' onchange=\"$('#importDiv h2').html(this.value)\" /> </div> ";
        str[str.length] = "  <input type='button' id='importBt' value='确定导入' class='tssbutton blue'/> ";
        str[str.length] = "</form>";
        str[str.length] = "<iframe style='width:0; height:0;' name='fileUpload'></iframe>";

        tssJS(importDiv).panel(remark, str.join("\r\n"), false);
        tssJS(importDiv).css("height", "300px").center();
    } else {
        tssJS("#sourceFile").value("");
        tssJS('#importDiv h2').text(" - " + remark);
        tssJS(importDiv).show();
    }

    // 每次 importUrl 可能不一样，比如导入门户组件时。不能缓存
    tssJS("#importBt").click( function() { 
        var fileValue = $1("sourceFile").value;
        if( !fileValue ) {
             return tssJS("#importDiv h2").notice("请选择导入文件!");               
        }

        var index  = fileValue.lastIndexOf(".");
        var subfix = fileValue.substring(index).toLowerCase();
        if( checkFileWrong && checkFileWrong(subfix) ) {
           return tssJS("#importDiv h2").notice(remark);
        }

        customSubmission && customSubmission();
        
    } );

    // $1("sourceFile").click(); // 加了干扰 货主选择框
    return importDiv;
}

/* 创建导入Div: createImportDiv(checkFileWrong, url, startProgress) */
function createImportDiv(importUrl, callback) {
    return abstractCreateImportDiv(function(){
        var form = $1("importForm");
        form.action = importUrl;
        form.submit();

        callback && callback();
        tssJS(importDiv).hide();
    })
}

URL_IMP_PROGRESS = "/tss/auth/xdata/progress/";  // {code} GET
URL_CANCEL_IMP   = "/tss/auth/xdata/progress/";  // {code} DELETE
var callCount = 0;
function startProgress(progressCode, callback) {
    progressCode =  progressCode || userCode;

    tssJS.ajax({
        url: URL_IMP_PROGRESS + progressCode, 
        params: {}, 
        method: "GET",
        onresult: function() {
            var data = this.getNodeValue("ProgressInfo");
            if( data == 'not found') {
                return callCount++ > 10 ? null : setTimeout(function() { 
                    startProgress(progressCode, callback);
                }, 10*1000);
            }

            var progress = new tssJS.Progress(URL_IMP_PROGRESS, data, URL_CANCEL_IMP);
            progress.oncomplete = function() {
                this.hide();
                callCount = 0;
            }
            progress.start();
            callback && callback();
        }
    });
}
/* --------------------------------------------------------- 导入导出 End ------------------------------------------------------------------*/


//打开上传附件页面，title可不填
var globalValiable = {};
function uploadX(tableName, rowid, title,readonly) {
    globalValiable = {"tableId": tableName, "itemId": rowid};   
    tssJS.openIframePanel("if1", title || "管理附件", 710, 257, "../../modules/dm/recorder_upload.html" + (readonly ? '?readonly=true' : ''), true);
    $('#if1').css('z-index', 999999)
}

function manageAttach(lineId, table, readonly) { 
    uploadX( table, lineId ,'',readonly);
}

/** <<waiting>><<DONE>> dialog 内嵌页打开
 * @param string 打开的ele的id
 * @param string 目标url
 * @param string title名
 * @param number ele的宽
 * @param number ele的高
 * @return  
 * @example 
 * @version v001：20170720 HK new
 */
 function openEasyuiIframe(panelId, src, title, width, height , modal ,top ,left) {
    title = title || 'new';
    width = width || tssJS.getInner().width - 20;
    height = height || tssJS.getInner().height - 20;
    $panel = $('#' + panelId);
    $panel.remove();
    var panel = document.createElement("div")
    document.body.appendChild(panel);
    $panel = $(panel);
    // $panel.attr("closable",false);
    $panel.addClass("easyui-dialog");
    $panel.attr("id", panelId).attr("closed", false);
    $('#' + panelId).dialog({
        title: title,
        width: width,
        height: height,
        modal : modal,
        top : top,
        left : left
    })
    $panel.append('<iframe frameborder="0"></iframe>')
    $panel.find('iframe').attr('src', src).css('width', width - 15).css('height', height - 38);
}

/** <<waiting>><<TODO>> 打开页面，// 非tab格式下，在当前页面，打开面板； tab格式下，新增tab页
 * @param stirng type 服务类型，即id对应的类型，url=网页、rec=录入表id&参数、rep=报表id&参数
 * @param string name 服务名称，即id对应的文字解释，tab名称或panel名称
 * @param string id 服务，可以是网页、数字、数字&参数
 * @return 打开新的tab页或panel页
 * @example addTab("rec", RECORDINFO[0].name, RECORDINFO[0].id );
 * @version v001：20180601GNILUW 
 */
function addTab(type, name, tableName, panelId, width, height) {
    width = width || 1200;
    height = height || 600;
    panelId = panelId || "panelId";
    var url;
    if (type == "url") { /*网页*/
        url = tableName;
    } else if (type == "rec") { /*录入表*/
        url = TOMCAT_URL + "/modules/dm/recorder.html?rctable=" + tableName;
    } else if (type == "rep") { /*报表*/
        url = TOMCAT_URL + "/modules/dm/report_portlet.html?leftBar=true&name=" + tableName;
    }

    if (window.parent.parent.document.getElementById("tabs") !== null) {
        window.parent.parent.addTab(name,url);
    } 
    else if (window.parent.document.getElementById("tabs") !== null) {
        window.parent.addTab(name,url);
    } 
    else {
        openEasyuiIframe(panelId,url,name,width,height);       
    }
}

// 当colList1不存在时，colList根据逗号分隔的field根据自身数据合并单元格。
// 当colList1存在时，colList1根据逗号分隔的field根据colList用竖杠分隔的一个或多个field数据合并单元格。
function mergeCellsByField(tableID, colList,colList1) {
    if(colList1){
        var ColArray = colList.split("|");
        var ColArray1= colList1.split(",");
    }
    else{
        var ColArray = colList.split(",");
        var ColArray1 = colList.split(",");
    }
    var tTable = $("#" + tableID);
    var TableRowCnts = tTable.datagrid("getRows").length;
    var tmpA;
    var tmpB;
    var PerTxt = "";
    var CurTxt = "";
    var alertStr = "";
    for (j = ColArray1.length - 1; j >= 0; j--) {
        PerTxt = "";
        tmpA = 1;
        tmpB = 0;

        for (i = 0; i <= TableRowCnts; i++) {
            if (i == TableRowCnts) {
                CurTxt = "";
            }
            else {
                if(colList1){
                    CurTxt = "";
                    for(k = 0; k< ColArray.length;k++){
                        CurTxt = CurTxt + tTable.datagrid("getRows")[i][ColArray[k]];
                    };
                }
                else{
                    CurTxt = tTable.datagrid("getRows")[i][ColArray[j]];
                }
                
            }
            if (PerTxt == CurTxt) {
                tmpA += 1;
            }
            else {
                tmpB += tmpA;
                
                tTable.datagrid("mergeCells", {
                    index: i - tmpA,
                    field: ColArray1[j],　　//合并字段
                    rowspan: tmpA,
                    colspan: null
                });
                
                tmpA = 1;
            }
            PerTxt = CurTxt;
        }
    }  
}

function checkTel(num){
    return num.indexOf('-')>=0 ? isTel(num) : isPhone(num);
}
function isPhone(num) {
    return /^[1][3,4,5,6,7,8,9][0-9]{9}$/.test(num);
}
function isTel(str) {
    return /0\d{2,3}-\d{7,8}$/.test(str);
}

function checkEmail(str) {
    return /^[a-zA-Z0-9_-]+@([a-zA-Z0-9]+\.)+(com|cn|net|org)$/.test(str);
}

function tooltip(value) {
    if (value) {
        return "<span title='" + value + "'>" + value + "</span>";
    }
}

$.fn.datagrid &&  $.extend($.fn.datagrid.methods, {  
    getEditingRowIndexs: function(jq) {
        var rows = $.data(jq[0], "datagrid").panel.find('.datagrid-row-editing');
        var indexs = [];
        rows.each(function(i, row) {
            var index = row.sectionRowIndex;
            if (indexs.indexOf(index) == -1) {
                indexs.push(index);
            }
        });
        return indexs;
    },
    addEditor: function(jq, param) { 
        if (param instanceof Array) { 
            $.each(param, function(index, item) { 
                var e = $(jq).datagrid('getColumnOption', item.field); 
                e.editor = item.editor; }); 
        } else { 
            var e = $(jq).datagrid('getColumnOption', param.field); 
            e.editor = param.editor; 
        } 
    }, 
    removeEditor: function(jq, param) { 
        if (param instanceof Array) { 
            $.each(param, function(index, item) { 
                var e = $(jq).datagrid('getColumnOption', item); 
                e.editor = {}; 
            }); 
        } else { 
            var e = $(jq).datagrid('getColumnOption', param);
            e.editor = {}; 
        } 
    }
});

// 将datagrid id为tid的选中行的field字段批量修改为value, 修改的后台表id为recordId
function batchUpdate(tid, field, value,recordId,callback) {
    var result=[];
    var rows = $('#'+tid).datagrid('getSelections');

    if(!rows) {
        return alert("你没有选中任何记录，请勾选后再进行批量操作。");
    }
    else if(rows.length >= 1000) {
        return alert("单次批量操作行数不能超过999行。")
    }
    else{
        result.push("id," + field);
        for(var i = 0; i < rows.length; i++){
            result.push(rows[i].id+','+value)           
        }  
        $.post( record_urls(recordId).CUD, {"csv": result.join("\n")}, function(data) {
            if(data.created || data.updated) {
                $.messager.show({
                    title: '提示',
                    msg: '批量处理成功！'
                });  
                callback ? callback() : $('#'+tid).datagrid('reload');                              
            }
        });
    }    
}

//datagrid反选
function unselectRow(tableId) {  
    var tableId = tableId || 't1';
    var s_rows = $.map($('#' + tableId).datagrid('getSelections'),  
        function(n) {  
            return $('#' + tableId).datagrid('getRowIndex', n);  
        });  
    $('#' + tableId).datagrid('selectAll');  
    $.each(s_rows, function(i, n) {  
        $('#' + tableId).datagrid('unselectRow', n);  
    }); 
}

function loginName2userName(value, row) {
    row._userName = userCode2userName(value);
    return '<span title = "' + (row._userName||'') + '">' + (row._userName||'') + '</span>'
}

function userCode2userName(code) {
    var codes = (code||"").split(","), names = [];
    codes.each(function(i, userCode) {
        userCode = userCode.trim();
        userCode && userPartner[userCode] && names.push( userPartner[userCode] );
    });

    return names.join(",");
}

function stylerCursor(value, row, index) {
    return 'cursor: pointer; color: blue; text-decoration: underline';
} 

function getFormData(formId) {
    var params = {};
    var t = $("#" + (formId ||'fm') ).serializeArray();
    $.each(t, function() {
        if( params[this.name] ) {
            params[this.name] += ',' + this.value;
        }
        else {
            params[this.name] = this.value;
        }     
    });
    return params;
}

/*
    实例：
    var data = {
            first: { value: '您的订单已提交成功', color: '#173177'},
            keyword1: { value: 'CX31806300002' },
            keyword2: { value: '2019-01-08 11:40:00' },
            keyword3: { value: '16吨书籍' },
            keyword4: { value: '姓名：张三 电话：13800138000' },
            keyword5: { value: '车牌：粤B88888 车型：厢式车' },
            remark: { value: '点击查看详情。客服电话：400-888-8888' }
        },miniprogram = {
            appid: 'wx5255074da90a4dca', -- 小程序ID
            pagepath: 'pages/homepage/index' -- 页面
        }
    var params_ = {
        appid:'wx784c62545bddf62b', -- 公众号ID
        phone:'13735547815', -- 接收方phone
        template_id:'K1uGpcq0oLh3tdYPle5Ciemjlg3KCDMu9YG51jW9_S4',  -- 模版ID
        miniprogram:miniprogram, -- 跳转小程序参数
        url:url, -- 跳转的网页地址
        data:data -- 模版内容
    }
*/
function sendGZHMsg(params, callback) {
    params.data = JSON.stringify(params.data).replace(/#/g,"%23"), 
    params.miniprogram = params.miniprogram ? JSON.stringify(params.miniprogram) : {};
    var url = SERVICE.sendgzhmsg + restfulParams(params).replace(/{/g,"%7B").replace(/}/g,"%7D");
    tssJS.ajax({
        url: url + '&uName=BD0000&uToken=oKYA65HP9aMry2lgcQgyorxYXasU', 
        headers: {appCode:'BD'}, 
        method : "GET",
        success: function(data){
            callback && callback( data )
        } 
    })
}
function restfulParams(params){
    var queryString = "?";
    tssJS.each(params, function(key, value) {
        if( queryString.length > 1 ) {
            queryString += "&";
        }
        queryString += (key + "=" + value);
    });
    return queryString
}


var maskWidth  = $(window).width();
var maskHeight = $(window).height();
var maskHtml = "<div id='maskLoading' class='panel-body' style='z-index:1000;position:absolute;left:0;width:100%;height:" + maskHeight + "px;top:0;background-color:#ccc;'>";
maskHtml += "<div style='position:absolute;cursor:wait;left:" + ((maskWidth / 2) - 100) + "px;top:" + (maskHeight / 2 - 50) + "px;width:150px;height:40px;";
maskHtml += "padding:10px 5px 10px 30px;font-size:12px;border:0px solid #ccc;background-color:white;'>";
maskHtml += "页面加载中......";
maskHtml += "</div>";
maskHtml += "</div>";
document.write(maskHtml);
function closeMask() {
    $('#maskLoading').fadeOut('fast', function () {
        $(this).remove();
    });
}
var loadComplete;
$.parser.onComplete = function () {
    loadComplete && clearTimeout(loadComplete);
    loadComplete = setTimeout(closeMask, 500);
}