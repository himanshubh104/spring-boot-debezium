package com.himansh.dataobserver.listener.impl;

import com.himansh.dataobserver.constant.EntityConstants;
import com.himansh.dataobserver.listener.DebeziumListener;
import com.himansh.dataobserver.util.EventObjectUtil;
import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static io.debezium.data.Envelope.FieldName.*;
import static io.debezium.data.Envelope.Operation;


@Slf4j
@Component
public class EventListenerImpl implements DebeziumListener {
    private final DebeziumEngine<RecordChangeEvent<SourceRecord>> debeziumEngine;
    private final EventObjectUtil eventObjectUtil;

    public EventListenerImpl(Configuration customerConnectorConfiguration, EventObjectUtil eventObjectUtil) {
        this.debeziumEngine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(customerConnectorConfiguration.asProperties())
                .notifying(this::handleChangeEvent)
                .build();
        this.eventObjectUtil = eventObjectUtil;
    }

    @Override
    public void handleChangeEvent(RecordChangeEvent<SourceRecord> sourceRecordRecordChangeEvent) {
        SourceRecord sourceRecord = sourceRecordRecordChangeEvent.record();
        // log.info("Key = {}, Value = {}", sourceRecord.key(), sourceRecord.value());
        Struct sourceRecordChangeValue = (Struct) sourceRecord.value();
        // log.info("SourceRecordChangeValue = '{}'", sourceRecordChangeValue);
        if (sourceRecordChangeValue != null) {
            Operation operation = Operation.forCode((String) sourceRecordChangeValue.get(OPERATION));
            log.info("Operation = {}", operation);
            Struct source = (Struct) sourceRecordChangeValue.get(SOURCE);
            String updatedTable = source.getString("table");
            log.info("Updated table = {}", updatedTable);

            // Operation.READ operation events are always triggered when application initializes
            // We're only interested in CREATE operation which are triggered upon new update registry
            if (operation == Operation.UPDATE) {
                Struct beforeStruct = (Struct) sourceRecordChangeValue.get(BEFORE);
                Map<String, Object> beforeObj = eventObjectUtil.getObjectMap(beforeStruct);
                Struct afterStruct = (Struct) sourceRecordChangeValue.get(AFTER);
                Map<String, Object> afterObj = eventObjectUtil.getObjectMap(afterStruct);
                Integer custId = (Integer) afterObj.get(EntityConstants.CUST_ID);
                boolean status = eventObjectUtil.compareBeforeAfter(beforeObj, afterObj, updatedTable);
                if (status) {
                    log.info("Sending event to External Service for cust_id: {}", custId);
                }
            }
        }

    }

    @PostConstruct
    private void start() {
        executor.execute(debeziumEngine);
    }

    @PreDestroy
    private void stop() throws IOException {
        if (Objects.nonNull(this.debeziumEngine)) {
            this.debeziumEngine.close();
        }
    }
}
