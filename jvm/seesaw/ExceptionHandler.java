package seesaw;
/*
 * Copyright (c) Dave Ray, 2011. All rights reserved.
 *
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this 
 *   distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 *   the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 */

/**
 * Support class for (seesaw.debug). I was using proxy, but proxy-super kept
 * failing after catching the first exception. So AOT it is :(
 */
public class ExceptionHandler extends java.awt.EventQueue {

  private clojure.lang.IFn handler;

  public ExceptionHandler() {
    // nothing
  }

  public clojure.lang.IFn getHandler() {
    return handler;
  }

  public ExceptionHandler setHandler(clojure.lang.IFn handler) {
    this.handler = handler;
    return this;
  }

  public void dispatchEvent(java.awt.AWTEvent event) {
    if(handler != null) {
      try {
        super.dispatchEvent(event);
      } catch (Throwable ex) {
        if(handler != null) {
          try {
            handler.invoke(event, ex);
          }
          catch (Exception handlerEx) {
            throw new RuntimeException(handlerEx.getMessage(), handlerEx);
          }
        }
      }
    }
    else {
      super.dispatchEvent(event);
    }
  }
}
