package ars.database.spring;

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import ars.util.Strings;
import ars.invoke.event.InvokeEvent;
import ars.invoke.event.InvokeListener;
import ars.invoke.event.InvokeBeforeEvent;
import ars.invoke.event.InvokeCompleteEvent;

/**
 * 数据源路由器实现
 *
 * @author wuyongqiang
 */
public class DataSourceRouter extends AbstractRoutingDataSource implements InvokeListener<InvokeEvent> {
    private Map<String, String> routes = new HashMap<String, String>();
    private final ThreadLocal<String> dataSource = new ThreadLocal<String>();

    public Map<String, String> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, String> routes) {
        this.routes = routes;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return this.dataSource.get();
    }

    @Override
    public void onInvokeEvent(InvokeEvent event) {
        if (event instanceof InvokeBeforeEvent) {
            String uri = event.getSource().getUri();
            for (Entry<String, String> entry : this.routes.entrySet()) {
                if (Strings.matches(uri, entry.getKey())) {
                    this.dataSource.set(entry.getValue());
                    break;
                }
            }
        } else if (event instanceof InvokeCompleteEvent) {
            this.dataSource.remove();
        }
    }

}
