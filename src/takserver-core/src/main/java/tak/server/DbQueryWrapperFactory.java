package tak.server;

import org.springframework.beans.factory.FactoryBean;

import com.bbn.marti.JDBCQueryAuditLogHelper;

public class DbQueryWrapperFactory implements FactoryBean<JDBCQueryAuditLogHelper> {
 
    @Override
    public JDBCQueryAuditLogHelper getObject() throws Exception {
        return new JDBCQueryAuditLogHelper();
    }
 
    @Override
    public Class<?> getObjectType() {
        return JDBCQueryAuditLogHelper.class;
    }
 
    @Override
    public boolean isSingleton() {
        return false;
    }
 
}