package com.company.aem.additional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(immediate = true, service = HelloService.class)
class HelloService {
    // intentionally empty
}