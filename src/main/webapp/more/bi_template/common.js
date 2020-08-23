
TOMCAT_URL = "";
BASE_JSON_URL  = TOMCAT_URL + '/tss/data/json/';
BASE_JSONP_URL = TOMCAT_URL + '/tss/data/jsonp/';

function json_url(id)  { return BASE_JSON_URL + id; }
function jsonp_url(id) { return BASE_JSONP_URL + id; }

/*
 * 过滤参数集，剔除掉为空的参数项。
 * PASS: 因JQuery的ajax会发送空字符串，而TSS的ajax不发送，导致数据缓存条件失效。
 */
function filterEmptyParams(params) {
	for(var key in params) {
		if(params[key] == null || params[key] == "") {
			delete params[key];
		}
	}
}

// 脱敏
function cover(s, from, to) {
    return _cover(s, from, to);
}
function _cover(s, from, to) {
    
    if( !s || !s.length) return s;

    var l = s.length;
    from = from || l - 1;
    to = to || l;
    if( l < from ) return s;

    var a = s.split("");
    for(var i = 0; i < l; i++) {
        if(i >=from-1 && i < to) {
            a[i] = "*";
        }
    }
    return a.join("");
}

/*
 * 读取给定数组指定字段的最大值、最小值、平均值、总和
 * 参数1  dataArray 数组（一维数组 或 二维数组 或 一维对象数组）
 * 参数2  fieldIndex 字段的下标或Key值，为空则是一维数组
 */
function selectEdge(dataArray, fieldIndex) {
	if( dataArray == null ||　dataArray.length == 0 ) {
		return {"max" : 0, "min" : 0, "avg" : 0, "total" : 0};
	}
	var length = dataArray.length;

	var maxValue = 0, minValue = 999999999, total = 0, avgValue = 0;
	for(var i = 0; i < length; i++) {

		var value = fieldIndex ? dataArray[i][fieldIndex] : dataArray[i];

		maxValue = Math.max(maxValue, value);
		minValue = Math.min(minValue, value);
		total += value;
	}
	avgValue = Math.round(total / length);

	return {"max" : maxValue, "min" : minValue, "avg" : avgValue, "total" : total};
}

// 合并两个对象的属性
function combine(obj1, obj2) {
	if(obj1 == null) return obj2;
    if(obj2 == null) return obj1;

    var obj3 = {};
    for (var attrname in obj1) { obj3[attrname] = obj1[attrname]; }
    for (var attrname in obj2) { obj3[attrname] = obj2[attrname]; }
    return obj3;
}

// 读取画布上一级element的大小，以自动调整画布的大小
function autoAdjustSize(elementID) {
	var parentNode, node = $1(elementID);
	parentNode = node.parentNode ? node.parentNode : document.body;

	var _width  = Math.max(600, parentNode.offsetWidth - 5);
    var _height = Math.max(300, parentNode.offsetHeight - 5);
    tssJS(node).css("width", _width + "px").css("height", _height + "px");

    return [ _width, _height];
}
