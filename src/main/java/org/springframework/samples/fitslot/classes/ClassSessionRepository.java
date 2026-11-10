// classes/ClassSessionRepository.java
package org.springframework.samples.fitslot.classes;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface ClassSessionRepository extends CrudRepository<ClassSession, Integer> {
    List<ClassSession> findByDateOrderByStartTimeAsc(LocalDate date);
}