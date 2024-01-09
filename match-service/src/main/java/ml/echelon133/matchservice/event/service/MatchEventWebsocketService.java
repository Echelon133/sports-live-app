package ml.echelon133.matchservice.event.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import ml.echelon133.common.event.dto.MatchEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MatchEventWebsocketService {

    private final static String MATCH_EVENT_TYPE = "match-event";
    private final static String MATCH_EVENT_NAMESPACE = "/api/ws/match-events";

    private final SocketIOServer server;
    private final static Logger logger = LoggerFactory.getLogger(MatchEventWebsocketService.class);

    @Autowired
    public MatchEventWebsocketService(SocketIOServer server) {
        this.server = server;

        // create a namespace and bind both connection and disconnection handlers to that namespace
        var namespace = server.addNamespace(MATCH_EVENT_NAMESPACE);
        namespace.addConnectListener(onConnected());
        namespace.addDisconnectListener(onDisconnected());

        server.start();
    }

    private ConnectListener onConnected() {
        return (client) -> {
            var sessionId = client.getSessionId();
            logger.debug("Connection with session-id {}", sessionId);

            // to listen to events happening in a particular match, the client needs to connect to a
            // websocket using a URL which contains the id of the match, e.g.
            // ws://host:port/api/ws/match-events?match_id=6c8cfd96-3d88-46a3-919b-eabc2aa6ebd5
            //
            // id of the match is extracted from the match_id url param, after that the client
            // is placed in a room represented by that id
            String matchId = client.getHandshakeData().getSingleUrlParam("match_id");

            UUID roomId;
            try {
                // make sure that the match_id is a valid UUID, otherwise disconnect the client
                roomId = UUID.fromString(matchId);
            } catch (IllegalArgumentException ignore) {
                client.disconnect();
                logger.debug("Session-id {} disconnected because match_id {} is not a valid uuid",
                        sessionId, matchId);
                return;
            }
            client.joinRoom(roomId.toString());
            logger.debug("Session-id {} joins room {} with namespace {}",
                    sessionId, roomId, client.getNamespace().getName());
        };
    }

    private DisconnectListener onDisconnected() {
        return (client) -> {
            logger.debug("Disconnection of session-id {}", client.getSessionId());
        };
    }

    /**
     * Sends a match event to all clients who listen to events of a particular match.
     *
     * @param matchId id of the match during which the event took place
     * @param matchEventDto dto representing the event
     */
    public void sendMatchEvent(UUID matchId, MatchEventDto matchEventDto) {
        var namespace = server.getNamespace(MATCH_EVENT_NAMESPACE);
        namespace.getRoomOperations(matchId.toString()).sendEvent(MATCH_EVENT_TYPE, matchEventDto);
        logger.debug("Sent match event {} to all clients in room {}, namespace {}",
                matchEventDto.getId(), matchId, namespace.getName());
    }
}
