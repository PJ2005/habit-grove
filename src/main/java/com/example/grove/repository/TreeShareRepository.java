package com.example.grove.repository;

import com.example.grove.model.AppUser;
import com.example.grove.model.Tree;
import com.example.grove.model.TreeShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TreeShareRepository extends JpaRepository<TreeShare, Long> {
    Optional<TreeShare> findByTreeAndViewer(Tree tree, AppUser viewer);
}
