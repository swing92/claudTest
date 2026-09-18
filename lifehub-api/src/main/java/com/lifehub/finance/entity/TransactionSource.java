package com.lifehub.finance.entity;

/**
 * Where a transaction row came from. Only MANUAL is ever written today — SYNCED is reserved for
 * future bank/card auto-sync (open banking / MyData), which is not implemented yet.
 */
public enum TransactionSource {
    MANUAL,
    SYNCED
}
