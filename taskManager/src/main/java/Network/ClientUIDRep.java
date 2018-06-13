package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class ClientUIDRep implements CatalystSerializable{

    public String id;
    public boolean ack;

    public ClientUIDRep(){}

    public ClientUIDRep(String idParam, boolean ackParam){
        ack = ackParam;
        id = idParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(id);
        bufferOutput.writeBoolean(ack);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        id = bufferInput.readString();
        ack = bufferInput.readBoolean();
    }
}
