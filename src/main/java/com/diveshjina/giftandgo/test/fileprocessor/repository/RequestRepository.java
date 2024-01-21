package com.diveshjina.giftandgo.test.fileprocessor.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends CrudRepository<Request, UUID> {
}
