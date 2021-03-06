package backtraceio.library.interfaces;

import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;

/**
 * API sender interface
 */
public interface Api {

    /**
     * Send a Backtrace report to Backtrace API
     *
     * @param data diagnostic data
     */
    void send(BacktraceData data, OnServerResponseEventListener callback);


    /**
     * Set an event executed when received bad request, unauthorized request or other information
     * from server
     */
    void setOnServerError(OnServerErrorEventListener onServerError);

    /**
     * Set custom request method to prepare HTTP request to Backtrace API
     *
     * @param requestHandler event which will be executed instead of default request to Backtrace API
     */
    void setRequestHandler(RequestHandler requestHandler);
}