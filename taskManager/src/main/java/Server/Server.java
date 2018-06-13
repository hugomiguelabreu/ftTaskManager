package Server;

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
import java.util.concurrent.atomic.AtomicReference;

public class Server {

    static boolean isPrimary = false;

    public static void main(String[] args) throws SpreadException {
        //Mapa de conexões ID -> uri para saber que uri estava a tratar um client
        HashMap<String, String> userHandling = new HashMap<>();
        //Mapa de conexões conn -> ID para saber que uri estava a tratar um client
        HashMap<String, String> userRegistrar = new HashMap<>();
        //Mapa para as respostas depois dos acks e pedidos repetidos;
        HashMap<String, Map.Entry<Object, CompletableFuture>> responses = new HashMap<>();
        //Mapa para os acks dos backups;
        HashMap<String, Set<String>> acks = new HashMap<>();
        //Set de servidores antes de mim a serem primários;
        Set<String> membersBeforeMe = new HashSet<>();
        //Servidores da vista
        Set<String> spreadGroups = new HashSet<>();
        TaskImpl tasks = new TaskImpl();
        Transport t = new NettyTransport();;
        ThreadContext tc = new SingleThreadContext("srv-%d", new Serializer());
        Spread sp = new Spread("server-" + UUID.randomUUID().toString().split("-")[4], true);

        AtomicReference<String> askedStateTo = new AtomicReference<>("");

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
                    //Já respondi posso remover a espera de ACKS
                    acks.remove(m.id);
                }
            });

            //Nova task
            sp.handler(AddTasksSpreadReq.class, (s, m) -> {
                if(!s.getSender().toString().equals(sp.getPrivateGroup().toString())){
                    System.out.println("NOVA TASK");
                    //Adiciona a task de forma deterministica
                    tasks.addTaskIndex(m.uri, m.index);
                    //Confirma a atualização de estado
                    SpreadMessage sm = new SpreadMessage();
                    sm.addGroup(s.getSender());
                    sm.setFifo();
                    sm.setReliable();
                    sp.multicast(sm, new Ack(m.id));
                }
            });

            //Colocar task no ongoing
            sp.handler(GetTaskSpreadReq.class, (s, m) -> {
                if(!s.getSender().toString().equals(sp.getPrivateGroup().toString())){
                    System.out.println("PERMUTAR TASK PARA TRATAMENTO");
                    //Move a task para ongoing de forma determinista
                    tasks.moveTaskToOngoing(m.uri);
                    userHandling.put(m.clientuid, m.uri);
                    tasks.print();
                    //Confirma a atualização de estado
                    SpreadMessage sm = new SpreadMessage();
                    sm.addGroup(s.getSender());
                    sm.setFifo();
                    sm.setReliable();
                    sp.multicast(sm, new Ack(m.id));
                }
            });

            //Completar a task
            sp.handler(CompleteTaskSpreadReq.class, (s, m) -> {
                if(!s.getSender().toString().equals(sp.getPrivateGroup().toString())) {
                    System.out.println("TASK COMPLETA");
                    //Adiciona as novas tasks nos sitios deterministicos
                    for (int i = 0; i < m.size; i++) {
                        tasks.addTaskIndex(m.tasks.get(i), m.indexes.get(i));
                    }
                    //Remove a task do que estava a fazer
                    tasks.removeOngoing(m.uri);
                    userHandling.remove(m.clientuid);
                    tasks.print();
                    //Confirma a atualização de estado
                    SpreadMessage sm = new SpreadMessage();
                    sm.addGroup(s.getSender());
                    sm.setFifo();
                    sm.setReliable();
                    sp.multicast(sm, new Ack(m.id));
                }
            });

            sp.handler(IncompleteTaskSpreadReq.class, (s, m) -> {
                if(!s.getSender().toString().equals(sp.getPrivateGroup().toString())) {
                    System.out.println("TASK INCOMPLETA");
                    tasks.moveOngoingToQueue(m.uri);
                }
            });

            sp.handler(RecoverReq.class, (s, m) -> {

                SpreadMessage sm = new SpreadMessage();
                sm.addGroup(s.getSender());
                sm.setFifo();
                sm.setReliable();

                RecoverRep recRep = new RecoverRep(((TaskImpl) tasks).getTasks(), ((TaskImpl) tasks).getOnGoing(), responses);
                System.out.println("REPLY FOR STATE");
                sp.multicast(sm, recRep);
            });

            sp.handler(RecoverRep.class, (s, m) -> {
                System.out.println("GOT STATE");
                askedStateTo.set("");
                responses.clear();
                for(String k : m.responses.keySet())
                    responses.put(k,m.responses.get(k));
                tasks.setOnGoing(m.onGoing);
                tasks.setTasks(m.tasks);

                tasks.print();
            });

            sp.handler(MembershipInfo.class, (s, m) -> {
                spreadGroups.clear();
                for (SpreadGroup sg : m.getMembers())
                    spreadGroups.add(sg.toString());

                //Se o set está vazio é porque estou a iniciar
                if(membersBeforeMe.size() == 0) {
                    for (SpreadGroup sg : m.getMembers())
                        membersBeforeMe.add(sg.toString());
                }

                if( (m.isCausedByJoin() && m.getJoined().toString().equals(sp.getPrivateGroup().toString()))
                        || (m.isCausedByDisconnect() && m.getDisconnected().equals(askedStateTo.get()))) {
                    for (String g : spreadGroups) {
                        if(!g.equals(sp.getPrivateGroup().toString())) {
                            System.out.println("ASKING FOR STATE");
                            askedStateTo.set(g);
                            SpreadMessage sm = new SpreadMessage();
                            sm.addGroup(g);
                            sm.setFifo();
                            sm.setReliable();
                            sp.multicast(sm, (new RecoverReq()));
                            break;
                        }
                    }
                }

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
                if(membersBeforeMe.size() == 1 && !isPrimary) {
                    startPrimary(t, 5000, tasks, userHandling, responses, acks, sp, spreadGroups, userRegistrar);
                }
            });
        });
    }

    private static void startPrimary(Transport t, int port, TaskImpl tasks, HashMap<String, String> userHandling,
                                     HashMap<String, Map.Entry<Object, CompletableFuture>> responses,
                                     HashMap<String, Set<String>> acks, Spread sp,
                                     Set<String> spreadGroups, HashMap<String, String> userRegistrar){
        //Client server;
        System.out.println("\u001B[34mINITIALIZING PRIMARY SERVER\u001B[0m");
        t.server().listen(new Address("127.0.0.1", port), conn -> {

            conn.handler(AddTasksReq.class, (m) -> {
                if(responses.containsKey(m.id))
                    return responses.get(m.id).getValue();
                //Processa o pedido
                boolean result = tasks.addTask(m.uri);
                //Pedido não foi bem sucedido ou não há backups portanto não enviamos para os Backup
                if(!result || spreadGroups.size() == 1)
                    return Futures.completedFuture(new AddTasksRep(m.id, result));
                //Aqui o pedido foi bem sucedido
                int resultIndex = tasks.taskIndex(m.uri);
                CompletableFuture<AddTasksRep> response = new CompletableFuture<>();
                //Entry com a resposta para completar e o futuro a ser completado
                Map.Entry me = new AbstractMap.SimpleEntry(new AddTasksRep(m.id, result), response);
                responses.put(m.id, me);

                //Backups neste momento ativos;
                Set s = new HashSet();
                for (String sg : spreadGroups)
                    s.add(sg);
                //Precisamos dos ACK's deles porque temos abordagem blocking;
                acks.put(m.id, s);

                //Multicast da mensagem para os Backup;
                SpreadMessage sm = new SpreadMessage();
                sm.addGroup("CRAWLERS");
                sm.setFifo();
                sm.setReliable();
                sp.multicast(sm, new AddTasksSpreadReq(m.id, m.uri, resultIndex));

                return response;
            });

            conn.handler(GetTaskReq.class, (m) -> {
                if(responses.containsKey(m.id))
                    return responses.get(m.id).getValue();
                //Processa o pedido
                String uri = tasks.getTask();
                if(uri == null || spreadGroups.size() == 1)
                    return Futures.completedFuture(new GetTaskRep(m.id, uri));
                //Aqui a resposta foi efetivamente um URL, registar o client a tratar
                userHandling.put(userRegistrar.get(conn.toString()), uri);
                CompletableFuture<GetTaskRep> response = new CompletableFuture<>();
                //Entry com a resposta para completar e o futuro a ser completado
                Map.Entry me = new AbstractMap.SimpleEntry(new GetTaskRep(m.id, uri), response);
                responses.put(m.id, me);

                //Backups neste momento ativos;
                Set s = new HashSet();
                for (String sg : spreadGroups)
                    s.add(sg);
                //Precisamos dos ACK's deles porque temos abordagem blocking;
                acks.put(m.id, s);

                //Multicast da mensagem para os Backup;
                SpreadMessage sm = new SpreadMessage();
                sm.addGroup("CRAWLERS");
                sm.setFifo();
                sm.setReliable();
                sp.multicast(sm, new GetTaskSpreadReq(m.id, userRegistrar.get(conn.toString()), uri));

                return response;
            });

            conn.handler(CompleteTaskReq.class, (m) -> {
                if(responses.containsKey(m.id))
                    return responses.get(m.id).getValue();
                String taskEnded = m.uri;
                //Recoloquei a task pois demorou demasiado tempo a anunciar
                if(!userHandling.containsValue(m.uri))
                    return Futures.completedFuture(new CompleteTaskRep(m.id, false));
                
                ArrayList<String> newTasks = new ArrayList<>();
                for (String taskNew: m.tasks)
                    if(!tasks.tasksContains(taskNew))
                        newTasks.add(taskNew);

                System.out.println(taskEnded);
                boolean result = tasks.completeTask(taskEnded, newTasks);
                if(!result || spreadGroups.size() == 1)
                    return Futures.completedFuture(new CompleteTaskRep(m.id, result));
                //Aqui o processamento completo foi efetuado
                userHandling.remove(userRegistrar.get(conn.toString()));
                //Obter os indices onde adicionamos cada nova task para processamento deterministico
                ArrayList<Integer> indexes = new ArrayList<>();
                for(String newTask: newTasks)
                    indexes.add(tasks.taskIndex(newTask));

                CompletableFuture<CompleteTaskRep> response = new CompletableFuture<>();
                //Entry com a resposta para completar e o futuro a ser completado
                Map.Entry me = new AbstractMap.SimpleEntry(new CompleteTaskRep(m.id, result), response);
                responses.put(m.id, me);

                //Backups neste momento ativos;
                Set s = new HashSet();
                for (String sg : spreadGroups)
                    s.add(sg);
                //Precisamos dos ACK's deles porque temos abordagem blocking;
                acks.put(m.id, s);

                //Multicast da mensagem para os Backup;
                SpreadMessage sm = new SpreadMessage();
                sm.addGroup("CRAWLERS");
                sm.setFifo();
                sm.setReliable();
                sp.multicast(sm, new CompleteTaskSpreadReq(m.id, userRegistrar.get(conn.toString()),
                        m.uri, newTasks, indexes));

                return response;
            });

            //Cliente quer registar a sua conexão
            conn.handler(ClientUIDReq.class, m ->{
                userRegistrar.put(conn.toString(), m.clientuid);
                return Futures.completedFuture(new ClientUIDRep(m.id, true));
            });

            //Um cliente vai abaixo vamos colocar a task dele a não completa
            conn.onClose(connection -> {
                String user = userRegistrar.get(connection.toString());
                String uri = null;
                if(user!=null)
                    uri = userHandling.get(user);
                if(uri!=null) {
                    tasks.setUncompleted(uri);
                    userHandling.remove(user);
                    //Multicast da mensagem para os Backup;
                    SpreadMessage sm = new SpreadMessage();
                    sm.addGroup("CRAWLERS");
                    sm.setFifo();
                    sm.setReliable();
                    sp.multicast(sm, new IncompleteTaskSpreadReq(user, uri));
                    System.out.println("Client closed. Task reinserted.");
                }
                userRegistrar.remove(connection.toString());
                tasks.print();
            });
        });
        isPrimary = true;
        //Os clientes têm no máximo 10 segundos para voltarem a anunciar-se
        new Timer().schedule(new TimerTask() {
            public void run() {
                for (Map.Entry<String, String> e: userHandling.entrySet()) {
                    if(!userRegistrar.containsValue(e.getKey())){
                        //O cliente não se registou
                        tasks.setUncompleted(e.getValue());
                        userHandling.remove(e.getKey());
                        //Multicast da mensagem para os Backup;
                        SpreadMessage sm = new SpreadMessage();
                        sm.addGroup("CRAWLERS");
                        sm.setFifo();
                        sm.setReliable();
                        sp.multicast(sm, new IncompleteTaskSpreadReq(e.getKey(), e.getValue()));
                        System.out.println("Client DID NOT SAY NOTHING. Task reinserted.");
                    }
                }
            }
        }, 10000);
    }

}
