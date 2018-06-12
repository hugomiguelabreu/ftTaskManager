package Network.Spread;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

import java.util.ArrayList;

public class IncompleteTaskSpreadReq implements CatalystSerializable{

    public String id;
    public String uri;

    public IncompleteTaskSpreadReq(){}

    public IncompleteTaskSpreadReq(String idParam, String uriParam){
        uri = uriParam;
        id = idParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(id);
        bufferOutput.writeString(uri);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        id = bufferInput.readString();
        uri = bufferInput.readString();
    }
}
