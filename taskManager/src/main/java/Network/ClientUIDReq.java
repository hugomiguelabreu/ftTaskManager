package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class ClientUIDReq implements CatalystSerializable{

    public String id;
    public String clientuid;

    public ClientUIDReq(){}

    public ClientUIDReq(String idParam, String clientuidParam) {
        clientuid = clientuidParam;
        id = idParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(id);
        bufferOutput.writeString(clientuid);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        id = bufferInput.readString();
        clientuid = bufferInput.readString();
    }
}
