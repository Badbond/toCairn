package me.soels.thesis.repositories;

import me.soels.thesis.model.DataClass;
import me.soels.thesis.model.DataRelationship;
import me.soels.thesis.model.OtherClass;
import org.springframework.data.neo4j.repository.query.Query;

public interface OtherClassRepository extends ClassRepository<OtherClass> {
    @Query("MATCH (caller:OtherClass), (callee:DataClass) " +
            "WHERE ID(caller) = $0.__id__ AND ID(callee) = $1.__id__ " +
            "CREATE (caller)-[r:DATA_DEPENDS_ON]->(callee) SET r = $2.__properties__")
    void addDataRelationship(OtherClass caller, DataClass callee, DataRelationship relationship);
}
