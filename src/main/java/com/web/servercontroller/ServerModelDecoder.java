package com.web.servercontroller;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.google.gson.Gson;
import com.web.servermodel.ServerModel;

public class ServerModelDecoder implements Decoder.Text<ServerModel>{

	@Override
	public void init(EndpointConfig config) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ServerModel decode(String s) throws DecodeException {
		return new Gson().fromJson(s,ServerModel.class);
	}

	@Override
	public boolean willDecode(String s) {
		return (s != null);
	}
	

}
