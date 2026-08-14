package com.spms.parking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spms.parking.dto.request.CreateParkingSpaceRequest;
import com.spms.parking.dto.request.UpdateParkingSpaceRequest;
import com.spms.parking.dto.request.UpdateParkingSpaceStatusRequest;
import com.spms.parking.dto.response.ParkingSpaceResponse;
import com.spms.parking.service.ParkingSpaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/parking/spaces")
public class ParkingSpaceController {

    private final ParkingSpaceService parkingSpaceService;

    public ParkingSpaceController(ParkingSpaceService parkingSpaceService) {
        this.parkingSpaceService = parkingSpaceService;
    }

    @PostMapping
    public ResponseEntity<ParkingSpaceResponse> createSpace(@Valid @RequestBody CreateParkingSpaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSpaceService.createSpace(request));
    }

    @GetMapping
    public ResponseEntity<List<ParkingSpaceResponse>> searchSpaces(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) Boolean available) {
        return ResponseEntity.ok(parkingSpaceService.searchSpaces(city, zone, available));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> getSpace(@PathVariable Long id) {
        return ResponseEntity.ok(parkingSpaceService.getSpace(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> updateSpace(@PathVariable Long id,
            @Valid @RequestBody UpdateParkingSpaceRequest request) {
        return ResponseEntity.ok(parkingSpaceService.updateSpace(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpace(@PathVariable Long id) {
        parkingSpaceService.deleteSpace(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ParkingSpaceResponse> updateStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateParkingSpaceStatusRequest request) {
        return ResponseEntity.ok(parkingSpaceService.updateStatus(id, request));
    }
}
