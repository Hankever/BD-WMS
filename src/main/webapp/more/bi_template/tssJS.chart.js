

/* 
 * 1、时间、空间维度切换展示;
 * 2、echart常见绘图封装
 */
;(function($) {

    $.COLORS = ["#124BA0", "#AFEEEE", "#9370DB", "#90EE90", "#FFD700", "#FF6347", "#9ACD32", "#FF6347", "#228B22", 
                "gray", "green", "red", "blue", "yellow", "silver", "orange", "olive"];

    /* 
     * 时间 + 空间2维，tssJS.TimeSpace2(data, el, c).show();
     * data格式：
     *   [ { "value": 21425.8, "day": "2015-04-09", "space1": "安徽分公司", "space2": "蚌埠分拨" }, ...... ]
     */
    $.TimeSpace2 = function(data, el, config) {
        var title = config.title, cDay, cSpace1;
        config.timeKey = config.timeKey || 'day';
        config.valKey  = config.valKey  || 'value';
        config.space1Key = config.space1Key || 'space1';
        config.space2Key = config.space2Key || 'space2';

        $(el).html(
            "<div><div id='tsChart1'></div></div>" +
            "<div><div id='tsChart2'></div></div>" + 
            "<div><div id='tsChart3'></div></div>"
        );
        $("div:nth-child(3)", el).css("height", "0");

        window.onresize = show;

        this.show = function() {   
            // 依据el的高度，自动计算各个图的高度
            $("#tsChart1").css("height", config.height1 || '230px');
            $("#tsChart2").css("height", config.height2 || '220px');
            config.height3 = config.height3 || '220px';

            showDay();
            showSpace1();
        };

        function showDay(space) {
            var temp = [], isSpace1 = false;

            if(space) {
                data.each(function(i, item){
                    if( item.space1 === space ) {
                        isSpace1 = true;
                    }
                });

                data.each(function(i, item) {
                    if(isSpace1) {
                        if( item.space1 === space ) {
                            temp.push(item);
                        }
                    }
                    else {
                        if( item.space1 === cSpace1 && item.space2 === space ) {
                            temp.push(item);
                        }
                    }
                });
            } else {
                temp = data.slice(0);
            }
         
            var result = $.Data.groupby(temp, config.timeKey, config.valKey);
            result.each(function(i, item){
                item.name = item.day;
            });
            $.Echart.line2D("tsChart1", "【" +(space||"") +title+ "】走势", result, null, showSpace1);

            if(isSpace1) {
                cSpace1 = space;
                showSpace2();
            }
        }

        function showSpace1(day) {
            var temp = [];
            data.each(function(i, item){
                if(!day || item.day === day) {
                    temp.push(item);
                }
            });

            var result = $.Data.groupby(temp, config.space1Key, config.valKey);
            result.each(function(i, item){
                item.name = item.space1;
            });
            $.Echart.column2D("tsChart2", "【" + (day||"") + title + "】分布", result, showDay);

            cDay = day;

            cSpace1 && showSpace2();
        }

        function showSpace2(day) {
            if(!cSpace1) return;

            cDay = day ||　cDay;

            tssJS("#tsChart3").css("height", config.height3);

            var temp = [];
            data.each(function(i, item){
                if( (!cDay || item.day === cDay) && item.space1 === cSpace1 ) {
                    temp.push(item);
                }
            });

            var result = $.Data.groupby(temp, config.space2Key, config.valKey);
            result.each(function(i, item){
                item.name = item.space2;
            });
            $.Echart.column2D("tsChart3", "【" + (cDay||"") + cSpace1 + title + "】分布", result, showDay);
        }

        return this;
    };

    /* 
     * 时间 + 空间1维,  tssJS.TimeSpace1(data, el, c).show();
     */
    $.TimeSpace1 = function(data, el, config) {
        var title = config.title, cDay;
        config.timeKey = config.timeKey || 'day';
        config.valKey  = config.valKey  || 'value';
        config.space1Key = config.space1Key || 'space1';

        $(el).html(
            "<divs tyle='height:50%'><div id='tsChart1'></div></div>" +
            "<div style='height:50%'><div id='tsChart2'></div></div>"
        );

        window.onresize = show;

        this.show = function() {   
            // 依据el的高度，自动计算各个图的高度
            $("#tsChart1").css("height", config.height1 || '300px');
            $("#tsChart2").css("height", config.height2 || '250px');

            showDay();
            showSpace();
        };

        function showDay(space1) {
            var temp = [];
            data.each(function(i, item){
                if(!space1 || item.space1 === space1) {
                    temp.push(item);
                }
            });

            var result = $.Data.groupby(temp, config.timeKey, config.valKey);
            result.each(function(i, item){
                item.name = item.day;
            });
            $.Echart.line2D("tsChart1", "【" + (space1||"") + title + "】走势", result, null, showSpace);
        }

        function showSpace(day) {
            var temp = [];
            data.each(function(i, item){
                if(!day || item.day === day) {
                    temp.push(item);
                }
            });

            var result = $.Data.groupby(temp, config.space1Key, config.valKey);
            result.each(function(i, item){
                item.name = item.space1;
            });
            $.Echart.column2D("tsChart2", "【" + (day||"") + title + "】分布", result, showDay);
        }

        return this;
    };

    $.Echart = {
        /* 
         * 折线图，支持一次花多条。
         * 例: $.Echart.line2D('canvas1', 'XXX走势图', [{'name': '周一', 'value': '30'}, .....], 'KG', func);
         *     $.Echart.line2D('canvas1', null, ['X走势图': data1, 'Y走势图': data2], 'KG', func);
         *
         * 注：data中数据个数和labels个数不一定相等，以labels为准，labels有data里没有的默认等于0。
         */
        line2D : function (canvasID, _title, data, unitName, func) {
            if($.Data.isArray(data)) { 
                var _data = {}; // 单条折线图
                _data[_title] = data;
                data = _data;
            }

            var legends = [], series = [], labels = [];
         
            for(var title in data) {
                legends.push(title);
                var legendData = data[title];
         
                var serieData = [];
                var tempMap = {};
                for(var i = 0; i < legendData.length; i++) {
                    var item = legendData[i];
                    tempMap[item.name] = item.value;
                    
                    !labels.contains(item.name) && labels.push(item.name);
                }

                for(var i = 0; i < labels.length; i++) {
                    serieData[i] = tempMap[labels[i]] || 0;
                }

                series.push({
                    name: title,
                    type: 'line',
                    itemStyle : { normal: {label : {show: true, position: 'top'}, color: $.COLORS[0]}},
                    data: serieData,
                    smooth: true,
                    symbolSize: 3
                });
            }   
            
            var chartObj = echarts.init($1(canvasID));
            var option = {
                title:   { text: _title || title },
                tooltip: { trigger: 'axis' },
                legend:  { data: legends },
                toolbox: {
                    show : true,
                    feature : {
                        saveAsImage : {show: true}
                    }
                },
                xAxis : [
                    {
                        type : 'category',
                        boundaryGap : false,
                        data : labels
                    }
                ],
                yAxis : [
                    {
                        type : 'value',
                        axisLabel : {
                            formatter: '{value} ' + (unitName||'')
                        }
                    }
                ],
                series : series
            };  

            chartObj.on( 'click', function (param) { func(param.name); } );
            chartObj.setOption(option);                  
        }, 

        /*
         * 饼图， pie2D('canvas1', '货量占比', [{'name': '仓库一', 'value': '300'}, .....])
         */
        pie2D: function(canvasID, title, data) {
            var labels = [];
            data.each(function(i, item) {
                labels[i] = item.name;
            });

            var chartObj = echarts.init($1(canvasID));
            var option = {
                title : {
                    text: title,
                    x:'center'
                },
                tooltip : {
                    trigger: 'item',
                    formatter: "{a} <br/>{b} : {c} ({d}%)"
                },
                legend: {
                    orient : 'vertical',
                    x : 'left',
                    data: labels
                },
                toolbox: {
                    show : true,
                    feature : {
                        saveAsImage : {show: true}
                    }
                },
                series : [
                    {
                        name: title,
                        type:'pie',
                        radius : '75%',
                        center: ['50%', '50%'],
                        data:data
                    }
                ]
            };
            chartObj.setOption(option);         
        },

        /*
         * 柱状图，column2D('canvas1', '货量分布', [{'name': '仓库一', 'value': '300'}, .....])
         */
        column2D: function(canvasID, title, data, func, config) {
            config = config || {};
            var labels = [], _data = [];
            data.each(function(i, item) {
                labels[i] = item.name;
                _data[i]  = item.value;
            });

            var chartObj = echarts.init($1(canvasID));
            var option = {
                title : { text: title },
                tooltip : { trigger: 'axis' },
                toolbox: {
                    show : true,
                    feature : {
                        saveAsImage : {show: true}
                    }
                },
                xAxis : [
                    {
                        type : 'category',
                        data : labels,
                        axisLabel : {    
                            rotate: 15, // 倾斜度
                            textStyle: { fontSize: 14 }
                        }
                    }
                ],
                yAxis : [ { type : 'value' } ],
                series : [
                    {
                        name:title,
                        type:'bar',
                        barWidth: config.barWidth || 18, 
                        itemStyle : { normal: {label : {show: true, position: 'top'}, color: $.COLORS[0]}},
                        data: _data
                    }
                ]
            };

            chartObj.on( 'click', function (param) { func(param.name); } );
            chartObj.setOption(option);         
        }
    }

})(tssJS);