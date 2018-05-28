import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import pt.haslab.ekit.Spread;
import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadMessage;

import java.util.UUID;

public class Main {

    public static void main(String[] args) throws SpreadException {
        ThreadContext tc = new SingleThreadContext("srv-%d", new Serializer());
        Spread sp = new Spread("server-" + UUID.randomUUID().toString().split("-")[4], true);

        tc.execute(() -> {
            sp.open().thenRun(() -> {
                sp.join("CRAWLERS");
                System.out.println("CONNECTED TO GROUP");
            });

            sp.handler(String.class, (s, m) -> {
                System.out.println(m);
                SpreadMessage sm = new SpreadMessage();
                sm.addGroup(s.getSender());
                sp.multicast(sm, "kek");
            });

            sp.handler(MembershipInfo.class, (s, m) -> {
                System.out.println(m.getGroup());
                System.out.println(m.isCausedByJoin());
                System.out.println(m.isCausedByDisconnect());
            });

        });
    }

}
