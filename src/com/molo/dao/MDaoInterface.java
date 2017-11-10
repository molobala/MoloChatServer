package com.molo.dao;

import java.util.List;

public interface MDaoInterface<T> {
	public T insert(T t);
	public void delete(T t);
	public T update(T t);
	public List<T> getAll();
}
