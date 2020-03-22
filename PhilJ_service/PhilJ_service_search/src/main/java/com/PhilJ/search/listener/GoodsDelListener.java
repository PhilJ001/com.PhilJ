package com.PhilJ.search.listener;

import com.PhilJ.search.config.RabbitMQConfig;
import com.PhilJ.search.service.ESManagerService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoodsDelListener {

    @Autowired
    private ESManagerService esManagerService;

    @RabbitListener(queues = RabbitMQConfig.SEARCH_DEL_QUEUE) //定义监听哪一个队列
    public void receiveMessage(String spuId){
        System.out.println("删除索引库监听类,接收到的spuId:  "+spuId);

        //调用业务层完成索引库数据删除
        esManagerService.delDataBySpuId(spuId);
    }
}
