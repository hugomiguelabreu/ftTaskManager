package Network.Spread;

import Network.AddTasksRep;
import Network.ClientUIDRep;
import Network.CompleteTaskRep;
import Network.GetTaskRep;
import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RecoverRep implements CatalystSerializable {

    public ArrayList<String> tasks;
    public ArrayList<String> onGoing;
    public HashMap<String, Map.Entry<Object, CompletableFuture>> responses;

    public RecoverRep() {
    }

    public RecoverRep(ArrayList<String> tasks, ArrayList<String> onGoing, HashMap<String, Map.Entry<Object, CompletableFuture>> responses) {
        this.tasks = tasks;
        this.onGoing = onGoing;
        this.responses = responses;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeInt(tasks.size());
        for (String t : tasks)
            bufferOutput.writeString(t);

        bufferOutput.writeInt(onGoing.size());
        for (String t : onGoing)
            bufferOutput.writeString(t);

        bufferOutput.writeInt(responses.size());
        for(String r: responses.keySet()){
            bufferOutput.writeString(r);
            bufferOutput.writeString(responses.get(r).getKey().getClass().getName());
            serializer.writeObject(responses.get(r).getKey());
            bufferOutput.writeBoolean(responses.get(r).getValue().isDone());
            //serializer.writeObject(responses.get(r).getValue());
        }

    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        tasks = new ArrayList<>();
        onGoing = new ArrayList<>();
        responses = new HashMap<>();
        int size = bufferInput.readInt();

        for(int i = 0; i < size; i++)
            tasks.add(bufferInput.readString());

        size = bufferInput.readInt();
        for(int i = 0; i < size; i++)
            onGoing.add(bufferInput.readString());

        size = bufferInput.readInt();
        for(int i = 0; i < size; i++) {
            String uid = bufferInput.readString();
            String objectType = bufferInput.readString();
            Object ob = null;
            serializer.register(GetTaskRep.class);
            serializer.register(ClientUIDRep.class);
            serializer.register(CompleteTaskRep.class);
            serializer.register(AddTasksRep.class);
            switch (objectType) {
                case "Network.AddTaskRep":
                    AddTasksRep atr = serializer.readObject(bufferInput);
                    ob = atr;
                    break;
                case "Network.CompleteTaskRep":
                    CompleteTaskRep ctr = serializer.readObject(bufferInput);
                    ob = ctr;
                    break;
                case "Network.ClientUIDRep":
                    ClientUIDRep cuid = serializer.readObject(bufferInput);
                    ob = cuid;
                    break;
                case "Network.GetTaskRep":
                    GetTaskRep gt = serializer.readObject(bufferInput);
                    ob = gt;
                    break;
            }

            boolean completedFuture = bufferInput.readBoolean();
            CompletableFuture cf = new CompletableFuture();
            //O futuro é suposto estar completo
            if(completedFuture)
                cf.complete(ob);
            Map.Entry me = new AbstractMap.SimpleEntry(ob, cf);
            responses.put(uid, me);
        }

    }
}
