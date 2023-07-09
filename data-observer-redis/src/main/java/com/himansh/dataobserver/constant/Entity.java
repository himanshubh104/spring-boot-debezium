package com.himansh.dataobserver.constant;

import java.util.Set;

public enum Entity {
    CUSTOMERS(Set.of("ID","NAME","AGE","SEX"));
    public final Set<String> columnSet;
    Entity(Set<String> columnSet) {
        this.columnSet = columnSet;
    }
}
