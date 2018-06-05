package Server;

import Interfaces.Task;
import Network.*;
import io.atomix.catalyst.concurrent.Futures;
import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.transport.Transport;
import io.atomix.catalyst.transport.netty.NettyTransport;
import pt.haslab.ekit.Spread;
import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadMessage;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Server {

    public static void main(String[] args) throws SpreadException {
        Task tasks = new TaskImpl();
        Transport t = new NettyTransport();;
        ThreadContext tc = new SingleThreadContext("srv-%d", new Serializer());
        Spread sp = new Spread("server-" + UUID.randomUUID().toString().split("-")[4], true);

        tc.serializer().register(AddTasksReq.class);
        tc.serializer().register(AddTasksRep.class);
        tc.serializer().register(CompleteTaskReq.class);
        tc.serializer().register(CompleteTaskRep.class);
        tc.serializer().register(GetTaskReq.class);
        tc.serializer().register(GetTaskRep.class);

        tc.execute(() -> {
            //Client server;
            t.server().listen(new Address("127.0.0.1", Integer.parseInt(args[0])), conn -> {
                conn.handler(AddTasksReq.class, (m) -> {
                    System.out.println("New task");
                    boolean result = tasks.addTask(m.uri);
                    return Futures.completedFuture(new AddTasksRep(result));
                });
                conn.handler(GetTaskReq.class, (m) -> {
                    System.out.println("Get task");
                    String uri = tasks.getTask();
                    System.out.println(uri);
                    return Futures.completedFuture(new GetTaskRep(uri));
                });
                conn.handler(CompleteTaskReq.class, (m) -> {
                    System.out.println("Complete task");
                    String taskEnded = m.uri;
                    ArrayList<String> newTasks = m.tasks;
                    System.out.println(taskEnded);
                    boolean result = tasks.completeTask(taskEnded, newTasks);
                    return Futures.completedFuture(new CompleteTaskRep(result));
                });
            });

            //Server network;
            sp.open().thenRun(() -> {
                sp.join("CRAWLERS");
                System.out.println("CONNECTED TO GROUP");
            });

            sp.handler(MembershipInfo.class, (s, m) -> {
                System.out.println(m.getGroup());
                System.out.println(m.isCausedByJoin());
                System.out.println(m.isCausedByDisconnect());
            });

        });
    }

}
