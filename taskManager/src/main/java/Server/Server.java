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

    static boolean available = true;
    static Queue<Map.Entry<SpreadMessage,Object>> queue = new LinkedList<>();

    //Mapa de conexões ID -> uri para saber que uri estava a tratar um client
    static HashMap<String, String> userHandling = new HashMap<>();
    //Mapa de conexões conn -> ID para saber que uri estava a tratar um client
    static HashMap<String, String> userRegistrar = new HashMap<>();
    //Mapa para as respostas depois dos acks e pedidos repetidos;
    static HashMap<String, Map.Entry<Object, CompletableFuture>> responses = new HashMap<>();
    //Mapa para os acks dos backups;
    static HashMap<String, Set<String>> acks = new HashMap<>();
    //Set de servidores antes de mim a serem primários;
    static Set<String> membersBeforeMe = new HashSet<>();
    //Servidores da vista
    static Set<String> spreadGroups = new HashSet<>();
    static AtomicReference<String> askedStateTo = new AtomicReference<>("");
    static TaskImpl tasks = new TaskImpl();
    static Transport t = new NettyTransport();;
    static ThreadContext tc = new SingleThreadContext("srv-%d", new Serializer());
    static Spread sp;


    public static void main(String[] args) throws SpreadException {
        sp = new Spread("server-" + UUID.randomUUID().toString().split("-")[4], true);

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
                if (available)
                    ack_handle(s,m);
                else
                    queue.add( new AbstractMap.SimpleEntry(s, m));
            });

            //Nova task
            sp.handler(AddTasksSpreadReq.class, (s, m) -> {
                if (available)
                    addTasksSpreadReq_handle(s,m);
                else
                    queue.add( new AbstractMap.SimpleEntry(s, m));
            });

            //Colocar task no ongoing
            sp.handler(GetTaskSpreadReq.class, (s, m) -> {
                if (available)
                    getTaskSpreadReq_handle(s,m);
                else
                    queue.add( new AbstractMap.SimpleEntry(s, m));
            });

            //Completar a task
            sp.handler(CompleteTaskSpreadReq.class, (s, m) -> {
                if (available)
                    completeTaskSpreadReq_handle(s,m);
                else
                    queue.add( new AbstractMap.SimpleEntry(s, m));
            });

            sp.handler(IncompleteTaskSpreadReq.class, (s, m) -> {
                if (available)
                    incompleteTaskSpreadReq_handle(s,m);
                else
                    queue.add( new AbstractMap.SimpleEntry(s, m));
            });

            sp.handler(RecoverReq.class, (s, m) -> {
                if (available)
                    recoverReq_handle(s,m);
                else
                    queue.add(new AbstractMap.SimpleEntry(s, m));
            });

            sp.handler(RecoverRep.class, (s, m) -> {
                recoverRep_handle(s,m);
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
                            available = false;
                            askedStateTo.set(g);
                            SpreadMessage sm = new SpreadMessage();
                            sm.addGroup(g);
                            sm.setSafe();
                            sp.multicast(sm, (new RecoverReq()));
                            break;
                        }
                    }
                }

                //Portanto, alguém saiu, por isso vamos retira-lo do set se
                //está lá.
                if (m.isCausedByLeave() || m.isCausedByDisconnect()) {
                    membersBeforeMe.remove(m.getDisconnected().toString());
                    for (Iterator<Map.Entry<String, Set<String>>> it = acks.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<String, Set<String>> entry = it.next();
                        entry.getValue().remove(m.getDisconnected().toString());
                    //Quem morreu era o ultimo ACK que eu precisava;
                        if (entry.getValue().size() == 1) {
                            responses.get(entry.getKey()).getValue().complete(responses.get(entry.getKey()).getKey());
                            it.remove();
                        }
                    }
                }

                //Só existo eu no set e não sou primário
                if(membersBeforeMe.size() == 1 && !isPrimary) {
                    startPrimary(5000);
                }
            });
        });
    }

    public static void recoverQueue() {
        while(!queue.isEmpty()){
            Map.Entry msg = queue.poll();
            switch (msg.getValue().getClass().getSimpleName()){
                case "Ack":
                    ack_handle((SpreadMessage) msg.getKey(),(Ack) msg.getValue());
                    break;
                case "AddTasksSpreadReq":
                    addTasksSpreadReq_handle((SpreadMessage) msg.getKey(), (AddTasksSpreadReq) msg.getValue());
                    break;
                case "GetTaskSpreadReq":
                    getTaskSpreadReq_handle((SpreadMessage) msg.getKey(), (GetTaskSpreadReq) msg.getValue());
                    break;
                case "CompleteTaskSpreadReq":
                    completeTaskSpreadReq_handle((SpreadMessage) msg.getKey(), (CompleteTaskSpreadReq) msg.getValue());
                    break;
                case "IncompleteTaskSpreadReq":
                    incompleteTaskSpreadReq_handle((SpreadMessage) msg.getKey(), (IncompleteTaskSpreadReq) msg.getValue());
                    break;
                case "RecoverReq":
                    recoverReq_handle((SpreadMessage) msg.getKey(), (RecoverReq) msg.getValue());
                    break;
            }
        }
        available = true;
    }

    private static void ack_handle(SpreadMessage s, Ack m){
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
    }

    private static void addTasksSpreadReq_handle(SpreadMessage s, AddTasksSpreadReq m){
        if(!s.getSender().toString().equals(sp.getPrivateGroup().toString())){
            System.out.println("NOVA TASK");
            //Adiciona a task de forma deterministica
            tasks.addTaskIndex(m.uri, m.index);
            AddTasksRep reply = new AddTasksRep(m.id, true);
            CompletableFuture<AddTasksRep> cf = new CompletableFuture<>();
            cf.complete(reply);
            Map.Entry e = new AbstractMap.SimpleEntry(reply, cf);
            responses.put(m.id, e);
            //Confirma a atualização de estado
            SpreadMessage sm = new SpreadMessage();
            sm.addGroup(s.getSender());
            sm.setSafe();
            sp.multicast(sm, new Ack(m.id));
        }
    }

    private static void getTaskSpreadReq_handle(SpreadMessage s, GetTaskSpreadReq m){
        if(!s.getSender().toString().equals(sp.getPrivateGroup().toString())){
            System.out.println("PERMUTAR TASK PARA TRATAMENTO");
            //Move a task para ongoing de forma determinista
            tasks.moveTaskToOngoing(m.uri);
            userHandling.put(m.clientuid, m.uri);
            GetTaskRep reply = new GetTaskRep(m.id, m.uri);
            CompletableFuture<GetTaskRep> cf = new CompletableFuture<>();
            cf.complete(reply);
            Map.Entry e = new AbstractMap.SimpleEntry(reply, cf);
            responses.put(m.id, e);
            //Confirma a atualização de estado
            SpreadMessage sm = new SpreadMessage();
            sm.addGroup(s.getSender());
            sm.setSafe();
            sp.multicast(sm, new Ack(m.id));
        }
    }

    private static void completeTaskSpreadReq_handle(SpreadMessage s, CompleteTaskSpreadReq m){
        if(!s.getSender().toString().equals(sp.getPrivateGroup().toString())) {
            System.out.println("TASK COMPLETA");
            //Adiciona as novas tasks nos sitios deterministicos
            for (int i = 0; i < m.size; i++) {
                tasks.addTaskIndex(m.tasks.get(i), m.indexes.get(i));
            }
            //Remove a task do que estava a fazer
            tasks.removeOngoing(m.uri);
            userHandling.remove(m.clientuid);
            CompleteTaskRep reply = new CompleteTaskRep(m.id, true);
            CompletableFuture<CompleteTaskRep> cf = new CompletableFuture<>();
            cf.complete(reply);
            Map.Entry e = new AbstractMap.SimpleEntry(reply, cf);
            responses.put(m.id, e);
            tasks.print();
            //Confirma a atualização de estado
            SpreadMessage sm = new SpreadMessage();
            sm.addGroup(s.getSender());
            sm.setSafe();
            sp.multicast(sm, new Ack(m.id));
        }
    }

    private static void incompleteTaskSpreadReq_handle(SpreadMessage s, IncompleteTaskSpreadReq m){
        if(!s.getSender().toString().equals(sp.getPrivateGroup().toString())) {
            System.out.println("TASK INCOMPLETA");
            tasks.moveOngoingToQueue(m.uri);
            userHandling.remove(m.id);
        }
    }

    private static void recoverReq_handle(SpreadMessage s, RecoverReq m){
        SpreadMessage sm = new SpreadMessage();
        sm.addGroup(s.getSender());
        sm.setSafe();

        RecoverRep recRep = new RecoverRep(((TaskImpl) tasks).getTasks(), ((TaskImpl) tasks).getOnGoing(), responses);
        System.out.println("REPLY FOR STATE");
        sp.multicast(sm, recRep);
    }

    private static void recoverRep_handle(SpreadMessage s, RecoverRep m){
        System.out.println("GOT STATE");
        askedStateTo.set("");
        responses.clear();
        for(String k : m.responses.keySet())
            responses.put(k,m.responses.get(k));
        tasks.setOnGoing(m.onGoing);
        tasks.setTasks(m.tasks);
        recoverQueue();
        tasks.print();
    }

    private static void startPrimary(int port){
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
                sm.setSafe();
                sp.multicast(sm, new AddTasksSpreadReq(m.id, m.uri, resultIndex));

                return response;
            });

            conn.handler(GetTaskReq.class, (m) -> {
                if(responses.containsKey(m.id))
                    return responses.get(m.id).getValue();
                //Processa o pedido
                String uri = tasks.getTask();
                if(uri == null)
                    return Futures.completedFuture(new GetTaskRep(m.id, uri));
                //Aqui a resposta foi efetivamente um URL, registar o client a tratar
                userHandling.put(userRegistrar.get(conn.toString()), uri);
                CompletableFuture<GetTaskRep> response = new CompletableFuture<>();
                //Entry com a resposta para completar e o futuro a ser completado
                Map.Entry me = new AbstractMap.SimpleEntry(new GetTaskRep(m.id, uri), response);
                responses.put(m.id, me);
                //Só sou eu posso retornar a vontade
                if(spreadGroups.size() == 1){
                    responses.get(m.id).getValue().complete(responses.get(m.id).getKey());
                    return response;
                }
                //Backups neste momento ativos;
                Set s = new HashSet();
                for (String sg : spreadGroups)
                    s.add(sg);
                //Precisamos dos ACK's deles porque temos abordagem blocking;
                acks.put(m.id, s);

                //Multicast da mensagem para os Backup;
                SpreadMessage sm = new SpreadMessage();
                sm.addGroup("CRAWLERS");
                sm.setSafe();
                sp.multicast(sm, new GetTaskSpreadReq(m.id, userRegistrar.get(conn.toString()), uri));

                return response;
            });

            conn.handler(CompleteTaskReq.class, (m) -> {
                //Recoloquei a task pois demorou demasiado tempo a anunciar
                if(!userHandling.containsValue(m.uri) && !responses.containsKey(m.id))
                        return Futures.completedFuture(new CompleteTaskRep(m.id, false));

                if(responses.containsKey(m.id))
                    return responses.get(m.id).getValue();

                String taskEnded = m.uri;

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
                sm.setSafe();
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
                    sm.setSafe();
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
                for (Iterator<Map.Entry<String, String>> it = userHandling.entrySet().iterator(); it.hasNext();) {
                    Map.Entry e = it.next();
                    if(!userRegistrar.containsValue(e.getKey())){
                        //O cliente não se registou
                        tasks.setUncompleted((String) e.getValue());
                        it.remove();
                        //Multicast da mensagem para os Backup;
                        SpreadMessage sm = new SpreadMessage();
                        sm.addGroup("CRAWLERS");
                        sm.setSafe();
                        sp.multicast(sm, new IncompleteTaskSpreadReq((String) e.getKey(), (String) e.getValue()));
                        System.out.println("Client DID NOT SAY NOTHING. Task reinserted.");
                    }
                }
            }
        }, 10000);
    }

}
