package Network.Spread;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class GetTaskSpreadReq implements CatalystSerializable{

    public String id;
    public String uri;
    public String user;//COMO VOU DISTINGUIR NAS REPLICAS???

    public GetTaskSpreadReq(){}

    public GetTaskSpreadReq(String idParam){
        id = idParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(id);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        id = bufferInput.readString();
    }
}
