package com.hubspot.baragon.service.exceptions;

import com.hubspot.baragon.service.platform.http.MediaType;
import com.hubspot.baragon.service.platform.http.Response;
import com.hubspot.baragon.service.platform.http.WebApplicationException;

public class BaragonNotFoundException extends WebApplicationException {

  /**
   * Create a HTTP 404 (Not Found) exception.
   */
  public BaragonNotFoundException() {
    super(Response.Status.NOT_FOUND);
  }

  /**
   * Create a HTTP 404 (Not Found) exception.
   * @param message the String that is the entity of the 404 response.
   */
  public BaragonNotFoundException(String message) {
    super(
      Response.status(Response.Status.NOT_FOUND).entity(message).type(MediaType.TEXT_PLAIN).build()
    );
  }
}
