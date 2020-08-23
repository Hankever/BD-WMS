/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound.wave;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.boubei.tss.util.MathUtil;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.inventory.InvOperation;

@Component
public class InvOperation4Order extends InvOperation {
 
    /**
     * 根据配置拣货规则池内容，对订单明细列表进行库存预分配。
     * 
     * @param oiList
     *            待分配的订单明细列表
     * @param oi_invs
     * @param lastInv_qty
     *            最新库存余量
     * @param lastOi_qty
     *            最新的待分配量
     * @return
     */
    public Object[] prePickup(List<OrderItem> oiList, 
    		Map<Long, List<Inventory>> oi_invs, 
    		Map<Long, Double> lastInv_qty, 
    		Map<Long, Double> lastOi_qty) {

        log.debug(" prePickup begin: oiList.size = " + oiList.size());

        List<OperationItem> pkds = new ArrayList<OperationItem>();
        List<OrderItem> notEnoughOiList = new ArrayList<OrderItem>(); // 记录明细库存不足的明细

        for (OrderItem oi : oiList) {
            Long oiID = oi.getId();

            Double qtyNeedPickup; // 要拣货量=待分配量=下单量-已分配量
            if (lastOi_qty.containsKey(oiID)) {
                qtyNeedPickup = lastOi_qty.get(oiID);
            }
            else {
                qtyNeedPickup = MathUtil.addDoubles(oi.getQty(), oi.getQty_allocated() * -1); // 要拣货量=待分配量=下单量-已分配量
            }

            if (qtyNeedPickup == 0) continue;

            List<Inventory> invs = new ArrayList<Inventory>(); // 优选的库存列表
            List<Inventory> prefereceInvs = oi_invs.get(oiID);
            if (prefereceInvs != null && prefereceInvs.size() > 0) {
                invs.addAll(prefereceInvs);
            }

            /* 判断库存是否满足oi要求的数量 */
            for (Inventory inv : invs) {
                Long invId = inv.getId();

                /* 如果剩余待拣量为0了，则说明该OI以及拣货完成了 */
                if (qtyNeedPickup == 0)
                    break;

                /* 可用量 */
                Double qtyAvailable = lastInv_qty.get(invId);
                if (qtyAvailable <= 0) continue;

                /*
                 * 本次循环inv 提供的分配量: 如果可用量满足了需要的拣货量，则=qtyNeedPickup;
                 * 不满足库存量，则该库存的“可用量”全部作为“分配量”分配掉.
                 */
                Double thisTimeQtyAllocated = (qtyAvailable >= qtyNeedPickup ? qtyNeedPickup : qtyAvailable);

                qtyNeedPickup = MathUtil.subDoubles(qtyNeedPickup, thisTimeQtyAllocated); /* 还要拣货的量 */
                qtyAvailable  = MathUtil.subDoubles(qtyAvailable, thisTimeQtyAllocated); /* 该库存的可用量=减去本次待拣货量后的剩余量*/

                lastInv_qty.put(invId, qtyAvailable);

                /* 生成PKD对象，使之与 oi 和 inv 关联 */
                pkds.add(createPKD(oi, inv, thisTimeQtyAllocated));
            }

            /* 如果库存分配完了，而oi的待拣货量还没有完全满足，则说明库存不足，记录下来。 */
            if (qtyNeedPickup > 0) {
                notEnoughOiList.add(oi);
            }

            lastOi_qty.put(oiID, qtyNeedPickup); // 记录本轮过后该oi的还欠的待分配量
        }

        log.debug(" prePickup end: pkds.size = " + pkds.size());
        return new Object[] { pkds, notEnoughOiList };
    }

    /** 生成拣货单 PKD 对象，设置分配量，并使之与 oi 和 inv 关联 */
    static OperationItem createPKD(OrderItem oi, Inventory inv, Double thisTimeQtyAllocated) {
        OperationItem pkd = new OperationItem(inv, thisTimeQtyAllocated);
		pkd.setOrderitem(oi);
        return pkd;
    }
}
