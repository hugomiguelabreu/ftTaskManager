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

public class TaskStub implements Task {

    private Transport t;
    private ThreadContext tc;
    private Connection c;
    private int currentServer;
    private final Address[] addresses = new Address[]{
            new Address("127.0.0.1:5000"),
            new Address("127.0.0.1:5001"),
            new Address("127.0.0.1:5002"),
            new Address("127.0.0.1:5003"),
            new Address("127.0.0.1:5004")
    };

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
    }

    @Override
    public void addTask(String uri) {

    }

    @Override
    public void setUncompleted(String uri) {

    }

    @Override
    public void completeTask(String uri) {

    }

    @Override
    public String getTask() {
        return null;
    }

    private boolean connect(){
        for(int i = currentServer; i<addresses.length; i++){
            try{
                Address server = addresses[i];
                c = tc.execute(() ->
                        t.client().connect(server)
                ).join().get();
                currentServer = i;
                return true;
            }
            catch (Exception e){
                System.out.println("Server " +  i + " is down.");
            }
        }
        return false;
    }

}
