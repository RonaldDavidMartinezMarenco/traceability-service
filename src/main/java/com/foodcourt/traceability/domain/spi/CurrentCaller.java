package com.foodcourt.traceability.domain.spi;

import com.foodcourt.traceability.domain.model.Caller;

/** Reads the authenticated caller. The domain does not import a security framework. */
public interface CurrentCaller {

    Caller require();
}
