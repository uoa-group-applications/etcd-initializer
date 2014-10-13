package nz.ac.auckland.bathe.initializers;

import bathe.BatheInitializer;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class EtcdInitializer implements BatheInitializer {

    private static final String MINUS_E = "-E";

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public String getName() {
        return "bathe-etcd-initializer";
    }

    @Override
    public String[] initialize(String[] args, String jumpClass) {
        List<URI> uris = new ArrayList<>();
        for (String uri : System.getProperty("etcd.Uris", "http://localhost:4001").split(";")) {
            uris.add(URI.create(uri));
        }

        List<String> appArguments = new ArrayList<>();

        for (String arg : args) {
            if (arg.startsWith(MINUS_E)) {
                String etcdDirRef = arg.substring(MINUS_E.length());
                System.getProperties().putAll(fetchEtcdDir(etcdDirRef, uris.toArray(new URI[uris.size()])));
            } else appArguments.add(arg);
        }

        return appArguments.toArray(new String[appArguments.size()]);
    }

    private Properties fetchEtcdDir(String dirRef, URI... etcdHosts) {
        Properties properties = new Properties();

        try (EtcdClient etcd = new EtcdClient(etcdHosts)) {
            EtcdKeysResponse response = etcd.getDir(dirRef).recursive().send().get();
            for (EtcdKeysResponse.EtcdNode node : response.node.nodes) {
                String nodeKey = node.key;
                //the +2 accounts for first and last /
                properties.put(nodeKey.substring(dirRef.length() + 2), node.value);
            }
            return properties;
        } catch (IOException | EtcdException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}