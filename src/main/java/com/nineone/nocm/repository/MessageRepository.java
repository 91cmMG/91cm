package com.nineone.nocm.repository;

import java.util.List;
import java.util.Map;

import com.nineone.nocm.domain.Message;

public interface MessageRepository {
	int insertMessage(Message msg);
	List<Message> getMessageList(Map<String,Object> map);
}
