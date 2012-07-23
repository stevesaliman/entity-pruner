-- This file contains tables needed for the unit tests.  They are NOT needed
-- by applications using the framework.
--
-- All objects are creaetd in the current database.
--
-- This script also doesn't worry about permissions.  We assume that framework
-- tests will run as the same user that creaetd the objects
--
-- Note due to a MySql  bug (http://bugs.mysql.com/bug.php?id=6295), we can't
-- have "NOT NULL" constraints on columns that are potentially populated by
-- triggers.

delimiter //

-- The following tables are used for testing the BaseDao and DataObject.  It
-- is important that they look like actual application tables, complete with
-- constraints, etc.  The Framework assumes a TABLE stragegy for id generation,
-- so the id columns should not AUTO_INCREMENT.

-- ---------------------------------------------------------------------------
-- Table: test_parent    
-- This is the basic test table for testing the base DAO implementations.
-- It has columns of different types for testing different things.  It is up
-- to the tests themselves to setup the data for each test. It also tests 
-- objects with children.  There are two kinds of children; children that have
-- a parent object (bidirectional), and children that have only the parent's
-- ID (unidirectional).  Each will be in a different table so Hibernate can 
-- map them differently. The parent-child relationships will help test the
-- prune and unprune methods of the entity pruner.
create table test_parent (
    id                         INT NOT NULL auto_increment,
    version                    INT DEFAULT 0 NOT NULL,
    code                       VARCHAR(50) NOT NULL,
    description                VARCHAR(2000),
    string_value               VARCHAR(50),
    int_value                  INT,
    double_value               DOUBLE(5,2),
    date_value                 DATETIME,
    true_flag                  CHAR(1) DEFAULT 'N' NOT NULL,
    clob_value                 TEXT null,
    blob_value                 BLOB null,
    create_user                VARCHAR(30),
    create_date                DATETIME,
    update_user                VARCHAR(30),
    update_date                DATETIME,
    CONSTRAINT test_parent_pk
        PRIMARY KEY (id)
)
//

alter table test_parent
    add constraint parent_code_unq
        unique(code)
//

alter table test_parent
    add constraint parent_true_flag_chk
        check (true_flag IN ('Y', 'y', 'N', 'n'))
//

create trigger test_parent_bins BEFORE INSERT ON test_parent FOR EACH ROW
BEGIN
    IF new.create_user IS NULL  or new.create_user = '' THEN
        set new.create_user = user();
    END IF; 
    IF new.create_date IS NULL OR new.create_date = 0 THEN
        set new.create_date = now();
    END IF; 
    IF new.update_user IS NULL  or new.update_user = '' THEN
        set new.update_user = user();
    END IF; 
    IF new.update_date IS NULL OR new.update_date = 0 THEN
        set new.update_date = now();
    END IF; 
END;
//

create trigger test_parent_bupd BEFORE UPDATE ON test_parent FOR EACH ROW
BEGIN
    IF new.update_user IS NULL OR new.update_user = '' THEN
        set new.update_user = user();
    END IF; 
    IF new.update_date IS NULL OR new.update_date = 0 THEN
        set new.update_date = now();
    END IF; 
END;
//

-- ---------------------------------------------------------------------------
-- Table: test_child     
-- This table will be used to test the base Hibernate DAO for objects with
-- parents. 
create table test_child (
    id                         INT NOT NULL auto_increment,
    version                    INT DEFAULT 0 NOT NULL,
    test_parent_id             INT NOT NULL,
    code                       VARCHAR(50) NOT NULL,
    description                VARCHAR(2000),
    create_user                VARCHAR(30),
    create_date                DATETIME,
    update_user                VARCHAR(30),
    update_date                DATETIME,
    CONSTRAINT test_child_pk
        PRIMARY KEY (id)
)
//

alter table test_child
    add constraint child_parent_id_fk
        foreign key(test_parent_id)
            references test_parent(id)
//

alter table test_child
    add constraint child_parent_code_unq
        unique(test_parent_id, code)
//

create trigger test_child_bins BEFORE INSERT ON test_child FOR EACH ROW
BEGIN
    IF new.create_user IS NULL  or new.create_user = '' THEN
        set new.create_user = user();
    END IF; 
    IF new.create_date IS NULL OR new.create_date = 0 THEN
        set new.create_date = now();
    END IF; 
    IF new.update_user IS NULL  or new.update_user = '' THEN
        set new.update_user = user();
    END IF; 
    IF new.update_date IS NULL OR new.update_date = 0 THEN
        set new.update_date = now();
    END IF; 
END;
//

create trigger test_child_bupd BEFORE UPDATE ON test_child FOR EACH ROW
BEGIN
    IF new.update_user IS NULL OR new.update_user = '' THEN
        set new.update_user = user();
    END IF; 
    IF new.update_date IS NULL OR new.update_date = 0 THEN
        set new.update_date = now();
    END IF; 
END;
//

-- ---------------------------------------------------------------------------
-- Table: test_uni_child     
-- This table will be used to test the base Hibernate DAO for objects with
-- parent ids. 
create table test_uni_child (
    id                         INT NOT NULL auto_increment,
    version                    INT DEFAULT 0 NOT NULL,
    test_parent_id             INT NOT NULL,
    code                       VARCHAR(50) NOT NULL,
    description                VARCHAR(2000),
    create_user                VARCHAR(30),
    create_date                DATETIME,
    update_user                VARCHAR(30),
    update_date                DATETIME,
    CONSTRAINT test_uni_child_pk
        PRIMARY KEY (id)
)
//

alter table test_uni_child
    add constraint uni_child_parent_id_fk
        foreign key(test_parent_id)
            references test_parent(id)
//

alter table test_uni_child
    add constraint uni_child_parent_code_unq
        unique(test_parent_id, code)
//

create trigger test_uni_child_bins BEFORE INSERT ON test_uni_child FOR EACH ROW
BEGIN
    IF new.create_user IS NULL  or new.create_user = '' THEN
        set new.create_user = user();
    END IF; 
    IF new.create_date IS NULL OR new.create_date = 0 THEN
        set new.create_date = now();
    END IF; 
    IF new.update_user IS NULL  or new.update_user = '' THEN
        set new.update_user = user();
    END IF; 
    IF new.update_date IS NULL OR new.update_date = 0 THEN
        set new.update_date = now();
    END IF; 
END;
//

create trigger test_uni_child_bupd BEFORE UPDATE ON test_uni_child FOR EACH ROW
BEGIN
    IF new.update_user IS NULL OR new.update_user = '' THEN
        set new.update_user = user();
    END IF; 
    IF new.update_date IS NULL OR new.update_date = 0 THEN
        set new.update_date = now();
    END IF; 
END;
//

commit//

delimiter ;
