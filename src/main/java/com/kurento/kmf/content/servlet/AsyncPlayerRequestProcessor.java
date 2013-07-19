package com.kurento.kmf.content.servlet;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;

public class AsyncPlayerRequestProcessor implements RejectableRunnable {

	private static final Logger log = LoggerFactory
			.getLogger(AsyncPlayerRequestProcessor.class);

	private PlayerHandler playerHandler;
	private PlayRequest playRequest;

	public AsyncPlayerRequestProcessor(PlayerHandler playerHandler,
			PlayRequest playRequest) {
		this.playerHandler = playerHandler;
		this.playRequest = playRequest;
	}

	@Override
	public void run() {
		try {
			// In case this call ends without error, we do not complete the
			// AsyncContext. This makes possible PlayRequest.play to be
			// asynchronous
			playerHandler.onPlayRequest(playRequest);
		} catch (Throwable t) {
			AsyncContext asyncCtx = playRequest.getHttpServletRequest()
					.getAsyncContext();
			log.error(
					"Error processing onPlayRequest to "
							+ ((HttpServletRequest) asyncCtx.getRequest())
									.getRequestURI(), t);

			// In case of error we do complete the AsyncContext to make the
			// specific error visible for developers and final users.
			if (playRequest.getHttpServletRequest().isAsyncStarted()) {
				try {
					((HttpServletResponse) asyncCtx.getResponse()).sendError(
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"AsyncContext was not completed by application. Reason: "
									+ t.getMessage());
				} catch (IOException e) {
				}
				asyncCtx.complete();
			}
		}

	}

	@Override
	public void reject(int statusCode, String message) {
		playRequest.reject(statusCode, message);
	}
}
