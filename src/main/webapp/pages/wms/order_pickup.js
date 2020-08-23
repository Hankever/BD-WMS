
var dg_inv, dg_pkd_selected;

function queryInvs(pkd) {
    $('#dlgInv').dialog( {"modal": true} ).dialog('open');

    var invParams = {};
    invParams.wh_id = SELECTED_WH;
    invParams.owner_id = pkd.owner_id || pkd.owner.id
    invParams.qty = '[1, 99999999]';
    invParams.sku_id = pkd.sku_id;

    dg_inv = $('#t_inv').datagrid({
        url: INV.QUERY, 
        queryParams: invParams,
        fit: true,
        singleSelect: true,
        columns: [
            [
                {field: "sku_id", title: "货品名称", width: "16%", formatter:skuid2name},
                {field: "location_id", title: "库位", formatter:locid2name, width: "10%"},
                {field: "owner_id", title: "货主", formatter:ownerid2name, width: "10%"},    
                {field: "qty", title: "库存数量", width: "8%"},
                {field: "qty_locked", title: "锁定数量", width: "8%"},
                {field: "invstatus", title: "货品状态", width: "8%"},
                {field: "createdate", title: "生产日期", width: "9%"},
                {field: "expiredate", title: "过期日期", width: "9%"},
                {field: "lotatt01", title: "批号", width: "12%"},
                {field: "lotatt02", title: "包装量", width: "7%"}
            ]
        ]
    });
}

function selectInv() {
    var opi = dg_pkd_selected;
    var inv = dg_inv.datagrid("getSelected");

    if( !inv ) return $.messager.alert('提示', '点击选择一条库存进行更换') ;

    var opi_inv_id = opi.opinv_id || opi.opinv.id;
    if( opi_inv_id === inv.id ) {
        return $.messager.alert('提示', '选择的库存和已分配的是同一条库存');
    }

    var qty_available = inv.qty - inv.qty_locked;
    if( qty_available < opi.qty ) {
        return $.messager.alert('提示', '新指定的库存【可用量 = ' +qty_available+ '】不足');
    }

    $.messager.confirm('操作确认', '您确定要更换拣货库存吗？', function(confirmed) {
        confirmed && tssJS.post(SERVICE.inv_opi +opi.id+ "/" +inv.id, {}, function(data){
            queryPKDs()
        })
    })
    
    $('#dlgInv').dialog('close');
}

function pickup(cancel) {
    var pkds = dg_pkd.datagrid("getSelections");
    if( pkds.length == 0 ) {
        return $.messager.show({ title: '提示', msg: '请批量勾选要' +(cancel ? "取消拣货" : "确认拣货完成")+ '的明细行'});
    }
          
    var opId = $("#_opid_").val(), 
        pkdIds = [], 
        exlist = [];

    pkds.each( function(i, pkd) {
        pkdIds.push(pkd.id);

        if( cancel && pkd.status != '已完成' ) {
            exlist.push(i+1);
        }
        if( !cancel && pkd.status != '新建' ) {
            exlist.push(i+1);
        }
    } );

    if( exlist.length ) {
        return $.messager.alert("异常提示", "第【" +exlist.join(",")+ "】行不能拣货" + (cancel ? "取消" : "确认") );
    }

    var serivce = cancel ? SERVICE.order_cancelPickup : SERVICE.order_pickup;
    JQ.post( serivce + opId, {pkdIds: pkdIds.join()}, function(result) {
        queryPKDs();

        var oRow = dg1.datagrid('getSelected'); 
        oRow.status = cancel ? '已分配' : '已拣货';
        oRow.udf4 = cancel ? '拣货取消' : '';
        dg1.datagrid("refreshRow", dg1_selected);
    } );
}

function sendMsg(row, params_) {
  /*
    var appid = 'wx6f5e8e3f5b6b45d8';
    var data = {
        keyword1: { value: row.orderno },
        keyword2: { value: userHas[4] },
        keyword3: { value: userCode2userName(params_.worker) },
        keyword4: { value: row.status },
        keyword5: { value: '您收到一个新的拣货任务，请接收。' }
    }
    var params = {
        touser: params_.worker,
        appid: appid,
        data: JSON.stringify(data),
        page: 'pages/tss/_redirect/index?url=' + '../../wms/index?msg_list=' + JSON.stringify(msg_list)
    }
    sendGZHMsg(params);
 */
}