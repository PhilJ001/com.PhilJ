package com.PhilJ.canal.listener;

import com.PhilJ.canal.config.RabbitMQConfig;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.xpand.starter.canal.annotation.CanalEventListener;
import com.xpand.starter.canal.annotation.ListenPoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author ZJ
 */
@CanalEventListener // 声明当前的类是canal的监听类
public class BusinessListener {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     *
     * @param eventType 当前操作数据库的类型
     * @param rowData 当前操作数据库的数据
     */
    @ListenPoint(schema = "PhilJ_business", table = "tb_ad") //来设置当前监听哪一个数据库，哪一张表。
    public void adUpdate(CanalEntry.EventType eventType, CanalEntry.RowData rowData){
        System.out.println("广告表数据发生改变");
        //遍历更新后的每一个字段列
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            //获取position字段列数据
            if ("position".equals(column.getName())){
                String positionValue = column.getValue();
                System.out.println("发送最新的数据到MQ:"+positionValue);

                //给队列发送消息
                rabbitTemplate.convertAndSend("", RabbitMQConfig.AD_UPDATE_QUEUE,positionValue);
            }
        }
    }
}
