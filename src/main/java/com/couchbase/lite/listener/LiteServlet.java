package com.couchbase.lite.listener;

import Acme.Serve.Serve;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.router.RequestAuthorization;
import com.couchbase.lite.router.Router;
import com.couchbase.lite.router.RouterCallbackBlock;
import com.couchbase.lite.router.URLConnection;
import com.couchbase.lite.util.Log;

import javax.net.ssl.SSLSocket;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("serial")
public class LiteServlet extends HttpServlet {

    private Manager manager;
    private LiteListener listener;
    public static final String TAG = "LiteServlet";
    private final RequestAuthorization requestAuthorization;

    /**
     *
     * @param manager
     * @param requestAuthorization This can be null if no authorize check is being used
     */
    public LiteServlet(Manager manager, RequestAuthorization requestAuthorization) {
        super();
        this.manager = manager;
        this.requestAuthorization = requestAuthorization;
    }

    @Override
    public void service(HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        //set path
        String urlString = request.getRequestURI();
        String queryString = request.getQueryString();
        if(queryString != null) {
            urlString += "?" + queryString;
        }
        URL url = new URL(LiteServer.CBL_URI_SCHEME +  urlString);
        final URLConnection conn = (URLConnection)url.openConnection();
        conn.setDoOutput(true);

        //find SSL session, if any
        Serve.ServeConnection serveConnection = (Serve.ServeConnection)request; // This only works for TJWS
        if (serveConnection.getSocket() instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) serveConnection.getSocket();
            conn.setSSLSession(sslSocket.getSession());
        }

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
        Log.v(Database.TAG, String.format("Buffer size is %d", response.getBufferSize()));

        final CountDownLatch doneSignal = new CountDownLatch(1);

        final Router router = new Router(manager, conn, requestAuthorization);

        RouterCallbackBlock callbackBlock = new RouterCallbackBlock() {

            @Override
            public void onResponseReady() {
                //set the response code
                response.setStatus(conn.getResponseCode());

                //add the resonse headers
                Map<String, List<String>> headers = conn.getHeaderFields();
                if(headers != null) {
                    for (String headerName : headers.keySet()) {
                        for (String headerValue : headers.get(headerName)) {
                            response.addHeader(headerName, headerValue);
                        }
                    }
                }

                doneSignal.countDown();
            }

        };

        router.setCallbackBlock(callbackBlock);

        synchronized (manager) {
            router.start();
        }

        try {
            doneSignal.await();
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
            Log.e(Database.TAG, "Interrupted waiting for result", e);
        } finally {
            if(router != null) {
                router.stop();
            }
        }

    }
}
