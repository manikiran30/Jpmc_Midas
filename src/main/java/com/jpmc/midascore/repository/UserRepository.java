package com.jpmc.midascore.repository;

import com.jpmc.midascore.entity.UserRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<UserRecord, Long> {
    // remove custom findById — use CrudRepository's Optional<UserRecord> findById(ID)
    UserRecord findByName(String name);
}
