package com.example.grove.repository;

import com.example.grove.model.AppUser;
import com.example.grove.model.FriendStatus;
import com.example.grove.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    Optional<Friendship> findByUserAndFriend(AppUser user, AppUser friend);
    Optional<Friendship> findByUserAndFriendAndStatus(AppUser user, AppUser friend, FriendStatus status);
}
