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
import com.molo.entity.Attachment;
import com.molo.extra.JsonNodeRowMapper;

public class AttachmentDao extends BaseDao implements MDaoInterface<Attachment>{
	private ObjectMapper mapper;
	public AttachmentDao(DBHandler dbh) {
		super(dbh);
		// TODO Auto-generated constructor stub
		mapper=new ObjectMapper();
	}
	@Override
	public Attachment insert(Attachment message) {
		// TODO Auto-generated method stub
		String req="INSERT INTO chat_message_attachment SET path=?, size=?,type=?, message=?";
		try {
			PreparedStatement stat=this.request.getConnection().prepareStatement(req,com.mysql.jdbc.Statement.RETURN_GENERATED_KEYS);
			stat.setString(1, message.getPath());
			stat.setLong(2, message.getSize());
			stat.setString(3, message.getType());
			stat.setLong(4, message.getMessage());
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
	public void delete(Attachment m) {
		// TODO Auto-generated method stub
		String req="DELETE FROM chat_message_attachment WHERE id=?";
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
		String req="DELETE FROM chat_message_attachment WHERE id=?";
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
	public void deleteAllForAAttachment(int thId) {
		// TODO Auto-generated method stub
		String req="DELETE FROM chat_message_attachment cm WHERE message=?";
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
	public Attachment update(Attachment t) {
		return null;
	}
	@Override
	public List<Attachment> getAll() {
		List<Attachment> ret=new ArrayList<Attachment>();
		String req="SELECT * FROM chat_message_attachment";
		try {
			ResultSet r=this.request.executeQuery(req);
			while(r.next()){
				ret.add(mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),Attachment.class));
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
	public List<Attachment> getAllForAMessage(long th) {
		List<Attachment> ret=new ArrayList<Attachment>();
		String req="SELECT * FROM chat_message_attachment WHERE message=?";
		try {
			PreparedStatement st=request.getConnection().prepareStatement(req);
			st.setLong(1, th);
			ResultSet r=st.executeQuery();
			while(r.next()){
				ret.add(mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),Attachment.class));
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

	public Attachment getOne(int id) {
		Attachment ret=null;
		String req="SELECT * FROM chat_message_attachment WHERE id=?";
		try {
			PreparedStatement st=this.request.getConnection().prepareStatement(req);
			st.setInt(1, id);
			ResultSet r=st.executeQuery();
			if(r.next()){
				ret=mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),Attachment.class);
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
	public Attachment getLastAttachment(long id) {
		// TODO Auto-generated method stub
		String req="SELECT * FROM chat_message_attachment WHERE message=? ORDER BY id DESC LIMIT 1";
		try {
			PreparedStatement st=request.getConnection().prepareStatement(req);
			st.setLong(1, id);
			ResultSet r=st.executeQuery();
			if(r.next()){
				return (mapper.readValue(JsonNodeRowMapper.mapRow(mapper, r),Attachment.class));
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
		String req="SELECT COUNT(id) as n FROM chat_message_attachment WHERE message=?";
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
