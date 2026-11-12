package com.hubspot.baragon.service.web;

import java.beans.PropertyEditorSupport;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * How the values of a request's parameters are read. The editors a web framework falls back on read a
 * value of blanks as no value at all, and read a number written in hexadecimal or with blanks around
 * it; the API has always read the value of a parameter as the plain decimal number it is, and
 * answered a request whose parameter holds anything else as it answers one it cannot read. Only a
 * value written with nothing at all in it is a parameter the request does not carry.
 */
@ControllerAdvice
public class ParameterBinding {

  @InitBinder
  public void readNumbersExactly(WebDataBinder binder) {
    for (Class<?> type : new Class<?>[] { Integer.class, int.class }) {
      binder.registerCustomEditor(
        type,
        new PropertyEditorSupport() {

          @Override
          public void setAsText(String text) {
            setValue(
              text == null || text.trim().isEmpty() ? Integer.valueOf(0) : Integer.valueOf(text)
            );
          }
        }
      );
    }
    for (Class<?> type : new Class<?>[] { Long.class, long.class }) {
      binder.registerCustomEditor(
        type,
        new PropertyEditorSupport() {

          @Override
          public void setAsText(String text) {
            setValue(
              text == null || text.trim().isEmpty() ? Long.valueOf(0L) : Long.valueOf(text)
            );
          }
        }
      );
    }
  }
}
