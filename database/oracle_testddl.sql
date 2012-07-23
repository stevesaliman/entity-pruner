--------------------------------------------------------------------------------------------
--
-- This file contains tables needed for the unit tests.  They are NOT needed
-- by applications using the framework.
--
-- All objects are creaetd in the default tablespace.  You will probably want
-- to modify this script to add tablespace information.
--
-- This script also doesn't worry about permissions.  We assume that framework
-- tests will run as the schema owner.

/*============================================================================*/
/* Table: Test_xml
/* This table will be used to test the Oracle XML datatype for Hibernate.
/*============================================================================*/
create table test_xml (
    id                         NUMBER(18) NOT NULL,
    version                    NUMBER(18) DEFAULT 0 NOT NULL,
    description                VARCHAR2(2000),
    clob_data                  CLOB NULL,
    xml_data                   XMLTYPE NULL,
    create_user                VARCHAR2(30) DEFAULT USER NOT NULL,
    create_date                DATE DEFAULT SYSDATE NOT NULL,
    update_user                VARCHAR2(30) DEFAULT USER NOT NULL,
    update_date                DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT test_xml_pk
        PRIMARY KEY (id)
);

insert into test_xml(id, description, clob_data, xml_data, create_user, 
                        update_user)
values(-1, 'Test Record 1',
'<project>
    <target name="compile-tests"
            depends="-check-properties, compile"
            description="Compile all of the unit tests for the project" >
        <mkdir dir="${test.build.dir}/classes"/>
        <javac srcdir="${test.src.dir}/com"
               destdir="${test.build.dir}/classes"
               debug="${compile.debug}"
               deprecation="${compile.deprecation}"
               optimize="${compile.optimize}"
               source="${compiler.source}"
               target="${compiler.target}" >
            <include name="**/*.java"/>
            <classpath>
                <path refid="project.class.path" />
                <pathelement path="${src.build.dir}/classes" />
            </classpath>
        </javac>
    </target>
</project>',
xmltype('<project>
    <target name="compile-tests"
            depends="-check-properties, compile"
            description="Compile all of the unit tests for the project" >
        <mkdir dir="${test.build.dir}/classes"/>
        <javac srcdir="${test.src.dir}/com"
               destdir="${test.build.dir}/classes"
               debug="${compile.debug}"
               deprecation="${compile.deprecation}"
               optimize="${compile.optimize}"
               source="${compiler.source}"
               target="${compiler.target}" >
            <include name="**/*.java"/>
            <classpath>
                <path refid="project.class.path" />
                <pathelement path="${src.build.dir}/classes" />
            </classpath>
        </javac>
    </target>
</project>'), 'JUNIT', 'JUNIT');

commit;

-- The following tables are used for testing the BaseDao and DataObject.  It
-- is important that they look like actual application tables, complete with
-- constraints, etc.
/*============================================================================*/
/* Table: test_parent    
/* This is the basic test table for testing the base DAO implementations.
/* It has columns of different types for testing different things.  It is up
/* to the tests themselves to setup the data for each test. It also tests 
/* objects with children.  There are two kinds of children; children that have
/* a parent object (bidirectional), and children that have only the parent's
/* ID (unidirectional).  Each will be in a different table so Hibernate can 
/* map them differently. The parent-child relationships will help test the
/* dehydrate and rehydrate methods.
/*============================================================================*/
create table test_parent (
    id                         NUMBER(18) NOT NULL,
    version                    NUMBER(18) DEFAULT 0 NOT NULL,
    code                       VARCHAR2(50) NOT NULL,
    description                VARCHAR2(2000),
    string_value               VARCHAR2(50),
    int_value                  NUMBER(5),
    double_value               NUMBER(5,2),
    date_value                 DATE,
    true_flag                  CHAR(1) DEFAULT 'N' NOT NULL,
    clob_value                 CLOB null,
    blob_value                 BLOB null,
    create_user                VARCHAR2(30) DEFAULT USER NOT NULL,
    create_date                DATE DEFAULT SYSDATE NOT NULL,
    update_user                VARCHAR2(30) DEFAULT USER NOT NULL,
    update_date                DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT test_parent_pk
        PRIMARY KEY (id)
);

alter table test_parent
    add constraint parent_code_unq
        unique(code);

alter table test_parent
    add constraint parent_true_flag_chk
        check (true_flag IN ('Y', 'y', 'N', 'n'));
/*============================================================================*/
/* Table: test_child     
/* This table will be used to test the base Hibernate DAO for objects with
/* parents. 
/*============================================================================*/
create table test_child (
    id                         NUMBER(18) NOT NULL,
    version                    NUMBER(18) DEFAULT 0 NOT NULL,
    test_parent_id             NUMBER(18) NOT NULL,
    code                       VARCHAR2(50) NOT NULL,
    description                VARCHAR2(2000),
    create_user                VARCHAR2(30) DEFAULT USER NOT NULL,
    create_date                DATE DEFAULT SYSDATE NOT NULL,
    update_user                VARCHAR2(30) DEFAULT USER NOT NULL,
    update_date                DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT test_child_pk
        PRIMARY KEY (id)
);

alter table test_child
    add constraint child_parent_id_fk
        foreign key(test_parent_id)
            references test_parent(id);

alter table test_child
    add constraint child_parent_code_unq
        unique(test_parent_id, code);

/*============================================================================*/
/* Table: test_uni_child     
/* This table will be used to test the base Hibernate DAO for objects with
/* parent ids. 
/*============================================================================*/
create table test_uni_child (
    id                         NUMBER(18) NOT NULL,
    version                    NUMBER(18) DEFAULT 0 NOT NULL,
    test_parent_id             NUMBER(18) NOT NULL,
    code                       VARCHAR2(50) NOT NULL,
    description                VARCHAR2(2000),
    create_user                VARCHAR2(30) DEFAULT USER NOT NULL,
    create_date                DATE DEFAULT SYSDATE NOT NULL,
    update_user                VARCHAR2(30) DEFAULT USER NOT NULL,
    update_date                DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT test_uni_child_pk
        PRIMARY KEY (id)
);

alter table test_uni_child
    add constraint uni_child_parent_id_fk
        foreign key(test_parent_id)
            references test_parent(id);

alter table test_uni_child
    add constraint uni_child_parent_code_unq
        unique(test_parent_id, code);

/*-------------------------------------------------------------------------*/
/* Create trigger on the test_parent table to test triggers on an update.  */
/* Updates work differently than inserts in the persistence layer.         *
/*-------------------------------------------------------------------------*/
CREATE OR REPLACE TRIGGER test_parent_insupd
BEFORE INSERT OR UPDATE ON test_parent
FOR EACH ROW
DECLARE
    v_create_user test_parent.create_user%TYPE;
    v_update_user test_parent.create_user%TYPE;
BEGIN
IF (:new.create_user IS NOT NULL) THEN
    v_create_user := :new.create_user;
ELSE
    v_create_user := USER;
END IF;
IF (:new.update_user IS NOT NULL) THEN
    v_update_user := :new.update_user;
ELSE 
    v_update_user := USER;
END IF;
IF INSERTING THEN 
    :new.create_user := v_create_user;
    :new.create_date := SYSDATE;
    :new.update_user := v_update_user;
    :new.update_date := SYSDATE;
END IF; 
IF UPDATING THEN 
    :new.create_user := :old.create_user;
    :new.create_date := :old.create_date;
    :new.update_user := v_update_user;
    :new.update_date := SYSDATE;
END IF; 
END;
/
show errors;

-----------------------------------------------------------------------------
-- Create the sequence for ids
create sequence id_generator_seq;

commit;
