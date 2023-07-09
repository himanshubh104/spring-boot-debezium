package com.himansh.dataobserver.util;

import com.himansh.dataobserver.constant.Entity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
public class EventObjectUtil {

    public Map<String, Object> getObjectMap(Struct struct) {
        return struct.schema().fields().stream()
                .map(Field::name)
                .filter(fieldName -> struct.get(fieldName) != null)
                .map(fieldName -> Pair.of(fieldName, struct.get(fieldName)))
                .collect(toMap(Pair::getKey, Pair::getValue));
    }

    public boolean compareBeforeAfter(Map<String, Object> beforeObj, Map<String, Object> afterObj, String updatedTable) {
        try {
            Entity entity = Entity.valueOf(updatedTable.toUpperCase());
            for (String field : entity.columnSet) {
                Object beforeValue = beforeObj.get(field);
                Object afterValue = afterObj.get(field);
                if (!Objects.equals(beforeValue, afterValue)) {
                    log.info("difference in values found, field: {}, before: {}, after: {}", field, beforeValue, afterValue);
                    return true;
                }
            }
        } catch (IllegalArgumentException e) {
            log.error("Table: {} entry is not present", updatedTable);
        }
//        for (var entry : beforeObj.entrySet()) {
//            String field = entry.getKey();
//            Object beforeValue = entry.getValue();
//            if (!entity.columnSet.contains(field)) {
//                continue;
//            } else {
//                Object afterValue = afterObj.get(field);
//                if ((Objects.nonNull(beforeValue) && !beforeValue.equals(afterValue)) ||
//                        (Objects.nonNull(afterValue) && !afterValue.equals(beforeValue))) {
//                    log.info("difference in values found, field: {}, before: {}, after: {}", field, beforeValue, afterValue);
//                    return true;
//                }
//            }
//        }
        return false;
    }
}
