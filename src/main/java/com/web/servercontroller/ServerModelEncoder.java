package com.web.servercontroller;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.google.gson.GsonBuilder;
import com.web.servermodel.ServerModel;

public class ServerModelEncoder implements Encoder.Text<ServerModel> {

	@Override
	public void init(EndpointConfig config) {}

	@Override
	public void destroy() {}

	@Override
	public String encode(ServerModel object) throws EncodeException {
		return new GsonBuilder().disableHtmlEscaping().create().toJson(object);
	}

}
