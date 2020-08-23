const 
    appid_BB = 2018051160132356,
    appid_BD = 2019022563365268, 

    MODULE_DEF        = "/tss/cloud/modules",
    MODULE_MONEY      = "/tss/cloud/order/price/query",
    MODULE_ORDER      = '/tss/cloud/order',
    MODULE_ORDER_LIST = '/tss/cloud/order/list',

    ACCOUNT           = '/tss/auth/account',
    ORDER_FLOW        = '/tss/auth/account/flow',
    SUBAUTHORIZE      = '/tss/auth/account/subauthorize',
    SUBAUTHORIZE_ROLE = "/tss/auth/account/subauthorize/role",
    API_USER          = "/tss/wx/api/users_ex_customer",
    OFFLINE_PAYED     = "/tss/cloud/order/payed/";

var appid = appid_BD;

function offline_payed(order_no, money_real, callback){
    $.post(OFFLINE_PAYED + order_no, {"money_real": money_real}, (result) => {
        callback && callback(result)
    })
}

function getFormData(formId){
    formId = formId || 'fm';
    if( !formId.indexOf('.') == 0 && !formId.indexOf('#') == 0 ){
        formId = '#' + formId;
    }
    var d = {};
    var t = $(formId).serializeArray();
    $.each(t, function() {
        if(d[this.name]){
            d[this.name] += ',' + this.value;
        }
        else{
            d[this.name] = this.value;
        }     
    });
    return d;
}


function restfulParams(params){
    var queryString = "?";
    $.each(params, function(key, value) {
        if( queryString.length > 1 ) {
            queryString += "&";
        }
        queryString += (key + "=" + value);
    });
    return queryString
}

function showRange(el) {
    $(el.nextSibling).text( el.value );
}

function createElement(obj){
    var dom = document.createElement(obj.dom);
    var text = obj.innerText;
    var html = obj.innerHTML;
    delete obj.dom;
    delete obj.innerText;
    delete obj.innerHTML;
    for(k in obj){
        dom.setAttribute(k, obj[k])
    }
    if(text){
        dom.innerText = text;
    }
    if(html){
        dom.innerHTML = html;
    }
    return dom;
}


/*
* 格式化金额数字
* number：要格式化的数字
* decimals：保留几位小数
* */
function number_format(number, decimals) {
    number = (number + '').replace(/[^0-9+-Ee.]/g, '');
    var n = !isFinite(+number) ? 0 : +number,
        prec = !isFinite(+decimals) ? 0 : Math.abs(decimals),
        sep = ',',
        s = '',
        toFixedFix = function (n, prec) {
            var k = Math.pow(10, prec);
            return '' + Math.ceil(n * k) / k;
        };
 
    s = (prec ? toFixedFix(n, prec) : '' + Math.round(n)).split('.');
    var re = /(-?\d+)(\d{3})/;
    while (re.test(s[0])) {
        s[0] = s[0].replace(re, "$1" + sep + "$2");
    }
 
    if ((s[1] || '').length < prec) {
        s[1] = s[1] || '';
        s[1] += new Array(prec - s[1].length + 1).join('0');
    }
    return s.join('.');
};


const moduleMap = {}, _moduleMap = {};

function queryModuleDef(callback){
    $.get(MODULE_DEF, {}, function(data){
        data.each(function(i, item){
            moduleMap[item.id] = item.module;
            _moduleMap[item.id] = item;
        })
        callback && callback()
    })
}

function createPanel(params){
    tssJS && tssJS.showWaitingLayer();
    $('._panel').remove();
    let $div = $(`
        <div class="_panel">
            <div style="position:relative; width:100%; height: 100%;">
                <div class="_panel-title">
                    <span class="_panel-title-text">` + params.title + `</span>
                    <div class="_panel-title-button">
                        <span class="_panel-title-button-close">x</span>
                    </div>
                </div>
                <div class="_panel-content"></div>
                <div class="_panel-footer">
                    <div class="_panel-footer-button">
                        <span> <button class="_panel-footer-button-submit">提交保存</button> </span>
                    </div>
                </div>
            </div>
        </div>`);
    let $content = $div.find('._panel-content');
    let css = params.css
    if(css){
        $div.css(css)
    }
    let height = ( (css||{}).height || '400px' ).replace('px','')*1 - 60;
    $content.append(params.content).css('height', height + 'px' );
    $div.appendTo('body');

    $('._panel-title-button-close').click((e)=>{
        $div.remove();
        tssJS && tssJS.hideWaitingLayer();
    })
    if( params.funcSubmit ){
        $('._panel-footer-button-submit').click((e)=>{
            params.funcSubmit($div);
        })
    }else{
        $('._panel-footer-button-submit').remove();
    }
}

function payOrder(order_no, money_cal, product, return_url){
    let params = restfulParams({
        appid : appid,
        out_trade_no : order_no,
        total_amount : money_cal,
        subject : '支付' + product,
        body : "",
        afterPaySuccess : 'CloudService',
        return_url : return_url || (window.location.origin + '/tss/bi.html')
    })

    window.parent.location.href = '/tss/alipay/api/pay/' + (tssJS.isMobile || document.body.clientWidth < 768 ? 'wap' : 'pc') + params;
}