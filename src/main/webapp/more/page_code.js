function PageDefine(code, name, columns, fn) {
    this.id = null;
    this.code = code;
    this.name = name;
    this.tabName = name + '字段权限配置';
    this.columns = columns;
    this.userColumns = null;
    this.fn = fn;
    console.log(code + ' init')

    this.go = function () {
        addTab(this.tabName, '../../tss/pages/site/page_design.html?code=' + this.code, false)
    };

    this.getColumns = function (callback) {
        if (this.userColumns) {
            callback && callback(this.combineColumns())
            return
        }
        let fthis = this;
        $.getJSON('/tss/e8/api/domain/setting/' + this.code, {}, function (data) {
            fthis.userColumns = data.columns || [];
            callback && callback(fthis.combineColumns())
        }, 'GET')
    };

    this.combineColumns = function () {
        let c_ = [], userColumnMap = list2MapByKey(this.userColumns, 'field');

        for (let i = 0; i < this.columns.length; i++) {
            let item = $.extend({}, this.columns[i]),
                key = item.field,
                userColumn = userColumnMap[key];
            // 列是后来加的 所有人都能看
            if (!userColumn) {
                c_.push(item)
                continue;
            }

            if (item.hidden || userColumn.hidden == '√') {
                item.hidden = true;

            } else {
                let hasSetRole = false;
                for (key in userColumn) {
                    if (/^[0-9]+_[0-9]+$/.test(key)) {
                        let role_op = key.split('_');
                        let roleId = role_op[0] * 1, roleOp = role_op[1];
                        hasSetRole = hasSetRole || userColumn[key] == '√';

                        if (parent.userHas[1].contains(roleId)) {
                            if (roleOp == '0') {
                                item.canSee = item.canSee || userColumn[key] == '√';
                            }
                            if (roleOp == '1') {
                                item.canOp = item.canOp || userColumn[key] == '√';
                            }
                        }
                    }
                }
                // 如果这行记录没打任何一个勾，默认都可见
                if (!item.canSee && hasSetRole) {
                    item.hidden = true;
                }
            }

            $.extend(item, {
                title: userColumn.title_ || userColumn.title,
                seq: (userColumn.seq || 99) * 1,
                readonly: !item.canOp,
                required: (item.hidden || !item.canOp) ? false : userColumn.required == '√'
            })

            c_.push(item)
        }

        return c_.sort(function (a, b) {
            return a.seq - b.seq
        });
    };

    this.reset = function () {
        this.fn && this.fn.call()
    }
}

pageDefineDirector = (function () {
    let list = [];

    function findObject(code) {
        for (let i = 0; i < list.length; i++) {
            if (list[i].code == code) {
                return list[i];
            }
        }
        return null;
    }

    function go(code, id) {
        let e = findObject(code);
        if (e) {
            e.id = id;
            e.go();
        } else {
            alert('本页不支持定制')
        }
    }

    function getColumns(code, name, columns, fn, callback) {
        let define = findObject(code);
        if (!define) {
            define = new PageDefine(code, name, columns, fn)
            list.push(define);
        }
        define.getColumns(callback)
    }

    function reset(code) {
        findObject(code).reset()
    }

    return {
        get: findObject,
        go: go,
        getColumns: getColumns,
        reset: reset,
    }
})()

function list2MapByKey() {
    let list = arguments[0];
    let map = {};
    list.each((i, item) => {
        let values = [];
        for (let j = 1; j < arguments.length; j++) {
            values.push(item[arguments[j]]);
        }
        map[values] = item;
    });
    return map;
}