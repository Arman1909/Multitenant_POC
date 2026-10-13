package com.example.pbm.dto.response;
import com.example.pbm.entity.*;
import java.util.UUID;
public record UserResponse(UUID id, String username, Role role, DeRole deRole, String status) {
    public static UserResponse from(User u) { return new UserResponse(u.getId(), u.getUsername(), u.getRole(), u.getDeRole(), u.getStatus()); }
}
