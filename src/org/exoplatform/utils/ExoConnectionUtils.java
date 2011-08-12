package org.exoplatform.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.exoplatform.controller.AppController;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

//interact with server
public class ExoConnectionUtils {
  private static int         splitLinesAt = 76;

  public static List<Cookie> _sessionCookies;      // Cookie array

  public static String       _strCookie   = "";    // Cookie string

  private static String      _strFirstLoginContent; // String data for the first
                                                    // time

  // login

  private static String      _fullDomainStr;       // Host

  private static byte[] zeroPad(int length, byte[] bytes) {
    byte[] padded = new byte[length]; // initialized to zero by JVM
    System.arraycopy(bytes, 0, padded, 0, bytes.length);
    return padded;
  }

  // Get string data for the first time login
  public static String getFirstLoginContent() {
    return _strFirstLoginContent;
  }

  private static String splitLines(String string) {
    String lines = "";
    for (int i = 0; i < string.length(); i += splitLinesAt) {
      lines += string.substring(i, Math.min(string.length(), i + splitLinesAt));
      lines += "\r\n";
    }
    return lines;

  }

  // Encode String to Base64String
  public static String stringEncodedWithBase64(String str) {
    String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789"
        + "+/";
    String encoded = "";
    byte[] stringArray;
    try {
      // use appropriate encoding string!
      stringArray = str.getBytes("UTF-8");
    } catch (Exception ignored) {
      // use local default rather than croak
      stringArray = str.getBytes();
    }
    // determine how many padding bytes to add to the output
    int paddingCount = (3 - (stringArray.length % 3)) % 3;
    // add any necessary padding to the input
    stringArray = zeroPad(stringArray.length + paddingCount, stringArray);
    // process 3 bytes at a time, churning out 4 output bytes
    // worry about CRLF insertions later
    for (int i = 0; i < stringArray.length; i += 3) {
      int j = ((stringArray[i] & 0xff) << 16) + ((stringArray[i + 1] & 0xff) << 8)
          + (stringArray[i + 2] & 0xff);

      encoded = encoded + base64code.charAt((j >> 18) & 0x3f) + base64code.charAt((j >> 12) & 0x3f)
          + base64code.charAt((j >> 6) & 0x3f) + base64code.charAt(j & 0x3f);
    }
    // replace encoded padding nulls with "="
    return splitLines(encoded.substring(0, encoded.length() - paddingCount)
        + "==".substring(0, paddingCount));
  }

  // Convert stream to String
  public static String convertStreamToString(InputStream is) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();

    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        sb.append(line + "\n");
      }
    } catch (IOException e) {
      // e.printStackTrace();
    } finally {
      // try
      // {
      // is.close();
      // }
      // catch (IOException e)
      // {
      // e.printStackTrace();
      // }
    }
    return sb.toString();
  }

  // Get sub URL path
  private static String getExtend(String domain) {
    // if(domain.indexOf("http://platform.demo.exoplatform") >= 0)
    // {
    // if(domain.equalsIgnoreCase("http://platform.demo.exoplatform.org"))
    // {
    // return "/portal/private/intranet";
    // }
    // return "ERROR";
    //
    // }

    return "/portal/private/intranet";
    /*
     * String context = ""; String strUrlContent = ""; try { URL url = new
     * URL(domain); HttpURLConnection con = (HttpURLConnection)
     * url.openConnection(); // set up url connection to get retrieve
     * information back con.setRequestMethod("GET"); con.setDoInput( true ); //
     * pull the information back from the URL InputStream ipstr =
     * con.getInputStream(); strUrlContent = convertStreamToString(ipstr);
     * con.disconnect(); } catch (ClientProtocolException e) { e.getMessage(); }
     * catch (IOException e) { e.getMessage(); }
     * if(strUrlContent.contains("error', '/main?url")) return "ERROR"; int
     * index = strUrlContent.indexOf("eXo.env.server.portalBaseURL = \"");
     * if(index > 0) { strUrlContent = strUrlContent.substring(index + 32, index
     * + 100); index = strUrlContent.indexOf(" ;"); strUrlContent =
     * strUrlContent.substring(0, index - 2); context =
     * strUrlContent.replace("public", "private"); } else { context =
     * "/portal/private/intranet"; } return context;
     */
  }

  // Send request with authentication
  public static String sendAuthentication(String domain, String username, String password) {

    try {
      HttpResponse response;
      HttpEntity entity;
      CookieStore cookiesStore;
      String strCookie = "";

      _fullDomainStr = getExtend(domain);

      if (_fullDomainStr.equalsIgnoreCase("ERROR"))
        return "ERROR";

      String redirectStr = domain.concat(_fullDomainStr);

      HttpParams httpParameters = new BasicHttpParams();
      HttpConnectionParams.setConnectionTimeout(httpParameters, 60000);
      HttpConnectionParams.setSoTimeout(httpParameters, 60000);
      HttpConnectionParams.setTcpNoDelay(httpParameters, true);

      DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);

      HttpGet httpGet = new HttpGet(redirectStr);

      response = httpClient.execute(httpGet);
      cookiesStore = httpClient.getCookieStore();
      List<Cookie> cookies = cookiesStore.getCookies();
      System.out.println("cookie" + cookies.size());

      if (!cookies.isEmpty()) {
        for (int i = 0; i < cookies.size(); i++) {
          strCookie = cookies.get(i).getName().toString() + "="
              + cookies.get(i).getValue().toString();
          System.out.println("strCookie" + strCookie);

        }
      }

      int indexOfPrivate = redirectStr.indexOf("/classic");
      
      // Request to login
      String loginStr;
      if (indexOfPrivate > 0)
        loginStr = redirectStr.substring(0, indexOfPrivate).concat("/j_security_check");
      else
        loginStr = redirectStr.concat("/j_security_check");

      HttpPost httpPost = new HttpPost(loginStr);
      List<NameValuePair> nvps = new ArrayList<NameValuePair>(2);
      nvps.add(new BasicNameValuePair("j_username", username));
      nvps.add(new BasicNameValuePair("j_password", password));
      httpPost.setEntity(new UrlEncodedFormEntity(nvps));
      httpPost.setHeader("Cookie", strCookie);
      _strCookie = strCookie;
      response = httpClient.execute(httpPost);
      entity = response.getEntity();

      _sessionCookies = new ArrayList<Cookie>(cookies);

      if (entity != null) {
        InputStream instream = entity.getContent();
        _strFirstLoginContent = convertStreamToString(instream);
        System.out.println("_strFirstLoginContent" + _strFirstLoginContent);
        if (_strFirstLoginContent.contains("Sign in failed. Wrong username or password.")) {
          return "NO";
        } else if (_strFirstLoginContent.contains("error', '/main?url")) {
          _strFirstLoginContent = null;
          return "ERROR";
        } else if (_strFirstLoginContent.contains("eXo.env.portal")) {
          return "YES";
        }
      } else {
        return "ERROR";
      }
    } catch (ClientProtocolException e) {
      return e.getMessage();
    } catch (IOException e) {
      return e.getMessage();
    }

    return "ERROR";
  }

  // Standalone gadget requset
  private String loginForStandaloneGadget(String domain, String username, String password) {
    try {
      HttpResponse response;
      HttpEntity entity;
      CookieStore cookiesStore;
      String strCookie = "";

      _fullDomainStr = getExtend(domain);

      if (_fullDomainStr.equalsIgnoreCase("ERROR"))
        return "ERROR";

      String redirectStr = domain.concat(_fullDomainStr);

      HttpParams httpParameters = new BasicHttpParams();
      HttpConnectionParams.setConnectionTimeout(httpParameters, 60000);
      HttpConnectionParams.setSoTimeout(httpParameters, 60000);
      HttpConnectionParams.setTcpNoDelay(httpParameters, true);

      DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);

      HttpGet httpGet = new HttpGet(redirectStr);

      response = httpClient.execute(httpGet);

      cookiesStore = httpClient.getCookieStore();
      List<Cookie> cookies = cookiesStore.getCookies();

      if (!cookies.isEmpty()) {
        for (int i = 0; i < cookies.size(); i++) {
          strCookie = cookies.get(i).getName().toString() + "=";
          strCookie = strCookie + cookies.get(i).getValue().toString()
              + ";domain=mobile.demo.exoplatform.org";
        }
      }

      /*
       * int indexOfPrivate = redirectStr.indexOf("/classic"); //Request to
       * login String loginStr =
       * "http://mobile.demo.exoplatform.org/portal/login"; if(indexOfPrivate >
       * 0) loginStr = redirectStr.substring(0,
       * indexOfPrivate).concat("/j_security_check"); else loginStr =
       * redirectStr.concat("/j_security_check");
       */

      HttpPost httpPost = new HttpPost("http://mobile.demo.exoplatform.org/portal/login");
      List<NameValuePair> nvps = new ArrayList<NameValuePair>(2);
      nvps.add(new BasicNameValuePair("username", username));
      nvps.add(new BasicNameValuePair("password", password));
      httpPost.setEntity(new UrlEncodedFormEntity(nvps));
      httpPost.setHeader("Cookie", strCookie);
      _strCookie = strCookie;
      response = httpClient.execute(httpPost);

      cookiesStore = httpClient.getCookieStore();
      cookies = cookiesStore.getCookies();

      if (!cookies.isEmpty()) {
        for (int i = 0; i < cookies.size(); i++) {
          strCookie = cookies.get(i).getName().toString() + "="
              + cookies.get(i).getValue().toString();
        }
      }
      _sessionCookies = cookies;

      entity = response.getEntity();

      if (entity != null) {
        InputStream instream = entity.getContent();
        _strFirstLoginContent = convertStreamToString(instream);
        if (_strFirstLoginContent.contains("Sign in failed. Wrong username or password.")) {
          return "NO";
        } else if (_strFirstLoginContent.contains("error', '/main?url")) {
          _strFirstLoginContent = null;
          return "ERROR";
        } else if (_strFirstLoginContent.contains("eXo.env.portal")) {
          return "YES";
        }
      } else {
        return "ERROR";
      }

    } catch (ClientProtocolException e) {
      return e.getMessage();
    } catch (IOException e) {
      return e.getMessage();
    }

    return "ERROR";
  }

  // Normal gadget request
  public static String sendRequestToGetGadget(String urlStr, String username, String password) {
    try {
      HttpResponse response;
      HttpEntity entity;
      CookieStore cookiesStore;
      String strCookie = "";

      String strRedirectUrl = urlStr;
      DefaultHttpClient httpClient = new DefaultHttpClient();
      HttpGet httpGet = new HttpGet(strRedirectUrl);

      response = httpClient.execute(httpGet);
      cookiesStore = httpClient.getCookieStore();
      List<Cookie> cookies = cookiesStore.getCookies();

      if (!cookies.isEmpty()) {
        for (int i = 0; i < cookies.size(); i++) {
          strCookie = cookies.get(i).getName().toString() + "="
              + cookies.get(i).getValue().toString();
        }
      }

      String strMarked = "/classic";
      int r = strRedirectUrl.indexOf(strMarked);
      String strLoginUrl = "";
      if (r >= 0)
        strLoginUrl = urlStr.substring(0, r).concat("/j_security_check");
      else {
        strLoginUrl = urlStr.concat("/j_security_check");
      }

      HttpPost httpPost = new HttpPost(strLoginUrl);
      List<NameValuePair> nvps = new ArrayList<NameValuePair>(2);
      nvps.add(new BasicNameValuePair("j_username", username));
      nvps.add(new BasicNameValuePair("j_password", password));
      httpPost.setEntity(new UrlEncodedFormEntity(nvps));
      httpPost.setHeader("Cookie", strCookie);
      _strCookie = strCookie;
      response = httpClient.execute(httpPost);
      entity = response.getEntity();

      if (entity != null) {
        InputStream instream = entity.getContent();
        String strResult = convertStreamToString(instream);
        return strResult;
      } else {
        return "ERROR";
      }
    } catch (ClientProtocolException e) {
      return e.getMessage();
    } catch (IOException e) {
      return e.getMessage();
    }

  }

  // Send request with authentication
  public static String authorizationHeader(String username, String password) {
    String s = "Basic ";
    String strAuthor = s + stringEncodedWithBase64(username + ":" + password);
    return strAuthor.substring(0, strAuthor.length() - 2);
  }

  // Get input stream from URL with authentication
  private InputStream sendRequestWithAuthorization(String urlStr) {

    InputStream ipstr = null;
    try {
      String strUserName = AppController.sharedPreference.getString(AppController.EXO_PRF_USERNAME,
                                                                    "exo_prf_username");
      String strPassword = AppController.sharedPreference.getString(AppController.EXO_PRF_PASSWORD,
                                                                    "exo_prf_password");

      urlStr = urlStr.replace(" ", "%20");

      URL url = new URL(urlStr);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();

      // set up url connection to get retrieve information back
      con.setRequestMethod("GET");
      con.setDoInput(true);

      // stuff the Authorization request header

      con.setRequestProperty("Authorization", authorizationHeader(strUserName, strPassword));

      // pull the information back from the URL
      ipstr = con.getInputStream();

      StringBuffer buf = new StringBuffer();

      int c;
      while ((c = ipstr.read()) != -1) {
        buf.append((char) c);
      }

      con.disconnect();

    } catch (ClientProtocolException e) {
      e.getMessage();
    } catch (IOException e) {
      e.getMessage();
    }

    return ipstr;
  }

  // Get string data with authentication request
  public static String sendRequestWithAuthorizationReturnString(String urlStr) {
    StringBuffer buf = new StringBuffer();
    try {
      String strUserName = AppController.sharedPreference.getString(AppController.EXO_PRF_USERNAME,
                                                                    "exo_prf_username");
      String strPassword = AppController.sharedPreference.getString(AppController.EXO_PRF_PASSWORD,
                                                                    "exo_prf_password");

      urlStr = urlStr.replace(" ", "%20");

      URL url = new URL(urlStr);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();

      // set up url connection to get retrieve information back
      con.setRequestMethod("GET");
      con.setDoInput(true);

      // stuff the Authorization request header

      con.setRequestProperty("Authorization", authorizationHeader(strUserName, strPassword));
      // pull the information back from the URL
      InputStream ipstr = con.getInputStream();

      int c;
      while ((c = ipstr.read()) != -1) {
        buf.append((char) c);
      }

      con.disconnect();

    } catch (ClientProtocolException e) {
      e.getMessage();
    } catch (IOException e) {
      e.getMessage();
    }

    return buf.toString();
  }

  // Get input stream from URL
  private static InputStream sendRequest(String strUrlRequest) {
    InputStream ipstr = null;
    try {
      HttpResponse response;
      HttpEntity entity;
      DefaultHttpClient httpClient = new DefaultHttpClient();
      HttpGet httpGet = new HttpGet(strUrlRequest);
      httpGet.setHeader("Cookie", _strCookie);
      response = httpClient.execute(httpGet);
      entity = response.getEntity();
      if (entity != null) {
        ipstr = entity.getContent();
      }
    } catch (ClientProtocolException e) {
      e.getMessage();
    } catch (IOException e) {
      e.getMessage();
    }
    return ipstr;
  }

  // Get string input stream from URL
  public static String sendRequestAndReturnString(String strUrlRequest) {
    return convertStreamToString(sendRequest(strUrlRequest));
  }

  /*
   * Check the version of PLF return true if version number is >= 3.5 else
   * return false
   */
  public static boolean checkPLFVersion() {
    try {
      String versionUrl = SocialActivityUtil.getDomain() + "/portal/rest/platform/version";
      String result = sendRequestAndReturnString(versionUrl);
      JSONObject json = (JSONObject) JSONValue.parse(result);
      String verObject = json.get("platformVersion").toString();
      int index = verObject.lastIndexOf(".");
      String verNumber = verObject.substring(0, index);
      float num = Float.parseFloat(verNumber);
      if (num < 3.5) {
        return false;
      } else
        return true;
    } catch (RuntimeException e) {
      return false;
    }

  }
}
