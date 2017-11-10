package com.molo.dao;

import java.io.IOException;
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
import com.molo.entity.ChatMessage;
import com.molo.extra.JsonNodeRowMapper;

public class ChatMessageDao extends BaseDao implements MDaoInterface<ChatMessage>{
	private ObjectMapper mapper;
	public ChatMessageDao(DBHandler dbh) {
		super(dbh);
		// TODO Auto-generated constructor stub
		mapper=new ObjectMapper();
	}
	@Override
	public ChatMessage insert(ChatMessage message) {
		// TODO Auto-generated method stub
		String req="INSERT INTO chat_message SET content=?, sender=?,receiver=?, thread=?,date=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req,com.mysql.jdbc.Statement.RETURN_GENERATED_KEYS);
			stat.setString(1, message.getContent());
			stat.setString(2, message.getSender());
			stat.setString(3, message.getReceiver());
			stat.setLong(4, message.getThread());
			stat.setString(5, message.getDate());
			stat.executeUpdate();
			ResultSet re=stat.getGeneratedKeys();
			if(re.next()){				
				message.setId(re.getLong(1));
				re.close();
			}
			stat.close();
		} catch (SQLException e) {
			//System.out.println("Nullllar ya");
			e.printStackTrace();
			return null;
		}
		return message;
	}
	@Override
	public void delete(ChatMessage m) {
		// TODO Auto-generated method stub
		String req="DELETE FROM chat_message WHERE id=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req);
			stat.setLong(1, m.getId());
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void delete(int id) {
		// TODO Auto-generated method stub
		String req="DELETE FROM chat_message WHERE id=?";
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
	public void deleteAllForAThread(int thId) {
		// TODO Auto-generated method stub
		String req="DELETE FROM chat_message cm WHERE thread=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req);
			stat.setInt(1, thId);
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public ChatMessage update(ChatMessage t) {
		return null;
	}
	@Override
	public List<ChatMessage> getAll() {
		List<ChatMessage> ret=new ArrayList<ChatMessage>();
		String req="SELECT * FROM chat_message";
		try {
			ResultSet r=this.request.executeQuery(req);
			while(r.next()){
				ret.add(mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),ChatMessage.class));
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
	public List<ChatMessage> getAllDescForAThread(long th) {
		List<ChatMessage> ret=new ArrayList<ChatMessage>();
		String req="SELECT * FROM chat_message WHERE thread=? ORDER BY id DESC";
		try {
			PreparedStatement st=request.getConnection().prepareStatement(req);
			st.setLong(1, th);
			ResultSet r=st.executeQuery();
			while(r.next()){
				ret.add(mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),ChatMessage.class));
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
	public List<ChatMessage> getAllForAThread(long th) {
		List<ChatMessage> ret=new ArrayList<ChatMessage>();
		String req="SELECT * FROM chat_message WHERE thread=?";
		try {
			PreparedStatement st=request.getConnection().prepareStatement(req);
			st.setLong(1, th);
			ResultSet r=st.executeQuery();
			while(r.next()){
				ret.add(mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),ChatMessage.class));
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

	public ChatMessage getOne(int id) {
		ChatMessage ret=null;
		String req="SELECT * FROM chat_message WHERE id=?";
		try {
			PreparedStatement st=this.request.getConnection().prepareStatement(req);
			st.setInt(1, id);
			ResultSet r=st.executeQuery();
			if(r.next()){
				ret=mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),ChatMessage.class);
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
	public ChatMessage getLastMessage(long id) {
		// TODO Auto-generated method stub
		String req="SELECT * FROM chat_message WHERE thread=? ORDER BY id DESC LIMIT 1";
		try {
			PreparedStatement st=request.getConnection().prepareStatement(req);
			st.setLong(1, id);
			ResultSet r=st.executeQuery();
			if(r.next()){
				return (mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),ChatMessage.class));
			}else{
				return null;
			}
		} catch (SQLException | IllegalArgumentException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	}
	public long getCountFor(long id) {
		String req="SELECT COUNT(id) as n FROM chat_message WHERE thread=?";
		try {
			PreparedStatement st=request.getConnection().prepareStatement(req);
			st.setLong(1, id);
			ResultSet r=st.executeQuery();
			if(r.next()){
				return r.getLong("n");
			}else{
				return 0;
			}
		} catch (SQLException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return 0;
	}

}
