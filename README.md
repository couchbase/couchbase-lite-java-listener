## couchbase-lite-java-listener

This provides a webserver wrapper around Couchbase-Lite so that it can be called via HTTP REST calls.

## How to add to your project

This can be added either via a source dependency or a maven artifact dependency.

See the [couchbase-lite-android-liteserv](https://github.com/couchbaselabs/couchbase-lite-android-liteserv), which provides an example with both dependency styles.

## TJWS dependency

Couchbase-lite android depends on the [Tiny Java Web Server and Servlet Container](http://tjws.sourceforge.net/).  See libs-src/Webserver-194-README.md for more details.

## Ektorp Gotchas

If you are connecting to CBLiteListener from a client that uses Ektorp, you may run into issues: issue #5, issue #7, or issue #8.

The workaround is to call:

```
StdHttpClient.Builder builder = new StdHttpClient.Builder().host(hostName).port(port).useExpectContinue(false);
return builder.build();
```

(the key point being to use `useExpectContinue(false)`)

