package com.company.aem.example.core;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = CoreService.class)
class CoreService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CoreService.class);

  @Activate
  protected void activate() {
    LOGGER.info("Hello core!");
  }

  @Deactivate
  protected void deactivate() {
    LOGGER.info("Good bye core!");
  }

}