package Network.Spread;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class GetTaskSpreadReq implements CatalystSerializable{

    public String id;
    public String clientuid;
    public String uri;


    public GetTaskSpreadReq(){}

    public GetTaskSpreadReq(String idParam, String clientuidParam, String uriParam){
        id = idParam;
        clientuid = clientuidParam;
        uri = uriParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(id);
        bufferOutput.writeString(clientuid);
        bufferOutput.writeString(uri);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        id = bufferInput.readString();
        clientuid = bufferInput.readString();
        uri = bufferInput.readString();
    }
}
