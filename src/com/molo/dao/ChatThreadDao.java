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
import com.molo.entity.ChatThread;
import com.molo.entity.ChatThread;
import com.molo.entity.ChatThread;
import com.molo.extra.JsonNodeRowMapper;

public class ChatThreadDao extends BaseDao implements MDaoInterface<ChatThread>{
	private ObjectMapper mapper=new ObjectMapper();
	public ChatThreadDao(DBHandler dbh) {
		super(dbh);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ChatThread insert(ChatThread t) {
		// TODO Auto-generated method stub
		String req="INSERT INTO chat_thread SET member1=?, member2=?,date=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req,Statement.RETURN_GENERATED_KEYS);
			stat.setString(1, t.getMember1());
			stat.setString(2, t.getMember2());
			stat.setString(3, t.getDate());
			stat.executeUpdate();
			ResultSet re=stat.getGeneratedKeys();
			if(re.next()){				
				t.setId(re.getLong(1));
				re.close();
			}
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return t;
		}
		return t;
	}

	@Override
	public void delete(ChatThread t) {
		// TODO Auto-generated method stub
		String req="DELETE FROM chat_thread WHERE id=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req);
			stat.setLong(1, t.getId());
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void delete(int id) {
		// TODO Auto-generated method stub
		String req="DELETE FROM chat_thread WHERE id=?";
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

	@Override
	public ChatThread update(ChatThread t) {
		// TODO Auto-generated method stub
		return null;
	}
	public int updateUnreadNumber(long tid,int increment) {
		// TODO Auto-generated method stub
		String req="UPDATE  chat_thread SET unread=unread+? WHERE id=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req,Statement.RETURN_GENERATED_KEYS);
			stat.setInt(1, increment);
			stat.setLong(2, tid);
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		return getOne(tid).getUnread();
	}
	@Override
	public List<ChatThread> getAll() {
		List<ChatThread> ret=new ArrayList<ChatThread>();
		String req="SELECT * FROM chat_thread";
		try {
			ResultSet r=this.request.executeQuery(req);
			while(r.next()){
				ret.add(mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),ChatThread.class));
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
	public List<ChatThread> getAllForAMember(String mId) {
		List<ChatThread> ret=new ArrayList<ChatThread>();
		String req="SELECT *  FROM chat_thread WHERE member1=? OR member2=?";
		try {
			PreparedStatement st=this.request.getConnection().prepareStatement(req);
			st.setString(1, mId);
			st.setString(2, mId);
			ResultSet r=st.executeQuery();
			while(r.next()){
				ret.add(mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),ChatThread.class));
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


	public ChatThread getOne(long id) {
		ChatThread ret=null;
		String req="SELECT * FROM chat_thread WHERE id=?";
		try {
			PreparedStatement st=this.request.getConnection().prepareStatement(req);
			st.setLong(1, id);
			ResultSet r=st.executeQuery();
			if(r.next()){
				ret=mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),ChatThread.class);
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

	public ChatThread getFor(String m1, String m2) {
		// TODO Auto-generated method stub
		ChatThread ret=null;
		String req="SELECT * FROM chat_thread WHERE (member1=? AND member2=?) OR (member1=? AND member2=?)";
		try {
			PreparedStatement st=this.request.getConnection().prepareStatement(req);
			st.setString(1, m1);
			st.setString(4, m1);
			st.setString(2, m2);
			st.setString(3, m2);
			ResultSet r=st.executeQuery();
			if(r.next()){
				ret=mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),ChatThread.class);
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

	public void setUnread(long tid, int n) {
		// TODO Auto-generated method stub
		String req="UPDATE  chat_thread SET unread=? WHERE id=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req,Statement.RETURN_GENERATED_KEYS);
			stat.setInt(1, n);
			stat.setLong(2, tid);
			stat.executeUpdate();
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
