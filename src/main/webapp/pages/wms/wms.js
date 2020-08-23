var role_manager = "仓储经理",
    role_keeper  = "仓库主管",
    role_cs      = "仓库文员",
    role_owner   = "货主客户",
    role_operater= "作业人员",
    role_operater_id = 5,
    role_supplier= "供货商";

var TIME_SPAN = 100; // 默认查询近N天的订单

var RECORD = {
    /* WMS */
    'wms_sku': { 'name': '货品表'},
    'wms_inv': { 'name': '库存表'},
    'wms_order': { 'name': '订单表'},
    'wms_order_item': { 'name': '订单明细表'},
    'wms_asn': { 'name': '入库表'},
    'wms_asn_item': { 'name': '入库单明细表'},
    'wms_warehouse': { 'name': '仓库表'},
    'wms_location': {'name': '位置'},
    'wms_owner': {'name': '货主'},
    'wms_operation': {'name': '库内作业单'},
    'wms_operation_item': {'name': '库内作业明细'},
    'wms_order_wave': {'name': '订单波次'},
    'wms_box': {'name': '出库箱'},
    'wms_inv_sn': {'name': '序列号'},
    'wms_rule': {'name': '规则配置'},
    'dm_record_field': {'name': '字段自定义'},
    'x_domain': {'name': '域信息'},
    'wmsx_customer': {'name': '往来对象'},
    'wms_inv_check': {'name': '库存盘点单'},
    'wms_op_exception': {'name': '作业单异常反馈'},
    'fms_invoice': {'name': '开票申请'},
    'oms_order': {'name': '电子面单'},
    'wmsx_bill_model': {'name': '计费规则'},
    'wmsx_bill_price': {'name': '计费单价规则'},
    'wmsx_bill_work': {'name': '劳务作业信息'}
}

var REPORT = {
    'wms_putaway':'',
    'wms_operation':'',
}

var SERVICE = {
    'sendgzhmsg': '/tss/wx/api/sendgzhmsg',
    
    'doc_code': '/tss/wms/api/coder',

    'asn_delete': '/tss/asn/delete',
    'asn_create_inbound': '/tss/wms/api/createAndInbound',
    'asn_inbound': '/tss/asn/inbound',
    'asn_ck_inbound': '/tss/asn/ck_inbound',
    'asn_cancel_inbound': '/tss/asn/cancel_inbound',
    'asn_cancel': '/tss/asn/cancel_asn',
    'asn_close': '/tss/asn/close',
    'asn_putaway_info': '/tss/asn/putawayInfo',
    'asn_pre_putaway': '/tss/asn/prePutaway',
    'asn_putaway': '/tss/asn/putaway',
    'asn_assign': '/tss/asn/assignAsn',

    'sku_list': '/tss/api/bi/sql/skuList',
    'inv_locked_info': '/tss/api/bi/sql/queryLockedInfo',
    'inv_unlock': '/tss/inv/unlock/', 
    'inv_opi': '/tss/inv/opi/',    /* /opi/{opiId}/{toInvId} */
    'inv_query': '/tss/inv/query',
    'inv_adjust': '/tss/inv/adjust',
    'inv_check_create': '/tss/inv/createInvCheck',
    'inv_check_cancel': '/tss/inv/cancelInvCheck',
    'inv_check_close': '/tss/inv/closeInvCheck',
    'inv_check_submit': '/tss/inv/submitInvCheck',
    'inv_check_save': '/tss/inv/saveInvCheck',
    'inv_check_query': '/tss/api/bi/invCheck',
    'inv_day_io': '/tss/api/bi/dayInvIO',
    'inv_current': '/tss/api/bi/currentInv',


    "order_create_outbound": '/tss/wms/api/createAndOutbound',
    "order_cancel": '/tss/wmsorder/cancel/',
    "order_cancelAllocate": '/tss/wmsorder/cancelAllocate/',
    'order_delete': '/tss/wmsorder/',
    "order_pickup": '/tss/wms/api/pickup/',
    "order_cancelPickup": '/tss/wms/api/pickup/cancel/',
    'order_outbound': '/tss/wmsorder/outbound/',
    'order_pickupOutbound': '/tss/wmsorder/pickupOutbound/',
    'order_cancelOutbound': '/tss/wmsorder/cancelOutbound/',
    'order_close': '/tss/wmsorder/close/',
    'order_assign': '/tss/wmsorder/assignOrder',
    'order_sealing': '/tss/wmsorder/sealing',
    'order_cancel_sealing': '/tss/wmsorder/cancelSealing',
    'order_query_box_item': '/tss/wms/api/queryBoxItem',
    'order_pallet': '/tss/wmsorder/palletOrders',
    'order_palletOutbound': '/tss/wmsorder/palletOutbound',

    'order_allocate': '/tss/wmsorder/allocate',
    'order_pkd_op': '/tss/wms/api/pk_operation',
    'order_check': '/tss/wmsorder/check',

    'wave_create': '/tss/wave/create',
    'wave_cancel': '/tss/wave/cancel',
    'wave_allocate': '/tss/wave/allocate',
    "wave_cancelAllocate": '/tss/wave/cancelAllocate/',
    'wave_order_change': '/tss/wave/change',
    'wave_assign': '/tss/wave/assign',
    'wave_split_pkh': '/tss/wave/split_pkh',
    'wave_check': '/tss/wave/check',

    'opException': '/tss/api/bi/opException',
    'opCheckException': '/tss/wms/api/opCheckException',
    'iq': '/tss/api/bi/getSKUs4ABC',
    'abc_save': '/tss/bi/abc',
    'wave_order_item': '/tss/api/bi/waveOrderItem',
    'wave_order_sum': '/tss/api/bi/waveOrderSum',
    'jobRate': '/tss/api/bi/jobRate',
    'jobWorkQty': '/tss/api/bi/jobWorkQty',

    'fms_asn_query': '/tss/api/bi/asnAndItem',
    'fms_order_query': '/tss/api/bi/orderAndItem',
    'fms_asn_save': '/tss/fms/api/asn_save',
    'fms_order_save': '/tss/fms/api/order_save',
    'fms_create_inbound': '/tss/fms/api/createAndInbound',
    'fms_create_outbound': '/tss/fms/api/createAndOutbound',
    'fms_asn_inbound': '/tss/fms/api/inbound',
    'fms_order_outbound': '/tss/fms/api/outbound',
    'fms_loc_inbound': '/tss/fms/api/initAccount',
    'fms_cancel_inbound': '/tss/fms/api/cancel_inbound',
    'fms_cancel_outbound': '/tss/fms/api/cancel_outbound',
    'fms_asn_close': '/tss/fms/api/asn_close',
    'fms_order_close': '/tss/fms/api/order_close',
    'fms_asn_delete': '/tss/fms/api/asn_delete',
    'fms_order_delete': '/tss/fms/api/order_delete',

    'bill_model': '/tss/api/bi/billModel',
    'bill_model_owner': '/tss/api/bi/billModelOwner',
    'bill_model_wh': '/tss/api/bi/getbillWH',
    'bill_model_company': '/tss/api/bi/billCompany',
    'bill_model_money': '/tss/bill_model/getBillMoney',

    'oorder_by_asn': '/tss/oms/api/oorderByAsn'
}

var units = [
    [{text: '吨', value: '吨'}, {text: '立方', value: '立方'}, {text: '平方', value: '平方'}, {text: '箱', value: '箱'}, {text: '托', value: '托'}, {text: '板', value: '板'}, {text: '件', value: '件'}, {text: '单', value: '单'}],
    [{text: '天', value: '天'}, {text: '月', value: '月'}, {text: '年', value: '年'}, {text: '小时', value: '小时'}],
    [{text: '人', value: '人'}, {text: '车', value: '车'}, {text: '间', value: '间'}, {text: '次', value: '次'}]
];

var miniprogram = {
    appid: 'wx6f5e8e3f5b6b45d8', // 小程序ID
    pagepath: 'pages/wms/homepage/index'  // 页面
}, 
gzh_app_id = 'wxd1e32997a92e1630',
gzh_msg_id = ['I77ACtoRkHhqqkq3VWxM72JHvZZs-1KLtHGTVWkDUiU', '--Lno8MdbLVYwvxtADryKqXBDsR3c90eNJ_ltKbiazo'];

tssJS.each(RECORD, function(key, item) {
    item.URL = record_urls(key);
    item.table = key;
});
tssJS.each(REPORT, function(key, item) {
    REPORT[key] = item || BASE_JSON_URL + key
})
tssJS.each(SERVICE, function(key, item) {
    SERVICE[key] = item
})

var SKU    = RECORD.wms_sku.URL,
    INV    = RECORD.wms_inv.URL,
    ORDER  = RECORD.wms_order.URL,
    OITEMS = RECORD.wms_order_item.URL,
    ASN    = RECORD.wms_asn.URL,
    AITEMS = RECORD.wms_asn_item.URL,
    WAREHOUSE = RECORD.wms_warehouse.URL,
    OPERATION = RECORD.wms_operation.URL,
    OPERATIONITEM = RECORD.wms_operation_item.URL,
    ORDERWAVE = RECORD.wms_order_wave.URL,
    OWNER = RECORD.wms_owner.URL,
    LOC = RECORD.wms_location.URL,
    BOX = RECORD.wms_box.URL, 
    RECORD_FIELD = RECORD.dm_record_field.URL,
    CUSTOMER = RECORD.wmsx_customer.URL,
    SN = RECORD.wms_inv_sn.URL,
    EXCEPTION = RECORD.wms_op_exception.URL,
    INVOICE = RECORD.fms_invoice.URL,
    OMSORDER = RECORD.oms_order.URL,
    INVCHECK = RECORD.wms_inv_check.URL,
    BILL_MODEL = RECORD.wmsx_bill_model.URL,


    SKU_L, SKU_M = {}, SKU_C = {}, SKU_BAR = {}, SKU_BAR2 = {},
    LOC_D, LOC_I = {}, LOC_C = {},
    WAREHOUSE_D, WAREHOUSE_I = {}, WAREHOUSE_C = {}, WAREHOUSE_N = {},
    MY_WAREHOUSE_D, MY_WAREHOUSE_I = {}, MY_WAREHOUSE_C = {},
    WAREHOUSE_FMS = [], WAREHOUSE_WMS = [],
    OWNER_D, OWNER_I = {},OWNER_C = {},
    CUSTOMER_D, CUSTOMER_I = {},
    OPTYPE_D, OPTYPE_I= {}, OPTYPE_N = {}, OPERATION_TYPE_I = {}, OPERATION_TYPE_T = {}, OPERATION_TYPE_D = {};

var orderTable = RECORD.wms_order.table,
    asnTable = RECORD.wms_asn.table,
    operationTable = RECORD.wms_operation.table,
    invCheckTable = RECORD.wms_inv_check.table;

var FIELDS_SKU = [
    {field: 'ck', checkbox: true},
    {field: "id", title: "货品ID", hidden: true},
    {field: "code", title: "货品编码", width: 120},
    {field: "name", title: "货品名称", width: 160},
    {field: "barcode", title: "条码", width: 100},
    {field: "uom", title: "单位", width: 60},
    {field: "price", title: "单价", width: 55},
    {field: "shelflife", title: "有效期", width: 60},
    {field: "category", title: "大类", width: 90},
    {field: "brand", title: "品牌", width: 70},
    {field: "guige", title: "规格", width: 70},
    {field: "udf1", title: "自定义1", width: 70}
];
var FIELDS_INV = [
    {field: 'ck', checkbox: true},
    {field: "skucode", title: "货品编码", width: "8%"},
    {field: "sku_id", title: "货品名称", width: "15%", formatter:skuid2name},
    {field: "barcode", title: "货品条码", width: "11%"},
    {field: "location_id", title: "库位", formatter:locid2name, styler: frozenStyler},
    {field: "owner_id", title: "货主", formatter:ownerid2name, hidden: hide_ow},    
    {field: "qty", title: "库存数量", width: "7%"},
    {field: "invstatus", width: "6%"},
    {field: "createdate"},
    {field: "expiredate"},
    {field: "lotatt01"},
    {field: "lotatt02"},
    {field: "lotatt03"},
    {field: "lotatt04"},
    {field: "uom", title: "单位", width: "5%"},
    {field: "category", title: "大类", width: "6%"},
    {field: "udf1", title: "自定义1", width: "5%"}
];

$.each(FIELDS_SKU, function(i, field) {
    field.align = field.align||"center";
    field.width = field.width||"10%";
});
$.each(FIELDS_INV, function(i, field) {
    field.align = field.align || "center";
    field.width = field.width || "9%";
});

function cursorHand(value, row, index){ 
    return 'cursor: pointer;'; 
}
function linkStyler(value,row,index) { 
    return 'cursor: pointer; text-decoration:underline;'; 
}
function hrefStyler(value,row,index) { 
    return 'background-color: rgb(254, 247, 169); cursor: pointer; text-decoration:underline;'; 
}
function highlightStyler(value,row,index) { 
    return 'background-color: #ffe48d; color: black;'; 
}
function frozenStyler(value, row, index) { 
    if( row.location && row.location.status == 0 ) {
        return 'cursor: pointer; background-color: #eee; text-decoration: line-through; '; 
    }
    else if( row.frozen ) {
        return 'cursor: pointer; background-color: #eee;'; 
    } 
    else {
        return 'cursor: pointer;'
    }
}
function expireStyler(value, row, index) { 
    if(row.expiredate && row.createdate && row.shelflife) {
        if( subDateS( toDate(row.createdate), row.shelflife*-1 ) != row.expiredate ) {
            return 'background-color: yellow;'; 
        }
    }
    if( row.expiredate && toDate(row.expiredate) < now ) {
        return 'background-color: #fb4848;'; 
    } 
    if(!row.expiredate && row.createdate && row.shelflife && subDate( toDate(row.createdate), row.shelflife*-1 ) < now) {
        return 'background-color: #fb4848;'; 
    }
}
function calExpireDate(item) {
    if(item.createdate && item.shelflife && !item.expiredate) {
        item.expiredate = subDateS( toDate(item.createdate), parseFloat(item.shelflife)*-1 );
    }
}
function unFinishedStyler(value, row, index) { 
    if( row.status != '已完成' && row.status != '关闭' ) {
        return 'background-color: rgb(254, 247, 169); cursor: pointer;'; 
    }
}

var hide_wh = parseInt( tssJS.Cookie.getValue('count_wh') ) == 1,
    hide_ow = parseInt( tssJS.Cookie.getValue('count_ow') ) == 1;

function prepareWHs(callback) {
    $.getJSON(WAREHOUSE.QUERY, {status: 1}, function(data) {
        WAREHOUSE_D = data;
        WAREHOUSE_FMS = [];
        WAREHOUSE_WMS = [];
        WAREHOUSE_D.each(function(i, item) {
            WAREHOUSE_I[item.id] = item;
            WAREHOUSE_C[item.code] = item;
            WAREHOUSE_N[item.name] = item;

            if( item.type == 2 ) {
                WAREHOUSE_FMS.push(item);
            } else {
                WAREHOUSE_WMS.push(item);
            }
        });
        callback && callback();
    });
}

function prepareOwners(callback){
    var params = {status: 1, sortField: 'code'};

    $.getJSON(OWNER.QUERY, params, function(data){
        OWNER_D = data;
        OWNER_D.each(function(i, item) {
            OWNER_I[item.id] = item;
            OWNER_C[item.code] = item;
        });
        callback && callback();
    })
}

function prepareOperationType(callback){
    $.getJSON('/tss/param/json/combo/OpType?KV=true&valField=id',{},function(data){
        OPERATION_TYPE_D = data;
        data.each((i, item) => {
            OPERATION_TYPE_I[item.value] = item;
            OPERATION_TYPE_T[item.text] = item;
        })
        callback && callback(data);
    })
}

function prepareLOCs(callback, params) {
    params = params || {};
    params.warehouse_id = SELECTED_WH;
    if( params.all_status || !params.status ) {
        delete params[status];
    } else {
        params.status = 1;
    }
    params.rows = 5000;
    
    $.getJSON(LOC.QUERY, params, function(d) {
        var loc_list = d.rows;
        prepareWHs(function(){            
            loc_list.length && loc_list.each(function(i, item) {
                item['warehouse_code'] = WAREHOUSE_I[item.warehouse_id].code;
                item['warehouse_name'] = WAREHOUSE_I[item.warehouse_id].name;
                LOC_I[item.id] = item;
                LOC_C[item.code] = item;
            });
            LOC_D = loc_list;
            callback && callback(); 
        })  
    });
}

function prepareSKUs(callback, p) {
    p = p || {};

    var params = { };
    params.wh_id = p.wh_id || SELECTED_WH;
    params.pagesize = 50000;

    if(p && p.codes) {
        params.codes = p.codes;
    }
    if(p && p.ids) {
        params.ids = p.ids;
    }
    if(p && p.category) {
        params.category = p.category;
    }

    $.getJSON(SERVICE.sku_list, params, function(data) {
        SKU_L = data;
        SKU_L.each(function(i, item) {
            SKU_M[item.id] = item;
            SKU_C[item.code] = item;
            SKU_BAR[item.barcode] = item;
            SKU_BAR2[item.barcode2] = item;
        });

        callback();
    });
}

function querySKUs( isQuery ) {
    isQuery = isQuery || SKU_L.length > 100;  // 超过一页数据

    var params = {};
    if(isQuery) {
        params.code = $('#_skucode').textbox("getValue");
        params.name = $('#_skuname').textbox("getValue");
        params.barcode = $('#_barcode').textbox("getValue");
        params.category = $('#_category').textbox("getValue");
        params.brand = $('#_brand').textbox("getValue");
        params.owner_id =  $('#_owner_id').textbox("getValue");
        params.udf1 = $('#_udf1').textbox("getValue");

        filterParams(params);

        dg3 = $('#t3').datagrid({
            url: SERVICE.sku_list, 
            method: 'GET',
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
        var skus = [];
        if( CURRENT_OWNER ) {
            SKU_L.each( function(i, sku) {
                if( !sku.owner_id || sku.owner_id == CURRENT_OWNER ) {
                    skus.push( sku );
                }
            } );
        } 
        else {
            skus = SKU_L;
        }

        dg3 = $('#t3').datagrid({
            data: skus,
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

// 过滤掉 货币 类商品
function filterMoneySKU( data ) {
    var _data = [];
    if( data.rows ) {
        data.rows.each(function(i, item) {
            if( item.category != '货币' ) {
                _data.push(item);
            }
        });
        data.rows = _data;

        return data;
    }
    else {
        data.each(function(i, item) {
            if( item.category != '货币' ) {
                _data.push(item);
            }
        });
        return _data;
    }
}

function selectSKUs(target, callback) {
    var skus = dg3.datagrid("getSelections");
    $.each( skus, function(i, sku) {
        var item = {};
        item.sku_id = sku.id;
        item.name = sku.name;
        item.skuname = sku.name;
        item.code = sku.code;
        item.skucode = sku.code;
        item.brand = sku.brand;
        item.guige = sku.guige;
        item.price = sku.price2 || sku.price || sku.price0 || 0;
        item.qty = 0;
        item.qty_actual = 0;
        item.uom = sku.uom;
        item.shelflife = sku.shelflife;
        item.owner_id = sku.owner_id;
        item.opts = '<a href="javascript:void(0)" style="text-decoration: underline;">删 除</a>';

        target.datagrid("appendRow", item);
    } );

    dg3.datagrid("unselectAll");
    $('#dlg2').dialog('close');
    callback && callback()
}

function queryInvHistory(wh_id, inv_id, sku_id, location_id) {
    var width = document.body.clientWidth * 0.96,
        height = document.body.clientHeight * 0.85;

    var params = "wh_id="+wh_id;
    if(inv_id) {
        params += "&inv_id="+inv_id;
    }
    if(sku_id) {
        params += "&sku_id="+sku_id;
    }
    if(location_id) {
        params += "&location_id="+location_id;
    }

    openIframe("p1", "/tss/pages/wms/inv_history.html?" + params, "库存历史查看", {'width': width, 'height': height} ); 
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
        qty += parseFloat(row[i_qty]||0);
    });

    var row = dg1.datagrid('getSelected');
    row[o_money] = money.toFixed(2);
    row[o_qty] = qty.toFixed(_qty_precision);
    dg1.datagrid('refreshRow', dg1_selected);
}

function prepareCustomers(callback){
    var params = {sortField: 'id'};

    $.getJSON(CUSTOMER.QUERY, params, function(data){
        CUSTOMER_D = data;
        CUSTOMER_D.each(function(i, item) {
            CUSTOMER_I[item.id] = item;
        });
        callback && callback();
    })
}

function ownerid2name(value,row){
    if(value){
        var showValue = OWNER_I[value] ? OWNER_I[value].name : value;
        row.owner_name = showValue;
        return '<span title="' + showValue + '">' + showValue + '</span>'
    }
}

function formateTypeByOwner(value,row){
    var owner_id = row.owner_id;
    var showValue = OWNER_I[owner_id] ? (OWNER_I[owner_id].type || '') : '';
    return '<span title="' + showValue + '">' + showValue + '</span>'
}

function warehouseid2name(value){
    if(value){
        var showValue = WAREHOUSE_I[value] ? WAREHOUSE_I[value].name : value;
        return '<span title="' + showValue + '">' + showValue + '</span>'
    }
}

function loccode2name(value){
    if(value){
        var showValue = LOC_C[value] ? LOC_C[value].name : value;
        return '<span title="' + showValue + '">' + showValue + '</span>'
    }
}

function locid2name(value){
    if(value){
        var showValue = LOC_I[value] ? LOC_I[value].code : value;
        return '<span title="' + showValue + '">' + showValue + '</span>'
    }
}

function locid2name2(value){
    if(value){
        var showValue = LOC_I[value] ? LOC_I[value].name : value;
        return '<span title="' + showValue + '">' + showValue + '</span>'
    }
}

function locid2name3(value) {
    var loc = LOC_I[value];
    if( loc ) {
        var showValue  = loc.code;
        if( loc.parent_id && LOC_I[loc.parent_id] ) {
            showValue = LOC_I[loc.parent_id].code + ' / ' + showValue;
        }
        return '<span title="' + showValue + '">' + showValue + '</span>'
    }
}

function skuid2name(value){
    if(value){
        var showValue = SKU_M[value] ? SKU_M[value].name : value;
        return '<span title="' + showValue + '">' + showValue + '</span>'
    }
}

function skuCode2Name(value,row){
    var sku = SKU_C[row.skucode]
    if(sku){
        return '<span title = "' + sku.name + '">' + sku.name + '<span>'
    }
}

function optypeid2name(value){
    if(value){
        var showValue = OPERATION_TYPE_I[value] ? OPERATION_TYPE_I[value].text : value;
        return '<span title="' + showValue + '">' + showValue + '</span>'
    }
}

function customerid2name(value,row){
    if(value){
        var showValue = CUSTOMER_I[value] ? CUSTOMER_I[value].name : value;
        return '<span title="' + showValue + '">' + showValue + '</span>'
    }
}

function dateNoSecond(value){
    if(value && value.length == 19){
        return value.substring(0, 16);
    }
    return value;
}
function dateNoHour(value){
    if(value && value.length == 19) {
        return value.substring(0, 10);
    }
    return value;
}

function excludeZero(value) {
    return value == 0 ? null : value; 
}

function round(num, precision) {
    precision = precision || 0;
    var k = 1;
    for( var n = 0; n < precision; n++ ) {
        k = k * 10;
    }

    return Math.round(num * k) / k;
}

function formatThous(num) {  
    if( !num ) return '-';
    return tssJS.Data.number_format(num, 2);  
}

function getMoneyByUnit(money, unit, deltaDays) {
    deltaDays = unit == '月' ? deltaDays : (unit == '年' ? 365 : 1);
    return money / deltaDays;
}

function getUnits(unit_1, unit_2, unit_3) {
    var units = '元';
    if(!unit_1 && !unit_2 && !unit_3) units = '-';
    if(unit_1) units += ('/' + unit_1);
    if(unit_2) units += ('/' + unit_2);
    if(unit_3) units += ('/' + unit_3);
    return units;
}

parent.global_env = parent.global_env || {};
var SELECTED_WH = parent.global_env.SELECTED_WH || tssJS.Cookie.getValue("SELECTED_WH");
var CURRENT_OWNER; // 当前单据的货主，用于选择货品等

function isMoneyWh(wh) {
    wh = wh || SELECTED_WH;
    return WAREHOUSE_I[wh] && WAREHOUSE_I[wh].type == 2;
}

function transToMoneyWh(fields, width) {
    fields.each(function(i, f) {
        if( f.zj_title === true ) {
            f.width = width || f.width;
        } 
        else if( f.zj_title ) {
            f.title = f.zj_title;
            f.width = width || f.width;
        }
        else {
            f.hidden = true;
        }
    });
}

function prepareMyWHs(callback){
    var params = { status: 1 };
    $.getJSON(WAREHOUSE.QUERY, params, function(data) {
        MY_WAREHOUSE_D = data;
        MY_WAREHOUSE_D.each(function(i, item) {
            MY_WAREHOUSE_I[item.id] = item;
            MY_WAREHOUSE_C[item.code] = item;
        });
        callback && callback();
    });
}

function isMoneyWarehouse() {
    var wh = MY_WAREHOUSE_I[SELECTED_WH];
    return wh && wh.type == 2;
}

function showMoneyLog(opno) {
    var row = dg1.datagrid("getRows")[dg1_selected] || getSelectedRow("t1");
    opno = opno || row.asnno || row.orderno;

    addTab('url', "资金流水", "/tss/pages/wms/wms_log.html?opno=" + opno);
}

function showOpLog(opno) {
    var row = dg1.datagrid("getRows")[dg1_selected] || getSelectedRow("t1");
    opno = opno || row.asnno || row.orderno;

    addTab('url', "作业日志", "/tss/pages/wms/wms_log.html?opno=" + opno);
}

function showOpLog2(wh_id, type, date1, date2, opIDs) {
    var url = "/tss/pages/wms/wms_log.html?wh_id=" +wh_id+ "&date1=" +date1 + "&date2=" +date2;
    if( type )  url += "&type=" +type;
    if( opIDs ) url += "&opIDs=" +opIDs;

    addTab('url', "作业日志", url);
}

function showInv(wh_id, owner_id, sku_id) {
    var url = "/tss/pages/wms/inv.html?wh_id=" + wh_id + "&justQuery=true";
    if( owner_id ) url += "&owner_id=" +owner_id;
    if( sku_id )   url += "&sku_id=" +sku_id;
    
    addTab('url', "库存查询", url);
}

function recordLog(table, code, content, udf1) {
    var params = {table: table, code: code, content: content, udf1: udf1};
    tssJS.post("/tss/api/log", params);
}

/* --------------- easyui.js 复制方法 ---------------*/

/*
 * 过滤参数集，剔除掉为空的参数项。
 * PASS: 因JQuery的ajax会发送空字符串，而TSS的ajax不发送，导致数据缓存条件失效。
 */
function filterParams(params) {
    for(var key in params) {
        if(params[key] == null || params[key] == "") {
            delete params[key];
        }
        else {
            params[key] = (params[key]+'').trim();
        }
    }
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
        openIframe(panelId, url, name, {'width': width, 'height': height} );       
    }
}

function openIframe(panelId, src, title, customize) {
    var obj = {
        title : title || '查看',
        width : tssJS.getInner().width - 100,
        height : tssJS.getInner().height - 100,
    };
    $.extend(obj,customize);

    $('#' + panelId).remove();
    var panel = document.createElement("div")
    document.body.appendChild(panel);
    $panel = $(panel);
    $panel.addClass("easyui-dialog")
          .attr("id", panelId)
          .append('<iframe frameborder="0"></iframe>')
          .dialog(obj);
    $panel.find('iframe').attr('src', src).css('width', obj.width - 15).css('height', obj.height - 40);
}

/* --------------- warehouse choose START ---------------*/
function initContext(callback, type) {
    type = type || 1;
    prepareMyWHs(function(){
        var sw_html = '';
        var thisTypeWHs = [];
        MY_WAREHOUSE_D.each(function(i, item) {
            if( (item.type||1) == type || type == 9 ) {
                thisTypeWHs.push( item.id );
                sw_html += '<div class="selectItem">'
                            + '<input name="mywh" type="radio" value="' + item.id + '">'
                            + '<span>' +item.name+ '</span>'
                        + '</div>';
            }
        })
        $('#init').after('<div class="ribbon-block"><div class="ribbon" id="selectedwh_tag" onclick="chooseWH()">选择仓库</div></div>'
            + '<div id="dlg_choose_wh" title="选择仓库" class="easyui-dialog" closed="true" buttons="#dlg_choose_wh_btns">'
                        + '<div class="dlg_choose_wh">'
                        + sw_html
                        + '</div>'
                    + '</div>'
                    + ' <div id="dlg_choose_wh_btns">'
                        + '<a href="#" class="easyui-linkbutton" iconCls="ion ion-ios-checkmark-circle" onclick="confirmWH(' +callback+ ')">确认</a>'
                        + '<a href="#" class="easyui-linkbutton" iconCls="ion ion-ios-close-circle" onclick="$(\'#dlg_choose_wh\').dialog(\'close\')">取消</a>'
                    + '</div>')

        $.parser.parse()
        if(MY_WAREHOUSE_D && thisTypeWHs.length == 1){
            SELECTED_WH = thisTypeWHs[0];
        }
        if( SELECTED_WH && MY_WAREHOUSE_I[SELECTED_WH] && thisTypeWHs.contains( parseInt(SELECTED_WH)) ) {
            $('#selectedwh_tag').text(MY_WAREHOUSE_I[SELECTED_WH].name);
            callback(SELECTED_WH);
            $('input[type="radio"][name="mywh"][value="' +SELECTED_WH+ '"]').prop("checked", "true");
        }
        else{
            chooseWH();
        }
    })    
}
function chooseWH(){
    $('#dlg_choose_wh').dialog( {"modal": true} ).dialog('open')
    $("#dlg_choose_wh").panel("move", {top: '20%'});
}

function confirmWH( callback ){
    
    const dlg_wh = $('input[type="radio"][name="mywh"]:checked').val();
    if( !dlg_wh ) return $.messager.show({ title: '提示', msg: '您还没有选中任何仓库，选中后再确认'});;

    if(SELECTED_WH == dlg_wh){
        tssJS.Cookie.setValue("SELECTED_WH", SELECTED_WH);
    }
    else if(dlg_wh){
        SELECTED_WH = parent.global_env.SELECTED_WH = dlg_wh;
        tssJS.Cookie.setValue("SELECTED_WH", SELECTED_WH);

        $('#selectedwh_tag').text(MY_WAREHOUSE_I[SELECTED_WH].name)
        callback(dlg_wh)
    }

    $('#dlg_choose_wh').dialog('close');
    prepareLOCs();
}

/* --------------- owner、worker choose START ---------------*/

function formatterWorker(value, row, index) {
    if( !value ) return "";
    var list = value.split(','), namelist = [];
    for(var i = 0; i < list.length; i++) {
        namelist.push(userPartner[list[i]])
    }
    var names = namelist.join(',')
    row._userName = names;
    return '<span title = "' + (names || '') + '">' + (names || '') + '</span>'
}

function chooseWorker(oldWorker) {
    if( oldWorker ) {
        $.messager.confirm('操作确认', '此订单已指派【' +userCode2userName(oldWorker)+ '】为作业人员，确定要重新指派吗？', function(confirmed) {
            confirmed && _chooseWorker( oldWorker );
        })
    } else { 
        _chooseWorker( '' );
    }
}

function _chooseWorker(oldWorker) {
    var workers = (oldWorker || "").split(",");
    tssJS.getJSON("/tss/wx/api/users_ex_customer2", {roleName: role_operater}, function(data) {

        var opers = [];
        data.each( function(i, item) {
            var whname = WAREHOUSE_I[SELECTED_WH].name;
            if( userCode == item.loginName || item.group == whname ) { // 只过滤出当前仓库人员
                opers.push( item );
            }
        } );

        $('#worker').datalist({
            data: opers,
            lines: false,
            singleSelect: false,
            checkbox: true,
            textField: 'userName',
            valueField: 'loginName',
            groupField: 'group',
            onLoadSuccess: function (data) {
                $.each(data.rows, function (index, item) {
                    if ( workers.contains(item.loginName) ) {
                        $('#worker').datalist('checkRow', index);
                    }
                });
            }
        });
    })
    openDialog('指派作业人员', false, 'fm_worker', 'dlg_worker');
}

function getChoosedWorkers() {
    var list = $('#worker').datalist('getChecked'), workers = [];
    list.each( function(i, item) {
        workers.push(item.loginName);
    });

    closeDialog('fm_worker', 'dlg_worker');
    return workers.join(',');
}

function chooseOwner(params, type) {
    params = params || {};
    type = type || '所属货主';

    params.status = 1;
    tssJS.getJSON("/tss/xdata/json/wms_owner", params, function(data) {
        $('#owner').datalist({
            data: data,
            singleSelect: true,
            checkbox: true,
            textField: 'name',
            valueField: 'id',
            group: type
        });
    })
    openDialog('选择'+type, false, 'fm_owner', 'dlg_owner');
}

function getChoosedOwner() {
    var row = $('#owner').datalist('getSelected');
    if( !row ) {
        return $.messager.alert('异常提示', '请打勾选择一个货主(账户)');
    }

    closeDialog('fm_owner', 'dlg_owner');
    return row.id;
}

/* --------------- 出入库单据 START ---------------*/
var _HEADER_TL;
function chooseOW() {
    var owner = getChoosedOwner();
    importExcel( owner, _HEADER_TL);
}

function downloadImpTL(tl_record) {
    /* 1. 页面自定义模板
    var params = {"name": "入库单导入模板"};
    params.data = "入库单号,下单日期,货品,货品编码,入库单数量,生产日期,过期日期,批号,包装量,单价,金额,备注";
    tssJS.Data.exportCSV('/tss/data/export/data2csv', params);
    */

    // 2. 通过录入表定义的模板
    var tl_url = "/tss/xdata/import/tl/" + tl_record;
    tssJS("#downloadFrame").attr( "src", encodeURI( tl_url ) );
}

function customizeTL(tl_record, name) {
    var url = '/tss/pages/base/tl_config.html?imp_rc_table=' + tl_record;
        
    addTab('url', name, url);
}

function appendTL( callback ) {
    tssJS.get("/tss/xdata/json/dm_record_imptl", {type: tl_record, status: 1}, function(data) {
        data.each( function(i, item) {
            var li = tssJS.createElement("div");
            tssJS(li)
                .text( item.tlname )
                .attr("data-options", "iconCls:'ion-ios-cloud-upload'" )
                .attr("onclick", "importExcel(null, '" +item.tldef+ "')" );
            tssJS("#impbtns").appendChild(li);
        } );

        callback();
    });
}

/* --------------- 出入库验货  START ---------------*/
function formatterLot(value,row,index) {
    var lot = [
        ifNull(row.lotatt01), ifNull(row.lotatt02), ifNull(row.lotatt03), ifNull(row.lotatt04), 
        ifNull(row.invstatus), ifNull(row.createdate), ifNull(row.expiredate)
    ].join('');

    return lot ? lot.substring(0, lot.length - 1) : lot;
}

function ifNull(value) {
    return value ? value + ',' : ''
}

function noticeException( content, justTip ) {
    if(!justTip) {
        $("#exc_reason").css("background-color", content ? 'rgb(254, 247, 169)' : "#fff");
        $("#exc_reason").text(content);
    }

    if( content ) {
        if( !justTip && recException ) {
            $.messager.confirm('异常提示', content + '。确定登记此异常吗？', function(confirmed) {
                confirmed && recException();
            });
        }
        else {
            $.messager.alert('异常提示', content);
        }
    }
}

$(function () {
    tssJS('#scanfactor').blur( function() {
        var val = tssJS('#scanfactor').value();
        if( !val || (!/^[1-9]+[0-9]*]*$/.test(val) && val != '-1') ) {
            tssJS('#scanfactor').value(1);
        }
    } )
});

var skuPics = [];
function querySkuPic(skuid) {
    $('.img_').attr('src', ' ');

    if( !skuPics[skuid] ) {
        skuPics[skuid] = ' ';
        getAttachs("wms_sku", skuid, function(d) {
            skuPics[skuid] = d.downloadUrl || ' ';
            $('.img_').attr('src', skuPics[skuid]);
        }) 
    } else {
        $('.img_').attr('src', skuPics[skuid]);
    }
}
/* --------------- 出入库验货 END ---------------*/

/* --------------- 自定义字段、流程、界面展示等 -----------------*/
var enable_sn         = tssJS.Cookie.getValue('js_enable_sn') == "1",
    can_beyond_asn    = tssJS.Cookie.getValue('js_beyond_asn') == "1",
    inv_check_approve = tssJS.Cookie.getValue('js_inv_check_approve') == "1",
    operation_approve = tssJS.Cookie.getValue('js_operation_approve') == "1",
    hide_money        = tssJS.Cookie.getValue('js_hide_money') != "0",
    hide_weight       = tssJS.Cookie.getValue('js_hide_weight') != "0",  // 默认为空，隐藏
    checkin_with_lot  = tssJS.Cookie.getValue('js_checkin_with_lot') == "1",
    _qty_precision    = Math.min(2, Math.max(0, (tssJS.Cookie.getValue('js_qty_precision') || 0) * 1)),
    qty_precision     = {precision: _qty_precision, min: 0}, // 货品数量允许几位小数
    show_qty_uom      = false;  

var FIELD_DEFAULT = [

    {code: 'createdate', label: "生产日期", cwidth: '7%', align: 'center'},
    {code: 'expiredate', label: "过期日期", cwidth: '7%', align: 'center'},
    {code: 'invstatus',  label: "货品状态", cwidth: '6%', align: 'center', options: '残次品 良品'},
    {code: 'lotatt02', label: "装箱量",   cwidth: '5%', align: 'center', editable: false, checkreg: '^[1-9]+[0-9]*$'},
    {code: 'lotatt01', label: "批号",     cwidth: '5%', align: 'center', type: 'hidden'},
    {code: 'lotatt03', label: "批次属性3", cwidth: '5%', align: 'center', type: 'hidden'},
    {code: 'lotatt04', label: "批次属性4", cwidth: '5%', align: 'center', type: 'hidden'},
    
    {tbl: 'wms_sku',   code: 'barcode', label: "条码"},
    {tbl: 'wms_sku',   code: 'barcode2', label: "条码2", type: 'hidden'},
    {tbl: 'wms_sku',   code: 'udf1', label: "自定义1", type: 'hidden'},
    {tbl: 'wms_sku',   code: 'udf2', label: "自定义2", type: 'hidden'},
    {tbl: 'wms_sku',   code: 'udf3', label: "自定义3", type: 'hidden'},
    {tbl: 'wms_sku',   code: 'udf4', label: "自定义4", type: 'hidden'},
    {tbl: 'wms_sku',   code: 'price0', label: "进价", type: 'hidden'},
    {tbl: 'wms_sku',   code: 'price', label: "批价", type: 'hidden'},
    {tbl: 'wms_sku',   code: 'price2', label: "售价"},
    {tbl: 'wms_sku',   code: 'uom', label: "单位"},
    {tbl: 'wms_sku',   code: 'guige', label: "规格(型号)"},
    {tbl: 'wms_sku',   code: 'brand', label: "品牌"},
    {tbl: 'wms_sku',   code: 'category', label: "大类"},
    {tbl: 'wms_sku',   code: 'shelflife', label: "保质期"},
    {tbl: 'wms_sku',   code: 'owner_id', label: "货主", type: 'hidden'},
    {tbl: 'wms_sku',   code: 'warehouse_id', label: "仓库", type: 'hidden'},

    {tbl: 'wms_skux',   code: 'parent_id', label: "组合货品"},
    {tbl: 'wms_skux',   code: 'weight', label: "组合权重"},
    {tbl: 'wms_skux',   code: 'private_loc_id', label: "绑定库位"},
    {tbl: 'wms_skux',   code: 'safety_qty', label: "安全库存"},

    {tbl: 'wms_order', code: 'type', label: "出库类型", options: '销售出库 调拨出库 报废出库 盘亏损耗出库', udf1: 'wms_order_import'},
    {tbl: 'wms_order', code: 'd_receiver', label: "收件人", udf1: 'wms_order_import'},
    {tbl: 'wms_order', code: 'udf1', label: "自定义1", type: 'hidden', udf1: 'wms_order_import'},
    {tbl: 'wms_order', code: 'udf2', label: "自定义2", type: 'hidden', udf1: 'wms_order_import'},
    {tbl: 'wms_order', code: 'udf3', label: "自定义3", type: 'hidden', udf1: 'wms_order_import'},
    {tbl: 'wms_order', code: 'udf4', label: "自定义4", type: 'hidden', udf1: 'wms_order_import'},
    {tbl: 'wms_order', code: 'udf5', label: "自定义5", type: 'hidden', udf1: 'wms_order_import'},
    {tbl: 'wms_order', code: 'udf6', label: "自定义6", type: 'hidden', udf1: 'wms_order_import'},
    {tbl: 'wms_order', code: 'udf7', label: "自定义7", type: 'hidden', udf1: 'wms_order_import'},
    {tbl: 'wms_order', code: 'udf8', label: "自定义8", type: 'hidden', udf1: 'wms_order_import'},

    {tbl: 'wms_asn',   code: 'type', label: "入库类型", options: '成品入库 调拨入库 采购入库 退货入库 盘盈入库', udf1: 'wms_asn_import'},
    {tbl: 'wms_asn',   code: 'supplier', label: "供货方", udf1: 'wms_asn_import'},
    {tbl: 'wms_asn',   code: 'udf1', label: "自定义1", type: 'hidden', udf1: 'wms_asn_import'},
    {tbl: 'wms_asn',   code: 'udf2', label: "自定义2", type: 'hidden', udf1: 'wms_asn_import'},
    {tbl: 'wms_asn',   code: 'udf3', label: "自定义3", type: 'hidden', udf1: 'wms_asn_import'},
    {tbl: 'wms_asn',   code: 'udf4', label: "自定义4", type: 'hidden', udf1: 'wms_asn_import'},
    {tbl: 'wms_asn',   code: 'udf5', label: "自定义5", type: 'hidden', udf1: 'wms_asn_import'},
    {tbl: 'wms_asn',   code: 'udf6', label: "自定义6", type: 'hidden', udf1: 'wms_asn_import'},
    {tbl: 'wms_asn',   code: 'udf7', label: "自定义7", type: 'hidden', udf1: 'wms_asn_import'},
    {tbl: 'wms_asn',   code: 'udf8', label: "自定义8", type: 'hidden', udf1: 'wms_asn_import'},

    {tbl: 'wms_asn_import',   code: 'sku_guige', label: "规格(型号)"},
    {tbl: 'wms_asn_import',   code: 'sku_uom', label: "单位"},
    {tbl: 'wms_asn_import',   code: 'sku_brand', label: "品牌", type: 'hidden'},
    {tbl: 'wms_asn_import',   code: 'sku_category', label: "大类", type: 'hidden'},
    {tbl: 'wms_asn_import',   code: 'sku_shelflife', label: "保质期", type: 'hidden'},
    {tbl: 'wms_asn_import',   code: 'sku_udf1', label: "SKU自定义1", type: 'hidden'},
    {tbl: 'wms_asn_import',   code: 'sku_udf2', label: "SKU自定义2", type: 'hidden'},
    {tbl: 'wms_asn_import',   code: 'sku_udf3', label: "SKU自定义3", type: 'hidden'},
    {tbl: 'wms_asn_import',   code: 'sku_udf4', label: "SKU自定义4", type: 'hidden'},

    {tbl: 'fms_order', code: 'origin', label: "详情", cwidth: '4%', udf1: 'fms_order_import'},
    {tbl: 'fms_order', code: 'type', label: "付款类型", cwidth: '5%', options: '支出付款 收入退款 报销付款 借支付款 税费付款 其他支出', udf1: 'fms_order_import'},
    {tbl: 'fms_order', code: 'udf1', label: "付款科目", cwidth: '6%', options: '科目一 科目二 其他', udf1: 'fms_order_import'},
    {tbl: 'fms_order', code: 'udf3', label: "发票", cwidth: '5%', type: 'hidden', udf1: 'fms_order_import'},
    {tbl: 'fms_order', code: 'udf5', label: "账单金额", cwidth: '5%', align: 'right', udf1: 'fms_order_import'},
    {tbl: 'fms_order', code: 'udf6', label: "自定义6", type: 'hidden', udf1: 'fms_order_import'},
    {tbl: 'fms_order', code: 'udf7', label: "自定义7", type: 'hidden', udf1: 'fms_order_import'},
    {tbl: 'fms_order', code: 'udf8', label: "自定义8", type: 'hidden', udf1: 'fms_order_import'},
    {tbl: 'fms_order', code: 'udf2', label: "自定义2", type: 'hidden', udf1: 'fms_order_import'},
    {tbl: 'fms_order', code: 'udf4', label: "自定义4", type: 'hidden', udf1: 'fms_order_import'},

    {tbl: 'fms_asn',   code: 'origin', label: "详情", cwidth: '4%', udf1: 'fms_asn_import'},
    {tbl: 'fms_asn',   code: 'type', label: "收款类型", cwidth: '5%', options: '现结收款 月结收款 支出退款 初始收款', udf1: 'fms_asn_import'},
    {tbl: 'fms_asn',   code: 'udf1', label: "收款科目", cwidth: '6%', options: '科目一 科目二 其他', udf1: 'fms_asn_import'},
    {tbl: 'fms_asn',   code: 'udf3', label: "发票", cwidth: '5%', udf1: 'fms_asn_import'},
    {tbl: 'fms_asn',   code: 'udf5', label: "账单金额", cwidth: '5%', align: 'right', udf1: 'fms_asn_import'},
    {tbl: 'fms_asn',   code: 'udf6', label: "自定义6", type: 'hidden', udf1: 'fms_asn_import'},
    {tbl: 'fms_asn',   code: 'udf7', label: "自定义7", type: 'hidden', udf1: 'fms_asn_import'},
    {tbl: 'fms_asn',   code: 'udf8', label: "自定义8", type: 'hidden', udf1: 'fms_asn_import'},
    {tbl: 'fms_asn',   code: 'udf2', label: "自定义2", type: 'hidden', udf1: 'fms_asn_import'},
    {tbl: 'fms_asn',   code: 'udf4', label: "自定义4", type: 'hidden', udf1: 'fms_asn_import'},

    {tbl: 'fms_invoice',code: 'type', label: "发票类型", options: '增值税普通发票 增值税专用发票 增值税电子发票 其他'},
    {tbl: 'fms_invoice',code: 'tax_rate', label: "税率", options: '0% 1% 3% 5% 6% 9% 10% 11% 13%'}
];

var FIELD_CUSTOMIZE = [], 
    _M = {}, hasCustomizeFileds = false;  // 域级

(parent.FIELD_CUSTOMIZE|| parent.parent.FIELD_CUSTOMIZE || []).each( function(i, item) {
    var key = (item.tbl||"") + item.code;
    _M[key] = item;
    hasCustomizeFileds = true;
} );

show_qty_uom = !hasCustomizeFileds ||((_M["lotatt02"] && _M["lotatt02"].type != 'hidden'));
qty_uom_label = ((_M["lotatt02"]||{}).label || '装箱量').replace('装', '').replace('量', '') + '数';

FIELD_DEFAULT.each( function(i, item) {
    var key = (item.tbl||"") + item.code;
    FIELD_CUSTOMIZE.push( _M[key] || item );
} );

var LOTATT_CODES = ['createdate','expiredate','invstatus','lotatt01','lotatt02','lotatt03','lotatt04'];

var LOTATTS = {}, FIELDS = {};
FIELD_CUSTOMIZE.each(function(i, f) {
    var key = (f.tbl||'') + f.code;
    f.width = f.cwidth;

    if( f.options ) {
        var options = f.options.split(" ");
        f.editor = { type: 'combobox', options: { data: [], panelHeight:'auto' } }, 
        options.each( function(i, option) {
            option && f.editor.options.data.push( {text: option, value: option} );
        } );
    }

    if( LOTATT_CODES.contains(key) ) LOTATTS[key] = f;
    FIELDS[key] = f;
});

function fixFields(fields, table) {
    fields.each(function(i, f) {
        if( table == 'wms_sku' && ['price', 'owner_id', 'warehouse_id'].contains(f.field) ) return true;

        var key = (table || '') + f.field;
        var attrs = FIELDS[key];
        if( attrs ) {
            f.title = attrs.label;
            f.width = f.width || attrs.width;
            f.align = f.align || attrs.align;
            if( attrs.type === 'hidden' ) {
                f.hidden = true;
            }

            f.editor = attrs.editor || f.editor;
        }

        if( f.field == 'expiredate' ) {
            f.styler = expireStyler;
        }
    });
}

function fixLotatts(fields) {
    fixFields(fields);
}
fixLotatts(FIELDS_INV);

function initColumn(column, width, align){
    var l = 0;
    column.each(function(i,item){
        !item.hidden && l++
    })
    var default_ = Math.floor(100 / l) + '%';
    $.each(column, function(i, field) {
        field.align = field.align || align || "center";
        field.width = field.width || width || default_;
        field.sortable = true;
    });

    fixLotatts(column);
}

function dialogShowUdf(udfs) {
    udfs.each((i, _udf) => {
        var label_key = "label_udf" + (i + 1), 
            key = "udf" + (i + 1);
        if( _udf && _udf.type != 'hidden' ) {
            $("#" + label_key).text( _udf.label + ":" );
            var readonly = _udf.readonly == 'true'
            if( _udf.options && _udf.editor.options.data ) {
                $("#" + key).combobox({
                    data: _udf.editor.options.data,
                    textField: 'text',
                    valueField: 'text',
                    panelHeight: 'auto'
                })
                if(_udf.defaultvalue) {
                    $("#" + key).combobox('setValue', _udf.defaultvalue)
                }
                $("#" + key).combobox("readonly", readonly);
            }
            else {
                $("#" + key).textbox({});
                $("#" + key).textbox("readonly", readonly);
            }
        }
        else {
            $("#" + label_key).text("");
            $("#" + key).hide();
        }
    })
}

/* --------------- 重写 jQuery.getJSON 和 post 使之能自动处理异常 -----------------*/
var JQ = {};
JQ.getJSON = function(url, params, callback) {
    tssJS.showWaitingLayer();
    $.getJSON( url, params, function(result) {
        tssJS.hideWaitingLayer();
        if(result.errorMsg) {
            return $.messager.alert('出错了', result.errorMsg)
        }
        callback && callback(result);
    } )
}

JQ.post = function(url, params, callback) {
    tssJS.showWaitingLayer();
    $.post( url, params, function(result) {
        tssJS.hideWaitingLayer();
        if(result.errorMsg) {
            return $.messager.alert('出错了', result.errorMsg)
        }
        callback && callback(result);
    } )
}

function addScript(fullCode) {
    var script = document.createElement("script");
    script.type = "text/javascript";
    try {
        script.appendChild(document.createTextNode(fullCode));
    } catch (ex) {
        script.text = fullCode;
    }
    document.body.appendChild(script);
}

function sortStr(field, type){
    return function(a, b){
        var valueA = a[field];
        var valueB = b[field];
        if(type == "desc"){
            return -valueA.localeCompare(valueB);
        }
        else{
            return valueA.localeCompare(valueB);
        }
    }
}

