var asnno_ = decodeURI(tssJS.Query.get("asnno"));

var FIELDS_1 = [
    {field: "sku_id", title: "货品ID", hidden: true},
    {field: "skuname", title: "货品", width: '11%', sortable: true, align: "left"},
    {field: "skucode", title: "编码"},
    {field: "barcode", title: "条码"},
    {field: "status", title: "状态", width: '5%'},
    {field: "qty", title: "订单数"},
    {field: "qty_actual", title: "已验货入库"},
    {field: "qty_this", title: "当前验货中", styler: highlightStyler},
    {field: "invstatus", editor: "textbox"},
    {field: "createdate", editor: "datebox"},
    {field: "expiredate", editor: "datebox"},
    {field: "lotatt01", editor: "textbox"},
    {field: "lotatt02", editor: "textbox"},
    {field: "lotatt03", editor: "textbox"},
    {field: "lotatt04", editor: "textbox"},
    {field: "guige", title: "规格", width:'5%'},
    {field: "uom", title: "单位", width:'4%'},
    {field: "price", title: "单价(元)", hidden: hide_money, align: "right", width:'5%', editor: {type:'numberbox', options: {precision:3, min:0} }},
    {field: "money", title: "金额(元)", hidden: hide_money, align: "right", width:'6%', editor: {type:'numberbox', options: {precision:2, min:0}} },
], 
asn_owner, with_items_ck,
asnno, asn_id, asnitemList = [], currCKItem, _checking, skuList = [], 
locList = [], locCodeList = [], receive_loc, MV_LOC_TYPE_ID, IN_LOC_TYPE_ID;

fixLotatts(FIELDS_1);
fixFields(FIELDS_1, "wms_sku");

$.each(FIELDS_1, function(i, field) {
    field.align = field.align||"center";
    field.width = field.width||"7%";
});

$(function () {
    initContext(drawTable);
    init();
    bindAsnnoChange();
    bindBarcodeChange();
    FxOrRkKeyDown();

    if(enable_sn) {
        enable_sn = false;
        $('#enable_sn').switchbutton({
            checked: false,
            onChange: function(checked){
                enable_sn = checked;
            }
        })
    } else {
        $('#_enable_sn').hide();
    }

    dg = $('#t1').datagrid({
        fit: true,
        fitColumns: true,
        rownumbers: true,
        singleSelect: true,
        checkOnSelect: true,
        selectOnCheck: true,
        remoteSort: false,
        columns: [FIELDS_1],
        data: [],
        showFooter: true,
        onClickRow:function(index, data){
            if( checkin_with_lot ) {
                endEdit();
                dg.datagrid("beginEdit", index);
            }
        },
        onAfterEdit: function(index,field, changes) {
            var row = dg.datagrid('getRows')[index];
            row["qty"] = parseFloat(row["qty"]);
            row["money"] = parseFloat(row["money"]);

            if(row.skuItem) {
                for(var j = 0; j < LOTATT_CODES.length; j++) {
                    var key = LOTATT_CODES[j];
                    row.skuItem[key] = row[key];
                }
                row.skuItem["qty"] = row["qty"];
                row.skuItem["money"] = row["money"];
            }
            
            setCheckingQty();
        }
    });
});

function endEdit(){ dg.datagrid('acceptChanges') }

function drawTable(SELECTED_WH){
    prepareSKUs(function(){
        prepareLOCs(function() {
            if(asnno_ != 'undefined') {
                asnno = asnno_;
                $('#asnno').val( asnno);
                $('#asnno_tag').text( asnno || '');
            }

            SELECTED_WH = SELECTED_WH;
            query();
        })
    });
}

function init() {
    $('#qty, #qty_actual, #qty_in, #qty_remain').text(0);
    $('#' + (asnno ? 'barcode' : 'asnno') ).focus();

    tssJS.get('/tss/param/json/combo/LocType', {}, function(res) {
        for(var i = 0; i < res.length; i++) {
            if(res[i][1] == '收货区')  IN_LOC_TYPE_ID = res[i][0];
            if(res[i][1] == '中转容器') MV_LOC_TYPE_ID = res[i][0];
        }
        locCombobox();
    });
}

function locCombobox() {
    tssJS.getJSON( LOC.QUERY, {warehouse_id: SELECTED_WH}, function(data) {
        locList = [];
        data.each(function(i, item) {
            if(item.type_id != MV_LOC_TYPE_ID && item.status == 1) {
                locList.push(item);
            } else if(item.type_id == MV_LOC_TYPE_ID) {
                locList.push(item); // 中转容器停用的也加进来
            }
            locCodeList.push( item.code );

            if(item.type_id == IN_LOC_TYPE_ID && item.status == 1) {
                receive_loc = item.code;
            }
        })

        $('#loccode').combobox({
            data: locList,
            textField: 'name',
            valueField: 'code',
            icons: [
                { iconCls:'ion ion-ios-add-circle-outline ion-mt3', handler: function(){ openDialog('新增收货容器', false, 'fm2', 'dlg2'); } } ,
                { iconCls:'ion ion-ios-refresh ion-mt3', handler: function(){ locCombobox();} }
            ],
            onSelect: function(d) {
                if( !checkAsnno() ) {
                    $('#loccode').combobox('setValue', '');
                }
            }
        })

        locKeydown();
    })
}

function locKeydown() {
    $('#loccode').textbox('textbox').keydown(function(){
        if(event.keyCode == 13) {
            var loc = $('#loccode').combobox('getValue');
            if( !checkAsnno() ) {
                return $('#loccode').combobox('setValue', '');
            }

            if( loc && !locCodeList.includes(loc)) {
                $.messager.confirm("操作提示", "收货容器【" + loc + "】不存在，是否马上添加？", function (flag) {
                    if ( flag ) {
                        $('#loc_code').textbox('setValue', loc);
                        openDialog('新增收货容器', false, 'fm2', 'dlg2');
                    }
                });
            }
        }
    })
}

function saveLoc() {
    var fm_data = $("#fm2").serializeArray(), params_ = {}, 
        flag = $("#fm2").form('validate'),
        $saveBtn = $('#dlg-buttons>a[onclick^="saveLoc"]');

    fm_data.each(function(i, item){
        params_[item.name] = item.value;
    })
    params_['id'] = '';
    params_['status'] = 1;
    params_['name'] = params_['code'];
    params_['type_id'] = MV_LOC_TYPE_ID;
    params_['warehouse_id'] = SELECTED_WH;

    if( flag ) {
        $saveBtn.linkbutton("disable");
        JQ.post('/tss/xdata/wms_location', params_, function(r) {
            $.messager.alert('提示', '新增收货容器成功');
            closeDialog('fm2', 'dlg2');
        })
    }
}

function bindAsnnoChange() {
    $("#asnno").keydown(function () {
        asnno = $('#asnno').val();
        if(event.keyCode == 13) {
            query(); 
        }
    })
}

function loadDataGrid(asnitemList) {
    var qty = 0, qty_actual = 0, qty_this = 0, money = 0;
    asnitemList.each(function(i, item) {
        qty += item.qty;
        qty_actual += (item.qty_actual ? item.qty_actual : 0); 
        qty_this   += (item.qty_this ? item.qty_this : 0);
        money += (item.money||0);
    });

    qty = round(qty, _qty_precision);
    qty_actual = round(qty_actual, _qty_precision);
    money = round(money, 1);
    $('#qty').text(qty);
    $('#qty_actual').text(qty_actual);
    $('#qty_remain').text( Math.max(0, qty - qty_actual) );

    dg.datagrid("loadData", asnitemList);
    dg.datagrid('reloadFooter',[
        { skuname: '合计', qty: qty, qty_actual: qty_actual, qty_this: qty_this, money: money}
    ]);
}

function bindBarcodeChange() {
    $('#barcode').keydown(function() {
        if(event.keyCode == 13) {
            if( !checkAsnno() ) {
                return $('#barcode').val('')
            }

            var barcode = ($('#barcode').val()||'').trim();
            if( barcode ) {
                endEdit();
                checkSku( barcode );
            } 
        }
    })
}

function checkAsnno() {
    if( with_items_ck && !asnno ) {
        noticeException('请先扫描入库单号', true);
        return false;
    }
    return true;
}

var sku_check_list = {};
function checkSku(barcode) {
    if(sku_check_list[barcode]) {
        return setSkuList(barcode);
    }

    tssJS.getJSON(SKU.QUERY, {"status": "1", "barcode|barcode2": barcode, strictQuery: true}, function(data) {
        if(data.length == 0) {
            $('#barcode').val('');
            $.messager.confirm("操作提示", "条码为【" + barcode + "】的货品不存在，是否马上添加？", function (flag) {
                if (flag) {
                    tssJS.getJSON('/tss/sn/Sxxxx/1', {}, function(d_) {
                        $('#sku_barcode').textbox('setValue', barcode);
                        $('#sku_code').textbox('setValue',d_[0])
                        openDialog('快速新增货品信息');
                    })
                }
            });
        } else {
            sku_check_list[barcode] = data[0];
            setSkuList(barcode);
        }
    })
}

function saveSku() {
    var fm_data = $("#fm").serializeArray(), params_ = {}, 
        flag = $("#fm").form('validate'),
        $saveBtn = $('#dlg-buttons>a[onclick^="saveSku"]');

    fm_data.each(function(i, item){
        params_[item.name] = item.value;
    })
    params_['id'] = '';
    params_['status'] = 1;
    if(flag) {
        $saveBtn.linkbutton("disable");
        JQ.post('/tss/xdata/wms_sku', params_, function(r) {
            $.messager.alert('提示', '新增货品成功');
            closeDialog();
        })
    } else {
        $.messager.alert('提示', "请将必填项填写完毕再保存");
    }
}

function FxOrRkKeyDown() {
    $(document).keydown(function () {
        if(event.keyCode == 120) { // F9
            saveData();
        }
        if(event.keyCode == 115) { // F4
            recException();
        }
    })
}

function setCheckingQty() {
    var qty_checking = 0;
    skuList.each(function(i, item){
        qty_checking += parseFloat(item.qty_this || item.qty);
    })
    $('#qty_checking').val(qty_checking || 0);
}

function saveData() {
    var toloccode = $('#loccode').combobox('getValue'), 
        $saveBtn = $('#dlg-buttons>a[onclick^="saveData"]');

    $saveBtn.linkbutton("disable");
    endEdit();

    if(skuList.length == 0) {
        $.messager.alert('提示', "当前没有产生验货数据，请扫描货品条码进行验货");
        $saveBtn.linkbutton("enable");
        return;
    }
    if( !toloccode ) {
        $('#chatAudio')[0].play();
        $.messager.alert('提示', "收货容器不能为空，请扫描或选择一个收货容器的编码；如果是直接入库到指定库位，请选择入库库位");
        $saveBtn.linkbutton("enable");
        return;
    }
    var params_ = {
        id: asn_id,
        items: JSON.stringify(skuList),
        toloccode: toloccode || receive_loc
    }

    $.messager.confirm("操作提示", "是否确定入库？", function (data) {
        if (data) {
            _saveData(params_);
        } else {
           $saveBtn.linkbutton("enable");
        }
    }); 
}

function _saveData(params_) {
    params_.whId = SELECTED_WH;
    params_.ownerId = asn_owner;
    params_.asnno = asnno;

    var ck_save_url = with_items_ck ? SERVICE.asn_ck_inbound : SERVICE.asn_create_inbound;

    tssJS.post(ck_save_url, params_, function(res) {
        $('#scanfactor').val(1);
        $('#qty_checking').val(0);
        $('#qty, #qty_actual, #qty_remain').text(0);
        $('#loccode').combobox('setValue', '');
        $("#asnno").val(asnno);
        $('.img_').attr('src', '');

        query();
        skuList = [];
        if( !with_items_ck ) {
            asnitemList = [];
            loadDataGrid([]);
        }
        $('#dlg-buttons>a[onclick^="saveData"]').linkbutton("enable");

        $.messager.show({ title: '提示', msg: '入库提交成功'});
    })
}

function recException() {
    var content = $('#exc_reason').text();
    if( content ) {
        if( currCKItem ) {
            var lot = formatterLot('', currCKItem, '');
            content += "。异常货品 【" + currCKItem.skuname + ", " + currCKItem.skucode + (lot ? (', 批次:' + lot) : '') + "】";
        }
        
        $('#content').text( content );
    }
    openDialog('异常登记', false, 'fm1', 'dlg1');
}

function submitExc() {
    var fm_data = $("#fm1").serializeArray(), params = {}, $saveBtn = $('#dlg-buttons1>a[onclick^="submitExc"]');
    $saveBtn.linkbutton("disable");

    fm_data.each(function(i,item){
        params[item.name] = item.value;
    })
    params.asnId = asn_id;

    if(params.content) {
        tssJS.post(SERVICE.opCheckException, params, function(res) {
            closeDialog('fm1', 'dlg1');
            $saveBtn.linkbutton("enable");
            $.messager.show({ title: '提示', msg: '异常登记成功'});
        })
    } else {
        $.messager.alert('提示', "请填写异常反馈内容");
        $saveBtn.linkbutton("enable");
    }
}

var curr_item_qty = 0;
function showTagBox(snlist, qty) {
    curr_item_qty = qty || 0;
    openDialog('序列号采集', false, null, 'dlg_sn_list');
    $("#tagbox").next('.tagbox').find('input').focus();

    snlist && $('#tagbox').tagbox('setValues', snlist.split(","));
}
    
$('#tagbox').tagbox({
    label: '扫描序列号',
    tagStyler: function(value){
        return 'background:#eee; color:#555; font-size: 14px; margin-left: 5px';
    },
    onBeforeRemoveTag: function(value) {
        console.log("del: " + value);
    },
    onChange: function(newV, oldV) {
        var last = newV[newV.length - 1];
        if( newV.length > oldV.length && oldV.contains(last) ) {
            $.messager.show({ title: '异常提示', msg: "序列号【" + last + "】已存在，请勿重复扫描。"});
            $(this).tagbox('setValues', oldV);
            var index = oldV.indexOf(last);
            $('span[tagbox-index=' +index+ ']').css("background-color", 'chartreuse');
        }
        else {
            $("#sn_count").text(newV.length);
        }
    }
});

function clearSNList() {
    $('#tagbox').tagbox('setValues', []);
}
