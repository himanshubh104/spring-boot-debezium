package com.himansh.dataobserver.listener;


import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public interface DebeziumListener {
    Executor executor = Executors.newFixedThreadPool(5);
    void handleChangeEvent(RecordChangeEvent<SourceRecord> sourceRecordRecordChangeEvent);
}
