package com.web.logindao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.web.loginmodel.LoginModel;
@Component
public class LoginDao {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	public String checkData(LoginModel data) {
		List<LoginModel> Fulldetails = new ArrayList<LoginModel>();
		String sql = "select * from details";
		Fulldetails = jdbcTemplate.query(sql,new DetailMapper());
		for(int i = 0; i < Fulldetails.size(); i++) {
			
			if(data.getName().equals(Fulldetails.get(i).getName()) && data.getPassword().equals(Fulldetails.get(i).getPassword()) 
					&& data.getisAgent().equals(Fulldetails.get(i).getisAgent())) {
				return "SUCCESS";
			}
		}
		return "FAILURE";
	}
	//Row Mapper to get each row from DB table and assign to the setter method
	private static final class DetailMapper implements RowMapper<LoginModel> {
		@Override
		public LoginModel mapRow(ResultSet resultSet,int rowNum) throws SQLException {
			LoginModel logindetail = new LoginModel();
			logindetail.setName(resultSet.getString("name"));
			logindetail.setPassword(resultSet.getString("password"));
			logindetail.setisAgent(resultSet.getString("isAgent"));
			return logindetail;
		}
	}
}
