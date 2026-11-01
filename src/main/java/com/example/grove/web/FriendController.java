package com.example.grove.web;

import com.example.grove.model.Friendship;
import com.example.grove.model.TreeShare;
import com.example.grove.service.SharingService;
import com.example.grove.service.SharingService.FriendTreeView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class FriendController {

    private final SharingService sharing;

    public FriendController(SharingService sharing) {
        this.sharing = sharing;
    }

    public record IdResponse(Long id) {}
    public record ShareRequest(boolean showHabitNames) {}
    public record ShareResponse(Long treeId, Long viewerId, boolean showHabitNames) {}

    /** Send a friend request from userId to friendId. */
    @PostMapping("/{userId}/friends/{friendId}")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse request(@PathVariable Long userId, @PathVariable Long friendId) {
        Friendship f = sharing.requestFriendship(userId, friendId);
        return new IdResponse(f.getId());
    }

    /** The user accepts a request sent by requesterId. */
    @PostMapping("/{userId}/friend-requests/{requesterId}/accept")
    public void accept(@PathVariable Long userId, @PathVariable Long requesterId) {
        sharing.acceptFriendship(userId, requesterId);
    }

    /** The owner grants a friend access to their tree, with or without habit names. */
    @PutMapping("/{ownerId}/tree/shares/{viewerId}")
    public ShareResponse setShare(@PathVariable Long ownerId, @PathVariable Long viewerId,
                                  @RequestBody ShareRequest req) {
        TreeShare share = sharing.setShare(ownerId, viewerId, req.showHabitNames());
        return new ShareResponse(share.getTree().getId(), viewerId, share.isShowHabitNames());
    }

    /** A friend views the owner's tree. */
    @GetMapping("/{viewerId}/friends/{ownerId}/tree")
    public FriendTreeView viewFriendTree(@PathVariable Long viewerId, @PathVariable Long ownerId) {
        return sharing.viewTree(viewerId, ownerId);
    }
}
