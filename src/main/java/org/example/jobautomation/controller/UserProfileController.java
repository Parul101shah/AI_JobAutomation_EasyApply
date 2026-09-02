package org.example.jobautomation.controller;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.ExtractedProfile;
import org.example.jobautomation.dto.UserRegistrationRequest;
import org.example.jobautomation.entity.UserProfile;
import org.example.jobautomation.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {
    private UserProfileService userProfileService;
    //user manual pref
    @PostMapping("/register")
    public ResponseEntity<UserProfile> registerUser(@RequestBody UserRegistrationRequest userRegistrationRequest) {
        return ResponseEntity.ok(userProfileService.register(userRegistrationRequest));
    }
    //upload resume -> get Ai extracted preview
    @PostMapping("/{id}/upload-resume")
    public ResponseEntity<ExtractedProfile> resumeUser(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(userProfileService.uploadResume(id,file));
    }

    //User confirms (edit ) extracted file
    @PostMapping("/{id}/confirm-profile")
    public ResponseEntity<UserProfile> confirmProfile(@PathVariable Long id, @RequestBody ExtractedProfile extractedProfile) {
        return ResponseEntity.ok(userProfileService.confirmProfile(id, extractedProfile));
    }

    //View complete profile
    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userProfileService.getProfile(id));
    }

}
