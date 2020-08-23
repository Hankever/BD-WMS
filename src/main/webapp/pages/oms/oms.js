var role_owner = "商家,仓库主管,仓储经理", 
    role_customer = "下单客户", 
    role_os = "商家,系统管理员"; 

var RECORD = {
    'wms_sku': { 'name': '货品表'},
    'wms_inv': { 'name': '库存表'},

    'oms_order': { 'name': '订单表'},
    'oms_order_mall': { 'name': '物流导入模板'},
    'oms_order_import': { 'name': '订单导入模板'},
    'oms_order_item': { 'name': '订单明细表'},
    'oms_edi_key': { 'name': '对接账号'},
    'oms_store': { 'name': '店铺渠道'},
    'oms_default_config': { 'name': '默认配置'},
    'oms_print_size': { 'name': '面单模板'}
}

var REPORT = {
    'region_province': '/tss/data/json/provinceList',
    'region_city': '/tss/data/json/cityList',
    'region_district': '/tss/data/json/districtList'
}

var SERVICE = {
    'sys_addr_aplit': '/tss/addr/splitNamePhoneAddress',
    'sendgzhmsg': '/tss/wx/api/sendgzhmsg',

    'oms_code_map': '/tss/api/bi/sql/codeMap',
    'oms_model_query': '/tss/api/bi/sql/queryModel',
    'oms_wh_manager': '/tss/api/bi/sql/queryWhManager',

    'oms_store_auth': '/tss/oms/api/store/auth',
    'oms_store_saveauth': '/tss/oms/api/store/saveAuth',
    'oms_print_ecode': '/tss/oms/api/ecode/print',
    'oms_print_pdf': '/tss/oms/api/ecode/pdf',
    'oms_model_add': '/tss/oms/api/ecode/model/add',
    'oms_pdf_draw': '/tss/api/print/draw',
    'oms_pdf_down': '/tss/api/print/down',
    'oms_order_towms': '/tss/oms/api/towms',
    'oms_order_tracks': '/tss/oms/api/tracks/'
}

tssJS.each(RECORD, function(key, item) {
    item.URL = record_urls(key);
    item.table = key;
});
tssJS.each(REPORT, function(key, item) {
    REPORT[key] = item || BASE_JSON_URL + key
});
tssJS.each(SERVICE, function(key, item) {
    SERVICE[key] = item;
});

var SKU    = RECORD.wms_sku.URL,
    INV    = RECORD.wms_inv.URL,
    ORDER  = RECORD.oms_order.URL,
    OITEMS = RECORD.oms_order_item.URL,
    SKU_L, SKU_ON = [], SKU_M = {}, SKU_C = {};
    
var CODE_MAP = {}, CODE_MAP_LIST = {}, SITES = {};
var CODE_LIST_PLATFORM = [], CODE_LIST_CARRIER  = [],
    CODE_LIST_EXPTYPE  = [], CODE_LIST_PAYTYPE  = [],
    CODE_LIST_PACK     = [], CODE_LIST_DELIVERY = [],
    CODE_LIST_CURRENCY = [], CODE_LIST_SIZE     = [];

var CONFIG_TYPE_DOMAIN = "domain";
var EDI_KEY_TYPE_STORE  = "1", EDI_KEY_TYPE_PLATFORM  = "2", EDI_KEY_TYPE_SITE  = "3";
var STATUS_ENABLE = "1", STATUS_DISABLE = "0";
var CODE_TYPE_PLATFORM = "1", CODE_TYPE_CARRIER  = "2", 
    CODE_TYPE_EXPTYPE  = "3", CODE_TYPE_PAYTYPE  = "4", 
    CODE_TYPE_PACK     = "5", CODE_TYPE_DELIVERY = "6", 
    CODE_TYPE_CURRENCY = "7", CODE_TYPE_SIZE     = "8";
var STORE_TYPE_ONLINE = "1", STORE_TYPE_OFFLINE = "2";
var CARRIER_TYPE_KUAIDI = "1", CARRIER_TYPE_KUAIYUN = "2";

var ORDER_STATUS_LIST = ["新建", "待付款", "已付款", "已发货", "待收货", "已收货", "已取消"], ORDER_STATUS = [];
var VERIFY_STATUS_LIST = ["待审核", "通过", "拒绝"], VERIFY_STATUS = [];
ORDER_STATUS_LIST.each(function(i, item){
    ORDER_STATUS.push({"name": item});
});

VERIFY_STATUS_LIST.each(function(i, item){
    VERIFY_STATUS.push({"name": item});
});


var STORE_TYPE_LIST = [], STORE_TYPE = {"TAOBAO": "淘宝"};
for(key in STORE_TYPE){
    var s = {"name": STORE_TYPE[key], "value": key};
    STORE_TYPE_LIST.push(s);
}

function formatStorePlatform(value, row, index){
    var v = STORE_TYPE[value];
    if(v){
        return v;
    }
    return value;
}

var orderTable = RECORD.oms_order.table;

var FIELDS_SKU = [
    {field: "ck", checkbox: true},
    {field: "id", title: "货品ID", hidden: true},
    {field: "name", title: "货品名称", width: 300},
    {field: "barcode", title: "货品条码", width: 120},
    {field: "guige", title: "规格", width: 80},
    {field: "uom", title: "包装", width: 45},
    {field: "inv_qty", title: "库存", width: 70},
    {field: "price", title: "单价", width: 50},
    {field: "shelflife", title: "有效期", width: 60},
    {field: "category", title: "分类", width: 90},
    {field: "supplier", title: "供应商", width: 70}
];

$.each(FIELDS_SKU, function(i, field) {
    field.align = field.align || "center";
    field.width = field.width || "12%";
});

function prepareSKUs(callback){
    $.getJSON(SKU.QUERY, {}, function(data) {
        SKU_L = data;
        SKU_L.each(function(i, item) {
            SKU_M[item.id] = item;
            SKU_C[item.code] = item;
            if(item.status == 1) {
                SKU_ON.push(item);
            }
        });

        callback && callback();
    });
}

function querySKUs( isQuery ) {
    var params = {"status": 1};
    if(isQuery) {
        params.barcode = $('#_barcode').textbox("getValue");
        params.name = $('#_skuname').textbox("getValue");
        params.category = $('#_category').textbox("getValue");
        params.brand = $('#_brand').textbox("getValue");
        params.supplier = $('#_supplier').textbox("getValue");

        dg3 = $('#t3').datagrid({
            url: SKU.QUERY, 
            queryParams: params, // 参数
            fit: true,
            pagination: true,
            pageSize : 100,
            pageList: [100, 200, 500],
            checkOnSelect: true,
            selectOnCheck: true,
            columns: [FIELDS_SKU],
            loadFilter: filterMoneySKU
        });
    } 
    else {
        dg3 = $('#t3').datagrid({
            data: SKU_ON,
            fit: true,
            pagination: true,
            pageSize : 100,
            pageList: [100, 200, 500],
            checkOnSelect: true,
            selectOnCheck: true,
            columns: [FIELDS_SKU],
            loadFilter: filterMoneySKU
        });
    }
}

function selectSKUs(target) {
    var skus = dg3.datagrid("getSelections");
    $.each(skus, function(i, sku) {
        var item = {};
        item.sku_id = sku.id;
        item.sku = sku.name;
        item.skucode = sku.barcode;
        item.brand = sku.brand;
        item.supplier = sku.supplier;
        item.guige = sku.guige;
        item.price = sku.price2 || sku.price || sku.price1;
        item.qty = 0;
        item.qty_actual = 0;
        item.uom = sku.uom;
        item.shelflife = sku.shelflife;
        item.opts = '<a href="javascript:void(0)" style="text-decoration: underline;">删 除</a>';

        target.datagrid("appendRow", item);
    });

    dg3.datagrid("unselectAll");
    $('#dlg2').dialog('close');
}

// 根据明细行的金额，重新计算订单总金额
function fixOrderMoney(i_qty, i_money, o_qty, o_money) {
    i_qty = i_qty || 'qty';
    o_qty = o_qty || 'qty';
    i_money = i_money || 'money';
    o_money = o_money || 'money';

    var money = 0, qty = 0;
    var rows = dg2.datagrid('getRows');
    $.each(rows, function(i, row) {
        money += parseFloat(row[i_money]||0);
        qty += parseInt(row[i_qty]||0);
    });

    var row = dg1.datagrid('getSelected');
    row[o_money] = money.toFixed(2);
    row[o_qty] = qty;
    dg1.datagrid('refreshRow', dg1_selected);
}

function formatStoreType(value, row, index){
    if(value == STORE_TYPE_ONLINE){
        return "线上";
    }
    else{
        return "线下";
    }
}

function formatStatus(value, row, index){
    if(value == STATUS_ENABLE){
        return "启用";
    }
    else{
        return "停用";
    }
}

function formatAccountType(value, row, index){
    if(value == EDI_KEY_TYPE_STORE){
        return "店铺";
    }
    else if(value == EDI_KEY_TYPE_PLATFORM){
        return "平台";
    }
    else if(value == EDI_KEY_TYPE_SITE){
        return "网点";
    }
    else{
        return value;
    }
}

function fieldAlign(fields, align){
    fields.each(function(i, item){
        item.align = align;
    });
    return fields;
}

function radioCheck(name, id){
    var obj = document.getElementsByName(name);
    for(var i = 0; i < obj.length; i++){
        if(obj[i].id == id){
            obj[i].checked = true;
        }
        else{
            obj[i].checked = false;
        }
    }
}

function getChangeRadio(name){
    return $("input[type='radio'][name='" + name + "']:checked").val();
}

function openDialog(dlg, fm, btn, title){
    $("#" + btn).linkbutton("enable");
    $("#" + dlg).dialog({"modal": true}).dialog("open").dialog("setTitle", title).dialog("center");
    $("#" + fm).form("clear");
}

function fetchCodeMap(){
    var p = new Promise(function(resolve, reject){
        $.get(SERVICE.oms_code_map, {sys:'1'}, function(data){
            if(data.errorMsg){
                reject(data.errorMsg);
            }
            else{
                CODE_MAP = {};
                CODE_MAP_LIST = {};

                data.each(function(i, item){
                    if(!CODE_MAP[item.type]){
                        CODE_MAP[item.type] = {};
                        CODE_MAP_LIST[item.type] = [];
                    }
                    CODE_MAP[item.type][item.value] = item.text;
                    if(item.status == STATUS_ENABLE){
                        CODE_MAP_LIST[item.type].push(item);
                    }
                });

                CODE_LIST_PLATFORM = CODE_MAP_LIST[CODE_TYPE_PLATFORM];
                CODE_LIST_CARRIER  = CODE_MAP_LIST[CODE_TYPE_CARRIER];
                CODE_LIST_EXPTYPE  = CODE_MAP_LIST[CODE_TYPE_EXPTYPE]; 
                CODE_LIST_PAYTYPE  = CODE_MAP_LIST[CODE_TYPE_PAYTYPE];
                CODE_LIST_PACK     = CODE_MAP_LIST[CODE_TYPE_PACK]; 
                CODE_LIST_DELIVERY = CODE_MAP_LIST[CODE_TYPE_DELIVERY];
                CODE_LIST_CURRENCY = CODE_MAP_LIST[CODE_TYPE_CURRENCY]; 
                CODE_LIST_SIZE     = CODE_MAP_LIST[CODE_TYPE_SIZE];
                resolve(data);
            }
        });
    });
    return p;
}

function formatPlatform(value, row, index){
    return formatCode(CODE_TYPE_PLATFORM, value);
}

function formatCarrier(value, row, index){
    return formatCode(CODE_TYPE_CARRIER, value);
}

function formatExp(value, row, index){
    return formatCode(CODE_TYPE_EXPTYPE, value);
}

function formatPack(value, row, index){
    return formatCode(CODE_TYPE_PACK, value);
}

function formatDelivery(value, row, index){
    return formatCode(CODE_TYPE_DELIVERY, value);
}

function formatPay(value, row, index){
    return formatCode(CODE_TYPE_PAYTYPE, value);
}

function formatCurrency(value, row, index){
    return formatCode(CODE_TYPE_CURRENCY, value);
}

function formatPrintSize(value, row, index){
    return formatCode(CODE_TYPE_SIZE, value);
}

function formatCode(type, value){
    if(!CODE_MAP[type]){
        return value;
    }
    if(!CODE_MAP[type][value]){
        return value;
    }
    return CODE_MAP[type][value];
}

function getSite(){
    var p = new Promise(function(resolve, reject){
        $.post(RECORD.oms_edi_key.URL.QUERY, {type: EDI_KEY_TYPE_SITE}, function(data){
            if(data.errorMsg){
                reject(data.errorMsg);
            }
            else{
                data.each(function(i, item){
                    SITES[item.id] = item;
                });
                resolve(data);
            }
        });
    });
    return p;
}

function showTab(name, src, params){
    parent.$('#tabs').tabs('close', name);
    setTimeout(function(){
        addTab('url', name, src + (params || ''));
    }, 100);
}

function concatBlank(list){
    var blank = [{"value":"", "text":""}];
    return blank.concat(list);
}