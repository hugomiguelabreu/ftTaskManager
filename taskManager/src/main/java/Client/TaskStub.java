package Client;

import Interfaces.Task;
import Network.*;
import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.transport.Transport;
import io.atomix.catalyst.transport.netty.NettyTransport;

import java.util.ArrayList;
import java.util.UUID;

public class TaskStub implements Task {

    private Transport t;
    private ThreadContext tc;
    private Connection c;
    private int currentServer;
    private final Address primary = new Address("127.0.0.1:5000");
    private String uid;

    public TaskStub(){
        currentServer = 0;
        t = new NettyTransport();
        tc = new SingleThreadContext("cli-%d", new Serializer());
        tc.serializer().register(AddTasksReq.class);
        tc.serializer().register(AddTasksRep.class);
        tc.serializer().register(CompleteTaskReq.class);
        tc.serializer().register(CompleteTaskRep.class);
        tc.serializer().register(GetTaskReq.class);
        tc.serializer().register(GetTaskRep.class);
        generateUUID();
        connect();
    }

    @Override
    public boolean addTask(String uri) {
        AddTasksRep result;
        try {
            result = (AddTasksRep) tc.execute(() ->
                    c.sendAndReceive(new AddTasksReq(uid, uri))
            ).join().get();
        }catch (Exception e){
            if(connect())
                return this.addTask(uri);
            else
                return false;
        }

        generateUUID();
        return result.result;
    }

    @Override
    public void setUncompleted(String uri) { }

    @Override
    public boolean completeTask(String uri, ArrayList<String> tasks) {
        CompleteTaskRep result;
        try {
            result = (CompleteTaskRep) tc.execute(() ->
                    c.sendAndReceive(new CompleteTaskReq(uid, uri, tasks))
            ).join().get();
        }catch (Exception e){
            e.printStackTrace();
            if(connect())
                return this.completeTask(uri, tasks);
            else
                return false;
        }

        generateUUID();
        return result.response;
    }

    @Override
    public String getTask() {
        GetTaskRep result = null;
        try {
            result = (GetTaskRep) tc.execute(() ->
                    c.sendAndReceive(new GetTaskReq(uid))
            ).join().get();
        }catch (Exception e){
            if(connect())
                return this.getTask();
            else
                return null;
        }

        generateUUID();
        return result.uri;
    }

    private void generateUUID(){
        uid = UUID.randomUUID().toString().split("-")[4];
    }

    private boolean connect(){
        for(int i = 0; i < 10; i++){
            try{
                c = tc.execute(() ->
                        t.client().connect(primary)
                ).join().get();
                System.out.println("Connected to primary server");
                return true;
            }
            catch (Exception e){
                System.out.println("Primary server is down, trying again.");
            }
        }
        return false;
    }

}
