package Server;

import Interfaces.Task;
import Network.*;
import io.atomix.catalyst.concurrent.Futures;
import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Transport;
import io.atomix.catalyst.transport.netty.NettyTransport;
import pt.haslab.ekit.Spread;
import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.*;

public class Server {

    static boolean isPrimary = false;

    public static void main(String[] args) throws SpreadException {
        HashMap<String, String> userHandling = new HashMap<>();
        Set<String> membersBeforeMe = new HashSet<>();
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

            //Server network;
            sp.open().thenRun(() -> {
                sp.join("CRAWLERS");
                System.out.println("\u001B[32mCONNECTED TO GROUP\u001B[0m");
            });

            sp.handler(AddTasksRep.class, (s, m) -> {
                System.out.println("A SERIO?");
            });

            //Nova task
            sp.handler(AddTasksReq.class, (s, m) -> {
                if(!s.getSender().toString().equals(sp.getPrivateGroup().toString())){
                    System.out.println("NOVA TASK");
                    sp.multicast(createMessage(s.getSender()), new AddTasksRep(true));
                }
            });
            //Colocar task no ongoing
            sp.handler(GetTaskReq.class, (s, m) -> {
                System.out.println("PREMUTAR PARA EM TRATAMENTO");
            });
            //Completar a task
            sp.handler(CompleteTaskReq.class, (s, m) -> {
                System.out.println("TASK COMPLETA");
            });

            sp.handler(MembershipInfo.class, (s, m) -> {
                if(membersBeforeMe.size() == 0)
                    for (SpreadGroup sg : m.getMembers())
                        membersBeforeMe.add(sg.toString());
                if(m.isCausedByLeave() || m.isCausedByDisconnect())
                    membersBeforeMe.remove(m.getDisconnected().toString());
                if(membersBeforeMe.size() == 1 && !isPrimary)
                    startPrimary(t, 5000, tasks, userHandling, sp);
            });
        });

    }

    private static SpreadMessage createMessage(SpreadGroup sg){
        SpreadMessage sm = new SpreadMessage();
        if(sg == null)
            sm.addGroup("CRAWLERS");
        else
            sm.addGroup(sg);
        sm.setFifo();
        sm.setReliable();
        return sm;
    }

    private static void startPrimary(Transport t, int port, Task tasks, HashMap<String, String> userHandling, Spread sp){
        //Client server;
        System.out.println("\u001B[34mINITIALIZING PRIMARY SERVER\u001B[0m");
        t.server().listen(new Address("127.0.0.1", port), conn -> {
            conn.handler(AddTasksReq.class, (m) -> {
                System.out.println("New task");
                boolean result = tasks.addTask(m.uri);
                sp.multicast(createMessage(null), m);
                return Futures.completedFuture(new AddTasksRep(result));
            });
            conn.handler(GetTaskReq.class, (m) -> {
                System.out.println("Get task");
                String uri = tasks.getTask();
                System.out.println(uri);
                userHandling.put(conn.toString(), uri);
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
            //Um cliente vai abaixo vamos colocar
            //a task dele a não completa
            conn.onClose(connection -> {
                String user = connection.toString();
                String uri = userHandling.get(user);
                tasks.setUncompleted(uri);
                System.out.println("Client closed. Task reinserted.");
            });
        });
        isPrimary = true;
    }

}
