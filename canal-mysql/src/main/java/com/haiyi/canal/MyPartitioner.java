package com.haiyi.canal;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;

import java.util.List;
import java.util.Map;

/**
 * @Author:XuChengLei
 * @Date:2020-07-22
 * 自定义分区器
 */
public class MyPartitioner implements Partitioner {
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        Integer numPartitions = cluster.partitionCountForTopic(topic);
        String k = key.toString();
        int keyCode = k.hashCode() % numPartitions;
        return keyCode;
    }

    public void close() {

    }

    public void configure(Map<String, ?> map) {

    }
}
