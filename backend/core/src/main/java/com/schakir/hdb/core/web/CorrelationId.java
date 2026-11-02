package com.schakir.hdb.core.web;

/** The MDC key under which the correlation id is stored (generation/propagation: #13). */
public final class CorrelationId {

  public static final String MDC_KEY = "correlationId";

  private CorrelationId() {}
}
