package net.saliman.entitypruner.testhelper;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.mapping.Table;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;
/**
 * This class generates Id values for Hibernate entities. Hibernate's built
 * in <code>SequenceGenerator</code> cannot generate ID values for 
 * <code>BigInteger</code> columns.  This class can.  To use this generator
 * in a JPA environment, add the following annotations on the ID column
 * of your Entity class (in addition to Id and Column annotations):
 * <code>
   <pre>
    &#064;GeneratedValue(strategy=GenerationType.SEQUENCE, generator="ID_GENERATOR")
    &#064;GenericGenerator(name="ID_GENERATOR",
                      strategy = "net.saliman.entitypruner.testhelper.BigIntegerSequenceGenerator",
                      parameters = {
                          &#064;Parameter(name="sequence", value="<i>&lt;sequence_name&gt;</i>")
                      })
   </pre>
 * </code>
 * 
 * @author Steven C. Saliman
 */
public class BigIntegerSequenceGenerator implements PersistentIdentifierGenerator, Configurable {

    /** The sequence name parameter */
    public static final String SEQUENCE = "sequence";

    /**
     * The parameters parameter, appended to the create sequence DDL. For
     * example (Oracle):
     * <tt>INCREMENT BY 1 START WITH 1 MAXVALUE 100 NOCACHE</tt>.
     */
    public static final String PARAMETERS = "parameters";
    private static final Logger LOG = Logger.getLogger(BigIntegerSequenceGenerator.class);

    private String sequenceName;
    private String parameters;
    private Type identifierType;
    private String sql;

    @Override
    public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
        this.sequenceName = PropertiesHelper.getString(SEQUENCE, params, "hibernate_sequence");
        this.parameters = params.getProperty(PARAMETERS);
        String schemaName = params.getProperty(SCHEMA);
        String catalogName = params.getProperty(CATALOG);

        if (sequenceName.indexOf("." ) < 0) {
            sequenceName = Table.qualify( catalogName, schemaName, sequenceName );
        }

        this.identifierType = type;
        sql = dialect.getSequenceNextValString(sequenceName);
    }

    /**
     * This is the heart of this class.  It does the same thing as Hibernate's
     * SequenceGenerator, but it uses the local {@link #get(ResultSet, Type)}
     * method instead of Hibernate's factory to get the actual ID.
     */
    @Override
    public Serializable generate(SessionImplementor session, Object obj)
    throws HibernateException {
        try {

            PreparedStatement st = session.getBatcher().prepareSelectStatement(sql);
            try {
                ResultSet rs = st.executeQuery();
                try {
                    rs.next();
                    Serializable result = get(
                            rs, identifierType
                    );
                    if ( LOG.isDebugEnabled() ) {
                        LOG.debug("Sequence identifier generated: " + result);
                    }
                    return result;
                }
                finally {
                    rs.close();
                }
            }
            finally {
                session.getBatcher().closeStatement(st);
            }

        }
        catch (SQLException sqle) {
            throw JDBCExceptionHelper.convert(
                    session.getFactory().getSQLExceptionConverter(),
                    sqle,
                    "could not get next sequence value",
                    sql
            );
        }

    }

    /**
     * This method gets the ID value from the sequence casted to the right
     * type for the ID column.
     * @param rs The <code>ResultSet</code> containing the Id.
     * @param type The datatype of the ID column.
     * @return The ID from the sequence as the correct type.
     * @throws SQLException If we can't read the result set.
     * @throws IdentifierGenerationException
     */
    public static Serializable get(ResultSet rs, Type type)
    throws SQLException, IdentifierGenerationException {

        Class<?> clazz = type.getReturnedClass();
        if ( clazz==Long.class ) {
            return new Long( rs.getLong(1) );
        }
        else if ( clazz==Integer.class ) {
            return new Integer( rs.getInt(1) );
        }
        else if ( clazz==Short.class ) {
            return new Short( rs.getShort(1) );
        }
        else if ( clazz==String.class ) {
            return rs.getString(1);
        }
        else if ( clazz==BigInteger.class ) {
            return new BigInteger(rs.getString(1));
        }
        else {
            throw new IdentifierGenerationException("this id generator generates long, integer, short, BigInteger or string");
        }

    }

    @SuppressWarnings("deprecation")
    @Override
    public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
        String[] ddl = dialect.getCreateSequenceStrings(sequenceName);
        if ( parameters!=null ) ddl[ddl.length-1] += ' ' + parameters;
        return ddl;
    }

    @Override
    public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
        return dialect.getDropSequenceStrings(sequenceName);
    }

    @Override
    public Object generatorKey() {
        return sequenceName;
    }

} 