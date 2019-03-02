package com.socketanywhere.httpsocketdebug;


public class ServerRunner {
//    public static void run(Class serverClass) {
//        try {
//            executeInstance((NanoHTTPD) serverClass.newInstance());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void executeInstance(NanoHTTPD server) {
        try {
            server.start();
        } catch (Throwable ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
        }

        System.out.println("Debug Server started.\n");

//        try {
//            System.in.read();
//        } catch (Throwable ignored) {
//        }
//
//        server.stop();
//        System.out.println("Server stopped.\n");
    }
}