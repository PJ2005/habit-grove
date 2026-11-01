package com.example.grove.repository;

import com.example.grove.model.AppUser;
import com.example.grove.model.Tree;
import com.example.grove.model.TreeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TreeRepository extends JpaRepository<Tree, Long> {
    Optional<Tree> findFirstByOwnerAndStatus(AppUser owner, TreeStatus status);
    List<Tree> findByOwnerOrderByCreatedAtAsc(AppUser owner);
}
