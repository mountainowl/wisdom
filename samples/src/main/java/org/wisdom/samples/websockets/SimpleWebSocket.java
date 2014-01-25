package org.wisdom.samples.websockets;

import org.apache.felix.ipojo.annotations.Requires;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.*;
import org.wisdom.api.http.websockets.Publisher;

/**
 * A very simple controller handling web sockets.
 */
@Controller
public class SimpleWebSocket extends DefaultController {

    @Requires
    Publisher publisher;

    @Opened("/{name}")
    public void open(@Parameter("name") String name) {
        System.out.println("Web socket opened => " + name);
    }

    @Closed("/{name}")
    public void close(@Parameter("name") String name) {
        System.out.println("Web socket closed => " + name);
    }

    @OnMessage("/{name}")
    public void onMessage(@Body Message message, @Parameter("name") String name) {
        System.out.println("Receiving message on " + name + " : " + message.message);
        publisher.publish("/" + name, message.message.toUpperCase());
    }
}
