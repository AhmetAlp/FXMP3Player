JVM Settings

--module-path "&lt;PATH&gt;/javafx-sdk-21.0.5/lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics --add-reads javafx.base=ALL-UNNAMED --add-reads javafx.graphics=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED --add-opens javafx.graphics/com.sun.glass.utils=ALL-UNNAMED --add-opens javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED -Djavax.net.ssl.trustStore="<PATH>\lib\security\cacerts" -Djavax.net.ssl.trustStorePassword=&lt;PASSWORD&gt;
