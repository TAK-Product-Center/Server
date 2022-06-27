package com.bbn.tak.schema;

import com.beust.jcommander.Parameters;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Parameters(commandDescription = "Setup a Postgres instance. Will install postgis, its dependencies and set them to the superuser")
public class SetupPostresGeneric extends Command
{
    public static final String name = "SetupGenericDatabase";

    SetupPostresGeneric(SchemaManager manager)
    {
        super(manager, LoggerFactory.getLogger(SetupPostresGeneric.class));
    }

    @Override
    public boolean execute()
    {
        boolean installSucceeded = false;
        String database = schemaManager.commonOptions.database;
        try (Connection connection = schemaManager.getConnection())
        {
            Statement installStatement = null;
            try
            {
                StringBuilder sqlBuilder = new StringBuilder();

                sqlBuilder.append("create extension if not exists fuzzystrmatch;");
                sqlBuilder.append("create extension if not exists postgis_tiger_geocoder;");
                sqlBuilder.append("create extension if not exists postgis_topology;");
                sqlBuilder.append("alter schema tiger owner to postgres;");
                sqlBuilder.append("alter schema tiger_data owner to postgres;");
                sqlBuilder.append("alter schema topology owner to postgres;");
                sqlBuilder.append("CREATE FUNCTION exec(text) returns text language plpgsql volatile AS $f$ BEGIN EXECUTE $1; RETURN $1; END; $f$;");
                sqlBuilder.append("SELECT exec('ALTER TABLE ' || quote_ident(s.nspname) || '.' || quote_ident(s.relname) || ' OWNER TO postgres;') FROM (SELECT nspname, relname FROM pg_class c JOIN pg_namespace n ON (c.relnamespace = n.oid) WHERE nspname in ('tiger','topology') AND relkind IN ('r','S','v') ORDER BY relkind = 'S') s;");
                String sql = sqlBuilder.toString();
                installStatement = connection.createStatement();
                logger.debug(sql);
                installStatement.execute(sql);
                logger.info("SUCCESS. Installed Postgis and its dependencies to Postgres Database " + database);
                installSucceeded = true;
            } catch (SQLException ex)
            {
                logger.info(ex.getMessage());
                if (logger.isDebugEnabled())
                {
                    ex.printStackTrace();
                }
            } finally
            {
                if (installStatement != null)
                {
                    try
                    {
                        installStatement.close();
                    } catch (SQLException ex)
                    {
                        logger.debug(ex.getMessage());
                    }
                }
            }
        } catch (SQLException ex)
        {
            logger.error("Error trying to open SQL connection ", ex);
        }

        return installSucceeded;
    }
}
