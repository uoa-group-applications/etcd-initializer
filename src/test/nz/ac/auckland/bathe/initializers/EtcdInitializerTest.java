package nz.ac.auckland.bathe.initializers;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdException;
import org.junit.*;

import java.net.URI;

/**
 * Configuring etcd for testing:
 * ./etcdctl mkdir a
 * ./etcdctl set "a/foo" "asdf"
 * ./etcdctl set "a/baz" "1234"
 * ./etcdctl mkdir c
 * ./etcdctl set "c/1" "1234"
 * ./etcdctl set "c/baz" "4321"
 * <p/>
 * At the end:
 * ./etcdctl rm "a" --recursive
 * ./etcdctl rm "c" --recursive
 */

@Ignore
public class EtcdInitializerTest extends Assert {

    private static final EtcdClient ETCD = new EtcdClient(URI.create("http://localhost:4001"));

    @Before
    public void create() throws Exception {
        //delete just in case
        delete();
        ETCD.putDir("a").send();
        ETCD.putDir("c").send();

        ETCD.put("a/foo", "asdf").send();
        ETCD.put("a/baz", "1234").send();
        ETCD.put("c/1", "1234").send();
        ETCD.put("c/baz", "4321").send();
    }

    @After
    public void delete() throws Exception {
        rmdir("a");
        rmdir("c");
    }

    private void rmdir(String dir) throws Exception {
        try {
            ETCD.deleteDir(dir).recursive().send().get();
        } catch (EtcdException e) {
            if (!e.etcdMessage.equals("Key not found")) throw e;
        }
    }

    @Test
    public void testGettingDir() throws Exception {
        EtcdInitializer init = new EtcdInitializer();
        String[] returned = init.initialize(new String[]{"foo", "-Ea", "baz", "-Ec"}, "some.class");
        assertEquals(2, returned.length);
        assertEquals("foo", returned[0]);
        assertEquals("baz", returned[1]);

        assertEquals("asdf", System.getProperty("foo"));
        assertEquals("4321", System.getProperty("baz"));
        assertEquals("1234", System.getProperty("1"));
    }
}
