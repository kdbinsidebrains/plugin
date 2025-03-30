package org.kdb.inside.brains.psi;

public enum ElementScope {
    // global
    FILE,
    CONTEXT,
    // private
    DICT,
    TABLE,
    QUERY,
    LAMBDA,
    // lambda
    PARAMETERS
}
