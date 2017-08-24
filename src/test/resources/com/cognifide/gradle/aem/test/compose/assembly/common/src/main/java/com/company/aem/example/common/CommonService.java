package com.company.aem.example.common;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    service = CommonService.class
)
class CommonService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommonService.class);

  @Activate
  protected void activate() {
    LOGGER.info("Hello common!");
  }

  @Deactivate
  protected void deactivate() {
    LOGGER.info("Good bye common!");
  }

}