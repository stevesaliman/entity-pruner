# The Entity Pruner #
This project deals with the "Impedence Mismatch" that occurs when JPA entities need to be returned across RMI, AMF, Web Service calls, etc. It currently only works with a combination of Hibernate and JPA.

# Latest News #
The entity Pruner is now tested in Spring 3.1.  It is also tested against a MySql database.

# Really Really Quick Start #

Include net.saliman:entity-pruner:3.1.0 in your maven or gradle compile dependencies.

# Details #

The Entity Pruner contains Just 3 classes that interface with your code:

1) A PrunableEntity interface that marks your own POJO entities as "prunable".

2) An EntityPruner interface that defines the work the the EntityPruner can do. It implements the EJB local interface for use in Java EE containers.

3) An EntityPrunerHibernateJpa class that does the actual work of pruning an entity to make it safe for transmission over the wire. This class can be injected into service beans.

In addition, there is an EntityUtil class that you can use to populate an entity with the desired collections and attributes while there is still a session, and before pruning.

For more information on how to use this package, see the javadocs for the classes.

# Supported Container and Database Versions #

The Entity Pruner has been tested in Oracle 10 and 11 databases, as well as MySql 5.5.  It has been tested in GlassFish 2 and 3, as well as Spring 3.1.

There is one known bug with the EntityPruner: When a client sets a pruned entity's parent to null, when it had a previous value (for example removing an employee from a department), the EntityPruner will restore the value when it unprunes. The workaround is to find and remove the entry in the fieldIdMap for the entity's parent attribute.

Keep an eye on the updates page to see when bugs have been fixed, or features have been added. 

# Building the Entity Pruner

Because the Oracle JDBC jar is not in Maven Central, it must be installed to
your local Maven repository with ```mvn install:install-file -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=10.2.0.4.0 -Dpackaging=jar -Dfile=glassfish-3.1.1/lib/ojdbc6.jar -DgeneratePom=true ``` before the Entity Pruner can be built.

Building from source can be done with ```gradle install```, though unit tests
can be tricky to run because of the way I'm doing GlassFish in-container
testing. 

Also note that the cobertura task will currently fail while I resolve a bug
in Cobertura and/or the Cobertura plugin.
