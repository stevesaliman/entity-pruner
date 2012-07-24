# The Entity Pruner #
This project deals with the "Impedence Mismatch" that occurs when JPA entities need to be returned across RMI, AMF, Web Service calls, etc. It currently only works with a combination of Hibernate and JPA.

# Latest News #
The entity Pruner is now tested in Spring 3.1.  It is also tested against a MySql database.

# Details #
It contains Just 3 classes that interface with your code:

1) A PrunableEntity interface that marks your own POJO entities as "prunable".

2) An EntityPruner interface that defines the work the the EntityPruner can do. It implements the EJB local interface for use in Java EE containers.

3) An EntityPrunerHibernateJpa class that does the actual work of pruning an entity to make it safe for transmission over the wire. This class can be injected into service beans.

In addition, there is an EntityUtil class that you can use to populate an entity with the desired collections and attributes while there is still a session, and before pruning.

For more information on how to use this package, see the javadocs for the classes.

There is one known bug with the EntityPruner: When a client sets a pruned entity's parent to null, when it had a previous value (for example removing an employee from a department), the EntityPruner will restore the value when it unprunes. The workaround is to find and remove the entry in the fieldIdMap for the entity's parent attribute.

Keep an eye on the updates page to see when bugs have been fixed, or features have been added. 
