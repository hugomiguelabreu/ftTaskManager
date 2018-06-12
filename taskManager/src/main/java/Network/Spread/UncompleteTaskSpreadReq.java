package Network.Spread;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

import java.util.ArrayList;

public class UncompleteTaskSpreadReq implements CatalystSerializable{

    public String id;
    public String uri;
    public int index;

    public UncompleteTaskSpreadReq(){}

    public UncompleteTaskSpreadReq(String idParam, String uriParam, int indexParam){
        uri = uriParam;
        id = idParam;
        index = indexParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(id);
        bufferOutput.writeString(uri);
        bufferOutput.writeInt(index);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        id = bufferInput.readString();
        uri = bufferInput.readString();
        index = bufferInput.readInt();
    }
}
