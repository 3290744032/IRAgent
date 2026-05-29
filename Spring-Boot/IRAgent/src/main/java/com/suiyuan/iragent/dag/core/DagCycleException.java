package com.suiyuan.iragent.dag.core;

public class DagCycleException extends RuntimeException {
    public DagCycleException(String message) {
        super(message);
    }
}
