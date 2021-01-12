package com.ontimize.jee.server.callback.websocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.ontimize.jee.common.exceptions.OntimizeJEERuntimeException;
import com.ontimize.jee.common.jackson.OntimizeMapper;
import com.ontimize.util.Base64Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.ontimize.jee.common.callback.CallbackWrapperMessage;
import com.ontimize.jee.server.callback.CallbackSession;
import com.ontimize.jee.server.callback.ICallbackEventListener;
import com.ontimize.jee.server.callback.ICallbackHandler;

/**
 * The Class WebSocketHandler.
 */
@Component("websocketHandler")
@Lazy(value = true)
public class WebSocketHandler extends TextWebSocketHandler implements ICallbackHandler {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    /** The sessions. */
    private final List<WebSocketSession> sessions;

    /** The message listeners. */
    private final List<ICallbackEventListener> messageListeners;

    /**
     * Instantiates a new web socket handler.
     */
    public WebSocketHandler() {
        super();
        this.sessions = new ArrayList<WebSocketSession>();
        this.messageListeners = new ArrayList<>();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#handleTextMessage(org.
     * springframework.web.socket.WebSocketSession, org.springframework.web.socket.TextMessage)
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        CallbackWrapperMessage wrappedMessage = null;
        try {
            String newMessage = Base64Utils
                .decode(new String(message.getPayload().getBytes(StandardCharsets.ISO_8859_1)));
            wrappedMessage = new OntimizeMapper().readValue(newMessage, CallbackWrapperMessage.class);
        } catch (Exception error) {
            throw new OntimizeJEERuntimeException(error);
        }
        // CallbackWrapperMessage.deserialize(message.getPayload());
        this.fireMessageReceived(new WebSocketCallbackSession(session), wrappedMessage);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.web.socket.handler.AbstractWebSocketHandler#afterConnectionEstablished(org.
     * springframework.web.socket.WebSocketSession)
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        this.sessions.add(session);
        WebSocketHandler.logger.info("new websocket session from {}", session.getPrincipal().getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#afterConnectionClosed(org.
     * springframework.web.socket.WebSocketSession, org.springframework.web.socket.CloseStatus)
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        this.sessions.remove(session);
        WebSocketHandler.logger.info("websocket session closed from {}", session.getPrincipal().getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#handleTransportError(org.
     * springframework.web.socket.WebSocketSession, java.lang.Throwable)
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ontimize.jee.server.callback.ICallbackHandler#addCallbackEventListener(com.ontimize.jee.
     * server.callback.ICallbackEventListener)
     */
    @Override
    public void addCallbackEventListener(ICallbackEventListener listener) {
        this.messageListeners.add(listener);
    }

    /**
     * Fire message received.
     * @param session the session
     * @param message the message
     */
    protected void fireMessageReceived(WebSocketCallbackSession session, CallbackWrapperMessage message) {
        for (ICallbackEventListener listener : this.messageListeners) {
            listener.onCallbackMessageReceived(session, message);
        }
    }

    /**
     * Send message.
     * @param messageType the message type
     * @param messageSubtype the message subtype
     * @param ob the ob
     * @param receivers the receivers
     */
    @Override
    public void sendMessage(Integer messageType, String messageSubtype, Object ob, CallbackSession... receivers) {
        TextMessage textMessage = this.buildTextMessage(messageType, messageSubtype, ob);
        for (CallbackSession session : receivers) {
            try {
                this.getWebSocketSession(session).sendMessage(textMessage);
            } catch (IOException error) {
                // TODO deberia lanzar excepcion si falla con una session?
                WebSocketHandler.logger.error(null, error);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ontimize.jee.server.callback.ICallbackHandler#sendMessage(java.lang.Integer,
     * java.lang.String, java.lang.Object, com.ontimize.jee.server.callback.CallbackSession)
     */
    @Override
    public void sendMessage(Integer messageType, String messageSubtype, Object ob, CallbackSession receiver)
            throws IOException {
        TextMessage textMessage = this.buildTextMessage(messageType, messageSubtype, ob);
        this.getWebSocketSession(receiver).sendMessage(textMessage);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ontimize.jee.server.callback.ICallbackHandler#sendBroadcastMessage(java.lang.Integer,
     * java.lang.String, java.lang.Object)
     */
    @Override
    public void sendBroadcastMessage(Integer messageType, String messageSubtype, Object ob) {
        TextMessage textMessage = this.buildTextMessage(messageType, messageSubtype, ob);
        for (WebSocketSession session : this.sessions) {
            try {
                session.sendMessage(textMessage);
            } catch (IOException error) {
                // TODO deberia lanzar excepcion si falla con una session?
                WebSocketHandler.logger.error(null, error);
            }
        }
    }

    /**
     * Builds the text message.
     * @param messageType the message type
     * @param messageSubtype the message subtype
     * @param ob the ob
     * @return the text message
     */
    protected TextMessage buildTextMessage(Integer messageType, String messageSubtype, Object ob) {
        return new TextMessage(new CallbackWrapperMessage(messageType, messageSubtype, ob).serialize());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ontimize.jee.server.callback.ICallbackHandler#getSessionsForUser(java.lang.String)
     */
    @Override
    public List<CallbackSession> getSessionsForUser(String userLogin) {
        List<CallbackSession> res = new ArrayList<>();
        if (userLogin == null) {
            return res;
        }
        for (WebSocketSession session : this.sessions) {
            if (userLogin.equals(session.getPrincipal().getName())) {
                res.add(new WebSocketCallbackSession(session));
            }
        }
        return res;
    }

    /**
     * Gets the web socket session.
     * @param session the session
     * @return the web socket session
     */
    protected WebSocketSession getWebSocketSession(CallbackSession session) {
        return (WebSocketSession) session.getNativeSession();
    }

}
