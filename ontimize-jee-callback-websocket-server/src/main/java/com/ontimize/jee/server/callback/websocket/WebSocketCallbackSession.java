package com.ontimize.jee.server.callback.websocket;

import org.springframework.web.socket.WebSocketSession;

import com.ontimize.jee.server.callback.CallbackSession;

/**
 * The Class WebSocketCallbackSession.
 */
public class WebSocketCallbackSession implements CallbackSession {

    /** The native session. */
    private final Object nativeSession;

    /**
     * Instantiates a new web socket callback session.
     * @param session the session
     */
    public WebSocketCallbackSession(WebSocketSession session) {
        this.nativeSession = session;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ontimize.jee.server.callback.CallbackSession#getNativeSession()
     */
    @Override
    public Object getNativeSession() {
        return this.nativeSession;
    }

}
