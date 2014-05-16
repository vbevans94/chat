package chat.server;

import thrift.entity.Chat;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

public class Main {

    public static ChatHandler handler = new ChatHandler();

    public static Chat.Processor processor = new Chat.Processor(handler);

    public static void main(String[] args) {
        Runnable simple = new Runnable() {
            public void run() {
                simple(processor);
            }
        };

        new Thread(simple).start();
    }

    public static void simple(Chat.Processor processor) {
        try {
            TServerTransport serverTransport = new TServerSocket(9090);
            TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

            System.out.println("Starting the simple server...");
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
