package Server;

import Interfaces.Task;
import Network.*;
import Network.Spread.*;
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
import java.util.concurrent.CompletableFuture;

public class Server {

    static boolean isPrimary = false;

    public static void main(String[] args) throws SpreadException {
        //Mapa de conexões para saber que uri estava a tratar um client
        HashMap<String, String> userHandling = new HashMap<>();
        //Mapa para as respostas depois dos acks e pedidos repetidos;
        HashMap<String, Map.Entry<Object, CompletableFuture>> responses = new HashMap<>();
        //Mapa para os acks dos backups;
        HashMap<String, Set<String>> acks = new HashMap<>();
        //Set de servidores antes de mim a serem primários;
        Set<String> membersBeforeMe = new HashSet<>();
        //Servidores da vista
        Set<String> spreadGroups = new HashSet<>();
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

            sp.handler(Ack.class, (s, m) -> {
                System.out.println("ACK");
                //Obter os membros que faltam dar ACK
                Set members = acks.get(m.id);
                //Remover este dos que faltam
                members.remove(s.getSender().toString());
                //Se só falto eu, completo a resposta com o objeto correto;
                if(members.size() == 1) {
                    responses.get(m.id).getValue().complete(responses.get(m.id).getKey());
                    acks.remove(m.id);
                }
            });

            //Nova task
            sp.handler(AddTasksSpreadReq.class, (s, m) -> {
                if(!s.getSender().toString().equals(sp.getPrivateGroup().toString())){
                    System.out.println("NOVA TASK");
                    SpreadMessage sm = new SpreadMessage();
                    sm.addGroup(s.getSender());
                    sm.setFifo();
                    sm.setReliable();
                    sp.multicast(sm, new Ack(m.id));
                }
            });

            //Colocar task no ongoing
            sp.handler(GetTaskSpreadReq.class, (s, m) -> {
                System.out.println("PREMUTAR PARA EM TRATAMENTO");
            });

            //Completar a task
            sp.handler(CompleteTaskSpreadReq.class, (s, m) -> {
                System.out.println("TASK COMPLETA");
            });

            sp.handler(UncompleteTaskSpreadReq.class, (s, m) -> {
                System.out.println("TASK COMPLETA");
            });

            sp.handler(MembershipInfo.class, (s, m) -> {
                spreadGroups.clear();
                for (SpreadGroup sg : m.getMembers())
                    spreadGroups.add(sg.toString());

                //Se o set está vazio é porque estou a iniciar
                if(membersBeforeMe.size() == 0)
                    for (SpreadGroup sg : m.getMembers())
                        membersBeforeMe.add(sg.toString());

                //Portanto, alguém saiu, por isso vamos retira-lo do set se
                //está lá.
                if (m.isCausedByLeave() || m.isCausedByDisconnect()) {
                    membersBeforeMe.remove(m.getDisconnected().toString());
                    for (Map.Entry<String, Set<String>> entry : acks.entrySet()) {
                        entry.getValue().remove(m.getDisconnected().toString());
                    //Quem morreu era o ultimo ACK que eu precisava;
                        if (entry.getValue().size() == 1) {
                            responses.get(entry.getKey()).getValue().complete(responses.get(entry.getKey()).getKey());
                            acks.remove(entry.getKey());
                        }
                    }
                }

                //Só existo eu no set e não sou primário
                if(membersBeforeMe.size() == 1 && !isPrimary)
                    startPrimary(t, 5000, tasks, userHandling, responses, acks, sp, spreadGroups);
            });
        });
    }

    private static void startPrimary(Transport t, int port, Task tasks, HashMap<String, String> userHandling,
                                     HashMap<String, Map.Entry<Object, CompletableFuture>> responses,
                                     HashMap<String, Set<String>> acks, Spread sp,
                                     Set<String> spreadGroups){
        //Client server;
        System.out.println("\u001B[34mINITIALIZING PRIMARY SERVER\u001B[0m");
        t.server().listen(new Address("127.0.0.1", port), conn -> {

            conn.handler(AddTasksReq.class, (m) -> {
                System.out.println(conn.toString());
                if(responses.containsKey(m.id))
                    return responses.get(m.id).getValue();
                //Processa o pedido
                boolean result = tasks.addTask(m.uri);
                //Pedido não foi bem sucedido portanto não enviamos para os Backup
                if(!result)
                    return Futures.completedFuture(new AddTasksRep(m.id, result));
                //Aqui o pedido foi bem sucedido
                CompletableFuture<AddTasksRep> response = new CompletableFuture<>();
                //Entry com a resposta para completar e o futuro a ser completado
                Map.Entry me = new AbstractMap.SimpleEntry(new AddTasksRep(m.id, result), response);
                responses.put(m.id, me);

                //Backups neste momento ativos;
                Set s = new HashSet();
                for (String sg : spreadGroups)
                    s.add(sg);
                //Precisamos dos ACK's deles porque blocking;
                acks.put(m.id, s);

                //Multicast da mensagem para os Backup;
                SpreadMessage sm = new SpreadMessage();
                sm.addGroup("CRAWLERS");
                sm.setFifo();
                sm.setReliable();
                sp.multicast(sm, m);

                return response;
            });

            conn.handler(GetTaskReq.class, (m) -> {
                if(responses.containsKey(m.id))
                    return responses.get(m.id).getValue();

                System.out.println("Get task");
                String uri = tasks.getTask();
                System.out.println(uri);
                userHandling.put(conn.toString(), uri);
                return Futures.completedFuture(new GetTaskRep(m.id, uri));
            });

            conn.handler(CompleteTaskReq.class, (m) -> {
                if(responses.containsKey(m.id))
                    return responses.get(m.id).getValue();

                System.out.println("Complete task");
                String taskEnded = m.uri;
                ArrayList<String> newTasks = m.tasks;
                System.out.println(taskEnded);
                boolean result = tasks.completeTask(taskEnded, newTasks);
                return Futures.completedFuture(new CompleteTaskRep(m.id, result));
            });

            //Um cliente vai abaixo vamos colocar a task dele a não completa
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
