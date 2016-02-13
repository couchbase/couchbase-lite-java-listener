package com.couchbase.lite.listener;

import com.couchbase.lite.Manager;
import com.couchbase.lite.router.Router;
import com.couchbase.lite.router.RouterCallbackBlock;
import com.couchbase.lite.router.URLConnection;
import com.couchbase.lite.support.Base64;
import com.couchbase.lite.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class LiteServlet extends HttpServlet {

    private Manager manager;
    private LiteListener listener;

    // if this is non-null, then users must present BasicAuth
    // credential in every request which matches up with allowedCredentials,
    // or else the request will be refused.
    // REF: https://github.com/couchbase/couchbase-lite-java-listener/issues/35
    private Credentials allowedCredentials;

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public void setListener(LiteListener listener) {
        this.listener = listener;
    }

    public void setAllowedCredentials(Credentials allowedCredentials) {
        this.allowedCredentials = allowedCredentials;
    }

    @Override
    public void service(HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        Credentials requestCredentials = credentialsWithBasicAuthentication(request);

        if (allowedCredentials != null && !allowedCredentials.empty()) {
            if (requestCredentials == null || !requestCredentials.equals(allowedCredentials)) {
                Log.d(Log.TAG_LISTENER, "Unauthorized -- requestCredentials not given or do not match allowed credentials");
                response.setHeader("WWW-Authenticate", "Basic realm=\"Couchbase Lite\"");
                response.setStatus(401);
                return;
            }
            Log.v(Log.TAG_LISTENER, "Authorized via basic auth");
        } else {
            Log.v(Log.TAG_LISTENER, "Not enforcing basic auth -- allowedCredentials null or empty");
        }

        //set path
        String urlString = request.getRequestURI();
        String queryString = request.getQueryString();
        if(queryString != null) {
            urlString += "?" + queryString;
        }
        URL url = new URL(LiteServer.CBL_URI_SCHEME +  urlString);
        final URLConnection conn = (URLConnection)url.openConnection();
        conn.setDoOutput(true);

        //set the method
        conn.setRequestMethod(request.getMethod());

        //set the headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            conn.setRequestProperty(headerName, request.getHeader(headerName));
        }

        //set the body
        InputStream is = request.getInputStream();
        if(is != null) {
            conn.setDoInput(true);
            conn.setRequestInputStream(is);
        }

        final ServletOutputStream os = response.getOutputStream();
        response.setBufferSize(128);

        final CountDownLatch doneSignal = new CountDownLatch(1);

        final Router router = new Router(manager, conn);

        RouterCallbackBlock callbackBlock = new RouterCallbackBlock() {
            @Override
            public void onResponseReady() {
                Log.v(Log.TAG_LISTENER, "RouterCallbackBlock.onResponseReady() START");
                //set the response code
                response.setStatus(conn.getResponseCode());
                //add the resonse headers
                Map<String, List<String>> headers = conn.getHeaderFields();
                if (headers != null) {
                    for (String headerName : headers.keySet()) {
                        for (String headerValue : headers.get(headerName)) {
                            response.addHeader(headerName, headerValue);
                        }
                    }
                }
                doneSignal.countDown();
                Log.v(Log.TAG_LISTENER, "RouterCallbackBlock.onResponseReady() END");
            }
        };

        router.setCallbackBlock(callbackBlock);

        // set remote URL as source
        router.setSource(remoteURL(request));

        router.start();

        try {
            Log.v(Log.TAG_LISTENER, "CountDownLatch.await() START");
            doneSignal.await();
            Log.v(Log.TAG_LISTENER, "CountDownLatch.await() END");

            InputStream responseInputStream = conn.getResponseInputStream();
            final byte[] buffer = new byte[65536];
            int r;
            while ((r = responseInputStream.read(buffer)) > 0) {
                os.write(buffer, 0, r);
                os.flush();
                response.flushBuffer();
            }
            os.close();
        } catch (InterruptedException e) {
            Log.e(Log.TAG_LISTENER, "Interrupted waiting for result", e);
        } finally {
            if (router != null) {
                router.stop();
            }
        }
    }

    public Credentials credentialsWithBasicAuthentication(HttpServletRequest req) {
        try {
            String authHeader = req.getHeader("Authorization");
            if (authHeader != null) {
                StringTokenizer st = new StringTokenizer(authHeader);
                if (st.hasMoreTokens()) {
                    String basic = st.nextToken();
                    if (basic.equalsIgnoreCase("Basic")) {
                        try {
                            String credentials = new String(Base64.decode(st.nextToken()), "UTF-8");
                            Log.v(Log.TAG_LISTENER, "Credentials: ", credentials);
                            int p = credentials.indexOf(":");
                            if (p != -1) {
                                String login = credentials.substring(0, p).trim();
                                String password = credentials.substring(p + 1).trim();

                                return new Credentials(login, password);
                            } else {
                                Log.e(Log.TAG_LISTENER, "Invalid authentication token");
                            }
                        } catch (Exception e) {
                            Log.w(Log.TAG_LISTENER, "Couldn't retrieve authentication", e);
                        }
                    }
                }
            } else {
                Log.d(Log.TAG_LISTENER, "authHeader is null");
            }
        } catch (Exception e) {
            Log.e(Log.TAG_LISTENER, "Exception getting basic auth credentials", e);
        }
        return null;
    }

    private URL remoteURL(HttpServletRequest req) {
        String addr = req.getRemoteAddr();
        Log.v(Log.TAG_LISTENER, "remoteURL() addr=" + addr);
        if (!"127.0.0.1".equals(addr) && !"::1".equals(addr)) {
            // IPv6
            if (addr.indexOf(':') >= 0)
                addr = String.format("[%s]", addr);

            String username = allowedCredentials != null ? allowedCredentials.getLogin() : null;
            String protocol = req.isSecure() ? "https" : "http";
            String spec = null;
            if(username != null && username.length() > 0)
                spec = String.format("%s://%s@%s/", protocol, username, addr);
            else
                spec = String.format("%s://%s/", protocol, addr);
            try {
                return new URL(spec);
            } catch (MalformedURLException e) {
                Log.w(Log.TAG_LISTENER, "failed to create remote URL", e);
                return null;
            }
        }
        return null;
    }
}
