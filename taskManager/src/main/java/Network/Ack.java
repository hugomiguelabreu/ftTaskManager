package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class Ack implements CatalystSerializable {

    public String id;
    public int op;

    public Ack(){}

    public Ack(String idParam, int opParam){
        id = idParam;
        op = opParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(id);
        bufferOutput.writeInt(op);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        id = bufferInput.readString();
        op = bufferInput.readInt();
    }
}
