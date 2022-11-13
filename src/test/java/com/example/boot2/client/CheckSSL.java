package com.example.boot2.client;

import java.net.URL;

/**
 * A main manual class to check SSL access works OK.
 */
public class CheckSSL {

  public static void main(String[] args) throws Exception {
    try
    {
      new URL( args[0] ).openConnection().getInputStream();
      System.out.println( "Steve this Succeeded." );
    }
    catch( javax.net.ssl.SSLHandshakeException e )
    {
      System.out.println( "SSL exception." );
    }
  }
}
