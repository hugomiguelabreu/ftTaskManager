package Network.Spread;

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
            serializer.writeObject(responses.get(r).getKey());
            serializer.writeObject(responses.get(r).getValue());
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
        for(int i = 0; i < size; i++)
            responses.put(bufferInput.readString(),
                          new AbstractMap.SimpleEntry(serializer.readObject(bufferInput), serializer.readObject(bufferInput)))  ;

    }
}
