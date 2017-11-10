package com.molo.dao;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.molo.dbconnection.DBHandler;
import com.molo.entity.Membre;
import com.molo.extra.JsonNodeRowMapper;
import com.molo.security.Encrypt;

public class MembreDao extends BaseDao implements MDaoInterface<Membre>{
	private ObjectMapper mapper;
	public MembreDao(DBHandler dbh) {
		super(dbh);
		// TODO Auto-generated constructor stub
		mapper=new ObjectMapper();
	}
	public Membre insert(Membre m){
		String req="INSERT INTO members SET nom=?, prenom=?,login=?, password=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req,Statement.RETURN_GENERATED_KEYS);
			stat.setString(1, m.getNom());
			stat.setString(2, m.getPrenom());
			stat.setString(3, m.getLogin());
			stat.setString(4, Encrypt.encrypt(m.getPassword(), "SHA-1"));
			stat.executeUpdate();
			ResultSet re=stat.getGeneratedKeys();
			if(re.next()){				
				m.setId(re.getInt(1));
				re.close();
			}
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return m;
	}
	public void delete(Membre m){
		String req="DELETE FROM members WHERE id=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req);
			stat.setInt(1, m.getId());
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void delete(int id){
		String req="DELETE FROM members WHERE id=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req);
			stat.setInt(1, id);
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void updateNom(int id,String n){
		String req="UPDATE  members SET nom=? WHERE id=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req);
			stat.setInt(2, id);
			stat.setString(1, n);
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void updatePrenom(int id,String pn){
		String req="UPDATE  members SET prenom=? WHERE id=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req);
			stat.setInt(2, id);
			stat.setString(1, pn);
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void updatePassword(int id,String p){
		String req="UPDATE  members SET password=? WHERE id=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req);
			stat.setInt(1, id);
			stat.setString(2, p);
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public List<Membre> getAllMembre(){
		List<Membre> ret=new ArrayList<Membre>();
		String req="SELECT id,nom,prenom,password,login,profil FROM members";
		try {
			ResultSet r=this.request.executeQuery(req);
			while(r.next()){
				ret.add(mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),Membre.class));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	public Membre getOne(int id){
		Membre ret=null;
		String req="SELECT id,nom,prenom,password,login,profil FROM members WHERE id=?";
		try {
			PreparedStatement st=this.request.getConnection().prepareStatement(req);
			st.setInt(1, id);
			ResultSet r=st.executeQuery();
			if(r.next()){
				ret=mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),Membre.class);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	@Override
	public Membre update(Membre t) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<Membre> getAll() {
		// TODO Auto-generated method stub
		return getAllMembre();
	}
	public Membre getOne(String login, String password) {
		Membre ret=null;
		String req="SELECT * FROM members WHERE login=? AND password=?";
		try {
			PreparedStatement st=this.request.getConnection().prepareStatement(req);
			st.setString(1, login);
			st.setString(2, Encrypt.encrypt(password, "SHA-1"));
			ResultSet r=st.executeQuery();
			if(r.next()){
				ret=mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),Membre.class);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	public Membre getOne(String login) {
		Membre ret=null;
		String req="SELECT * FROM members WHERE login=?";
		try {
			PreparedStatement st=this.request.getConnection().prepareStatement(req);
			st.setString(1, login);
			ResultSet r=st.executeQuery();
			if(r.next()){
				ret=mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),Membre.class);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	public void updateProfil(int id, String prof) {
		// TODO Auto-generated method stub
		String req="UPDATE  members SET profil=? WHERE id=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req);
			stat.setString(1, prof);
			stat.setInt(2, id);
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public String getOneProfilPath(String other) {
		// TODO Auto-generated method stub
		Membre ret=null;
		String req="SELECT profil FROM members WHERE login=?";
		try {
			PreparedStatement st=this.request.getConnection().prepareStatement(req);
			st.setString(1, other);
			ResultSet r=st.executeQuery();
			if(r.next()){
				String p=r.getString("profil");
				return (p!=null)?p:"";
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
}
