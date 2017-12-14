package ars.database.repository;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import ars.database.repository.Repository;
import ars.database.repository.RepositoryFactory;

/**
 * 数据持久化操作工厂标准实现
 * 
 * @author yongqiangwu
 * 
 */
public class StandardRepositoryFactory implements RepositoryFactory {
	private Map<Class<?>, Repository<?>> repositories = Collections.emptyMap();

	public StandardRepositoryFactory(Repository<?>... repositories) {
		this.repositories = new HashMap<Class<?>, Repository<?>>(repositories.length);
		for (Repository<?> repository : repositories) {
			this.repositories.put(repository.getModel(), repository);
		}
	}

	@Override
	public Map<Class<?>, Repository<?>> getRepositories() {
		return Collections.unmodifiableMap(this.repositories);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Repository<T> getRepository(Class<T> model) {
		if (model == null) {
			throw new IllegalArgumentException("Illegal model:" + model);
		}
		Repository<T> repository = (Repository<T>) this.repositories.get(model);
		if (repository == null) {
			throw new RuntimeException("Repository not found:" + model);
		}
		return repository;
	}

}
