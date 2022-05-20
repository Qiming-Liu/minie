package uk.ac.ucl.cs.mr;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author Pasquale Minervini
 */

public class Main {

    public static void main(String[] args) {

        ProcessJsonMinIE.process(args[0], Integer.parseInt(args[1]));
    }

    public static ResourceConfig create() {
        return new MinIEService();
    }
}
