package com.web.logincontroller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.GsonBuilder;
import com.web.logindao.LoginDao;
import com.web.loginmodel.LoginModel;

@RestController
public class LoginController {
	
	@Autowired
	private LoginDao logindata;
	
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@RequestMapping(value="/login",method=RequestMethod.POST)
	public @ResponseBody ResponseEntity<String> logindata(@RequestBody LoginModel data) {
		try {
			String status = logindata.checkData(data);
			System.out.println(status);
			if(status.equals("SUCCESS")) {
				String res = new GsonBuilder().disableHtmlEscaping().create().toJson(status);
				return ResponseEntity.ok(res);
			} else {
				String res = new GsonBuilder().disableHtmlEscaping().create().toJson(status);
				return ResponseEntity.ok(res);
			}
		} catch(DataAccessException e)
		{
			String res = new GsonBuilder().disableHtmlEscaping().create().toJson("DATA ACCESS EXCEPTION OCCURED");
			return ResponseEntity.ok(res);
		}
	}
}
