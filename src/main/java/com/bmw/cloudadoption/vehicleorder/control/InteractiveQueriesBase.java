package com.bmw.cloudadoption.vehicleorder.control;

import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecordMetadata;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

/**
 * Abstract, generic base class for kafka-streams interactive queries against a
 * state-store.
 * <p>
 * There are two public methods: - getEntry - get one entry inside the
 * state-store by key - getAll - get all values inside the state-store (whether
 * distributed or not)
 * <p>
 * If the kafka-streams application scales to multiple instances these instances
 * communicate via their REST-API using MicroProfile REST Client. If deployed on
 * OpenShift the instances communicate with their Pod IP-addresses.
 *
 * @param <K> key type of the concrete entity
 * @param <V> value type of the concrete entity
 * @param <R> Rest-Client interface describing the API to collect data from
 * other instances that cover other partitions
 */
public class InteractiveQueriesBase<K, V, R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveQueriesBase.class);
    private static final String INTERRUPTED_WHILE_WAITING_FOR_STREAMS_STORE_TO_BE_READY = "interrupted while waiting for streams store to be ready";

    @Inject
    KafkaStreams streams;

    @ConfigProperty(name = "pod.ip")
    String host;

    @ConfigProperty(name = "quarkus.http.port")
    int port;

    /**
     * This channel uses {@link AllToNullSerializer} and therefore only produces
     * tombstone record
     */
    @Inject
    @Channel("kafka-empty")
    Emitter<String> allToNullChannel;

    // host-info of the current application instance
    private HostInfo hostInfo;

    // name of the kafka-streams state-store
    private final String stateStoreName;

    // cache for all microprofile rest-clients on different ip-adresses (OpenShift pods)
    private final Map<String, R> restClientCache = new HashMap<>();

    // function for getting one value by key (in the local state-store)
    private final BiFunction<R, K, V> getEntry;

    // function for getting all values (in the local state-store)
    private final Function<R, CompletionStage<List<V>>> getAll;

    // function for getting the rest-client for a URL
    private final Function<URL, R> getRestClient;

    // type of the topic-key
    private final Class<K> keyType;

    /**
     * Creates an instance that overs all required functions to interact with
     * the state store containing our master data as well as the data collection
     * from other running instances.
     *
     * @param stateStoreName name of the state-store that materializes the
     * entities data
     * @param keyType of the entity
     * @param getRestClient rest-client to fetch data from other instances
     * @param getEntry returns a single entry from the state-store
     * @param getAll returns a list of all entries in the state-store
     */
    protected InteractiveQueriesBase(
            String stateStoreName,
            Class<K> keyType,
            Function<URL, R> getRestClient,
            BiFunction<R, K, V> getEntry,
            Function<R, CompletionStage<List<V>>> getAll) {

        this.stateStoreName = stateStoreName;
        this.getRestClient = getRestClient;
        this.getEntry = getEntry;
        this.getAll = getAll;
        this.keyType = keyType;
    }

    @PostConstruct
    void init() {
        this.hostInfo = new HostInfo(this.host, this.port);
    }

    /**
     * Get one value out of the state-store by key
     *
     * @param key The key of the message
     * @return The value for the given key
     */
    public V getEntry(K key) {
        LOGGER.info("getEntry (key={})", key);
        KeyQueryMetadata metadata = streams.queryMetadataForKey(stateStoreName, key, Serdes.serdeFrom(keyType).serializer());
        
        if (metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            LOGGER.error("Neither this or other instances has access to requested key. Metadata: {}", metadata);
            return null;
        }

        if (isRemoteHost(metadata.getActiveHost())) {
            LOGGER.info("Querying remote store {} for key: {}", stateStoreName, key);
            return getOneFromRemote(metadata.getActiveHost(), key);
        }

        LOGGER.info("Querying local store {} for key: {}", stateStoreName, key);
        return waitUntilStoreIsQueryable().get(key);
    }

    /**
     * Get all values of the state-store (local or distributed)
     *
     * @param localOnly if true, return only values of the local state-store
     * @return All values
     */
    public List<V> getAll(boolean localOnly) {
        LOGGER.info("getAll (localOnly={})", localOnly);
        List<V> values = new ArrayList<>();

        // get data from local state-store
        LOGGER.info("get all from local state-store");
        KeyValueIterator<K, V> iterator = waitUntilStoreIsQueryable().all();
        while (iterator.hasNext()) {
            values.add(iterator.next().value);
        }

        if (localOnly) {
            // stop after fetching local data
            return values;
        }

        // get metadata for state-store which can be distributed across different servers
        Collection<StreamsMetadata> allStreamsMetadata = streams.allMetadataForStore(this.stateStoreName);

        // if state-store is distributed across more than one servers
        if (allStreamsMetadata.size() > 1) {
            LOGGER.info("request all remote servers");

            // trigger all async rest calls
            List<CompletableFuture<List<V>>> completionStages = allStreamsMetadata.stream()
                    .filter(streamsMetadata -> isRemoteHost(streamsMetadata.hostInfo()))
                    .map(streamsMetadata -> getAllFromRemote(streamsMetadata.hostInfo()).toCompletableFuture())
                    .collect(Collectors.toList());

            // wait for all async calls to finish
            List<V> remoteValues = completionStages.stream().map(CompletableFuture::join).flatMap(Collection::stream)
                    .collect(Collectors.toList());

            LOGGER.info("add remote data to result");
            values.addAll(remoteValues);
        }

        return values;
    }

    private V getOneFromRemote(HostInfo hostInfo, K key) {
        R restClient = getRestClientForUrl(hostInfo);
        return this.getEntry.apply(restClient, key);
    }

    private CompletionStage<List<V>> getAllFromRemote(HostInfo hostInfo) {
        R restClient = getRestClientForUrl(hostInfo);
        return this.getAll.apply(restClient);
    }

    private R getRestClientForUrl(HostInfo hostInfo) {
        String url = String.format("http://%s:%d", hostInfo.host(), hostInfo.port());
        LOGGER.info("REST client for URL: {}", url);
        try {
            if (!restClientCache.containsKey(url)) {
                R restClient = this.getRestClient.apply(new URL(url));
                restClientCache.put(url, restClient);
            }
            return restClientCache.get(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean isRemoteHost(HostInfo hostInfo) {
        return !this.hostInfo.equals(hostInfo);
    }

    private ReadOnlyKeyValueStore<K, V> waitUntilStoreIsQueryable() {
        for (int i = 0; i < 100; i++) {
            try {
                return streams.store(StoreQueryParameters.fromNameAndType(stateStoreName, QueryableStoreTypes.keyValueStore()));
            } catch (InvalidStateStoreException | IllegalStateException e2) {
                LOGGER.warn(
                        "Invalid State Store Error while fetching the kafkaStream store: {}. A retry will happen after 300 ms",
                        stateStoreName, e2);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    // If we get interrupted, we simply skip the sleep time
                    LOGGER.error(INTERRUPTED_WHILE_WAITING_FOR_STREAMS_STORE_TO_BE_READY);
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(INTERRUPTED_WHILE_WAITING_FOR_STREAMS_STORE_TO_BE_READY);
                }
            }
        }
        throw new IllegalStateException(
                "Invalid State Store Error while fetching the kafkaStream store. The service is not available at the moment");
    }

    /**
     * Checks the state store if the given entry is already present in our state
     * store. If data has been modified, the Streams API must poll the new
     * messages in order for us to see the latest change that has been done via
     * publishing a new message to the backing topic. This can take a short
     * amount of time since publishing the message and receiving it are two
     * decoupled operations.
     *
     * @param key to check for
     * @param entry that should be checked for existence
     * @return {@code true} if the entry is present as expected, otherwise
     * {@code false}.
     */
    public boolean verifyEntryInStateStore(K key, V entry) {
        int remainderRetries = 10;
        for (int i = 0; i < 10; i++) {
            --remainderRetries;
            try {
                V entryFromStateStore = waitUntilStoreIsQueryable().get(key);
                if (!entry.equals(entryFromStateStore)) {
                    throw new IllegalStateException();
                }
                LOGGER.warn("Found the new entry in our materialized view.");
                return true;
            } catch (InvalidStateStoreException | IllegalStateException e2) {
                LOGGER.warn(buildMessage(remainderRetries), e2.getMessage());
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    // If we get interrupted, we simply skip the sleep time
                    LOGGER.error(INTERRUPTED_WHILE_WAITING_FOR_STREAMS_STORE_TO_BE_READY);
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(INTERRUPTED_WHILE_WAITING_FOR_STREAMS_STORE_TO_BE_READY);
                }
            }
        }
        LOGGER.warn("Could not find the new entry in our materialized view, even after retries.");
        return false;
    }

    /**
     * Creates the warning message if the events has not yet been added to the
     * state-store
     *
     * @param remainderRetries The number of retries left check is an entry has
     * been added to the state-store
     * @return The message for the log output.
     */
    private String buildMessage(int remainderRetries) {
        StringBuilder messageBuilder = new StringBuilder("Could not yet find the new entry in our materialized view.");
        if (remainderRetries > 0) {
            messageBuilder.append(" Will try again ");
            messageBuilder.append(remainderRetries);
            messageBuilder.append(remainderRetries == 1 ? " more time." : " more times.");
        }
        return messageBuilder.toString();
    }

    /**
     * Checks the state store if the given entry is no longer present in our
     * state store. If data has been modified, the Streams API must poll the new
     * messages in order for us to see the latest change that has been done via
     * publishing a new tombstone-message to the backing topic with the
     * corresponding key. This can take a short amount of time since publishing
     * the message and receiving it are two decoupled operations.
     *
     * @param key to check for
     * @return {@code true} if the entry is no longer present as expected,
     * otherwise {@code false}.
     */
    public boolean verifyEntryIsGoneFromStateStore(K key) {
        for (int i = 0; i < 10; i++) {
            try {
                V entryFromStateStore = waitUntilStoreIsQueryable().get(key);
                if (entryFromStateStore != null) {
                    throw new IllegalStateException();
                }
                LOGGER.warn("Entry no longer present in state store...");
                return true;
            } catch (InvalidStateStoreException | IllegalStateException e2) {
                LOGGER.warn("State store not queryable or Entry could still be found.", e2);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    // If we get interrupted, we simply skip the sleep time
                    LOGGER.error(INTERRUPTED_WHILE_WAITING_FOR_STREAMS_STORE_TO_BE_READY);
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(INTERRUPTED_WHILE_WAITING_FOR_STREAMS_STORE_TO_BE_READY);
                }
            }
        }
        LOGGER.warn("Entry still present in our materialized view, even after retries.");
        return false;
    }

    /**
     * Deletes an entry by its key.
     *
     * @param key of the entry
     * @param topic the entity type is backed by
     * @param iq {@link InteractiveQueriesBase} instance handling this entity
     * type
     * @return {@link Response} whether the entity was deleted or not
     */
    public Response deleteEntry(K key, String topic, InteractiveQueriesBase<K, V, R> iq) {
        OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                .withKey((String) key).withTopic(topic).build();
        allToNullChannel.send(Message.of("tombstone", Metadata.of(metadata)));
        // Verify that our deleted entity already caused the deletion in our state store
        Response.ResponseBuilder response = iq.verifyEntryIsGoneFromStateStore(key)
                ? Response.noContent() : Response.status(Response.Status.NOT_ACCEPTABLE);
        return response.build();
    }

}
