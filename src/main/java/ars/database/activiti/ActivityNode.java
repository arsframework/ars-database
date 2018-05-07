package ars.database.activiti;

import java.io.Serializable;

/**
 * Activiti活动节点
 *
 * @author wuyongqiang
 */
public class ActivityNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id; // 状态标识
    private String code; // 状态编号
    private String name; // 状态名称

    public ActivityNode(int id, String code, String name) {
        if (code == null) {
            throw new IllegalArgumentException("Code must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return 31 + this.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ActivityNode)) {
            return false;
        }
        return this.code.equals(((ActivityNode) obj).getCode());
    }

    @Override
    public String toString() {
        return this.name;
    }

}
