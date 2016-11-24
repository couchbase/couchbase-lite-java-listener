# TJWS

## Project Page
http://tjws.sourceforge.net/

## github repo
https://github.com/drogatkin/TJWS2

## commit hash value CBL adopted
https://github.com/drogatkin/TJWS2/commit/a50a6eca875371f11955ad4fd8f94958e39ff0fc

# Changes from original commit
- two method signatures are modfied to make the code compilable.

https://github.com/drogatkin/TJWS2/blob/master/1.x/src/Acme/Serve/Serve.java#L443
https://github.com/drogatkin/TJWS2/blob/master/1.x/src/Acme/Serve/Serve.java#L518

from:
```java
public javax.servlet.ServletRegistration.Dynamic addServlet(String urlPat, String className) {
public javax.servlet.ServletRegistration.Dynamic addServlet(String urlPat, Servlet servlet) {
```
to:
```java
public void addServlet(String urlPat, String className) {
public void addServlet(String urlPat, Servlet servlet) {
```

# Notes
- Previously CBL depends on TJWS jar file. Instead, couchbase-lite-java-listener incorporates tjws codes. CBL only incorporates `Acme.Serve` package classes. 
