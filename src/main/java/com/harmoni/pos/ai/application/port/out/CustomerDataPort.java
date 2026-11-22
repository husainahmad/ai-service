package com.harmoni.pos.ai.application.port.out;

import com.harmoni.pos.ai.domain.model.CustomerMetrics;
import com.harmoni.pos.ai.domain.model.CustomerProfile;

import java.util.Optional;

public interface CustomerDataPort {

    Optional<CustomerProfile> findCustomer(long customerId);

    CustomerMetrics getMetrics(long customerId);
}
